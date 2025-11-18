/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.service.executormanagement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.DeRegisterAckDto;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.DeRegisterRequestDto;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.ReportAckDto;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.ReportMsgDto;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.UeItemDto;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.flow.WssMessageSender;
import com.huawei.cloududn.dialingtestapp.dao.executormanagement.ExecutorDao;
import com.huawei.cloududn.dialingtestapp.dao.executormanagement.UeDao;
import com.huawei.cloududn.dialingtest.model.Executor;
import com.huawei.cloududn.dialingtest.model.Ue;
import com.huawei.cloududn.dialingtestapp.service.executormanagement.dto.ExecutorDetailDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;

import javax.websocket.Session;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Core executor management service: heartbeat and disconnect handling.
 *
 * <p>Implementation to be extended to update DB and UE list.</p>
 *
 * @author g00940940
 * @since 2025-11-04
 */
@Service
public class ExecutorMgmtService {

    private static final Logger logger = LoggerFactory.getLogger(ExecutorMgmtService.class);

    @Autowired
    private ExecutorDao executorDao;

    @Autowired
    private UeDao ueDao;

    @Autowired
    private SessionBindingRegistry registry;
    
    @Autowired
    private WssMessageSender wssMessageSender;

    /**
     * Handle heartbeat payload from agent (Legacy JSON version).
     *
     * @param data    heartbeat payload
     * @param session ws session
     * @deprecated Use {@link #handleReportMsg(ReportMsgDto, Session)} for V3 TLV protocol
     */
    @Deprecated
    public void handleHeartbeatStatus(JsonNode data, Session session) {
        logger.info("Received heartbeat, sessionId={}", session.getId());
        String executorName = registry.getExecutorName(session.getId());
        if (executorName == null) {
            logger.warn("Skip heartbeat: no binding for sessionId={}", session.getId());
            return;
        }
        logger.debug("Updating executor status to online, name={}", executorName);
        executorDao.updateStatus(executorName, 1, Instant.now());
        JsonNode ueList = data != null ? data.get("ue_list") : null;
        if (ueList != null && ueList.isArray()) {
            int ueCount = ueList.size();
            logger.debug("Processing UE list from heartbeat, executor={}, ueCount={}", executorName, ueCount);
            for (Iterator<JsonNode> it = ueList.elements(); it.hasNext(); ) {
                JsonNode ue = it.next();
                Ue ueModel = new Ue();
                ueModel.setMsisdn(text(ue, "msisdn"));
                ueModel.setExecutorName(executorName);
                ueModel.setVendor(text(ue, "vendor"));
                ueModel.setOs(text(ue, "os_version"));
                ueModel.setInfo(ue.toString());
                ueModel.setTaskInfo(null);
                ueDao.upsert(ueModel);
            }
            logger.debug("UE list processed successfully, executor={}, upserted {} records", executorName, ueCount);
        } else {
            logger.debug("No UE list in heartbeat, executor={}", executorName);
        }
    }
    
    /**
     * Handle ReportMsg (0x11): process heartbeat and UE list (V4 JSON version).
     * V4版本：接收ReportMsgDto，更新数据库，发送ReportAck
     *
     * @param dto     ReportMsg DTO (包含token、state、ue-list)
     * @param session WebSocket session
     */
    public void handleReportMsg(ReportMsgDto dto, Session session) {
        logger.info("Received ReportMsg, sessionId={}, token={}, state={}", 
            session.getId(), dto.getToken(), dto.getState());
        
        // Get executor name from session binding
        String executorName = registry.getExecutorName(session.getId());
        if (executorName == null) {
            logger.warn("Skip ReportMsg: no binding for sessionId={}", session.getId());
            sendReportAck(session.getId(), dto.getToken(), 1); // Error: no binding
            return;
        }
        
        // Update executor status and last_online_time
        logger.debug("Updating executor status to ONLINE, name={}", executorName);
        executorDao.updateStatus(executorName, 1, Instant.now()); // 1=ONLINE
        
        // Process UE list
        if (dto.getUeList() != null && !dto.getUeList().isEmpty()) {
            int ueCount = dto.getUeList().size();
            logger.debug("Processing UE list from ReportMsg, executor={}, ueCount={}", 
                executorName, ueCount);
            
            for (UeItemDto ueItem : dto.getUeList()) {
                processUeItem(executorName, ueItem);
            }
            
            logger.debug("UE list processed successfully, executor={}, upserted {} records", 
                executorName, ueCount);
        } else {
            logger.debug("No UE list in ReportMsg, executor={}", executorName);
        }
        
        // Send ReportAck (0x12)
        sendReportAck(session.getId(), dto.getToken(), 0); // 0=OK
        
        logger.info("ReportMsg processed successfully, executor={}", executorName);
    }
    
