/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.flow;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * InboundFileCompleteEvent单元测试
 *
 * @author g00940940
 * @since 2025-11-29
 */
public class InboundFileCompleteEventTest {

    @Test
    public void testEventConstructionAndGetters() {
        Object source = new Object();
        InboundFileState state = new InboundFileState();
        state.setSessionId("session-1");
        state.setExpectedSize(100);
        state.setExpectedCrc("crc");
        state.setTempFilePath("path");
        
        InboundFileCompleteEvent event = new InboundFileCompleteEvent(source, state);
        
        assertNotNull(event);
        assertEquals(source, event.getSource());
        assertEquals(state, event.getState());
    }
}

