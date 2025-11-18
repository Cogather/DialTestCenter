/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.flow;

import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

/**
 * InboundFileState单元测试
 * 测试文件状态和CRC校验
 *
 * @author g00940940
 * @since 2025-11-16
 */
public class InboundFileStateTest {
    private InboundFileState state;

    @Before
    public void setUp() {
        state = new InboundFileState();
    }

    /**
     * 测试完整文件状态生命周期
     */
    @Test
    public void testFileStateLifecycle() {
        state.setSessionId("session-001");
        state.setExpectedSize(1000);
        state.setTempFilePath("/tmp/test-file.bin");

        assertEquals("session-001", state.getSessionId());
        assertEquals(1000, state.getExpectedSize());
        assertEquals(0, state.getReceivedSize());
        assertFalse(state.isComplete());
        assertFalse(state.hasError());

        state.addReceivedSize(500);
        assertEquals(500, state.getReceivedSize());
        assertFalse(state.isComplete());

        state.addReceivedSize(500);
        assertEquals(1000, state.getReceivedSize());
        assertTrue(state.isComplete());

        state.setError("Test error");
        assertTrue(state.hasError());
        assertEquals("Test error", state.getError());

        Object context = new Object();
        state.setBusinessContext(context);
        assertSame(context, state.getBusinessContext());
    }

    /**
     * 测试CRC校验（正常和异常）
     */
    @Test
    public void testVerifyCrc_ValidAndInvalid() {
        byte[] data1 = "test data".getBytes(StandardCharsets.UTF_8);
        state.updateCrc(data1);

        java.util.zip.CRC32 expectedCrc = new java.util.zip.CRC32();
        expectedCrc.update(data1);
        String crcValue = Long.toHexString(expectedCrc.getValue());

        state.setExpectedCrc(crcValue);
        assertTrue(state.verifyCrc());

        state.setExpectedCrc("ffffffff");
        assertFalse(state.verifyCrc());

        state.setExpectedCrc(null);
        assertTrue(state.verifyCrc());

        state.setExpectedCrc("");
        assertTrue(state.verifyCrc());

        InboundFileState state2 = new InboundFileState();
        byte[] data2 = "different data".getBytes(StandardCharsets.UTF_8);
        state2.updateCrc(data2);
        state2.setExpectedCrc(crcValue);
        assertFalse(state2.verifyCrc());
    }
}

