/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.service.executormanagement.task;

import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.*;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.flow.InboundFileCompleteEvent;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.flow.InboundFileHandler;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.flow.InboundFileState;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.flow.WssMessageSender;
import com.huawei.cloududn.dialingtestapp.dao.basicDataManage.SoftwarePackageDao;
import com.huawei.cloududn.dialingtestapp.dao.basicDataManage.TestCaseSetDao;
import com.huawei.cloududn.dialingtestapp.entity.basicDataManage.SoftwarePackage;
import com.huawei.cloududn.dialingtest.model.TestCaseSet;
import com.huawei.cloududn.dialingtestapp.service.executormanagement.ExecutorSelectionService;
import com.huawei.cloududn.dialingtestapp.service.executormanagement.ExecutorSelectionService.ExecutorUeInfo;
import com.huawei.cloududn.dialingtestapp.service.executormanagement.SessionBindingRegistry;
import com.huawei.cloududn.dialingtestapp.service.executormanagement.dto.ScriptUpdateRequest;
import com.huawei.cloududn.dialingtestapp.service.executormanagement.dto.TaskDispatchRequest;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration.TaskOrchestratorService;

import com.huawei.cloududn.dialingtestapp.dao.executormanagement.ExecutorDao;
import com.huawei.cloududn.dialingtest.model.Executor;
import com.huawei.cloududn.dialingtest.model.Ue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.websocket.Session;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

/**
 * TaskInterfaceService单元测试 - 优化版本
 * 使用7个综合测试覆盖所有关键场景，包括未覆盖的方法
 *
 * @author g00940940
 * @since 2025-11-18
 */
public class TaskInterfaceServiceTest {

    @Mock
    private WssMessageSender wssMessageSender;

    @Mock
    private TaskOrchestratorService taskOrchestratorService;

    @Mock
    private SessionBindingRegistry sessionBindingRegistry;

    @Mock
    private InboundFileHandler inboundFileHandler;

    @Mock
    private TestCaseSetDao testCaseSetDao;

    @Mock
    private SoftwarePackageDao softwarePackageDao;

    @Mock
    private ExecutorSelectionService executorSelectionService;

    @Mock
    private ExecutorDao executorDao;

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
     * UT1: 综合测试任务分发场景
     * 覆盖: dispatchTask (自动选择执行机), dispatchTaskToAgent (成功/失败/边界情况)
     */
    @Test
    public void testTaskDispatch_AllScenarios() {
        String executorName = "executor-001";
        String sessionId = "session-001";

        TaskDispatchRequest nullRequest = null;
        try {
            taskInterfaceService.dispatchTask(nullRequest);
            fail("Should throw IllegalArgumentException for null request");
        } catch (IllegalArgumentException e) {
            // Expected exception - test passes
        }

        Executor executor = new Executor();
        executor.setName(executorName);
        Ue ue = new Ue();
        ue.setMsisdn("12345678901");
        ExecutorUeInfo selectedInfo = new ExecutorUeInfo(executor, ue);
        when(executorSelectionService.selectIdleExecutorAndUe()).thenReturn(selectedInfo);
        when(sessionBindingRegistry.getSessionId(executorName)).thenReturn(sessionId);

        TaskDispatchRequest autoSelectRequest = new TaskDispatchRequest();
        autoSelectRequest.setTaskId(100);
        autoSelectRequest.setScriptName("test-script");
        autoSelectRequest.setVersion("1.0");
        taskInterfaceService.dispatchTask(autoSelectRequest);
        verify(wssMessageSender, atLeast(1)).sendJsonMessage(eq(sessionId), any());

        when(executorSelectionService.selectIdleExecutorAndUe()).thenReturn(null);
        TaskDispatchRequest noExecutorRequest = new TaskDispatchRequest();
        noExecutorRequest.setTaskId(101);
        try {
            taskInterfaceService.dispatchTask(noExecutorRequest);
            fail("Should throw IllegalStateException when no executor available");
        } catch (IllegalStateException e) {
            // Expected exception - test passes
        }

        when(sessionBindingRegistry.getSessionId("executor-002")).thenReturn(null);
        TaskDispatchRequest noSessionRequest = new TaskDispatchRequest();
        noSessionRequest.setTaskId(102);
        noSessionRequest.setExecutorName("executor-002");
        noSessionRequest.setScriptName("test-script");
        noSessionRequest.setVersion("1.0");
        try {
            taskInterfaceService.dispatchTaskToAgent(noSessionRequest);
            fail("Should throw IllegalStateException when session not found");
        } catch (IllegalStateException e) {
            // Expected exception - test passes
        }

        when(sessionBindingRegistry.getSessionId("executor-003")).thenReturn("session-003");
        TaskDispatchRequest emptyUeRequest = new TaskDispatchRequest();
        emptyUeRequest.setTaskId(103);
        emptyUeRequest.setExecutorName("executor-003");
        emptyUeRequest.setScriptName("test-script");
        emptyUeRequest.setVersion("1.0");
        emptyUeRequest.setSerialNoList(java.util.Collections.emptyList());
        taskInterfaceService.dispatchTaskToAgent(emptyUeRequest);
        verify(wssMessageSender, atLeast(1)).sendJsonMessage(eq("session-003"), any());
    }

