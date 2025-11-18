/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.service.taskmanagement;

import com.huawei.cloududn.dialingtest.model.TaskEntity;
import com.huawei.cloududn.dialingtest.model.TemplateEntity;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.dto.StartTaskRequest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * TaskTriggerService 单元测试
 *
 * @author g00940940
 * @since 2025-11-18
 */
@RunWith(MockitoJUnitRunner.class)
public class TaskTriggerServiceTest {
    @Mock
    private TaskMgmtService taskMgmtService;
    @Mock
    private TemplateMgmtService templateMgmtService;
    @InjectMocks
    private TaskTriggerService taskTriggerService;

    @Test
    public void testCreateTaskFromRequest_TrainingScenarioSetsTrainingStep() {
        StartTaskRequest request = new StartTaskRequest();
        request.setScenario("training");
        request.setBusinessType("biz-1");
        TaskEntity saved = new TaskEntity();
        Mockito.when(taskMgmtService.create(Mockito.any(TaskEntity.class))).thenReturn(saved);

        TaskEntity result = taskTriggerService.createTaskFromRequest(request);

        assertSame(saved, result);
        ArgumentCaptor<TaskEntity> captor = ArgumentCaptor.forClass(TaskEntity.class);
        Mockito.verify(taskMgmtService).create(captor.capture());
        TaskEntity submitted = captor.getValue();
        assertEquals("MANUAL", submitted.getCreator());
        assertEquals("RUNNING", submitted.getStatus());
        assertTrue(submitted.getContext().contains("START_TRAINING_DIALING"));
        assertTrue(submitted.getInput().contains("training"));
    }

    @Test
    public void testCreateTaskFromRequest_NullRequestDefaultsToValidation() {
        TaskEntity saved = new TaskEntity();
        Mockito.when(taskMgmtService.create(Mockito.any(TaskEntity.class))).thenReturn(saved);

        TaskEntity result = taskTriggerService.createTaskFromRequest(null);

        assertSame(saved, result);
        ArgumentCaptor<TaskEntity> captor = ArgumentCaptor.forClass(TaskEntity.class);
        Mockito.verify(taskMgmtService).create(captor.capture());
        TaskEntity submitted = captor.getValue();
        assertTrue(submitted.getContext().contains("START_VALIDATION"));
        assertNull(submitted.getInput());
    }

    @Test
    public void testCreateTaskFromTemplate_CopiesTemplateAttributes() {
        TemplateEntity template = new TemplateEntity();
        template.setId(5);
        template.setInput("{\"k\":\"v\"}");
        TaskEntity saved = new TaskEntity();
        Mockito.when(taskMgmtService.create(Mockito.any(TaskEntity.class))).thenReturn(saved);

        TaskEntity result = taskTriggerService.createTaskFromTemplate(template);

        assertSame(saved, result);
        ArgumentCaptor<TaskEntity> captor = ArgumentCaptor.forClass(TaskEntity.class);
        Mockito.verify(taskMgmtService).create(captor.capture());
        TaskEntity submitted = captor.getValue();
        assertEquals(Integer.valueOf(5), submitted.getTemplateTaskId());
        assertEquals("{\"k\":\"v\"}", submitted.getInput());
        assertTrue(submitted.getContext().contains("START_VALIDATION"));
        assertEquals("CRON", submitted.getCreator());
    }
}

