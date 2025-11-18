/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.service.taskmanagement;

import com.huawei.cloududn.dialingtest.model.TemplateEntity;
import com.huawei.cloududn.dialingtestapp.dao.taskmanagement.TemplateDao;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertSame;

/**
 * TemplateMgmtService 单元测试
 *
 * @author g00940940
 * @since 2025-11-18
 */
@RunWith(MockitoJUnitRunner.class)
public class TemplateMgmtServiceTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    @Mock
    private TemplateDao templateDao;
    @InjectMocks
    private TemplateMgmtService templateMgmtService;

    @Test
    public void testCreate_WhenInsertSucceeds_ReturnsEntity() {
        TemplateEntity template = new TemplateEntity();
        Mockito.when(templateDao.insert(template)).thenReturn(1);

        TemplateEntity result = templateMgmtService.create(template);

        assertSame(template, result);
        Mockito.verify(templateDao).insert(template);
    }

    @Test
    public void testCreate_WhenInsertFails_ThrowsException() {
        TemplateEntity template = new TemplateEntity();
        Mockito.when(templateDao.insert(template)).thenReturn(0);
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Insert template failed");

        templateMgmtService.create(template);
    }

    @Test
    public void testUpdate_RequeriesAndReturnsLatest() {
        TemplateEntity template = new TemplateEntity();
        template.setId(5);
        TemplateEntity refreshed = new TemplateEntity();
        Mockito.when(templateDao.update(template)).thenReturn(1);
        Mockito.when(templateDao.findById(5L)).thenReturn(refreshed);

        TemplateEntity result = templateMgmtService.update(template);

        assertSame(refreshed, result);
        Mockito.verify(templateDao).findById(5L);
    }
}

