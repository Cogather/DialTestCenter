/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.service.executormanagement.dto;

import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.UeItemDto;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * ExecutorDetailDto单元测试类
 * 测试执行机详情DTO的所有功能
 *
 * @author g00940940
 * @since 2025-11-18
 */
public class ExecutorDetailDtoTest {
    @Test
    public void testExecutorDetailDto_AllFunctionality_ShouldWorkCorrectly() {
        String testName = "Executor-001";
        String testIp = "192.168.1.100";
        Integer testStatus = 1;
        String testLastOnlineTime = "2025-11-18 10:00:00";
        List<UeItemDto> testUeList = new ArrayList<>();
        UeItemDto ueItem1 = new UeItemDto();
        UeItemDto ueItem2 = new UeItemDto();
        testUeList.add(ueItem1);
        testUeList.add(ueItem2);

        ExecutorDetailDto defaultDto = new ExecutorDetailDto();
        assertNotNull(defaultDto);
        assertNull(defaultDto.getName());
        assertNull(defaultDto.getIp());
        assertNull(defaultDto.getStatus());
        assertNull(defaultDto.getLastOnlineTime());
        assertNull(defaultDto.getUeList());

        ExecutorDetailDto dto = new ExecutorDetailDto(testName, testIp, testStatus,
                testLastOnlineTime, testUeList);
        assertEquals(testName, dto.getName());
        assertEquals(testIp, dto.getIp());
        assertEquals(testStatus, dto.getStatus());
        assertEquals(testLastOnlineTime, dto.getLastOnlineTime());
        assertEquals(testUeList, dto.getUeList());
        assertEquals(2, dto.getUeList().size());

        ExecutorDetailDto setterDto = new ExecutorDetailDto();
        setterDto.setName(testName);
        setterDto.setIp(testIp);
        setterDto.setStatus(testStatus);
        setterDto.setLastOnlineTime(testLastOnlineTime);
        setterDto.setUeList(testUeList);
        assertEquals(testName, setterDto.getName());
        assertEquals(testIp, setterDto.getIp());
        assertEquals(testStatus, setterDto.getStatus());
        assertEquals(testLastOnlineTime, setterDto.getLastOnlineTime());
        assertEquals(testUeList, setterDto.getUeList());

        String result = dto.toString();
        assertTrue(result.contains("name='" + testName + "'"));
        assertTrue(result.contains("ip='" + testIp + "'"));
        assertTrue(result.contains("status=" + testStatus));
        assertTrue(result.contains("lastOnlineTime=" + testLastOnlineTime));
        assertTrue(result.contains("ueList=" + testUeList.size() + " items"));

        ExecutorDetailDto nullUeListDto = new ExecutorDetailDto();
        nullUeListDto.setName(testName);
        nullUeListDto.setUeList(null);
        String nullResult = nullUeListDto.toString();
        assertTrue(nullResult.contains("ueList=0 items"));
    }
}

