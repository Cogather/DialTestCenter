import requests
import logging

logger = logging.getLogger(__name__)

class ApiClient:
    def __init__(self, base_url):
        self.base_url = base_url.rstrip('/')

    def submit_preprocess_task(self, ue_id, start_time, end_time):
        """
        提交预处理任务
        :param ue_id: 设备ID
        :param start_time: 开始时间戳 (ms)
        :param end_time: 结束时间戳 (ms)
        :return: task_id (str)
        """
        url = f"{self.base_url}/tasks/preprocess"
        payload = {
            "dialing_task_id": "test-dial-999", # Dummy ID
            "ue_id": ue_id,
            "time_range": {
                "start": start_time,
                "end": end_time
            }
        }
        logger.info(f"Submitting preprocess task: {payload}")
        resp = requests.post(url, json=payload)
        resp.raise_for_status()
        data = resp.json()
        # Assuming response format: {"code": 200, "data": {"job_id": "..."}}
        if data.get('code') != 200:
             raise Exception(f"API Error: {data}")
        
        return data.get('data', {}).get('job_id')