    /**
     * Process single UE item and upsert to database.
     * V3版本：处理UeItemDto，包含完整UE信息
     *
     * @param executorName executor name
     * @param ueItem       UE item DTO
     */
    private void processUeItem(String executorName, UeItemDto ueItem) {
        try {
            // Validate serial_no (used as msisdn)
            String serialNo = ueItem.getSerialNo();
            if (serialNo == null || serialNo.trim().isEmpty() || "null".equalsIgnoreCase(serialNo)) {
                logger.debug("Skipping UE item with null/empty serial_no, brand={}, model={}", 
                    ueItem.getBrand(), ueItem.getModel());
                return;
            }
            
            Ue ueModel = new Ue();
            
            // Map UeItemDto to Ue entity
            ueModel.setMsisdn(serialNo); // Use serial_no as msisdn
            ueModel.setExecutorName(executorName);
            ueModel.setVendor(ueItem.getBrand());
            
            // Build OS string, handle null values
            String os = ueItem.getOs();
            String version = ueItem.getVersion();
            if (os != null && version != null) {
                ueModel.setOs(os + " " + version);
            } else if (os != null) {
                ueModel.setOs(os);
            } else if (version != null) {
                ueModel.setOs(version);
            } else {
                ueModel.setOs("");
            }
            
            // Store detailed info as JSON in info field
            String infoJson = buildUeInfoJson(ueItem);
            ueModel.setInfo(infoJson);
            
            // Upsert to database
            ueDao.upsert(ueModel);
            
            logger.debug("UE info updated: serialNo={}, brand={}, model={}", 
                ueItem.getSerialNo(), ueItem.getBrand(), ueItem.getModel());
        } catch (IllegalArgumentException e) {
            logger.error("Failed to process UE item: serialNo={}", ueItem.getSerialNo(), e);
        } catch (Exception e) {
            logger.error("Unexpected error processing UE item: serialNo={}", ueItem.getSerialNo(), e);
        }
    }
    
    /**
     * Build UE info JSON string.
     *
     * @param ueItem UE item DTO
     * @return JSON string
     */
    private String buildUeInfoJson(UeItemDto ueItem) {
        // Simple JSON construction (should use ObjectMapper in production)
        // Handle null values properly
        return String.format(Locale.ROOT,
            "{\"serial\":\"%s\",\"brand\":\"%s\",\"model\":\"%s\",\"os\":\"%s\"," +
            "\"version\":\"%s\",\"resolution\":\"%s\",\"ipv4\":\"%s\",\"ipv6\":\"%s\"," +
            "\"battery\":%d}",
            nvl(ueItem.getSerialNo(), ""),
            nvl(ueItem.getBrand(), ""),
            nvl(ueItem.getModel(), ""),
            nvl(ueItem.getOs(), ""),
            nvl(ueItem.getVersion(), ""),
            nvl(ueItem.getWmsize(), ""),
            nvl(ueItem.getIpv4(), ""),
            nvl(ueItem.getIpv6(), ""),
            ueItem.getBattery() != null ? ueItem.getBattery() : 0
        );
    }
    
