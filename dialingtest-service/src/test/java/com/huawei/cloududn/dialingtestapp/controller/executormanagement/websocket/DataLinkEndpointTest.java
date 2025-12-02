/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.JsonMessageEnvelope;
import com.huawei.cloududn.dialingtestapp.service.executormanagement.ExecutorMgmtService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.websocket.CloseReason;
import javax.websocket.Session;
import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * DataLinkEndpoint单元测试
 * 覆盖数据链路的认证绑定和二进制流分发逻辑
 *
 * @since 2025-12-02
 */
public class DataLinkEndpointTest {

    private DataLinkEndpoint endpoint;
    private WebSocketSessionRegistry sessionRegistry;
    private WssMessageDispatcher dispatcher;
    private DualLinkRouter dualLinkRouter;
    private ObjectMapper objectMapper;
    private ExecutorMgmtService executorMgmtService;
    private Session session;
    private Session controlSession;

    @Before
    public void setUp() {
        endpoint = new DataLinkEndpoint();
        
        sessionRegistry = mock(WebSocketSessionRegistry.class);
        dispatcher = mock(WssMessageDispatcher.class);
        dualLinkRouter = mock(DualLinkRouter.class);
        objectMapper = mock(ObjectMapper.class);
        executorMgmtService = mock(ExecutorMgmtService.class);
        session = mock(Session.class);
        controlSession = mock(Session.class);

        // 注入静态 Mock
        endpoint.setSessionRegistry(sessionRegistry);
        endpoint.setDispatcher(dispatcher);
        endpoint.setDualLinkRouter(dualLinkRouter);
        endpoint.setObjectMapper(objectMapper);
        endpoint.setExecutorMgmtService(executorMgmtService);

        when(session.getId()).thenReturn("data-001");
    }

    @After
    public void tearDown() {
        endpoint.setSessionRegistry(null);
        endpoint.setDispatcher(null);
        endpoint.setDualLinkRouter(null);
        endpoint.setObjectMapper(null);
        endpoint.setExecutorMgmtService(null);
    }

    /**
     * 测试连接建立
     */
    @Test
    public void testOnOpen_ShouldRegisterSession() {
        endpoint.onOpen(session);
        verify(sessionRegistry).addDataSession(session);
    }

    /**
     * 测试认证消息：成功绑定
     */
    @Test
    public void testOnAuthMessage_ValidToken_ShouldBind() throws Exception {
        String msg = "{\"token\":12345}";
        JsonMessageEnvelope envelope = new JsonMessageEnvelope();
        envelope.setToken(12345L);

        when(dualLinkRouter.isDataLinkBound("data-001")).thenReturn(false);
        when(objectMapper.readValue(msg, JsonMessageEnvelope.class)).thenReturn(envelope);
        when(dualLinkRouter.registerDataLink("12345", "data-001", session)).thenReturn(true);

        endpoint.onAuthMessage(msg, session);

        verify(dualLinkRouter).registerDataLink("12345", "data-001", session);
        verify(session, never()).close(any());
    }

    /**
     * 测试认证消息：Token无效导致关闭连接
     */
    @Test
    public void testOnAuthMessage_InvalidToken_ShouldClose() throws Exception {
        String msg = "{\"token\":12345}";
        JsonMessageEnvelope envelope = new JsonMessageEnvelope();
        envelope.setToken(12345L);

        when(dualLinkRouter.isDataLinkBound("data-001")).thenReturn(false);
        when(objectMapper.readValue(msg, JsonMessageEnvelope.class)).thenReturn(envelope);
        // 模拟绑定失败（Token不存在）
        when(dualLinkRouter.registerDataLink("12345", "data-001", session)).thenReturn(false);

        endpoint.onAuthMessage(msg, session);

        verify(session).close(any(CloseReason.class));
    }

    /**
     * 测试认证消息：重复绑定忽略
     */
    @Test
    public void testOnAuthMessage_AlreadyBound_ShouldIgnore() throws Exception {
        String msg = "{\"token\":12345}";
        when(dualLinkRouter.isDataLinkBound("data-001")).thenReturn(true);

        endpoint.onAuthMessage(msg, session);

        verify(objectMapper, never()).readValue(anyString(), eq(JsonMessageEnvelope.class));
    }

    /**
     * 测试二进制数据：已绑定状态正常分发
     */
    @Test
    public void testOnDataMessage_Bound_ShouldDispatch() {
        ByteBuffer buffer = ByteBuffer.allocate(10);
        buffer.put((byte)1);
        ((java.nio.Buffer) buffer).flip();

        when(dualLinkRouter.isDataLinkBound("data-001")).thenReturn(true);

        endpoint.onDataMessage(buffer, session);

        verify(dispatcher).dispatchData(buffer, session);
    }

    /**
     * 测试二进制数据：未绑定状态忽略
     */
    @Test
    public void testOnDataMessage_NotBound_ShouldIgnore() {
        ByteBuffer buffer = ByteBuffer.allocate(10);
        
        when(dualLinkRouter.isDataLinkBound("data-001")).thenReturn(false);

        endpoint.onDataMessage(buffer, session);

        verifyNoInteractions(dispatcher);
    }

    /**
     * 测试二进制数据：空包忽略
     */
    @Test
    public void testOnDataMessage_EmptyBuffer_ShouldIgnore() {
        endpoint.onDataMessage(null, session);
        endpoint.onDataMessage(ByteBuffer.allocate(0), session);

        verifyNoInteractions(dispatcher);
    }

    /**
     * 测试连接关闭：正常关闭与级联
     */
    @Test
    public void testOnClose_WithControlLink_ShouldCascadeClose() throws Exception {
        String token = "token-123";
        String controlId = "control-001";
        CloseReason reason = new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Bye");
        
        when(dualLinkRouter.getTokenByDataSessionId("data-001")).thenReturn(token);
        when(dualLinkRouter.getControlSessionByToken(token)).thenReturn(controlSession);
        when(controlSession.getId()).thenReturn(controlId);
        when(controlSession.isOpen()).thenReturn(true);
        
        // 模拟控制链路仍未注销，需要级联关闭
        when(dualLinkRouter.getTokenByControlSessionId(controlId)).thenReturn(token);

        endpoint.onClose(session, reason);

        verify(dualLinkRouter).unregisterDataLink("data-001");
        verify(sessionRegistry).removeDataSession("data-001");
        
        // 验证级联关闭逻辑
        verify(executorMgmtService).handleExecutorDisconnect(controlId);
        verify(controlSession).close(any(CloseReason.class));
    }
    
    /**
     * 测试错误处理
     */
    @Test
    public void testOnError_ShouldLog() {
        endpoint.onError(session, new RuntimeException("Socket error"));
    }
    
    /**
     * 测试getSession代理
     */
    @Test
    public void testGetSession_ShouldDelegateToRegistry() {
        when(sessionRegistry.getDataSession("data-001")).thenReturn(session);
        
        Session result = endpoint.getSession("data-001");
        
        assertEquals(session, result);
        verify(sessionRegistry).getDataSession("data-001");
    }
}

