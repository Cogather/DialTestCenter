/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.service.executormanagement.dto;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * AppInstallRequest单元测试类
 * 测试App安装请求DTO的所有功能
 *
 * @author g00940940
 * @since 2025-11-18
 */
public class AppInstallRequestTest {
    @Test
    public void testAppInstallRequest_AllFunctionality_ShouldWorkCorrectly() {
        String testSerialNo = "SN123456";
        Integer testTaskId = 100;
        String testAppName = "TestApp";
        byte[] testScript = new byte[]{1, 2, 3, 4, 5};
        byte[] testPackageFile = new byte[]{10, 20, 30, 40, 50};
        String testCrc = "ABC123";

        AppInstallRequest defaultRequest = new AppInstallRequest();
        assertNotNull(defaultRequest);
        assertNull(defaultRequest.getSerialNo());
        assertNull(defaultRequest.getTaskId());
        assertNull(defaultRequest.getAppName());
        assertNull(defaultRequest.getScript());
        assertNull(defaultRequest.getPackageFile());
        assertNull(defaultRequest.getCrc());

        AppInstallRequest request = new AppInstallRequest(testSerialNo, testTaskId,
                testAppName, testScript, testPackageFile, testCrc);
        assertEquals(testSerialNo, request.getSerialNo());
        assertEquals(testTaskId, request.getTaskId());
        assertEquals(testAppName, request.getAppName());
        assertEquals(testScript, request.getScript());
        assertEquals(testPackageFile, request.getPackageFile());
        assertEquals(testCrc, request.getCrc());

        AppInstallRequest setterRequest = new AppInstallRequest();
        setterRequest.setSerialNo(testSerialNo);
        setterRequest.setTaskId(testTaskId);
        setterRequest.setAppName(testAppName);
        setterRequest.setScript(testScript);
        setterRequest.setPackageFile(testPackageFile);
        setterRequest.setCrc(testCrc);
        assertEquals(testSerialNo, setterRequest.getSerialNo());
        assertEquals(testTaskId, setterRequest.getTaskId());
        assertEquals(testAppName, setterRequest.getAppName());
        assertEquals(testScript, setterRequest.getScript());
        assertEquals(testPackageFile, setterRequest.getPackageFile());
        assertEquals(testCrc, setterRequest.getCrc());

        assertTrue(request.isApkInstall());
        assertTrue(request.isScriptInstall());

        AppInstallRequest emptyPackageRequest = new AppInstallRequest();
        emptyPackageRequest.setPackageFile(new byte[0]);
        assertFalse(emptyPackageRequest.isApkInstall());

        AppInstallRequest nullPackageRequest = new AppInstallRequest();
        nullPackageRequest.setPackageFile(null);
        assertFalse(nullPackageRequest.isApkInstall());

        AppInstallRequest emptyScriptRequest = new AppInstallRequest();
        emptyScriptRequest.setScript(new byte[0]);
        assertFalse(emptyScriptRequest.isScriptInstall());

        AppInstallRequest nullScriptRequest = new AppInstallRequest();
        nullScriptRequest.setScript(null);
        assertFalse(nullScriptRequest.isScriptInstall());

        String result = request.toString();
        assertTrue(result.contains("serialNo='" + testSerialNo + "'"));
        assertTrue(result.contains("taskId=" + testTaskId));
        assertTrue(result.contains("appName='" + testAppName + "'"));
        assertTrue(result.contains("script=" + testScript.length + " bytes"));
        assertTrue(result.contains("packageFile=" + testPackageFile.length + " bytes"));
        assertTrue(result.contains("crc='" + testCrc + "'"));

        AppInstallRequest nullArrayRequest = new AppInstallRequest();
        nullArrayRequest.setSerialNo(testSerialNo);
        nullArrayRequest.setScript(null);
        nullArrayRequest.setPackageFile(null);
        String nullResult = nullArrayRequest.toString();
        assertTrue(nullResult.contains("script=0 bytes"));
        assertTrue(nullResult.contains("packageFile=0 bytes"));
    }
}

