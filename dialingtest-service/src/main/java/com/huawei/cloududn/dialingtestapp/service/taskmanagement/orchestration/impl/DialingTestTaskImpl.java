/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration.impl;

import com.huawei.cloududn.dialingtestapp.dao.taskmanagement.TaskExecutorMappingDao;
import com.huawei.cloududn.dialingtestapp.entity.taskmanagement.TaskExecutorMapping;
import com.huawei.cloududn.dialingtestapp.service.executormanagement.ExecutorSelectionService;
import com.huawei.cloududn.dialingtestapp.service.executormanagement.SessionBindingRegistry;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.dto.TaskContext;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration.event.TaskDispatchEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 拨测任务实现,负责任务下发到执行机
 *
 * @author g00940940
 * @since 2025-11-09
 */
@Component
public class DialingTestTaskImpl {
    private static final Logger logger = LoggerFactory.getLogger(DialingTestTaskImpl.class);

    @Autowired
    private ExecutorSelectionService executorSelectionService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private SessionBindingRegistry sessionBindingRegistry;

    @Autowired
    private TaskExecutorMappingDao taskExecutorMappingDao;

    /**
     * 启动拨测异步任务,选择执行机并下发任务
     *
     * @param context 任务上下文
     * @return 异步任务ID
     */
    public String start(TaskContext context) {
        String taskId = "T_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
        logger.info("Starting dialing test task: {}", taskId);
        ExecutorSelectionService.ExecutorUeInfo executorUeInfo = executorSelectionService.selectIdleExecutorAndUe();
        if (executorUeInfo == null) {
            logger.warn("No available executor and UE found for task: {}", taskId);
            return taskId;
        } else {
            String executorName = executorUeInfo.getExecutor().getName();
            String ueSerial = extractUeSerial(executorUeInfo.getUe().getInfo());
            logger.info("Selected executor={}, ue={} for task={}", executorName, ueSerial, taskId);
            Map<String, Object> taskPayload = buildTaskPayload(taskId, ueSerial, context);
            String sessionId = sessionBindingRegistry.getSessionId(executorName);
            if (sessionId == null) {
                logger.warn("No active session for executor={}, task={}", executorName, taskId);
                return taskId;
            } else {
                try {
                    TaskExecutorMapping mapping = new TaskExecutorMapping();
                    mapping.setTaskId(taskId);
                    mapping.setExecutorName(executorName);
                    mapping.setUeSerial(ueSerial);
                    mapping.setAssignTime(Instant.now().toString());
                    taskExecutorMappingDao.insert(mapping);
                    TaskDispatchEvent event = new TaskDispatchEvent(this, sessionId, taskPayload, taskId);
                    if (eventPublisher != null) {
                        eventPublisher.publishEvent(event);
                        logger.info("Task dispatch event published: taskId={}, executor={}, ue={}",
                                taskId, executorName, ueSerial);
                    } else {
                        logger.warn("EventPublisher is null, skipping event publish: taskId={}", taskId);
                    }
                    return taskId;
                } catch (Exception e) {
                    logger.error("Failed to publish task dispatch event: taskId={}", taskId, e);
                    return taskId;
                }
            }
        }
    }

    /**
     * 构建任务下发的payload
     *
     * @param taskId 任务ID
     * @param ueSerial UE序列号
     * @param context 任务上下文
     * @return 任务payload
     */
    private Map<String, Object> buildTaskPayload(String taskId, String ueSerial, TaskContext context) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("task_id", taskId);
        payload.put("ue_serial", ueSerial);
        payload.put("launcher", context.getData().getOrDefault("launcher", "default_launcher.py"));
        payload.put("script_name", context.getData().getOrDefault("script_name", "default_script.air"));
        payload.put("script_version", context.getData().getOrDefault("script_version", "1.0"));
        Map<String, Object> params = new HashMap<>();
        params.put("timeout", 300);
        payload.put("params", params);
        return payload;
    }

    /**
     * 从UE info JSON中提取serial字段
     *
     * @param ueInfo UE info JSON字符串
     * @return serial字段值或msisdn作为fallback
     */
    private String extractUeSerial(String ueInfo) {
        if (ueInfo == null || ueInfo.isEmpty()) {
            return "UNKNOWN";
        } else {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(ueInfo);
                if (node.has("serial")) {
                    return node.get("serial").asText("UNKNOWN");
                } else {
                    return node.has("msisdn") ? node.get("msisdn").asText("UNKNOWN") : "UNKNOWN";
                }
            } catch (Exception e) {
                logger.warn("Failed to parse UE info: {}", ueInfo, e);
                return "UNKNOWN";
            }
        }
    }
}


