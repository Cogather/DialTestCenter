/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.MessageType;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.AppInstallResponseDto;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.AppListResponseDto;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.DeRegisterRequestDto;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.JsonMessageEnvelope;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.RegisterRequestDto;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.RegisterResponseDto;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.ReportMsgDto;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.ScreencapResponseDto;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.ScriptUpdateAckDto;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.TaskStartResponseDto;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.TaskStopResponseDto;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.flow.InboundFileHandler;
import com.huawei.cloududn.dialingtestapp.service.executormanagement.ExecutorMgmtService;
import com.huawei.cloududn.dialingtestapp.service.executormanagement.auth.AuthSessionService;
import com.huawei.cloududn.dialingtestapp.service.executormanagement.task.TaskInterfaceService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;

import javax.websocket.Session;

/**
 * V5入站消息分发器
 * V5核心改进:
 * 1. dispatchControl(): 处理控制链路的JSON信令消息
 * 2. dispatchData(): 处理数据链路的二进制分片消息
 * 3. 支持双链路消息类型区分和路由
 *
 * @author g00940940
 * @since 2025-11-20
 */
@Component
public class WssMessageDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(WssMessageDispatcher.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InboundFileHandler inboundFileHandler;

    @Autowired
    private AuthSessionService authSessionService;

    @Autowired
    private ExecutorMgmtService executorMgmtService;

    @Autowired
    private TaskInterfaceService taskInterfaceService;

    /**
     * 分发控制链路JSON信令
     * V5: 专门处理控制链路消息
     *
     * @param jsonMessage JSON消息字符串
     * @param session WebSocket会话
     */
    public void dispatchControl(String jsonMessage, Session session) {
        try {
            JsonMessageEnvelope envelope = objectMapper.readValue(jsonMessage, JsonMessageEnvelope.class);
            String messageType = envelope.getType();
            logger.debug("Dispatching control message, sessionId={}, type={}", session.getId(), messageType);

            MessageType type = MessageType.fromJsonType(messageType);
            dispatchByMessageType(type, envelope, session, messageType);

        } catch (IllegalArgumentException e) {
            logger.error("Invalid control message type, sessionId={}", session.getId(), e);
        } catch (Exception e) {
            logger.error("Failed to dispatch control message, sessionId={}", session.getId(), e);
        }
    }

    /**
     * 兼容旧版本的dispatch方法
     * 委托给dispatchControl处理
     *
     * @param jsonMessage JSON消息字符串
     * @param session WebSocket会话
     */
    public void dispatch(String jsonMessage, Session session) {
        dispatchControl(jsonMessage, session);
    }

    /**
     * 根据消息类型分发到具体的处理方法
     *
     * @param type 消息类型
     * @param envelope 消息信封
     * @param session WebSocket会话
     * @param messageType 原始消息类型字符串
     */
    private void dispatchByMessageType(
            MessageType type, JsonMessageEnvelope envelope, Session session, String messageType) {
        switch (type) {
            case REGISTER_REQUEST: {
                handleRegisterRequest(envelope, session);
                break;
            }
            case REGISTER_RESPONSE: {
                handleRegisterResponse(envelope, session);
                break;
            }
            case DEREGISTER_REQUEST: {
                handleDeRegisterRequest(envelope, session);
                break;
            }
            case REPORT_MSG: {
                handleReportMsg(envelope, session);
                break;
            }
            case APP_LIST_RESPONSE: {
                handleAppListResponse(envelope, session);
                break;
            }
            case APP_INSTALL_RESPONSE: {
                handleAppInstallResponse(envelope, session);
                break;
            }
            case SCREENCAP_RESPONSE: {
                handleScreencapResponse(envelope, session);
                break;
            }
            case SCRIPT_UPDATE_ACK: {
                handleScriptUpdateAck(envelope, session);
                break;
            }
            case TASK_START_RESPONSE: {
                handleTaskStartResponse(envelope, session);
                break;
            }
            case TASK_STOP_RESPONSE: {
                handleTaskStopResponse(envelope, session);
                break;
            }
            default: {
                logger.warn("Unknown or unsupported message type: {}", messageType);
                break;
            }
        }
    }

    /**
     * 处理注册请求
     *
     * @param envelope 消息信封
     * @param session WebSocket会话
     */
    private void handleRegisterRequest(JsonMessageEnvelope envelope, Session session) {
        RegisterRequestDto dto = objectMapper.convertValue(envelope.getPayload(), RegisterRequestDto.class);
        authSessionService.handleRegisterRequest(dto, session);
    }

    /**
     * 处理注册响应
     *
     * @param envelope 消息信封
     * @param session WebSocket会话
     */
    private void handleRegisterResponse(JsonMessageEnvelope envelope, Session session) {
        RegisterResponseDto dto = objectMapper.convertValue(envelope.getPayload(), RegisterResponseDto.class);
        authSessionService.handleRegisterResponse(dto, session);
    }

    /**
     * 处理注销请求
     *
     * @param envelope 消息信封
     * @param session WebSocket会话
     */
    private void handleDeRegisterRequest(JsonMessageEnvelope envelope, Session session) {
        DeRegisterRequestDto dto = objectMapper.convertValue(envelope.getPayload(), DeRegisterRequestDto.class);
        executorMgmtService.handleDeRegisterRequest(dto, session);
    }

    /**
     * 处理上报消息
     *
     * @param envelope 消息信封
     * @param session WebSocket会话
     */
    private void handleReportMsg(JsonMessageEnvelope envelope, Session session) {
        ReportMsgDto dto = objectMapper.convertValue(envelope.getPayload(), ReportMsgDto.class);
        executorMgmtService.handleReportMsg(dto, session);
    }

    /**
     * 处理应用列表响应
     *
     * @param envelope 消息信封
     * @param session WebSocket会话
     */
    private void handleAppListResponse(JsonMessageEnvelope envelope, Session session) {
        AppListResponseDto dto = objectMapper.convertValue(envelope.getPayload(), AppListResponseDto.class);
        taskInterfaceService.handleAppListResponse(dto, session);
    }

    /**
     * 处理应用安装响应
     *
     * @param envelope 消息信封
     * @param session WebSocket会话
     */
    private void handleAppInstallResponse(JsonMessageEnvelope envelope, Session session) {
        AppInstallResponseDto dto = objectMapper.convertValue(envelope.getPayload(), AppInstallResponseDto.class);
        taskInterfaceService.handleAppInstallResponse(dto, session);
    }

    /**
     * 处理截屏响应
     *
     * @param envelope 消息信封
     * @param session WebSocket会话
     */
    private void handleScreencapResponse(JsonMessageEnvelope envelope, Session session) {
        ScreencapResponseDto dto = objectMapper.convertValue(envelope.getPayload(), ScreencapResponseDto.class);
        taskInterfaceService.handleScreencapResponse(dto, session);
    }

    /**
     * 处理脚本更新确认
     *
     * @param envelope 消息信封
     * @param session WebSocket会话
     */
    private void handleScriptUpdateAck(JsonMessageEnvelope envelope, Session session) {
        ScriptUpdateAckDto dto = objectMapper.convertValue(envelope.getPayload(), ScriptUpdateAckDto.class);
        taskInterfaceService.handleScriptUpdateAck(dto, session);
    }

    /**
     * 处理任务启动响应
     *
     * @param envelope 消息信封
     * @param session WebSocket会话
     */
    private void handleTaskStartResponse(JsonMessageEnvelope envelope, Session session) {
        TaskStartResponseDto dto = objectMapper.convertValue(envelope.getPayload(), TaskStartResponseDto.class);
        taskInterfaceService.handleTaskStartResponse(dto, session);
    }

    /**
     * 处理任务停止响应
     *
     * @param envelope 消息信封
     * @param session WebSocket会话
     */
    private void handleTaskStopResponse(JsonMessageEnvelope envelope, Session session) {
        TaskStopResponseDto dto = objectMapper.convertValue(envelope.getPayload(), TaskStopResponseDto.class);
        taskInterfaceService.handleTaskStopResponse(dto, session);
    }

    /**
     * 分发数据链路二进制分片
     * V5: 专门处理数据链路消息
     *
     * @param buffer 二进制数据
     * @param session WebSocket会话
     */
    public void dispatchData(ByteBuffer buffer, Session session) {
        try {
            String sessionId = session.getId();

            if (inboundFileHandler.isReceivingFile(sessionId)) {
                logger.debug("Handling data chunk for sessionId={}, size={} bytes",
                        sessionId, buffer.remaining());
                inboundFileHandler.handleChunk(sessionId, buffer);
            } else {
                logger.warn("Received unexpected data chunk, sessionId={}", sessionId);
            }

        } catch (Exception e) {
            logger.error("Failed to dispatch data chunk, sessionId={}", session.getId(), e);
        }
    }

    /**
     * 兼容旧版本的dispatch方法
     * 委托给dispatchData处理
     *
     * @param buffer 二进制数据
     * @param session WebSocket会话
     */
    public void dispatch(ByteBuffer buffer, Session session) {
        dispatchData(buffer, session);
    }
}

