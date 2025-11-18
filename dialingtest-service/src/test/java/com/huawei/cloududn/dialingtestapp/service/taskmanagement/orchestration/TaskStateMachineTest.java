/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration;

import com.huawei.cloududn.dialingtestapp.service.taskmanagement.dto.TaskContext;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration.action.TaskAction;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration.listener.TaskStateChangeListener;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration.state.TaskEvent;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration.state.TaskState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;

/**
 * TaskStateMachine unit test
 *
 * @author g00940940
 * @since 2025-11-18
 */
@RunWith(MockitoJUnitRunner.class)
public class TaskStateMachineTest {
    @InjectMocks
    private TaskStateMachine taskStateMachine;
    @Mock
    private TaskAction mockAction;
    @Mock
    private TaskStateChangeListener mockListener;

    @Before
    public void setUp() {
        taskStateMachine.init();
    }

    @Test
    public void testSendEvent_ValidationSuccess() {
        TaskContext context = new TaskContext();
        context.getData().put("taskId", 1L);
        TaskState result = taskStateMachine.sendEvent(TaskState.START_VALIDATION, TaskEvent.TASK_SUCCESS, context);
        assertEquals(TaskState.FINAL, result);
    }

    @Test
    public void testSendEvent_ValidationFailed() {
        TaskContext context = new TaskContext();
        context.getData().put("taskId", 1L);
        TaskState result = taskStateMachine.sendEvent(TaskState.START_VALIDATION, TaskEvent.TASK_FAILED, context);
        assertEquals(TaskState.START_TRAINING_DIALING, result);
    }

    @Test
    public void testSendEvent_TrainingDialingSuccess() {
        TaskContext context = new TaskContext();
        context.getData().put("taskId", 1L);
        TaskState result = taskStateMachine.sendEvent(TaskState.START_TRAINING_DIALING,
                TaskEvent.TASK_SUCCESS, context);
        assertEquals(TaskState.START_MODEL_TRAIN, result);
    }

    @Test
    public void testRegisterAction_Success() {
        taskStateMachine.registerAction(TaskState.START_TRAINING_DIALING, mockAction);
        TaskContext context = new TaskContext();
        context.getData().put("taskId", 1L);
        taskStateMachine.sendEvent(TaskState.START_VALIDATION, TaskEvent.TASK_FAILED, context);
        Mockito.verify(mockAction, Mockito.times(1)).execute(Mockito.any(TaskContext.class));
    }

    @Test
    public void testAddListener_Success() {
        taskStateMachine.addListener(mockListener);
        TaskContext context = new TaskContext();
        context.getData().put("taskId", 1L);
        taskStateMachine.sendEvent(TaskState.START_VALIDATION, TaskEvent.TASK_SUCCESS, context);
        Mockito.verify(mockListener, Mockito.times(1)).onStateChanged(
            Mockito.eq(TaskState.START_VALIDATION), Mockito.eq(TaskState.FINAL), Mockito.eq(context));
    }

    @Test
    public void testSendEvent_NullCurrentState() {
        TaskContext context = new TaskContext();
        TaskState result = taskStateMachine.sendEvent(null, TaskEvent.TASK_SUCCESS, context);
        assertNull(result);
    }
}

