/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.controller.taskmanagement;

import com.huawei.cloududn.dialingtest.api.TemplatesApi;
import com.huawei.cloududn.dialingtest.model.TemplateEntity;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.TemplateMgmtService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 模板管理接口（CRUD）。
 *
 * @author g00940940
 * @since 2025-10-24
 */
@RestController
@RequestMapping("/api")
public class TemplateController implements TemplatesApi {
    private static final Logger logger = LoggerFactory.getLogger(TemplateController.class);

    @Autowired
    private TemplateMgmtService templateService;

    @Override
    public ResponseEntity<TemplateEntity> createTemplate(
            String xCsrfToken,
            String xUsername,
            @RequestBody TemplateEntity body) {
        logger.info("Create template request received by user: {}", xUsername);

        // 验证用户名
        if (xUsername == null || xUsername.trim().isEmpty()) {
            logger.warn("Create template request missing username");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 验证CSRF令牌
        if (xCsrfToken == null || xCsrfToken.trim().isEmpty()) {
            logger.warn("Create template request missing CSRF token");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // 设置创建人
        if (body != null) {
            body.setCreator(xUsername);
        }

        TemplateEntity saved = templateService.create(body);
        logger.info("Template created successfully with id: {} by user: {}", saved.getId(), xUsername);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Override
    public ResponseEntity<Void> deleteTemplate(String xUsername, Integer id) {
        logger.info("Delete template request received for id: {} by user: {}", id, xUsername);

        // 验证用户名
        if (xUsername == null || xUsername.trim().isEmpty()) {
            logger.warn("Delete template request missing username");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long longId = id == null ? null : id.longValue();
        templateService.delete(longId);
        logger.info("Template deleted successfully with id: {} by user: {}", id, xUsername);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<TemplateEntity> getTemplateById(Integer id) {
        Long longId = id == null ? null : id.longValue();
        TemplateEntity t = templateService.findById(longId);
        if (t == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } else {
            return ResponseEntity.ok(t);
        }
    }

    @Override
    public ResponseEntity<List<TemplateEntity>> getTemplates() {
        List<TemplateEntity> list = templateService.findAll();
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<TemplateEntity> updateTemplate(
            String xUsername,
            Integer id,
            @RequestBody TemplateEntity body) {
        logger.info("Update template request received for id: {} by user: {}", id, xUsername);

        // 验证用户名
        if (xUsername == null || xUsername.trim().isEmpty()) {
            logger.warn("Update template request missing username");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        body.setId(id);
        TemplateEntity updated = templateService.update(body);
        logger.info("Template updated successfully with id: {} by user: {}", id, xUsername);
        return ResponseEntity.ok(updated);
    }
}


