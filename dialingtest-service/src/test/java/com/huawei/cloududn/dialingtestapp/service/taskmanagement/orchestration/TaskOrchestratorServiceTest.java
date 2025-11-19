/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration;

import com.huawei.cloududn.dialingtest.model.TaskEntity;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.TaskMgmtService;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.dto.TaskContext;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration.state.TaskEvent;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration.state.TaskState;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;

/**
 * 任务编排服务单元测试
 *
 * @author g00940940
 * @since 2025-11-10
 */
@RunWith(MockitoJUnitRunner.class)
public class TaskOrchestratorServiceTest {
    @Mock
    private TaskStateMachine taskStateMachine;
    @Mock
    private TaskMgmtService taskMgmtService;
    @InjectMocks
    private TaskOrchestratorService orchestrator;

    @Test
    public void testSendResultEvent_Success_AndInit() {
        orchestrator.init();
        TaskEntity task = new TaskEntity();
        task.setId(1);
        task.setContext("{}");
        Mockito.when(taskMgmtService.findById(1L)).thenReturn(task);
        Mockito.when(taskStateMachine.sendEvent(Mockito.any(TaskState.class), Mockito.any(TaskEvent.class),
                        Mockito.any(TaskContext.class)))
                .thenReturn(TaskState.START_TRAINING_DIALING);
        boolean result = orchestrator.sendResultEvent(1L, true);
        assertTrue("sendResultEvent should return true for existing task", result);
        Mockito.verify(taskStateMachine).sendEvent(Mockito.eq(TaskState.START_VALIDATION),
                Mockito.eq(TaskEvent.TASK_SUCCESS), Mockito.any(TaskContext.class));
        Mockito.verify(taskMgmtService).updateStatusAndContext(Mockito.eq(1L), Mockito.eq("RUNNING"),
                Mockito.isNull(), Mockito.anyString());
    }

    @Test
    public void testSendResultEvent_TaskNotFound() {
        Mockito.when(taskMgmtService.findById(999L)).thenReturn(null);
        boolean result = orchestrator.sendResultEvent(999L, true);
        assertFalse("sendResultEvent should return false for non-existent task", result);
        Mockito.verify(taskStateMachine, Mockito.never()).sendEvent(Mockito.any(TaskState.class),
                Mockito.any(TaskEvent.class), Mockito.any(TaskContext.class));
    }

    @Test
    public void testSendResultEvent_FailedEvent() {
        TaskEntity task = new TaskEntity();
        task.setId(1);
        task.setContext("{\"step\":\"START_VALIDATION\",\"data\":{}}");
        Mockito.when(taskMgmtService.findById(1L)).thenReturn(task);
        Mockito.when(taskStateMachine.sendEvent(Mockito.any(TaskState.class), Mockito.any(TaskEvent.class),
                        Mockito.any(TaskContext.class)))
                .thenReturn(TaskState.START_TRAINING_DIALING);
        boolean result = orchestrator.sendResultEvent(1L, false);
        assertTrue("sendResultEvent should return true for failed event on existing task", result);
        Mockito.verify(taskStateMachine).sendEvent(Mockito.eq(TaskState.START_VALIDATION),
                Mockito.eq(TaskEvent.TASK_FAILED), Mockito.any(TaskContext.class));
    }

    @Test
    public void testStopTask_Success() {
        TaskEntity task = new TaskEntity();
        task.setId(1);
        task.setContext("{\"step\":\"START_VALIDATION\",\"data\":{}}");
        Mockito.when(taskMgmtService.findById(1L)).thenReturn(task);
        Mockito.when(taskStateMachine.sendEvent(Mockito.any(TaskState.class), Mockito.any(TaskEvent.class),
                        Mockito.any(TaskContext.class)))
                .thenReturn(TaskState.FINAL);
        orchestrator.stopTask(1L);
        Mockito.verify(taskStateMachine).sendEvent(Mockito.eq(TaskState.START_VALIDATION),
                Mockito.eq(TaskEvent.STOP), Mockito.any(TaskContext.class));
        Mockito.verify(taskMgmtService).updateStatusAndContext(Mockito.eq(1L), Mockito.eq("STOPPED"),
                Mockito.eq("STOPPED"), Mockito.anyString());
    }

    @Test
    public void testStopTask_TaskNotFound() {
        Mockito.when(taskMgmtService.findById(999L)).thenReturn(null);
        orchestrator.stopTask(999L);
        Mockito.verify(taskStateMachine, Mockito.never()).sendEvent(Mockito.any(TaskState.class),
                Mockito.any(TaskEvent.class), Mockito.any(TaskContext.class));
        Mockito.verify(taskMgmtService, Mockito.never()).updateStatusAndContext(Mockito.anyLong(),
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testSendResultEvent_FinalState() {
        TaskEntity task = new TaskEntity();
        task.setId(1);
        task.setContext("{\"step\":\"START_FULL_RELEASE\",\"data\":{}}");
        Mockito.when(taskMgmtService.findById(1L)).thenReturn(task);
        Mockito.when(taskStateMachine.sendEvent(Mockito.any(TaskState.class), Mockito.any(TaskEvent.class),
                        Mockito.any(TaskContext.class)))
                .thenReturn(TaskState.FINAL);
        boolean result = orchestrator.sendResultEvent(1L, true);
        assertTrue("sendResultEvent should return true for final state", result);
        Mockito.verify(taskMgmtService).updateStatusAndContext(Mockito.eq(1L), Mockito.eq("COMPLETED"),
                Mockito.eq("SUCCESS"), Mockito.anyString());
    }

    @Test
    public void testSendResultEvent_EdgeCases() {
        TaskEntity task1 = new TaskEntity();
        task1.setId(1);
        task1.setContext("{\"step\":\"START_VALIDATION\",\"data\":{\"last_callback_fingerprint\":\"S:null\"}}");
        Mockito.when(taskMgmtService.findById(1L)).thenReturn(task1);
        boolean result1 = orchestrator.sendResultEvent(1L, true);
        assertTrue("sendResultEvent should return true for duplicate callback", result1);
        Mockito.verify(taskStateMachine, Mockito.never()).sendEvent(Mockito.any(TaskState.class),
                Mockito.any(TaskEvent.class), Mockito.any(TaskContext.class));
        TaskEntity task2 = new TaskEntity();
        task2.setId(2);
        task2.setContext("");
        Mockito.when(taskMgmtService.findById(2L)).thenReturn(task2);
        Mockito.when(taskStateMachine.sendEvent(Mockito.any(TaskState.class), Mockito.any(TaskEvent.class),
                        Mockito.any(TaskContext.class)))
                .thenReturn(TaskState.FINAL);
        boolean result2 = orchestrator.sendResultEvent(2L, true);
        assertTrue("sendResultEvent should return true for empty context", result2);
        Mockito.verify(taskStateMachine).sendEvent(Mockito.any(TaskState.class),
                Mockito.eq(TaskEvent.TASK_SUCCESS), Mockito.any(TaskContext.class));
    }
}


