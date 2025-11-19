import unittest
import logging
import json

from .utils import APIClient, Assertions, DatabaseHelper, compute_chap_response, wait_for_condition
from .config import API_ENDPOINTS, WS_URL, WS_ENABLE, AGENT_NAME, AGENT_USERNAME, AGENT_NTLM_HASH, WS_TIMEOUT
from .json_message import JsonMessageHelper
from .binary_codec import BinaryCodec
from .ws_client import ExecutorWsClient


class BaseTestCase(unittest.TestCase):
    """执行机管理 - 集成测试基类"""

    @staticmethod
    def _token_db_to_hex(db_token):
        """将数据库中的token（有符号long字符串）转换为16进制字符串
        
        数据库中存储的是Java long类型（有符号64位整数）的字符串表示，
        而客户端解码时转换为16进制字符串（8字节）
        """
        if db_token is None:
            return None
        
        # 转换为整数
        if isinstance(db_token, str):
            token_long = int(db_token)
        else:
            token_long = db_token
        
        # 如果是负数，转换为无符号表示（Java有符号long -> Python无符号）
        if token_long < 0:
            token_long = (1 << 64) + token_long
        
        # 转换为16字符的hex字符串（8字节，小写）
        return format(token_long, '016x')

    @classmethod
    def setUpClass(cls) -> None:
        logging.getLogger(__name__).info("== Exec tests start: %s ==", cls.__name__)
        cls.api = APIClient()
        cls.assertions = Assertions()
        cls.db = DatabaseHelper()
        # 确保测试所需表结构存在
        try:
            cls.db.ensure_schema()
            # 准备刷新接口所需的基础数据
            cls.db.ensure_executor_exists("Executor_PC_001")
            # 创建测试用户（用于SHA256 CHAP认证）
            cls.db.ensure_agent_user_exists(AGENT_USERNAME, AGENT_NTLM_HASH)
        except Exception:
            # 不中断收集；具体用例执行时再报错便于定位
            pass

        # 预热一次接口，避免首个请求包含数据源初始化等冷启动开销影响性能断言
        try:
            cls.api.get(API_ENDPOINTS['LIST_EXECUTORS'])
        except Exception:
            # 预热失败不影响后续用例执行，由具体用例来报告错误
            pass

    @classmethod
    def tearDownClass(cls) -> None:
        if getattr(cls, 'db', None):
            try:
                cls.db.close()
            except Exception:
                pass
        logging.getLogger(__name__).info("== Exec tests finished: %s ==", cls.__name__)

    def setUp(self) -> None:
        # 可按需清理或准备数据
        pass

    # ---------- WS helpers（JSON 协议） ----------
    def _open_ws(self):
        client = ExecutorWsClient()
        client.connect()
        return client

    def _ws_register_and_get_token(self) -> str:
        """
        使用 JSON 协议完成四阶段CHAP认证（SHA256算法），返回 token。
        连接在方法结束时关闭。
        """
        if not WS_ENABLE:
            self.skipTest("WS 未启用")
        if not AGENT_NTLM_HASH:
            self.skipTest("未提供 EXEC_AGENT_SHA256_HASH，无法计算CHAP摘要")

        client = self._open_ws()
        helper = JsonMessageHelper()
        try:
            # 1. RegisterRequest
            req_env = helper.build(
                "RegisterRequest",
                {"hostname": AGENT_NAME, "username": AGENT_USERNAME},
            )
            client.send_json(req_env)

            # 2. RegisterChallenge
            res_env = client.recv_json()
            msg_type, _, payload = helper.parse(res_env)
            if msg_type not in ("RegisterChallenge", "register_challenge"):
                self.fail(f"unexpected first response: {res_env}")
            challenge_b64 = payload.get("challenge")
            challenge_id = payload.get("challenge-id", 0)
            self.assertIsNotNone(challenge_b64)
            challenge_bytes = BinaryCodec.decode_base64(challenge_b64)

            # 3. RegisterResponse
            response_hex = BinaryCodec.compute_chap_response(AGENT_NTLM_HASH, challenge_bytes)
            auth_env = helper.build(
                "RegisterResponse",
                {"challenge-id": challenge_id, "username": AGENT_USERNAME, "response": response_hex},
            )
            client.send_json(auth_env)

            # 4. RegisterResult
            res2_env = client.recv_json()
            msg_type2, _, payload2 = helper.parse(res2_env)
            self.assertIn(msg_type2, ("RegisterResult", "register_ack"))
            result = payload2.get("result")
            if result is not None:
                self.assertEqual(result, 0)
            status = payload2.get("status")
            if status is not None:
                self.assertIn(status, ("success", 0))
            token = payload2.get("token")
            self.assertTrue(token)
            return str(token)
        finally:
            client.close()

    def _ws_register_and_keep_connection(self):
        """
        使用 JSON 协议完成CHAP认证（SHA256算法），返回 (ExecutorWsClient, token)；调用者负责关闭连接。
        """
        if not WS_ENABLE:
            self.skipTest("WS 未启用")
        if not AGENT_NTLM_HASH:
            self.skipTest("未提供 EXEC_AGENT_SHA256_HASH，无法计算CHAP摘要")

        client = self._open_ws()
        helper = JsonMessageHelper()

        # 1. RegisterRequest
        req_env = helper.build(
            "RegisterRequest",
            {"hostname": AGENT_NAME, "username": AGENT_USERNAME},
        )
        client.send_json(req_env)

        # 2. RegisterChallenge
        res_env = client.recv_json()
        msg_type, _, payload = helper.parse(res_env)
        if msg_type not in ("RegisterChallenge", "register_challenge"):
            self.fail(f"unexpected first response: {res_env}")
        challenge_b64 = payload.get("challenge")
        challenge_id = payload.get("challenge-id", 0)
        self.assertIsNotNone(challenge_b64)
        challenge_bytes = BinaryCodec.decode_base64(challenge_b64)

        # 3. RegisterResponse
        response_hex = BinaryCodec.compute_chap_response(AGENT_NTLM_HASH, challenge_bytes)
        auth_env = helper.build(
            "RegisterResponse",
            {"challenge-id": challenge_id, "username": AGENT_USERNAME, "response": response_hex},
        )
        client.send_json(auth_env)

        # 4. RegisterResult
        res2_env = client.recv_json()
        msg_type2, _, payload2 = helper.parse(res2_env)
        self.assertIn(msg_type2, ("RegisterResult", "register_ack"))
        result = payload2.get("result")
        if result is not None:
            self.assertEqual(result, 0)
        status = payload2.get("status")
        if status is not None:
            self.assertIn(status, ("success", 0))
        token = payload2.get("token")
        self.assertTrue(token)
        return client, str(token)


