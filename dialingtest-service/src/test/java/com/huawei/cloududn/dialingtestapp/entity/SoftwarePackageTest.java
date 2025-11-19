/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.entity;

import com.huawei.cloududn.dialingtestapp.entity.basicDataManage.SoftwarePackage;
import org.junit.Test;
import org.junit.Before;

import static org.junit.Assert.*;

/**
 * SoftwarePackage实体类单元测试
 *
 * @author g00940940
 * @since 2025-01-27
 */
public class SoftwarePackageTest {
    
    private SoftwarePackage softwarePackage;
    private byte[] testFileContent;
    private String testSoftwareName;
    private String testDescription;
    private String testFileSha256;
    private Long testFileSize;
    
    @Before
    public void setUp() {
        testFileContent = "test file content".getBytes();
        testSoftwareName = "TestApp_1.0.0.apk";
        testDescription = "Test application package";
        testFileSha256 = "a1b2c3d4e5f6789012345678901234567890abcdef1234567890abcdef123456";
        testFileSize = 1024L;
        
        softwarePackage = new SoftwarePackage();
    }
    
    /**
     * 测试默认构造函数
     */
    @Test
    public void testDefaultConstructor_ShouldCreateEmptyObject() {
        // Arrange & Act
        SoftwarePackage emptyPackage = new SoftwarePackage();
        
        // Assert
        assertNotNull("默认构造函数应创建非空对象", emptyPackage);
        assertNull("ID应为null", emptyPackage.getId());
        assertNull("软件名称应为null", emptyPackage.getSoftwareName());
        assertNull("描述应为null", emptyPackage.getDescription());
        assertNull("文件内容应为null", emptyPackage.getFileContent());
        assertNull("SHA256应为null", emptyPackage.getFileSha256());
        assertNull("文件大小应为null", emptyPackage.getFileSize());
    }
    
    /**
     * 测试参数化构造函数
     */
    @Test
    public void testParameterizedConstructor_ShouldSetAllFields() {
        // Arrange & Act
        SoftwarePackage packageWithParams = new SoftwarePackage(
            testSoftwareName, testDescription, testFileContent, testFileSha256, testFileSize);
        
        // Assert
        assertEquals("软件名称应正确设置", testSoftwareName, packageWithParams.getSoftwareName());
        assertEquals("描述应正确设置", testDescription, packageWithParams.getDescription());
        assertArrayEquals("文件内容应正确设置", testFileContent, packageWithParams.getFileContent());
        assertEquals("SHA256应正确设置", testFileSha256, packageWithParams.getFileSha256());
        assertEquals("文件大小应正确设置", testFileSize, packageWithParams.getFileSize());
    }
    
    /**
     * 测试ID的getter和setter
     */
    @Test
    public void testIdGetterSetter_ShouldWorkCorrectly() {
        // Arrange
        Long testId = 1L;
        
        // Act
        softwarePackage.setId(testId);
        
        // Assert
        assertEquals("ID应正确设置和获取", testId, softwarePackage.getId());
    }
    
    /**
     * 测试软件名称的getter和setter
     */
    @Test
    public void testSoftwareNameGetterSetter_ShouldWorkCorrectly() {
        // Act
        softwarePackage.setSoftwareName(testSoftwareName);
        
        // Assert
        assertEquals("软件名称应正确设置和获取", testSoftwareName, softwarePackage.getSoftwareName());
    }
    
    /**
     * 测试描述的getter和setter
     */
    @Test
    public void testDescriptionGetterSetter_ShouldWorkCorrectly() {
        // Act
        softwarePackage.setDescription(testDescription);
        
        // Assert
        assertEquals("描述应正确设置和获取", testDescription, softwarePackage.getDescription());
    }
    
    /**
     * 测试文件内容的getter和setter
     */
    @Test
    public void testFileContentGetterSetter_ShouldWorkCorrectly() {
        // Act
        softwarePackage.setFileContent(testFileContent);
        
        // Assert
        assertArrayEquals("文件内容应正确设置和获取", testFileContent, softwarePackage.getFileContent());
    }
    
    /**
     * 测试SHA256的getter和setter
     */
    @Test
    public void testFileSha256GetterSetter_ShouldWorkCorrectly() {
        // Act
        softwarePackage.setFileSha256(testFileSha256);
        
        // Assert
        assertEquals("SHA256应正确设置和获取", testFileSha256, softwarePackage.getFileSha256());
    }
    
    /**
     * 测试文件大小的getter和setter
     */
    @Test
    public void testFileSizeGetterSetter_ShouldWorkCorrectly() {
        // Act
        softwarePackage.setFileSize(testFileSize);
        
        // Assert
        assertEquals("文件大小应正确设置和获取", testFileSize, softwarePackage.getFileSize());
    }
    
    /**
     * 测试equals方法 - 相同对象
     */
    @Test
    public void testEquals_SameObject_ShouldReturnTrue() {
        // Act & Assert
        assertTrue("相同对象应相等", softwarePackage.equals(softwarePackage));
    }
    
