/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.Session;

/**
 * V5版本: 双连接会话注册表-物理分离版本
 * 分别管理控制链路和数据链路的两条独立连接
 * 提供独立的会话存储和查询功能
 *
 * @author g00940940
 * @since 2025-11-20
 */
@Component
public class WebSocketSessionRegistry {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketSessionRegistry.class);

    private final Map<String, Session> controlSessions = new ConcurrentHashMap<>();
    
    private final Map<String, Session> dataSessions = new ConcurrentHashMap<>();

    @Autowired
    private DualLinkRouter dualLinkRouter;

    /**
     * 添加控制链路会话
     * V5: 物理分离-独立管理控制链路
     *
     * @param session 控制链路WebSocket会话
     */
    public void addControlSession(Session session) {
        if (session == null) {
            logger.warn("Attempted to add null control session");
            return;
        }
        String sessionId = session.getId();
        controlSessions.put(sessionId, session);
        logger.info("Control session added, sessionId={}", sessionId);
    }

    /**
     * 添加数据链路会话
     * V5: 物理分离-独立管理数据链路
     *
     * @param session 数据链路WebSocket会话
     */
    public void addDataSession(Session session) {
        if (session == null) {
            logger.warn("Attempted to add null data session");
            return;
        }
        String sessionId = session.getId();
        dataSessions.put(sessionId, session);
        logger.info("Data session added, sessionId={}", sessionId);
    }

    /**
     * 移除控制链路会话
     *
     * @param sessionId 控制链路会话ID
     */
    public void removeControlSession(String sessionId) {
        if (sessionId == null) {
            logger.warn("Attempted to remove control session with null id");
            return;
        }
        controlSessions.remove(sessionId);
        logger.info("Control session removed, sessionId={}", sessionId);
    }

    /**
     * 移除数据链路会话
     *
     * @param sessionId 数据链路会话ID
     */
    public void removeDataSession(String sessionId) {
        if (sessionId == null) {
            logger.warn("Attempted to remove data session with null id");
            return;
        }
        dataSessions.remove(sessionId);
        logger.info("Data session removed, sessionId={}", sessionId);
    }

    /**
     * 获取控制链路Session
     *
     * @param sessionId 控制链路会话ID
     * @return Session或null
     */
    public Session getControlSession(String sessionId) {
        return controlSessions.get(sessionId);
    }

    /**
     * 获取数据链路Session
     *
     * @param sessionId 数据链路会话ID
     * @return Session或null
     */
    public Session getDataSession(String sessionId) {
        return dataSessions.get(sessionId);
    }

    /**
     * 通过token获取控制链路Session
     * V5新增: 支持token查询
     *
     * @param token 认证token
     * @return Session或null
     */
    public Session getControlSessionByToken(String token) {
        return dualLinkRouter.getControlSessionByToken(token);
    }

    /**
     * 通过token获取数据链路Session
     * V5新增: 支持token查询
     *
     * @param token 认证token
     * @return Session或null
     */
    public Session getDataSessionByToken(String token) {
        return dualLinkRouter.getDataSessionByToken(token);
    }

    /**
     * 发送文本消息到控制链路(通过sessionId，用于认证阶段)
     * V5: 物理分离-认证阶段token尚未生成，直接通过sessionId发送
     *
     * @param sessionId 控制链路会话ID
     * @param message 文本消息
     */
    public void sendTextBySessionId(String sessionId, String message) {
        Session session = getControlSession(sessionId);
        if (session == null) {
            logger.warn("Control session not found for sessionId={}", sessionId);
            return;
        }
        if (!session.isOpen()) {
            logger.warn("Control session is closed, sessionId={}", sessionId);
            return;
        }
        try {
            session.getBasicRemote().sendText(message);
            logger.debug("Control message sent via sessionId, size={} chars", message.length());
        } catch (IOException e) {
            logger.error("Failed to send control message to sessionId={}", sessionId, e);
        }
    }

    /**
     * 发送文本消息到控制链路(通过token)
     * V5: 物理分离-通过token找到控制链路发送
     *
     * @param token 认证token
     * @param message 文本消息
     */
    public void sendTextByToken(String token, String message) {
        Session session = dualLinkRouter.getControlSessionByToken(token);
        if (session == null) {
            logger.warn("Control session not found for token={}", token);
            return;
        }
        if (!session.isOpen()) {
            logger.warn("Control session is closed, token={}", token);
            return;
        }
        try {
            session.getBasicRemote().sendText(message);
            logger.debug("Control message sent via token, size={} chars", message.length());
        } catch (IOException e) {
            logger.error("Failed to send control message to token={}", token, e);
        }
    }

    /**
     * 发送二进制消息到数据链路(通过token)
     * V5: 物理分离-通过token找到数据链路发送
     *
     * @param token 认证token
     * @param buffer 二进制数据缓冲区
     */
    public void sendBinaryByToken(String token, ByteBuffer buffer) {
        Session session = dualLinkRouter.getDataSessionByToken(token);
        if (session == null) {
            logger.warn("Data session not found for token={}", token);
            return;
        }
        if (!session.isOpen()) {
            logger.warn("Data session is closed, token={}", token);
            return;
        }
        try {
            session.getBasicRemote().sendBinary(buffer);
            logger.debug("Data message sent via token, size={} bytes", buffer.remaining());
        } catch (IOException e) {
            logger.error("Failed to send data message to token={}", token, e);
        }
    }

    /**
     * 获取会话统计信息
     * V5: 物理分离-包含双连接状态
     *
     * @return 统计信息字符串
     */
    public String getSessionStats() {
        int totalControlSessions = controlSessions.size();
        int totalDataSessions = dataSessions.size();
        String linkStats = dualLinkRouter.getLinkStats();
        return String.format(Locale.ROOT, "SessionStats[control=%d, data=%d, %s]", 
                totalControlSessions, totalDataSessions, linkStats);
    }
}


