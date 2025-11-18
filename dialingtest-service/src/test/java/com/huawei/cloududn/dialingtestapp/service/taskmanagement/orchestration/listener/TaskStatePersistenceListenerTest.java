/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration.listener;

import com.huawei.cloududn.dialingtestapp.service.taskmanagement.TaskMgmtService;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.dto.TaskContext;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration.state.TaskState;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;

/**
 * TaskStatePersistenceListener unit test
 *
 * @author g00940940
 * @since 2025-11-18
 */
@RunWith(MockitoJUnitRunner.class)
public class TaskStatePersistenceListenerTest {
    @Mock
    private TaskMgmtService taskMgmtService;
    @InjectMocks
    private TaskStatePersistenceListener listener;

    @Test
    public void testOnStateChanged_VariousTaskIdTypes() {
        TaskContext context1 = new TaskContext();
        context1.getData().put("taskId", 1L);
        Mockito.doNothing().when(taskMgmtService).updateStatusAndContext(
            Mockito.anyLong(), Mockito.anyString(), Mockito.isNull(), Mockito.anyString());
        listener.onStateChanged(TaskState.START_VALIDATION, TaskState.START_TRAINING_DIALING, context1);
        assertEquals(TaskState.START_TRAINING_DIALING, context1.getStep());
        TaskContext context2 = new TaskContext();
        context2.getData().put("taskId", 2);
        listener.onStateChanged(TaskState.START_VALIDATION, TaskState.START_MODEL_TRAIN, context2);
        TaskContext context3 = new TaskContext();
        context3.getData().put("taskId", "3");
        listener.onStateChanged(TaskState.START_MODEL_TRAIN, TaskState.START_MODEL_REPLAY, context3);
        Mockito.verify(taskMgmtService, Mockito.times(3)).updateStatusAndContext(
            Mockito.anyLong(), Mockito.eq("RUNNING"), Mockito.isNull(), Mockito.anyString());
    }

    @Test
    public void testOnStateChanged_EdgeCases() {
        listener.onStateChanged(TaskState.START_VALIDATION, null, new TaskContext());
        TaskContext emptyContext = new TaskContext();
        listener.onStateChanged(TaskState.START_VALIDATION, TaskState.FINAL, emptyContext);
        TaskContext invalidIdContext = new TaskContext();
        invalidIdContext.getData().put("taskId", "invalid");
        listener.onStateChanged(TaskState.START_VALIDATION, TaskState.FINAL, invalidIdContext);
        listener.onStateChanged(TaskState.START_VALIDATION, TaskState.FINAL, null);
        Mockito.verify(taskMgmtService, Mockito.never()).updateStatusAndContext(
            Mockito.anyLong(), Mockito.anyString(), Mockito.isNull(), Mockito.anyString());
    }
}

