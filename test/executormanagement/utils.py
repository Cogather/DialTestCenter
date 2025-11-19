import json
import os
import logging
import hashlib
import base64
import binascii
import time
from typing import Any, Dict, Optional, Tuple
import requests

from .config import BASE_URL, DATABASE_CONFIG, REQUEST_CONFIG, LOGGING_CONFIG
from common.http import APIClient as _CommonAPIClient
from common.db import BaseDatabaseHelper
from common.helpers import (
    measure_response_time_ms as _measure_response_time_ms,
    wait_for_condition as _wait_for_condition,
    json_to_string as _json_to_string,
)

# 日志初始化（与 taskmanagement 对齐）
logging.basicConfig(
    level=LOGGING_CONFIG['level'],
    format=LOGGING_CONFIG['format'],
    handlers=[
        logging.FileHandler(LOGGING_CONFIG['file']),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)


class APIClient(_CommonAPIClient):
    def __init__(self, base_url: str = BASE_URL) -> None:
        super().__init__(base_url=base_url, request_config=REQUEST_CONFIG)


class Assertions:
    """测试断言工具类"""
    
    def assert_status_code(self, response: requests.Response, expected: int) -> None:
        """断言HTTP状态码"""
        assert response.status_code == expected, \
            f"Expected status {expected}, got {response.status_code}: {response.text}"

    def assert_status_code_in(self, response: requests.Response, expected_codes: list) -> None:
        """断言HTTP状态码在指定列表中"""
        assert response.status_code in expected_codes, \
            f"Expected status in {expected_codes}, got {response.status_code}: {response.text}"

    def assert_json_field_not_null(self, response: requests.Response, field: str) -> None:
        """断言JSON响应中指定字段非空"""
        data = response.json()
        assert field in data and data[field] is not None, \
            f"Field '{field}' is null or missing"

    def assert_json_field_equals(self, response: requests.Response, field: str, expected_value: Any) -> None:
        """断言JSON响应中指定字段等于期望值"""
        data = response.json()
        actual_value = data.get(field)
        assert actual_value == expected_value, \
            f"Field '{field}' expected {expected_value}, got {actual_value}"

    def assert_json_has_fields(self, response: requests.Response, fields: list) -> None:
        """断言JSON响应包含所有指定字段"""
        data = response.json()
        missing_fields = [f for f in fields if f not in data]
        assert len(missing_fields) == 0, \
            f"Missing required fields: {missing_fields}"

    def assert_response_time(self, cost_ms: float, threshold_ms: float) -> None:
        """断言响应时间在阈值内"""
        assert cost_ms <= threshold_ms, \
            f"Response time {cost_ms}ms exceeds threshold {threshold_ms}ms"

    def assert_executor_status(self, executor: Optional[Dict[str, Any]], expected_status: int) -> None:
        """断言执行机状态"""
        assert executor is not None, "Executor not found"
        actual_status = executor.get('status')
        assert actual_status == expected_status, \
            f"Expected executor status {expected_status}, got {actual_status}"

    def assert_ue_exists(self, ue: Optional[Dict[str, Any]], msisdn: str) -> None:
        """断言UE存在"""
        assert ue is not None, f"UE with MSISDN {msisdn} not found"

    def assert_ue_field(self, ue: Dict[str, Any], field: str, expected_value: Any) -> None:
        """断言UE字段值"""
        actual_value = ue.get(field)
        assert actual_value == expected_value, \
            f"UE field '{field}' expected {expected_value}, got {actual_value}"

    def assert_message_type(self, message: Dict[str, Any], expected_type: str) -> None:
        """断言WebSocket消息类型"""
        actual_type = message.get('message_type')
        assert actual_type == expected_type, \
            f"Expected message_type '{expected_type}', got '{actual_type}'"

    def assert_websocket_data_has_fields(self, message: Dict[str, Any], fields: list) -> None:
        """断言WebSocket消息数据包含所有指定字段"""
        data = message.get('data', {})
        missing_fields = [f for f in fields if f not in data]
        assert len(missing_fields) == 0, \
            f"Missing required fields in message data: {missing_fields}"


def measure_response_time(func, *args, **kwargs) -> Tuple[Any, float]:
    start = time.time()
    result = func(*args, **kwargs)
    end = time.time()
    return result, (end - start) * 1000.0


class DatabaseHelper(BaseDatabaseHelper):
    def __init__(self) -> None:
        super().__init__(config=DATABASE_CONFIG)

    def get_executor_by_name(self, name: str) -> Optional[Dict[str, Any]]:
        """根据名称查询执行机信息"""
        results = self.execute_query("SELECT * FROM executor WHERE name = %s", (name,))
        return results[0] if results else None

    def get_executor_by_id(self, executor_id: int) -> Optional[Dict[str, Any]]:
        """根据ID查询执行机信息"""
        results = self.execute_query("SELECT * FROM executor WHERE id = %s", (executor_id,))
        return results[0] if results else None

    def get_all_executors(self, limit: int = 100) -> list:
        """查询所有执行机"""
        return self.execute_query("SELECT * FROM executor ORDER BY id LIMIT %s", (limit,))

    def update_executor_status(self, name: str, status: int) -> None:
        """更新执行机状态"""
        self.execute_update(
            "UPDATE executor SET status = %s WHERE name = %s",
            (status, name)
        )

    def get_ue_by_msisdn(self, msisdn: str) -> Optional[Dict[str, Any]]:
        """根据MSISDN查询UE信息"""
        results = self.execute_query("SELECT * FROM ue WHERE msisdn = %s", (msisdn,))
        return results[0] if results else None

    def get_ue_by_serial(self, serial: str) -> Optional[Dict[str, Any]]:
        """根据序列号查询UE信息"""
        results = self.execute_query("SELECT * FROM ue WHERE serial = %s", (serial,))
        return results[0] if results else None

    def get_ues_by_executor(self, executor_name: str) -> list:
        """查询指定执行机的所有UE"""
        if not self.table_has_column('ue', 'executor_name'):
            return []
        return self.execute_query(
            "SELECT * FROM ue WHERE executor_name = %s ORDER BY id",
            (executor_name,)
        )

    def count_ues_by_executor(self, executor_name: str) -> int:
        """统计指定执行机的UE数量"""
        if not self.table_has_column('ue', 'executor_name'):
            return 0
        results = self.execute_query(
            "SELECT COUNT(*) as cnt FROM ue WHERE executor_name = %s",
            (executor_name,)
        )
        return results[0].get('cnt', 0) if results else 0

    def delete_ue_by_msisdn(self, msisdn: str) -> None:
        """删除指定MSISDN的UE"""
        self.execute_update("DELETE FROM ue WHERE msisdn = %s", (msisdn,))

    def delete_ues_by_executor(self, executor_name: str) -> None:
        """删除指定执行机的所有UE"""
        if not self.table_has_column('ue', 'executor_name'):
            return
        self.execute_update("DELETE FROM ue WHERE executor_name = %s", (executor_name,))

    def get_agent_user(self, username: str) -> Optional[Dict[str, Any]]:
        """查询Agent用户信息（现在从dial_users表查询）"""
        results = self.execute_query(
            "SELECT * FROM dial_users WHERE username = %s",
            (username,)
        )
        return results[0] if results else None

    def ensure_schema(self) -> None:
        """确保执行机相关表存在（仅用于测试环境初始化）。"""
        self.execute_update(
            """
            CREATE TABLE IF NOT EXISTS executor (
              id BIGSERIAL PRIMARY KEY,
              name VARCHAR(128) NOT NULL UNIQUE,
              ip VARCHAR(64),
              token VARCHAR(256),
              proxy VARCHAR(256),
              description TEXT,
              status INTEGER,
              last_online_time TIMESTAMP NULL
            )
            """
        )
        self.execute_update(
            """
            CREATE TABLE IF NOT EXISTS ue (
              id BIGSERIAL PRIMARY KEY,
              msisdn VARCHAR(32) UNIQUE,
              serial VARCHAR(64),
              imsi VARCHAR(32),
              imei VARCHAR(32),
              status INTEGER,
              executor_name VARCHAR(128),
              vendor VARCHAR(64),
              model VARCHAR(64),
              os_version VARCHAR(64),
              info JSONB
            )
            """
        )
        # Create dial_users table (unified user management for frontend and executor CHAP auth)
        # password field stores NTLM Hash format (32-char hex string)
        self.execute_update(
            """
            CREATE TABLE IF NOT EXISTS dial_users (
              id BIGSERIAL PRIMARY KEY,
              username VARCHAR(128) NOT NULL UNIQUE,
              password VARCHAR(256) NOT NULL,
              last_login_time TIMESTAMP NULL
            )
            """
        )

    def ensure_executor_exists(self, name: str) -> None:
        """确保指定名称的执行机存在"""
        self.execute_update(
            """
            INSERT INTO executor (name, status)
            VALUES (%s, 0)
            ON CONFLICT (name) DO NOTHING
            """,
            (name,)
        )

    def ensure_agent_user_exists(self, username: str, password_hash: str) -> None:
        """确保指定的Agent用户存在（现在使用dial_users表存储NTLM Hash）"""
        self.execute_update(
            """
            INSERT INTO dial_users (username, password)
            VALUES (%s, %s)
            ON CONFLICT (username) DO NOTHING
            """,
            (username, password_hash)
        )

    def cleanup_test_data(self, prefix: str = "TEST_") -> None:
        """清理测试数据（名称以指定前缀开头的记录）"""
        # 清理测试UE
        self.execute_update(
            "DELETE FROM ue WHERE msisdn LIKE %s OR serial LIKE %s",
            (f"{prefix}%", f"{prefix}%")
        )
        # 清理测试执行机
        self.execute_update(
            "DELETE FROM executor WHERE name LIKE %s",
            (f"{prefix}%",)
        )

    def table_has_column(self, table_name: str, column_name: str) -> bool:
        """检查指定表是否存在某列。兼容 PostgreSQL information_schema。"""
        rows = self.execute_query(
            """
            SELECT 1
            FROM information_schema.columns
            WHERE table_name = %s AND column_name = %s
            LIMIT 1
            """,
            (table_name, column_name),
        )
        return len(rows) > 0

    def has_unique_constraint(self, table_name: str) -> bool:
        """是否存在任意唯一约束（不含主键）。"""
        rows = self.execute_query(
            """
            SELECT 1
            FROM information_schema.table_constraints tc
            WHERE tc.table_name = %s AND tc.constraint_type = 'UNIQUE'
            LIMIT 1
            """,
            (table_name,),
        )
        return len(rows) > 0

    def has_foreign_key(self, table_name: str, column_name: str = None) -> bool:
        """是否存在外键（可选限定到列）。"""
        if column_name is None:
            rows = self.execute_query(
                """
                SELECT 1
                FROM information_schema.table_constraints tc
                WHERE tc.table_name = %s AND tc.constraint_type = 'FOREIGN KEY'
                LIMIT 1
                """,
                (table_name,),
            )
            return len(rows) > 0
        rows = self.execute_query(
            """
            SELECT 1
            FROM information_schema.table_constraints tc
            JOIN information_schema.key_column_usage kcu
              ON tc.constraint_name = kcu.constraint_name
             AND tc.table_name = kcu.table_name
            WHERE tc.table_name = %s
              AND tc.constraint_type = 'FOREIGN KEY'
              AND kcu.column_name = %s
            LIMIT 1
            """,
            (table_name, column_name),
        )
        return len(rows) > 0


def pretty_json(data: Dict[str, Any]) -> str:
    return _json_to_string(data)


def wait_for_condition(condition_func, timeout: int = 5, interval: float = 0.5) -> bool:
    return _wait_for_condition(condition_func, timeout=timeout, interval=interval)


def measure_response_time(func, *args, **kwargs) -> Tuple[Any, float]:
    return _measure_response_time_ms(func, *args, **kwargs)


def _decode_challenge(challenge: str) -> bytes:
    """尝试将 challenge 解码为原始字节：优先按hex，其次按base64。"""
    try:
        return binascii.unhexlify(challenge)
    except Exception:
        pass
    try:
        return base64.b64decode(challenge)
    except Exception:
        pass
    # 最后退回字面UTF-8字节
    return challenge.encode('utf-8')


def compute_chap_response(sha256_hash_hex: str, challenge_hex: str) -> str:
    """
    计算 CHAP 摘要，符合设计文档规范。

    根据《执行机管理软件实现设计》文档（V4更新）：
    Formula: Response = SHA256(SHA256-Hash bytes + Challenge bytes)

    Args:
        sha256_hash_hex: SHA256 Hash（十六进制字符串，64字符表示32字节）
        challenge_hex: Challenge（十六进制字符串，32字符表示16字节）

    Returns:
        SHA256 hex 字符串（小写，64字符）
        
    Note: 此函数已废弃，建议使用 binary_codec.BinaryCodec.compute_chap_response()
    """
    # Decode SHA256 Hash from hex string
    sha256_bytes = binascii.unhexlify(sha256_hash_hex)

    # Decode Challenge from hex string (兼容处理，如果已经是bytes则直接使用)
    if isinstance(challenge_hex, bytes):
        challenge_bytes = challenge_hex
    else:
        challenge_bytes = binascii.unhexlify(challenge_hex)

    # Concatenate and compute SHA256
    combined = sha256_bytes + challenge_bytes
    sha256 = hashlib.sha256()
    sha256.update(combined)
    return sha256.hexdigest()


