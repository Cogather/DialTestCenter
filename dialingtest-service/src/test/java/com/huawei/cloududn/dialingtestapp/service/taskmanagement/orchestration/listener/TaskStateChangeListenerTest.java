/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration.listener;

import com.huawei.cloududn.dialingtestapp.service.taskmanagement.dto.TaskContext;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration.state.TaskState;

import org.junit.Assert;
import org.junit.Test;

/**
 * TaskStateChangeListener 接口单元测试
 *
 * @author g00940940
 * @since 2025-11-18
 */
public class TaskStateChangeListenerTest {
    @Test
    public void testOnStateChanged_CallbackReceivesArguments() {
        RecordingListener listener = new RecordingListener();
        TaskContext context = new TaskContext();

        listener.onStateChanged(TaskState.START_VALIDATION, TaskState.START_MODEL_TRAIN, context);

        Assert.assertEquals(TaskState.START_VALIDATION, listener.from);
        Assert.assertEquals(TaskState.START_MODEL_TRAIN, listener.to);
        Assert.assertSame(context, listener.context);
    }

    @Test
    public void testOnStateChanged_AllowsNullValues() {
        RecordingListener listener = new RecordingListener();

        listener.onStateChanged(null, TaskState.FINAL, null);

        Assert.assertNull(listener.from);
        Assert.assertEquals(TaskState.FINAL, listener.to);
        Assert.assertNull(listener.context);
    }

    private static class RecordingListener implements TaskStateChangeListener {
        private TaskState from;
        private TaskState to;
        private TaskContext context;

        @Override
        public void onStateChanged(TaskState from, TaskState to, TaskContext context) {
            this.from = from;
            this.to = to;
            this.context = context;
        }
    }
}

