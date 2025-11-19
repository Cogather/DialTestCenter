/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.entity;

import com.huawei.cloududn.dialingtestapp.entity.basicDataManage.PreprocessRulePackageEntity;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * PreprocessRulePackageEntity 单元测试
 * 
 * @author g00940940
 * @since 2025-01-27
 */
public class PreprocessRulePackageEntityTest {

    @Test
    public void testDefaultConstructor_CreatesEmptyEntity() {
        // Act
        PreprocessRulePackageEntity entity = new PreprocessRulePackageEntity();

        // Assert
        assertNotNull(entity);
        assertNull(entity.getId());
        assertNull(entity.getPackageName());
        assertNull(entity.getBusinessZh());
        assertNull(entity.getBusinessEn());
        assertNull(entity.getFileContent());
        assertNull(entity.getFileSize());
        assertNull(entity.getDescription());
    }

    @Test
    public void testParameterizedConstructor_SetsAllFields() {
        // Arrange
        String packageName = "test-package";
        String businessZh = "测试业务";
        String businessEn = "TEST_BUSINESS";
        byte[] fileContent = "test content".getBytes();
        Long fileSize = 1024L;
        String description = "测试描述";

        // Act
        PreprocessRulePackageEntity entity = new PreprocessRulePackageEntity(
            packageName, businessZh, businessEn, fileContent, fileSize, description);

        // Assert
        assertNotNull(entity);
        assertEquals(packageName, entity.getPackageName());
        assertEquals(businessZh, entity.getBusinessZh());
        assertEquals(businessEn, entity.getBusinessEn());
        assertArrayEquals(fileContent, entity.getFileContent());
        assertEquals(fileSize, entity.getFileSize());
        assertEquals(description, entity.getDescription());
    }

    @Test
    public void testSettersAndGetters_WorkCorrectly() {
        // Arrange
        PreprocessRulePackageEntity entity = new PreprocessRulePackageEntity();
        Long id = 1L;
        String packageName = "test-package";
        String businessZh = "测试业务";
        String businessEn = "TEST_BUSINESS";
        byte[] fileContent = "test content".getBytes();
        Long fileSize = 1024L;
        String description = "测试描述";

        // Act
        entity.setId(id);
        entity.setPackageName(packageName);
        entity.setBusinessZh(businessZh);
        entity.setBusinessEn(businessEn);
        entity.setFileContent(fileContent);
        entity.setFileSize(fileSize);
        entity.setDescription(description);

        // Assert
        assertEquals(id, entity.getId());
        assertEquals(packageName, entity.getPackageName());
        assertEquals(businessZh, entity.getBusinessZh());
        assertEquals(businessEn, entity.getBusinessEn());
        assertArrayEquals(fileContent, entity.getFileContent());
        assertEquals(fileSize, entity.getFileSize());
        assertEquals(description, entity.getDescription());
    }

    @Test
    public void testEquals_SameId_ReturnsTrue() {
        // Arrange
        PreprocessRulePackageEntity entity1 = new PreprocessRulePackageEntity();
        entity1.setId(1L);
        entity1.setPackageName("package1");

        PreprocessRulePackageEntity entity2 = new PreprocessRulePackageEntity();
        entity2.setId(1L);
        entity2.setPackageName("package2"); // 不同的包名

        // Act & Assert
        assertTrue(entity1.equals(entity2));
    }

    @Test
    public void testEquals_DifferentId_ReturnsFalse() {
        // Arrange
        PreprocessRulePackageEntity entity1 = new PreprocessRulePackageEntity();
        entity1.setId(1L);

        PreprocessRulePackageEntity entity2 = new PreprocessRulePackageEntity();
        entity2.setId(2L);

        // Act & Assert
        assertFalse(entity1.equals(entity2));
    }

    @Test
    public void testEquals_SameObject_ReturnsTrue() {
        // Arrange
        PreprocessRulePackageEntity entity = new PreprocessRulePackageEntity();
        entity.setId(1L);

        // Act & Assert
        assertTrue(entity.equals(entity));
    }

    @Test
    public void testEquals_NullObject_ReturnsFalse() {
        // Arrange
        PreprocessRulePackageEntity entity = new PreprocessRulePackageEntity();
        entity.setId(1L);

        // Act & Assert
        assertFalse(entity.equals(null));
    }

    @Test
    public void testEquals_DifferentClass_ReturnsFalse() {
        // Arrange
        PreprocessRulePackageEntity entity = new PreprocessRulePackageEntity();
        entity.setId(1L);

        String differentObject = "different";

        // Act & Assert
        assertFalse(entity.equals(differentObject));
    }

