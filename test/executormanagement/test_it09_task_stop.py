import unittest
import time

from .base import BaseTestCase
from .config import WS_ENABLE, AGENT_NTLM_HASH
from .json_message import JsonMessageHelper


@unittest.skipUnless(WS_ENABLE, "WS测试默认关闭，设置 EXEC_WS_ENABLE=1 以启用")
class TestTaskStopIT09(BaseTestCase):
    """IT-09: 任务停止流程"""

    @unittest.skipUnless(AGENT_NTLM_HASH, "未提供 EXEC_AGENT_NTLM_HASH，无法计算")
    def test_it_09_001_task_stop_request(self):
        """IT-09-001: 任务停止响应（Agent发送TaskStop-Response）"""
        client, token = self._ws_register_and_keep_connection()
        helper = JsonMessageHelper()
        try:
            task_id = 100001
            stop_env = helper.build(
                "TaskStopResponse",
                {"taskId": task_id, "result": 0},
                token=int(token) if str(token).isdigit() else None,
            )
            client.send_json(stop_env)
            time.sleep(0.2)
        finally:
            client.close()

    @unittest.skipUnless(AGENT_NTLM_HASH, "未提供 EXEC_AGENT_NTLM_HASH，无法计算")
    def test_it_09_002_task_stop_nonexistent(self):
        """IT-09-002: 停止不存在的任务"""
        client, token = self._ws_register_and_keep_connection()
        helper = JsonMessageHelper()
        try:
            stop_env = helper.build(
                "TaskStopResponse",
                {"taskId": 999999, "result": 1},
                token=int(token) if str(token).isdigit() else None,
            )
            client.send_json(stop_env)
            time.sleep(0.2)
        finally:
            client.close()

    @unittest.skipUnless(AGENT_NTLM_HASH, "未提供 EXEC_AGENT_NTLM_HASH，无法计算")
    def test_it_09_003_task_stop_after_completion(self):
        """IT-09-003: 任务完成后停止"""
        client, token = self._ws_register_and_keep_connection()
        helper = JsonMessageHelper()
        try:
            task_id = 100002
            stop_env = helper.build(
                "TaskStopResponse",
                {"taskId": task_id, "result": 1},
                token=int(token) if str(token).isdigit() else None,
            )
            client.send_json(stop_env)
            time.sleep(0.2)
        finally:
            client.close()

    @unittest.skipUnless(AGENT_NTLM_HASH, "未提供 EXEC_AGENT_NTLM_HASH，无法计算")
    def test_it_09_004_multiple_task_stop(self):
        """IT-09-004: 多次停止同一任务"""
        client, token = self._ws_register_and_keep_connection()
        helper = JsonMessageHelper()
        try:
            task_id = 100003
            for _ in range(3):
                stop_env = helper.build(
                    "TaskStopResponse",
                    {"taskId": task_id, "result": 0},
                    token=int(token) if str(token).isdigit() else None,
                )
                client.send_json(stop_env)
                time.sleep(0.1)
        finally:
            client.close()

    @unittest.skipUnless(AGENT_NTLM_HASH, "未提供 EXEC_AGENT_NTLM_HASH，无法计算")
    def test_it_09_005_task_stop_during_execution(self):
        """IT-09-005: 执行期间停止任务"""
        client, token = self._ws_register_and_keep_connection()
        helper = JsonMessageHelper()
        try:
            task_id = 100004
            stop_env = helper.build(
                "TaskStopResponse",
                {"taskId": task_id, "result": 0},
                token=int(token) if str(token).isdigit() else None,
            )
            client.send_json(stop_env)
            time.sleep(0.2)
        finally:
            client.close()

    @unittest.skipUnless(AGENT_NTLM_HASH, "未提供 EXEC_AGENT_NTLM_HASH，无法计算")
    def test_it_09_006_task_stop_multiple_tasks(self):
        """IT-09-006: 停止多个任务"""
        client, token = self._ws_register_and_keep_connection()
        helper = JsonMessageHelper()
        try:
            task_ids = [100005, 100006, 100007]
            for task_id in task_ids:
                stop_env = helper.build(
                    "TaskStopResponse",
                    {"taskId": task_id, "result": 0},
                    token=int(token) if str(token).isdigit() else None,
                )
                client.send_json(stop_env)
                time.sleep(0.1)
        finally:
            client.close()

    @unittest.skipUnless(AGENT_NTLM_HASH, "未提供 EXEC_AGENT_NTLM_HASH，无法计算")
    def test_it_09_007_task_stop_with_deregister(self):
        """IT-09-007: 注销时停止所有任务"""
        client, token = self._ws_register_and_keep_connection()
        helper = JsonMessageHelper()
        try:
            dereg_env = helper.build(
                "DeRegisterRequest",
                {"hostname": "TestExecutor"},
                token=int(token) if str(token).isdigit() else None,
            )
            client.send_json(dereg_env)
            time.sleep(0.2)
        finally:
            client.close()

    @unittest.skipUnless(AGENT_NTLM_HASH, "未提供 EXEC_AGENT_NTLM_HASH，无法计算")
    def test_it_09_008_task_stop_error_handling(self):
        """IT-09-008: 任务停止错误处理"""
        client, token = self._ws_register_and_keep_connection()
        helper = JsonMessageHelper()
        try:
            invalid_task_ids = [-1, 0, None]
            for task_id in invalid_task_ids:
                if task_id is not None:
                    try:
                        stop_env = helper.build(
                            "TaskStopResponse",
                            {"taskId": task_id, "result": 1},
                            token=int(token) if str(token).isdigit() else None,
                        )
                        client.send_json(stop_env)
                    except Exception:
                        pass
        finally:
            client.close()
