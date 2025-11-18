/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.service.executormanagement;

import com.huawei.cloududn.dialingtestapp.dao.executormanagement.ExecutorDao;
import com.huawei.cloududn.dialingtestapp.dao.executormanagement.UeDao;
import com.huawei.cloududn.dialingtest.model.Executor;
import com.huawei.cloududn.dialingtest.model.Ue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * ExecutorSelectionService单元测试
 *
 * @author g00940940
 * @since 2025-11-09
 */
@RunWith(MockitoJUnitRunner.class)
public class ExecutorSelectionServiceTest {
    @Mock
    private ExecutorDao executorDao;

    @Mock
    private UeDao ueDao;

    @InjectMocks
    private ExecutorSelectionService selectionService;

    private Executor testExecutor;
    private Ue idleUe;
    private Ue busyUe;

    @Before
    public void setUp() {
        testExecutor = new Executor();
        testExecutor.setName("executor1");
        testExecutor.setStatus(1);
        idleUe = new Ue();
        idleUe.setMsisdn("8613800138000");
        idleUe.setExecutorName("executor1");
        idleUe.setInfo("{\"serial\":\"SN001\",\"status\":\"idle\"}");
        busyUe = new Ue();
        busyUe.setMsisdn("8613800138001");
        busyUe.setExecutorName("executor1");
        busyUe.setInfo("{\"serial\":\"SN002\",\"status\":\"busy\"}");
    }

    @Test
    public void testSelectIdleExecutorAndUe_Success() {
        List<Executor> executors = new ArrayList<>();
        executors.add(testExecutor);
        Mockito.when(executorDao.findPage(1, null, 0, 100)).thenReturn(executors);
        List<Ue> ueList = new ArrayList<>();
        ueList.add(idleUe);
        Mockito.when(ueDao.findByExecutorName("executor1")).thenReturn(ueList);
        ExecutorSelectionService.ExecutorUeInfo result = selectionService.selectIdleExecutorAndUe();
        assertNotNull(result);
        assertEquals("executor1", result.getExecutor().getName());
        assertEquals("8613800138000", result.getUe().getMsisdn());
    }

    @Test
    public void testSelectIdleExecutorAndUe_NoOnlineExecutors() {
        Mockito.when(executorDao.findPage(1, null, 0, 100)).thenReturn(new ArrayList<>());
        ExecutorSelectionService.ExecutorUeInfo result = selectionService.selectIdleExecutorAndUe();
        assertNull(result);
    }

    @Test
    public void testSelectIdleExecutorAndUe_NoIdleUe() {
        List<Executor> executors = new ArrayList<>();
        executors.add(testExecutor);
        Mockito.when(executorDao.findPage(1, null, 0, 100)).thenReturn(executors);
        List<Ue> ueList = new ArrayList<>();
        ueList.add(busyUe);
        Mockito.when(ueDao.findByExecutorName("executor1")).thenReturn(ueList);
        ExecutorSelectionService.ExecutorUeInfo result = selectionService.selectIdleExecutorAndUe();
        assertNull(result);
    }

    @Test
    public void testSelectIdleExecutorAndUe_NoUeForExecutor() {
        List<Executor> executors = new ArrayList<>();
        executors.add(testExecutor);
        Mockito.when(executorDao.findPage(1, null, 0, 100)).thenReturn(executors);
        Mockito.when(ueDao.findByExecutorName("executor1")).thenReturn(new ArrayList<>());
        ExecutorSelectionService.ExecutorUeInfo result = selectionService.selectIdleExecutorAndUe();
        assertNull(result);
    }

    @Test
    public void testSelectIdleExecutorAndUe_InvalidUeInfo() {
        List<Executor> executors = new ArrayList<>();
        executors.add(testExecutor);
        Mockito.when(executorDao.findPage(1, null, 0, 100)).thenReturn(executors);
        Ue invalidUe = new Ue();
        invalidUe.setMsisdn("8613800138002");
        invalidUe.setInfo("{invalid json");
        List<Ue> ueList = new ArrayList<>();
        ueList.add(invalidUe);
        Mockito.when(ueDao.findByExecutorName("executor1")).thenReturn(ueList);
        ExecutorSelectionService.ExecutorUeInfo result = selectionService.selectIdleExecutorAndUe();
        assertNull(result);
    }

