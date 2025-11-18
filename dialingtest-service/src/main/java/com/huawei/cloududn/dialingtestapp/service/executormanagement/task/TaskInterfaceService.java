/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.service.executormanagement.task;

import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.*;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.flow.InboundFileCompleteEvent;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.flow.InboundFileHandler;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.flow.InboundFileState;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.flow.WssMessageSender;
import com.huawei.cloududn.dialingtestapp.dao.SoftwarePackageDao;
import com.huawei.cloududn.dialingtestapp.dao.TestCaseSetDao;
import com.huawei.cloududn.dialingtestapp.dao.executormanagement.ExecutorDao;
import com.huawei.cloududn.dialingtestapp.entity.SoftwarePackage;
import com.huawei.cloududn.dialingtest.model.TestCaseSet;
import com.huawei.cloududn.dialingtestapp.service.executormanagement.ExecutorSelectionService;
import com.huawei.cloududn.dialingtestapp.service.executormanagement.ExecutorSelectionService.ExecutorUeInfo;
import com.huawei.cloududn.dialingtestapp.service.executormanagement.SessionBindingRegistry;
import com.huawei.cloududn.dialingtestapp.service.executormanagement.dto.ScriptUpdateRequest;
import com.huawei.cloududn.dialingtestapp.service.executormanagement.dto.TaskDispatchRequest;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration.TaskOrchestratorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.Session;

/**
 * 任务接口服务：作为任务管理模块与执行机管理模块之间的适配器
 * V4版本：通过 Spring 事件机制监听文件完成事件，支持JSON信令和文件传输
 *
 * @author g00940940
 * @since 2025-11-14
 */
@Service
public class TaskInterfaceService {

    private static final Logger logger = LoggerFactory.getLogger(TaskInterfaceService.class);

    // 任务ID到执行机名称的映射，用于任务停止时查找
    private final Map<Integer, String> taskToExecutorMap = new ConcurrentHashMap<>();

    @Autowired
    private WssMessageSender wssMessageSender;

    @Autowired
    private InboundFileHandler inboundFileHandler;

    @Autowired
    private ExecutorDao executorDao;

    @Autowired
    private SessionBindingRegistry sessionBindingRegistry;

    @Autowired
    private ExecutorSelectionService executorSelectionService;

    @Autowired
    private TaskOrchestratorService taskOrchestratorService;

    @Autowired
    private TestCaseSetDao testCaseSetDao;

    @Autowired
    private SoftwarePackageDao softwarePackageDao;

    /**
     * 向执行机分发拨测任务（支持自动选择执行机/UE）
     * V4版本：当请求中未指定执行机时，通过ExecutorSelectionService自动选择
     *
     * @param request 任务分发请求
     */
    public void dispatchTask(TaskDispatchRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("TaskDispatchRequest must not be null");
        }

        if (request.getExecutorName() == null || request.getExecutorName().trim().isEmpty()) {
            logger.info("No executor specified in request, trying to select one automatically for taskId={}",
                    request.getTaskId());
            ExecutorUeInfo selected = executorSelectionService.selectIdleExecutorAndUe();
            if (selected == null) {
                logger.error("No available executor/UE for taskId={}", request.getTaskId());
                throw new IllegalStateException("No available executor/UE for task " + request.getTaskId());
            }
            request.setExecutorName(selected.getExecutor().getName());
            if (request.getSerialNoList() == null || request.getSerialNoList().isEmpty()) {
                java.util.List<String> serialList = new java.util.ArrayList<>();
                if (selected.getUe() != null && selected.getUe().getMsisdn() != null) {
                    serialList.add(selected.getUe().getMsisdn());
                }
                request.setSerialNoList(serialList);
            }
            logger.info("Executor auto selected for taskId={}, executor={}, ue={}",
                    request.getTaskId(),
                    selected.getExecutor().getName(),
                    selected.getUe() != null ? selected.getUe().getMsisdn() : null);
        }

