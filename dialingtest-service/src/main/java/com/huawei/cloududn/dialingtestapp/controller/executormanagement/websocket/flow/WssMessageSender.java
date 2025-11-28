/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.flow;

import java.io.InputStream;

/**
 * V4 出站消息发送器接口
 * 业务层依赖此接口发送消息
 *
 * @author g00940940
 * @since 2025-11-14
 */
public interface WssMessageSender {

    /**
     * 发送纯 JSON 信令消息
     *
     * @param sessionId 会话ID
     * @param dto 业务DTO
     */
    void sendJsonMessage(String sessionId, Object dto);

    /**
     * 发送 JSON 信令 + 文件流
     * 信令进入高优队列，文件分片进入低优队列
     *
     * @param sessionId 会话ID
     * @param dto 业务DTO（含 filelen, crc）
     * @param fileStream 文件输入流
     */
    void sendFile(String sessionId, Object dto, InputStream fileStream);

    /**
     * 根据token移除发送队列
     * V6: 物理分离版本清理资源
     *
     * @param token 认证token
     */
    void removeQueueByToken(String token);
}

