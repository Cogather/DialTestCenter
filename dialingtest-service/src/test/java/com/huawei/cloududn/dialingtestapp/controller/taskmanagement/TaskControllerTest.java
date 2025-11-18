/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.controller.taskmanagement;

import com.huawei.cloududn.dialingtest.model.StartTaskRequest;
import com.huawei.cloududn.dialingtest.model.TaskEntity;
import com.huawei.cloududn.dialingtest.model.TaskPageResponse;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.TaskMgmtService;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.TaskTriggerService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 任务管理控制器测试类
 *
 * @author g00940940
 * @since 2025-11-13
 */
@RunWith(MockitoJUnitRunner.class)
public class TaskControllerTest {
    @Mock
    private TaskTriggerService triggerService;
    @Mock
    private TaskMgmtService taskMgmtService;
    @InjectMocks
    private TaskController taskController;

    @Test
    public void testGetTaskById_Success_ReturnsOk() {
        // Arrange
        TaskEntity task = new TaskEntity();
        task.setId(1);
        when(taskMgmtService.findById(1L)).thenReturn(task);

        // Act
        ResponseEntity<TaskEntity> response = taskController.getTaskById(1);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(task, response.getBody());
    }

    @Test
    public void testGetTaskById_NotFound_ReturnsNotFound() {
        // Arrange
        when(taskMgmtService.findById(999L)).thenReturn(null);

        // Act
        ResponseEntity<TaskEntity> response = taskController.getTaskById(999);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void testGetTasks_Success_ReturnsOk() {
        // Arrange
        List<TaskEntity> tasks = Collections.singletonList(new TaskEntity());
        when(taskMgmtService.findMainTasks(0, 20)).thenReturn(tasks);
        when(taskMgmtService.countMainTasks()).thenReturn(1L);

        // Act
        ResponseEntity<TaskPageResponse> response = taskController.getTasks(null, null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements().intValue());
    }

    @Test
    public void testStartTask_Success_ReturnsAccepted() {
        // Arrange
        StartTaskRequest request = new StartTaskRequest();
        request.setBusinessType("test");
        request.setScriptNames(Collections.singletonList("script1"));
        request.setTargetUes(Collections.singletonList("ue1"));
        TaskEntity task = new TaskEntity();
        when(triggerService.createTaskFromRequest(any())).thenReturn(task);

        // Act
        ResponseEntity<TaskEntity> response = taskController.startTask("test-csrf-token", "admin", request);

        // Assert
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        verify(triggerService).createTaskFromRequest(any());
    }

    @Test
    public void testStartTask_InvalidRequest_ReturnsBadRequest() {
        // Test null body
        assertEquals(HttpStatus.BAD_REQUEST,
                taskController.startTask("test-csrf-token", "admin", null).getStatusCode());
        
        // Test missing businessType
        StartTaskRequest req1 = new StartTaskRequest();
        req1.setScriptNames(Collections.singletonList("s1"));
        req1.setTargetUes(Collections.singletonList("u1"));
        assertEquals(HttpStatus.BAD_REQUEST,
                taskController.startTask("test-csrf-token", "admin", req1).getStatusCode());
        
        // Test empty scriptNames
        StartTaskRequest req2 = new StartTaskRequest();
        req2.setBusinessType("test");
        req2.setScriptNames(Collections.emptyList());
        req2.setTargetUes(Collections.singletonList("u1"));
        assertEquals(HttpStatus.BAD_REQUEST,
                taskController.startTask("test-csrf-token", "admin", req2).getStatusCode());
        
        verify(triggerService, never()).createTaskFromRequest(any());
    }

    @Test
    public void testStartTask_MissingUsername_ReturnsUnauthorized() {
        // Arrange
        StartTaskRequest request = new StartTaskRequest();
        request.setBusinessType("test");
        request.setScriptNames(Collections.singletonList("script1"));
        request.setTargetUes(Collections.singletonList("ue1"));

        // Act
        ResponseEntity<TaskEntity> response = taskController.startTask("test-csrf-token", null, request);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(triggerService, never()).createTaskFromRequest(any());
    }

    @Test
    public void testStartTask_MissingCsrfToken_ReturnsForbidden() {
        // Arrange
        StartTaskRequest request = new StartTaskRequest();
        request.setBusinessType("test");
        request.setScriptNames(Collections.singletonList("script1"));
        request.setTargetUes(Collections.singletonList("ue1"));

        // Act
        ResponseEntity<TaskEntity> response = taskController.startTask(null, "admin", request);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(triggerService, never()).createTaskFromRequest(any());
    }

    @Test
    public void testGetSubTasksByMainTaskId_Success_ReturnsOk() {
        // Arrange
        List<TaskEntity> subTasks = Collections.singletonList(new TaskEntity());
        when(taskMgmtService.findSubTasksByMainTaskId(1L)).thenReturn(subTasks);

        // Act
        ResponseEntity<List<TaskEntity>> response = taskController.getSubTasksByMainTaskId(1);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    public void testGetSubTasksByMainTaskId_NullId_ReturnsBadRequest() {
        // Act
        ResponseEntity<List<TaskEntity>> response = taskController.getSubTasksByMainTaskId(null);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(taskMgmtService, never()).findSubTasksByMainTaskId(anyLong());
    }
}