        dispatchTaskToAgent(request);
    }

    /**
     * 向指定执行机分发拨测任务
     * V3版本：支持serial-no-list/proctype/sub-result等新字段
     *
     * @param request 任务分发请求
     */
    public void dispatchTaskToAgent(TaskDispatchRequest request) {
        logger.info("Dispatching task to executor: {}", request.getExecutorName());

        try {
            // 获取执行机会话
            String sessionId = sessionBindingRegistry.getSessionId(request.getExecutorName());
            if (sessionId == null) {
                logger.error("No session found for executor: {}", request.getExecutorName());
                throw new IllegalStateException("Executor not connected: " + request.getExecutorName());
            }

            // 构造TaskStartRequestDto
            TaskStartRequestDto taskDto = new TaskStartRequestDto();
            taskDto.setTaskId(request.getTaskId());
            taskDto.setScriptName(request.getScriptName());
            taskDto.setVersion(request.getVersion());
            taskDto.setSerialNoList(request.getSerialNoList());
            taskDto.setProcType(request.getProctype() != null ? Integer.valueOf(request.getProctype()) : 1);
            taskDto.setParameters(request.getParameters());

            // V4: 发送任务开始消息（JSON格式）
            wssMessageSender.sendJsonMessage(sessionId, taskDto);

            // 记录任务到执行机的映射，用于后续停止操作
            taskToExecutorMap.put(request.getTaskId(), request.getExecutorName());

            logger.info("Task dispatched successfully: taskId={}, executor={}, sessionId={}",
                request.getTaskId(), request.getExecutorName(), sessionId);

        } catch (Exception e) {
            logger.error("Failed to dispatch task: taskId={}, executor={}",
                request.getTaskId(), request.getExecutorName(), e);
            throw new IllegalStateException("Task dispatch failed", e);
        }
    }

    /**
     * 处理任务启动响应
     * V4版本：处理JSON格式的TaskStart-Response消息，支持文件传输
     *
     * @param dto     任务启动响应DTO
     * @param session WebSocket会话
     */
    public void handleTaskStartResponse(TaskStartResponseDto dto, Session session) {
        logger.info("Handling task start response: taskId={}, result={}", dto.getTaskId(), dto.getResult());

        try {
            boolean isSuccess = "SUCCESS".equalsIgnoreCase(dto.getResult()) ||
                               "success".equalsIgnoreCase(dto.getResult());

            if (dto.getFilelen() > 0) {
                String tempPath = "/tmp/task_result_" + dto.getTaskId() + ".log";
                inboundFileHandler.startReceiving(session.getId(), dto.getFilelen(), dto.getCrc(), tempPath, dto);
                logger.info("Started receiving task result file for taskId={}, size={} bytes",
                        dto.getTaskId(), dto.getFilelen());
            } else {
                processTaskStartResponse(dto);
            }

        } catch (Exception e) {
            logger.error("Failed to handle task start response: taskId={}", dto.getTaskId(), e);
        }
    }

    private void processTaskStartResponse(TaskStartResponseDto dto) {
        boolean isSuccess = "SUCCESS".equalsIgnoreCase(dto.getResult()) ||
                           "success".equalsIgnoreCase(dto.getResult());

        Map<String, Object> resultData = new HashMap<>();
        resultData.put("task_id", dto.getTaskId());
        resultData.put("result", dto.getResult());
        resultData.put("block", dto.getBlock());
        resultData.put("sub_results", dto.getSubResult());
        resultData.put("description", dto.getDescription());
        if (dto.getFilelen() > 0) {
            resultData.put("has_file", Boolean.TRUE);
        } else {
            resultData.put("has_file", Boolean.FALSE);
        }

        taskOrchestratorService.sendResultEvent((long) dto.getTaskId(), isSuccess, resultData);

        logger.info("Task start response processed successfully: taskId={}, result={}",
                dto.getTaskId(), dto.getResult());
    }

    /**
     * 处理任务停止请求
     * V3版本：接收上层模块的中断指令，发送TaskStop-Request
     *
     * @param taskId 任务ID
     */
    public void handleTaskStopRequest(Integer taskId) {
        logger.info("Handling task stop request: taskId={}", taskId);

        try {
            // 根据taskId找到对应的executor和session
            String executorName = taskToExecutorMap.get(taskId);
            if (executorName == null) {
                logger.warn("No executor mapping found for task: taskId={}", taskId);
                return;
            }

            String sessionId = sessionBindingRegistry.getSessionId(executorName);

            if (sessionId != null) {
                TaskStopRequestDto stopDto = new TaskStopRequestDto();
                stopDto.setTaskId(taskId);
                // V4: 发送任务停止消息（JSON格式）
                wssMessageSender.sendJsonMessage(sessionId, stopDto);
                logger.info("Task stop request sent: taskId={}, executor={}", taskId, executorName);
            } else {
                logger.warn("No session found for task stop request: taskId={}, executor={}", taskId, executorName);
            }

        } catch (Exception e) {
            logger.error("Failed to handle task stop request: taskId={}", taskId, e);
        }
    }

    /**
     * 处理任务停止响应
     * V3版本：处理TaskStop-Response消息
     *
     * @param dto     任务停止响应DTO
     * @param session WebSocket会话
     */
    public void handleTaskStopResponse(TaskStopResponseDto dto, Session session) {
        logger.info("Handling task stop response: taskId={}, state={}", dto.getTaskId(), dto.getState());

        try {
            // 通知上层任务管理模块任务已停止
            taskOrchestratorService.stopTask((long) dto.getTaskId());

            // 清理任务映射
            taskToExecutorMap.remove(dto.getTaskId());

            logger.info("Task stop response processed successfully: taskId={}, state={}",
                    dto.getTaskId(), dto.getState());

        } catch (Exception e) {
            logger.error("Failed to handle task stop response: taskId={}", dto.getTaskId(), e);
        }
    }

    /**
     * 发送App列表查询请求
     * V4版本：查询指定执行机和UE的已安装App列表
     *
     * @param executorName 执行机名称
     * @param serialNo UE序列号
     */
    public void sendAppListQuery(String executorName, String serialNo) {
        logger.info("Sending app list query: executor={}, serialNo={}", executorName, serialNo);

        try {
            String sessionId = sessionBindingRegistry.getSessionId(executorName);
            if (sessionId != null) {
                AppListQueryDto queryDto = new AppListQueryDto();
                queryDto.setSerialNo(serialNo);
                // V4: 发送App列表查询（JSON格式）
                wssMessageSender.sendJsonMessage(sessionId, queryDto);
                logger.debug("App list query sent: executor={}, serialNo={}", executorName, serialNo);
            } else {
                logger.warn("No session found for app list query: executor={}", executorName);
            }

        } catch (Exception e) {
            logger.error("Failed to send app list query: executor={}, serialNo={}", executorName, serialNo, e);
        }
    }

    /**
     * 发送脚本更新请求
     * V4版本：向指定执行机推送新的拨测脚本（JSON信令 + 文件流）
     *
     * @param executorName 执行机名称
     * @param script       脚本更新请求
     */
    public void sendScriptUpdate(String executorName, ScriptUpdateRequest script) {
        logger.info("Sending script update: executor={}, scriptName={}", executorName, script.getScriptName());

        try {
            String sessionId = sessionBindingRegistry.getSessionId(executorName);
            if (sessionId != null) {
                ScriptUpdateNotifyDto updateDto = new ScriptUpdateNotifyDto();
                updateDto.setScriptName(script.getScriptName());
                updateDto.setVersion(script.getVersion());
                updateDto.setFilelen(script.getScriptFile().length);
                updateDto.setCrc(script.getCrc());
                
                // V4: 发送脚本更新（JSON信令 + 文件流）
                java.io.ByteArrayInputStream fileStream = new java.io.ByteArrayInputStream(script.getScriptFile());
                wssMessageSender.sendFile(sessionId, updateDto, fileStream);
                logger.debug("Script update sent: executor={}, scriptName={}, size={} bytes",
                        executorName, script.getScriptName(), script.getScriptFile().length);
            } else {
                logger.warn("No session found for script update: executor={}", executorName);
            }

        } catch (Exception e) {
            logger.error("Failed to send script update: executor={}, scriptName={}",
                executorName, script.getScriptName(), e);
        }
    }

    /**
     * 发送UE截屏查询请求
     * V4版本：查询指定执行机和UE的屏幕截图（PNG文件通过Binary分片上传）
     *
     * @param executorName 执行机名称
     * @param serialNo UE序列号
     */
    public void sendScreanCapQuery(String executorName, String serialNo) {
        logger.info("Sending screencap query: executor={}, serialNo={}", executorName, serialNo);

        try {
            String sessionId = sessionBindingRegistry.getSessionId(executorName);
            if (sessionId != null) {
                ScreencapQueryDto queryDto = new ScreencapQueryDto();
                queryDto.setSerialNo(serialNo);
                // V4: 发送截屏查询（JSON格式）
                wssMessageSender.sendJsonMessage(sessionId, queryDto);
                logger.debug("Screencap query sent: executor={}, serialNo={}", executorName, serialNo);
            } else {
                logger.warn("No session found for screencap query: executor={}", executorName);
            }

        } catch (Exception e) {
            logger.error("Failed to send screencap query: executor={}, serialNo={}", executorName, serialNo, e);
        }
    }


    /**
     * 处理UE截屏响应
     * V4版本：处理JSON格式的UE截屏响应，支持文件传输
     *
     * @param dto     截屏响应DTO
     * @param session WebSocket会话
     */
    public void handleScreencapResponse(ScreencapResponseDto dto, Session session) {
        logger.info("Handling screencap response: serialNo={}, state={}, filename={}",
                dto.getSerialNo(), dto.getState(), dto.getFilename());

        try {
            if (dto.isSuccess() && dto.getFilelen() != null && dto.getFilelen() > 0) {
                String tempPath = "/tmp/screencap_" + dto.getSerialNo() + "_" + System.currentTimeMillis() + ".png";
                inboundFileHandler.startReceiving(session.getId(), dto.getFilelen(), dto.getCrc(), tempPath, dto);
                logger.info("Started receiving screencap file for serialNo={}, size={} bytes",
                        dto.getSerialNo(), dto.getFilelen());
            } else {
                if (!dto.isSuccess()) {
                    logger.warn("Screencap failed for serialNo: {}, state={}", dto.getSerialNo(), dto.getState());
                } else {
                    logger.info("Screencap completed (no file) for serialNo: {}", dto.getSerialNo());
                }
            }

        } catch (Exception e) {
            logger.error("Failed to handle screencap response: serialNo={}", dto.getSerialNo(), e);
        }
    }

    /**
     * 文件接收完成事件监听器
     * V4版本：通过 Spring 事件机制监听 InboundFileCompleteEvent
     *
     * @param event 文件完成事件
     */
    @EventListener
    public void handleFileComplete(InboundFileCompleteEvent event) {
        InboundFileState state = event.getState();
        logger.info("File receive completed: sessionId={}, filePath={}, size={}/{} bytes",
                state.getSessionId(), state.getTempFilePath(),
                state.getReceivedSize(), state.getExpectedSize());

        if (state.hasError()) {
            logger.error("File receive failed: sessionId={}, error={}",
                    state.getSessionId(), state.getError());
            return;
        }

        if (!state.verifyCrc()) {
            logger.error("File CRC verification failed: sessionId={}", state.getSessionId());
            return;
        }

        Object businessContext = state.getBusinessContext();
        if (businessContext instanceof TaskStartResponseDto) {
            TaskStartResponseDto dto = (TaskStartResponseDto) businessContext;
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("task_id", dto.getTaskId());
            resultData.put("result", dto.getResult());
            resultData.put("block", dto.getBlock());
            resultData.put("sub_results", dto.getSubResult());
            resultData.put("description", dto.getDescription());
            resultData.put("log_path", state.getTempFilePath());
            resultData.put("has_file", Boolean.TRUE);
            boolean isSuccess = "SUCCESS".equalsIgnoreCase(dto.getResult()) ||
                    "success".equalsIgnoreCase(dto.getResult());
            taskOrchestratorService.sendResultEvent((long) dto.getTaskId(), isSuccess, resultData);
            logger.info("Task result file stored for taskId={}, path={}",
                    dto.getTaskId(), state.getTempFilePath());
        } else if (businessContext instanceof ScreencapResponseDto) {
            ScreencapResponseDto dto = (ScreencapResponseDto) businessContext;
            logger.info("Screencap file saved: serialNo={}, path={}",
                    dto.getSerialNo(), state.getTempFilePath());
        } else {
            logger.warn("Unknown business context type: {}", 
                    businessContext != null ? businessContext.getClass().getName() : "null");
        }
    }


    /**
     * 处理App安装响应
     * V4版本：处理JSON格式的App安装响应
     *
     * @param dto     App安装响应DTO
     * @param session WebSocket会话
     */
    public void handleAppInstallResponse(AppInstallResponseDto dto, Session session) {
        logger.info("Handling app install response: serialNo={}, taskId={}, state={}",
                dto.getSerialNo(), dto.getTaskId(), dto.getState());

        if (dto.getState() == 0) {
            logger.info("App install completed successfully: serialNo={}, taskId={}",
                    dto.getSerialNo(), dto.getTaskId());
        } else {
            logger.warn("App install failed: serialNo={}, taskId={}, state={}",
                    dto.getSerialNo(), dto.getTaskId(), dto.getState());
        }
    }

    /**
     * 处理App列表响应
     * V4版本：处理JSON格式的App列表响应
     *
     * @param dto     App列表响应DTO
     * @param session WebSocket会话
     */
    public void handleAppListResponse(AppListResponseDto dto, Session session) {
        logger.info("Handling app list response: serialNo={}, state={}, appCount={}",
                dto.getSerialNo(), dto.getState(),
                dto.getAppList() != null ? dto.getAppList().size() : 0);

        if (dto.getState() == 0) {
            logger.info("App list query completed successfully for serialNo: {}", dto.getSerialNo());
        } else {
            logger.warn("App list query failed for serialNo: {}, state={}",
                    dto.getSerialNo(), dto.getState());
        }
    }

    /**
     * 处理脚本更新确认
     * V4版本：处理JSON格式的脚本更新确认
     *
     * @param dto     脚本更新确认DTO
     * @param session WebSocket会话
     */
    public void handleScriptUpdateAck(ScriptUpdateAckDto dto, Session session) {
        logger.info("Handling script update ack: scriptName={}, version={}, state={}",
                dto.getScriptName(), dto.getVersion(), dto.getState());

        if (dto.getState() == 0) {
            logger.info("Script update completed successfully: scriptName={}, version={}",
                    dto.getScriptName(), dto.getVersion());
        } else {
            logger.warn("Script update failed: scriptName={}, version={}, state={}",
                    dto.getScriptName(), dto.getVersion(), dto.getState());
        }
    }




    /**
     * 向指定执行机推送脚本更新（从数据库读取）
     * V3版本：从test_case_set表读取脚本内容并推送到执行机
     * 对应设计文档5.6节流程
     *
     * @param executorName 执行机名称
     * @param scriptName 脚本名称
     * @param version 版本号
     */
    public void pushScriptToExecutor(String executorName, String scriptName, String version) {
        logger.info("Pushing script to executor: executor={}, scriptName={}, version={}", 
                   executorName, scriptName, version);
        
        try {
            // 1. 从数据库查询脚本数据
            TestCaseSet testCaseSet = testCaseSetDao.findByNameAndVersion(scriptName, version);
            if (testCaseSet == null) {
                logger.warn("Test case set not found: name={}, version={}", scriptName, version);
                return;
            }
            
            // 2. 获取文件内容和SHA256
            byte[] fileContent = testCaseSet.getFileContent();
            if (fileContent == null || fileContent.length == 0) {
                logger.warn("Test case set file content is empty: name={}, version={}", scriptName, version);
                return;
            }
            
            String sha256 = testCaseSet.getSha256();
            
            // 3. 构造ScriptUpdateRequest对象
            ScriptUpdateRequest request = new ScriptUpdateRequest();
            request.setScriptName(scriptName);
            request.setVersion(version);
            request.setScriptFile(fileContent);
            request.setCrc(sha256 != null ? sha256 : "");
            
            // 4. 调用现有的sendScriptUpdate方法推送到执行机
            sendScriptUpdate(executorName, request);
            
            logger.info("Script push completed: executor={}, scriptName={}, version={}, size={} bytes", 
                       executorName, scriptName, version, fileContent.length);
            
        } catch (Exception e) {
            logger.error("Failed to push script to executor: executor={}, scriptName={}, version={}", 
                       executorName, scriptName, version, e);
        }
    }

    /**
     * 向指定执行机和UE推送APP安装（从数据库读取）
     * V3版本：从software_package表读取APP文件并推送到执行机
     * 对应设计文档5.5节流程
     *
     * @param executorName 执行机名称
     * @param serialNo UE序列号
     * @param appName APP名称
     * @param taskId 任务ID
     */
    public void pushAppToUe(String executorName, String serialNo, String appName, Integer taskId) {
        logger.info("Pushing app to UE: executor={}, serialNo={}, appName={}, taskId={}", 
                   executorName, serialNo, appName, taskId);
        
        try {
            // 1. 从数据库查询APP数据
            SoftwarePackage softwarePackage = softwarePackageDao.findBySoftwareName(appName);
            if (softwarePackage == null) {
                logger.warn("Software package not found: appName={}", appName);
                return;
            }
            
            // 2. 获取文件内容
            byte[] fileContent = softwarePackage.getFileContent();
            if (fileContent == null || fileContent.length == 0) {
                logger.warn("Software package file content is empty: appName={}", appName);
                return;
            }
            
            // 3. 获取会话ID
            String sessionId = sessionBindingRegistry.getSessionId(executorName);
            if (sessionId == null) {
                logger.error("No session found for executor: {}", executorName);
                return;
            }
            
            // 4. 构造AppInstallRequestDto并发送（V4版本）
            AppInstallRequestDto installDto = new AppInstallRequestDto();
            installDto.setSerialNo(serialNo);
            installDto.setTaskId(taskId);
            installDto.setAppName(appName);
            installDto.setFilelen(fileContent.length);
            installDto.setFiletype("package");
            installDto.setCrc(softwarePackage.getFileSha256());
            
            // 5. 发送APP安装请求（JSON信令 + 文件流）
            java.io.ByteArrayInputStream fileStream = new java.io.ByteArrayInputStream(fileContent);
            wssMessageSender.sendFile(sessionId, installDto, fileStream);
            
            logger.info("App push completed: executor={}, serialNo={}, appName={}, taskId={}, size={} bytes", 
                       executorName, serialNo, appName, taskId, fileContent.length);
            
        } catch (Exception e) {
            logger.error("Failed to push app to UE: executor={}, serialNo={}, appName={}, taskId={}", 
                       executorName, serialNo, appName, taskId, e);
        }
    }

}


