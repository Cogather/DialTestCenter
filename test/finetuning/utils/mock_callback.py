import threading
import json
import time
import logging
from http.server import HTTPServer, BaseHTTPRequestHandler
from typing import Optional, Dict

logger = logging.getLogger(__name__)

class CallbackHandler(BaseHTTPRequestHandler):
    server_instance = None # Reference to parent wrapper

    def do_POST(self):
        if self.path == '/dialingtest/callbacks/notify':
            content_length = int(self.headers['Content-Length'])
            post_data = self.rfile.read(content_length)
            try:
                data = json.loads(post_data.decode('utf-8'))
                logger.info(f"Received callback: {data}")
                if self.server_instance:
                    self.server_instance.add_callback(data)
                
                self.send_response(200)
                self.send_header('Content-Type', 'application/json')
                self.end_headers()
                self.wfile.write(json.dumps({"code": 200, "message": "OK"}).encode('utf-8'))
            except Exception as e:
                logger.error(f"Error processing callback: {e}")
                self.send_response(400)
                self.end_headers()
        else:
            self.send_response(404)
            self.end_headers()

    def log_message(self, format, *args):
        # Suppress default logging to keep console clean
        pass

class MockCallbackServer:
    def __init__(self, host, port):
        self.host = host
        self.port = port
        self.server = HTTPServer((host, port), CallbackHandler)
        self.server.allow_reuse_address = True
        CallbackHandler.server_instance = self
        self.callbacks = [] # List of received callbacks
        self.lock = threading.Lock()
        self.thread = threading.Thread(target=self.server.serve_forever)
        self.thread.daemon = True

    def start(self):
        logger.info(f"Starting Mock Callback Server on {self.host}:{self.port}")
        self.thread.start()

    def stop(self):
        logger.info("Stopping Mock Callback Server")
        self.server.shutdown()
        self.server.server_close()

    def add_callback(self, data):
        with self.lock:
            self.callbacks.append(data)

    def get_latest_callback(self, job_id: str) -> Optional[Dict]:
        """Find callback by job_id (async_job_id in payload)"""
        with self.lock:
            # Search backwards for latest
            for cb in reversed(self.callbacks):
                if str(cb.get('async_job_id')) == str(job_id):
                    return cb
        return None

    def wait_for_callback(self, job_id: str, timeout=30) -> Optional[Dict]:
        start = time.time()
        while time.time() - start < timeout:
            cb = self.get_latest_callback(job_id)
            if cb:
                return cb
            time.sleep(0.5)
        return None