    @Test
    public void testEquals_BothNullId_ReturnsTrue() {
        // Arrange
        PreprocessRulePackageEntity entity1 = new PreprocessRulePackageEntity();
        PreprocessRulePackageEntity entity2 = new PreprocessRulePackageEntity();

        // Act & Assert
        assertTrue(entity1.equals(entity2));
    }

    @Test
    public void testHashCode_SameId_ReturnsSameHashCode() {
        // Arrange
        PreprocessRulePackageEntity entity1 = new PreprocessRulePackageEntity();
        entity1.setId(1L);

        PreprocessRulePackageEntity entity2 = new PreprocessRulePackageEntity();
        entity2.setId(1L);

        // Act & Assert
        assertEquals(entity1.hashCode(), entity2.hashCode());
    }

    @Test
    public void testHashCode_DifferentId_ReturnsDifferentHashCode() {
        // Arrange
        PreprocessRulePackageEntity entity1 = new PreprocessRulePackageEntity();
        entity1.setId(1L);

        PreprocessRulePackageEntity entity2 = new PreprocessRulePackageEntity();
        entity2.setId(2L);

        // Act & Assert
        assertNotEquals(entity1.hashCode(), entity2.hashCode());
    }

    @Test
    public void testHashCode_NullId_ReturnsObjectsHashOfNull() {
        // Arrange
        PreprocessRulePackageEntity entity = new PreprocessRulePackageEntity();

        // Act
        int hashCode = entity.hashCode();

        // Assert
        assertEquals(java.util.Objects.hash((Object) null), hashCode);
    }

    @Test
    public void testToString_ContainsAllFields() {
        // Arrange
        PreprocessRulePackageEntity entity = new PreprocessRulePackageEntity();
        entity.setId(1L);
        entity.setPackageName("test-package");
        entity.setBusinessZh("测试业务");
        entity.setBusinessEn("TEST_BUSINESS");
        entity.setFileSize(1024L);
        entity.setDescription("测试描述");

        // Act
        String toString = entity.toString();

        // Assert
        assertNotNull(toString);
        assertTrue(toString.contains("id=1"));
        assertTrue(toString.contains("packageName='test-package'"));
        assertTrue(toString.contains("businessZh='测试业务'"));
        assertTrue(toString.contains("businessEn='TEST_BUSINESS'"));
        assertTrue(toString.contains("fileSize=1024"));
        assertTrue(toString.contains("description='测试描述'"));
    }

    @Test
    public void testToString_NullFields_HandlesGracefully() {
        // Arrange
        PreprocessRulePackageEntity entity = new PreprocessRulePackageEntity();

        // Act
        String toString = entity.toString();

        // Assert
        assertNotNull(toString);
        assertTrue(toString.contains("id=null"));
        assertTrue(toString.contains("packageName='null'"));
        assertTrue(toString.contains("businessZh='null'"));
        assertTrue(toString.contains("businessEn='null'"));
        assertTrue(toString.contains("fileSize=null"));
        assertTrue(toString.contains("description='null'"));
    }

    @Test
    public void testFileContent_SetAndGet_WorksCorrectly() {
        // Arrange
        PreprocessRulePackageEntity entity = new PreprocessRulePackageEntity();
        byte[] originalContent = "original content".getBytes();
        byte[] newContent = "new content".getBytes();

        // Act
        entity.setFileContent(originalContent);
        byte[] retrievedContent = entity.getFileContent();

        // Assert
        assertArrayEquals(originalContent, retrievedContent);

        // Act - 修改内容
        entity.setFileContent(newContent);
        retrievedContent = entity.getFileContent();

        // Assert
        assertArrayEquals(newContent, retrievedContent);
    }

    @Test
    public void testFileContent_SetNull_HandlesCorrectly() {
        // Arrange
        PreprocessRulePackageEntity entity = new PreprocessRulePackageEntity();
        entity.setFileContent("test".getBytes());

        // Act
        entity.setFileContent(null);

        // Assert
        assertNull(entity.getFileContent());
    }

    @Test
    public void testFileSize_SetAndGet_WorksCorrectly() {
        // Arrange
        PreprocessRulePackageEntity entity = new PreprocessRulePackageEntity();
        Long originalSize = 1024L;
        Long newSize = 2048L;

        // Act
        entity.setFileSize(originalSize);
        Long retrievedSize = entity.getFileSize();

        // Assert
        assertEquals(originalSize, retrievedSize);

        // Act - 修改大小
        entity.setFileSize(newSize);
        retrievedSize = entity.getFileSize();

        // Assert
        assertEquals(newSize, retrievedSize);
    }

    @Test
    public void testFileSize_SetNull_HandlesCorrectly() {
        // Arrange
        PreprocessRulePackageEntity entity = new PreprocessRulePackageEntity();
        entity.setFileSize(1024L);

        // Act
        entity.setFileSize(null);

        // Assert
        assertNull(entity.getFileSize());
    }
}