    @Test
    public void testSelectIdleExecutorAndUe_MultipleExecutors_SelectsBestCandidate() {
        // 创建多个执行机
        Executor executor1 = new Executor();
        executor1.setName("executor1");
        executor1.setStatus(1);
        executor1.setIp("192.168.1.1");
        executor1.setLastOnlineTime("2025-11-18T10:00:00Z");

        Executor executor2 = new Executor();
        executor2.setName("executor2");
        executor2.setStatus(1);
        executor2.setIp("192.168.1.2");
        executor2.setLastOnlineTime("2025-11-18T09:00:00Z");

        List<Executor> executors = new ArrayList<>();
        executors.add(executor1);
        executors.add(executor2);
        Mockito.when(executorDao.findPage(1, null, 0, 100)).thenReturn(executors);

        // executor1 有5个空闲UE（高负载得分）
        List<Ue> ueList1 = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Ue ue = new Ue();
            ue.setMsisdn("86138001380" + i);
            ue.setExecutorName("executor1");
            ue.setInfo("{\"status\":\"idle\"}");
            ueList1.add(ue);
        }
        Mockito.when(ueDao.findByExecutorName("executor1")).thenReturn(ueList1);

        // executor2 只有1个空闲UE（低负载得分）
        List<Ue> ueList2 = new ArrayList<>();
        Ue ue2 = new Ue();
        ue2.setMsisdn("8613800138010");
        ue2.setExecutorName("executor2");
        ue2.setInfo("{\"status\":\"idle\"}");
        ueList2.add(ue2);
        Mockito.when(ueDao.findByExecutorName("executor2")).thenReturn(ueList2);

        // 执行选择
        ExecutorSelectionService.ExecutorUeInfo result = selectionService.selectIdleExecutorAndUe();

