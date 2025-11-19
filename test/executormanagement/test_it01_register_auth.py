import unittest
import binascii

from .base import BaseTestCase
from .config import WS_ENABLE, AGENT_SHA256_HASH, AGENT_NAME, AGENT_USERNAME, AGENT_NTLM_HASH
from .utils import wait_for_condition
from .json_message import JsonMessageHelper
from .binary_codec import BinaryCodec


@unittest.skipUnless(WS_ENABLE, "WS测试默认关闭，设置 EXEC_WS_ENABLE=1 以启用")
class TestRegisterAuthIT01(BaseTestCase):
    """IT-01: Agent注册认证流程 - 四阶段CHAP认证（SHA256算法）"""

    @unittest.skipUnless(AGENT_SHA256_HASH, "未提供 EXEC_AGENT_SHA256_HASH，无法计算CHAP摘要")
    def test_it_01_001_register_success(self):
        """IT-01-001: 成功注册并获取token"""
        ws, token = self._ws_register_and_keep_connection()
        try:
            self.assertTrue(token, "Token 应该非空")

            # 验证数据库状态：executor.status=1（ONLINE），token已更新
            def _check_online():
                ex = self.db.get_executor_by_name(AGENT_NAME)
                if not ex:
                    return False
                return (
                    ex.get("status") == 1
                    and ex.get("token") is not None
                    and ex.get("last_online_time") is not None
                )

            ok = wait_for_condition(_check_online, timeout=5, interval=0.5)
            self.assertTrue(ok, "executor 应该更新为 ONLINE 状态，并设置 token 和 last_online_time")

            # 验证token格式：允许整数或十六进制字符串
            try:
                int_token = int(token)
                self.assertIsInstance(int_token, int)
            except ValueError:
                self.assertEqual(len(token), 16, "Token 应该是16字符的十六进制字符串（8字节）")
                try:
                    binascii.unhexlify(token)
                except Exception:
                    self.fail("Token 应该是有效的十六进制字符串")
        finally:
            ws.close()

    def test_it_01_002_register_user_not_found(self):
        """IT-01-002: 用户名不存在"""
        client = self._open_ws()
        helper = JsonMessageHelper()
        try:
            # 发送 RegisterRequest（不存在的用户名）
            env = helper.build(
                "RegisterRequest",
                {"hostname": AGENT_NAME, "username": "__not_exists__"},
            )
            client.send_json(env)

            # 收到 RegisterChallenge
            res_env = client.recv_json()
            msg_type, _, payload = helper.parse(res_env)
            self.assertIn(
                msg_type, ("RegisterChallenge", "register_challenge"),
                "即使用户名不存在，也应返回 RegisterChallenge 消息",
            )
            challenge_b64 = payload.get("challenge")
            challenge_id = payload.get("challenge-id", 0)
            self.assertIsNotNone(challenge_b64, "Challenge 不应为空")

            # 发送错误 response（SHA256为64位十六进制）
            fake_response = "0" * 64
            resp_env = helper.build(
                "RegisterResponse",
                {"challenge-id": challenge_id, "username": "__not_exists__", "response": fake_response},
            )
            client.send_json(resp_env)

            # 收到 RegisterResult，结果为失败
            res2_env = client.recv_json()
            msg_type2, _, payload2 = helper.parse(res2_env)
            self.assertIn(msg_type2, ("RegisterResult", "register_result", "register_ack"))
            result = payload2.get("result")
            status = payload2.get("status")
            if result is not None:
                self.assertNotEqual(result, 0, "认证应该失败")
            if status is not None:
                self.assertNotIn(status, ("success", 0), "认证应该失败")
            self.assertNotIn("token", payload2, "失败时不应有 token 字段")

            # 验证数据库未更新为 ONLINE
            ex = self.db.get_executor_by_name(AGENT_NAME)
            if ex:
                self.assertNotEqual(ex.get("status"), 1, "认证失败时不应更新为 ONLINE 状态")
        finally:
            client.close()

    @unittest.skipUnless(AGENT_SHA256_HASH, "未提供 EXEC_AGENT_SHA256_HASH，无法计算")
    def test_it_01_003_register_auth_response_mismatch(self):
        """IT-01-003: 响应摘要不匹配（SHA256算法）"""
        client = self._open_ws()
        helper = JsonMessageHelper()
        try:
            # 发送 RegisterRequest
            env = helper.build(
                "RegisterRequest",
                {"hostname": AGENT_NAME, "username": AGENT_USERNAME},
            )
            client.send_json(env)

            # 收到 RegisterChallenge
            res_env = client.recv_json()
            msg_type, _, payload = helper.parse(res_env)
            self.assertIn(msg_type, ("RegisterChallenge", "register_challenge"))
            challenge_b64 = payload.get("challenge")
            challenge_id = payload.get("challenge-id", 0)
            self.assertIsNotNone(challenge_b64)

            # 发送错误的 response（不使用正确 CHAP 计算，SHA256为64位十六进制）
            wrong_response = "0" * 64
            resp_env = helper.build(
                "RegisterResponse",
                {"challenge-id": challenge_id, "username": AGENT_USERNAME, "response": wrong_response},
            )
            client.send_json(resp_env)

            res2_env = client.recv_json()
            msg_type2, _, payload2 = helper.parse(res2_env)
            self.assertIn(msg_type2, ("RegisterResult", "register_result", "register_ack"))
            result = payload2.get("result")
            status = payload2.get("status")
            if result is not None:
                self.assertNotEqual(result, 0, "认证应该失败")
            if status is not None:
                self.assertNotIn(status, ("success", 0), "认证应该失败")
            self.assertNotIn("token", payload2, "失败时不应有 token 字段")

            # 验证数据库未更新为 ONLINE
            ex = self.db.get_executor_by_name(AGENT_NAME)
            if ex:
                self.assertNotEqual(ex.get("status"), 1, "认证失败时不应更新为 ONLINE 状态")
        finally:
            client.close()

    @unittest.skipUnless(AGENT_SHA256_HASH, "未提供 EXEC_AGENT_SHA256_HASH，无法计算")
    def test_it_01_004_register_challenge_format(self):
        """IT-01-004: 验证Challenge格式（16字节随机数）"""
        client = self._open_ws()
        helper = JsonMessageHelper()
        try:
            env = helper.build(
                "RegisterRequest",
                {"hostname": AGENT_NAME, "username": AGENT_USERNAME},
            )
            client.send_json(env)

            res_env = client.recv_json()
            msg_type, _, payload = helper.parse(res_env)
            self.assertIn(msg_type, ("RegisterChallenge", "register_challenge"))

            challenge_b64 = payload.get("challenge")
            self.assertIsNotNone(challenge_b64)

            challenge_bytes = BinaryCodec.decode_base64(challenge_b64)
            self.assertEqual(len(challenge_bytes), 16, "Challenge 应该是16字节的随机数")
            challenge_hex = BinaryCodec.encode_hex(challenge_bytes)
            self.assertEqual(len(challenge_hex), 32, "Challenge 转为十六进制应为32字符")
        finally:
            client.close()

    @unittest.skipUnless(AGENT_SHA256_HASH, "未提供 EXEC_AGENT_SHA256_HASH，无法计算")
    def test_it_01_005_register_sha256_calculation(self):
        """IT-01-005: 验证SHA256摘要算法正确性"""
        client = self._open_ws()
        helper = JsonMessageHelper()
        try:
            # 发送 RegisterRequest
            env = helper.build(
                "RegisterRequest",
                {"hostname": AGENT_NAME, "username": AGENT_USERNAME},
            )
            client.send_json(env)

            # 收到 RegisterChallenge
            res_env = client.recv_json()
            msg_type, _, payload = helper.parse(res_env)
            self.assertIn(msg_type, ("RegisterChallenge", "register_challenge"))
            challenge_b64 = payload.get("challenge")
            challenge_id = payload.get("challenge-id", 0)
            self.assertIsNotNone(challenge_b64)

            challenge_bytes = BinaryCodec.decode_base64(challenge_b64)

            # 使用 BinaryCodec 计算正确的 CHAP 摘要（SHA256算法）
            correct_response = BinaryCodec.compute_chap_response(AGENT_SHA256_HASH, challenge_bytes)
            # 验证response为64位十六进制字符串
            self.assertEqual(len(correct_response), 64, "SHA256摘要应该是64位十六进制字符串")

            resp_env = helper.build(
                "RegisterResponse",
                {"challenge-id": challenge_id, "username": AGENT_USERNAME, "response": correct_response},
            )
            client.send_json(resp_env)

            res2_env = client.recv_json()
            msg_type2, _, payload2 = helper.parse(res2_env)
            self.assertIn(msg_type2, ("RegisterResult", "register_result", "register_ack"))
            result = payload2.get("result")
            status = payload2.get("status")
            if result is not None:
                self.assertEqual(result, 0, f"使用正确SHA256摘要应该认证成功，错误: {payload2}")
            if status is not None:
                self.assertIn(status, ("success", 0))
            token = payload2.get("token")
            self.assertTrue(token, "认证成功时应该返回token")
        finally:
            client.close()
