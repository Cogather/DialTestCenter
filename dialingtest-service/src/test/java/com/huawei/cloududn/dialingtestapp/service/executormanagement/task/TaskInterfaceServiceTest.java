/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.service.executormanagement.task;

import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.AppInstallResponseDto;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.AppItemDto;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.AppListResponseDto;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.ScriptUpdateAckDto;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.TaskStartResponseDto;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.TaskStopResponseDto;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.flow.InboundFileHandler;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.flow.WssMessageSender;
import com.huawei.cloududn.dialingtestapp.dao.SoftwarePackageDao;
import com.huawei.cloududn.dialingtestapp.dao.TestCaseSetDao;
import com.huawei.cloududn.dialingtestapp.dao.taskmanagement.TaskExecutorMappingDao;
import com.huawei.cloududn.dialingtestapp.entity.SoftwarePackage;
import com.huawei.cloududn.dialingtest.model.TestCaseSet;
import com.huawei.cloududn.dialingtestapp.service.executormanagement.SessionBindingRegistry;
import com.huawei.cloududn.dialingtestapp.service.executormanagement.dto.TaskDispatchRequest;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration.TaskOrchestratorService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.websocket.Session;

import static org.mockito.Mockito.*;

/**
 * TaskInterfaceService单元测试 - V4协议版本
 * 测试任务下发、停止、结果上报等功能
 *
 * @author g00940940
 * @since 2025-11-16
 */
public class TaskInterfaceServiceTest {

    @Mock
    private WssMessageSender wssMessageSender;

    @Mock
    private TaskOrchestratorService taskOrchestratorService;

    @Mock
    private TaskExecutorMappingDao taskExecutorMappingDao;

    @Mock
    private SessionBindingRegistry sessionBindingRegistry;

    @Mock
    private InboundFileHandler inboundFileHandler;

    @Mock
    private TestCaseSetDao testCaseSetDao;

    @Mock
    private SoftwarePackageDao softwarePackageDao;

    @InjectMocks
    private TaskInterfaceService taskInterfaceService;

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
     * 测试handleTaskStopRequest：发送任务停止请求
     */
    @Test
    public void testHandleTaskStopRequest_Success() {
        // Given
        Integer taskId = 123;
        String executorName = "executor-001";
        String sessionId = "session-001";

        when(sessionBindingRegistry.getSessionId(executorName)).thenReturn(sessionId);

        TaskDispatchRequest request = new TaskDispatchRequest();
        request.setTaskId(taskId);
        request.setExecutorName(executorName);
        request.setScriptName("test-script");
        request.setVersion("1.0");
        request.setSerialNoList(java.util.Arrays.asList("UE001"));
        taskInterfaceService.dispatchTaskToAgent(request);

        // When
        taskInterfaceService.handleTaskStopRequest(taskId);

        // Then
        verify(wssMessageSender, atLeast(2)).sendJsonMessage(eq(sessionId), any());
    }

    /**
     * 测试handleTaskStopRequest：任务不存在
     */
    @Test
    public void testHandleTaskStopRequest_TaskNotFound() {
        // Given
        Integer taskId = 999;

        // When
        taskInterfaceService.handleTaskStopRequest(taskId);

        // Then
        verify(wssMessageSender, never()).sendJsonMessage(anyString(), any());
    }

    /**
     * 测试handleTaskStartResponse：任务启动成功
     */
    @Test
    public void testHandleTaskStartResponse_Success() {
        // Given
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("session-002");

        TaskStartResponseDto responseDto = new TaskStartResponseDto();
        responseDto.setTaskId(100);
        responseDto.setResult("SUCCESS");
        responseDto.setSubResult(new java.util.ArrayList<>());

        // When
        taskInterfaceService.handleTaskStartResponse(responseDto, session);

        // Then
        verify(taskOrchestratorService).sendResultEvent(eq(100L), eq(true), any());
    }

    /**
     * 测试handleTaskStartResponse：任务启动失败
     */
    @Test
    public void testHandleTaskStartResponse_Failure() {
        // Given
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("session-003");

        TaskStartResponseDto responseDto = new TaskStartResponseDto();
        responseDto.setTaskId(101);
        responseDto.setResult("FAILED");
        responseDto.setSubResult(new java.util.ArrayList<>());

        // When
        taskInterfaceService.handleTaskStartResponse(responseDto, session);

        // Then
        verify(taskOrchestratorService).sendResultEvent(eq(101L), eq(false), any());
    }

