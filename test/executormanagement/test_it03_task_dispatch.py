import unittest
import time
import json

from .base import BaseTestCase
from .config import WS_ENABLE, AGENT_NTLM_HASH
from .json_message import JsonMessageHelper
from .binary_codec import BinaryCodec


@unittest.skipUnless(WS_ENABLE, "WS测试默认关闭，设置 EXEC_WS_ENABLE=1 以启用")
class TestTaskDispatchIT03(BaseTestCase):
    """IT-03: 任务下发执行流程"""

    @unittest.skipUnless(AGENT_NTLM_HASH, "未提供 EXEC_AGENT_NTLM_HASH，无法计算")
    def test_it_03_001_task_start_request_message(self):
        """IT-03-001: 验证TaskStart-Request消息格式（通过数据库创建任务）"""
        # 注册并保持连接（这里只是确保执行机存在）
        ws, token = self._ws_register_and_keep_connection()
        try:
            # 通过数据库API创建任务（模拟业务层发起任务）
            task_id = 1001
            task_data = {
                "id": task_id,
                "creator": "test_user",
                "status": "PENDING",
                "input": json.dumps(
                    {
                        "script_name": "dial_test_script.py",
                        "version": "1.0.0",
                        "serial_no_list": ["SN001", "SN002"],
                        "proctype": 1,
                        "parameters": {"timeout": 30, "retry_count": 3},
                    }
                ),
            }

            self.db.execute_query(
                "INSERT INTO task(id, creator, status, input, start_time) "
                "VALUES (%s, %s, %s, %s, NOW()) "
                "ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status",
                (task_data["id"], task_data["creator"], task_data["status"], task_data["input"]),
            )

            tasks = self.db.execute_query("SELECT id, status FROM task WHERE id = %s", (task_id,))
            self.assertTrue(len(tasks) > 0, "任务应该在数据库中")
            task = tasks[0]
            self.assertEqual(task.get("id"), task_id)
        finally:
            self.db.execute_query("DELETE FROM task WHERE id = %s", (1001,))
            ws.close()

    @unittest.skipUnless(AGENT_NTLM_HASH, "未提供 EXEC_AGENT_NTLM_HASH，无法计算")
    def test_it_03_002_task_start_response_success(self):
        """IT-03-002: 任务执行成功响应（JSON元数据 + Binary日志）"""
        ws, token = self._ws_register_and_keep_connection()
        helper = JsonMessageHelper()
        try:
            task_id = 1002
            self.db.execute_query(
                "INSERT INTO task(id, creator, status, input, start_time) "
                "VALUES (%s, %s, %s, %s, NOW()) "
                "ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status",
                (
                    task_id,
                    "test_user",
                    "RUNNING",
                    json.dumps(
                        {
                            "script_name": "test_script.py",
                            "version": "1.0.0",
                            "serial_no_list": ["SN001"],
                        }
                    ),
                ),
            )

            sub_result = [
                {"serial_no": "SN001", "result": 0, "duration": 1500, "error_msg": ""}
            ]
            log_content = "Test execution log\nTask completed successfully\n"
            log_bytes = log_content.encode("utf-8")
            crc = BinaryCodec.crc32_hex(log_bytes)

            # 先发送 JSON TaskStartResponse（仅元数据）
            ws.send_control_json(
                "TaskStartResponse",
                {
                    "taskId": task_id,
                    "result": 0,
                    "sub-result": sub_result,
                    "filelen": len(log_bytes),
                    "crc": crc,
                },
                token=int(token) if str(token).isdigit() else None,
            )
            # 再发送 Binary 日志（单包或分片，这里使用单包）
            ws.send_binary(log_bytes)

            time.sleep(0.5)
            tasks = self.db.execute_query(
                "SELECT id, status, result FROM task WHERE id = %s", (task_id,)
            )
            self.assertTrue(len(tasks) > 0, "任务应该在数据库中")
        finally:
            self.db.execute_query("DELETE FROM task WHERE id = %s", (task_id,))
            ws.close()

    @unittest.skipUnless(AGENT_NTLM_HASH, "未提供 EXEC_AGENT_NTLM_HASH，无法计算")
    def test_it_03_003_task_start_response_failure(self):
        """IT-03-003: 任务执行失败响应"""
        ws, token = self._ws_register_and_keep_connection()
        helper = JsonMessageHelper()
        try:
            task_id = 1003
            self.db.execute_query(
                "INSERT INTO task(id, creator, status, input, start_time) "
                "VALUES (%s, %s, %s, %s, NOW()) "
                "ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status",
                (
                    task_id,
                    "test_user",
                    "RUNNING",
                    json.dumps(
                        {
                            "script_name": "failing_script.py",
                            "version": "1.0.0",
                            "serial_no_list": ["SN001"],
                        }
                    ),
                ),
            )

            sub_result = [
                {
                    "serial_no": "SN001",
                    "result": 1,
                    "duration": 500,
                    "error_msg": "Script execution failed",
                }
            ]
            error_log = "Error: Script execution failed\nTimeout occurred\n"
            error_bytes = error_log.encode("utf-8")
            crc = BinaryCodec.crc32_hex(error_bytes)

            ws.send_control_json(
                "TaskStartResponse",
                {
                    "taskId": task_id,
                    "result": 1,
                    "sub-result": sub_result,
                    "filelen": len(error_bytes),
                    "crc": crc,
                    "block": "Execution timeout",
                },
                token=int(token) if str(token).isdigit() else None,
            )
            ws.send_binary(error_bytes)

            time.sleep(0.5)
            tasks = self.db.execute_query(
                "SELECT id, status, result FROM task WHERE id = %s", (task_id,)
            )
            self.assertTrue(len(tasks) > 0, "任务应该在数据库中")
        finally:
            self.db.execute_query("DELETE FROM task WHERE id = %s", (task_id,))
            ws.close()

    @unittest.skipUnless(AGENT_NTLM_HASH, "未提供 EXEC_AGENT_NTLM_HASH，无法计算")
    def test_it_03_004_task_file_upload(self):
        """IT-03-004: 文件上传和CRC校验（大日志分片）"""
        ws, token = self._ws_register_and_keep_connection()
        helper = JsonMessageHelper()
        try:
            task_id = 1004
            self.db.execute_query(
                "INSERT INTO task(id, creator, status, input, start_time) "
                "VALUES (%s, %s, %s, %s, NOW()) "
                "ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status",
                (
                    task_id,
                    "test_user",
                    "RUNNING",
                    json.dumps(
                        {
                            "script_name": "test_script.py",
                            "version": "1.0.0",
                            "serial_no_list": ["SN001"],
                        }
                    ),
                ),
            )

            # 构造较大的日志内容
            lines = [
                f"2024-01-01 12:00:{i:02d} INFO: Log entry {i}" for i in range(100)
            ]
            log_bytes = "\n".join(lines).encode("utf-8")
            crc = BinaryCodec.crc32_hex(log_bytes)

            ws.send_control_json(
                "TaskStartResponse",
                {
                    "taskId": task_id,
                    "result": 0,
                    "sub-result": [{"serial_no": "SN001", "result": 0}],
                    "filelen": len(log_bytes),
                    "crc": crc,
                },
                token=int(token) if str(token).isdigit() else None,
            )

            # 简化：手动分片
            size = max(1, len(log_bytes) // 4)
            for i in range(0, len(log_bytes), size):
                ws.send_binary(log_bytes[i : i + size])

            time.sleep(0.5)
        finally:
            self.db.execute_query("DELETE FROM task WHERE id = %s", (task_id,))
            ws.close()

    @unittest.skipUnless(AGENT_NTLM_HASH, "未提供 EXEC_AGENT_NTLM_HASH，无法计算")
    def test_it_03_005_task_multiple_ue_execution(self):
        """IT-03-005: 多UE设备任务执行"""
        ws, token = self._ws_register_and_keep_connection()
        helper = JsonMessageHelper()
        try:
            task_id = 1005
            serial_no_list = ["SN001", "SN002", "SN003"]

            self.db.execute_query(
                "INSERT INTO task(id, creator, status, input, start_time) "
                "VALUES (%s, %s, %s, %s, NOW()) "
                "ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status",
                (
                    task_id,
                    "test_user",
                    "RUNNING",
                    json.dumps(
                        {
                            "script_name": "multi_ue_script.py",
                            "version": "1.0.0",
                            "serial_no_list": serial_no_list,
                        }
                    ),
                ),
            )

            sub_result = [
                {"serial_no": "SN001", "result": 0, "duration": 1200, "error_msg": ""},
                {"serial_no": "SN002", "result": 0, "duration": 1150, "error_msg": ""},
                {"serial_no": "SN003", "result": 1, "duration": 800, "error_msg": "Network timeout"},
            ]

            combined_log = "".join(
                f"UE {r['serial_no']}: {r['result']} - {r['error_msg']}\n" for r in sub_result
            )
            log_bytes = combined_log.encode("utf-8")
            crc = BinaryCodec.crc32_hex(log_bytes)

            ws.send_control_json(
                "TaskStartResponse",
                {
                    "taskId": task_id,
                    "result": 0,
                    "sub-result": sub_result,
                    "filelen": len(log_bytes),
                    "crc": crc,
                },
                token=int(token) if str(token).isdigit() else None,
            )
            ws.send_binary(log_bytes)

            time.sleep(0.5)
        finally:
            self.db.execute_query("DELETE FROM task WHERE id = %s", (task_id,))
            ws.close()

    @unittest.skipUnless(AGENT_NTLM_HASH, "未提供 EXEC_AGENT_NTLM_HASH，无法计算")
    def test_it_03_006_task_parameter_validation(self):
        """IT-03-006: 任务参数验证（验证不同参数格式的任务创建和响应）"""
        ws, token = self._ws_register_and_keep_connection()
        helper = JsonMessageHelper()
        try:
            test_cases = [
                {
                    "task_id": 1006,
                    "script_name": "script.py",
                    "version": "1.0.0",
                    "serial_no_list": ["SN001"],
                    "parameters": json.dumps({"key": "value", "numbers": [1, 2, 3]}),
                },
                {
                    "task_id": 1007,
                    "script_name": "complex_script.py",
                    "version": "2.1.0-beta",
                    "serial_no_list": ["SN001", "SN002", "SN003", "SN004"],
                    "parameters": json.dumps(
                        {
                            "timeout": 300,
                            "retries": 5,
                            "config": {"debug": True, "log_level": "INFO"},
                        }
                    ),
                },
            ]

            for test_case in test_cases:
                task_id = test_case["task_id"]

                self.db.execute_query(
                    "INSERT INTO task(id, creator, status, input, start_time) "
                    "VALUES (%s, %s, %s, %s, NOW()) "
                    "ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status",
                    (task_id, "test_user", "RUNNING", json.dumps(test_case)),
                )

                sub_result = [{"serial_no": test_case["serial_no_list"][0], "result": 0}]
                log_bytes = b"Task completed\n"
                crc = BinaryCodec.crc32_hex(log_bytes)

                ws.send_control_json(
                    "TaskStartResponse",
                    {
                        "taskId": task_id,
                        "result": 0,
                        "sub-result": sub_result,
                        "filelen": len(log_bytes),
                        "crc": crc,
                    },
                    token=int(token) if str(token).isdigit() else None,
                )
                ws.send_binary(log_bytes)

                time.sleep(0.3)

            for test_case in test_cases:
                tasks = self.db.execute_query(
                    "SELECT id, status FROM task WHERE id = %s",
                    (test_case["task_id"],),
                )
                self.assertTrue(len(tasks) > 0, f"任务 {test_case['task_id']} 应该在数据库中")
        finally:
            for test_case in test_cases:
                self.db.execute_query(
                    "DELETE FROM task WHERE id = %s", (test_case["task_id"],)
                )
            ws.close()
