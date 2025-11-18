/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration.event;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

/**
 * TaskDispatchEvent 单元测试
 *
 * @author g00940940
 * @since 2025-11-18
 */
public class TaskDispatchEventTest {
    @Test
    public void testConstructor_PreservesValues() {
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("k", "v");
        TaskDispatchEvent event = new TaskDispatchEvent(this, "session-1", payload, "101");

        Assert.assertEquals("session-1", event.getSessionId());
        Assert.assertEquals(payload, event.getTaskPayload());
        Assert.assertEquals("101", event.getTaskId());
        Assert.assertEquals(this, event.getSource());
    }

    @Test
    public void testGetTaskPayload_SupportsCustomObjects() {
        Object customPayload = new Object();
        TaskDispatchEvent event = new TaskDispatchEvent("source", "session-2", customPayload, "9");

        Assert.assertSame(customPayload, event.getTaskPayload());
        Assert.assertEquals("session-2", event.getSessionId());
    }
}