    /**
     * 测试handleTaskStopResponse：任务停止成功
     */
    @Test
    public void testHandleTaskStopResponse_Success() {
        // Given
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("session-004");

        TaskStopResponseDto responseDto = new TaskStopResponseDto();
        responseDto.setTaskId(102);
        responseDto.setState(0);

        // When
        taskInterfaceService.handleTaskStopResponse(responseDto, session);

        // Then
        verify(taskOrchestratorService).stopTask(eq(102L));
    }

    /**
     * 测试handleTaskStopResponse：任务停止失败
     */
    @Test
    public void testHandleTaskStopResponse_Failure() {
        // Given
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("session-005");

        TaskStopResponseDto responseDto = new TaskStopResponseDto();
        responseDto.setTaskId(103);
        responseDto.setState(1);

        // When
        taskInterfaceService.handleTaskStopResponse(responseDto, session);

        // Then
        verify(taskOrchestratorService).stopTask(eq(103L));
    }

    /**
     * 测试dispatchTaskToAgent：成功下发任务
     */
    @Test
    public void testDispatchTaskToAgent_Success() {
        // Given
        String executorName = "executor-002";
        String sessionId = "session-006";

        when(sessionBindingRegistry.getSessionId(executorName)).thenReturn(sessionId);

        TaskDispatchRequest request = new TaskDispatchRequest();
        request.setTaskId(104);
        request.setExecutorName(executorName);
        request.setScriptName("test-script");
        request.setVersion("1.0");
        request.setSerialNoList(java.util.Arrays.asList("UE001", "UE002"));

        // When
        taskInterfaceService.dispatchTaskToAgent(request);

        // Then
        verify(wssMessageSender).sendJsonMessage(eq(sessionId), any());
    }

    /**
     * 测试dispatchTaskToAgent：会话不存在
     */
    @Test(expected = RuntimeException.class)
    public void testDispatchTaskToAgent_SessionNotFound() {
        // Given
        String executorName = "executor-003";

        when(sessionBindingRegistry.getSessionId(executorName)).thenReturn(null);

        TaskDispatchRequest request = new TaskDispatchRequest();
        request.setTaskId(105);
        request.setExecutorName(executorName);
        request.setScriptName("test-script");
        request.setVersion("1.0");

        // When & Then - 期望抛出 RuntimeException（因为执行机未连接）
        taskInterfaceService.dispatchTaskToAgent(request);
    }

    /**
     * 测试dispatchTaskToAgent：空UE列表
     */
    @Test
    public void testDispatchTaskToAgent_EmptyUeList() {
        // Given
        String executorName = "executor-004";
        String sessionId = "session-007";

        when(sessionBindingRegistry.getSessionId(executorName)).thenReturn(sessionId);

        TaskDispatchRequest request = new TaskDispatchRequest();
        request.setTaskId(106);
        request.setExecutorName(executorName);
        request.setScriptName("test-script");
        request.setVersion("1.0");
        request.setSerialNoList(java.util.Collections.emptyList());

        // When
        taskInterfaceService.dispatchTaskToAgent(request);

        // Then
        verify(wssMessageSender).sendJsonMessage(eq(sessionId), any());
    }

    /**
     * 测试dispatchTaskToAgent：null UE列表
     */
    @Test
    public void testDispatchTaskToAgent_NullUeList() {
        // Given
        String executorName = "executor-005";
        String sessionId = "session-008";

        when(sessionBindingRegistry.getSessionId(executorName)).thenReturn(sessionId);

        TaskDispatchRequest request = new TaskDispatchRequest();
        request.setTaskId(107);
        request.setExecutorName(executorName);
        request.setScriptName("test-script");
        request.setVersion("1.0");
        request.setSerialNoList(null);

        // When
        taskInterfaceService.dispatchTaskToAgent(request);

        // Then
        verify(wssMessageSender).sendJsonMessage(eq(sessionId), any());
    }

    /**
     * 测试sendAppListQuery：成功发送查询
     */
    @Test
    public void testSendAppListQuery_Success() {
        // Given
        String executorName = "executor-006";
        String sessionId = "session-009";
        String serialNo = "UE001";

        when(sessionBindingRegistry.getSessionId(executorName)).thenReturn(sessionId);

        // When
        taskInterfaceService.sendAppListQuery(executorName, serialNo);

        // Then
        verify(wssMessageSender).sendJsonMessage(eq(sessionId), any());
    }

