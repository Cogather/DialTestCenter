/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.DualLinkRouter;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.WebSocketSessionRegistry;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.JsonMessageEnvelope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * V6双队列并行发送实现-物理分离版本
 * 通过全局线程池实现控制消息和数据消息的真正并行处理
 * 物理分离改进: 基于token管理双连接发送队列
 *
 * @author g00940940
 * @since 2025-11-20
 */
@Component
public class WssMessageSenderImpl implements WssMessageSender {
    private static final Logger logger = LoggerFactory.getLogger(WssMessageSenderImpl.class);
    private static final int CHUNK_SIZE = 8192;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebSocketSessionRegistry sessionRegistry;

    @Autowired
    @Qualifier("websocketMessageSenderExecutor")
    private ExecutorService globalThreadPool;

    @Autowired
    private ThreadPoolConfig threadPoolConfig;

    @Autowired
    private DualLinkRouter dualLinkRouter;

    private final Map<String, SessionSendQueue> tokenQueues = new ConcurrentHashMap<>();

    @Override
    public void sendJsonMessage(String sessionId, Object dto) {
        try {
            String token = dualLinkRouter.getTokenBySessionId(sessionId);
            String messageType = getMessageTypeFromDto(dto);
            
            // V5物理分离：认证阶段（token为null）直接通过控制链路发送
            if (token == null) {
                logger.debug("Token not found for sessionId={}, sending via control session directly (auth phase)", 
                        sessionId);
                
                // 构造信封（认证阶段不需要token字段）
                JsonMessageEnvelope envelope = new JsonMessageEnvelope(messageType, null, dto);
                String jsonMessage = objectMapper.writeValueAsString(envelope);
                
                // 直接通过SessionRegistry发送，不走队列（认证阶段）
                sessionRegistry.sendTextBySessionId(sessionId, jsonMessage);
                logger.debug("Control message sent directly for sessionId={}, type={}", sessionId, messageType);
                return;
            }

            // 正常流程：通过token队列发送
            JsonMessageEnvelope envelope = new JsonMessageEnvelope(messageType, Long.parseLong(token), dto);
            String jsonMessage = objectMapper.writeValueAsString(envelope);

            SessionSendQueue queue = getOrCreateQueue(token);
            queue.enqueueControlMessage(jsonMessage);

            logger.debug("Control message enqueued for token={}, type={}", token, messageType);

        } catch (Exception e) {
            logger.error("Failed to send control message for sessionId={}", sessionId, e);
        }
    }
    
    /**
     * 从DTO类名获取消息类型名称
     * 移除"Dto"后缀以匹配MessageType枚举名称
     *
     * @param dto DTO对象
     * @return 消息类型名称
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
            String token = dualLinkRouter.getTokenBySessionId(sessionId);
            if (token == null) {
                logger.warn("Token not found for sessionId={}, cannot send file", sessionId);
                return;
            }

            sendJsonMessage(sessionId, dto);

            SessionSendQueue queue = getOrCreateQueue(token);
            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;
            int totalBytes = 0;

            while ((bytesRead = fileStream.read(buffer)) != -1) {
                ByteBuffer chunk = ByteBuffer.wrap(buffer, 0, bytesRead);
                queue.enqueueDataMessage(chunk);
                totalBytes += bytesRead;
            }

            logger.info("File chunks enqueued for token={}, totalSize={} bytes, chunks={}", 
                    token, totalBytes, (totalBytes + CHUNK_SIZE - 1) / CHUNK_SIZE);

        } catch (IOException e) {
            logger.error("Failed to send file for sessionId={}", sessionId, e);
        }
    }

    /**
     * 获取或创建token发送队列
     * V6: 物理分离版本使用token
     *
     * @param token 认证token
     * @return SessionSendQueue实例
     */
    private SessionSendQueue getOrCreateQueue(String token) {
        return tokenQueues.computeIfAbsent(token, 
                t -> new SessionSendQueue(t, sessionRegistry, globalThreadPool));
    }

    /**
     * token失效时清理队列
     * V6: 物理分离版本使用token
     *
     * @param token 认证token
     */
    @Override
    public void removeQueueByToken(String token) {
        SessionSendQueue queue = tokenQueues.remove(token);
        if (queue != null) {
            queue.shutdown();
            logger.info("Removed send queue for token={}, final stats: {}", 
                    token, queue.getQueueStats());
        } else {
            logger.warn("Queue not found for removal, token={}", token);
        }
    }

    /**
     * 会话关闭时清理队列(兼容方法)
     *
     * @param sessionId 会话ID
     */
    public void removeQueue(String sessionId) {
        String token = dualLinkRouter.getTokenBySessionId(sessionId);
        if (token != null) {
            removeQueueByToken(token);
        } else {
            logger.warn("Token not found for sessionId={}, cannot remove queue", sessionId);
        }
    }

    /**
     * 获取token队列统计信息
     * V6: 物理分离版本
     *
     * @param token 认证token
     * @return 队列统计信息字符串
     */
    public String getQueueStatsByToken(String token) {
        SessionSendQueue queue = tokenQueues.get(token);
        if (queue == null) {
            return "Queue not found";
        }
        return queue.getQueueStats();
    }

    /**
     * 获取会话队列统计信息(兼容方法)
     *
     * @param sessionId 会话ID
     * @return 队列统计信息字符串
     */
    public String getQueueStats(String sessionId) {
        String token = dualLinkRouter.getTokenBySessionId(sessionId);
        if (token == null) {
            return "Token not found for session";
        }
        return getQueueStatsByToken(token);
    }

    /**
     * 获取全局线程池统计信息
     * V6: 提供线程池监控数据
     *
     * @return 线程池统计信息字符串
     */
    public String getThreadPoolStats() {
        return threadPoolConfig.getThreadPoolStats();
    }

    /**
     * 获取所有队列的统计信息
     * V6: 物理分离版本-基于token
     *
     * @return 统计信息字符串
     */
    public String getAllQueuesStats() {
        int totalTokens = tokenQueues.size();
        int totalControlMessages = tokenQueues.values().stream()
                .mapToInt(SessionSendQueue::getControlQueueSize)
                .sum();
        int totalDataMessages = tokenQueues.values().stream()
                .mapToInt(SessionSendQueue::getDataQueueSize)
                .sum();

        return String.format(Locale.ROOT, "AllQueues[tokens=%d, controlTotal=%d, dataTotal=%d, %s]",
                totalTokens, totalControlMessages, totalDataMessages, getThreadPoolStats());
    }
}



