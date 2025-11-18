/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.WebSocketSessionRegistry;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.RegisterChallengeDto;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * WssMessageSenderImpl单元测试
 * 测试出站消息队列管理和文件分片
 *
 * @author g00940940
 * @since 2025-11-16
 */
public class WssMessageSenderImplTest {
    private WssMessageSenderImpl sender;
    private ObjectMapper objectMapper;
    private WebSocketSessionRegistry sessionRegistry;

    @Before
    public void setUp() {
        sender = new WssMessageSenderImpl();
        objectMapper = new ObjectMapper();
        sessionRegistry = mock(WebSocketSessionRegistry.class);

        setField(sender, "objectMapper", objectMapper);
        setField(sender, "sessionRegistry", sessionRegistry);
    }

    /**
     * 测试发送JSON消息成功
     */
    @Test
    public void testSendJsonMessage_Success() throws Exception {
        String sessionId = "session-001";
        RegisterChallengeDto dto = new RegisterChallengeDto();
        dto.setChallengeId(123);
        dto.setChallenge("test-challenge");

        sender.sendJsonMessage(sessionId, dto);

        Thread.sleep(100);
        verify(sessionRegistry, atLeastOnce()).sendText(eq(sessionId), anyString());
    }

    /**
     * 测试从DTO提取消息类型
     */
    @Test
    public void testGetMessageTypeFromDto() throws Exception {
        RegisterChallengeDto dto = new RegisterChallengeDto();

        java.lang.reflect.Method method = WssMessageSenderImpl.class
                .getDeclaredMethod("getMessageTypeFromDto", Object.class);
        method.setAccessible(true);

        String messageType = (String) method.invoke(sender, dto);
        assertEquals("RegisterChallenge", messageType);

        Object plainObject = new Object();
        String plainType = (String) method.invoke(sender, plainObject);
        assertEquals("Object", plainType);
    }

    /**
     * 测试发送文件成功
     */
    @Test
    public void testSendFile_Success() throws Exception {
        String sessionId = "session-001";
        RegisterChallengeDto dto = new RegisterChallengeDto();
        dto.setChallengeId(123);

        byte[] fileData = new byte[10000];
        for (int i = 0; i < fileData.length; i++) {
            fileData[i] = (byte) (i % 256);
        }
        InputStream fileStream = new ByteArrayInputStream(fileData);

        sender.sendFile(sessionId, dto, fileStream);

        Thread.sleep(200);
        verify(sessionRegistry, atLeastOnce()).sendText(eq(sessionId), anyString());
        verify(sessionRegistry, atLeastOnce()).sendBinary(eq(sessionId), any());
    }

    /**
     * 测试发送空文件
     */
    @Test
    public void testSendFile_EmptyFile() throws Exception {
        String sessionId = "session-001";
        RegisterChallengeDto dto = new RegisterChallengeDto();

        InputStream emptyStream = new ByteArrayInputStream(new byte[0]);

        sender.sendFile(sessionId, dto, emptyStream);

        Thread.sleep(100);
        verify(sessionRegistry, atLeastOnce()).sendText(eq(sessionId), anyString());
    }

    /**
     * 测试移除队列
     */
    @Test
    public void testRemoveQueue_Success() throws Exception {
        String sessionId = "session-001";
        RegisterChallengeDto dto = new RegisterChallengeDto();

        sender.sendJsonMessage(sessionId, dto);
        Thread.sleep(100);

        sender.removeQueue(sessionId);

        String stats = sender.getQueueStats(sessionId);
        assertEquals("Queue not found", stats);
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

