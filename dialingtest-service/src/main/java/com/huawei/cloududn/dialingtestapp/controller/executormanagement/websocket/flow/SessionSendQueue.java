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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * V6双队列管理器-物理分离版本
 * 包含控制消息队列(JSON信令)和数据消息队列(二进制分片)
 * 通过全局线程池实现并行处理
 * V6物理分离改进: 基于token而非sessionId,支持独立的双连接
 *
 * @author g00940940
 * @since 2025-11-20
 */
public class SessionSendQueue {
    private static final Logger logger = LoggerFactory.getLogger(SessionSendQueue.class);
    private static final int POLL_TIMEOUT_MS = 50;

    private final String token;
    private final WebSocketSessionRegistry sessionRegistry;
    private final ExecutorService globalThreadPool;

    private final BlockingQueue<QueuedMessage> controlQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<QueuedMessage> dataQueue = new LinkedBlockingQueue<>();

    private volatile boolean running = true;

    /**
     * 构造函数
     * V6: 使用全局共享线程池,启动双队列消费任务
     * 物理分离版本: 使用token标识双连接
     *
     * @param token 认证token
     * @param sessionRegistry 会话注册表
     * @param globalThreadPool 全局线程池
     */
    public SessionSendQueue(String token, WebSocketSessionRegistry sessionRegistry, 
            ExecutorService globalThreadPool) {
        this.token = token;
        this.sessionRegistry = sessionRegistry;
        this.globalThreadPool = globalThreadPool;

        startControlQueueConsumer();
        startDataQueueConsumer();
        logger.info("Dual queue consumers started for token={}", token);
    }

    /**
     * 启动控制消息队列消费任务
     * V6: 独立线程处理控制消息,物理分离版本使用token
     */
    private void startControlQueueConsumer() {
        globalThreadPool.submit(() -> {
            logger.debug("Control queue consumer started for token={}", token);
            while (running) {
                try {
                    QueuedMessage message = controlQueue.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                    if (message != null) {
                        sendControlMessage(message);
                    }
                } catch (InterruptedException e) {
                    logger.warn("Control queue consumer interrupted for token={}", token);
                    break;
                } catch (Exception e) {
                    logger.error("Error in control queue consumer for token={}", token, e);
                }
            }
            logger.debug("Control queue consumer stopped for token={}", token);
        });
    }

    /**
     * 启动数据消息队列消费任务
     * V6: 独立线程处理数据消息,物理分离版本使用token
     */
    private void startDataQueueConsumer() {
        globalThreadPool.submit(() -> {
            logger.debug("Data queue consumer started for token={}", token);
            while (running) {
                try {
                    QueuedMessage message = dataQueue.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                    if (message != null) {
                        sendDataMessage(message);
                    }
                } catch (InterruptedException e) {
                    logger.warn("Data queue consumer interrupted for token={}", token);
                    break;
                } catch (Exception e) {
                    logger.error("Error in data queue consumer for token={}", token, e);
                }
            }
            logger.debug("Data queue consumer stopped for token={}", token);
        });
    }

    /**
     * 将控制消息加入控制队列
     * V6: JSON信令走控制队列,物理分离版本使用token
     *
     * @param jsonMessage JSON消息字符串
     */
    public void enqueueControlMessage(String jsonMessage) {
        QueuedMessage message = QueuedMessage.text(jsonMessage);
        if (!controlQueue.offer(message)) {
            logger.warn("Failed to enqueue control message for token={}", token);
        } else {
            logger.debug("Control message enqueued for token={}, queueSize={}", 
                    token, controlQueue.size());
        }
    }

    /**
     * 将数据消息加入数据队列
     * V6: 二进制分片走数据队列,物理分离版本使用token
     *
     * @param chunk 二进制数据
     */
    public void enqueueDataMessage(ByteBuffer chunk) {
        QueuedMessage message = QueuedMessage.binary(chunk);
        if (!dataQueue.offer(message)) {
            logger.warn("Failed to enqueue data chunk for token={}", token);
        } else {
            logger.debug("Data chunk enqueued for token={}, queueSize={}", 
                    token, dataQueue.size());
        }
    }

    /**
     * 发送控制消息
     * V6: 通过控制链路发送,物理分离版本使用token
     *
     * @param message 队列消息
     */
    private void sendControlMessage(QueuedMessage message) {
        logger.debug("Attempting to send control message for token={}", token);
        if (message.isText()) {
            sessionRegistry.sendTextByToken(token, message.getTextMessage());
            logger.debug("Control message sent successfully for token={}", token);
        } else {
            logger.warn("Invalid message type in control queue for token={}", token);
        }
    }

    /**
     * 发送数据消息
     * V6: 通过数据链路发送,物理分离版本使用token
     *
     * @param message 队列消息
     */
    private void sendDataMessage(QueuedMessage message) {
        if (message.isBinary()) {
            sessionRegistry.sendBinaryByToken(token, message.getBinaryMessage());
        } else {
            logger.warn("Invalid message type in data queue for token={}", token);
        }
    }

    /**
     * 关闭队列消费任务
     * V6: 不需要关闭全局线程池,只需要停止消费循环
     * 物理分离版本使用token
     */
    public void shutdown() {
        running = false;
        logger.info("Session send queue shutdown initiated for token={}, controlQueue={}, dataQueue={}", 
                token, controlQueue.size(), dataQueue.size());
    }

    /**
     * 获取控制队列大小
     *
     * @return 队列大小
     */
    public int getControlQueueSize() {
        return controlQueue.size();
    }

    /**
     * 获取数据队列大小
     *
     * @return 队列大小
     */
    public int getDataQueueSize() {
        return dataQueue.size();
    }

    /**
     * 获取队列统计信息
     * V6: 双队列统计
     *
     * @return 统计信息字符串
     */
    public String getQueueStats() {
        return String.format("QueueStats[control=%d, data=%d]", 
                controlQueue.size(), dataQueue.size());
    }
}


