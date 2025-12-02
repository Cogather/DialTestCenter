/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import java.nio.ByteBuffer;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * WebSocketSessionRegistry单元测试
 * 测试会话注册、移除、查询及消息发送功能
 *
 * @since 2025-12-02
 */
@RunWith(MockitoJUnitRunner.class)
public class WebSocketSessionRegistryTest {

    @InjectMocks
    private WebSocketSessionRegistry registry;

    @Mock
    private DualLinkRouter dualLinkRouter;

    @Mock
    private Session session;

    @Mock
    private RemoteEndpoint.Basic basicRemote;

    @Before
    public void setUp() {
        when(session.getId()).thenReturn("session-001");
        when(session.getBasicRemote()).thenReturn(basicRemote);
    }

    @Test
    public void testAddControlSession() {
        registry.addControlSession(session);
        assertEquals(session, registry.getControlSession("session-001"));
    }

    @Test
    public void testAddControlSession_Null() {
        registry.addControlSession(null);
        // Should not throw exception and size should be 0 (tested implicitly by other tests starting empty)
        assertNull(registry.getControlSession("any"));
    }

    @Test
    public void testAddDataSession() {
        registry.addDataSession(session);
        assertEquals(session, registry.getDataSession("session-001"));
    }

    @Test
    public void testAddDataSession_Null() {
        registry.addDataSession(null);
        assertNull(registry.getDataSession("any"));
    }

    @Test
    public void testRemoveControlSession() {
        registry.addControlSession(session);
        registry.removeControlSession("session-001");
        assertNull(registry.getControlSession("session-001"));
    }

    @Test
    public void testRemoveControlSession_NullId() {
        registry.addControlSession(session);
        registry.removeControlSession(null);
        assertEquals(session, registry.getControlSession("session-001"));
    }

    @Test
    public void testRemoveDataSession() {
        registry.addDataSession(session);
        registry.removeDataSession("session-001");
        assertNull(registry.getDataSession("session-001"));
    }

    @Test
    public void testRemoveDataSession_NullId() {
        registry.addDataSession(session);
        registry.removeDataSession(null);
        assertEquals(session, registry.getDataSession("session-001"));
    }

    @Test
    public void testGetControlSessionByToken() {
        String token = "token-123";
        when(dualLinkRouter.getControlSessionByToken(token)).thenReturn(session);
        
        Session result = registry.getControlSessionByToken(token);
        assertEquals(session, result);
        verify(dualLinkRouter).getControlSessionByToken(token);
    }

    @Test
    public void testGetDataSessionByToken() {
        String token = "token-123";
        when(dualLinkRouter.getDataSessionByToken(token)).thenReturn(session);
        
        Session result = registry.getDataSessionByToken(token);
        assertEquals(session, result);
        verify(dualLinkRouter).getDataSessionByToken(token);
    }

    @Test
    public void testSendTextBySessionId_Success() throws Exception {
        registry.addControlSession(session);
        when(session.isOpen()).thenReturn(true);
        
        registry.sendTextBySessionId("session-001", "hello");
        
        verify(basicRemote).sendText("hello");
    }

    @Test
    public void testSendTextBySessionId_SessionNotFound() throws Exception {
        registry.sendTextBySessionId("unknown", "hello");
        verify(basicRemote, never()).sendText(anyString());
    }

    @Test
    public void testSendTextBySessionId_SessionClosed() throws Exception {
        registry.addControlSession(session);
        when(session.isOpen()).thenReturn(false);
        
        registry.sendTextBySessionId("session-001", "hello");
        verify(basicRemote, never()).sendText(anyString());
    }

    @Test
    public void testSendTextByToken_Success() throws Exception {
        String token = "token-123";
        when(dualLinkRouter.getControlSessionByToken(token)).thenReturn(session);
        when(session.isOpen()).thenReturn(true);
        
        registry.sendTextByToken(token, "hello");
        
        verify(basicRemote).sendText("hello");
    }

    @Test
    public void testSendTextByToken_SessionNotFound() throws Exception {
        String token = "token-123";
        when(dualLinkRouter.getControlSessionByToken(token)).thenReturn(null);
        
        registry.sendTextByToken(token, "hello");
        verify(basicRemote, never()).sendText(anyString());
    }

    @Test
    public void testSendTextByToken_SessionClosed() throws Exception {
        String token = "token-123";
        when(dualLinkRouter.getControlSessionByToken(token)).thenReturn(session);
        when(session.isOpen()).thenReturn(false);
        
        registry.sendTextByToken(token, "hello");
        verify(basicRemote, never()).sendText(anyString());
    }

    @Test
    public void testSendBinaryByToken_Success() throws Exception {
        String token = "token-123";
        ByteBuffer buffer = ByteBuffer.wrap(new byte[]{1, 2, 3});
        when(dualLinkRouter.getDataSessionByToken(token)).thenReturn(session);
        when(session.isOpen()).thenReturn(true);
        
        registry.sendBinaryByToken(token, buffer);
        
        verify(basicRemote).sendBinary(buffer);
    }

    @Test
    public void testSendBinaryByToken_SessionNotFound() throws Exception {
        String token = "token-123";
        ByteBuffer buffer = ByteBuffer.wrap(new byte[]{1, 2, 3});
        when(dualLinkRouter.getDataSessionByToken(token)).thenReturn(null);
        
        registry.sendBinaryByToken(token, buffer);
        verify(basicRemote, never()).sendBinary(any());
    }

    @Test
    public void testSendBinaryByToken_SessionClosed() throws Exception {
        String token = "token-123";
        ByteBuffer buffer = ByteBuffer.wrap(new byte[]{1, 2, 3});
        when(dualLinkRouter.getDataSessionByToken(token)).thenReturn(session);
        when(session.isOpen()).thenReturn(false);
        
        registry.sendBinaryByToken(token, buffer);
        verify(basicRemote, never()).sendBinary(any());
    }

    @Test
    public void testGetSessionStats() {
        registry.addControlSession(session);
        registry.addDataSession(session);
        when(dualLinkRouter.getLinkStats()).thenReturn("LinkStats");
        
        String stats = registry.getSessionStats();
        
        assertTrue(stats.contains("control=1"));
        assertTrue(stats.contains("data=1"));
        assertTrue(stats.contains("LinkStats"));
    }
}

