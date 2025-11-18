/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.flow;

import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.WebSocketSessionRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * V4 单会话发送队列
 * 包含高优队列（JSON信令）和低优队列（Binary分片）
 * 高优队列总是优先发送
 * V4改进：使用QueuedMessage统一包装消息
 *
 * @author g00940940
 * @since 2025-11-14
 */
public class SessionSendQueue {
    private static final Logger logger = LoggerFactory.getLogger(SessionSendQueue.class);
    private static final int POLL_TIMEOUT_MS = 50;

    private final String sessionId;
    private final WebSocketSessionRegistry sessionRegistry;

    private final BlockingQueue<QueuedMessage> hiPriorityQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<QueuedMessage> loPriorityQueue = new LinkedBlockingQueue<>();

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private volatile boolean running = true;

    public SessionSendQueue(String sessionId, WebSocketSessionRegistry sessionRegistry) {
        this.sessionId = sessionId;
        this.sessionRegistry = sessionRegistry;

        executorService.submit(this::sendLoop);
    }

    /**
     * 将JSON信令加入高优先级队列
     *
     * @param jsonMessage JSON消息字符串
     */
    public void enqueueHighPriority(String jsonMessage) {
        QueuedMessage message = QueuedMessage.text(jsonMessage);
        if (!hiPriorityQueue.offer(message)) {
            logger.warn("Failed to enqueue high priority message for sessionId={}", sessionId);
        }
    }

    /**
     * 将二进制分片加入低优先级队列
     *
     * @param chunk 二进制数据
     */
    public void enqueueLowPriority(ByteBuffer chunk) {
        QueuedMessage message = QueuedMessage.binary(chunk);
        if (!loPriorityQueue.offer(message)) {
            logger.warn("Failed to enqueue low priority chunk for sessionId={}", sessionId);
        }
    }

    private void sendLoop() {
        logger.info("Send loop started for sessionId={}", sessionId);

        while (running) {
            try {
                QueuedMessage hiMessage = hiPriorityQueue.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (hiMessage != null) {
                    sendMessage(hiMessage);
                    continue;
                }

                QueuedMessage loMessage = loPriorityQueue.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (loMessage != null) {
                    sendMessage(loMessage);
                    continue;
                }

            } catch (InterruptedException e) {
                logger.warn("Send loop interrupted for sessionId={}, stopping loop", sessionId);
                // 线程被中断，退出循环
                break;
            } catch (Exception e) {
                logger.error("Error in send loop for sessionId={}", sessionId, e);
            }
        }

        logger.info("Send loop stopped for sessionId={}", sessionId);
    }

    private void sendMessage(QueuedMessage message) {
        if (message.isText()) {
            sessionRegistry.sendText(sessionId, message.getTextMessage());
        } else if (message.isBinary()) {
            sessionRegistry.sendBinary(sessionId, message.getBinaryMessage());
        } else {
            logger.warn("Unknown message type for sessionId={}", sessionId);
        }
    }

    public void shutdown() {
        running = false;
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("ExecutorService did not terminate in time for sessionId={}, forcing shutdown", sessionId);
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.warn("Shutdown interrupted for sessionId={}, forcing shutdown", sessionId);
            executorService.shutdownNow();
            // 不重新设置中断状态，因为已经强制关闭了
        }
    }

    public int getHighPriorityQueueSize() {
        return hiPriorityQueue.size();
    }

    public int getLowPriorityQueueSize() {
        return loPriorityQueue.size();
    }
}

