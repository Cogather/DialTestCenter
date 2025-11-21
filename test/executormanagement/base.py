import unittest
import logging
import json

from .utils import APIClient, Assertions, DatabaseHelper, compute_chap_response, wait_for_condition
from .config import API_ENDPOINTS, WS_ENABLE, AGENT_NAME, AGENT_USERNAME, AGENT_NTLM_HASH, WS_TIMEOUT
from .json_message import JsonMessageHelper
from .binary_codec import BinaryCodec
from .dual_ws_client import DualWebSocketClient


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

    def assertExecutorStatus(self, name: str, expected_status: int, msg: str = None):
        """
        断言executor状态
        
        Args:
            name: executor名称
            expected_status: 期望的状态 (0=OFFLINE, 1=ONLINE)
            msg: 断言失败时的消息
        """
        executor = self.db.get_executor_by_name(name)
        if executor is None:
            self.fail(f"Executor '{name}' not found in database")
        
        actual_status = executor.get("status")
        if msg is None:
            msg = f"Expected executor '{name}' status to be {expected_status}, but got {actual_status}"
        
        self.assertEqual(actual_status, expected_status, msg)

    def _cleanup_executor(self, name: str = None):
        """
        清理executor记录（设置为离线状态）
        
        Args:
            name: executor名称，默认使用AGENT_NAME
        """
        if name is None:
            name = AGENT_NAME
        
        try:
            self.db.update_executor_status(name, 0)  # 0=OFFLINE
            logging.getLogger(__name__).debug(f"Cleaned up executor: {name}")
        except Exception as e:
            logging.getLogger(__name__).warning(f"Failed to cleanup executor {name}: {e}")

    # ---------- V5双连接WS helpers ----------
    def _open_ws(self):
        """
        V5: 建立双连接并完成认证
        返回DualWebSocketClient实例
        """
        client = DualWebSocketClient()
        client.connect_all()
        client.authenticate_and_bind(
            username=AGENT_USERNAME,
            password_hash=AGENT_NTLM_HASH,
            hostname=AGENT_NAME
        )
        return client

    def _ws_register_and_get_token(self) -> int:
        """
        V5: 完成双连接认证，返回token
        连接在方法结束时关闭
        """
        if not WS_ENABLE:
            self.skipTest("WS 未启用")
        if not AGENT_NTLM_HASH:
            self.skipTest("未提供 EXEC_AGENT_SHA256_HASH，无法计算CHAP摘要")

        client = DualWebSocketClient()
        try:
            client.connect_all()
            token = client.authenticate_and_bind(
                username=AGENT_USERNAME,
                password_hash=AGENT_NTLM_HASH,
                hostname=AGENT_NAME
            )
            return token
        finally:
            client.close_all()

    def _ws_register_and_keep_connection(self):
        """
        V5: 完成双连接认证，返回 (DualWebSocketClient, token)
        调用者负责关闭连接
        """
        if not WS_ENABLE:
            self.skipTest("WS 未启用")
        if not AGENT_NTLM_HASH:
            self.skipTest("未提供 EXEC_AGENT_SHA256_HASH，无法计算CHAP摘要")

        client = DualWebSocketClient()
        client.connect_all()
        token = client.authenticate_and_bind(
            username=AGENT_USERNAME,
            password_hash=AGENT_NTLM_HASH,
            hostname=AGENT_NAME
        )
        return client, token


