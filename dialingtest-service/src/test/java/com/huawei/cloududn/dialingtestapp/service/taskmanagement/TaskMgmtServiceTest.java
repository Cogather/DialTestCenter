/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.service.taskmanagement;

import com.huawei.cloududn.dialingtest.model.TaskEntity;
import com.huawei.cloududn.dialingtestapp.dao.taskmanagement.TaskDao;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * TaskMgmtService unit test
 *
 * @author g00940940
 * @since 2025-11-18
 */
@RunWith(MockitoJUnitRunner.class)
public class TaskMgmtServiceTest {
    @Mock
    private TaskDao taskDao;
    @InjectMocks
    private TaskMgmtService taskMgmtService;

    private TaskEntity mockTask;

    @Before
    public void setUp() {
        mockTask = new TaskEntity();
        mockTask.setId(1);
        mockTask.setCreator("MANUAL");
        mockTask.setStatus("RUNNING");
    }

    @Test
    public void testCreate_AndCreateMainTask_Success() {
        mockTask.setId(1);
        Mockito.when(taskDao.insert(Mockito.any(TaskEntity.class))).thenReturn(1);
        Mockito.when(taskDao.findById(Mockito.anyLong())).thenReturn(mockTask);
        TaskEntity entity = new TaskEntity();
        entity.setCreator("MANUAL");
        entity.setId(1);
        TaskEntity result1 = taskMgmtService.create(entity);
        assertNotNull(result1);
        TaskEntity result2 = taskMgmtService.createMainTask(entity);
        assertNotNull(result2);
        assertNull(entity.getParentTaskId());
        Mockito.verify(taskDao, Mockito.times(2)).insert(entity);
    }

    @Test
    public void testCreateSubTask_AndFindById_Success() {
        Mockito.when(taskDao.insert(Mockito.any(TaskEntity.class))).thenReturn(1);
        Mockito.when(taskDao.findById(Mockito.any())).thenReturn(mockTask);
        TaskEntity entity = new TaskEntity();
        entity.setId(2);
        entity.setCreator("CALLBACK");
        TaskEntity result = taskMgmtService.createSubTask(1L, 1L, entity);
        assertNotNull(result);
        assertEquals(Integer.valueOf(1), entity.getMainTaskId());
        assertEquals(Integer.valueOf(1), entity.getParentTaskId());
        TaskEntity found = taskMgmtService.findById(1L);
        assertNotNull(found);
        Mockito.verify(taskDao).insert(entity);
    }

    @Test
    public void testFindMethods_AndCountMainTasks() {
        List<TaskEntity> mockTasks = Collections.singletonList(mockTask);
        Mockito.when(taskDao.findMainTasks(Mockito.anyInt(), Mockito.anyInt())).thenReturn(mockTasks);
        Mockito.when(taskDao.countMainTasks()).thenReturn(5L);
        Mockito.when(taskDao.findSubTasksByMainTaskId(Mockito.anyLong())).thenReturn(mockTasks);
        List<TaskEntity> result1 = taskMgmtService.findMainTasks(0, 10);
        assertNotNull(result1);
        assertEquals(1, result1.size());
        long count = taskMgmtService.countMainTasks();
        assertEquals(5L, count);
        List<TaskEntity> result2 = taskMgmtService.findSubTasksByMainTaskId(1L);
        assertNotNull(result2);
        assertEquals(1, result2.size());
    }

    @Test
    public void testUpdateStatusAndContext_AndExceptionHandling() {
        Mockito.when(taskDao.updateStatusAndContext(Mockito.anyLong(), Mockito.anyString(),
            Mockito.anyString(), Mockito.anyString())).thenReturn(1).thenReturn(0);
        taskMgmtService.updateStatusAndContext(1L, "COMPLETED", "SUCCESS", "{}");
        Mockito.verify(taskDao).updateStatusAndContext(1L, "COMPLETED", "SUCCESS", "{}");
        try {
            taskMgmtService.updateStatusAndContext(2L, "FAILED", "ERROR", "{}");
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("Update task failed"));
        }
    }
}