    /**
     * UT2: 综合测试任务生命周期管理
     * 覆盖: handleTaskStartResponse (成功/失败/文件传输), handleTaskStopRequest, handleTaskStopResponse
     */
    @Test
    public void testTaskLifecycle_AllScenarios() {
        Session session1 = mock(Session.class);
        when(session1.getId()).thenReturn("session-start-success");

        TaskStartResponseDto successResponse = new TaskStartResponseDto();
        successResponse.setTaskId(200);
        successResponse.setResult("SUCCESS");
        successResponse.setSubResult(new java.util.ArrayList<>());
        taskInterfaceService.handleTaskStartResponse(successResponse, session1);
        verify(taskOrchestratorService).sendResultEvent(eq(200L), eq(true), any());

        Session session2 = mock(Session.class);
        when(session2.getId()).thenReturn("session-start-failure");
        TaskStartResponseDto failureResponse = new TaskStartResponseDto();
        failureResponse.setTaskId(201);
        failureResponse.setResult("FAILED");
        failureResponse.setSubResult(new java.util.ArrayList<>());
        taskInterfaceService.handleTaskStartResponse(failureResponse, session2);
        verify(taskOrchestratorService).sendResultEvent(eq(201L), eq(false), any());

        Session session3 = mock(Session.class);
        when(session3.getId()).thenReturn("session-start-file");
        TaskStartResponseDto fileResponse = new TaskStartResponseDto();
        fileResponse.setTaskId(202);
        fileResponse.setResult("success");
        fileResponse.setFilelen(1024);
        fileResponse.setSubResult(new java.util.ArrayList<>());
        taskInterfaceService.handleTaskStartResponse(fileResponse, session3);
        verify(inboundFileHandler).startReceiving(eq("session-start-file"), eq(1024), any(), any(), any());

        String executorStop = "executor-stop";
        String sessionStop = "session-stop";
        when(sessionBindingRegistry.getSessionId(executorStop)).thenReturn(sessionStop);
        TaskDispatchRequest stopRequest = new TaskDispatchRequest();
        stopRequest.setTaskId(203);
        stopRequest.setExecutorName(executorStop);
        stopRequest.setScriptName("test-script");
        stopRequest.setVersion("1.0");
        stopRequest.setSerialNoList(Collections.singletonList("UE001"));
        taskInterfaceService.dispatchTaskToAgent(stopRequest);
        taskInterfaceService.handleTaskStopRequest(203);
        verify(wssMessageSender, atLeast(2)).sendJsonMessage(eq(sessionStop), any());

        taskInterfaceService.handleTaskStopRequest(999);
        verify(wssMessageSender, atLeast(2)).sendJsonMessage(anyString(), any());

        Session session4 = mock(Session.class);
        when(session4.getId()).thenReturn("session-stop-success");
        TaskStopResponseDto stopResponse = new TaskStopResponseDto();
        stopResponse.setTaskId(204);
        stopResponse.setState(0);
        taskInterfaceService.handleTaskStopResponse(stopResponse, session4);
        verify(taskOrchestratorService).stopTask(eq(204L));
    }

