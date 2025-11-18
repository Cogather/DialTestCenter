/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.flow;

import org.springframework.context.ApplicationEvent;

/**
 * 入站文件接收完成事件
 * 当 InboundFileHandler 完成文件接收并校验CRC后发布此事件
 * 业务层通过 @EventListener 监听此事件
 *
 * @author g00940940
 * @since 2025-11-16
 */
public class InboundFileCompleteEvent extends ApplicationEvent {
    private static final long serialVersionUID = 1L;

    private final InboundFileState state;

    /**
     * 构造入站文件完成事件
     *
     * @param source 事件源（通常是 InboundFileHandler）
     * @param state 文件接收状态
     */
    public InboundFileCompleteEvent(Object source, InboundFileState state) {
        super(source);
        this.state = state;
    }

    /**
     * 获取文件接收状态
     *
     * @return 文件接收状态对象
     */
    public InboundFileState getState() {
        return state;
    }
}

