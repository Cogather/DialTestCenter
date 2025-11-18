/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration.event;

import org.springframework.context.ApplicationEvent;

/**
 * 任务分发事件，用于解耦任务编排和执行机通信
 *
 * @author g00940940
 * @since 2025-11-09
 */
public class TaskDispatchEvent extends ApplicationEvent {
    private static final long serialVersionUID = 1L;

    private final String sessionId;
    private final Object taskPayload;
    private final String taskId;

    /**
     * 构造任务分发事件
     *
     * @param source 事件源
     * @param sessionId WebSocket会话ID
     * @param taskPayload 任务负载
     * @param taskId 任务ID
     */
    public TaskDispatchEvent(Object source, String sessionId, Object taskPayload, String taskId) {
        super(source);
        this.sessionId = sessionId;
        this.taskPayload = taskPayload;
        this.taskId = taskId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Object getTaskPayload() {
        return taskPayload;
    }

    public String getTaskId() {
        return taskId;
    }
}

