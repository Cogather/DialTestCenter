/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.config;

import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.ControlLinkEndpoint;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.DataLinkEndpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

/**
 * V5版本: 双连接WebSocket配置-物理分离版本
 * 注册两个独立的WebSocket端点:
 * 1. 控制链路端点: /ws/executor/control
 * 2. 数据链路端点: /ws/executor/data
 * 配置独立的缓冲区和超时参数
 *
 * @author g00940940
 * @since 2025-11-20
 */
@Configuration
public class WebSocketJsr356Config {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketJsr356Config.class);

    @Autowired
    private ServletContext servletContext;

    @Autowired
    private ControlLinkEndpoint controlLinkEndpoint;

    @Autowired
    private DataLinkEndpoint dataLinkEndpoint;

    @Value("${websocket.control.text-buffer-size:2097152}")
    private int controlTextBufferSize;

    @Value("${websocket.data.binary-buffer-size:10485760}")
    private int dataBinaryBufferSize;

    @Value("${websocket.max-idle-timeout:300000}")
    private long maxIdleTimeout;

    /**
     * 注册双连接WebSocket端点
     * V5: 物理分离-注册两个独立端点
     */
    @PostConstruct
    public void registerWebSocketEndpoints() {
        try {
            ServerContainer serverContainer = (ServerContainer) servletContext
                    .getAttribute(ServerContainer.class.getName());
            
            if (serverContainer == null) {
                logger.error("ServerContainer not found in ServletContext");
                throw new IllegalStateException("ServerContainer not available");
            }
            
            serverContainer.setDefaultMaxTextMessageBufferSize(controlTextBufferSize);
            serverContainer.setDefaultMaxBinaryMessageBufferSize(dataBinaryBufferSize);
            serverContainer.setDefaultMaxSessionIdleTimeout(maxIdleTimeout);
            
            logger.info("WebSocket dual-connection container configured: " +
                    "controlTextBuffer={}KB, dataBinaryBuffer={}KB, idleTimeout={}s",
                    controlTextBufferSize / 1024, dataBinaryBufferSize / 1024, maxIdleTimeout / 1000);
            
            ServerEndpointConfig controlConfig = ServerEndpointConfig.Builder
                    .create(ControlLinkEndpoint.class, "/ws/executor/control")
                    .configurator(new SpringAwareEndpointConfigurator<>(controlLinkEndpoint))
                    .build();
            
            ServerEndpointConfig dataConfig = ServerEndpointConfig.Builder
                    .create(DataLinkEndpoint.class, "/ws/executor/data")
                    .configurator(new SpringAwareEndpointConfigurator<>(dataLinkEndpoint))
                    .build();
            
            serverContainer.addEndpoint(controlConfig);
            logger.info("Control link endpoint registered successfully: /ws/executor/control");
            
            serverContainer.addEndpoint(dataConfig);
            logger.info("Data link endpoint registered successfully: /ws/executor/data");
            
            logger.info("WebSocket dual-connection endpoints registered successfully (V5: physical separation)");
        } catch (DeploymentException e) {
            logger.error("Failed to register dual-connection WebSocket endpoints", e);
            throw new IllegalStateException("Failed to register dual-connection WebSocket endpoints", e);
        }
    }

    /**
     * 自定义配置器,使用Spring管理的端点实例
     *
     * @param <T> 端点类型
     */
    private static class SpringAwareEndpointConfigurator<T> extends ServerEndpointConfig.Configurator {
        private final T endpoint;

        /**
         * 构造函数
         *
         * @param endpoint WebSocket端点实例
         */
        public SpringAwareEndpointConfigurator(T endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public <E> E getEndpointInstance(Class<E> endpointClass) throws InstantiationException {
            return endpointClass.cast(endpoint);
        }
    }
}


