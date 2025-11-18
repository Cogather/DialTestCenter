/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket;

import com.huawei.cloududn.dialingtestapp.service.executormanagement.ExecutorMgmtService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.websocket.CloseReason;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * V4版本ExecutorWebsocketEndpoint测试
 * 测试JSON信令(Text)和二进制分片(Binary)混合模式
 *
 * @author g00940940
 * @since 2025-11-16
 */
public class ExecutorWebsocketEndpointTest {
    private ExecutorWebsocketEndpoint endpoint;
    private WebSocketSessionRegistry registry;
    private WssMessageDispatcher dispatcher;
    private ExecutorMgmtService execService;

    @Before
    public void setUp() {
        endpoint = new ExecutorWebsocketEndpoint();
        registry = Mockito.mock(WebSocketSessionRegistry.class);
        dispatcher = Mockito.mock(WssMessageDispatcher.class);
        execService = Mockito.mock(ExecutorMgmtService.class);
        setField(endpoint, "sessionRegistry", registry);
        setField(endpoint, "dispatcher", dispatcher);
        setField(endpoint, "executorMgmtService", execService);
    }

    /**
     * 测试连接建立成功
     */
    @Test
    public void testOnOpen_Success() {
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("session-001");

        endpoint.onOpen(session);

        verify(registry).addSession(session);
    }

    /**
     * 测试接收有效JSON消息
     */
    @Test
    public void testOnMessage_JsonMessage_Success() {
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("session-001");
        String jsonMessage = "{\"type\":\"RegisterRequest\",\"payload\":{\"hostname\":\"agent-01\"}}";

        endpoint.onMessage(jsonMessage, session);

        verify(dispatcher).dispatch(eq(jsonMessage), eq(session));
    }

    /**
     * 测试接收空或null JSON消息
     */
    @Test
    public void testOnMessage_JsonMessage_EmptyOrNull() {
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("session-001");

        endpoint.onMessage("", session);
        verify(dispatcher, never()).dispatch(anyString(), eq(session));

        endpoint.onMessage((String) null, session);
        verify(dispatcher, never()).dispatch(anyString(), eq(session));
    }

    /**
     * 测试接收有效Binary分片
     */
    @Test
    public void testOnMessage_BinaryChunk_Success() {
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("session-001");
        ByteBuffer buffer = ByteBuffer.allocate(100);
        buffer.put(new byte[100]);
        buffer.flip();

        endpoint.onMessage(buffer, session);

        verify(dispatcher).dispatch(eq(buffer), eq(session));
    }

    /**
     * 测试接收空或null Binary分片
     */
    @Test
    public void testOnMessage_BinaryChunk_EmptyOrNull() {
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("session-001");

        ByteBuffer emptyBuffer = ByteBuffer.allocate(0);
        endpoint.onMessage(emptyBuffer, session);
        verify(dispatcher, never()).dispatch(any(ByteBuffer.class), eq(session));

        endpoint.onMessage((ByteBuffer) null, session);
        verify(dispatcher, never()).dispatch(any(ByteBuffer.class), eq(session));
    }

    /**
     * 测试连接关闭
     */
    @Test
    public void testOnClose_Success() {
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("session-001");
        CloseReason reason = mock(CloseReason.class);
        when(reason.getReasonPhrase()).thenReturn("Normal closure");

        endpoint.onClose(session, reason);

        verify(registry).removeSession("session-001");
        verify(execService).handleExecutorDisconnect("session-001");
    }

    /**
     * 测试发送Text和Binary消息
     */
    @Test
    public void testSendText_AndSendBinary_Success() throws IOException {
        Session session = mock(Session.class);
        RemoteEndpoint.Basic basic = mock(RemoteEndpoint.Basic.class);
        when(session.getId()).thenReturn("session-001");
        when(session.isOpen()).thenReturn(true);
        when(session.getBasicRemote()).thenReturn(basic);
        when(registry.getSession("session-001")).thenReturn(session);

        String jsonMessage = "{\"type\":\"RegisterChallenge\"}";
        endpoint.sendText("session-001", jsonMessage);
        verify(basic).sendText(jsonMessage);

        ByteBuffer buffer = ByteBuffer.allocate(10);
        endpoint.sendBinary("session-001", buffer);
        verify(basic).sendBinary(buffer);
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
