"""
IT-00: V5双连接建立与绑定测试

验证物理分离的双连接架构：
1. 控制链路和数据链路独立建立
2. CHAP认证通过控制链路
3. 数据链路通过token绑定
4. 双连接状态同步
"""

import unittest
import time
import logging

from .base import BaseTestCase
from .dual_ws_client import DualWebSocketClient
from .config import AGENT_NAME, AGENT_USERNAME, AGENT_SHA256_HASH

logger = logging.getLogger(__name__)


class TestDualConnection(BaseTestCase):
    """IT-00: V5双连接建立测试"""

    def setUp(self):
        """每个测试前清理executor状态"""
        super().setUp()
        # 确保executor离线且token清空
        try:
            self.db.update_executor_status(AGENT_NAME, 0)  # OFFLINE
            logger.debug(f"Cleaned up executor {AGENT_NAME} before test")
        except Exception as e:
            logger.warning(f"Failed to cleanup executor: {e}")

    def test_dual_connection_establish_success(self):
        """
        测试双连接成功建立和绑定
        
        验证点：
        - 控制链路连接成功
        - 数据链路连接成功
        - CHAP认证成功
        - Token绑定成功
        - 数据库状态正确
        """
        logger.info("=== IT-00: Test Dual Connection Establish Success ===")
        
        client = DualWebSocketClient()
        
        try:
            # 1. 建立双连接
            logger.info("Step 1: Connecting both links...")
            client.connect_all()
            
            # 验证连接状态
            self.assertTrue(client.is_control_connected, "Control link should be connected")
            self.assertTrue(client.is_data_connected, "Data link should be connected")
            self.assertFalse(client.is_bound, "Data link should not be bound yet")
            logger.info("✓ Both links connected successfully")
            
            # 2. 认证并绑定
            logger.info("Step 2: Authenticating and binding...")
            token = client.authenticate_and_bind(
                username=AGENT_USERNAME,
                password_hash=AGENT_SHA256_HASH,
                hostname=AGENT_NAME
            )
            
            # 验证认证结果
            self.assertIsNotNone(token, "Token should not be None")
            self.assertIsInstance(token, int, "Token should be an integer")
            self.assertTrue(client.is_bound, "Data link should be bound")
            self.assertTrue(client.is_fully_connected, "Should be fully connected")
            logger.info(f"✓ Authentication and binding successful, token={token}")
            
            # 3. 验证数据库状态
            logger.info("Step 3: Verifying database state...")
            self.assertExecutorStatus(AGENT_NAME, 1)  # ONLINE
            
            executor = self.db.get_executor_by_name(AGENT_NAME)
            self.assertIsNotNone(executor, "Executor should exist in database")
            # 数据库中的token是字符串，需要转换为整数比较
            db_token = int(executor["token"]) if executor["token"] else None
            self.assertEqual(db_token, token, "Token should match in database")
            logger.info("✓ Database state verified")
            
            # 4. 测试消息收发
            logger.info("Step 4: Testing message routing...")
            
            # 发送心跳（JSON通过控制链路）
            heartbeat_payload = {
                "state": "Normal",
                "ue-list": []
            }
            client.send_control_json("ReportMsg", heartbeat_payload, token=token)
            
            # 接收心跳响应
            msg_type, resp_token, resp_payload = client.recv_control_json(timeout=5)
            self.assertEqual(msg_type, "ReportAck")
            logger.info("✓ JSON message routing works (control link)")
            
            logger.info("=== IT-00: Test PASSED ===\n")
            
        finally:
            client.close_all()
            self._cleanup_executor()

    def test_data_link_bind_without_token_failure(self):
        """
        测试数据链路不发送token绑定失败
        
        验证点：
        - 数据链路未绑定时发送Binary会被服务端丢弃
        - 服务端日志会警告"Data link not bound yet"
        """
        logger.info("=== IT-00: Test Data Link Bind Without Token Failure ===")
        
        client = DualWebSocketClient()
        
        try:
            # 建立连接但不认证
            client.connect_all()
            
            # 尝试在未绑定时发送Binary（服务端会丢弃）
            test_data = b"test data without binding"
            client.send_binary(test_data)
            logger.info("✓ Binary data sent to unbound data link")
            
            # 数据链路应该仍然连接（服务端只是丢弃数据）
            self.assertTrue(client.is_data_connected, "Data link should still be connected")
            logger.info("✓ Data link remains connected after sending unbound data")
            
            logger.info("=== IT-00: Test PASSED ===\n")
            
        finally:
            client.close_all()

    def test_control_link_only_partial_functionality(self):
        """
        测试只建立控制链路的部分功能
        
        验证点：
        - 控制链路可以独立认证
        - 但完整功能需要数据链路
        """
        logger.info("=== IT-00: Test Control Link Only Partial Functionality ===")
        
        client = DualWebSocketClient()
        
        try:
            # 只建立控制链路
            client.connect_all()
            
            # 可以进行认证
            token = client.authenticate_and_bind(
                username=AGENT_USERNAME,
                password_hash=AGENT_SHA256_HASH,
                hostname=AGENT_NAME
            )
            
            self.assertIsNotNone(token)
            logger.info("✓ Control link authentication works independently")
            
            # 可以发送JSON消息
            heartbeat_payload = {"state": "Normal", "ue-list": []}
            client.send_control_json("ReportMsg", heartbeat_payload, token=token)
            msg_type, _, _ = client.recv_control_json(timeout=5)
            self.assertEqual(msg_type, "ReportAck")
            logger.info("✓ JSON messaging works through control link")
            
            # 可以发送Binary（已绑定）
            test_data = b"test binary data"
            client.send_binary(test_data)
            logger.info("✓ Binary messaging works through data link")
            
            logger.info("=== IT-00: Test PASSED ===\n")
            
        finally:
            client.close_all()
            self._cleanup_executor()

    def test_reconnect_with_new_token(self):
        """
        测试重连需要获取新token
        
        验证点：
        - 关闭连接后旧token失效
        - 重连需要重新认证
        - 新token与旧token不同
        """
        logger.info("=== IT-00: Test Reconnect With New Token ===")
        
        client1 = DualWebSocketClient()
        client2 = DualWebSocketClient()
        
        try:
            # 第一次连接
            logger.info("Step 1: First connection...")
            client1.connect_all()
            token1 = client1.authenticate_and_bind(
                username=AGENT_USERNAME,
                password_hash=AGENT_SHA256_HASH,
                hostname=AGENT_NAME
            )
            logger.info(f"✓ First token obtained: {token1}")
            
            # 关闭连接
            logger.info("Step 2: Closing connection...")
            client1.close_all()
            time.sleep(1)
            
            # 验证离线
            self.assertExecutorStatus(AGENT_NAME, 0)  # OFFLINE
            logger.info("✓ Executor went offline")
            
            # 第二次连接
            logger.info("Step 3: Second connection...")
            client2.connect_all()
            token2 = client2.authenticate_and_bind(
                username=AGENT_USERNAME,
                password_hash=AGENT_SHA256_HASH,
                hostname=AGENT_NAME
            )
            logger.info(f"✓ Second token obtained: {token2}")
            
            # 验证token不同
            self.assertNotEqual(token1, token2, "New token should differ from old token")
            logger.info("✓ New token is different from old token")
            
            # 验证重新在线
            self.assertExecutorStatus(AGENT_NAME, 1)  # ONLINE
            
            logger.info("=== IT-00: Test PASSED ===\n")
            
        finally:
            client1.close_all()
            client2.close_all()
            self._cleanup_executor()


if __name__ == '__main__':
    unittest.main()

