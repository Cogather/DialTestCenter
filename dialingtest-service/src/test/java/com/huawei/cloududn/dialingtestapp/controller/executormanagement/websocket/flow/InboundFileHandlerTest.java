/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.flow;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * InboundFileHandler单元测试
 * 测试文件分片接收、CRC校验和事件发布
 *
 * @author g00940940
 * @since 2025-11-16
 */
public class InboundFileHandlerTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private InboundFileHandler handler;
    private ApplicationEventPublisher eventPublisher;

    @Before
    public void setUp() {
        handler = new InboundFileHandler();
        eventPublisher = mock(ApplicationEventPublisher.class);
        setField(handler, "eventPublisher", eventPublisher);
    }

    /**
     * 测试开始接收文件
     */
    @Test
    public void testStartReceiving_Success() {
        String sessionId = "session-001";
        String tempPath = tempFolder.getRoot().getAbsolutePath() + "/test-file.bin";
        String crc = "12345678";

        handler.startReceiving(sessionId, 1000, crc, tempPath);

        assertTrue(handler.isReceivingFile(sessionId));
    }

    /**
     * 测试处理多个分片
     */
    @Test
    @SuppressWarnings("null")
    public void testHandleChunk_MultipleChunks() throws Exception {
        String sessionId = "session-001";
        File tempFile = tempFolder.newFile("test-multi-chunk.bin");
        String tempPath = tempFile.getAbsolutePath();

        handler.startReceiving(sessionId, 300, null, tempPath);

        ByteBuffer chunk1 = ByteBuffer.wrap(new byte[100]);
        handler.handleChunk(sessionId, chunk1);

        ByteBuffer chunk2 = ByteBuffer.wrap(new byte[100]);
        handler.handleChunk(sessionId, chunk2);

        ByteBuffer chunk3 = ByteBuffer.wrap(new byte[100]);
        handler.handleChunk(sessionId, chunk3);

        verify(eventPublisher).publishEvent(any(InboundFileCompleteEvent.class));
        assertEquals(300, tempFile.length());
    }

    /**
     * 测试最后分片CRC有效
     */
    @Test
    @SuppressWarnings("null")
    public void testHandleChunk_LastChunk_CrcValid() throws Exception {
        String sessionId = "session-001";
        File tempFile = tempFolder.newFile("test-crc-valid.bin");
        String tempPath = tempFile.getAbsolutePath();

        byte[] data = "test data".getBytes(StandardCharsets.UTF_8);
        String expectedCrc = Long.toHexString(calculateCrc32(data));

        handler.startReceiving(sessionId, data.length, expectedCrc, tempPath);

        ByteBuffer chunk = ByteBuffer.wrap(data);
        handler.handleChunk(sessionId, chunk);

        ArgumentCaptor<InboundFileCompleteEvent> captor = ArgumentCaptor.forClass(InboundFileCompleteEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        InboundFileState state = captor.getValue().getState();
        assertFalse(state.hasError());
        assertTrue(state.isComplete());
    }

    /**
     * 测试最后分片CRC无效
     */
    @Test
    @SuppressWarnings("null")
    public void testHandleChunk_LastChunk_CrcInvalid() throws Exception {
        String sessionId = "session-001";
        File tempFile = tempFolder.newFile("test-crc-invalid.bin");
        String tempPath = tempFile.getAbsolutePath();

        byte[] data = "test data".getBytes(StandardCharsets.UTF_8);
        String wrongCrc = "ffffffff";

        handler.startReceiving(sessionId, data.length, wrongCrc, tempPath);

        ByteBuffer chunk = ByteBuffer.wrap(data);
        handler.handleChunk(sessionId, chunk);

        ArgumentCaptor<InboundFileCompleteEvent> captor = ArgumentCaptor.forClass(InboundFileCompleteEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        InboundFileState state = captor.getValue().getState();
        assertTrue(state.hasError());
        assertEquals("CRC mismatch", state.getError());
    }

    /**
     * 测试文件写入IO异常
     * 使用包含非法字符的文件名来触发IOException
     */
    @Test
    @SuppressWarnings("null")
    public void testHandleChunk_IOException() throws Exception {
        String sessionId = "session-001";
        
        // 使用包含非法字符的文件名(Windows: <, >, :, ", |, ?, *)
        String invalidPath = System.getProperty("os.name").toLowerCase().contains("win") 
            ? "C:\\temp\\invalid<>file.bin"  // Windows非法字符
            : "/dev/null/invalid/file.bin";  // Linux不可写路径

        handler.startReceiving(sessionId, 100, null, invalidPath);

        ByteBuffer chunk = ByteBuffer.wrap(new byte[100]);
        handler.handleChunk(sessionId, chunk);

        ArgumentCaptor<InboundFileCompleteEvent> captor = ArgumentCaptor.forClass(InboundFileCompleteEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        InboundFileState state = captor.getValue().getState();
        assertTrue("Expected error state but got: " + state.getError(), state.hasError());
        assertNotNull("Error message should not be null", state.getError());
    }

    /**
     * 测试检查接收状态
     */
    @Test
    public void testIsReceivingFile() {
        String sessionId = "session-001";
        assertFalse(handler.isReceivingFile(sessionId));

        String tempPath = tempFolder.getRoot().getAbsolutePath() + "/test.bin";
        handler.startReceiving(sessionId, 100, null, tempPath);
        assertTrue(handler.isReceivingFile(sessionId));

        handler.cancelReceiving(sessionId);
        assertFalse(handler.isReceivingFile(sessionId));
    }

    /**
     * 测试取消文件接收
     */
    @Test
    public void testCancelReceiving_Success() {
        String sessionId = "session-001";
        String tempPath = tempFolder.getRoot().getAbsolutePath() + "/test-cancel.bin";

        handler.startReceiving(sessionId, 100, null, tempPath);
        assertTrue(handler.isReceivingFile(sessionId));

        handler.cancelReceiving(sessionId);
        assertFalse(handler.isReceivingFile(sessionId));

        handler.cancelReceiving("non-existent-session");
    }

    private static long calculateCrc32(byte[] data) {
        java.util.zip.CRC32 crc32 = new java.util.zip.CRC32();
        crc32.update(data);
        return crc32.getValue();
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Failed to find field: " + fieldName, e);
        } catch (IllegalAccessException e) {
            throw new AssertionError("Failed to access field: " + fieldName, e);
        }
    }
}

