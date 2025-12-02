/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.Session;

/**
 * V5双链路路由器-物理分离版本
 * 负责通过token关联控制链路和数据链路两条物理连接
 * 管理双链路的绑定、同步和状态一致性
 *
 * @author g00940940
 * @since 2025-11-20
 */
@Component
public class DualLinkRouter {
    private static final Logger logger = LoggerFactory.getLogger(DualLinkRouter.class);

    private final Map<String, LinkPair> tokenLinks = new ConcurrentHashMap<>();
    
    private final Map<String, String> controlSessionToToken = new ConcurrentHashMap<>();
    
    private final Map<String, String> dataSessionToToken = new ConcurrentHashMap<>();

    /**
     * 链路对,包含控制链路和数据链路的两个独立Session
     */
    private static class LinkPair {
        private Session controlSession;
        private Session dataSession;
        private final String controlSessionId;
        private String dataSessionId;
        private volatile boolean controlActive;
        private volatile boolean dataActive;
        private volatile boolean dataBound;

        LinkPair(String controlSessionId, Session controlSession) {
            this.controlSessionId = controlSessionId;
            this.controlSession = controlSession;
            this.controlActive = true;
            this.dataActive = false;
            this.dataBound = false;
        }

        void bindDataLink(String dataSessionId, Session dataSession) {
            this.dataSessionId = dataSessionId;
            this.dataSession = dataSession;
            this.dataActive = true;
            this.dataBound = true;
        }

        boolean isBothActive() {
            return controlActive && dataActive && dataBound;
        }

        boolean isAnyActive() {
            return controlActive || dataActive;
        }
    }

    /**
     * 注册控制链路
     * V5: 控制链路先建立,生成token后等待数据链路绑定
     *
     * @param controlSessionId 控制链路会话ID
     * @param controlSession 控制链路Session
     */
    public void registerControlLink(String controlSessionId, Session controlSession) {
        if (controlSession == null) {
            logger.warn("Attempted to register null control session");
            return;
        }
        logger.info("Control link registered, waiting for token generation, sessionId={}", 
                controlSessionId);
    }

    /**
     * 绑定控制链路到token
     * V5: 认证成功后调用,将token和控制链路关联
     *
     * @param token 认证token
     * @param controlSessionId 控制链路会话ID
     * @param controlSession 控制链路Session
     */
    public void bindControlLinkWithToken(String token, String controlSessionId, Session controlSession) {
        if (token == null || controlSession == null) {
            logger.warn("Invalid token or control session");
            return;
        }

        LinkPair linkPair = new LinkPair(controlSessionId, controlSession);
        tokenLinks.put(token, linkPair);
        controlSessionToToken.put(controlSessionId, token);
        
        logger.info("Control link bound to token, sessionId={}, token={}", controlSessionId, token);
    }

    /**
     * 注册数据链路并绑定到token
     * V5: 数据链路携带token连接,完成双链路绑定
     *
     * @param token 认证token
     * @param dataSessionId 数据链路会话ID
     * @param dataSession 数据链路Session
     * @return 绑定是否成功
     */
    public boolean registerDataLink(String token, String dataSessionId, Session dataSession) {
        if (token == null || dataSession == null) {
            logger.warn("Invalid token or data session");
            return false;
        }

        LinkPair linkPair = tokenLinks.get(token);
        if (linkPair == null) {
            logger.error("Control link not found for token, cannot bind data link, token={}", token);
            return false;
        }

        linkPair.bindDataLink(dataSessionId, dataSession);
        dataSessionToToken.put(dataSessionId, token);
        
        logger.info("Data link bound successfully, sessionId={}, token={}, dual link established", 
                dataSessionId, token);
        return true;
    }

    /**
     * 注销控制链路
     *
     * @param controlSessionId 控制链路会话ID
     */
    public void unregisterControlLink(String controlSessionId) {
        if (controlSessionId == null) {
            logger.warn("Attempted to unregister control link with null id");
            return;
        }

        String token = controlSessionToToken.remove(controlSessionId);
        if (token != null) {
            LinkPair linkPair = tokenLinks.get(token);
            if (linkPair != null) {
                linkPair.controlActive = false;
                if (!linkPair.isAnyActive()) {
                    tokenLinks.remove(token);
                    if (linkPair.dataSessionId != null) {
                        dataSessionToToken.remove(linkPair.dataSessionId);
                    }
                }
            }
            logger.info("Control link unregistered, sessionId={}, token={}", controlSessionId, token);
        } else {
            logger.warn("Token not found for control link, sessionId={}", controlSessionId);
        }
    }