    /**
     * 测试sendAppListQuery：会话不存在
     */
    @Test
    public void testSendAppListQuery_SessionNotFound() {
        // Given
        String executorName = "executor-007";
        String serialNo = "UE002";

        when(sessionBindingRegistry.getSessionId(executorName)).thenReturn(null);

        // When
        taskInterfaceService.sendAppListQuery(executorName, serialNo);

        // Then
        verify(wssMessageSender, never()).sendJsonMessage(anyString(), any());
    }

    /**
     * 测试sendScreanCapQuery：成功发送截屏请求
     */
    @Test
    public void testSendScreanCapQuery_Success() {
        // Given
        String executorName = "executor-008";
        String sessionId = "session-010";
        String serialNo = "UE003";

        when(sessionBindingRegistry.getSessionId(executorName)).thenReturn(sessionId);

        // When
        taskInterfaceService.sendScreanCapQuery(executorName, serialNo);

        // Then
        verify(wssMessageSender).sendJsonMessage(eq(sessionId), any());
    }

    /**
     * 测试sendScreanCapQuery：会话不存在
     */
    @Test
    public void testSendScreanCapQuery_SessionNotFound() {
        // Given
        String executorName = "executor-009";
        String serialNo = "UE004";

        when(sessionBindingRegistry.getSessionId(executorName)).thenReturn(null);

        // When
        taskInterfaceService.sendScreanCapQuery(executorName, serialNo);

        // Then
        verify(wssMessageSender, never()).sendJsonMessage(anyString(), any());
    }

    /**
     * 测试handleAppInstallResponse：安装成功
     */
    @Test
    public void testHandleAppInstallResponse_Success() {
        // Given
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("session-011");

        AppInstallResponseDto responseDto = new AppInstallResponseDto();
        responseDto.setSerialNo("UE005");
        responseDto.setTaskId(201);
        responseDto.setState(0); // Success

        // When
        taskInterfaceService.handleAppInstallResponse(responseDto, session);

        // Then - should log success (no exceptions)
    }

    /**
     * 测试handleAppInstallResponse：安装失败
     */
    @Test
    public void testHandleAppInstallResponse_Failure() {
        // Given
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("session-012");

        AppInstallResponseDto responseDto = new AppInstallResponseDto();
        responseDto.setSerialNo("UE006");
        responseDto.setTaskId(202);
        responseDto.setState(1); // Failure

        // When
        taskInterfaceService.handleAppInstallResponse(responseDto, session);

        // Then - should log failure (no exceptions)
    }

    /**
     * 测试handleAppListResponse：查询成功
     */
    @Test
    public void testHandleAppListResponse_Success() {
        // Given
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("session-013");

        AppListResponseDto responseDto = new AppListResponseDto();
        responseDto.setSerialNo("UE007");
        responseDto.setState(0); // Success
        
        // Create AppItemDto list
        java.util.List<AppItemDto> appList = new java.util.ArrayList<>();
        AppItemDto app1 = new AppItemDto();
        app1.setPackageName("com.test.app1");
        app1.setName("App1");
        appList.add(app1);
        
        responseDto.setAppList(appList);

        // When
        taskInterfaceService.handleAppListResponse(responseDto, session);

        // Then - should log success (no exceptions)
    }

    /**
     * 测试handleAppListResponse：查询失败
     */
    @Test
    public void testHandleAppListResponse_Failure() {
        // Given
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("session-014");

        AppListResponseDto responseDto = new AppListResponseDto();
        responseDto.setSerialNo("UE008");
        responseDto.setState(1); // Failure

        // When
        taskInterfaceService.handleAppListResponse(responseDto, session);

        // Then - should log failure (no exceptions)
    }

    /**
     * 测试handleScriptUpdateAck：更新成功
     */
    @Test
    public void testHandleScriptUpdateAck_Success() {
        // Given
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("session-015");

        ScriptUpdateAckDto ackDto = new ScriptUpdateAckDto();
        ackDto.setScriptName("test-script");
        ackDto.setVersion("1.0");
        ackDto.setState(0); // Success

        // When
        taskInterfaceService.handleScriptUpdateAck(ackDto, session);

        // Then - should log success (no exceptions)
    }

    /**
     * 测试handleScriptUpdateAck：更新失败
     */
    @Test
    public void testHandleScriptUpdateAck_Failure() {
        // Given
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("session-016");

        ScriptUpdateAckDto ackDto = new ScriptUpdateAckDto();
        ackDto.setScriptName("test-script");
        ackDto.setVersion("1.0");
        ackDto.setState(1); // Failure

        // When
        taskInterfaceService.handleScriptUpdateAck(ackDto, session);

        // Then - should log failure (no exceptions)
    }

