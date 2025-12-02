/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.JsonMessageEnvelope;
import com.huawei.cloududn.dialingtestapp.service.executormanagement.ExecutorMgmtService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 * V5版本: 数据链路WebSocket端点
 * 物理分离的数据链路,专门处理二进制分片消息
 * 首条消息必须携带token进行身份关联
 *
 * @author g00940940
 * @since 2025-11-20
 */
@Component
@ServerEndpoint("/ws/executor/data")
public class DataLinkEndpoint {
    private static final Logger logger = LoggerFactory.getLogger(DataLinkEndpoint.class);

    private static WebSocketSessionRegistry sessionRegistry;

    private static WssMessageDispatcher dispatcher;

    private static DualLinkRouter dualLinkRouter;

    private static ObjectMapper objectMapper;

    private static ExecutorMgmtService executorMgmtService;

    @Autowired
    public void setSessionRegistry(WebSocketSessionRegistry registry) {
        DataLinkEndpoint.sessionRegistry = registry;
    }

    @Autowired
    public void setDispatcher(WssMessageDispatcher disp) {
        DataLinkEndpoint.dispatcher = disp;
    }

    @Autowired
    public void setDualLinkRouter(DualLinkRouter router) {
        DataLinkEndpoint.dualLinkRouter = router;
    }

    @Autowired
    public void setObjectMapper(ObjectMapper mapper) {
        DataLinkEndpoint.objectMapper = mapper;
    }

    @Autowired
    public void setExecutorMgmtService(ExecutorMgmtService service) {
        DataLinkEndpoint.executorMgmtService = service;
    }

    /**
     * 数据链路连接建立事件
     * V5: 注册数据链路会话,等待token绑定
     *
     * @param session WebSocket会话
     */
    @OnOpen
    public void onOpen(Session session) {
        String sessionId = session.getId();
        sessionRegistry.addDataSession(session);
        logger.info("Data link connected, sessionId={}, waiting for token binding", sessionId);
    }

    /**
     * 接收首条认证消息(携带token)
     * V5: 数据链路首条消息必须是Text格式,包含token用于绑定
     *
     * @param message 认证消息(JSON格式,包含token)
     * @param session WebSocket会话
     */
    @OnMessage
    public void onAuthMessage(String message, Session session) {
        String sessionId = session.getId();
        try {
            if (dualLinkRouter.isDataLinkBound(sessionId)) {
                logger.warn("Data link already bound, ignoring text message, sessionId={}", sessionId);
                return;
            }

            JsonMessageEnvelope envelope = objectMapper.readValue(message, JsonMessageEnvelope.class);
            Long token = envelope.getToken();
            
            if (token == null) {
                logger.error("Data link auth failed: token is null, sessionId={}", sessionId);
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, 
                        "Token required for data link"));
                return;
            }

            boolean bound = dualLinkRouter.registerDataLink(token.toString(), sessionId, session);
            if (bound) {
                logger.info("Data link bound successfully, sessionId={}, token={}", sessionId, token);
            } else {
                logger.error("Data link binding failed, sessionId={}, token={}", sessionId, token);
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, 
                        "Invalid token or control link not found"));
            }
        } catch (Exception e) {
            logger.error("Data link auth message processing failed, sessionId={}", sessionId, e);
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, 
                        "Auth message processing error"));
            } catch (Exception closeEx) {
                logger.error("Failed to close data link session, sessionId={}", sessionId, closeEx);
            }
        }
    }

    /**
     * 接收二进制分片消息
     * V5: 数据链路处理Binary Message
     *
     * @param buffer 二进制数据
     * @param session WebSocket会话
     */
    @OnMessage
    public void onDataMessage(ByteBuffer buffer, Session session) {
        if (buffer == null || buffer.remaining() == 0) {
            logger.warn("Received empty data chunk, sessionId={}", session.getId());
            return;
        }
        try {
            String sessionId = session.getId();
            
            if (!dualLinkRouter.isDataLinkBound(sessionId)) {
                logger.warn("Data link not bound yet, discarding binary data, sessionId={}", sessionId);
                return;
            }

            logger.debug("Received data chunk, sessionId={}, size={} bytes", 
                    sessionId, buffer.remaining());

            dispatcher.dispatchData(buffer, session);
        } catch (Exception e) {
            logger.error("Failed to process data chunk, sessionId={}", session.getId(), e);
        }
    }

    /**
     * 数据链路连接关闭事件
     * V5: 清理数据链路资源
     *
     * @param session WebSocket会话
     * @param reason 关闭原因
     */
    @OnClose
    public void onClose(Session session, CloseReason reason) {
        String sessionId = session.getId();
        
        // Get token and control session BEFORE unregistering
        String token = dualLinkRouter.getTokenByDataSessionId(sessionId);
        Session controlSession = null;
        if (token != null) {
            controlSession = dualLinkRouter.getControlSessionByToken(token);
        }
        
        dualLinkRouter.unregisterDataLink(sessionId);
        sessionRegistry.removeDataSession(sessionId);
        
        logger.info("Data link disconnected, sessionId={}, reason={}", 
                sessionId, reason.getReasonPhrase());

        // Trigger full offline if control session exists
        if (controlSession != null) {
            String controlSessionId = controlSession.getId();
            
            // Check if control link is still registered in router (avoid loop/double cleanup)
            if (dualLinkRouter.getTokenByControlSessionId(controlSessionId) == null) {
                logger.debug("Control link already unregistered, skipping cascade disconnect for sessionId={}", 
                        controlSessionId);
                return;
            }
            
            logger.info("Data link closed, triggering full disconnect for controlSessionId={}", controlSessionId);
            
            // Update status to OFFLINE
            executorMgmtService.handleExecutorDisconnect(controlSessionId);
            
            // Force close control session if open
            if (controlSession.isOpen()) {
                try {
                    controlSession.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, 
                            "Data link disconnected"));
                } catch (IOException e) {
                    logger.error("Failed to close control session", e);
                }
            }
        }
    }

    /**
     * 错误事件
     *
     * @param session WebSocket会话
     * @param throwable 异常信息
     */
    @OnError
    public void onError(Session session, Throwable throwable) {
        logger.error("Data link error, sessionId={}", session.getId(), throwable);
    }

    /**
     * 获取数据链路会话
     *
     * @param sessionId 会话ID
     * @return Session或null
     */
    public Session getSession(String sessionId) {
        return sessionRegistry.getDataSession(sessionId);
    }
}

