/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.service.taskmanagement;

import com.huawei.cloududn.dialingtest.model.TemplateEntity;
import com.huawei.cloududn.dialingtestapp.dao.taskmanagement.TemplateDao;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * TemplateMgmtService unit test
 *
 * @author g00940940
 * @since 2025-11-18
 */
@RunWith(MockitoJUnitRunner.class)
public class TemplateMgmtServiceTest {
    @Mock
    private TemplateDao templateDao;
    @InjectMocks
    private TemplateMgmtService templateMgmtService;

    private TemplateEntity mockTemplate;

    @Before
    public void setUp() {
        mockTemplate = new TemplateEntity();
        mockTemplate.setId(1);
        mockTemplate.setName("test-template");
        mockTemplate.setEnabled(true);
    }

    @Test
    public void testCreate_Update_Delete_Template() {
        Mockito.when(templateDao.insert(Mockito.any(TemplateEntity.class))).thenReturn(1);
        Mockito.when(templateDao.update(Mockito.any(TemplateEntity.class))).thenReturn(1);
        Mockito.when(templateDao.findById(Mockito.anyLong())).thenReturn(mockTemplate);
        TemplateEntity entity = new TemplateEntity();
        entity.setId(1);
        entity.setName("new-template");
        TemplateEntity created = templateMgmtService.create(entity);
        assertNotNull(created);
        Mockito.verify(templateDao).insert(entity);
        TemplateEntity updated = templateMgmtService.update(entity);
        assertNotNull(updated);
        Mockito.verify(templateDao).update(entity);
        templateMgmtService.delete(1L);
        Mockito.verify(templateDao).delete(1L);
    }

    @Test
    public void testFindMethods_All_Enabled_ById() {
        List<TemplateEntity> mockTemplates = Collections.singletonList(mockTemplate);
        Mockito.when(templateDao.findAll()).thenReturn(mockTemplates);
        Mockito.when(templateDao.findEnabled()).thenReturn(mockTemplates);
        Mockito.when(templateDao.findById(Mockito.anyLong())).thenReturn(mockTemplate);
        List<TemplateEntity> allResult = templateMgmtService.findAll();
        assertNotNull(allResult);
        assertEquals(1, allResult.size());
        List<TemplateEntity> enabledResult = templateMgmtService.findEnabled();
        assertNotNull(enabledResult);
        assertEquals(1, enabledResult.size());
        TemplateEntity byIdResult = templateMgmtService.findById(1L);
        assertNotNull(byIdResult);
        Mockito.verify(templateDao).findAll();
        Mockito.verify(templateDao).findEnabled();
        Mockito.verify(templateDao).findById(1L);
    }
}

