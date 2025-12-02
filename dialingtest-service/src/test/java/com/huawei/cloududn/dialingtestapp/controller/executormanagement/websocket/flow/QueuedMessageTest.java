/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.flow;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

/**
 * QueuedMessage单元测试
 *
 * @author g00940940
 * @since 2025-11-29
 */
public class QueuedMessageTest {

    @Test
    public void testText_ValidMessage_Success() {
        String content = "{\"type\":\"test\"}";
        QueuedMessage message = QueuedMessage.text(content);

        assertNotNull(message);
        assertTrue(message.isText());
        assertFalse(message.isBinary());
        assertEquals(content, message.getTextMessage());
        assertNull(message.getBinaryMessage());
        assertEquals(content.length(), message.getSize());
        assertEquals(QueuedMessage.MessageTypeEnum.TEXT, message.getType());
        assertTrue(message.getTimestamp() > 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testText_NullMessage_ThrowsException() {
        QueuedMessage.text(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testText_EmptyMessage_ThrowsException() {
        QueuedMessage.text("");
    }

    @Test
    public void testBinary_ValidMessage_Success() {
        byte[] data = "test data".getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.wrap(data);
        QueuedMessage message = QueuedMessage.binary(buffer);

        assertNotNull(message);
        assertFalse(message.isText());
        assertTrue(message.isBinary());
        assertEquals(buffer, message.getBinaryMessage());
        assertNull(message.getTextMessage());
        assertEquals(data.length, message.getSize());
        assertEquals(QueuedMessage.MessageTypeEnum.BINARY, message.getType());
        assertTrue(message.getTimestamp() > 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBinary_NullMessage_ThrowsException() {
        QueuedMessage.binary(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBinary_EmptyMessage_ThrowsException() {
        QueuedMessage.binary(ByteBuffer.allocate(0));
    }

    @Test
    public void testToString() {
        String content = "test";
        QueuedMessage message = QueuedMessage.text(content);
        String str = message.toString();
        
        assertNotNull(str);
        assertTrue(str.contains("type=TEXT"));
        assertTrue(str.contains("size=" + content.length()));
    }
}