    /**
     * 测试equals方法 - null对象
     */
    @Test
    public void testEquals_NullObject_ShouldReturnFalse() {
        // Act & Assert
        assertFalse("与null比较应返回false", softwarePackage.equals(null));
    }
    
    /**
     * 测试equals方法 - 不同类型对象
     */
    @Test
    public void testEquals_DifferentClass_ShouldReturnFalse() {
        // Arrange
        String differentObject = "different object";
        
        // Act & Assert
        assertFalse("不同类型对象应返回false", softwarePackage.equals(differentObject));
    }
    
    /**
     * 测试equals方法 - 相同ID和软件名称
     */
    @Test
    public void testEquals_SameIdAndName_ShouldReturnTrue() {
        // Arrange
        SoftwarePackage package1 = new SoftwarePackage();
        package1.setId(1L);
        package1.setSoftwareName("TestApp.apk");
        
        SoftwarePackage package2 = new SoftwarePackage();
        package2.setId(1L);
        package2.setSoftwareName("TestApp.apk");
        
        // Act & Assert
        assertTrue("相同ID和软件名称的对象应相等", package1.equals(package2));
    }
    
    /**
     * 测试equals方法 - 不同ID
     */
    @Test
    public void testEquals_DifferentId_ShouldReturnFalse() {
        // Arrange
        SoftwarePackage package1 = new SoftwarePackage();
        package1.setId(1L);
        package1.setSoftwareName("TestApp.apk");
        
        SoftwarePackage package2 = new SoftwarePackage();
        package2.setId(2L);
        package2.setSoftwareName("TestApp.apk");
        
        // Act & Assert
        assertFalse("不同ID的对象应不相等", package1.equals(package2));
    }
    
    /**
     * 测试equals方法 - 不同软件名称
     */
    @Test
    public void testEquals_DifferentName_ShouldReturnFalse() {
        // Arrange
        SoftwarePackage package1 = new SoftwarePackage();
        package1.setId(1L);
        package1.setSoftwareName("TestApp1.apk");
        
        SoftwarePackage package2 = new SoftwarePackage();
        package2.setId(1L);
        package2.setSoftwareName("TestApp2.apk");
        
        // Act & Assert
        assertFalse("不同软件名称的对象应不相等", package1.equals(package2));
    }
    
    /**
     * 测试hashCode方法
     */
    @Test
    public void testHashCode_ShouldReturnConsistentValue() {
        // Arrange
        softwarePackage.setId(1L);
        softwarePackage.setSoftwareName("TestApp.apk");
        
        // Act
        int hashCode1 = softwarePackage.hashCode();
        int hashCode2 = softwarePackage.hashCode();
        
        // Assert
        assertEquals("hashCode应返回一致的值", hashCode1, hashCode2);
    }
    
    /**
     * 测试hashCode方法 - 相同对象应有相同hashCode
     */
    @Test
    public void testHashCode_EqualObjects_ShouldHaveSameHashCode() {
        // Arrange
        SoftwarePackage package1 = new SoftwarePackage();
        package1.setId(1L);
        package1.setSoftwareName("TestApp.apk");
        
        SoftwarePackage package2 = new SoftwarePackage();
        package2.setId(1L);
        package2.setSoftwareName("TestApp.apk");
        
        // Act
        int hashCode1 = package1.hashCode();
        int hashCode2 = package2.hashCode();
        
        // Assert
        assertEquals("相等对象应有相同的hashCode", hashCode1, hashCode2);
    }
    
    /**
     * 测试toString方法
     */
    @Test
    public void testToString_ShouldContainAllFields() {
        // Arrange
        softwarePackage.setId(1L);
        softwarePackage.setSoftwareName(testSoftwareName);
        softwarePackage.setDescription(testDescription);
        softwarePackage.setFileSha256(testFileSha256);
        softwarePackage.setFileSize(testFileSize);
        
        // Act
        String toString = softwarePackage.toString();
        
        // Assert
        assertNotNull("toString不应为null", toString);
        assertTrue("toString应包含ID", toString.contains("id=1"));
        assertTrue("toString应包含软件名称", toString.contains("softwareName='" + testSoftwareName + "'"));
        assertTrue("toString应包含描述", toString.contains("description='" + testDescription + "'"));
        assertTrue("toString应包含SHA256", toString.contains("fileSha256='" + testFileSha256 + "'"));
        assertTrue("toString应包含文件大小", toString.contains("fileSize=" + testFileSize));
    }
    
    /**
     * 测试toString方法 - null值处理
     */
    @Test
    public void testToString_WithNullValues_ShouldHandleGracefully() {
        // Arrange - 使用默认构造函数创建的对象，所有字段为null
        
        // Act
        String toString = softwarePackage.toString();
        
        // Assert
        assertNotNull("toString不应为null", toString);
        assertTrue("toString应包含null值", toString.contains("null"));
    }
}

