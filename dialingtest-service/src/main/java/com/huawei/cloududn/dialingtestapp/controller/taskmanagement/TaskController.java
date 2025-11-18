/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.controller.taskmanagement;

import com.huawei.cloududn.dialingtest.api.TasksApi;
import com.huawei.cloududn.dialingtest.model.StartTaskRequest;
import com.huawei.cloududn.dialingtest.model.TaskEntity;
import com.huawei.cloududn.dialingtest.model.TaskPageResponse;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.TaskMgmtService;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.TaskTriggerService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 任务管理接口（人工启动/查询）。
 *
 * @author g00940940
 * @since 2025-10-24
 */
@RestController
@RequestMapping("/api")
public class TaskController implements TasksApi {
    private static final Logger logger = LoggerFactory.getLogger(TaskController.class);

    @Autowired
    private TaskTriggerService triggerService;

    @Autowired
    private TaskMgmtService taskMgmtService;

    @Override
    public ResponseEntity<TaskEntity> getTaskById(Integer id) {
        Long longId = id == null ? null : id.longValue();
        TaskEntity task = taskMgmtService.findById(longId);
        if (task == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } else {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(task);
        }
    }

    @Override
    public ResponseEntity<TaskPageResponse> getTasks(Integer page, Integer size) {
        int safePage = (page == null || page < 0) ? 0 : page;
        int safeSize = (size == null || size <= 0) ? 20 : size;
        List<TaskEntity> content = taskMgmtService.findMainTasks(safePage, safeSize);
        long total = taskMgmtService.countMainTasks();
        TaskPageResponse resp = new TaskPageResponse();
        for (TaskEntity e : content) {
            resp.addContentItem(e);
        }
        resp.totalElements((int) total);
        resp.size(safeSize);
        resp.number(safePage);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(resp);
    }

    @Override
    public ResponseEntity<TaskEntity> startTask(
            String xCsrfToken,
            String xUsername,
            @RequestBody StartTaskRequest body) {
        logger.info("Start task request received by user: {}", xUsername);

        // 验证用户名
        if (xUsername == null || xUsername.trim().isEmpty()) {
            logger.warn("Start task request missing username");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 验证CSRF令牌
        if (xCsrfToken == null || xCsrfToken.trim().isEmpty()) {
            logger.warn("Start task request missing CSRF token");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (body == null) {
            logger.warn("Start task request body is null");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(null);
        } else {
            // Basic validation: businessType not null/empty; scriptNames/targetUes must be non-empty lists
            String businessType = body.getBusinessType();
            java.util.List<String> scriptNames = body.getScriptNames();
            java.util.List<String> targetUes = body.getTargetUes();

            if (businessType == null || String.valueOf(businessType).trim().isEmpty()) {
                logger.warn("Validation failed: businessType is null or empty");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            } else if (scriptNames == null || scriptNames.isEmpty()) {
                logger.warn("Validation failed: scriptNames is null or empty");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            } else if (targetUes == null || targetUes.isEmpty()) {
                logger.warn("Validation failed: targetUes is null or empty");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            } else {
                // Reuse the API request directly (no conversion needed)
                body.setBusinessType(businessType);
                body.setScriptNames(scriptNames);
                body.setTargetUes(targetUes);
                TaskEntity task = triggerService.createTaskFromRequest(body);
                
                logger.info("Task started successfully with id: {} by user: {}", task.getId(), xUsername);
                return ResponseEntity.status(HttpStatus.ACCEPTED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(task);
            }
        }
    }

    @GetMapping("/tasks/{id}/subtasks")
    public ResponseEntity<List<TaskEntity>> getSubTasksByMainTaskId(@PathVariable("id") Integer id) {
        Long longId = id == null ? null : id.longValue();
        if (longId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } else {
            List<TaskEntity> list = taskMgmtService.findSubTasksByMainTaskId(longId);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(list);
        }
    }
}


