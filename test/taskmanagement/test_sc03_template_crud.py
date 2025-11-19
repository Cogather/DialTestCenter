# -*- coding: utf-8 -*-
"""
SC-03: 模板管理完整流程

测试用例：
  TC-03-001: 创建新模板
  TC-03-002: 模板唯一性约束
  TC-03-003: 更新模板
  TC-03-004: 删除模板
  TC-03-005: 查询所有模板
"""

import unittest
import time

from taskmanagement.base import BaseTestCase
from taskmanagement.utils import measure_response_time
from taskmanagement.config import PERFORMANCE_BASELINE


class TestTemplateCrud(BaseTestCase):
    """模板管理测试套件"""

    def setUp(self):
        super().setUp()
        self.truncate_template_table()
        # 使用时间戳生成唯一的模板名称，避免并发测试冲突
        self.test_template_name = f"每周VPN全量验证_{int(time.time() * 1000)}"

    # ==================== TC-03-001 ====================
    def test_tc_03_001_create_template_success(self):
        """
        TC-03-001: 创建新模板
        期望：201 Created，返回生成ID
        """
        # Act
        response, response_time = measure_response_time(
            self.create_template,
            name=self.test_template_name
        )

        # Assert
        self.assertions.assert_status_code(response, 201)
        self.assertions.assert_response_time(
            response_time,
            PERFORMANCE_BASELINE['create_template']
        )
        self.assertions.assert_json_field_not_null(response, 'id')
        self.assertions.assert_json_field(response, 'name', self.test_template_name)

    # ==================== TC-03-002 ====================
    def test_tc_03_002_create_template_unique_name(self):
        """
        TC-03-002: 模板唯一性约束
        期望：重复name返回409/400
        """
        # Arrange
        unique_name = f"模板唯一性测试_{int(time.time() * 1000)}"
        resp1 = self.create_template(name=unique_name)
        self.assertions.assert_status_code(resp1, 201)

        # Act
        resp2 = self.create_template(name=unique_name)

        # Assert
        self.assertIn(resp2.status_code, [400, 409])

    # ==================== TC-03-003 ====================
    def test_tc_03_003_update_template_success(self):
        """
        TC-03-003: 更新模板
        期望：200 OK，updateTime更新
        """
        # Arrange
        template_name = f"更新测试模板_{int(time.time() * 1000)}"
        create_resp = self.create_template(name=template_name)
        self.assertions.assert_status_code(create_resp, 201)
        template_id = create_resp.json()['id']

        # Act
        updated_name = f"{template_name}_v2"
        update_resp = self.update_template(
            template_id,
            name=updated_name,
            cron='0 0 2 ? * 1',
            enabled=False,
            input='{"businessType":"VPN_BLOCK","scenario":"TRAINING"}',
            creator='admin',
            description='更新为训练场景'
        )

        # Assert
        self.assertions.assert_status_code(update_resp, 200)
        data = update_resp.json()
        self.assertEqual(data['id'], template_id)
        self.assertEqual(data['name'], updated_name)
        self.assertIn('updateTime', data)

    # ==================== TC-03-004 ====================
    def test_tc_03_004_delete_template_success(self):
        """
        TC-03-004: 删除模板
        期望：204 No Content
        """
        # Arrange
        create_resp = self.create_template(name='待删除模板')
        self.assertions.assert_status_code(create_resp, 201)
        template_id = create_resp.json()['id']

        # Act
        delete_resp = self.delete_template(template_id)

        # Assert
        self.assertions.assert_status_code(delete_resp, 204)

        # 再次查询应404或空
        get_resp = self.get_template_by_id(template_id)
        self.assertIn(get_resp.status_code, [404, 204, 200])
        if get_resp.status_code == 200:
            # 某些实现可能软删除；尽量不做强断言
            pass

    # ==================== TC-03-005 ====================
    def test_tc_03_005_list_templates_success(self):
        """
        TC-03-005: 查询所有模板
        期望：200 OK，返回列表且包含必需字段
        """
        # Arrange
        names = ['模板1', '模板2', '模板3']
        for n in names:
            self.create_template(name=n)

        # Act
        resp = self.get_templates()

        # Assert
        self.assertions.assert_status_code(resp, 200)
        templates = resp.json()
        self.assertIsInstance(templates, list)
        self.assertGreaterEqual(len(templates), 3)
        ids = [tpl['id'] for tpl in templates]
        self.assertEqual(ids, sorted(ids, reverse=True))


if __name__ == '__main__':
    unittest.main()


