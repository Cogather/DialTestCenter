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
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * TaskStateMachine 单元测试
 *
 * @author g00940940
 * @since 2025-11-18
 */
@RunWith(MockitoJUnitRunner.class)
public class TaskStateMachineTest {
    private TaskStateMachine stateMachine;
    @Mock
    private TaskAction action;
    @Mock
    private TaskStateChangeListener listener;
    @Mock
    private TaskStateChangeListener faultyListener;

    @Before
    public void setUp() {
        stateMachine = new TaskStateMachine();
        stateMachine.init();
    }

    @Test
    public void testSendEvent_CurrentStateNull_ReturnsNull() {
        TaskState next = stateMachine.sendEvent(null, TaskEvent.TASK_SUCCESS, new TaskContext());

        assertNull(next);
    }

    @Test
    public void testSendEvent_EventNull_ReturnsCurrent() {
        TaskState current = TaskState.START_VALIDATION;

        TaskState next = stateMachine.sendEvent(current, null, new TaskContext());

        assertEquals(current, next);
    }

    @Test
    public void testSendEvent_ContextNull_ReturnsCurrent() {
        TaskState current = TaskState.START_VALIDATION;

        TaskState next = stateMachine.sendEvent(current, TaskEvent.TASK_SUCCESS, null);

        assertEquals(current, next);
    }

    @Test
    public void testSendEvent_NoTransitionDefined_ReturnsCurrent() {
        TaskContext context = new TaskContext();

        TaskState next = stateMachine.sendEvent(TaskState.FINAL, TaskEvent.TASK_SUCCESS, context);

        assertEquals(TaskState.FINAL, next);
    }

    @Test
    public void testSendEvent_ValidTransition_NotifiesListenerAndExecutesAction() {
        stateMachine.addListener(listener);
        stateMachine.addListener(listener);
        stateMachine.registerAction(TaskState.START_MODEL_TRAIN, null);
        stateMachine.registerAction(TaskState.START_MODEL_TRAIN, action);
        TaskContext context = new TaskContext();

        TaskState next = stateMachine.sendEvent(TaskState.START_TRAINING_DIALING, TaskEvent.TASK_SUCCESS, context);

        assertEquals(TaskState.START_MODEL_TRAIN, next);
        verify(listener, times(1))
            .onStateChanged(TaskState.START_TRAINING_DIALING, TaskState.START_MODEL_TRAIN, context);
        verify(action).execute(context);
    }

    @Test
    public void testSendEvent_ListenerThrows_ActionStillExecutes() {
        TaskContext context = new TaskContext();
        stateMachine.addListener(faultyListener);
        stateMachine.addListener(listener);
        doThrow(new RuntimeException("boom")).when(faultyListener)
            .onStateChanged(Mockito.any(TaskState.class), Mockito.any(TaskState.class), Mockito.any(TaskContext.class));
        stateMachine.registerAction(TaskState.START_MODEL_REPLAY, action);

        TaskState next = stateMachine.sendEvent(TaskState.START_MODEL_TRAIN, TaskEvent.TASK_SUCCESS, context);

        assertEquals(TaskState.START_MODEL_REPLAY, next);
        verify(listener).onStateChanged(TaskState.START_MODEL_TRAIN, TaskState.START_MODEL_REPLAY, context);
        verify(action).execute(context);
    }
}