        // 验证结果 - 应该选择executor1（更多空闲UE，更高的优先级得分）
        assertNotNull(result);
        assertEquals("executor1", result.getExecutor().getName());
    }

    @Test
    public void testSelectIdleExecutorAndUe_MixedIdleAndBusyUe() {
        List<Executor> executors = new ArrayList<>();
        executors.add(testExecutor);
        Mockito.when(executorDao.findPage(1, null, 0, 100)).thenReturn(executors);

        // 混合空闲和忙碌的UE
        List<Ue> ueList = new ArrayList<>();
        ueList.add(idleUe);
        ueList.add(busyUe);
        Mockito.when(ueDao.findByExecutorName("executor1")).thenReturn(ueList);

        ExecutorSelectionService.ExecutorUeInfo result = selectionService.selectIdleExecutorAndUe();

        // 应该只选择空闲的UE
        assertNotNull(result);
        assertEquals("8613800138000", result.getUe().getMsisdn());
    }

    @Test
    public void testSelectIdleExecutorAndUe_NullExecutorsList() {
        Mockito.when(executorDao.findPage(1, null, 0, 100)).thenReturn(null);
        ExecutorSelectionService.ExecutorUeInfo result = selectionService.selectIdleExecutorAndUe();
        assertNull(result);
    }

    @Test
    public void testSelectIdleExecutorAndUe_EmptyUeInfo() {
        List<Executor> executors = new ArrayList<>();
        executors.add(testExecutor);
        Mockito.when(executorDao.findPage(1, null, 0, 100)).thenReturn(executors);

        Ue emptyInfoUe = new Ue();
        emptyInfoUe.setMsisdn("8613800138003");
        emptyInfoUe.setExecutorName("executor1");
        emptyInfoUe.setInfo("");  // 空info字段

        List<Ue> ueList = new ArrayList<>();
        ueList.add(emptyInfoUe);
        Mockito.when(ueDao.findByExecutorName("executor1")).thenReturn(ueList);

        ExecutorSelectionService.ExecutorUeInfo result = selectionService.selectIdleExecutorAndUe();
        assertNull(result);  // 空info应该被过滤掉
    }

    @Test
    public void testSelectIdleExecutorAndUe_NullUeInfo() {
        List<Executor> executors = new ArrayList<>();
        executors.add(testExecutor);
        Mockito.when(executorDao.findPage(1, null, 0, 100)).thenReturn(executors);

        Ue nullInfoUe = new Ue();
        nullInfoUe.setMsisdn("8613800138004");
        nullInfoUe.setExecutorName("executor1");
        nullInfoUe.setInfo(null);  // null info字段

        List<Ue> ueList = new ArrayList<>();
        ueList.add(nullInfoUe);
        Mockito.when(ueDao.findByExecutorName("executor1")).thenReturn(ueList);

        ExecutorSelectionService.ExecutorUeInfo result = selectionService.selectIdleExecutorAndUe();
        assertNull(result);  // null info应该被过滤掉
    }

    @Test
    public void testSelectIdleExecutorAndUe_UeWithNoStatusField() {
        List<Executor> executors = new ArrayList<>();
        executors.add(testExecutor);
        Mockito.when(executorDao.findPage(1, null, 0, 100)).thenReturn(executors);

        Ue noStatusUe = new Ue();
        noStatusUe.setMsisdn("8613800138005");
        noStatusUe.setExecutorName("executor1");
        noStatusUe.setInfo("{\"other_field\":\"value\"}");  // 没有status字段

        List<Ue> ueList = new ArrayList<>();
        ueList.add(noStatusUe);
        Mockito.when(ueDao.findByExecutorName("executor1")).thenReturn(ueList);

        ExecutorSelectionService.ExecutorUeInfo result = selectionService.selectIdleExecutorAndUe();
        assertNull(result);  // 没有status字段应该被视为非空闲
    }

    @Test
    public void testSelectIdleExecutorAndUe_MultipleIdleUes_RandomSelection() {
        List<Executor> executors = new ArrayList<>();
        executors.add(testExecutor);
        Mockito.when(executorDao.findPage(1, null, 0, 100)).thenReturn(executors);

        // 创建多个空闲UE
        List<Ue> ueList = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Ue ue = new Ue();
            ue.setMsisdn("86138001380" + (i + 10));
            ue.setExecutorName("executor1");
            ue.setInfo("{\"status\":\"idle\"}");
            ueList.add(ue);
        }
        Mockito.when(ueDao.findByExecutorName("executor1")).thenReturn(ueList);

        ExecutorSelectionService.ExecutorUeInfo result = selectionService.selectIdleExecutorAndUe();

        // 应该从空闲UE中选择一个
        assertNotNull(result);
        assertEquals("executor1", result.getExecutor().getName());
        assertNotNull(result.getUe());
    }

    @Test
    public void testSelectIdleExecutorAndUe_ExecutorWithoutIp() {
        // 测试没有IP地址的执行机（影响健康度评分）
        Executor executorNoIp = new Executor();
        executorNoIp.setName("executor-no-ip");
        executorNoIp.setStatus(1);
        executorNoIp.setIp(null);  // 无IP地址

        List<Executor> executors = new ArrayList<>();
        executors.add(executorNoIp);
        Mockito.when(executorDao.findPage(1, null, 0, 100)).thenReturn(executors);

        List<Ue> ueList = new ArrayList<>();
        ueList.add(idleUe);
        Mockito.when(ueDao.findByExecutorName("executor-no-ip")).thenReturn(ueList);

        ExecutorSelectionService.ExecutorUeInfo result = selectionService.selectIdleExecutorAndUe();

        // 即使没有IP，只要有空闲UE也应该能选中
        assertNotNull(result);
        assertEquals("executor-no-ip", result.getExecutor().getName());
    }

    @Test
    public void testSelectIdleExecutorAndUe_ExecutorWithNullLastOnlineTime() {
        // 测试lastOnlineTime为null的执行机
        Executor executorNullTime = new Executor();
        executorNullTime.setName("executor-null-time");
        executorNullTime.setStatus(1);
        executorNullTime.setIp("192.168.1.1");
        executorNullTime.setLastOnlineTime(null);  // null lastOnlineTime

        List<Executor> executors = new ArrayList<>();
        executors.add(executorNullTime);
        Mockito.when(executorDao.findPage(1, null, 0, 100)).thenReturn(executors);

        List<Ue> ueList = new ArrayList<>();
        ueList.add(idleUe);
        Mockito.when(ueDao.findByExecutorName("executor-null-time")).thenReturn(ueList);

        ExecutorSelectionService.ExecutorUeInfo result = selectionService.selectIdleExecutorAndUe();

        // 即使lastOnlineTime为null，也应该能选中
        assertNotNull(result);
        assertEquals("executor-null-time", result.getExecutor().getName());
    }
}

