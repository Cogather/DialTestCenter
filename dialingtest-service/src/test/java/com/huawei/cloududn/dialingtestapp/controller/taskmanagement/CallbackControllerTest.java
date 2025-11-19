/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.controller.taskmanagement;

import com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration.TaskOrchestratorService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 回调控制器测试类
 *
 * @author g00940940
 * @since 2025-11-13
 */
@RunWith(MockitoJUnitRunner.class)
public class CallbackControllerTest {
    @Mock
    private TaskOrchestratorService orchestratorService;
    @InjectMocks
    private CallbackController callbackController;

    @Test
    public void testNotifyCallback_ValidRequest_ReturnsOk() {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        request.put("mainTaskId", 1L);
        request.put("status", "SUCCESS");
        when(orchestratorService.sendResultEvent(eq(1L), eq(true))).thenReturn(true);

        // Act
        ResponseEntity<Object> response = callbackController.notifyCallback(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(orchestratorService).sendResultEvent(eq(1L), eq(true));
    }

    @Test
    public void testNotifyCallback_WithResultData_ReturnsOk() {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        request.put("main_task_id", Integer.valueOf(2));
        request.put("status", "FAILED");
        request.put("result_data", new HashMap<String, Object>());
        when(orchestratorService.sendResultEvent(eq(2L), eq(false), any(Map.class))).thenReturn(true);

        // Act
        ResponseEntity<Object> response = callbackController.notifyCallback(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(orchestratorService).sendResultEvent(eq(2L), eq(false), any(Map.class));
    }

    @Test
    public void testNotifyCallback_TaskNotFound_ReturnsNotFound() {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        request.put("mainTaskId", 999L);
        request.put("status", "SUCCESS");
        when(orchestratorService.sendResultEvent(eq(999L), eq(true))).thenReturn(false);

        // Act
        ResponseEntity<Object> response = callbackController.notifyCallback(request);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(orchestratorService).sendResultEvent(eq(999L), eq(true));
        
        // Verify response body contains error information
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("TASK_NOT_FOUND", body.get("errorCode"));
    }

    @Test
    public void testNotifyCallback_InvalidRequest_ReturnsBadRequest() {
        // Test null body
        assertEquals(HttpStatus.BAD_REQUEST, callbackController.notifyCallback(null).getStatusCode());
        
        // Test missing mainTaskId
        Map<String, Object> noId = new HashMap<>();
        noId.put("status", "SUCCESS");
        assertEquals(HttpStatus.BAD_REQUEST, callbackController.notifyCallback(noId).getStatusCode());
        
        // Test invalid status
        Map<String, Object> invalidStatus = new HashMap<>();
        invalidStatus.put("mainTaskId", 1L);
        invalidStatus.put("status", "INVALID");
        assertEquals(HttpStatus.BAD_REQUEST, callbackController.notifyCallback(invalidStatus).getStatusCode());
        
        verify(orchestratorService, never()).sendResultEvent(anyLong(), anyBoolean());
    }
}


