import unittest
import logging
from test.common.config import build_logging_config
from test.finetuning import config
from test.finetuning.utils.kafka_helper import KafkaHelper
from test.finetuning.utils.mock_callback import MockCallbackServer
from test.finetuning.utils.api_client import ApiClient
from test.finetuning.utils.db_helper import DbHelper

# Configure logging
logging.basicConfig(**build_logging_config('test_finetuning', debug=True))
logger = logging.getLogger(__name__)

class BaseTestCase(unittest.TestCase):
    kafka = None
    mock_server = None
    api = None
    db = None

    @classmethod
    def setUpClass(cls):
        logger.info("=== Setting up BaseTestCase ===")
        
        # 1. Init Kafka Helper
        cls.kafka = KafkaHelper(
            config.KAFKA_BOOTSTRAP_SERVERS,
            config.TOPIC_RAW_FLOW,
            config.TOPIC_LABELED_SAMPLE
        )

        # 2. Start Mock Callback Server
        # Note: Ensure the Java service is configured to call this host:port
        cls.mock_server = MockCallbackServer(config.TEST_HOST, config.MOCK_CALLBACK_PORT)
        cls.mock_server.start()

        # 3. Init API Client
        cls.api = ApiClient(config.API_BASE_URL)

        # 4. Init DB Helper
        cls.db = DbHelper(config.DB_CONFIG)
        cls.db.connect()

    @classmethod
    def tearDownClass(cls):
        logger.info("=== Tearing down BaseTestCase ===")
        if cls.kafka:
            cls.kafka.close()
        if cls.mock_server:
            cls.mock_server.stop()
        if cls.db:
            cls.db.close()
