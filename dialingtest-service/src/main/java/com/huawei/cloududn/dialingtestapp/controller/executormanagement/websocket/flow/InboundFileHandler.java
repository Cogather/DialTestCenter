/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * V4 入站文件处理器
 * 负责管理所有会话的文件接收状态
 * 文件接收完成后通过 Spring 事件机制通知业务层
 *
 * @author g00940940
 * @since 2025-11-14
 */
@Component
public class InboundFileHandler {
    private static final Logger logger = LoggerFactory.getLogger(InboundFileHandler.class);

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private final Map<String, InboundFileState> receivingFiles = new ConcurrentHashMap<>();

    /**
     * 业务层调用：开始接收文件
     *
     * @param sessionId 会话ID
     * @param expectedSize 预期文件大小
     * @param crc 预期CRC校验值
     * @param tempPath 临时文件路径
     */
    public void startReceiving(String sessionId, int expectedSize, String crc, String tempPath) {
        InboundFileState state = new InboundFileState();
        state.setSessionId(sessionId);
        state.setExpectedSize(expectedSize);
        state.setExpectedCrc(crc);
        state.setTempFilePath(tempPath);

        receivingFiles.put(sessionId, state);
        logger.info("Started receiving file for sessionId={}, expectedSize={} bytes", sessionId, expectedSize);
    }

    /**
     * 业务层调用：开始接收文件（带业务上下文）
     *
     * @param sessionId 会话ID
     * @param expectedSize 预期文件大小
     * @param crc 预期CRC校验值
     * @param tempPath 临时文件路径
     * @param businessContext 业务上下文对象
      */
     public void startReceiving(String sessionId, int expectedSize, String crc, String tempPath,
             Object businessContext) {
        InboundFileState state = new InboundFileState();
        state.setSessionId(sessionId);
        state.setExpectedSize(expectedSize);
        state.setExpectedCrc(crc);
        state.setTempFilePath(tempPath);
        state.setBusinessContext(businessContext);

        receivingFiles.put(sessionId, state);
        logger.info("Started receiving file for sessionId={}, expectedSize={} bytes", sessionId, expectedSize);
    }

    /**
     * Dispatcher调用：处理分片
     *
     * @param sessionId 会话ID
     * @param buffer 二进制分片数据
     */
    public void handleChunk(String sessionId, ByteBuffer buffer) {
        InboundFileState state = receivingFiles.get(sessionId);
        if (state == null) {
            logger.warn("No receiving state for sessionId={}", sessionId);
            return;
        }

        try {
            File tempFile = new File(state.getTempFilePath());
            File parentDir = tempFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                Files.createDirectories(parentDir.toPath());
            }

            try (FileOutputStream fos = new FileOutputStream(tempFile, true)) {
                int chunkSize = buffer.remaining();
                byte[] data = new byte[chunkSize];
                buffer.get(data);
                fos.write(data);

                state.updateCrc(data);
                state.addReceivedSize(chunkSize);

                logger.debug("Chunk received for sessionId={}, size={} bytes, total={}/{}",
                        sessionId, chunkSize, state.getReceivedSize(), state.getExpectedSize());
            }

            if (state.isComplete()) {
                logger.info("File received completely for sessionId={}", sessionId);

                if (!state.verifyCrc()) {
                    logger.error("CRC check failed for sessionId={}", sessionId);
                    state.setError("CRC mismatch");
                }

                // 发布文件完成事件
                eventPublisher.publishEvent(new InboundFileCompleteEvent(this, state));
                logger.debug("Published InboundFileCompleteEvent for sessionId={}", sessionId);

                receivingFiles.remove(sessionId);
            }

        } catch (IOException e) {
            logger.error("Failed to handle chunk for sessionId={}", sessionId, e);
            state.setError(e.getMessage());
            // 即使出错也发布事件，让业务层处理错误
            eventPublisher.publishEvent(new InboundFileCompleteEvent(this, state));
            receivingFiles.remove(sessionId);
        }
    }

    /**
     * 检查会话是否在接收文件
     *
     * @param sessionId 会话ID
     * @return 是否在接收文件
     */
    public boolean isReceivingFile(String sessionId) {
        return receivingFiles.containsKey(sessionId);
    }

    /**
     * 取消文件接收
     *
     * @param sessionId 会话ID
     */
    public void cancelReceiving(String sessionId) {
        InboundFileState state = receivingFiles.remove(sessionId);
        if (state != null) {
            logger.info("Cancelled file receiving for sessionId={}", sessionId);
        }
    }
}

