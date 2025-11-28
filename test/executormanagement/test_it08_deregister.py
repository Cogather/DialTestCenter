import unittest
import time

from .base import BaseTestCase
from .config import WS_ENABLE, AGENT_NAME, AGENT_NTLM_HASH, WS_WAIT_OFFLINE_SEC
from .utils import wait_for_condition
from .json_message import JsonMessageHelper


@unittest.skipUnless(WS_ENABLE, "WS测试默认关闭，设置 EXEC_WS_ENABLE=1 以启用")
class TestDeregisterIT08(BaseTestCase):
    """IT-08: Agent注销流程"""

    @unittest.skipUnless(AGENT_NTLM_HASH, "未提供 EXEC_AGENT_NTLM_HASH，无法计算")
    def test_it_08_001_deregister_success(self):
        """IT-08-001: 成功注销"""
        ws, token = self._ws_register_and_keep_connection()
        try:
            ex = self.db.get_executor_by_name(AGENT_NAME)
            self.assertIsNotNone(ex)
            self.assertEqual(ex.get("status"), 1)

            ws.send_control_json(
                "DeRegisterRequest",
                {"hostname": AGENT_NAME},
                token=int(token) if str(token).isdigit() else None,
            )

            time.sleep(0.5)
        finally:
            try:
                ws.close()
            except Exception:
                pass

        def _check_offline():
            ex_inner = self.db.get_executor_by_name(AGENT_NAME)
            if not ex_inner:
                return False
            return ex_inner.get("status") == 0

        ok = wait_for_condition(_check_offline, timeout=WS_WAIT_OFFLINE_SEC, interval=0.5)
        self.assertTrue(ok, "注销后executor应该更新为OFFLINE状态")

    @unittest.skipUnless(AGENT_NTLM_HASH, "未提供 EXEC_AGENT_NTLM_HASH，无法计算")
    def test_it_08_002_deregister_not_registered(self):
        """IT-08-002: 注销未注册的执行机"""
        ws = self._open_ws()
        try:
            ws.send_control_json("DeRegisterRequest", {"hostname": "NonExistentExecutor"}, token=None)
        finally:
            ws.close()

    @unittest.skipUnless(AGENT_NTLM_HASH, "未提供 EXEC_AGENT_NTLM_HASH，无法计算")
    def test_it_08_003_deregister_after_disconnect(self):
        """IT-08-003: 断开连接后的注销"""
        token = self._ws_register_and_get_token()

        ws = self._open_ws()
        try:
            ws.send_control_json(
                "DeRegisterRequest",
                {"hostname": AGENT_NAME},
                token=int(token) if str(token).isdigit() else None,
            )
        finally:
            ws.close()

        ex = self.db.get_executor_by_name(AGENT_NAME)
        self.assertIsNotNone(ex)

    @unittest.skipUnless(AGENT_NTLM_HASH, "未提供 EXEC_AGENT_NTLM_HASH，无法计算")
    def test_it_08_004_multiple_deregister(self):
        """IT-08-004: 重复注销"""
        token = self._ws_register_and_get_token()

        for _ in range(3):
            ws = self._open_ws()
            try:
                ws.send_control_json(
                    "DeRegisterRequest",
                    {"hostname": AGENT_NAME},
                    token=int(token) if str(token).isdigit() else None,
                )
            finally:
                ws.close()

        ex = self.db.get_executor_by_name(AGENT_NAME)
        self.assertIsNotNone(ex)
        self.assertEqual(ex.get("status"), 0, "重复注销后状态应该稳定为OFFLINE")

    @unittest.skipUnless(AGENT_NTLM_HASH, "未提供 EXEC_AGENT_NTLM_HASH，无法计算")
    def test_it_08_005_deregister_during_task(self):
        """IT-08-005: 任务执行期间注销"""
        ws, token = self._ws_register_and_keep_connection()
        try:
            ws.send_control_json(
                "DeRegisterRequest",
                {"hostname": AGENT_NAME},
                token=int(token) if str(token).isdigit() else None,
            )
        finally:
            ws.close()

        time.sleep(WS_WAIT_OFFLINE_SEC)

        ex = self.db.get_executor_by_name(AGENT_NAME)
        self.assertIsNotNone(ex)
        self.assertEqual(ex.get("status"), 0, "任务执行期间注销后也应该变为OFFLINE")

    @unittest.skipUnless(AGENT_NTLM_HASH, "未提供 EXEC_AGENT_NTLM_HASH，无法计算")
    def test_it_08_006_deregister_cleanup_verification(self):
        """IT-08-006: 注销清理验证"""
        ws, token = self._ws_register_and_keep_connection()
        try:
            # 发送心跳添加UE（字段名遵循协议文档，serial-no最多15字符）
            ue_list = [{"serial-no": "SN_DEREG_TEST", "brand": "TestBrand", "model": "TestModel"}]
            ws.send_control_json(
                "ReportMsg",
                {"token": token, "state": "Normal", "ue-list": ue_list},
            )
            ws.recv_control_json()
            time.sleep(1)
        finally:
            ws.close()

        ue = self.db.get_ue_by_msisdn("SN_DEREG_TEST")
        self.assertIsNotNone(ue, "UE应该已添加")

        ws2, token2 = self._ws_register_and_keep_connection()
        try:
            ws2.send_control_json(
                "DeRegisterRequest",
                {"hostname": AGENT_NAME},
                token=int(token2) if str(token2).isdigit() else None,
            )
            time.sleep(0.5)
        finally:
            try:
                ws2.close()
            except Exception:
                pass

        time.sleep(WS_WAIT_OFFLINE_SEC)

        ex = self.db.get_executor_by_name(AGENT_NAME)
        self.assertIsNotNone(ex)
        self.assertEqual(ex.get("status"), 0)