    /**
     * 注销数据链路
     *
     * @param dataSessionId 数据链路会话ID
     */
    public void unregisterDataLink(String dataSessionId) {
        if (dataSessionId == null) {
            logger.warn("Attempted to unregister data link with null id");
            return;
        }

        String token = dataSessionToToken.remove(dataSessionId);
        if (token != null) {
            LinkPair linkPair = tokenLinks.get(token);
            if (linkPair != null) {
                linkPair.dataActive = false;
                linkPair.dataBound = false;
                if (!linkPair.isAnyActive()) {
                    tokenLinks.remove(token);
                    controlSessionToToken.remove(linkPair.controlSessionId);
                }
            }
            logger.info("Data link unregistered, sessionId={}, token={}", dataSessionId, token);
        } else {
            logger.warn("Token not found for data link, sessionId={}", dataSessionId);
        }
    }

    /**
     * 检查数据链路是否已绑定
     *
     * @param dataSessionId 数据链路会话ID
     * @return 是否已绑定
     */
    public boolean isDataLinkBound(String dataSessionId) {
        if (dataSessionId == null) {
            return false;
        }
        String token = dataSessionToToken.get(dataSessionId);
        if (token == null) {
            return false;
        }
        LinkPair linkPair = tokenLinks.get(token);
        return linkPair != null && linkPair.dataBound;
    }

    /**
     * 检查双链路是否都活跃
     *
     * @param token 认证token
     * @return 是否都活跃
     */
    public boolean isBothLinksActive(String token) {
        if (token == null) {
            return false;
        }
        LinkPair linkPair = tokenLinks.get(token);
        if (linkPair == null) {
            return false;
        }
        return linkPair.isBothActive();
    }

    /**
     * 通过token获取控制链路Session
     *
     * @param token 认证token
     * @return Session或null
     */
    public Session getControlSessionByToken(String token) {
        if (token == null) {
            return null;
        }
        LinkPair linkPair = tokenLinks.get(token);
        if (linkPair == null) {
            return null;
        }
        return linkPair.controlSession;
    }

    /**
     * 通过token获取数据链路Session
     *
     * @param token 认证token
     * @return Session或null
     */
    public Session getDataSessionByToken(String token) {
        if (token == null) {
            return null;
        }
        LinkPair linkPair = tokenLinks.get(token);
        if (linkPair == null) {
            return null;
        }
        return linkPair.dataSession;
    }

    /**
     * 通过控制链路sessionId获取token
     *
     * @param controlSessionId 控制链路会话ID
     * @return token或null
     */
    public String getTokenByControlSessionId(String controlSessionId) {
        if (controlSessionId == null) {
            return null;
        }
        return controlSessionToToken.get(controlSessionId);
    }

    /**
     * 通过数据链路sessionId获取token
     *
     * @param dataSessionId 数据链路会话ID
     * @return token或null
     */
    public String getTokenByDataSessionId(String dataSessionId) {
        if (dataSessionId == null) {
            return null;
        }
        return dataSessionToToken.get(dataSessionId);
    }

    /**
     * 通过任意sessionId获取token(兼容方法)
     *
     * @param sessionId 会话ID
     * @return token或null
     */
    public String getTokenBySessionId(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        String token = controlSessionToToken.get(sessionId);
        if (token == null) {
            token = dataSessionToToken.get(sessionId);
        }
        return token;
    }

    /**
     * 获取链路统计信息
     *
     * @return 统计信息字符串
     */
    public String getLinkStats() {
        int totalTokens = tokenLinks.size();
        long activeBoth = tokenLinks.values().stream().filter(LinkPair::isBothActive).count();
        long activeAny = tokenLinks.values().stream().filter(LinkPair::isAnyActive).count();
        int controlLinks = controlSessionToToken.size();
        int dataLinks = dataSessionToToken.size();

        return String.format("LinkStats[tokens=%d, bothActive=%d, anyActive=%d, control=%d, data=%d]",
                totalTokens, activeBoth, activeAny, controlLinks, dataLinks);
    }
}


