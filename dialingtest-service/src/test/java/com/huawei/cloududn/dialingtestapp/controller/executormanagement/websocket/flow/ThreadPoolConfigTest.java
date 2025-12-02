/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.flow;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.Assert.*;

/**
 * ThreadPoolConfig单元测试
 *
 * @author g00940940
 * @since 2025-11-29
 */
public class ThreadPoolConfigTest {

    private ThreadPoolConfig config;

    @Before
    public void setUp() {
        config = new ThreadPoolConfig();
        ReflectionTestUtils.setField(config, "corePoolSize", 2);
        ReflectionTestUtils.setField(config, "maxPoolSize", 4);
        ReflectionTestUtils.setField(config, "queueCapacity", 10);
        ReflectionTestUtils.setField(config, "keepAliveSeconds", 60L);
    }

    @After
    public void tearDown() {
        config.shutdown();
    }

    @Test
    public void testGetExecutorService_Singleton() {
        ExecutorService executor1 = config.getExecutorService();
        assertNotNull(executor1);
        assertTrue(executor1 instanceof ThreadPoolExecutor);
        
        ThreadPoolExecutor tpe = (ThreadPoolExecutor) executor1;
        assertEquals(2, tpe.getCorePoolSize());
        assertEquals(4, tpe.getMaximumPoolSize());
        
        ExecutorService executor2 = config.getExecutorService();
        assertSame(executor1, executor2);
    }

    @Test
    public void testGetThreadPoolStats() {
        config.getExecutorService();
        String stats = config.getThreadPoolStats();
        assertNotNull(stats);
        assertTrue(stats.contains("ThreadPool"));
        assertTrue(stats.contains("coreSize=2"));
    }

    @Test
    public void testGetThreadPoolStats_NotInitialized() {
        ThreadPoolConfig emptyConfig = new ThreadPoolConfig();
        String stats = emptyConfig.getThreadPoolStats();
        assertEquals("Thread pool not initialized", stats);
    }
    
    @Test
    public void testShutdown() {
        ExecutorService executor = config.getExecutorService();
        assertFalse(executor.isShutdown());
        
        config.shutdown();
        assertTrue(executor.isShutdown());
    }
}

