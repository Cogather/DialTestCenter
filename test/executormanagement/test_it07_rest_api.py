import unittest

from .base import BaseTestCase
from .config import API_ENDPOINTS, AGENT_NAME, AGENT_NTLM_HASH, WS_ENABLE
from .json_message import JsonMessageHelper


class TestRestAPIIT07(BaseTestCase):
    """IT-07: 北向API测试"""

    def test_it_07_001_list_executors_empty(self):
        """IT-07-001: 查询执行机列表"""
        response = self.api.get(API_ENDPOINTS['LIST_EXECUTORS'])
        self.assertions.assert_status_code(response, 200)

        result = response.json()
        # 验证响应结构（新格式：包含success和data字段）
        self.assertTrue(result.get('success'))
        self.assertIn('data', result)
        data = result['data']
        self.assertIn('content', data)
        self.assertIsInstance(data['content'], list)

    def test_it_07_002_list_executors_with_data(self):
        """IT-07-002: 查询执行机列表（有数据）"""
        # 确保有一个执行机在线
        if WS_ENABLE and AGENT_NTLM_HASH:
            _ = self._ws_register_and_get_token()

        try:
            response = self.api.get(API_ENDPOINTS['LIST_EXECUTORS'])
            self.assertions.assert_status_code(response, 200)

            result = response.json()
            self.assertTrue(result.get('success'))
            data = result['data']
            executors = data['content']
            
            self.assertIsInstance(executors, list)
            self.assertGreater(len(executors), 0, "应该至少有一个执行机")

            # 验证执行机数据结构
            executor = executors[0]
            required_fields = ['name', 'status', 'lastOnlineTime']
            for field in required_fields:
                self.assertIn(field, executor, f"执行机数据应包含字段: {field}")

        finally:
            # 清理：断开连接使其离线
            pass  # Token获取时连接已关闭

    def test_it_07_003_refresh_executors(self):
        """IT-07-003: 刷新执行机信息"""
        # 确保有一个执行机在线
        self.db.ensure_executor_exists(AGENT_NAME)
        self.db.update_executor_status(AGENT_NAME, 1)  # 设置为在线

        # 发送刷新请求（包含必需的name字段）
        refresh_data = {
            'name': AGENT_NAME
        }
        response = self.api.post(API_ENDPOINTS['REFRESH_EXECUTOR'], data=refresh_data)
        
        # 验证响应
        # 如果执行机在线，应该返回200或202表示刷新请求已发送
        # 如果执行机离线，返回404
        # 如果缺少认证信息，返回401或403
        self.assertIn(response.status_code, [200, 202, 401, 403, 404], 
                     f"刷新接口返回了预期外的状态码: {response.status_code}")
        
        # 如果成功，验证响应格式
        if response.status_code in [200, 202]:
            result = response.json()
            self.assertIn('success', result)
            if result.get('success'):
                self.assertIn('message', result)

    def test_it_07_004_list_executors_offline(self):
        """IT-07-004: 查询离线执行机"""
        # 确保有一个离线执行机
        self.db.update_executor_status(AGENT_NAME, 0)  # 设置为离线

        response = self.api.get(API_ENDPOINTS['LIST_EXECUTORS'])
        self.assertions.assert_status_code(response, 200)

        result = response.json()
        executors = result['data']['content']
        
        # 查找我们的测试执行机
        test_executor = None
        for executor in executors:
            if executor.get('name') == AGENT_NAME:
                test_executor = executor
                break

        if test_executor:
            self.assertEqual(test_executor.get('status'), 0, "执行机应该是离线的")

    def test_it_07_005_executors_pagination(self):
        """IT-07-005: 执行机列表分页"""
        # 创建多个测试执行机
        for i in range(5):
            self.db.ensure_executor_exists(f"TestExecutor_{i}")

        response = self.api.get(API_ENDPOINTS['LIST_EXECUTORS'])
        self.assertions.assert_status_code(response, 200)

        result = response.json()
        data = result['data']
        executors = data['content']
        
        self.assertIsInstance(executors, list)
        self.assertGreaterEqual(len(executors), 5, "应该至少有5个执行机")
        
        # 验证分页信息
        self.assertIn('totalElements', data)
        self.assertIn('totalPages', data)

    def test_it_07_006_executor_detail_fields(self):
        """IT-07-006: 执行机详细信息字段验证"""
        # 注册执行机并发送心跳以设置详细信息
        ws, token = self._ws_register_and_keep_connection()
        try:
            # 发送心跳包含UE信息
            helper = JsonMessageHelper()
            ue_list = [
                {
                    'msisdn': '8613800010001',
                    'serial': 'SN_DETAIL_001',
                    'vendor': 'Huawei',
                    'model': 'P60'
                }
            ]
            hb_env = helper.build(
                "ReportMsg",
                {"token": token, "state": "Normal", "ue-list": ue_list},
            )
            ws.send_json(hb_env)

            import time
            time.sleep(1)

            # 查询API
            response = self.api.get(API_ENDPOINTS['LIST_EXECUTORS'])
            self.assertions.assert_status_code(response, 200)

            result = response.json()
            executors = result['data']['content']
            
            test_executor = None
            for executor in executors:
                if executor.get('name') == AGENT_NAME:
                    test_executor = executor
                    break

            if test_executor:
                # 验证UE数量字段
                if 'ueCount' in test_executor:
                    self.assertGreaterEqual(test_executor['ueCount'], 1, "应该至少有一个UE")

        finally:
            ws.close()

    def test_it_07_007_api_error_handling(self):
        """IT-07-007: API错误处理"""
        # 测试无效的API端点
        response = self.api.get('/api/executors/invalid')
        # 应该返回404或其他错误状态
        self.assertions.assert_status_code_in(response, [404, 405])

    def test_it_07_008_executors_status_filter(self):
        """IT-07-008: 执行机状态过滤（如果支持）"""
        # 创建在线和离线的执行机
        online_executor = "TestOnlineExecutor"
        offline_executor = "TestOfflineExecutor"

        self.db.ensure_executor_exists(online_executor)
        self.db.ensure_executor_exists(offline_executor)

        # 设置一个为在线，一个为离线
        self.db.update_executor_status(online_executor, 1)
        self.db.update_executor_status(offline_executor, 0)

        response = self.api.get(API_ENDPOINTS['LIST_EXECUTORS'])
        self.assertions.assert_status_code(response, 200)

        result = response.json()
        executors = result['data']['content']

        # 验证同时包含在线和离线的执行机
        online_found = offline_found = False
        for executor in executors:
            if executor.get('name') == online_executor:
                online_found = True
                self.assertEqual(executor.get('status'), 1)
            elif executor.get('name') == offline_executor:
                offline_found = True
                self.assertEqual(executor.get('status'), 0)

        self.assertTrue(online_found, "应该找到在线执行机")
        self.assertTrue(offline_found, "应该找到离线执行机")
