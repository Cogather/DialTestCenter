import logging
import time
from test.common.db import BaseDatabaseHelper

logger = logging.getLogger(__name__)

class DbHelper(BaseDatabaseHelper):
    def get_task_status(self, task_id):
        """查询任务状态"""
        # task_id might be string "job-pre-123", table id usually bigint. 
        # We need to know if task_id returned by API is the DB ID or a string code.
        # Assuming for now it's the DB ID.
        query = "SELECT status, worker_instance, error_msg FROM t_finetuning_task WHERE id = %s"
        # Handle potential string/int mismatch if needed
        rows = self.execute_query(query, (task_id,))
        if rows:
            return rows[0]
        return None

    def wait_for_task_status(self, task_id, target_statuses, timeout=30):
        """等待任务进入指定状态之一"""
        start = time.time()
        while time.time() - start < timeout:
            task = self.get_task_status(task_id)
            if task and task['status'] in target_statuses:
                return task
            time.sleep(1)
        return None
