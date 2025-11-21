"""
V5双连接WebSocket客户端
管理控制链路和数据链路两个独立的WebSocket连接
"""
import ssl
import json
import logging
from typing import Any, Dict, Optional, Tuple

import websocket  # type: ignore
from websocket import WebSocketConnectionClosedException

from .config import WS_CONTROL_URL, WS_DATA_URL, WS_TIMEOUT
from .json_message import JsonMessageHelper
from .binary_codec import BinaryCodec

logger = logging.getLogger(__name__)


class DualWebSocketClient:
    """
    V5版本执行机双链路 WebSocket 客户端封装
    管理控制链路和数据链路两个独立的WebSocket连接
    """

    def __init__(self, control_url: str = WS_CONTROL_URL, data_url: str = WS_DATA_URL, timeout: int = WS_TIMEOUT) -> None:
        self._control_url = control_url
        self._data_url = data_url
        self._timeout = timeout
        self._control_ws: Optional[websocket.WebSocket] = None
        self._data_ws: Optional[websocket.WebSocket] = None
        self._token: Optional[int] = None  # 存储认证后获取的token（整数）
        self._json_helper = JsonMessageHelper()
        self._sslopt = {"cert_reqs": ssl.CERT_NONE}

    def __del__(self):
        """析构函数：确保连接被关闭"""
        try:
            self.close_all()
        except:
            pass

    @property
    def token(self) -> Optional[int]:
        return self._token

    @property
    def is_fully_connected(self) -> bool:
        """检查双连接是否都已建立"""
        return (
            self._control_ws is not None and
            self._data_ws is not None and
            self._token is not None
        )

    @property
    def is_control_connected(self) -> bool:
        """检查控制链路是否已连接"""
        return self._control_ws is not None

    @property
    def is_data_connected(self) -> bool:
        """检查数据链路是否已连接"""
        return self._data_ws is not None

    @property
    def is_bound(self) -> bool:
        """检查是否已完成token绑定（别名，与is_fully_connected相同）"""
        return self.is_fully_connected

    def connect_control(self) -> None:
        """连接控制链路"""
        if self._control_ws is not None:
            logger.warning("Control link already exists, closing old connection")
            try:
                self._control_ws.close()
            except:
                pass
        self._control_ws = websocket.create_connection(
            self._control_url, 
            timeout=self._timeout, 
            sslopt=self._sslopt
            # 注意：不使用enable_multithread，避免后台线程导致连接无法关闭
        )
        logger.info(f"Control link connected to {self._control_url}")

    def connect_data(self) -> None:
        """连接数据链路"""
        if self._data_ws is not None:
            logger.warning("Data link already exists, closing old connection")
            try:
                self._data_ws.close()
            except:
                pass
        self._data_ws = websocket.create_connection(
            self._data_url, 
            timeout=self._timeout, 
            sslopt=self._sslopt
            # 注意：不使用enable_multithread，避免后台线程导致连接无法关闭
        )
        logger.info(f"Data link connected to {self._data_url}")

    def connect_all(self) -> None:
        """连接所有链路"""
        self.connect_control()
        self.connect_data()

    def close_control(self) -> None:
        """关闭控制链路"""
        if self._control_ws:
            try:
                # 先关闭WebSocket连接
                self._control_ws.close()
                # 如果存在后台线程，等待其终止
                if hasattr(self._control_ws, 'sock') and self._control_ws.sock:
                    try:
                        self._control_ws.sock.shutdown(2)  # SHUT_RDWR
                        self._control_ws.sock.close()
                    except:
                        pass
                logger.info("Control link closed.")
            except WebSocketConnectionClosedException:
                logger.debug("Control link already closed.")
            except Exception as e:
                logger.warning(f"Error closing control link: {e}")
            finally:
                self._control_ws = None

    def close_data(self) -> None:
        """关闭数据链路"""
        if self._data_ws:
            try:
                # 先关闭WebSocket连接
                self._data_ws.close()
                # 如果存在后台线程，等待其终止
                if hasattr(self._data_ws, 'sock') and self._data_ws.sock:
                    try:
                        self._data_ws.sock.shutdown(2)  # SHUT_RDWR
                        self._data_ws.sock.close()
                    except:
                        pass
                logger.info("Data link closed.")
            except WebSocketConnectionClosedException:
                logger.debug("Data link already closed.")
            except Exception as e:
                logger.warning(f"Error closing data link: {e}")
            finally:
                self._data_ws = None

    def close_all(self) -> None:
        """关闭所有链路"""
        logger.debug("Closing all WebSocket connections...")
        self.close_control()
        self.close_data()
        self._token = None
        logger.info("All WebSocket connections closed.")

    # Alias for compatibility
    def close(self) -> None:
        """兼容旧API：关闭所有连接"""
        self.close_all()

    def send_json(self, message: str) -> None:
        """通过控制链路发送JSON消息（已编码的字符串）"""
        if self._control_ws is None:
            raise RuntimeError("Control WebSocket is not connected")
        self._control_ws.send(message)
        logger.debug(f"Sent JSON to control link: {message}")

    def send_control_json(self, msg_type: str, payload: Dict[str, Any], token: Optional[int] = None) -> None:
        """
        通过控制链路发送JSON消息（使用JsonMessageHelper构造）
        
        Args:
            msg_type: 消息类型
            payload: 消息负载
            token: 认证token（可选，认证阶段为None）
        """
        if self._control_ws is None:
            raise RuntimeError("Control WebSocket is not connected")
        
        # 如果未指定token，且客户端已认证，则使用客户端的token
        actual_token = token if token is not None else self._token
        envelope = self._json_helper.build(msg_type, payload, actual_token)
        self._control_ws.send(json.dumps(envelope))
        logger.debug(f"Sent JSON ({msg_type}) to control link. Token: {actual_token}")

    def recv_json(self, timeout: Optional[int] = None) -> Dict[str, Any]:
        """从控制链路接收JSON消息（原始格式）"""
        if self._control_ws is None:
            raise RuntimeError("Control WebSocket is not connected")
        self._control_ws.settimeout(timeout if timeout is not None else self._timeout)
        raw = self._control_ws.recv()
        if isinstance(raw, bytes):
            raw = raw.decode("utf-8")
        return json.loads(raw)

    def recv_control_json(self, timeout: Optional[int] = None) -> Tuple[str, Optional[int], Dict[str, Any]]:
        """
        从控制链路接收JSON消息并解析
        
        Returns:
            (msg_type, token, payload)
        """
        if self._control_ws is None:
            raise RuntimeError("Control WebSocket is not connected")
        
        self._control_ws.settimeout(timeout if timeout is not None else self._timeout)
        raw = self._control_ws.recv()
        if isinstance(raw, bytes):
            raw = raw.decode("utf-8")
        envelope = json.loads(raw)
        msg_type, token, payload = self._json_helper.parse(envelope)
        logger.debug(f"Received JSON ({msg_type}) from control link. Token: {token}")
        return msg_type, token, payload

    def send_data_text(self, message: str) -> None:
        """通过数据链路发送文本消息（用于绑定请求等控制消息）"""
        if self._data_ws is None:
            raise RuntimeError("Data WebSocket is not connected")
        
        # 确保连接可用
        if not hasattr(self._data_ws, 'sock') or not self._data_ws.sock:
            raise RuntimeError("Data WebSocket socket is not active")
        
        # 明确指定为TEXT opcode，避免被误判为binary
        self._data_ws.send(message, opcode=websocket.ABNF.OPCODE_TEXT)
        logger.debug(f"Sent text message (length: {len(message)}) to data link.")

    def send_binary(self, data: bytes) -> None:
        """通过数据链路发送二进制消息"""
        if self._data_ws is None:
            raise RuntimeError("Data WebSocket is not connected")
        self._data_ws.send(data, opcode=websocket.ABNF.OPCODE_BINARY)
        logger.debug(f"Sent binary data (size: {len(data)}) to data link.")

    def recv_binary(self, timeout: Optional[int] = None) -> bytes:
        """从数据链路接收二进制消息"""
        if self._data_ws is None:
            raise RuntimeError("Data WebSocket is not connected")
        self._data_ws.settimeout(timeout if timeout is not None else self._timeout)
        raw = self._data_ws.recv()
        if not isinstance(raw, bytes):
            raise TypeError("Expected binary data, but received text.")
        logger.debug(f"Received binary data (size: {len(raw)}) from data link.")
        return raw

    def authenticate_and_bind(self, username: str, password_hash: str, hostname: str) -> int:
        """
        V5完整认证流程：控制链路CHAP认证 + 数据链路token绑定
        
        Args:
            username: 用户名
            password_hash: 密码的SHA256哈希（64位十六进制字符串）
            hostname: 主机名
            
        Returns:
            认证token（整数）
        """
        logger.info("Starting V5 authentication and data link binding...")
        
        # 确保控制链路已连接
        if self._control_ws is None:
            self.connect_control()

        # 1. Register-Request
        req_payload = {"hostname": hostname, "username": username}
        req_envelope = self._json_helper.build("RegisterRequest", req_payload, token=None)
        self._control_ws.send(json.dumps(req_envelope))
        logger.debug("Sent RegisterRequest")

        # 2. Register-Challenge
        res_envelope = self.recv_json()
        msg_type, _, payload = self._json_helper.parse(res_envelope)
        if msg_type not in ("RegisterChallenge", "register_challenge"):
            logger.error(f"Expected RegisterChallenge, got {msg_type}")
            raise RuntimeError(f"Expected RegisterChallenge, got {msg_type}")
        
        challenge_b64 = payload["challenge"]
        challenge_id = payload.get("challenge-id", 0)
        challenge_bytes = BinaryCodec.decode_base64(challenge_b64)
        logger.debug(f"Received RegisterChallenge, challenge_id={challenge_id}")

        # 3. Calculate response and send Register-Response
        response_hex = BinaryCodec.compute_chap_response(password_hash, challenge_bytes)
        resp_payload = {"challenge-id": challenge_id, "username": username, "response": response_hex}
        resp_envelope = self._json_helper.build("RegisterResponse", resp_payload, token=None)
        self._control_ws.send(json.dumps(resp_envelope))
        logger.debug("Sent RegisterResponse")

        # 4. Register-Result
        logger.info("Waiting for Register-Result...")
        res2_envelope = self.recv_json()
        logger.debug(f"Received Register-Result envelope: {res2_envelope}")
        msg_type2, token_from_msg, payload2 = self._json_helper.parse(res2_envelope)
        if msg_type2 not in ("RegisterResult", "register_result", "register_ack"):
            logger.error(f"Unexpected response: {msg_type2}, payload: {payload2}")
            raise RuntimeError(f"Unexpected response: {msg_type2}")
        
        result = payload2.get("result")
        if result is not None and result != 0:
            error_msg = payload2.get('description', 'Unknown error')
            logger.error(f"Authentication failed: {error_msg}")
            raise RuntimeError(f"Authentication failed: {error_msg}")
        
        status = payload2.get("status")
        if status is not None and status not in ("success", 0):
            logger.error(f"Authentication failed: status={status}")
            raise RuntimeError(f"Authentication failed: status={status}")
        
        # token可以从信封或payload中获取
        self._token = token_from_msg if token_from_msg is not None else payload2.get("token")
        if self._token is None:
            logger.error("No token in RegisterResult")
            raise RuntimeError("No token in RegisterResult")
            
        # 确保token是整数
        if isinstance(self._token, str):
            self._token = int(self._token)
            
        logger.info(f"Control link authenticated. Token: {self._token}")

        # 5. Data link binding (如果数据链路还未连接，则连接)
        if self._data_ws is None:
            self.connect_data()
            logger.debug("Data link connected for binding")
        else:
            logger.debug(f"Data link already connected, checking status...")
        
        # 确保数据链路可用（websocket-client的WebSocket对象没有connected属性，通过sock判断）
        try:
            if not self._data_ws or not hasattr(self._data_ws, 'sock') or not self._data_ws.sock:
                logger.warning("Data link connection lost, reconnecting...")
                self.close_data()
                self.connect_data()
        except Exception as e:
            logger.warning(f"Failed to check data link status: {e}, reconnecting...")
            self.close_data()
            self.connect_data()
        
        # V5: 数据链路绑定（发送token绑定请求）
        # 服务端DataLinkEndpoint期望接收一条包含token的文本消息（TEXT frame）
        try:
            bind_envelope = self._json_helper.build("DataLinkBindRequest", {}, self._token)
            bind_message = json.dumps(bind_envelope)
            logger.info(f"📤 Prepared DataLinkBindRequest: {bind_message}")
            
            # 使用send_data_text明确指定为TEXT消息
            self.send_data_text(bind_message)
            logger.info(f"✓ Sent DataLinkBindRequest (TEXT frame) to data link, token: {self._token}")
        except Exception as e:
            logger.error(f"✗ Failed to send DataLinkBindRequest: {e}", exc_info=True)
            raise RuntimeError(f"Data link binding failed: {e}")
        
        # 等待服务端处理绑定
        import time
        time.sleep(0.3)  # 给服务端时间处理绑定（增加到300ms）
        
        logger.info(f"V5 authentication and binding completed. Token: {self._token}")
        return self._token