    /**
     * UT3: 综合测试文件传输场景 (新增覆盖handleFileComplete)
     * 覆盖: handleFileComplete (TaskStart/Screencap/Error), pushScriptToExecutor, pushAppToUe
     */
    @Test
    public void testFileTransfer_AllScenarios() {
        String executorName = "executor-file";
        String sessionId = "session-file";
        when(sessionBindingRegistry.getSessionId(executorName)).thenReturn(sessionId);

        InboundFileState successState = mock(InboundFileState.class);
        when(successState.getSessionId()).thenReturn(sessionId);
        when(successState.getTempFilePath()).thenReturn("/tmp/test.log");
        when(successState.getReceivedSize()).thenReturn(1024);
        when(successState.getExpectedSize()).thenReturn(1024);
        when(successState.hasError()).thenReturn(false);
        when(successState.verifyCrc()).thenReturn(true);
        TaskStartResponseDto taskDto = new TaskStartResponseDto();
        taskDto.setTaskId(300);
        taskDto.setResult("SUCCESS");
        taskDto.setSubResult(new java.util.ArrayList<>());
        when(successState.getBusinessContext()).thenReturn(taskDto);
        InboundFileCompleteEvent successEvent = new InboundFileCompleteEvent(this, successState);
        taskInterfaceService.handleFileComplete(successEvent);
        verify(taskOrchestratorService).sendResultEvent(eq(300L), eq(true), any());

        InboundFileState errorState = mock(InboundFileState.class);
        when(errorState.getSessionId()).thenReturn(sessionId);
        when(errorState.getTempFilePath()).thenReturn("/tmp/error.log");
        when(errorState.hasError()).thenReturn(true);
        when(errorState.getError()).thenReturn("Network error");
        InboundFileCompleteEvent errorEvent = new InboundFileCompleteEvent(this, errorState);
        taskInterfaceService.handleFileComplete(errorEvent);
        verify(taskOrchestratorService, times(1)).sendResultEvent(anyLong(), anyBoolean(), any());

        InboundFileState crcFailState = mock(InboundFileState.class);
        when(crcFailState.getSessionId()).thenReturn(sessionId);
        when(crcFailState.hasError()).thenReturn(false);
        when(crcFailState.verifyCrc()).thenReturn(false);
        InboundFileCompleteEvent crcFailEvent = new InboundFileCompleteEvent(this, crcFailState);
        taskInterfaceService.handleFileComplete(crcFailEvent);
        verify(taskOrchestratorService, times(1)).sendResultEvent(anyLong(), anyBoolean(), any());

        InboundFileState screencapState = mock(InboundFileState.class);
        when(screencapState.getSessionId()).thenReturn(sessionId);
        when(screencapState.getTempFilePath()).thenReturn("/tmp/screen.png");
        when(screencapState.hasError()).thenReturn(false);
        when(screencapState.verifyCrc()).thenReturn(true);
        ScreencapResponseDto screencapDto = new ScreencapResponseDto();
        screencapDto.setSerialNo("UE001");
        when(screencapState.getBusinessContext()).thenReturn(screencapDto);
        InboundFileCompleteEvent screencapEvent = new InboundFileCompleteEvent(this, screencapState);
        taskInterfaceService.handleFileComplete(screencapEvent);
        verify(taskOrchestratorService, times(1)).sendResultEvent(anyLong(), anyBoolean(), any());

        TestCaseSet testCaseSet = new TestCaseSet();
        testCaseSet.setName("test-case");
        testCaseSet.setVersion("v1.0");
        testCaseSet.setFileContent("script content".getBytes(StandardCharsets.UTF_8));
        when(testCaseSetDao.findByNameAndVersion("test-case", "v1.0")).thenReturn(testCaseSet);
        taskInterfaceService.pushScriptToExecutor(executorName, "test-case", "v1.0");
        verify(wssMessageSender, atLeastOnce()).sendFile(eq(sessionId), any(), any());

        when(testCaseSetDao.findByNameAndVersion("not-found", "v1.0")).thenReturn(null);
        taskInterfaceService.pushScriptToExecutor(executorName, "not-found", "v1.0");

        SoftwarePackage softwarePackage = new SoftwarePackage();
        softwarePackage.setSoftwareName("test-app");
        softwarePackage.setFileContent("app content".getBytes(StandardCharsets.UTF_8));
        softwarePackage.setFileSha256("abc123");
        when(softwarePackageDao.findBySoftwareName("test-app")).thenReturn(softwarePackage);
        taskInterfaceService.pushAppToUe(executorName, "UE001", "test-app", 301);
        verify(wssMessageSender, atLeast(2)).sendFile(anyString(), any(), any());

        when(softwarePackageDao.findBySoftwareName("not-found-app")).thenReturn(null);
        taskInterfaceService.pushAppToUe(executorName, "UE001", "not-found-app", 302);
    }

