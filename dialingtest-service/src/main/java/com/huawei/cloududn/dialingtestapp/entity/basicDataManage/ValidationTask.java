/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.entity.basicDataManage;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 校验任务实体类
 * 
 * @author g00940940
 * @since 2025-10-30
 */
public class ValidationTask {
    
    private Long id;
    private Long testCaseSetId;
    private String taskId;
    private String status; // PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
    private Integer progress;
    private LocalDateTime startedTime;
    private LocalDateTime completedTime;
    private String errorMessage;
    private LocalDateTime createdTime;
    
    public ValidationTask() {
        // Default constructor
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getTestCaseSetId() {
        return testCaseSetId;
    }
    
    public void setTestCaseSetId(Long testCaseSetId) {
        this.testCaseSetId = testCaseSetId;
    }
    
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Integer getProgress() {
        return progress;
    }
    
    public void setProgress(Integer progress) {
        this.progress = progress;
    }
    
    public LocalDateTime getStartedTime() {
        return startedTime;
    }
    
    public void setStartedTime(LocalDateTime startedTime) {
        this.startedTime = startedTime;
    }
    
    public LocalDateTime getCompletedTime() {
        return completedTime;
    }
    
    public void setCompletedTime(LocalDateTime completedTime) {
        this.completedTime = completedTime;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public LocalDateTime getCreatedTime() {
        return createdTime;
    }
    
    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ValidationTask that = (ValidationTask) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(taskId, that.taskId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, taskId);
    }
    
    @Override
    public String toString() {
        return "ValidationTask{" +
                "id=" + id +
                ", testCaseSetId=" + testCaseSetId +
                ", taskId='" + taskId + '\'' +
                ", status='" + status + '\'' +
                ", progress=" + progress +
                ", startedTime=" + startedTime +
                ", completedTime=" + completedTime +
                ", createdTime=" + createdTime +
                '}';
    }
}

