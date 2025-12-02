/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.config;

import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.ControlLinkEndpoint;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.DataLinkEndpoint;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.ServletContext;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for WebSocketJsr356Config
 *
 * @since 2025-12-02
 */
@RunWith(MockitoJUnitRunner.class)
public class WebSocketJsr356ConfigTest {

    @Mock
    private ServletContext servletContext;

    @Mock
    private ControlLinkEndpoint controlLinkEndpoint;

    @Mock
    private DataLinkEndpoint dataLinkEndpoint;

    @Mock
    private ServerContainer serverContainer;

    @InjectMocks
    private WebSocketJsr356Config webSocketJsr356Config;

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(webSocketJsr356Config, "controlTextBufferSize", 2097152);
        ReflectionTestUtils.setField(webSocketJsr356Config, "dataBinaryBufferSize", 10485760);
        ReflectionTestUtils.setField(webSocketJsr356Config, "maxIdleTimeout", 300000L);
    }

    @Test
    public void testRegisterWebSocketEndpoints_Success() throws DeploymentException, InstantiationException {
        // Arrange
        when(servletContext.getAttribute(ServerContainer.class.getName())).thenReturn(serverContainer);

        // Act
        webSocketJsr356Config.registerWebSocketEndpoints();

        // Assert
        verify(serverContainer).setDefaultMaxTextMessageBufferSize(2097152);
        verify(serverContainer).setDefaultMaxBinaryMessageBufferSize(10485760);
        verify(serverContainer).setDefaultMaxSessionIdleTimeout(300000L);

        ArgumentCaptor<ServerEndpointConfig> captor = ArgumentCaptor.forClass(ServerEndpointConfig.class);
        verify(serverContainer, times(2)).addEndpoint(captor.capture());

        List<ServerEndpointConfig> configs = captor.getAllValues();

        // Verify Control Config
        ServerEndpointConfig controlConfig = configs.stream()
                .filter(c -> c.getPath().equals("/ws/executor/control"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Control endpoint not registered"));
        assertEquals(ControlLinkEndpoint.class, controlConfig.getEndpointClass());
        assertNotNull(controlConfig.getConfigurator());
        assertEquals(controlLinkEndpoint, controlConfig.getConfigurator().getEndpointInstance(ControlLinkEndpoint.class));

        // Verify Data Config
        ServerEndpointConfig dataConfig = configs.stream()
                .filter(c -> c.getPath().equals("/ws/executor/data"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Data endpoint not registered"));
        assertEquals(DataLinkEndpoint.class, dataConfig.getEndpointClass());
        assertNotNull(dataConfig.getConfigurator());
        assertEquals(dataLinkEndpoint, dataConfig.getConfigurator().getEndpointInstance(DataLinkEndpoint.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testRegisterWebSocketEndpoints_ServerContainerNull_ThrowsException() {
        // Arrange
        when(servletContext.getAttribute(ServerContainer.class.getName())).thenReturn(null);

        // Act
        webSocketJsr356Config.registerWebSocketEndpoints();
    }

    @Test(expected = IllegalStateException.class)
    public void testRegisterWebSocketEndpoints_DeploymentException_ThrowsException() throws DeploymentException {
        // Arrange
        when(servletContext.getAttribute(ServerContainer.class.getName())).thenReturn(serverContainer);
        doThrow(new DeploymentException("Test Exception")).when(serverContainer).addEndpoint(any(ServerEndpointConfig.class));

        // Act
        webSocketJsr356Config.registerWebSocketEndpoints();
    }
}

