package com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * TaskStart-Response (0x34) 消息DTO
 * 方向: ADCA → CloudUDN
 * 说明: 拨测任务启动应答，上报拨测结果和日志信息
 * V4版本：移除files字段，文件内容通过后续的Binary分片传输
 *
 * @author DialTestCenter
 * @version V4
 */
public class TaskStartResponseDto {

    @JsonProperty("token")
    private long token;

    @JsonProperty("taskid")
    private int taskId;

    @JsonProperty("result")
    private String result;

    @JsonProperty("block")
    private String block;

    @JsonProperty("description")
    private String description;

    @JsonProperty("sub-result")
    private List<SubResultItemDto> subResult;

    @JsonProperty("filelen")
    private int filelen;

    @JsonProperty("crc")
    private String crc;
    
    public TaskStartResponseDto() {
        this.subResult = new ArrayList<>();
    }
    
    public long getToken() {
        return token;
    }
    
    public void setToken(long token) {
        this.token = token;
    }
    
    public int getTaskId() {
        return taskId;
    }
    
    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }
    
    public String getResult() {
        return result;
    }
    
    public void setResult(String result) {
        this.result = result;
    }
    
    public String getBlock() {
        return block;
    }
    
    public void setBlock(String block) {
        this.block = block;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public List<SubResultItemDto> getSubResult() {
        return subResult;
    }
    
    public void setSubResult(List<SubResultItemDto> subResult) {
        this.subResult = subResult;
    }
    
    public int getFilelen() {
        return filelen;
    }
    
    public void setFilelen(int filelen) {
        this.filelen = filelen;
    }
    
    public String getCrc() {
        return crc;
    }

    public void setCrc(String crc) {
        this.crc = crc;
    }
    
    @Override
    public String toString() {
        return "TaskStartResponseDto{" +
                "token=" + token +
                ", taskId=" + taskId +
                ", result='" + result + '\'' +
                ", subResultCount=" + (subResult != null ? subResult.size() : 0) +
                ", filelen=" + filelen +
                '}';
    }
}

