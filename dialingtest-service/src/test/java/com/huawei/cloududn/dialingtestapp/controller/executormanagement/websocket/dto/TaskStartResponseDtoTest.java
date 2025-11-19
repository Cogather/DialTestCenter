/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * TaskStartResponseDto UT test
 *
 * @author g00940940
 * @since 2025-11-18
 */
public class TaskStartResponseDtoTest {
    @Test
    public void testTaskStartResponseDto_AllMethods_Success() {
        TaskStartResponseDto dto = new TaskStartResponseDto();
        assertNotNull(dto);
        assertNotNull(dto.getSubResult());
        
        dto.setToken(123456L);
        dto.setTaskId(1001);
        dto.setResult("success");
        dto.setBlock("block1");
        dto.setDescription("test description");
        dto.setFilelen(1024);
        dto.setCrc("abc123");
        
        List<SubResultItemDto> subResults = new ArrayList<>();
        dto.setSubResult(subResults);
        
        assertEquals(123456L, dto.getToken());
        assertEquals(1001, dto.getTaskId());
        assertEquals("success", dto.getResult());
        assertEquals("block1", dto.getBlock());
        assertEquals("test description", dto.getDescription());
        assertEquals(1024, dto.getFilelen());
        assertEquals("abc123", dto.getCrc());
        assertNotNull(dto.getSubResult());
        assertNotNull(dto.toString());
    }
}

