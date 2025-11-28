import os
import sys
from datetime import datetime
from pathlib import Path

# 确保 test 目录在 Python 路径中
test_dir = Path(__file__).parent.parent
if str(test_dir) not in sys.path:
    sys.path.insert(0, str(test_dir))

from common.config import (
    get_log_dir,
    build_logging_config,
    load_database_config,
    load_request_config,
    load_base_url,
    get_ws_base_url,
)

# API 配置（统一从 common 读取，兼容 EXEC_API_BASE_URL 与 API_BASE_URL）
# 默认值在 common.config.load_base_url 中已统一为 8086
BASE_URL = load_base_url(preferred_env_vars=['EXEC_API_BASE_URL'])

API_ENDPOINTS = {
    'LIST_EXECUTORS': '/api/executors',
    'REFRESH_EXECUTOR': '/api/executors/refresh',
}

# WebSocket 配置
# 自动基于 BASE_URL 推导 WS_BASE_URL，避免硬编码端口
WS_BASE_URL = get_ws_base_url(BASE_URL)

# V4单连接URL（向后兼容，已废弃）
WS_URL = os.getenv('EXEC_WS_URL', f'{WS_BASE_URL}/ws/executor')

# V5双连接URL（推荐使用）
WS_CONTROL_URL = os.getenv('EXEC_WS_CONTROL_URL', f'{WS_BASE_URL}/ws/executor/control')
WS_DATA_URL = os.getenv('EXEC_WS_DATA_URL', f'{WS_BASE_URL}/ws/executor/data')

WS_ENABLE = os.getenv('EXEC_WS_ENABLE', '1') == '1'  # 改为 '1' 默认启用

# Agent 凭据（用于 SC-01/02/07 的真实用例）
AGENT_NAME = os.getenv('EXEC_AGENT_NAME', 'Executor_PC_001')
AGENT_USERNAME = os.getenv('EXEC_AGENT_USERNAME', 'test_agent')
# Agent SHA256 Hash（数据库中 dial_users.password 字段的值，64位十六进制字符串）
# 默认值对应数据库中的测试用户：username=test_agent, password(SHA256 Hash)=ECD71870D1963316A97E3AC3408C9835AD8CF0F3C1BC703527C30265534F75AE
# 对应的明文密码是：test123
AGENT_SHA256_HASH = os.getenv('EXEC_AGENT_SHA256_HASH', 'ECD71870D1963316A97E3AC3408C9835AD8CF0F3C1BC703527C30265534F75AE')
# 保持旧变量名用于向后兼容（将逐步废弃）
AGENT_NTLM_HASH = AGENT_SHA256_HASH

# WS 超时与等待配置（秒）
WS_TIMEOUT = int(os.getenv('EXEC_WS_TIMEOUT', '10'))
WS_WAIT_OFFLINE_SEC = int(os.getenv('EXEC_WS_WAIT_OFFLINE_SEC', '5'))

# 数据库配置
DATABASE_CONFIG = load_database_config()

# 请求/日志配置（与 taskmanagement 对齐）
REQUEST_CONFIG = load_request_config(env_prefix='EXEC')

LOG_DIR = get_log_dir()
LOGGING_CONFIG = build_logging_config(prefix='exec_test', debug=False)

# 性能基准（毫秒）
PERFORMANCE_BASELINE = {
    'list_executors': 200,
    'refresh_executor': 300,
    'register_auth_handshake': 300,
    'heartbeat': 100,
}
