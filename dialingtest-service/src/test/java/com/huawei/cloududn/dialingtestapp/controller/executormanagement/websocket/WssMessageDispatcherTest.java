/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.DeRegisterRequestDto;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.RegisterRequestDto;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.ReportMsgDto;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.flow.InboundFileHandler;
import com.huawei.cloududn.dialingtestapp.service.executormanagement.ExecutorMgmtService;
import com.huawei.cloududn.dialingtestapp.service.executormanagement.auth.AuthSessionService;
import com.huawei.cloududn.dialingtestapp.service.executormanagement.task.TaskInterfaceService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.nio.ByteBuffer;

import javax.websocket.Session;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * WssMessageDispatcher单元测试
 * 测试JSON消息路由和二进制分片分发
 *
 * @author g00940940
 * @since 2025-11-16
 */
public class WssMessageDispatcherTest {
    private WssMessageDispatcher dispatcher;
    private ObjectMapper objectMapper;
    private InboundFileHandler inboundFileHandler;
    private AuthSessionService authSessionService;
    private ExecutorMgmtService executorMgmtService;
    private TaskInterfaceService taskInterfaceService;

    @Before
    public void setUp() {
        dispatcher = new WssMessageDispatcher();
        objectMapper = new ObjectMapper();
        inboundFileHandler = mock(InboundFileHandler.class);
        authSessionService = mock(AuthSessionService.class);
        executorMgmtService = mock(ExecutorMgmtService.class);
        taskInterfaceService = mock(TaskInterfaceService.class);

        setField(dispatcher, "objectMapper", objectMapper);
        setField(dispatcher, "inboundFileHandler", inboundFileHandler);
        setField(dispatcher, "authSessionService", authSessionService);
        setField(dispatcher, "executorMgmtService", executorMgmtService);
        setField(dispatcher, "taskInterfaceService", taskInterfaceService);
    }

    /**
     * 测试分发RegisterRequest消息
     */
    @Test
    public void testDispatch_JsonMessage_RegisterRequest() throws Exception {
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("session-001");

        String jsonMessage = "{\"type\":\"RegisterRequest\",\"payload\":{\"hostname\":\"agent-01\"}}";

        dispatcher.dispatch(jsonMessage, session);

        ArgumentCaptor<RegisterRequestDto> captor = ArgumentCaptor.forClass(RegisterRequestDto.class);
        verify(authSessionService).handleRegisterRequest(captor.capture(), eq(session));
        assertEquals("agent-01", captor.getValue().getHostname());
    }

    /**
     * 测试分发ReportMsg消息
     */
    @Test
    public void testDispatch_JsonMessage_ReportMsg() throws Exception {
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("session-001");

        String jsonMessage = "{\"type\":\"ReportMsg\",\"token\":12345,\"payload\":{\"state\":\"Normal\"}}";

        dispatcher.dispatch(jsonMessage, session);

        verify(executorMgmtService).handleReportMsg(any(ReportMsgDto.class), eq(session));
    }

    /**
     * 测试分发DeRegisterRequest消息
     */
    @Test
    public void testDispatch_JsonMessage_DeRegisterRequest() throws Exception {
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("session-001");

        String jsonMessage = "{\"type\":\"DeRegisterRequest\",\"token\":12345,\"payload\":{}}";

        dispatcher.dispatch(jsonMessage, session);

        verify(executorMgmtService).handleDeRegisterRequest(any(DeRegisterRequestDto.class), eq(session));
    }

    /**
     * 测试分发任务类消息（批量测试）
     */
    @Test
    public void testDispatch_JsonMessage_TaskMessages() throws Exception {
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("session-001");

        String appListResponse = "{\"type\":\"AppListResponse\",\"payload\":{\"serialNo\":\"123456\"}}";
        dispatcher.dispatch(appListResponse, session);
        verify(taskInterfaceService).handleAppListResponse(any(), eq(session));

        String scriptUpdateAck = "{\"type\":\"ScriptUpdateAck\",\"payload\":{\"scriptName\":\"test.py\"}}";
        dispatcher.dispatch(scriptUpdateAck, session);
        verify(taskInterfaceService).handleScriptUpdateAck(any(), eq(session));
    }

    /**
     * 测试未知消息类型
     */
    @Test
    public void testDispatch_JsonMessage_UnknownType() {
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("session-001");

        String jsonMessage = "{\"type\":\"UnknownMessageType\",\"payload\":{}}";

        dispatcher.dispatch(jsonMessage, session);

        verifyNoInteractions(authSessionService);
        verifyNoInteractions(executorMgmtService);
        verifyNoInteractions(taskInterfaceService);
    }

    /**
     * 测试无效JSON格式
     */
    @Test
    public void testDispatch_JsonMessage_InvalidJson() {
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("session-001");

        String invalidJson = "{invalid json}";

        dispatcher.dispatch(invalidJson, session);

        verifyNoInteractions(authSessionService);
        verifyNoInteractions(executorMgmtService);
        verifyNoInteractions(taskInterfaceService);
    }

    /**
     * 测试接收状态下处理二进制分片
     */
    @Test
    public void testDispatch_BinaryChunk_WithReceivingState() {
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("session-001");
        when(inboundFileHandler.isReceivingFile("session-001")).thenReturn(true);

        ByteBuffer buffer = ByteBuffer.allocate(100);
        buffer.put(new byte[100]);
        ((java.nio.Buffer) buffer).flip();

        dispatcher.dispatch(buffer, session);

        verify(inboundFileHandler).handleChunk("session-001", buffer);
    }

    /**
     * 测试非接收状态时收到二进制分片
     */
    @Test
    public void testDispatch_BinaryChunk_NoReceivingState() {
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("session-001");
        when(inboundFileHandler.isReceivingFile("session-001")).thenReturn(false);

        ByteBuffer buffer = ByteBuffer.allocate(100);
        buffer.put(new byte[100]);
        ((java.nio.Buffer) buffer).flip();

        dispatcher.dispatch(buffer, session);

        verify(inboundFileHandler, never()).handleChunk(anyString(), any(ByteBuffer.class));
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Failed to find field: " + fieldName, e);
        } catch (IllegalAccessException e) {
            throw new AssertionError("Failed to access field: " + fieldName, e);
        }
    }
}
