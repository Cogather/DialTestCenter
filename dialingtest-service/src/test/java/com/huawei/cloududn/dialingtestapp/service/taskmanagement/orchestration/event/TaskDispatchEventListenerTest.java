/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration.event;

import com.huawei.cloududn.dialingtest.model.Executor;
import com.huawei.cloududn.dialingtest.model.Ue;
import com.huawei.cloududn.dialingtestapp.service.executormanagement.ExecutorSelectionService;
import com.huawei.cloududn.dialingtestapp.service.executormanagement.dto.TaskDispatchRequest;
import com.huawei.cloududn.dialingtestapp.service.executormanagement.task.TaskInterfaceService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * TaskDispatchEventListener 行为测试
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
    public void testHandleTaskDispatch_WithExplicitExecutor_DispatchesRequest() {
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("executor_name", "exec-001");
        payload.put("script_name", "validate.air");
        payload.put("script_version", "2.1");
        payload.put("params", "{\"k\":\"v\"}");
        payload.put("proctype", "gray");
        payload.put("serial_no_list", Arrays.asList("ue-1", "ue-2"));
        TaskDispatchEvent event = new TaskDispatchEvent(this, "session", payload, "45");

        listener.handleTaskDispatch(event);

        ArgumentCaptor<TaskDispatchRequest> captor = ArgumentCaptor.forClass(TaskDispatchRequest.class);
        verify(taskInterfaceService).dispatchTaskToAgent(captor.capture());
        TaskDispatchRequest request = captor.getValue();
        assertEquals("exec-001", request.getExecutorName());
        assertEquals(Integer.valueOf(45), request.getTaskId());
        assertEquals("validate.air", request.getScriptName());
        assertEquals("2.1", request.getVersion());
        assertEquals(Arrays.asList("ue-1", "ue-2"), request.getSerialNoList());
        assertEquals("gray", request.getProctype());
        assertEquals("{\"k\":\"v\"}", request.getParameters());
        verify(executorSelectionService, never()).selectIdleExecutorAndUe();
    }

    @Test
    public void testHandleTaskDispatch_AutoSelectsExecutorWhenMissing() {
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("script_name", "gray.air");
        payload.put("params", "{}");
        TaskDispatchEvent event = new TaskDispatchEvent(this, "session-2", payload, "77");
        Executor executor = Mockito.mock(Executor.class);
        Mockito.when(executor.getName()).thenReturn("auto-01");
        Ue ue = Mockito.mock(Ue.class);
        ExecutorSelectionService.ExecutorUeInfo ueInfo =
            new ExecutorSelectionService.ExecutorUeInfo(executor, ue);
        Mockito.when(executorSelectionService.selectIdleExecutorAndUe()).thenReturn(ueInfo);

        listener.handleTaskDispatch(event);

        ArgumentCaptor<TaskDispatchRequest> captor = ArgumentCaptor.forClass(TaskDispatchRequest.class);
        verify(taskInterfaceService).dispatchTaskToAgent(captor.capture());
        TaskDispatchRequest request = captor.getValue();
        assertEquals("auto-01", request.getExecutorName());
        assertEquals(Arrays.asList("default_ue"), request.getSerialNoList());
        verify(executorSelectionService).selectIdleExecutorAndUe();
    }

    @Test
    public void testHandleTaskDispatch_InvalidPayloadType_NoDispatch() {
        TaskDispatchEvent event = new TaskDispatchEvent(this, "session-3", "raw-payload", "9");

        listener.handleTaskDispatch(event);

        verifyNoInteractions(taskInterfaceService);
        verifyNoInteractions(executorSelectionService);
    }
}

