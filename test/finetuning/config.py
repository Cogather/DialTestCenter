import os
from test.common.config import load_database_config, load_base_url

# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS = os.getenv('KAFKA_BOOTSTRAP_SERVERS', 'localhost:9092')
TOPIC_RAW_FLOW = os.getenv('TOPIC_RAW_FLOW', 'topic-raw-flow')
TOPIC_LABELED_SAMPLE = os.getenv('TOPIC_LABELED_SAMPLE', 'topic-labeled-sample')

# Database Configuration - reuse common logic if possible or use env vars
DB_CONFIG = load_database_config()
# Override DB_NAME if finetuning uses a different DB, but usually it's the same or specified by env
# If finetuning has its own DB, we might need to check that. 
# Assuming it shares the DB or uses env vars DB_NAME.
# In service-design.md it says "PostgreSQL (任务队列/业务数据)".
# FinetuningApp Service likely has its own DB or Schema.
# Let's assume it respects DB_NAME env var.

# API Configuration
# load_base_url handles API_BASE_URL
API_BASE_URL = load_base_url(['FINETUNING_API_URL'], 'http://localhost:8080/api/finetuning/v1')

# Test Host Configuration (for callbacks)
TEST_HOST = os.getenv('TEST_HOST', 'localhost')
MOCK_CALLBACK_PORT = int(os.getenv('MOCK_CALLBACK_PORT', '9999'))
