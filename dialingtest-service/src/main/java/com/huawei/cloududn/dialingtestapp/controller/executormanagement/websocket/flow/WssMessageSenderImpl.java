/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.WebSocketSessionRegistry;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.JsonMessageEnvelope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * V4 出站消息发送器实现
 * 内部管理每个会话的 SessionSendQueue
 *
 * @author g00940940
 * @since 2025-11-14
 */
@Component
public class WssMessageSenderImpl implements WssMessageSender {
    private static final Logger logger = LoggerFactory.getLogger(WssMessageSenderImpl.class);
    private static final int CHUNK_SIZE = 8192;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebSocketSessionRegistry sessionRegistry;

    private final Map<String, SessionSendQueue> sessionQueues = new ConcurrentHashMap<>();

    @Override
    public void sendJsonMessage(String sessionId, Object dto) {
        try {
            // Wrap DTO in JsonMessageEnvelope with type field
            String messageType = getMessageTypeFromDto(dto);
            JsonMessageEnvelope envelope = new JsonMessageEnvelope(messageType, null, dto);
            
            String jsonMessage = objectMapper.writeValueAsString(envelope);

            SessionSendQueue queue = getOrCreateQueue(sessionId);
            queue.enqueueHighPriority(jsonMessage);

            logger.debug("JSON message enqueued for sessionId={}, type={}", sessionId, messageType);

        } catch (Exception e) {
            logger.error("Failed to send JSON message to sessionId={}", sessionId, e);
        }
    }
    
    /**
     * Get message type name from DTO class name.
     * Removes "Dto" suffix to match MessageType enum names.
     *
     * @param dto DTO object
     * @return message type name
     */
    private String getMessageTypeFromDto(Object dto) {
        if (dto == null) {
            throw new IllegalArgumentException("DTO cannot be null");
        }
        String className = dto.getClass().getSimpleName();
        if (className.endsWith("Dto")) {
            return className.substring(0, className.length() - 3);
        } else {
            return className;
        }
    }

    @Override
    public void sendFile(String sessionId, Object dto, InputStream fileStream) {
        try {
            sendJsonMessage(sessionId, dto);

            SessionSendQueue queue = getOrCreateQueue(sessionId);
            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;
            int totalBytes = 0;

            while ((bytesRead = fileStream.read(buffer)) != -1) {
                ByteBuffer chunk = ByteBuffer.wrap(buffer, 0, bytesRead);
                queue.enqueueLowPriority(chunk);
                totalBytes += bytesRead;
            }

            logger.info("File enqueued for sessionId={}, totalSize={} bytes", sessionId, totalBytes);

        } catch (IOException e) {
            logger.error("Failed to send file to sessionId={}", sessionId, e);
        }
    }

    private SessionSendQueue getOrCreateQueue(String sessionId) {
        return sessionQueues.computeIfAbsent(sessionId, id -> new SessionSendQueue(id, sessionRegistry));
    }

    /**
     * 会话关闭时清理队列
     *
     * @param sessionId 会话ID
     */
    public void removeQueue(String sessionId) {
        SessionSendQueue queue = sessionQueues.remove(sessionId);
        if (queue != null) {
            queue.shutdown();
            logger.info("Removed send queue for sessionId={}", sessionId);
        }
    }

    /**
     * 获取会话队列统计信息
     *
     * @param sessionId 会话ID
     * @return 队列统计信息
     */
    public String getQueueStats(String sessionId) {
        SessionSendQueue queue = sessionQueues.get(sessionId);
        if (queue == null) {
            return "Queue not found";
        }
        return String.format(Locale.ROOT, "HiPri=%d, LoPri=%d", 
                queue.getHighPriorityQueueSize(), 
                queue.getLowPriorityQueueSize());
    }
}