    /**
     * 测试handleTaskStartResponse：带有文件长度
     */
    @Test
    public void testHandleTaskStartResponse_WithFileLength() {
        // Given
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("session-017");

        TaskStartResponseDto responseDto = new TaskStartResponseDto();
        responseDto.setTaskId(301);
        responseDto.setResult("SUCCESS");
        responseDto.setFileLen(1024); // File present
        responseDto.setCrc("abcd1234");
        responseDto.setSubResult(new java.util.ArrayList<>());

        // When
        taskInterfaceService.handleTaskStartResponse(responseDto, session);

        // Then - should start receiving file (no exceptions)
    }

    /**
     * 测试handleTaskStopRequest：无执行机映射
     */
    @Test
    public void testHandleTaskStopRequest_NoExecutorMapping() {
        // Given
        Integer taskId = 401;

        // When - taskId has no executor mapping
        taskInterfaceService.handleTaskStopRequest(taskId);

        // Then - should not send stop request
        verify(wssMessageSender, never()).sendJsonMessage(anyString(), any());
    }

    /**
     * 测试handleTaskStopRequest：有映射但无会话
     */
    @Test
    public void testHandleTaskStopRequest_HasMappingNoSession() {
        // Given
        Integer taskId = 402;
        String executorName = "executor-010";

        when(sessionBindingRegistry.getSessionId(executorName)).thenReturn(null);

        TaskDispatchRequest request = new TaskDispatchRequest();
        request.setTaskId(taskId);
        request.setExecutorName(executorName);
        request.setScriptName("test-script");
        request.setVersion("1.0");
        request.setSerialNoList(java.util.Arrays.asList("UE001"));

        when(sessionBindingRegistry.getSessionId(executorName)).thenReturn("session-temp");
        taskInterfaceService.dispatchTaskToAgent(request);
        when(sessionBindingRegistry.getSessionId(executorName)).thenReturn(null);

        // When
        taskInterfaceService.handleTaskStopRequest(taskId);

        // Then - should not send stop request when session not found
    }

    /**
     * 测试handleTaskStartResponse：结果为小写success
     */
    @Test
    public void testHandleTaskStartResponse_LowercaseSuccess() {
        // Given
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("session-018");

        TaskStartResponseDto responseDto = new TaskStartResponseDto();
        responseDto.setTaskId(501);
        responseDto.setResult("success"); // lowercase
        responseDto.setSubResult(new java.util.ArrayList<>());

        // When
        taskInterfaceService.handleTaskStartResponse(responseDto, session);

        // Then
        verify(taskOrchestratorService).sendResultEvent(eq(501L), eq(true), any());
    }

    /**
     * 测试pushScriptToExecutor：成功推送脚本
     */
    @Test
    public void testPushScriptToExecutor_Success() {
        // Given
        String executorName = "executor-script-001";
        String scriptName = "test-script";
        String version = "1.0";
        byte[] fileContent = "script content".getBytes();
        String sha256 = "abc123";

        TestCaseSet testCaseSet = new TestCaseSet();
        testCaseSet.setName(scriptName);
        testCaseSet.setVersion(version);
        testCaseSet.setFileContent(fileContent);
        testCaseSet.setSha256(sha256);

        when(testCaseSetDao.findByNameAndVersion(scriptName, version)).thenReturn(testCaseSet);
        when(sessionBindingRegistry.getSessionId(executorName)).thenReturn("session-script-001");

        // When
        taskInterfaceService.pushScriptToExecutor(executorName, scriptName, version);

        // Then
        verify(testCaseSetDao).findByNameAndVersion(scriptName, version);
        verify(wssMessageSender).sendFile(eq("session-script-001"), any(), any());
    }

    /**
     * 测试pushScriptToExecutor：脚本不存在
     */
    @Test
    public void testPushScriptToExecutor_ScriptNotFound() {
        // Given
        String executorName = "executor-script-002";
        String scriptName = "nonexistent-script";
        String version = "1.0";

        when(testCaseSetDao.findByNameAndVersion(scriptName, version)).thenReturn(null);

        // When
        taskInterfaceService.pushScriptToExecutor(executorName, scriptName, version);

        // Then
        verify(testCaseSetDao).findByNameAndVersion(scriptName, version);
        verify(wssMessageSender, never()).sendJsonMessage(anyString(), any());
    }

