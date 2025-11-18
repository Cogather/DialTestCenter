/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration.event;

import com.huawei.cloududn.dialingtestapp.service.executormanagement.task.TaskInterfaceService;
import com.huawei.cloududn.dialingtestapp.service.executormanagement.dto.TaskDispatchRequest;
import com.huawei.cloududn.dialingtestapp.service.executormanagement.ExecutorSelectionService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 任务分发事件监听器，处理任务分发到执行机的逻辑
 *
 * @author g00940940
 * @since 2025-11-09
 */
@Component
public class TaskDispatchEventListener {
    private static final Logger logger = LoggerFactory.getLogger(TaskDispatchEventListener.class);

    @Autowired
    private TaskInterfaceService taskInterfaceService;

    @Autowired
    private ExecutorSelectionService executorSelectionService;

    /**
     * 处理任务分发事件 (V3 TLV版本)
     * 使用新的TaskInterfaceService接口，构造TaskDispatchRequest对象
     *
     * @param event 任务分发事件
     */
    @EventListener
    @Async
    public void handleTaskDispatch(TaskDispatchEvent event) {
        logger.info("Handling task dispatch event: taskId={}, sessionId={}", event.getTaskId(), event.getSessionId());
        try {
            // 从taskPayload中提取V3版本所需的信息
            if (event.getTaskPayload() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> payload = (Map<String, Object>) event.getTaskPayload();

                // 构造TaskDispatchRequest
                TaskDispatchRequest request = new TaskDispatchRequest();

                // 设置执行机名称（优先使用指定的，否则自动选择）
                String executorName = (String) payload.get("executor_name");
                if (executorName == null || executorName.isEmpty()) {
                    // 自动选择执行机
                    ExecutorSelectionService.ExecutorUeInfo executorUeInfo =
                            executorSelectionService.selectIdleExecutorAndUe();
                    if (executorUeInfo != null) {
                        executorName = executorUeInfo.getExecutor().getName();
                        logger.info("Auto-selected executor: {}", executorName);
                    } else {
                        logger.error("No available executor found for task: {}", event.getTaskId());
                        return;
                    }
                }
                request.setExecutorName(executorName);

                // 设置任务基本信息
                request.setTaskId(Integer.parseInt(event.getTaskId()));
                request.setScriptName((String) payload.getOrDefault("script_name", "default_script.air"));
                request.setVersion((String) payload.getOrDefault("script_version", "1.0"));
                request.setParameters(payload.getOrDefault("params", "{}").toString());
                request.setProctype((String) payload.getOrDefault("proctype", "default"));

                // 设置UE序列号列表
                @SuppressWarnings("unchecked")
                List<String> serialNoList = (List<String>) payload.get("serial_no_list");
                if (serialNoList == null || serialNoList.isEmpty()) {
                    serialNoList = Collections.singletonList("default_ue"); // 默认UE
                }
                request.setSerialNoList(serialNoList);

                // 调用新的TaskInterfaceService接口
                taskInterfaceService.dispatchTaskToAgent(request);
                logger.info("Task dispatched successfully: taskId={}, executor={}, scriptName={}, ueCount={}",
                    event.getTaskId(), executorName, request.getScriptName(), serialNoList.size());
            } else {
                logger.error("Invalid task payload type: expected Map, got {}", event.getTaskPayload().getClass());
            }
        } catch (Exception e) {
            logger.error("Failed to dispatch task: taskId={}", event.getTaskId(), e);
        }
    }
}

