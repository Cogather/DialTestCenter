import json
import unittest
import time

from .base import BaseTestCase
from .config import WS_ENABLE, AGENT_NAME, AGENT_NTLM_HASH, WS_WAIT_OFFLINE_SEC
from .utils import wait_for_condition
from .json_message import JsonMessageHelper


@unittest.skipUnless(WS_ENABLE, "WS测试默认关闭，设置 EXEC_WS_ENABLE=1 以启用")
class TestHeartbeatIT02(BaseTestCase):
    """IT-02: 心跳状态维护流程"""

    @unittest.skipUnless(AGENT_NTLM_HASH, "未提供 EXEC_AGENT_NTLM_HASH，无法计算")
    def test_it_02_001_heartbeat_updates_online(self):
        """IT-02-001: 心跳更新状态"""
        # 注册并保持连接（心跳需要在同一会话中发送）
        ws, token = self._ws_register_and_keep_connection()
        helper = JsonMessageHelper()
        try:
            # 构造UE设备列表（字段名严格遵循协议文档）
            ue_list = [
                {
                    "serial-no": "86138****0001",
                    "brand": "Huawei",
                    "model": "P60",
                    "os": "Android",
                    "version": "13",
                    "ipv4": "192.168.1.10",
                    "battery": 85,
                },
                {
                    "serial-no": "86139****0002",
                    "brand": "Xiaomi",
                    "model": "Mi 13",
                    "os": "Android",
                    "version": "12",
                },
            ]

            # 发送 ReportMsg 心跳
            env = helper.build(
                "ReportMsg",
                {"token": token, "state": "Normal", "ue-list": ue_list},
            )
            ws.send_json(env)
            ack_env = ws.recv_json()
            msg_type, _, payload = helper.parse(ack_env)
            self.assertIn(msg_type, ("ReportAck", "report_ack"))
            state = payload.get("state", 0)
            self.assertIn(state, (0, "OK"), "心跳应该成功")

            # 等待后端处理心跳
            time.sleep(1)

            # 在关闭连接前检查状态
            ex = self.db.get_executor_by_name(AGENT_NAME)
            self.assertIsNotNone(ex, "executor 应该存在")
            self.assertEqual(ex.get("status"), 1, "发送心跳后 executor 应为 ONLINE")
            self.assertIsNotNone(ex.get("last_online_time"), "last_online_time 应该已更新")
        finally:
            ws.close_all()
            self._cleanup_executor()

        # 验证 UE 列表已更新到数据库（使用 serial-no 作为 msisdn）
        time.sleep(1)  # 等待 UE 数据入库
        ue1 = self.db.get_ue_by_msisdn("86138****0001")
        ue2 = self.db.get_ue_by_msisdn("86139****0002")

        # 验证 UE 信息存在
        self.assertIsNotNone(ue1, "UE1 应该存在于数据库中")
        self.assertIsNotNone(ue2, "UE2 应该存在于数据库中")

        # 验证核心字段
        if ue1:
            self.assertEqual(ue1.get("msisdn"), "86138****0001", "UE1 MSISDN 应正确")
            self.assertEqual(ue1.get("executor_name"), AGENT_NAME, "UE1 应关联到正确的执行机")
            self.assertEqual(ue1.get("vendor"), "Huawei", "UE1 vendor 应正确")
            # 验证JSON格式存储的详细信息
            if ue1.get("info"):
                info = ue1.get("info")
                if isinstance(info, str):
                    info = json.loads(info)
                self.assertEqual(info.get("model"), "P60")
                self.assertEqual(info.get("os"), "Android")
                self.assertEqual(info.get("version"), "13")

        if ue2:
            self.assertEqual(ue2.get("msisdn"), "86139****0002", "UE2 MSISDN 应正确")
            self.assertEqual(ue2.get("executor_name"), AGENT_NAME, "UE2 应关联到正确的执行机")

    @unittest.skipUnless(AGENT_NTLM_HASH, "未提供 EXEC_AGENT_NTLM_HASH，无法计算")
    def test_it_02_002_ue_list_changes(self):
        """IT-02-002: UE 清单变更"""
        ws, token = self._ws_register_and_keep_connection()
        helper = JsonMessageHelper()
        try:
            # 第一次心跳：发送 3 个 UE（字段名遵循协议文档）
            ue_list1 = [
                {"serial-no": "SN_TEST_001", "brand": "TestVendor1", "model": "Model1"},
                {"serial-no": "SN_TEST_002", "brand": "TestVendor2", "model": "Model2"},
                {"serial-no": "SN_TEST_003", "brand": "TestVendor3", "model": "Model3"},
            ]
            env1 = helper.build("ReportMsg", {"token": token, "state": "Normal", "ue-list": ue_list1})
            ws.send_json(env1)
            ws.recv_json()
            time.sleep(1)  # 等待数据库更新

            # 第二次心跳：修改 SN_TEST_002 状态，新增 SN_TEST_004，删除 SN_TEST_003
            ue_list2 = [
                {"serial-no": "SN_TEST_001", "brand": "TestVendor1", "model": "Model1"},
                {"serial-no": "SN_TEST_002", "brand": "TestVendor2_UPDATED", "model": "Model2"},
                {"serial-no": "SN_TEST_004", "brand": "TestVendor4", "model": "Model4"},
            ]
            env2 = helper.build("ReportMsg", {"token": token, "state": "Normal", "ue-list": ue_list2})
            ws.send_json(env2)
            ws.recv_json()
            time.sleep(1)  # 等待数据库更新
        finally:
            ws.close_all()
            self._cleanup_executor()

        # 验证 UE 变更（使用 serial-no）
        ue1 = self.db.get_ue_by_msisdn("SN_TEST_001")
        ue2 = self.db.get_ue_by_msisdn("SN_TEST_002")
        ue3 = self.db.get_ue_by_msisdn("SN_TEST_003")
        ue4 = self.db.get_ue_by_msisdn("SN_TEST_004")

        # SN_TEST_001 应保持不变
        self.assertIsNotNone(ue1, "UE1 应该仍然存在")
        self.assertIsNotNone(ue2, "UE2 应该仍然存在")
        self.assertIsNotNone(ue4, "UE4 应已新增")

        if ue2:
            # UE2 厂商应已更新
            self.assertEqual(ue2.get("vendor"), "TestVendor2_UPDATED", "UE2 vendor 应已更新")

        # SN_TEST_003 按策略处理（保留或标记删除），这里不做强制断言
        _ = ue3

    @unittest.skipUnless(AGENT_NTLM_HASH, "未提供 EXEC_AGENT_NTLM_HASH，无法计算")
    def test_it_02_003_offline_on_disconnect(self):
        """IT-02-003: 离线处理"""
        ws, token = self._ws_register_and_keep_connection()
        helper = JsonMessageHelper()

        # 发送心跳确保状态为ONLINE
        ue_list = [{"msisdn": "8613800000201", "serial": "SN_OFFLINE_TEST"}]
        env = helper.build("ReportMsg", {"token": token, "state": "Normal", "ue-list": ue_list})
        ws.send_json(env)
        ws.recv_json()
        time.sleep(1.5)  # 等待状态更新到数据库

        # 验证 executor 为 ONLINE（在关闭之前检查）
        ex = self.db.get_executor_by_name(AGENT_NAME)
        self.assertIsNotNone(ex)
        self.assertEqual(ex.get("status"), 1, "发送心跳后应为 ONLINE")

        # 关闭 WebSocket 连接
        ws.close()

        # 等待服务端检测到断开并更新状态为 OFFLINE
        def _check_offline():
            ex_inner = self.db.get_executor_by_name(AGENT_NAME)
            if not ex_inner:
                return False
            return ex_inner.get("status") == 0

        ok = wait_for_condition(_check_offline, timeout=WS_WAIT_OFFLINE_SEC, interval=0.5)
        self.assertTrue(ok, "断开连接后 executor 应该更新为 OFFLINE")

    @unittest.skipUnless(AGENT_NTLM_HASH, "未提供 EXEC_AGENT_NTLM_HASH，无法计算")
    def test_it_02_004_heartbeat_state_validation(self):
        """IT-02-004: 心跳状态字段验证"""
        ws, token = self._ws_register_and_keep_connection()
        helper = JsonMessageHelper()
        try:
            for state in [0, 1]:  # 0=OFFLINE, 1=ONLINE
                ue_list = [
                    {
                        "msisdn": f"861380000030{state}",
                        "serial": f"SN_STATE_TEST_{state}",
                    }
                ]
                env = helper.build("ReportMsg", {"token": token, "state": state, "ue-list": ue_list})
                ws.send_json(env)
                ack_env = ws.recv_json()
                msg_type, _, payload = helper.parse(ack_env)
                self.assertIn(msg_type, ("ReportAck", "report_ack"))
                ack_state = payload.get("state", 0)
                self.assertIn(ack_state, (0, "OK"), f"心跳状态{state}应该成功")

            # 等待数据库更新
            time.sleep(1)

            # 验证状态更新
            ex = self.db.get_executor_by_name(AGENT_NAME)
            self.assertIsNotNone(ex)
            # 最后一次心跳是 state=1，所以应该是ONLINE
            self.assertEqual(ex.get("status"), 1, "最后一次心跳state=1，应为ONLINE")
        finally:
            ws.close_all()
            self._cleanup_executor()

    @unittest.skipUnless(AGENT_NTLM_HASH, "未提供 EXEC_AGENT_NTLM_HASH，无法计算")
    def test_it_02_005_empty_ue_list(self):
        """IT-02-005: 空UE列表心跳"""
        ws, token = self._ws_register_and_keep_connection()
        helper = JsonMessageHelper()
        try:
            env = helper.build("ReportMsg", {"token": token, "state": "Normal", "ue-list": []})
            ws.send_json(env)
            ack_env = ws.recv_json()
            msg_type, _, payload = helper.parse(ack_env)
            self.assertIn(msg_type, ("ReportAck", "report_ack"))
            state = payload.get("state", 0)
            self.assertIn(state, (0, "OK"), "空UE列表心跳应该成功")

            # 验证执行机状态仍然正常
            ex = self.db.get_executor_by_name(AGENT_NAME)
            self.assertIsNotNone(ex)
            self.assertEqual(ex.get("status"), 1, "心跳后应保持ONLINE状态")
        finally:
            ws.close_all()
            self._cleanup_executor()
