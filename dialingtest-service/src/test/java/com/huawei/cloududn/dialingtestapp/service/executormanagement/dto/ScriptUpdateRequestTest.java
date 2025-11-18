/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.service.executormanagement.dto;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * ScriptUpdateRequest单元测试类
 * 测试脚本更新请求DTO的所有功能
 *
 * @author g00940940
 * @since 2025-11-18
 */
public class ScriptUpdateRequestTest {
    @Test
    public void testScriptUpdateRequest_AllFunctionality_ShouldWorkCorrectly() {
        String testScriptName = "dialtest_script";
        String testVersion = "v1.0.0";
        byte[] testScriptFile = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        String testCrc = "CRC123456";

        ScriptUpdateRequest defaultRequest = new ScriptUpdateRequest();
        assertNotNull(defaultRequest);
        assertNull(defaultRequest.getScriptName());
        assertNull(defaultRequest.getVersion());
        assertNull(defaultRequest.getScriptFile());
        assertNull(defaultRequest.getCrc());

        ScriptUpdateRequest request = new ScriptUpdateRequest(testScriptName, testVersion,
                testScriptFile, testCrc);
        assertEquals(testScriptName, request.getScriptName());
        assertEquals(testVersion, request.getVersion());
        assertEquals(testScriptFile, request.getScriptFile());
        assertEquals(testCrc, request.getCrc());

        ScriptUpdateRequest setterRequest = new ScriptUpdateRequest();
        setterRequest.setScriptName(testScriptName);
        setterRequest.setVersion(testVersion);
        setterRequest.setScriptFile(testScriptFile);
        setterRequest.setCrc(testCrc);
        assertEquals(testScriptName, setterRequest.getScriptName());
        assertEquals(testVersion, setterRequest.getVersion());
        assertEquals(testScriptFile, setterRequest.getScriptFile());
        assertEquals(testCrc, setterRequest.getCrc());

        String result = request.toString();
        assertTrue(result.contains("scriptName='" + testScriptName + "'"));
        assertTrue(result.contains("version='" + testVersion + "'"));
        assertTrue(result.contains("scriptFile=" + testScriptFile.length + " bytes"));
        assertTrue(result.contains("crc='" + testCrc + "'"));

        ScriptUpdateRequest nullScriptFileRequest = new ScriptUpdateRequest();
        nullScriptFileRequest.setScriptName(testScriptName);
        nullScriptFileRequest.setVersion(testVersion);
        nullScriptFileRequest.setScriptFile(null);
        nullScriptFileRequest.setCrc(testCrc);
        String nullResult = nullScriptFileRequest.toString();
        assertTrue(nullResult.contains("scriptFile=0 bytes"));
    }
}