    /**
     * Null-safe string converter for JSON.
     *
     * @param value input string
     * @param defaultValue default value if null
     * @return non-null string
     */
    private String nvl(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value.trim())) {
            return defaultValue;
        } else {
            return value;
        }
    }
    
    /**
     * Send ReportAck (0x12).
     * V4版本：发送心跳应答
     *
     * @param sessionId session ID
     * @param token     token from ReportMsg
     * @param state     state (0=OK, non-zero=error)
     */
    private void sendReportAck(String sessionId, long token, int state) {
        ReportAckDto ackDto = new ReportAckDto(token, state);
        wssMessageSender.sendJsonMessage(sessionId, ackDto);
        
        logger.debug("Sent ReportAck to sessionId={}, token={}, state={}", 
            sessionId, token, state);
    }
    
    /**
     * Handle DeRegisterRequest (0x05): executor logout (V4 JSON version).
     * V4版本：处理Agent注销请求
     *
     * @param dto     DeRegister request DTO
     * @param session WebSocket session
     */
    public void handleDeRegisterRequest(DeRegisterRequestDto dto, Session session) {
        logger.info("Received DeRegisterRequest, sessionId={}, token={}", 
            session.getId(), dto.getToken());
        
        // Get executor name from session binding
        String executorName = registry.getExecutorName(session.getId());
        if (executorName == null) {
            logger.warn("Skip DeRegister: no binding for sessionId={}", session.getId());
            sendDeRegisterAck(session.getId(), dto.getToken(), 1, "No binding found");
            return;
        }
        
        try {
            // Update executor status to OFFLINE
            executorDao.updateStatus(executorName, 0, Instant.now()); // 0=OFFLINE
            
            // Unbind session
            registry.unbind(session.getId());
            
            // Send DeRegister-Ack (0x06)
            sendDeRegisterAck(session.getId(), dto.getToken(), 0, "Logout successful");
            
            logger.info("DeRegister completed successfully, executor={}", executorName);
        } catch (IllegalArgumentException e) {
            logger.error("Failed to deregister executor={}", executorName, e);
            sendDeRegisterAck(session.getId(), dto.getToken(), 2, "Database error");
        }
    }
    
    /**
     * Send DeRegisterAck (0x06).
     * V4版本：发送注销应答
     *
     * @param sessionId   session ID
     * @param token       token from DeRegisterRequest
     * @param resultCode  result code (0=OK, non-zero=error)
     * @param description description message
     */
    private void sendDeRegisterAck(String sessionId, long token, int resultCode, String description) {
        DeRegisterAckDto ackDto = new DeRegisterAckDto(token, resultCode, description);
        wssMessageSender.sendJsonMessage(sessionId, ackDto);
        
        logger.debug("Sent DeRegisterAck to sessionId={}, token={}, resultCode={}", 
            sessionId, token, resultCode);
    }

    /**
     * Handle disconnect event by session id.
     *
     * @param sessionId session id
     */
    public void handleExecutorDisconnect(String sessionId) {
        logger.info("Handle disconnect for sessionId={}", sessionId);
        String executorName = registry.getExecutorName(sessionId);
        if (executorName != null) {
            logger.debug("Updating executor status to offline, name={}", executorName);
            executorDao.updateStatus(executorName, 0, Instant.now());
            registry.unbind(sessionId);
            logger.info("Executor disconnected and status updated, name={}", executorName);
        } else {
            logger.debug("No executor binding found for sessionId={}", sessionId);
        }
    }

    /**
     * Handle heartbeat timeout for executor.
     * V4版本：当在阈值时间内未收到心跳时，将执行机标记为离线
     *
     * @param executorName executor name
     */
    public void handleHeartbeatTimeout(String executorName) {
        if (executorName == null || executorName.trim().isEmpty()) {
            logger.warn("Skip heartbeat timeout handling: executorName is empty");
            return;
        }
        logger.info("Handle heartbeat timeout for executor={}", executorName);
        executorDao.updateStatus(executorName, 0, Instant.now());
        String sessionId = registry.getSessionId(executorName);
        if (sessionId != null) {
            registry.unbind(sessionId);
            logger.debug("Executor {} marked OFFLINE due to heartbeat timeout, session unbound", executorName);
        } else {
            logger.debug("No session bound for executor={} when handling heartbeat timeout", executorName);
        }
    }

    /**
     * Handle deregister request from agent.
     *
     * @param data    deregister payload
     * @param session ws session
     */
    public void handleDeregister(JsonNode data, Session session) {
        String executorName = text(data, "name");
        logger.info("Received deregister request, executorName={}, sessionId={}", executorName, session.getId());
        if (executorName == null || executorName.isEmpty()) {
            logger.warn("Deregister request missing executor name, sessionId={}", session.getId());
            return;
        } else {
            String boundExecutor = registry.getExecutorName(session.getId());
            if (executorName.equals(boundExecutor)) {
                logger.debug("Updating executor status to offline, name={}", executorName);
                executorDao.updateStatus(executorName, 0, Instant.now());
                registry.unbind(session.getId());
                logger.info("Executor {} deregistered successfully", executorName);
            } else {
                logger.warn("Deregister name mismatch: requested={}, bound={}", executorName, boundExecutor);
            }
        }
    }

    /**
     * Handle executor info response from agent.
     *
     * @param data    executor info payload
     * @param session ws session
     */
    public void handleExecutorInfoResponse(JsonNode data, Session session) {
        String executorName = text(data, "executor_name");
        logger.info("Received executor_info_response, executorName={}, sessionId={}", executorName, session.getId());
        if (executorName == null || executorName.isEmpty()) {
            logger.warn("Executor info response missing executor name, sessionId={}", session.getId());
            return;
        } else {
            JsonNode ueDetails = data != null ? data.get("ue_details") : null;
            if (ueDetails != null && ueDetails.isArray()) {
                int ueCount = ueDetails.size();
                logger.debug("Processing UE details, executor={}, count={}", executorName, ueCount);
                int processedCount = 0;
                for (Iterator<JsonNode> it = ueDetails.elements(); it.hasNext(); ) {
                    JsonNode ueDetail = it.next();
                    String msisdn = text(ueDetail, "msisdn");
                    if (msisdn != null && !msisdn.isEmpty()) {
                        Ue ueModel = new Ue();
                        ueModel.setMsisdn(msisdn);
                        ueModel.setExecutorName(executorName);
                        ueModel.setVendor(text(ueDetail, "vendor"));
                        ueModel.setOs(text(ueDetail, "os_version"));
                        ueModel.setInfo(ueDetail.toString());
                        ueModel.setTaskInfo(null);
                        ueDao.upsert(ueModel);
                        processedCount++;
                        logger.debug("Updated UE info for msisdn={}", msisdn);
                    } else {
                        logger.debug("Skipping UE with empty msisdn");
                    }
                }
                logger.info("Executor info response processed successfully for {}, {} UE records updated", 
                        executorName, processedCount);
            } else {
                logger.debug("No UE details in executor info response");
            }
        }
    }

    /**
     * 获取执行机详细信息列表
     * V3版本：返回包含UE列表的执行机详情
     *
     * @return 执行机详情列表
     */
    public List<ExecutorDetailDto> getExecutorDetails() {
        logger.info("Getting executor details");
        try {
            // 获取所有执行机
            List<Executor> executors = executorDao.findPage(null, null, 0, 1000);
            logger.debug("Found {} executors", executors.size());

            return executors.stream().map(executor -> {
                // 获取该执行机关联的UE列表
                List<Ue> ueList = ueDao.findByExecutorName(executor.getName());
                List<UeItemDto> ueItemDtos = ueList.stream().map(this::convertUeToUeItemDto)
                        .collect(Collectors.toList());

                ExecutorDetailDto dto = new ExecutorDetailDto();
                dto.setName(executor.getName());
                dto.setIp(executor.getIp());
                dto.setStatus(executor.getStatus());
                dto.setLastOnlineTime(executor.getLastOnlineTime());
                dto.setUeList(ueItemDtos);

                return dto;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Failed to get executor details", e);
            throw new IllegalStateException("Failed to get executor details", e);
        }
    }

    /**
     * 发送心跳应答
     * V3版本：发送Report-Ack消息
     *
     * @param sessionId 会话ID
     * @param token     认证令牌
     */
    public void sendReportAck(String sessionId, Long token) {
        logger.info("Sending Report-Ack for sessionId={}, token={}", sessionId, token);
        ReportAckDto ackDto = new ReportAckDto(token, 0); // 0=OK
        wssMessageSender.sendJsonMessage(sessionId, ackDto);
        logger.debug("Sent Report-Ack to sessionId={}", sessionId);
    }

    /**
     * 将Ue实体转换为UeItemDto
     * V3版本：用于构造执行机详情响应
     *
     * @param ue UE实体
     * @return UeItemDto
     */
    private UeItemDto convertUeToUeItemDto(Ue ue) {
        UeItemDto dto = new UeItemDto();
        dto.setSerialNo(ue.getMsisdn());
        String infoJson = ue.getInfo();
        if (infoJson == null || infoJson.trim().isEmpty()) {
            logger.debug("UE info is empty for msisdn={}, using entity fields with defaults", ue.getMsisdn());
            // 优先使用 Ue 实体字段，如果为空则使用默认值
            dto.setBrand("Unknown");
            dto.setModel("Unknown");
            dto.setOs(ue.getOs() != null && !ue.getOs().trim().isEmpty() ? ue.getOs() : "Unknown");
            dto.setVersion("Unknown");
            dto.setWmsize("Unknown");
            dto.setIpv4("Unknown");
            dto.setIpv6("Unknown");
            dto.setBattery(0);
            return dto;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(infoJson);
            dto.setBrand(getTextOrDefault(node, "brand", "Unknown"));
            dto.setModel(getTextOrDefault(node, "model", "Unknown"));
            dto.setOs(getTextOrDefault(node, "os", "Unknown"));
            dto.setVersion(getTextOrDefault(node, "version", "Unknown"));
            dto.setWmsize(getTextOrDefault(node, "resolution", "Unknown"));
            dto.setIpv4(getTextOrDefault(node, "ipv4", "Unknown"));
            dto.setIpv6(getTextOrDefault(node, "ipv6", "Unknown"));
            dto.setBattery(node.has("battery") ? node.get("battery").asInt(0) : 0);
            logger.debug("Successfully parsed UE info for msisdn={}", ue.getMsisdn());
        } catch (JsonProcessingException e) {
            logger.warn("Failed to parse UE info JSON for msisdn={}, using default values",
                ue.getMsisdn(), e);
            setDefaultUeValues(dto);
        } catch (Exception e) {
            logger.error("Unexpected error parsing UE info for msisdn={}", ue.getMsisdn(), e);
            setDefaultUeValues(dto);
        }
        return dto;
    }
    
    /**
     * Get text value from JSON node with default fallback.
     *
     * @param node         JSON node
     * @param fieldName    field name
     * @param defaultValue default value if field missing or null
     * @return field value or default
     */
    private String getTextOrDefault(JsonNode node, String fieldName, String defaultValue) {
        if (node == null || !node.has(fieldName)) {
            return defaultValue;
        } else {
            String value = node.get(fieldName).asText("");
            if (value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value)) {
                return defaultValue;
            } else {
                return value;
            }
        }
    }
    
    /**
     * Set default values for UeItemDto.
     *
     * @param dto UeItemDto to set defaults
     */
    private void setDefaultUeValues(UeItemDto dto) {
        dto.setBrand("Unknown");
        dto.setModel("Unknown");
        dto.setOs("Unknown");
        dto.setVersion("Unknown");
        dto.setWmsize("Unknown");
        dto.setIpv4("Unknown");
        dto.setIpv6("Unknown");
        dto.setBattery(0);
    }

    private static String text(JsonNode node, String field) {
        return node != null && node.has(field) ? node.get(field).asText("") : "";
    }
}


