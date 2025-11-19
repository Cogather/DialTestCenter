/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.entity;

import com.huawei.cloududn.dialingtestapp.entity.basicDataManage.ValidationTask;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.junit.Assert.*;

/**
 * ValidationTask 单元测试
 * 
 * @author g00940940
 * @since 2025-01-27
 */
public class ValidationTaskTest {

    private ValidationTask validationTask;

    @Before
    public void setUp() {
        validationTask = new ValidationTask();
    }

    /**
     * 测试默认构造函数
     */
    @Test
    public void testConstructor_Default_CreatesEmptyObject() {
        // Arrange & Act
        ValidationTask task = new ValidationTask();

        // Assert
        assertNotNull(task);
        assertNull(task.getId());
        assertNull(task.getTestCaseSetId());
        assertNull(task.getTaskId());
        assertNull(task.getStatus());
    }

    /**
     * 测试设置和获取ID
     */
    @Test
    public void testSetAndGetId_ValidId_ReturnsId() {
        // Arrange
        Long id = 1L;

        // Act
        validationTask.setId(id);

        // Assert
        assertEquals(id, validationTask.getId());
    }

    /**
     * 测试设置和获取用例集ID
     */
    @Test
    public void testSetAndGetTestCaseSetId_ValidId_ReturnsId() {
        // Arrange
        Long testCaseSetId = 1L;

        // Act
        validationTask.setTestCaseSetId(testCaseSetId);

        // Assert
        assertEquals(testCaseSetId, validationTask.getTestCaseSetId());
    }

    /**
     * 测试设置和获取任务ID
     */
    @Test
    public void testSetAndGetTaskId_ValidTaskId_ReturnsTaskId() {
        // Arrange
        String taskId = "task-123";

        // Act
        validationTask.setTaskId(taskId);

        // Assert
        assertEquals(taskId, validationTask.getTaskId());
    }

    /**
     * 测试设置和获取状态
     */
    @Test
    public void testSetAndGetStatus_ValidStatus_ReturnsStatus() {
        // Arrange
        String status = "PENDING";

        // Act
        validationTask.setStatus(status);

        // Assert
        assertEquals(status, validationTask.getStatus());
    }

    /**
     * 测试设置和获取进度
     */
    @Test
    public void testSetAndGetProgress_ValidProgress_ReturnsProgress() {
        // Arrange
        Integer progress = 50;

        // Act
        validationTask.setProgress(progress);

        // Assert
        assertEquals(progress, validationTask.getProgress());
    }

    /**
     * 测试设置和获取开始时间
     */
    @Test
    public void testSetAndGetStartedTime_ValidTime_ReturnsTime() {
        // Arrange
        LocalDateTime startedTime = LocalDateTime.now();

        // Act
        validationTask.setStartedTime(startedTime);

        // Assert
        assertEquals(startedTime, validationTask.getStartedTime());
    }

    /**
     * 测试设置和获取完成时间
     */
    @Test
    public void testSetAndGetCompletedTime_ValidTime_ReturnsTime() {
        // Arrange
        LocalDateTime completedTime = LocalDateTime.now();

        // Act
        validationTask.setCompletedTime(completedTime);

        // Assert
        assertEquals(completedTime, validationTask.getCompletedTime());
    }

    /**
     * 测试设置和获取错误消息
     */
    @Test
    public void testSetAndGetErrorMessage_ValidMessage_ReturnsMessage() {
        // Arrange
        String errorMessage = "Test error";

        // Act
        validationTask.setErrorMessage(errorMessage);

        // Assert
        assertEquals(errorMessage, validationTask.getErrorMessage());
    }

    /**
     * 测试设置和获取创建时间
     */
    @Test
    public void testSetAndGetCreatedTime_ValidTime_ReturnsTime() {
        // Arrange
        LocalDateTime createdTime = LocalDateTime.now();

        // Act
        validationTask.setCreatedTime(createdTime);

        // Assert
        assertEquals(createdTime, validationTask.getCreatedTime());
    }

    /**
     * 测试equals方法 - 相同对象
     */
    @Test
    public void testEquals_SameObject_ReturnsTrue() {
        // Act & Assert
        assertEquals(validationTask, validationTask);
    }

    /**
     * 测试equals方法 - 相同ID和taskId
     */
    @Test
    public void testEquals_SameIdAndTaskId_ReturnsTrue() {
        // Arrange
        ValidationTask task1 = new ValidationTask();
        task1.setId(1L);
        task1.setTaskId("task-123");

        ValidationTask task2 = new ValidationTask();
        task2.setId(1L);
        task2.setTaskId("task-123");

        // Act & Assert
        assertEquals(task1, task2);
    }

    /**
     * 测试equals方法 - 不同ID
     */
    @Test
    public void testEquals_DifferentId_ReturnsFalse() {
        // Arrange
        ValidationTask task1 = new ValidationTask();
        task1.setId(1L);
        task1.setTaskId("task-123");

        ValidationTask task2 = new ValidationTask();
        task2.setId(2L);
        task2.setTaskId("task-123");

        // Act & Assert
        assertNotEquals(task1, task2);
    }

    /**
     * 测试equals方法 - null对象
     */
    @Test
    public void testEquals_NullObject_ReturnsFalse() {
        // Act & Assert
        assertNotEquals(validationTask, null);
    }

    /**
     * 测试equals方法 - 不同类型
     */
    @Test
    public void testEquals_DifferentClass_ReturnsFalse() {
        // Act & Assert
        assertNotEquals(validationTask, "not a ValidationTask");
    }

    /**
     * 测试hashCode方法 - 相同ID和taskId
     */
    @Test
    public void testHashCode_SameIdAndTaskId_ReturnsSameHashCode() {
        // Arrange
        ValidationTask task1 = new ValidationTask();
        task1.setId(1L);
        task1.setTaskId("task-123");

        ValidationTask task2 = new ValidationTask();
        task2.setId(1L);
        task2.setTaskId("task-123");

        // Act & Assert
        assertEquals(task1.hashCode(), task2.hashCode());
    }

    /**
     * 测试toString方法
     */
    @Test
    public void testToString_ValidTask_ReturnsString() {
        // Arrange
        validationTask.setId(1L);
        validationTask.setTestCaseSetId(1L);
        validationTask.setTaskId("task-123");
        validationTask.setStatus("PENDING");
        validationTask.setProgress(0);

        // Act
        String result = validationTask.toString();

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("ValidationTask"));
        assertTrue(result.contains("task-123"));
        assertTrue(result.contains("PENDING"));
    }
}

