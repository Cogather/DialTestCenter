/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.service.executormanagement.dto;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * TaskDispatchRequest单元测试类
 * 测试任务分发请求DTO的所有功能
 *
 * @author g00940940
 * @since 2025-11-18
 */
public class TaskDispatchRequestTest {
    /**
     * 测试默认构造器
     * 验证默认构造器创建的对象所有字段均为null
     */
    @Test
    public void testDefaultConstructor_AllFieldsNull_ShouldBeNull() {
        TaskDispatchRequest defaultRequest = new TaskDispatchRequest();
        assertNotNull(defaultRequest);
        assertNull(defaultRequest.getExecutorName());
        assertNull(defaultRequest.getTaskId());
        assertNull(defaultRequest.getScriptName());
        assertNull(defaultRequest.getVersion());
        assertNull(defaultRequest.getSerialNoList());
        assertNull(defaultRequest.getProctype());
        assertNull(defaultRequest.getParameters());
    }

    /**
     * 测试参数化构造器
     * 验证参数化构造器正确设置所有字段
     */
    @Test
    public void testParameterizedConstructor_AllFields_ShouldBeSet() {
        String testExecutorName = "Executor-001";
        Integer testTaskId = 200;
        String testScriptName = "dialtest_script";
        String testVersion = "v1.0.0";
        List<String> testSerialNoList = new ArrayList<>();
        testSerialNoList.add("SN001");
        testSerialNoList.add("SN002");
        testSerialNoList.add("SN003");
        String testProctype = "TYPE_A";
        String testParameters = "{\"key\":\"value\"}";

        TaskDispatchRequest request = new TaskDispatchRequest(testExecutorName, testTaskId,
                testScriptName, testVersion, testSerialNoList, testProctype, testParameters);
        assertEquals(testExecutorName, request.getExecutorName());
        assertEquals(testTaskId, request.getTaskId());
        assertEquals(testScriptName, request.getScriptName());
        assertEquals(testVersion, request.getVersion());
        assertEquals(testSerialNoList, request.getSerialNoList());
        assertEquals(testProctype, request.getProctype());
        assertEquals(testParameters, request.getParameters());
    }

    /**
     * 测试Setter和Getter方法
     * 验证所有setter方法正确设置字段值
     */
    @Test
    public void testSettersAndGetters_AllFields_ShouldBeSet() {
        String testExecutorName = "Executor-001";
        Integer testTaskId = 200;
        String testScriptName = "dialtest_script";
        String testVersion = "v1.0.0";
        List<String> testSerialNoList = new ArrayList<>();
        testSerialNoList.add("SN001");
        testSerialNoList.add("SN002");
        testSerialNoList.add("SN003");
        String testProctype = "TYPE_A";
        String testParameters = "{\"key\":\"value\"}";

        TaskDispatchRequest setterRequest = new TaskDispatchRequest();
        setterRequest.setExecutorName(testExecutorName);
        setterRequest.setTaskId(testTaskId);
        setterRequest.setScriptName(testScriptName);
        setterRequest.setVersion(testVersion);
        setterRequest.setSerialNoList(testSerialNoList);
        setterRequest.setProctype(testProctype);
        setterRequest.setParameters(testParameters);
        assertEquals(testExecutorName, setterRequest.getExecutorName());
        assertEquals(testTaskId, setterRequest.getTaskId());
        assertEquals(testScriptName, setterRequest.getScriptName());
        assertEquals(testVersion, setterRequest.getVersion());
        assertEquals(testSerialNoList, setterRequest.getSerialNoList());
        assertEquals(testProctype, setterRequest.getProctype());
        assertEquals(testParameters, setterRequest.getParameters());
    }

    /**
     * 测试toString方法
     * 验证toString正确格式化所有字段
     */
    @Test
    public void testToString_AllFields_ShouldFormatCorrectly() {
        String testExecutorName = "Executor-001";
        Integer testTaskId = 200;
        String testScriptName = "dialtest_script";
        String testVersion = "v1.0.0";
        List<String> testSerialNoList = new ArrayList<>();
        testSerialNoList.add("SN001");
        testSerialNoList.add("SN002");
        testSerialNoList.add("SN003");
        String testProctype = "TYPE_A";
        String testParameters = "{\"key\":\"value\"}";

        TaskDispatchRequest request = new TaskDispatchRequest(testExecutorName, testTaskId,
                testScriptName, testVersion, testSerialNoList, testProctype, testParameters);
        String result = request.toString();
        assertTrue(result.contains("executorName='" + testExecutorName + "'"));
        assertTrue(result.contains("taskId=" + testTaskId));
        assertTrue(result.contains("scriptName='" + testScriptName + "'"));
        assertTrue(result.contains("version='" + testVersion + "'"));
        assertTrue(result.contains("serialNoList=" + testSerialNoList));
        assertTrue(result.contains("proctype='" + testProctype + "'"));
        assertTrue(result.contains("parameters='" + testParameters + "'"));
    }
}

