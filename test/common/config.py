import os
from datetime import datetime
from typing import Any, Dict, Optional


def get_log_dir() -> str:
    base_dir = os.path.dirname(os.path.dirname(__file__))
    log_dir = os.path.join(base_dir, 'logs')
    os.makedirs(log_dir, exist_ok=True)
    return log_dir


def build_logging_config(prefix: str, debug: bool = False) -> Dict[str, Any]:
    log_dir = get_log_dir()
    log_file = os.path.join(log_dir, f'{prefix}_{datetime.now().strftime("%Y%m%d_%H%M%S")}.log')
    return {
        'level': 'DEBUG' if debug else 'INFO',
        'format': '%(asctime)s - %(name)s - %(levelname)s - %(message)s',
        'file': log_file,
    }


def load_database_config() -> Dict[str, Any]:
    return {
        'host': os.getenv('DB_HOST', 'localhost'),
        'port': int(os.getenv('DB_PORT', '5432')),
        'database': os.getenv('DB_NAME', 'dialingtest'),
        'user': os.getenv('DB_USER', 'postgres'),
        'password': os.getenv('DB_PASSWORD', 'postgres'),
    }


def _get_bool_env(name: str, default: bool) -> bool:
    raw = os.getenv(name)
    if raw is None:
        return default
    return raw.lower() in ('1', 'true', 'yes', 'y', 'on')


def load_request_config(env_prefix: Optional[str] = None,
                        default_timeout: int = 30,
                        default_verify_ssl: bool = False) -> Dict[str, Any]:
    timeout_key = f'{env_prefix}_API_TIMEOUT' if env_prefix else 'API_TIMEOUT'
    verify_key = f'{env_prefix}_API_VERIFY_SSL' if env_prefix else 'API_VERIFY_SSL'
    timeout = int(os.getenv(timeout_key, str(default_timeout)))
    verify_ssl = _get_bool_env(verify_key, default_verify_ssl)
    
    # 获取测试用户名和CSRF令牌（用于测试环境）
    test_username = os.getenv('TEST_USERNAME', 'test_admin')
    test_csrf_token = os.getenv('TEST_CSRF_TOKEN', 'test-csrf-token-12345')
    
    return {
        'timeout': timeout,
        'verify_ssl': verify_ssl,
        'headers': {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
            'X-Username': test_username,
            'X-Csrf-Token': test_csrf_token,
        }
    }


def load_base_url(preferred_env_vars: Optional[list] = None,
                  default_base: str = 'https://localhost:8087/dialingtestapp') -> str:
    """加载统一 BASE_URL。

    优先读取 preferred_env_vars 中的环境变量，其次读取 API_BASE_URL；均未配置时使用默认值。
    约定：环境变量若提供，应为完整 base URL（包括上下文路径）。
    """
    if preferred_env_vars is None:
        preferred_env_vars = []

    for env_name in preferred_env_vars + ['API_BASE_URL']:
        val = os.getenv(env_name)
        if val:
            return val.rstrip('/')
    return default_base.rstrip('/')


