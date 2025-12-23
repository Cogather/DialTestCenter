/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.service.taskmanagement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.cloududn.dialingtest.model.StartTaskRequest;
import com.huawei.cloududn.dialingtest.model.TaskEntity;
import com.huawei.cloududn.dialingtest.model.TemplateEntity;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.dto.TaskContext;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration.TaskOrchestratorService;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration.state.TaskState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 任务触发服务：根据模板或人工请求创建主任务。
 *
 * @author g00940940
 * @since 2025-10-24
 */
@Service
public class TaskTriggerService {
    private static final Logger logger = LoggerFactory.getLogger(TaskTriggerService.class);

    @Autowired
    private TaskMgmtService taskMgmtService;

    @Autowired
    private TemplateMgmtService templateMgmtService;

    @Autowired
    private TaskOrchestratorService orchestratorService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public TaskEntity createTaskFromRequest(StartTaskRequest request) {
        TaskContext ctx = new TaskContext();
        if (request != null && request.getScenario() != null 
                && "TRAINING".equalsIgnoreCase(request.getScenario().toString())) {
            ctx.setStep(TaskState.START_TRAINING_DIALING);
        } else {
            ctx.setStep(TaskState.START_VALIDATION);
        }
        TaskEntity entity = buildMainTask("MANUAL", ctx, request == null ? null : toJson(request));
        // 初始化步骤进入后由状态机自动触发相应 Action
        entity = taskMgmtService.create(entity);
        
        // 显式触发当前状态的 Action
        Long taskId = entity.getId();
        orchestratorService.executeCurrentAction(taskId, ctx);
        
        return entity;
    }

    public TaskEntity createTaskFromTemplate(TemplateEntity template) {
        TaskContext ctx = new TaskContext();
        ctx.setStep(TaskState.START_VALIDATION);
        TaskEntity entity = buildMainTask("CRON", ctx, template == null ? null : template.getInput());
        entity.setTemplateTaskId(template == null ? null : template.getId());
        entity = taskMgmtService.create(entity);

        // 显式触发当前状态的 Action
        Long taskId = entity.getId();
        orchestratorService.executeCurrentAction(taskId, ctx);

        return entity;
    }

    private TaskEntity buildMainTask(String creator, TaskContext ctx, String inputJson) {
        TaskEntity entity = new TaskEntity();
        entity.setCreator(creator);
        entity.setStatus("RUNNING");
        entity.setResult(null);
        entity.setInput(inputJson);
        entity.setOutput(null);
        entity.setContext(toJson(ctx));
        entity.setMainTaskId(null); // 插入后由数据库触发器或后续流程设置为自身
        return entity;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.warn("Serialize context failed", e);
            return "{}";
        }
    }
}


