/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration.event;

import com.huawei.cloududn.dialingtestapp.service.executormanagement.ExecutorSelectionService;
import com.huawei.cloududn.dialingtestapp.service.executormanagement.dto.TaskDispatchRequest;
import com.huawei.cloududn.dialingtestapp.service.executormanagement.task.TaskInterfaceService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * TaskDispatchEventListener unit test
 *
 * @author g00940940
 * @since 2025-11-18
 */
@RunWith(MockitoJUnitRunner.class)
public class TaskDispatchEventListenerTest {
    @Mock
    private TaskInterfaceService taskInterfaceService;
    @Mock
    private ExecutorSelectionService executorSelectionService;
    @InjectMocks
    private TaskDispatchEventListener listener;

    @Test
    public void testHandleTaskDispatch_WithExecutorName() {
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("executor_name", "executor-1");
        payload.put("script_name", "test.air");
        payload.put("script_version", "1.0");
        payload.put("params", "{}");
        payload.put("proctype", "validation");
        payload.put("serial_no_list", Arrays.asList("ue-1", "ue-2"));
        TaskDispatchEvent event = new TaskDispatchEvent(this, "session-1", payload, "123");
        Mockito.doNothing().when(taskInterfaceService).dispatchTaskToAgent(Mockito.any(TaskDispatchRequest.class));
        listener.handleTaskDispatch(event);
        Mockito.verify(taskInterfaceService).dispatchTaskToAgent(Mockito.any(TaskDispatchRequest.class));
    }

    @Test
    public void testHandleTaskDispatch_InvalidPayload() {
        TaskDispatchEvent event = new TaskDispatchEvent(this, "session-2", "invalid-payload", "124");
        listener.handleTaskDispatch(event);
        Mockito.verify(taskInterfaceService, Mockito.never()).dispatchTaskToAgent(Mockito.any(TaskDispatchRequest.class));
    }
}

