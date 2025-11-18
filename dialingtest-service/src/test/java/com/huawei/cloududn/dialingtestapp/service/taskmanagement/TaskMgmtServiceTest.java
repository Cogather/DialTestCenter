/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.service.taskmanagement;

import com.huawei.cloududn.dialingtest.model.TaskEntity;
import com.huawei.cloududn.dialingtestapp.dao.taskmanagement.TaskDao;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * TaskMgmtService 单元测试
 *
 * @author g00940940
 * @since 2025-11-18
 */
@RunWith(MockitoJUnitRunner.class)
public class TaskMgmtServiceTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    @Mock
    private TaskDao taskDao;
    @InjectMocks
    private TaskMgmtService taskMgmtService;

    @Test
    public void testCreateMainTask_SuccessFlow() {
        TaskEntity entity = new TaskEntity();
        entity.setId(10);
        TaskEntity refreshed = new TaskEntity();
        refreshed.setId(10);
        Mockito.when(taskDao.insert(entity)).thenReturn(1);
        Mockito.when(taskDao.findById(10L)).thenReturn(refreshed);

        TaskEntity result = taskMgmtService.create(entity);

        assertSame(refreshed, result);
        assertNull(entity.getParentTaskId());
        Mockito.verify(taskDao).initMainTaskId(10L);
    }

    @Test
    public void testCreateSubTask_MissingIds_ThrowsException() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("mainTaskId and parentTaskId cannot be null for sub task");

        taskMgmtService.createSubTask(null, 2L, new TaskEntity());
    }

    @Test
    public void testUpdateStatusAndContext_DaoFailure_ThrowsException() {
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Update task failed");
        Mockito.when(taskDao.updateStatusAndContext(1L, "RUNNING", null, "{}")).thenReturn(0);

        taskMgmtService.updateStatusAndContext(1L, "RUNNING", null, "{}");
    }
}

