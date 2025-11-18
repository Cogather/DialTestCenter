/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.controller.executormanagement;

import com.huawei.cloududn.dialingtest.model.ExecutorPageResponse;
import com.huawei.cloududn.dialingtest.model.OperationResponse;
import com.huawei.cloududn.dialingtest.model.RefreshExecutorRequest;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.ExecutorWebsocketEndpoint;
import com.huawei.cloududn.dialingtestapp.dao.executormanagement.ExecutorDao;
import com.huawei.cloududn.dialingtestapp.service.executormanagement.SessionBindingRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * ExecutorController 单元测试 - V3协议版本
 * 测试OpenAPI生成的接口实现
 *
 * @author g00940940
 * @since 2025-11-11
 */
public class ExecutorControllerTest {

    @Mock
    private ExecutorDao executorDao;

    @Mock
    private SessionBindingRegistry registry;

    @Mock
    private ExecutorWebsocketEndpoint endpoint;

    @InjectMocks
    private ExecutorController controller;

    private AutoCloseable mocks;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    /**
     * 测试listExecutors：成功查询执行机列表
     */
    @Test
    public void testListExecutors_Success() {
        // Given
        List<com.huawei.cloududn.dialingtest.model.Executor> executors = new ArrayList<>();
        com.huawei.cloududn.dialingtest.model.Executor executor = new com.huawei.cloududn.dialingtest.model.Executor();
        executor.setName("executor1");
        executor.setIp("192.168.1.100");
        executor.setStatus(1);
        executors.add(executor);

        when(executorDao.findPage(any(), any(), eq(0), eq(20))).thenReturn(executors);
        when(executorDao.count(any(), any())).thenReturn(1);

        // When
        ResponseEntity<ExecutorPageResponse> response = controller.listExecutors(0, 20, null, null);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("OK", response.getBody().getMessage());
        assertNotNull(response.getBody().getData());
        assertEquals(1, response.getBody().getData().getTotalElements().intValue());
    }

    /**
     * 测试listExecutors：无效的分页参数（负数页码）
     */
    @Test
    public void testListExecutors_InvalidPageParameters() {
        // When
        ResponseEntity<ExecutorPageResponse> response = controller.listExecutors(-1, 20, null, null);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Invalid page parameters", response.getBody().getMessage());
    }

    /**
     * 测试listExecutors：无效的大小参数（为0）
     */
    @Test
    public void testListExecutors_InvalidSizeParameters() {
        // When
        ResponseEntity<ExecutorPageResponse> response = controller.listExecutors(0, 0, null, null);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
    }

    /**
     * 测试listExecutors：大小超过限制（超过200）
     */
    @Test
    public void testListExecutors_SizeExceedsLimit() {
        // When
        ResponseEntity<ExecutorPageResponse> response = controller.listExecutors(0, 201, null, null);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    /**
     * 测试refreshExecutor：成功处理刷新请求（在线执行机）
     */
    @Test
    public void testRefreshExecutor_Success() {
        // Given
        RefreshExecutorRequest request = new RefreshExecutorRequest();
        request.setName("executor1");

        com.huawei.cloududn.dialingtest.model.Executor executor =
            new com.huawei.cloududn.dialingtest.model.Executor();
        executor.setName("executor1");
        executor.setStatus(1); // 1=ONLINE

        when(executorDao.findByName("executor1")).thenReturn(executor);

        // When
        ResponseEntity<OperationResponse> response = controller.refreshExecutor("test-csrf-token", "admin", request);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Refresh request acknowledged"));
        assertTrue(response.getBody().getMessage().contains("heartbeat"));
    }

    /**
     * 测试refreshExecutor：执行机名称为空
     */
    @Test
    public void testRefreshExecutor_MissingExecutorName() {
        // Given
        RefreshExecutorRequest request = new RefreshExecutorRequest();
        request.setName(null);

        // When
        ResponseEntity<OperationResponse> response = controller.refreshExecutor("test-csrf-token", "admin", request);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Missing executor name", response.getBody().getMessage());
    }

    /**
     * 测试refreshExecutor：执行机名称为空字符串或只含空白字符
     */
    @Test
    public void testRefreshExecutor_EmptyExecutorName() {
        // Given
        RefreshExecutorRequest request = new RefreshExecutorRequest();
        request.setName("  ");

        // When
        ResponseEntity<OperationResponse> response = controller.refreshExecutor("test-csrf-token", "admin", request);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Missing"));
    }

    /**
     * 测试refreshExecutor：执行机不存在
     */
    @Test
    public void testRefreshExecutor_ExecutorNotFound() {
        // Given
        RefreshExecutorRequest request = new RefreshExecutorRequest();
        request.setName("nonexistent-executor");

        when(executorDao.findByName("nonexistent-executor")).thenReturn(null);

        // When
        ResponseEntity<OperationResponse> response = controller.refreshExecutor("test-csrf-token", "admin", request);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Executor not found"));
    }

    /**
     * 测试refreshExecutor：执行机离线状态
     */
    @Test
    public void testRefreshExecutor_ExecutorOffline() {
        // Given
        RefreshExecutorRequest request = new RefreshExecutorRequest();
        request.setName("offline-executor");

        com.huawei.cloududn.dialingtest.model.Executor executor =
            new com.huawei.cloududn.dialingtest.model.Executor();
        executor.setName("offline-executor");
        executor.setStatus(0); // 0=OFFLINE

        when(executorDao.findByName("offline-executor")).thenReturn(executor);

        // When
        ResponseEntity<OperationResponse> response = controller.refreshExecutor("test-csrf-token", "admin", request);

        // Then
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("not online"));
    }

