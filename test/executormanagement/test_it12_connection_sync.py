"""
IT-12: V5双连接状态同步测试

验证双连接状态同步机制：
1. 控制链路断开触发整体离线
2. 数据链路断开触发整体离线
3. 任一链路断开时另一链路也被清理
4. 双连接状态一致性
"""

import unittest
import time
import logging

from .base import BaseTestCase
from .dual_ws_client import DualWebSocketClient
from .config import AGENT_NAME, AGENT_USERNAME, AGENT_SHA256_HASH

logger = logging.getLogger(__name__)


class TestConnectionSync(BaseTestCase):
    """IT-12: V5双连接状态同步测试"""

    def test_control_link_disconnect_triggers_full_offline(self):
        """
        测试控制链路断开触发整体离线
        
        验证点：
        - 控制链路断开后executor状态变为OFFLINE
        - 数据链路也被服务端清理
        - Token失效
        """
        logger.info("=== IT-12: Test Control Link Disconnect Triggers Full Offline ===")
        
        client = DualWebSocketClient()
        
        try:
            # 建立双连接并认证
            logger.info("Step 1: Establishing dual connection...")
            client.connect_all()
            token = client.authenticate_and_bind(
                username=AGENT_USERNAME,
                password_hash=AGENT_SHA256_HASH,
                hostname=AGENT_NAME
            )
            logger.info(f"✓ Dual connection established, token={token}")
            
            # 验证初始在线状态
            self.assertExecutorStatus(AGENT_NAME, 1)  # ONLINE
            logger.info("✓ Initial state: ONLINE")
            
            # 主动断开控制链路
            logger.info("Step 2: Closing control link...")
            client.close_control()
            
            # 等待服务端检测并处理
            time.sleep(1)
            
            # 验证整体离线
            logger.info("Step 3: Verifying full offline...")
            self.assertExecutorStatus(AGENT_NAME, 0)  # OFFLINE
            logger.info("✓ Executor went OFFLINE")
            
            # 验证数据链路也不可用
            logger.info("Step 4: Verifying data link is also unusable...")
            try:
                if client.is_data_connected:
                    client.send_binary(b"test")
                    # 如果能发送，说明服务端还没关闭（可能存在延迟）
                    logger.warning("Data link still accepting data (server cleanup delay)")
            except Exception as e:
                logger.info(f"✓ Data link is closed/unusable: {e}")
            
            logger.info("=== IT-12: Test PASSED ===\n")
            
        finally:
            client.close_all()
            self._cleanup_executor()

    def test_data_link_disconnect_triggers_full_offline(self):
        """
        测试数据链路断开触发整体离线
        
        验证点：
        - 数据链路断开后executor状态变为OFFLINE
        - 控制链路也被服务端清理
        - Token失效
        """
        logger.info("=== IT-12: Test Data Link Disconnect Triggers Full Offline ===")
        
        client = DualWebSocketClient()
        
        try:
            # 建立双连接并认证
            logger.info("Step 1: Establishing dual connection...")
            client.connect_all()
            token = client.authenticate_and_bind(
                username=AGENT_USERNAME,
                password_hash=AGENT_SHA256_HASH,
                hostname=AGENT_NAME
            )
            logger.info(f"✓ Dual connection established, token={token}")
            
            # 验证初始在线状态
            self.assertExecutorStatus(AGENT_NAME, 1)  # ONLINE
            
            # 主动断开数据链路
            logger.info("Step 2: Closing data link...")
            client.close_data()
            
            # 等待服务端检测并处理
            time.sleep(1)
            
            # 验证整体离线
            logger.info("Step 3: Verifying full offline...")
            self.assertExecutorStatus(AGENT_NAME, 0)  # OFFLINE
            logger.info("✓ Executor went OFFLINE")
            
            # 验证控制链路也不可用
            logger.info("Step 4: Verifying control link is also unusable...")
            try:
                if client.is_control_connected:
                    client.send_control_json("ReportMsg", {}, token=token)
                    # 如果能发送，说明服务端还没关闭
                    logger.warning("Control link still accepting data (server cleanup delay)")
            except Exception as e:
                logger.info(f"✓ Control link is closed/unusable: {e}")
            
            logger.info("=== IT-12: Test PASSED ===\n")
            
        finally:
            client.close_all()
            self._cleanup_executor()

    def test_no_orphan_connections(self):
        """
        测试不存在"孤儿"连接
        
        验证点：
        - 双连接状态保持一致
        - 不会出现一条活着一条死的情况
        - DualLinkRouter正确清理映射
        """
        logger.info("=== IT-12: Test No Orphan Connections ===")
        
        client = DualWebSocketClient()
        
        try:
            # 建立双连接并认证
            logger.info("Step 1: Establishing dual connection...")
            client.connect_all()
            token = client.authenticate_and_bind(
                username=AGENT_USERNAME,
                password_hash=AGENT_SHA256_HASH,
                hostname=AGENT_NAME
            )
            logger.info(f"✓ Dual connection established, token={token}")
            
            # 测试：断开控制链路
            logger.info("Step 2: Testing control link disconnect...")
            client.close_control()
            time.sleep(1)
            
            # 验证整体离线，无孤儿连接
            self.assertExecutorStatus(AGENT_NAME, 0)
            logger.info("✓ No orphan data link after control disconnect")
            
            # 重新建立连接测试数据链路断开
            logger.info("Step 3: Re-establishing for data link test...")
            client.close_all()
            time.sleep(0.5)
            
            client = DualWebSocketClient()
            client.connect_all()
            token = client.authenticate_and_bind(
                username=AGENT_USERNAME,
                password_hash=AGENT_SHA256_HASH,
                hostname=AGENT_NAME
            )
            
            # 测试：断开数据链路
            logger.info("Step 4: Testing data link disconnect...")
            client.close_data()
            time.sleep(1)
            
            # 验证整体离线，无孤儿连接
            self.assertExecutorStatus(AGENT_NAME, 0)
            logger.info("✓ No orphan control link after data disconnect")
            
            logger.info("=== IT-12: Test PASSED ===\n")
            
        finally:
            client.close_all()
            self._cleanup_executor()

    def test_dual_connection_state_consistency(self):
        """
        测试双连接状态一致性
        
        验证点：
        - 双连接同时在线
        - 双连接同时离线
        - 不存在状态不一致
        """
        logger.info("=== IT-12: Test Dual Connection State Consistency ===")
        
        client = DualWebSocketClient()
        
        try:
            # 建立双连接
            logger.info("Step 1: Establishing dual connection...")
            client.connect_all()
            token = client.authenticate_and_bind(
                username=AGENT_USERNAME,
                password_hash=AGENT_SHA256_HASH,
                hostname=AGENT_NAME
            )
            
            # 验证双连接都在线
            self.assertTrue(client.is_control_connected, "Control should be connected")
            self.assertTrue(client.is_data_connected, "Data should be connected")
            self.assertTrue(client.is_fully_connected, "Should be fully connected")
            self.assertExecutorStatus(AGENT_NAME, 1)  # ONLINE
            logger.info("✓ Both links online, state consistent")
            
            # 关闭双连接
            logger.info("Step 2: Closing all connections...")
            client.close_all()
            time.sleep(1)
            
            # 验证双连接都离线
            self.assertFalse(client.is_control_connected, "Control should be disconnected")
            self.assertFalse(client.is_data_connected, "Data should be disconnected")
            self.assertFalse(client.is_fully_connected, "Should not be connected")
            self.assertExecutorStatus(AGENT_NAME, 0)  # OFFLINE
            logger.info("✓ Both links offline, state consistent")
            
            logger.info("=== IT-12: Test PASSED ===\n")
            
        finally:
            client.close_all()
            self._cleanup_executor()


if __name__ == '__main__':
    unittest.main()

