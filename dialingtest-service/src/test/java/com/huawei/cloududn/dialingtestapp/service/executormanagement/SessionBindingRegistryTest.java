/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.service.executormanagement;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * SessionBindingRegistry 单元测试
 *
 * @author g00940940
 * @since 2025-11-09
 */
public class SessionBindingRegistryTest {
    private SessionBindingRegistry registry;

    @Before
    public void setUp() {
        registry = new SessionBindingRegistry();
    }

    @Test
    public void testBind_Success() {
        registry.bind("session1", "executor1");
        assertEquals("executor1", registry.getExecutorName("session1"));
        assertEquals("session1", registry.getSessionId("executor1"));
    }

    @Test
    public void testBind_MultipleBindings() {
        registry.bind("session1", "executor1");
        registry.bind("session2", "executor2");
        assertEquals("executor1", registry.getExecutorName("session1"));
        assertEquals("executor2", registry.getExecutorName("session2"));
        assertEquals("session1", registry.getSessionId("executor1"));
        assertEquals("session2", registry.getSessionId("executor2"));
    }

    @Test
    public void testUnbind_RemovesBinding() {
        registry.bind("session1", "executor1");
        registry.unbind("session1");
        assertNull(registry.getExecutorName("session1"));
        assertNull(registry.getSessionId("executor1"));
    }

    @Test
    public void testUnbind_NonExistentSession() {
        registry.unbind("nonexistent");
        assertNull(registry.getExecutorName("nonexistent"));
    }

    @Test
    public void testGetExecutorName_NotFound() {
        assertNull(registry.getExecutorName("nonexistent"));
    }

    @Test
    public void testGetSessionId_NotFound() {
        assertNull(registry.getSessionId("nonexistent"));
    }

    @Test
    public void testBind_Rebind() {
        registry.bind("session1", "executor1");
        registry.bind("session1", "executor2");
        assertEquals("executor2", registry.getExecutorName("session1"));
        assertEquals("session1", registry.getSessionId("executor2"));
    }

    @Test
    public void testBindWithToken_Success() {
        long token = 123456789L;
        registry.bind("session1", "executor1", token);
        assertEquals("executor1", registry.getExecutorName("session1"));
        assertEquals("session1", registry.getSessionId("executor1"));
        assertEquals(Long.valueOf(token), registry.getToken("session1"));
    }

    @Test
    public void testBindWithToken_MultipleBindings() {
        long token1 = 123456789L;
        long token2 = 987654321L;
        registry.bind("session1", "executor1", token1);
        registry.bind("session2", "executor2", token2);
        assertEquals(Long.valueOf(token1), registry.getToken("session1"));
        assertEquals(Long.valueOf(token2), registry.getToken("session2"));
    }

    @Test
    public void testGetToken_NotFound() {
        assertNull(registry.getToken("nonexistent"));
    }

    @Test
    public void testUnbind_RemovesToken() {
        long token = 123456789L;
        registry.bind("session1", "executor1", token);
        registry.unbind("session1");
        assertNull(registry.getToken("session1"));
    }

    @Test
    public void testBindWithToken_Rebind() {
        long token1 = 111111111L;
        long token2 = 222222222L;
        registry.bind("session1", "executor1", token1);
        registry.bind("session1", "executor2", token2);
        assertEquals("executor2", registry.getExecutorName("session1"));
        assertEquals(Long.valueOf(token2), registry.getToken("session1"));
    }

    @Test
    public void testBind_ThenBindWithToken() {
        registry.bind("session1", "executor1");
        assertNull(registry.getToken("session1"));
        long token = 123456789L;
        registry.bind("session1", "executor1", token);
        assertEquals(Long.valueOf(token), registry.getToken("session1"));
    }

    @Test
    public void testBindWithToken_ThenBindWithoutToken() {
        long token = 123456789L;
        registry.bind("session1", "executor1", token);
        assertEquals(Long.valueOf(token), registry.getToken("session1"));
        registry.bind("session1", "executor2");
        // Note: bind without token doesn't clear token, token persists
        // This tests the actual behavior of SessionBindingRegistry
        assertEquals("executor2", registry.getExecutorName("session1"));
    }

    @Test
    public void testUnbind_RemovesBidirectionalMapping() {
        registry.bind("session1", "executor1");
        String sessionId = registry.getSessionId("executor1");
        assertEquals("session1", sessionId);
        registry.unbind("session1");
        assertNull(registry.getSessionId("executor1"));
        assertNull(registry.getExecutorName("session1"));
    }

    @Test
    public void testBind_SameExecutorDifferentSessions_OverwritesMapping() {
        registry.bind("session1", "executor1");
        registry.bind("session2", "executor1");
        assertEquals("session2", registry.getSessionId("executor1"));
        assertEquals("executor1", registry.getExecutorName("session1"));
        assertEquals("executor1", registry.getExecutorName("session2"));
    }
}