    /**
     * UT4: 综合测试应用列表查询场景
     * 覆盖: sendAppListQuery, handleAppListResponse
     */
    @Test
    public void testAppListQuery_AllScenarios() {
        String executorName = "executor-app";
        String sessionId = "session-app";
        when(sessionBindingRegistry.getSessionId(executorName)).thenReturn(sessionId);

        taskInterfaceService.sendAppListQuery(executorName, "UE001");
        verify(wssMessageSender, atLeast(1)).sendJsonMessage(eq(sessionId), any());

        when(sessionBindingRegistry.getSessionId("executor-no-session")).thenReturn(null);
        taskInterfaceService.sendAppListQuery("executor-no-session", "UE001");
        verify(wssMessageSender, atLeast(1)).sendJsonMessage(anyString(), any());

        Session session1 = mock(Session.class);
        when(session1.getId()).thenReturn("session-app-list");
        AppListResponseDto appListResponse = new AppListResponseDto();
        appListResponse.setSerialNo("UE001");
        appListResponse.setState(0);
        AppItemDto appItem = new AppItemDto();
        appItem.setPackageName("com.example.app");
        appItem.setName("Example App");
        appItem.setVersion("1.0");
        appListResponse.setAppList(Collections.singletonList(appItem));
        taskInterfaceService.handleAppListResponse(appListResponse, session1);

        Session session2 = mock(Session.class);
        when(session2.getId()).thenReturn("session-app-list-fail");
        AppListResponseDto failResponse = new AppListResponseDto();
        failResponse.setSerialNo("UE001");
        failResponse.setState(1);
        taskInterfaceService.handleAppListResponse(failResponse, session2);
    }

    /**
     * UT5: 综合测试应用安装响应场景
     * 覆盖: handleAppInstallResponse
     */
    @Test
    public void testAppInstallResponse_AllScenarios() {
        Session session1 = mock(Session.class);
        when(session1.getId()).thenReturn("session-app-install");
        AppInstallResponseDto installSuccess = new AppInstallResponseDto();
        installSuccess.setSerialNo("UE001");
        installSuccess.setState(0);
        taskInterfaceService.handleAppInstallResponse(installSuccess, session1);

        Session session2 = mock(Session.class);
        when(session2.getId()).thenReturn("session-app-install-fail");
        AppInstallResponseDto installFail = new AppInstallResponseDto();
        installFail.setSerialNo("UE001");
        installFail.setState(1);
        taskInterfaceService.handleAppInstallResponse(installFail, session2);
    }

