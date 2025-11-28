import unittest
import time

from .base import BaseTestCase
from .config import WS_ENABLE, AGENT_NTLM_HASH
from .json_message import JsonMessageHelper
from .binary_codec import BinaryCodec


@unittest.skipUnless(WS_ENABLE, "WS测试默认关闭，设置 EXEC_WS_ENABLE=1 以启用")
class TestUEManagementIT05(BaseTestCase):
    """IT-05: UE应用管理流程"""

    @unittest.skipUnless(AGENT_NTLM_HASH, "未提供 EXEC_AGENT_NTLM_HASH，无法计算")
    def test_it_05_001_app_list_query(self):
        """IT-05-001: 应用列表响应（Agent发送AppList-Response）"""
        client, token = self._ws_register_and_keep_connection()
        try:
            app_list = [
                {"name": "TestApp1", "version": "1.0.0", "package": "com.test.app1"},
                {"name": "TestApp2", "version": "2.1.0", "package": "com.test.app2"},
                {"name": "SystemApp", "version": "1.0.0", "package": "com.system.app"},
            ]
            client.send_control_json(
                "AppListResponse",
                {"apps": app_list},
                token=int(token) if str(token).isdigit() else None,
            )
            time.sleep(0.2)
        finally:
            client.close()

    @unittest.skipUnless(AGENT_NTLM_HASH, "未提供 EXEC_AGENT_NTLM_HASH，无法计算")
    def test_it_05_002_app_install_request(self):
        """IT-05-002: 应用安装响应（Agent发送AppInstall-Response）"""
        client, token = self._ws_register_and_keep_connection()
        try:
            client.send_control_json(
                "AppInstallResponse",
                {"state": 0},
                token=int(token) if str(token).isdigit() else None,
            )
            time.sleep(0.2)
        finally:
            client.close()

    @unittest.skipUnless(AGENT_NTLM_HASH, "未提供 EXEC_AGENT_NTLM_HASH，无法计算")
    def test_it_05_003_app_install_failure(self):
        """IT-05-003: 应用安装失败"""
        client, token = self._ws_register_and_keep_connection()
        try:
            client.send_control_json(
                "AppInstallResponse",
                {"state": 1, "errorMsg": "Package parsing failed"},
                token=int(token) if str(token).isdigit() else None,
            )
            time.sleep(0.2)
        finally:
            client.close()

    @unittest.skipUnless(AGENT_NTLM_HASH, "未提供 EXEC_AGENT_NTLM_HASH，无法计算")
    def test_it_05_004_screencap_query(self):
        """IT-05-004: 截屏响应（Agent发送ScreenCap-Response）"""
        client, token = self._ws_register_and_keep_connection()
        try:
            png_header = b"\x89PNG\r\n\x1a\n"
            image_data = png_header + b"fake_png_data_" + b"B" * 200
            crc = BinaryCodec.crc32_hex(image_data)
            client.send_control_json(
                "ScreencapResponse",
                {
                    "filename": "test.png",
                    "filelen": len(image_data),
                    "crc": crc,
                },
                token=int(token) if str(token).isdigit() else None,
            )
            client.send_binary(image_data)
            time.sleep(0.2)
        finally:
            client.close()

    @unittest.skipUnless(AGENT_NTLM_HASH, "未提供 EXEC_AGENT_NTLM_HASH，无法计算")
    def test_it_05_005_multiple_ue_app_management(self):
        """IT-05-005: 多UE应用管理"""
        client, token = self._ws_register_and_keep_connection()
        try:
            ues = ["SN001", "SN002", "SN003"]
            for serial_no in ues:
                client.send_control_json(
                    "AppListQuery",
                    {"serialNo": serial_no},
                    token=int(token) if str(token).isdigit() else None,
                )

                app_list = [
                    {"name": f"App_{serial_no}_1", "version": "1.0.0"},
                    {"name": f"App_{serial_no}_2", "version": "1.0.0"},
                ]
                client.send_control_json(
                    "AppListResponse",
                    {"apps": app_list},
                    token=int(token) if str(token).isdigit() else None,
                )

                if serial_no == "SN001":
                    client.send_control_json(
                        "AppInstallRequest",
                        {
                            "serialNo": serial_no,
                            "taskId": 200001,
                            "appName": "TestApp.apk",
                        },
                        token=int(token) if str(token).isdigit() else None,
                    )
                    client.send_control_json(
                        "AppInstallResponse",
                        {"state": 0},
                        token=int(token) if str(token).isdigit() else None,
                    )
        finally:
            client.close()

    @unittest.skipUnless(AGENT_NTLM_HASH, "未提供 EXEC_AGENT_NTLM_HASH，无法计算")
    def test_it_05_006_app_management_error_handling(self):
        """IT-05-006: 应用管理错误处理"""
        client, token = self._ws_register_and_keep_connection()
        try:
            client.send_control_json(
                "AppListQuery",
                {"serialNo": "NON_EXISTENT_SN"},
                token=int(token) if str(token).isdigit() else None,
            )

            client.send_control_json(
                "AppInstallResponse",
                {"state": 1, "errorMsg": "UE device not found"},
                token=int(token) if str(token).isdigit() else None,
            )

            client.send_control_json(
                "AppInstallRequest",
                {"serialNo": "SN001", "taskId": 999998, "appName": "invalid.apk"},
                token=int(token) if str(token).isdigit() else None,
            )

            client.send_control_json(
                "AppInstallResponse",
                {"state": 1, "errorMsg": "Invalid package data"},
                token=int(token) if str(token).isdigit() else None,
            )
        finally:
            client.close()
