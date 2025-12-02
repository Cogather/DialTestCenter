/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;


import javax.websocket.Session;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * DualLinkRouter单元测试
 * 覆盖双链路绑定、解绑及状态查询逻辑
 *
 * @since 2025-12-02
 */
public class DualLinkRouterTest {

    private DualLinkRouter dualLinkRouter;

    @Mock
    private Session controlSession;

    @Mock
    private Session dataSession;

    @Before
    public void setUp() {
        dualLinkRouter = new DualLinkRouter();
        controlSession = mock(Session.class);
        dataSession = mock(Session.class);
        
        // 通用 Mock 设置
        when(controlSession.getId()).thenReturn("control-001");
        when(dataSession.getId()).thenReturn("data-001");
    }

    /**
     * 测试完整流程：注册控制链路 -> 绑定Token -> 注册数据链路 -> 验证状态
     */
    @Test
    public void testRegisterAndBindFlow_Success() {
        String token = "token-123";

        // 1. 注册控制链路
        dualLinkRouter.registerControlLink("control-001", controlSession);
        
        // 2. 绑定控制链路到 Token
        dualLinkRouter.bindControlLinkWithToken(token, "control-001", controlSession);
        assertEquals("Token mapping should be consistent", token, 
                dualLinkRouter.getTokenByControlSessionId("control-001"));
        assertEquals("Token mapping should be consistent", token, 
                dualLinkRouter.getTokenBySessionId("control-001"));

        // 3. 注册数据链路 (绑定成功)
        boolean bound = dualLinkRouter.registerDataLink(token, "data-001", dataSession);
        assertTrue("Data link should be bound successfully", bound);
        
        // 4. 验证状态
        assertTrue(dualLinkRouter.isDataLinkBound("data-001"));
        assertTrue(dualLinkRouter.isBothLinksActive(token));
        assertEquals(controlSession, dualLinkRouter.getControlSessionByToken(token));
        assertEquals(dataSession, dualLinkRouter.getDataSessionByToken(token));
        
        // 验证统计信息不为空
        assertNotNull(dualLinkRouter.getLinkStats());
    }

    /**
     * 测试注册数据链路失败：Token不存在
     */
    @Test
    public void testRegisterDataLink_TokenNotFound_Fail() {
        boolean bound = dualLinkRouter.registerDataLink("invalid-token", "data-001", dataSession);
        assertFalse("Should fail when token not found", bound);
    }

    /**
     * 测试注销控制链路逻辑
     */
    @Test
    public void testUnregisterControlLink_ShouldCleanupResources() {
        String token = "token-123";
        
        // 准备已绑定的环境
        dualLinkRouter.bindControlLinkWithToken(token, "control-001", controlSession);
        dualLinkRouter.registerDataLink(token, "data-001", dataSession);

        // 执行注销
        dualLinkRouter.unregisterControlLink("control-001");

        // 验证清理
        assertNull("Token mapping for control session should be removed", 
                dualLinkRouter.getTokenByControlSessionId("control-001"));
        // 由于是控制链路主动断开，根据逻辑 linkPair 的 controlActive 变为 false
        assertFalse("Both links should not be active", dualLinkRouter.isBothLinksActive(token));
        
        // 再次查询 linkPair 可能仍然存在（取决于是否完全清理），但状态应更新
        // 根据代码逻辑，只有当 !isAnyActive() 时才会移除 tokenLinks
        // 此处 dataActive 仍为 true (除非外部也调用了 unregisterDataLink)
    }
    
    /**
     * 测试注销数据链路逻辑
     */
    @Test
    public void testUnregisterDataLink_ShouldUpdateStatus() {
        String token = "token-123";
        
        // 准备已绑定的环境
        dualLinkRouter.bindControlLinkWithToken(token, "control-001", controlSession);
        dualLinkRouter.registerDataLink(token, "data-001", dataSession);

        // 执行注销
        dualLinkRouter.unregisterDataLink("data-001");

        // 验证清理
        assertNull("Token mapping for data session should be removed", 
                dualLinkRouter.getTokenByDataSessionId("data-001"));
        assertFalse("Data link should not be bound", dualLinkRouter.isDataLinkBound("data-001"));
        assertFalse("Both links should not be active", dualLinkRouter.isBothLinksActive(token));
    }

    /**
     * 测试完全注销流程（两者都断开）
     */
    @Test
    public void testFullUnregisterCycle_ResourcesCleaned() {
        String token = "token-123";
        dualLinkRouter.bindControlLinkWithToken(token, "control-001", controlSession);
        dualLinkRouter.registerDataLink(token, "data-001", dataSession);

        // 模拟两者断开
        dualLinkRouter.unregisterControlLink("control-001");
        dualLinkRouter.unregisterDataLink("data-001");

        // 验证完全清理
        assertNull(dualLinkRouter.getControlSessionByToken(token));
        assertNull(dualLinkRouter.getDataSessionByToken(token));
    }

    /**
     * 测试空参数防御
     */
    @Test
    public void testNullArguments_ShouldNotThrowException() {
        dualLinkRouter.registerControlLink("c-1", null);
        dualLinkRouter.bindControlLinkWithToken(null, "c-1", controlSession);
        dualLinkRouter.registerDataLink(null, "d-1", dataSession);
        dualLinkRouter.unregisterControlLink(null);
        dualLinkRouter.unregisterDataLink(null);
        
        assertFalse(dualLinkRouter.isDataLinkBound(null));
    }
}

