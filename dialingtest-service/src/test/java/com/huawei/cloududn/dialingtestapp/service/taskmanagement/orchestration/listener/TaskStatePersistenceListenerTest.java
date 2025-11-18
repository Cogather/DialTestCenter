/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.TaskMgmtService;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.dto.TaskContext;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration.state.TaskState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * TaskStatePersistenceListener 行为测试
 *
 * @author g00940940
 * @since 2025-11-18
 */
@RunWith(MockitoJUnitRunner.class)
public class TaskStatePersistenceListenerTest {
    @Mock
    private TaskMgmtService taskMgmtService;
    private TaskStatePersistenceListener listener;

    @Before
    public void setUp() throws Exception {
        listener = new TaskStatePersistenceListener();
        injectField("taskMgmtService", taskMgmtService);
    }

    @Test
    public void testOnStateChanged_NullContext_SkipsPersistence() {
        listener.onStateChanged(TaskState.START_VALIDATION, TaskState.START_MODEL_TRAIN, null);

        verifyNoInteractions(taskMgmtService);
    }

    @Test
    public void testOnStateChanged_InvalidTaskId_SkipsPersistence() {
        TaskContext context = new TaskContext();
        context.getData().put("taskId", "invalid");

        listener.onStateChanged(TaskState.START_VALIDATION, TaskState.START_MODEL_TRAIN, context);

        verifyNoInteractions(taskMgmtService);
    }

    @Test
    public void testOnStateChanged_SerializationFailure_FallbacksToEmptyJson() throws Exception {
        TaskContext context = new TaskContext();
        context.getData().put("taskId", 15L);
        ObjectMapper faultyMapper = Mockito.mock(ObjectMapper.class);
        Mockito.when(faultyMapper.writeValueAsString(Mockito.any(TaskContext.class)))
            .thenThrow(new JsonProcessingException("boom") { private static final long serialVersionUID = 1L; });
        injectField("objectMapper", faultyMapper);

        listener.onStateChanged(TaskState.START_VALIDATION, TaskState.START_MODEL_TRAIN, context);

        verify(taskMgmtService).updateStatusAndContext(eq(15L), eq("RUNNING"), eq(null), eq("{}"));
        assertEquals(TaskState.START_MODEL_TRAIN, context.getStep());
    }

    private void injectField(String name, Object value) throws Exception {
        Field field = TaskStatePersistenceListener.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(listener, value);
    }
}

