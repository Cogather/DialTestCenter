/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.cloududn.dialingtest.model.TaskEntity;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.TaskMgmtService;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.dto.TaskContext;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration.action.FullReleaseAction;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration.action.GrayValidationAction;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration.action.ReplayAction;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration.action.TrainModelAction;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration.action.ValidationAction;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration.listener.TaskStatePersistenceListener;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration.state.TaskEvent;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration.state.TaskState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.PostConstruct;

/**
 * 任务编排器：负责恢复/驱动状态机，并在变更后持久化context.step。
 *
 * <p>为最小可运行演示版：仅提供根据结果驱动状态转换与持久化的能力。</p>
 *
 * @author g00940940
 * @since 2025-10-24
 */
@Service
public class TaskOrchestratorService {
    private static final Logger logger = LoggerFactory.getLogger(TaskOrchestratorService.class);
    @Autowired
    private TaskStateMachine taskStateMachine;
    @Autowired
    private TaskMgmtService taskMgmtService;
    @Autowired
    private ValidationAction validationAction;
    @Autowired
    private TrainModelAction trainModelAction;
    @Autowired
    private ReplayAction replayAction;
    @Autowired
    private GrayValidationAction grayValidationAction;
    @Autowired
    private FullReleaseAction fullReleaseAction;
    @Autowired
    private TaskStatePersistenceListener persistenceListener;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        taskStateMachine.registerAction(TaskState.START_VALIDATION, validationAction);
        taskStateMachine.registerAction(TaskState.START_TRAINING_DIALING, validationAction);
        taskStateMachine.registerAction(TaskState.START_MODEL_TRAIN, trainModelAction);
        taskStateMachine.registerAction(TaskState.START_MODEL_REPLAY, replayAction);
        taskStateMachine.registerAction(TaskState.START_GRAY_VALIDATION, grayValidationAction);
        taskStateMachine.registerAction(TaskState.START_FULL_REPLAY, replayAction);
        taskStateMachine.registerAction(TaskState.START_WHITELIST, validationAction);
        taskStateMachine.registerAction(TaskState.START_FULL_RELEASE, fullReleaseAction);
        taskStateMachine.addListener(persistenceListener);
        logger.info("TaskOrchestratorService initialized with all actions and listeners");
    }

    public boolean sendResultEvent(Long mainTaskId, boolean success) {
        return sendResultEvent(mainTaskId, success, null);
    }

    public boolean sendResultEvent(Long mainTaskId, boolean success, java.util.Map<String, Object> resultData) {
        TaskEntity task = taskMgmtService.findById(mainTaskId);
        if (task == null) {
            logger.warn("Task not found: {}, cannot process result event. Success={}, ResultData={}", 
                    mainTaskId, success, resultData != null ? resultData.keySet() : "null");
            return false;
        }

        TaskContext ctx = readContext(task);
        TaskState currentState = getCurrentState(ctx);
        String newFingerprint = generateFingerprint(success, resultData);

        if (isDuplicateCallback(ctx, newFingerprint, mainTaskId, currentState)) {
            logger.debug("Duplicate callback detected for task: {}, ignoring", mainTaskId);
            return true;
        }

        updateContextWithResult(ctx, mainTaskId, resultData, newFingerprint);
        TaskState newState = executeStateTransition(currentState, success, ctx, mainTaskId);
        createSubTaskRecord(mainTaskId, success, resultData, currentState, newState, newFingerprint);
        updateTaskStatus(mainTaskId, success, resultData, ctx, newState);
        return true;
    }

    /**
     * 获取当前任务状态
     *
     * @param ctx 任务上下文
     * @return 当前任务状态
     */
    private TaskState getCurrentState(TaskContext ctx) {
        return ctx.getStep() == null ? TaskState.START_VALIDATION : ctx.getStep();
    }

    /**
     * 生成回调指纹
     *
     * @param success 是否成功
     * @param resultData 结果数据
     * @return 指纹字符串
     */
    private String generateFingerprint(boolean success, java.util.Map<String, Object> resultData) {
        String fingerprintPrefix = success ? "S:" : "F:";
        String resultTextForFp = serializeResultData(resultData);
        return fingerprintPrefix + resultTextForFp;
    }

    /**
     * 序列化结果数据为字符串
     *
     * @param resultData 结果数据
     * @return 序列化后的字符串
     */
    private String serializeResultData(java.util.Map<String, Object> resultData) {
        if (resultData == null || resultData.isEmpty()) {
            return "null";
        }

        try {
            return objectMapper.writeValueAsString(resultData);
        } catch (JsonProcessingException e) {
            return String.valueOf(resultData);
        }
    }

    /**
     * 检查是否为重复回调
     *
     * @param ctx 任务上下文
     * @param newFingerprint 新指纹
     * @param mainTaskId 主任务ID
     * @param currentState 当前状态
     * @return true表示重复回调
      */
     private boolean isDuplicateCallback(TaskContext ctx, String newFingerprint, Long mainTaskId,
             TaskState currentState) {
        Object lastFpObj = ctx.getData().get("last_callback_fingerprint");
        if (lastFpObj instanceof String) {
            String lastFp = (String) lastFpObj;
            if (lastFp.equals(newFingerprint)) {
                logger.info("Task {} duplicate callback ignored for state {} with fingerprint {}", 
                        mainTaskId, currentState, newFingerprint);
                return true;
            }
        } else {
            logger.debug("No previous fingerprint found, proceeding with state transition");
        }
        return false;
    }

    /**
     * 更新任务上下文和结果数据
     *
     * @param ctx 任务上下文
     * @param mainTaskId 主任务ID
     * @param resultData 结果数据
     * @param newFingerprint 新指纹
     */
    private void updateContextWithResult(
            TaskContext ctx, Long mainTaskId, 
            java.util.Map<String, Object> resultData, String newFingerprint) {
        ctx.getData().put("taskId", mainTaskId);
        if (resultData != null) {
            ctx.getData().put("callback_result", resultData);
        } else {
            logger.debug("No result data provided for task: {}", mainTaskId);
        }
        ctx.getData().put("last_callback_fingerprint", newFingerprint);
    }

    /**
     * 执行状态转换
     *
     * @param currentState 当前状态
     * @param success 是否成功
     * @param ctx 任务上下文
     * @param mainTaskId 主任务ID
     * @return 新状态
      */
     private TaskState executeStateTransition(TaskState currentState, boolean success, TaskContext ctx,
             Long mainTaskId) {
        TaskEvent event = success ? TaskEvent.TASK_SUCCESS : TaskEvent.TASK_FAILED;
        TaskState newState = taskStateMachine.sendEvent(currentState, event, ctx);
        logger.info("Task {} sent event {} on {} -> {}", mainTaskId, event, currentState, newState);
        return newState;
    }

    /**
     * 创建子任务记录
     *
     * @param mainTaskId 主任务ID
     * @param success 是否成功
     * @param resultData 结果数据
     * @param currentState 当前状态
     * @param newState 新状态
     * @param newFingerprint 新指纹
     */
    private void createSubTaskRecord(
            Long mainTaskId, boolean success, java.util.Map<String, Object> resultData, 
            TaskState currentState, TaskState newState, String newFingerprint) {
        try {
            TaskEntity sub = new TaskEntity();
            sub.setCreator("CALLBACK");
            sub.setStatus(success ? "COMPLETED" : "FAILED");
            sub.setResult(success ? "SUCCESS" : "FAILED");
            setSubTaskInput(sub, resultData);
            setSubTaskContext(sub, currentState, newState, newFingerprint);
            taskMgmtService.createSubTask(mainTaskId, mainTaskId, sub);
        } catch (Exception ex) {
            logger.warn("Create sub task record failed for mainTaskId={}", mainTaskId, ex);
        }
    }

    /**
     * 设置子任务输入数据
     *
     * @param sub 子任务实体
     * @param resultData 结果数据
     */
    private void setSubTaskInput(TaskEntity sub, java.util.Map<String, Object> resultData) {
        if (resultData != null && !resultData.isEmpty()) {
            try {
                sub.setInput(objectMapper.writeValueAsString(resultData));
            } catch (JsonProcessingException e) {
                sub.setInput(String.valueOf(resultData));
            }
        } else {
            sub.setInput("{}");
        }
    }

    /**
     * 设置子任务上下文
     *
     * @param sub 子任务实体
     * @param currentState 当前状态
     * @param newState 新状态
     * @param newFingerprint 新指纹
     */
    private void setSubTaskContext(TaskEntity sub, TaskState currentState, TaskState newState, String newFingerprint) {
        com.fasterxml.jackson.databind.node.ObjectNode subCtx = objectMapper.createObjectNode();
        subCtx.put("from", "callback");
        subCtx.put("state_before", currentState == null ? null : currentState.name());
        subCtx.put("state_after", newState == null ? null : newState.name());
        subCtx.put("fingerprint", newFingerprint);
        sub.setContext(subCtx.toString());
    }

    /**
     * 更新任务状态
     *
     * @param mainTaskId 主任务ID
     * @param success 是否成功
     * @param resultData 结果数据
     * @param ctx 任务上下文
     * @param newState 新状态
     */
    private void updateTaskStatus(
            Long mainTaskId, boolean success, java.util.Map<String, Object> resultData, 
            TaskContext ctx, TaskState newState) {
        if (newState == TaskState.FINAL) {
            updateFinalTaskStatus(mainTaskId, success, resultData, ctx);
        } else {
            taskMgmtService.updateStatusAndContext(mainTaskId, "RUNNING", null, toJson(ctx));
        }
    }

    /**
     * 更新最终任务状态
     *
     * @param mainTaskId 主任务ID
     * @param success 是否成功
     * @param resultData 结果数据
     * @param ctx 任务上下文
     */
    private void updateFinalTaskStatus(
            Long mainTaskId, boolean success, 
            java.util.Map<String, Object> resultData, TaskContext ctx) {
        String result = success ? "SUCCESS" : "FAILED";
        String resultText = serializeResultData(resultData);

        taskMgmtService.updateStatusAndContext(mainTaskId, success ? "COMPLETED" : "FAILED", result, toJson(ctx));

        if (!success && resultText != null && !"null".equals(resultText)) {
            logger.warn("Task {} finalized with failure: {}", mainTaskId, resultText);
        } else {
            logger.info("Task {} finalized successfully", mainTaskId);
        }
    }

    /**
     * 停止任务执行
     *
     * @param mainTaskId 主任务ID
     */
    public void stopTask(Long mainTaskId) {
        logger.info("Stopping task: {}", mainTaskId);

        TaskEntity task = taskMgmtService.findById(mainTaskId);
        if (task == null) {
            logger.warn("Task not found for stop operation: {}, task may have been deleted or never existed", mainTaskId);
            return;
        }

        TaskContext ctx = readContext(task);
        TaskState currentState = ctx.getStep() == null ? TaskState.START_VALIDATION : ctx.getStep();

        ctx.getData().put("taskId", mainTaskId);
        ctx.getData().put("stop_reason", "manual_stop");

        TaskState newState = taskStateMachine.sendEvent(currentState, TaskEvent.STOP, ctx);
        logger.info("Task {} sent STOP event on {} -> {}", mainTaskId, currentState, newState);

        // 更新任务状态为停止
        taskMgmtService.updateStatusAndContext(mainTaskId, "STOPPED", "STOPPED", toJson(ctx));

        logger.info("Task {} stopped successfully", mainTaskId);
    }

    private TaskContext readContext(TaskEntity task) {
        try {
            if (task.getContext() == null || task.getContext().trim().isEmpty()) {
                return new TaskContext();
            } else {
                return objectMapper.readValue(task.getContext(), TaskContext.class);
            }
        } catch (Exception e) {
            logger.warn("Parse context failed, using empty", e);
            return new TaskContext();
        }
    }

    private String toJson(TaskContext ctx) {
        try {
            return objectMapper.writeValueAsString(ctx);
        } catch (JsonProcessingException e) {
            logger.warn("Serialize context failed", e);
            return "{}";
        }
    }
}


