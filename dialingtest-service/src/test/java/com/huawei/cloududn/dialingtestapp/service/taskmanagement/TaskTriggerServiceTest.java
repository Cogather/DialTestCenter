/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.service.taskmanagement;

import com.huawei.cloududn.dialingtest.model.StartTaskRequest;
import com.huawei.cloududn.dialingtest.model.TaskEntity;
import com.huawei.cloududn.dialingtest.model.TemplateEntity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;

/**
 * TaskTriggerService unit test
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

    private TaskEntity mockTask;

    @Before
    public void setUp() {
        mockTask = new TaskEntity();
        mockTask.setId(1);
        mockTask.setCreator("MANUAL");
        mockTask.setStatus("RUNNING");
    }

    @Test
    public void testCreateTaskFromRequest_TrainingScenario() {
        Mockito.when(taskMgmtService.create(Mockito.any(TaskEntity.class))).thenReturn(mockTask);
        StartTaskRequest request = new StartTaskRequest();
        request.setScenario(StartTaskRequest.ScenarioEnum.TRAINING);
        TaskEntity result = taskTriggerService.createTaskFromRequest(request);
        assertNotNull(result);
        assertEquals("MANUAL", result.getCreator());
        Mockito.verify(taskMgmtService).create(Mockito.any(TaskEntity.class));
    }

    @Test
    public void testCreateTaskFromRequest_ValidationScenario() {
        Mockito.when(taskMgmtService.create(Mockito.any(TaskEntity.class))).thenReturn(mockTask);
        StartTaskRequest request = new StartTaskRequest();
        request.setScenario(StartTaskRequest.ScenarioEnum.VALIDATION);
        TaskEntity result = taskTriggerService.createTaskFromRequest(request);
        assertNotNull(result);
        assertEquals("MANUAL", result.getCreator());
        Mockito.verify(taskMgmtService).create(Mockito.any(TaskEntity.class));
    }

    @Test
    public void testCreateTaskFromTemplate_Success() {
        Mockito.when(taskMgmtService.create(Mockito.any(TaskEntity.class))).thenReturn(mockTask);
        TemplateEntity template = new TemplateEntity();
        template.setId(100);
        template.setInput("{\"test\":\"data\"}");
        TaskEntity result = taskTriggerService.createTaskFromTemplate(template);
        assertNotNull(result);
        Mockito.verify(taskMgmtService).create(Mockito.any(TaskEntity.class));
    }
}

