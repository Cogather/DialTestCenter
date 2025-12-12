import time
import uuid
import logging
from test.finetuning.base import BaseTestCase
from test.finetuning import config

logger = logging.getLogger(__name__)

class TestPreprocessE2E(BaseTestCase):
    
    def test_e2e_preprocess_flow(self):
        """FT-01: 预处理任务 E2E 测试"""
        logger.info(">>> Starting FT-01: Preprocess E2E Test")
        
        # 1. Prepare Data
        ue_id = f"auto-test-{uuid.uuid4().hex[:8]}"
        now_ms = int(time.time() * 1000)
        # 时间窗口：前后2分钟
        start_time = now_ms - 120000
        end_time = now_ms + 120000
        
        # 发送符合条件的数据 (时间在窗口内, packet_count=100 > 假设默认阈值)
        self.kafka.send_raw_flow(
            ue_id=ue_id, 
            timestamp_ms=now_ms, 
            packet_count=100,
            src_ip="10.0.0.1",
            dst_ip="8.8.8.8"
        )
        
        # 发送不符合条件的数据 (可选，例如时间在窗口外)
        self.kafka.send_raw_flow(
            ue_id=ue_id,
            timestamp_ms=now_ms - 300000, # 5分钟前，超出窗口
            packet_count=100
        )
        
        # 2. Submit Task
        task_id = self.api.submit_preprocess_task(
            ue_id=ue_id,
            start_time=start_time,
            end_time=end_time
        )
        logger.info(f"Task submitted, ID: {task_id}")
        self.assertIsNotNone(task_id, "Task ID should not be None")

        # 3. Verification 1: Scheduling (PENDING -> RUNNING)
        # 等待任务被抢占
        logger.info("Waiting for task to be picked up...")
        running_task = self.db.wait_for_task_status(task_id, ['RUNNING', 'SUCCESS'], timeout=10)
        self.assertIsNotNone(running_task, "Task should transition to RUNNING or SUCCESS")
        logger.info(f"Task status: {running_task['status']}")

        # 4. Verification 2: Kafka Output
        # 验证是否输出了清洗后的数据
        result = self.kafka.wait_for_json_result(ue_id, timeout=30)
        self.assertIsNotNone(result, "Should receive cleaned data from Kafka")
        self.assertEqual(result.get('packet_count'), 100, "Packet count should match")
        self.assertEqual(result.get('ue_id'), ue_id, "UE ID should match")
        
        # 5. Verification 3: Callback & Final Status
        # 验证回调
        logger.info("Waiting for callback...")
        callback = self.mock_server.wait_for_callback(task_id, timeout=30)
        self.assertIsNotNone(callback, "Should receive callback from service")
        self.assertEqual(callback.get('status'), 'SUCCESS', "Callback status should be SUCCESS")
        
        # 验证 DB 最终状态
        final_task = self.db.get_task_status(task_id)
        self.assertEqual(final_task['status'], 'SUCCESS', "DB Task status should be SUCCESS")
        
        logger.info("<<< FT-01 Passed")