    /**
     * 测试pushScriptToExecutor：文件内容为空、会话不存在、异常等场景
     */
    @Test
    public void testPushScriptToExecutor_EdgeCases() {
        // Test 1: Empty file content
        String executorName1 = "executor-script-003";
        String scriptName1 = "empty-script";
        String version1 = "1.0";

        TestCaseSet emptySet = new TestCaseSet();
        emptySet.setName(scriptName1);
        emptySet.setVersion(version1);
        emptySet.setFileContent(new byte[0]); // Empty

        when(testCaseSetDao.findByNameAndVersion(scriptName1, version1)).thenReturn(emptySet);

        taskInterfaceService.pushScriptToExecutor(executorName1, scriptName1, version1);
        verify(wssMessageSender, never()).sendJsonMessage(anyString(), any());

        // Test 2: Exception during processing
        String executorName2 = "executor-script-004";
        String scriptName2 = "error-script";
        String version2 = "1.0";

        when(testCaseSetDao.findByNameAndVersion(scriptName2, version2))
                .thenThrow(new RuntimeException("Database error"));

        taskInterfaceService.pushScriptToExecutor(executorName2, scriptName2, version2);
        // Should handle exception gracefully
    }

    /**
     * 测试pushAppToUe：成功推送应用
     */
    @Test
    public void testPushAppToUe_Success() {
        // Given
        String executorName = "executor-app-001";
        String serialNo = "UE-APP-001";
        String appName = "test-app";
        Integer taskId = 123;
        byte[] fileContent = "app content".getBytes();
        String sha256 = "def456";

        SoftwarePackage softwarePackage = new SoftwarePackage();
        softwarePackage.setSoftwareName(appName);
        softwarePackage.setFileContent(fileContent);
        softwarePackage.setFileSha256(sha256);

        when(softwarePackageDao.findBySoftwareName(appName)).thenReturn(softwarePackage);
        when(sessionBindingRegistry.getSessionId(executorName)).thenReturn("session-app-001");

        // When
        taskInterfaceService.pushAppToUe(executorName, serialNo, appName, taskId);

        // Then
        verify(softwarePackageDao).findBySoftwareName(appName);
        verify(wssMessageSender).sendFile(eq("session-app-001"), any(), any());
    }

    /**
     * 测试pushAppToUe：应用包不存在、文件为空、会话不存在等场景
     */
    @Test
    public void testPushAppToUe_EdgeCases() {
        // Test 1: Software package not found
        String executorName1 = "executor-app-002";
        String appName1 = "nonexistent-app";
        when(softwarePackageDao.findBySoftwareName(appName1)).thenReturn(null);
        
        taskInterfaceService.pushAppToUe(executorName1, "UE001", appName1, 100);
        verify(wssMessageSender, never()).sendFile(anyString(), any(), any());

        // Test 2: Empty file content
        String executorName2 = "executor-app-003";
        String appName2 = "empty-app";
        SoftwarePackage emptyPackage = new SoftwarePackage();
        emptyPackage.setSoftwareName(appName2);
        emptyPackage.setFileContent(null); // null content
        
        when(softwarePackageDao.findBySoftwareName(appName2)).thenReturn(emptyPackage);
        taskInterfaceService.pushAppToUe(executorName2, "UE002", appName2, 101);
        verify(wssMessageSender, never()).sendFile(anyString(), any(), any());

        // Test 3: Session not found
        String executorName3 = "executor-app-004";
        String appName3 = "valid-app";
        SoftwarePackage validPackage = new SoftwarePackage();
        validPackage.setSoftwareName(appName3);
        validPackage.setFileContent("valid content".getBytes());
        
        when(softwarePackageDao.findBySoftwareName(appName3)).thenReturn(validPackage);
        when(sessionBindingRegistry.getSessionId(executorName3)).thenReturn(null);
        
        taskInterfaceService.pushAppToUe(executorName3, "UE003", appName3, 102);
        verify(wssMessageSender, never()).sendFile(anyString(), any(), any());

        // Test 4: Exception during processing
        String executorName4 = "executor-app-005";
        String appName4 = "error-app";
        when(softwarePackageDao.findBySoftwareName(appName4))
                .thenThrow(new RuntimeException("Database error"));
        
        taskInterfaceService.pushAppToUe(executorName4, "UE004", appName4, 103);
        // Should handle exception gracefully
    }
}