    /**
     * 测试refreshExecutor：执行机状态为数字1（在线）
     */
    @Test
    public void testRefreshExecutor_ExecutorOnlineWithNumericStatus() {
        // Given
        RefreshExecutorRequest request = new RefreshExecutorRequest();
        request.setName("numeric-status-executor");

        com.huawei.cloududn.dialingtest.model.Executor executor =
            new com.huawei.cloududn.dialingtest.model.Executor();
        executor.setName("numeric-status-executor");
        executor.setStatus(1); // Numeric status 1 = ONLINE

        when(executorDao.findByName("numeric-status-executor")).thenReturn(executor);

        // When
        ResponseEntity<OperationResponse> response = controller.refreshExecutor("test-csrf-token", "admin", request);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Refresh request acknowledged"));
    }

    /**
     * 测试refreshExecutor：请求对象为null
     */
    @Test
    public void testRefreshExecutor_NullRequest() {
        // When
        ResponseEntity<OperationResponse> response = controller.refreshExecutor("test-csrf-token", "admin", null);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Missing"));
    }

    /**
     * 测试refreshExecutor：异常处理
     */
    @Test
    public void testRefreshExecutor_ExceptionHandling() {
        // Given
        RefreshExecutorRequest request = new RefreshExecutorRequest();
        request.setName("exception-executor");

        when(executorDao.findByName("exception-executor")).thenThrow(new RuntimeException("Database error"));

        // When
        ResponseEntity<OperationResponse> response = controller.refreshExecutor("test-csrf-token", "admin", request);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Internal server error"));
    }

    /**
     * 测试refreshExecutor：缺少用户名
     */
    @Test
    public void testRefreshExecutor_MissingUsername() {
        // Given
        RefreshExecutorRequest request = new RefreshExecutorRequest();
        request.setName("executor1");

        // When
        ResponseEntity<OperationResponse> response = controller.refreshExecutor("test-csrf-token", null, request);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Missing username"));
    }

    /**
     * 测试refreshExecutor：缺少CSRF令牌
     */
    @Test
    public void testRefreshExecutor_MissingCsrfToken() {
        // Given
        RefreshExecutorRequest request = new RefreshExecutorRequest();
        request.setName("executor1");

        // When
        ResponseEntity<OperationResponse> response = controller.refreshExecutor(null, "admin", request);

        // Then
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Missing CSRF token"));
    }

    /**
     * 测试分页参数的边界情况
     */
    @Test
    public void testListExecutors_BoundaryConditions() {
        // Test with null page (should default to 0)
        ResponseEntity<ExecutorPageResponse> response = controller.listExecutors(null, 20, null, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Test with null size (should default to 20)
        response = controller.listExecutors(0, null, null, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    /**
     * 测试执行机状态过滤
     */
    @Test
    public void testListExecutors_WithStatusFilter() {
        // Given
        List<com.huawei.cloududn.dialingtest.model.Executor> executors = new ArrayList<>();
        when(executorDao.findPage(eq(1), any(), eq(0), eq(20))).thenReturn(executors);
        when(executorDao.count(eq(1), any())).thenReturn(0);

        // When - filter by online status (1)
        ResponseEntity<ExecutorPageResponse> response = controller.listExecutors(0, 20, 1, null);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(executorDao).findPage(eq(1), any(), eq(0), eq(20));
        verify(executorDao).count(eq(1), any());
    }

    /**
     * 测试关键字搜索功能
     */
    @Test
    public void testListExecutors_WithKeywordFilter() {
        // Given
        List<com.huawei.cloududn.dialingtest.model.Executor> executors = new ArrayList<>();
        when(executorDao.findPage(any(), eq("test"), eq(0), eq(20))).thenReturn(executors);
        when(executorDao.count(any(), eq("test"))).thenReturn(0);

        // When - search with keyword
        ResponseEntity<ExecutorPageResponse> response = controller.listExecutors(0, 20, null, "test");

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(executorDao).findPage(any(), eq("test"), eq(0), eq(20));
        verify(executorDao).count(any(), eq("test"));
    }
}
