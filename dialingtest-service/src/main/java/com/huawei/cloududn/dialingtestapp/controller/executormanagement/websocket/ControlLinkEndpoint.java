/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket;

import com.huawei.cloududn.dialingtestapp.service.executormanagement.ExecutorMgmtService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 * V5版本: 控制链路WebSocket端点
 * 物理分离的控制链路,专门处理JSON信令消息
 * 负责CHAP认证流程,生成token并注册到DualLinkRouter
 *
 * @author g00940940
 * @since 2025-11-20
 */
@Component
@ServerEndpoint("/ws/executor/control")
public class ControlLinkEndpoint {
    private static final Logger logger = LoggerFactory.getLogger(ControlLinkEndpoint.class);

    private static WebSocketSessionRegistry sessionRegistry;

    private static WssMessageDispatcher dispatcher;

    private static DualLinkRouter dualLinkRouter;

    private static ExecutorMgmtService executorMgmtService;

    @Autowired
    public void setSessionRegistry(WebSocketSessionRegistry registry) {
        ControlLinkEndpoint.sessionRegistry = registry;
    }

    @Autowired
    public void setDispatcher(WssMessageDispatcher disp) {
        ControlLinkEndpoint.dispatcher = disp;
    }

    @Autowired
    public void setDualLinkRouter(DualLinkRouter router) {
        ControlLinkEndpoint.dualLinkRouter = router;
    }

    @Autowired
    public void setExecutorMgmtService(ExecutorMgmtService service) {
        ControlLinkEndpoint.executorMgmtService = service;
    }

    /**
     * 控制链路连接建立事件
     * V5: 注册控制链路会话
     *
     * @param session WebSocket会话
     */
    @OnOpen
    public void onOpen(Session session) {
        String sessionId = session.getId();
        sessionRegistry.addControlSession(session);
        dualLinkRouter.registerControlLink(sessionId, session);
        logger.info("Control link connected, sessionId={}", sessionId);
    }

    /**
     * 接收JSON信令消息
     * V5: 控制链路只处理Text Message
     *
     * @param message JSON消息字符串
     * @param session WebSocket会话
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        if (message == null || message.isEmpty()) {
            logger.warn("Received empty control message, sessionId={}", session.getId());
            return;
        }
        try {
            String sessionId = session.getId();
            logger.debug("Received control message, sessionId={}, size={} chars", 
                    sessionId, message.length());

            dispatcher.dispatchControl(message, session);
        } catch (Exception e) {
            logger.error("Failed to process control message, sessionId={}", session.getId(), e);
        }
    }

    /**
     * 控制链路连接关闭事件
     * V5: 清理控制链路资源并触发双链路清理
     *
     * @param session WebSocket会话
     * @param reason 关闭原因
     */
    @OnClose
    public void onClose(Session session, CloseReason reason) {
        String sessionId = session.getId();
        
        // Get token before unregistering to ensure we can clean up resources
        String token = dualLinkRouter.getTokenBySessionId(sessionId);
        
        // Capture data session before unregistering
        Session dataSession = null;
        if (token != null) {
            dataSession = dualLinkRouter.getDataSessionByToken(token);
        }
        
        dualLinkRouter.unregisterControlLink(sessionId);
        sessionRegistry.removeControlSession(sessionId);
        
        // Handle business logic disconnect (database update)
        executorMgmtService.handleExecutorDisconnect(sessionId);
        
        // Clean up send queue resources using the token
        if (token != null) {
            executorMgmtService.removeSendQueue(token);
        }
        
        // Force close data session if open
        if (dataSession != null && dataSession.isOpen()) {
            try {
                logger.info("Control link disconnected, closing associated data link, sessionId={}", dataSession.getId());
                dataSession.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, 
                        "Control link disconnected"));
            } catch (Exception e) {
                logger.error("Failed to close data session", e);
            }
        }
        
        logger.info("Control link disconnected, sessionId={}, reason={}", 
                sessionId, reason.getReasonPhrase());
    }

    /**
     * 错误事件
     *
     * @param session WebSocket会话
     * @param throwable 异常信息
     */
    @OnError
    public void onError(Session session, Throwable throwable) {
        logger.error("Control link error, sessionId={}", session.getId(), throwable);
    }

    /**
     * 获取控制链路会话
     *
     * @param sessionId 会话ID
     * @return Session或null
     */
    public Session getSession(String sessionId) {
        return sessionRegistry.getControlSession(sessionId);
    }
}

