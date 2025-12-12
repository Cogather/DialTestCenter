import json
import time
import logging
from confluent_kafka import Producer, Consumer, KafkaError
from test.finetuning.generated import raw_flow_pb2

logger = logging.getLogger(__name__)

class KafkaHelper:
    def __init__(self, bootstrap_servers, raw_topic, labeled_topic):
        self.raw_topic = raw_topic
        self.labeled_topic = labeled_topic
        
        # Producer Config
        self.producer = Producer({
            'bootstrap.servers': bootstrap_servers,
            'message.max.bytes': 10000000
        })

        # Consumer Config
        self.consumer = Consumer({
            'bootstrap.servers': bootstrap_servers,
            'group.id': 'finetuning-test-verifier',
            'auto.offset.reset': 'latest',
            'enable.auto.commit': True
        })
        self.consumer.subscribe([labeled_topic])

    def send_raw_flow(self, ue_id, timestamp_ms, packet_count=100, src_ip="192.168.1.1", dst_ip="10.0.0.1"):
        """构造并发送 Protobuf 消息"""
        flow = raw_flow_pb2.RawFlowMsg()
        flow.ue_id = ue_id
        flow.timestamp = timestamp_ms
        flow.packet_count = packet_count
        flow.src_ip = src_ip
        flow.dst_ip = dst_ip
        flow.src_port = 12345
        flow.dst_port = 80
        flow.l4_proto = "TCP"
        flow.up_bytes = packet_count * 100
        flow.down_bytes = packet_count * 200
        # Optional payload snippet
        flow.payload_snippet = b"GET / HTTP/1.1\r\nHost: example.com\r\n\r\n"
        
        # 关键：Partition Key 必须是 ue_id，确保 Java 端 assign 正确
        self.producer.produce(
            topic=self.raw_topic,
            key=ue_id.encode('utf-8'),
            value=flow.SerializeToString()
        )
        self.producer.flush()
        logger.info(f"Sent RawFlowMsg to {self.raw_topic}: ue_id={ue_id}, ts={timestamp_ms}, pkts={packet_count}")

    def wait_for_json_result(self, ue_id, timeout=30):
        """轮询等待指定 UE 的清洗结果"""
        logger.info(f"Waiting for JSON result in {self.labeled_topic} for ue_id={ue_id}...")
        start = time.time()
        while time.time() - start < timeout:
            msg = self.consumer.poll(1.0)
            if msg is None: 
                continue
            if msg.error():
                if msg.error().code() == KafkaError._PARTITION_EOF:
                    continue
                else:
                    logger.error(f"Kafka error: {msg.error()}")
                    continue
            
            try:
                val_str = msg.value().decode('utf-8')
                data = json.loads(val_str)
                # 假设 JSON 结果中包含 ue_id 字段，或者我们需要根据其他特征匹配
                # 根据设计，输出应该是清洗后的样本，理应包含 ue_id
                if data.get('ue_id') == ue_id:
                    logger.info(f"Received matched data for ue_id={ue_id}")
                    return data
            except Exception as e:
                logger.warning(f"Failed to parse message: {e}")
                
        logger.warning(f"Timeout waiting for result for ue_id={ue_id}")
        return None

    def close(self):
        self.producer.flush()
        self.consumer.close()
