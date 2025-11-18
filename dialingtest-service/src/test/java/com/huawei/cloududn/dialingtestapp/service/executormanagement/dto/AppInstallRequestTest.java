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
    /**
     * 测试默认构造器
     * 验证默认构造器创建的对象所有字段均为null
     */
    @Test
    public void testDefaultConstructor_AllFieldsNull_ShouldBeNull() {
        AppInstallRequest defaultRequest = new AppInstallRequest();
        assertNotNull(defaultRequest);
        assertNull(defaultRequest.getSerialNo());
        assertNull(defaultRequest.getTaskId());
        assertNull(defaultRequest.getAppName());
        assertNull(defaultRequest.getScript());
        assertNull(defaultRequest.getPackageFile());
        assertNull(defaultRequest.getCrc());
    }

    /**
     * 测试参数化构造器
     * 验证参数化构造器正确设置所有字段
     */
    @Test
    public void testParameterizedConstructor_AllFields_ShouldBeSet() {
        String testSerialNo = "SN123456";
        Integer testTaskId = 100;
        String testAppName = "TestApp";
        byte[] testScript = new byte[]{1, 2, 3, 4, 5};
        byte[] testPackageFile = new byte[]{10, 20, 30, 40, 50};
        String testCrc = "ABC123";

        AppInstallRequest request = new AppInstallRequest(testSerialNo, testTaskId,
                testAppName, testScript, testPackageFile, testCrc);
        assertEquals(testSerialNo, request.getSerialNo());
        assertEquals(testTaskId, request.getTaskId());
        assertEquals(testAppName, request.getAppName());
        assertEquals(testScript, request.getScript());
        assertEquals(testPackageFile, request.getPackageFile());
        assertEquals(testCrc, request.getCrc());
    }

    /**
     * 测试Setter和Getter方法
     * 验证所有setter方法正确设置字段值
     */
    @Test
    public void testSettersAndGetters_AllFields_ShouldBeSet() {
        String testSerialNo = "SN123456";
        Integer testTaskId = 100;
        String testAppName = "TestApp";
        byte[] testScript = new byte[]{1, 2, 3, 4, 5};
        byte[] testPackageFile = new byte[]{10, 20, 30, 40, 50};
        String testCrc = "ABC123";

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
    }

    /**
     * 测试isApkInstall方法
     * 验证APK安装检查逻辑（非空且长度>0）
     */
    @Test
    public void testIsApkInstall_VariousConditions_ShouldReturnCorrectly() {
        byte[] testPackageFile = new byte[]{10, 20, 30, 40, 50};

        AppInstallRequest requestWithPackage = new AppInstallRequest();
        requestWithPackage.setPackageFile(testPackageFile);
        assertTrue(requestWithPackage.isApkInstall());

        AppInstallRequest emptyPackageRequest = new AppInstallRequest();
        emptyPackageRequest.setPackageFile(new byte[0]);
        assertFalse(emptyPackageRequest.isApkInstall());

        AppInstallRequest nullPackageRequest = new AppInstallRequest();
        nullPackageRequest.setPackageFile(null);
        assertFalse(nullPackageRequest.isApkInstall());
    }

    /**
     * 测试isScriptInstall方法
     * 验证脚本安装检查逻辑（非空且长度>0）
     */
    @Test
    public void testIsScriptInstall_VariousConditions_ShouldReturnCorrectly() {
        byte[] testScript = new byte[]{1, 2, 3, 4, 5};

        AppInstallRequest requestWithScript = new AppInstallRequest();
        requestWithScript.setScript(testScript);
        assertTrue(requestWithScript.isScriptInstall());

        AppInstallRequest emptyScriptRequest = new AppInstallRequest();
        emptyScriptRequest.setScript(new byte[0]);
        assertFalse(emptyScriptRequest.isScriptInstall());

        AppInstallRequest nullScriptRequest = new AppInstallRequest();
        nullScriptRequest.setScript(null);
        assertFalse(nullScriptRequest.isScriptInstall());
    }

    /**
     * 测试toString方法
     * 验证toString正确格式化所有字段，包括数组长度处理
     */
    @Test
    public void testToString_AllFields_ShouldFormatCorrectly() {
        String testSerialNo = "SN123456";
        Integer testTaskId = 100;
        String testAppName = "TestApp";
        byte[] testScript = new byte[]{1, 2, 3, 4, 5};
        byte[] testPackageFile = new byte[]{10, 20, 30, 40, 50};
        String testCrc = "ABC123";

        AppInstallRequest request = new AppInstallRequest(testSerialNo, testTaskId,
                testAppName, testScript, testPackageFile, testCrc);
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

