/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket;

import com.huawei.cloududn.dialingtestapp.service.executormanagement.ExecutorMgmtService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.websocket.CloseReason;
import javax.websocket.Session;

import static org.mockito.Mockito.*;

/**
 * ControlLinkEndpoint单元测试
 * 重点测试静态依赖注入的WebSocket端点生命周期
 *
 * @since 2025-12-02
 */
public class ControlLinkEndpointTest {

    private ControlLinkEndpoint endpoint;
    private WebSocketSessionRegistry sessionRegistry;
    private WssMessageDispatcher dispatcher;
    private DualLinkRouter dualLinkRouter;
    private ExecutorMgmtService executorMgmtService;
    private Session session;
    private Session dataSession;

    @Before
    public void setUp() {
        endpoint = new ControlLinkEndpoint();
        
        sessionRegistry = mock(WebSocketSessionRegistry.class);
        dispatcher = mock(WssMessageDispatcher.class);
        dualLinkRouter = mock(DualLinkRouter.class);
        executorMgmtService = mock(ExecutorMgmtService.class);
        session = mock(Session.class);
        dataSession = mock(Session.class);

        // 关键步骤：通过Setter注入静态Mock
        endpoint.setSessionRegistry(sessionRegistry);
        endpoint.setDispatcher(dispatcher);
        endpoint.setDualLinkRouter(dualLinkRouter);
        endpoint.setExecutorMgmtService(executorMgmtService);

        when(session.getId()).thenReturn("ws-001");
    }

    @After
    public void tearDown() {
        // 关键步骤：清理静态变量，防止污染其他测试
        endpoint.setSessionRegistry(null);
        endpoint.setDispatcher(null);
        endpoint.setDualLinkRouter(null);
        endpoint.setExecutorMgmtService(null);
    }

    /**
     * 测试连接建立：注册会话
     */
    @Test
    public void testOnOpen_ShouldRegisterSession() {
        endpoint.onOpen(session);

        verify(sessionRegistry).addControlSession(session);
        verify(dualLinkRouter).registerControlLink("ws-001", session);
    }

    /**
     * 测试消息接收：正常分发
     */
    @Test
    public void testOnMessage_ValidMessage_ShouldDispatch() {
        String msg = "{\"type\":\"RegisterRequest\"}";
        endpoint.onMessage(msg, session);

        verify(dispatcher).dispatchControl(msg, session);
    }

    /**
     * 测试消息接收：空消息忽略
     */
    @Test
    public void testOnMessage_EmptyMessage_ShouldIgnore() {
        endpoint.onMessage("", session);
        endpoint.onMessage(null, session);

        verifyNoInteractions(dispatcher);
    }

    /**
     * 测试消息接收：异常处理
     */
    @Test
    public void testOnMessage_Exception_ShouldLogButNotThrow() {
        String msg = "{\"type\":\"Error\"}";
        doThrow(new RuntimeException("Dispatch error")).when(dispatcher).dispatchControl(msg, session);

        endpoint.onMessage(msg, session);
        
        // 验证方法没有抛出异常
        verify(dispatcher).dispatchControl(msg, session);
    }

    /**
     * 测试连接关闭：资源清理与级联关闭
     */
    @Test
    public void testOnClose_WithDataLink_ShouldCascadeClose() throws Exception {
        CloseReason reason = new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Bye");
        String token = "token-123";
        
        when(dualLinkRouter.getTokenBySessionId("ws-001")).thenReturn(token);
        when(dualLinkRouter.getDataSessionByToken(token)).thenReturn(dataSession);
        when(dataSession.isOpen()).thenReturn(true);
        when(dataSession.getId()).thenReturn("data-001");

        endpoint.onClose(session, reason);

        // 验证控制链路清理
        verify(dualLinkRouter).unregisterControlLink("ws-001");
        verify(sessionRegistry).removeControlSession("ws-001");
        verify(executorMgmtService).handleExecutorDisconnect("ws-001");
        verify(executorMgmtService).removeSendQueue(token);
        
        // 验证数据链路级联关闭
        verify(dataSession).close(any(CloseReason.class));
    }

    /**
     * 测试连接关闭：无关联数据链路
     */
    @Test
    public void testOnClose_NoDataLink_ShouldCleanupLocalOnly() {
        CloseReason reason = new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Bye");
        when(dualLinkRouter.getTokenBySessionId("ws-001")).thenReturn(null);

        endpoint.onClose(session, reason);

        verify(dualLinkRouter).unregisterControlLink("ws-001");
        verify(sessionRegistry).removeControlSession("ws-001");
        verify(executorMgmtService).handleExecutorDisconnect("ws-001");
        
        verifyNoInteractions(dataSession);
    }

    /**
     * 测试错误处理
     */
    @Test
    public void testOnError_ShouldLog() {
        endpoint.onError(session, new RuntimeException("Socket error"));
        // 主要是为了覆盖率，实际逻辑只是打印日志
    }

    /**
     * 测试getSession代理方法
     */
    @Test
    public void testGetSession_ShouldDelegateToRegistry() {
        when(sessionRegistry.getControlSession("ws-001")).thenReturn(session);
        
        Session result = endpoint.getSession("ws-001");
        
        Assert.assertEquals(session, result);
        verify(sessionRegistry).getControlSession("ws-001");
    }
}