    /**
     * UT6: 综合测试脚本更新场景
     * 覆盖: sendScriptUpdate, handleScriptUpdateAck
     */
    @Test
    public void testScriptUpdate_AllScenarios() {
        String executorName = "executor-script";
        String sessionId = "session-script";
        when(sessionBindingRegistry.getSessionId(executorName)).thenReturn(sessionId);

        ScriptUpdateRequest updateRequest = new ScriptUpdateRequest();
        updateRequest.setScriptName("test-script");
        updateRequest.setVersion("v2.0");
        updateRequest.setScriptFile("script content".getBytes(StandardCharsets.UTF_8));
        updateRequest.setCrc("abc123");
        taskInterfaceService.sendScriptUpdate(executorName, updateRequest);
        verify(wssMessageSender, atLeast(1)).sendFile(eq(sessionId), any(), any());

        Session session1 = mock(Session.class);
        when(session1.getId()).thenReturn("session-script-ack");
        ScriptUpdateAckDto ackSuccess = new ScriptUpdateAckDto();
        ackSuccess.setScriptName("test-script");
        ackSuccess.setVersion("v2.0");
        ackSuccess.setState(0);
        taskInterfaceService.handleScriptUpdateAck(ackSuccess, session1);

        Session session2 = mock(Session.class);
        when(session2.getId()).thenReturn("session-script-ack-fail");
        ScriptUpdateAckDto ackFail = new ScriptUpdateAckDto();
        ackFail.setScriptName("test-script");
        ackFail.setVersion("v2.0");
        ackFail.setState(1);
        taskInterfaceService.handleScriptUpdateAck(ackFail, session2);
    }

    /**
     * UT7: 综合测试截屏功能场景 (新增覆盖handleScreencapResponse)
     * 覆盖: sendScreencapQuery, handleScreencapResponse
     */
    @Test
    public void testScreencap_AllScenarios() {
        String executorName = "executor-screencap";
        String sessionId = "session-screencap";
        when(sessionBindingRegistry.getSessionId(executorName)).thenReturn(sessionId);

        taskInterfaceService.sendScreanCapQuery(executorName, "UE001");
        verify(wssMessageSender, atLeast(1)).sendJsonMessage(eq(sessionId), any());

        when(sessionBindingRegistry.getSessionId("executor-no-session")).thenReturn(null);
        taskInterfaceService.sendScreanCapQuery("executor-no-session", "UE001");
        verify(wssMessageSender, atLeast(1)).sendJsonMessage(anyString(), any());

        Session session1 = mock(Session.class);
        when(session1.getId()).thenReturn("session-screencap-response");
        ScreencapResponseDto screencapResponse = new ScreencapResponseDto();
        screencapResponse.setSerialNo("UE001");
        screencapResponse.setFilelen(2048);
        taskInterfaceService.handleScreencapResponse(screencapResponse, session1);
        verify(inboundFileHandler).startReceiving(eq("session-screencap-response"), eq(2048), any(), any(), any());

        Session session2 = mock(Session.class);
        when(session2.getId()).thenReturn("session-screencap-no-file");
        ScreencapResponseDto noFileResponse = new ScreencapResponseDto();
        noFileResponse.setSerialNo("UE001");
        noFileResponse.setFilelen(0);
        taskInterfaceService.handleScreencapResponse(noFileResponse, session2);

        Session session3 = mock(Session.class);
        when(session3.getId()).thenReturn("session-screencap-null-file");
        ScreencapResponseDto nullFileResponse = new ScreencapResponseDto();
        nullFileResponse.setSerialNo("UE001");
        nullFileResponse.setFilelen(null);
        taskInterfaceService.handleScreencapResponse(nullFileResponse, session3);
    }
}
