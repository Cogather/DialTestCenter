/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.controller.taskmanagement;
import com.huawei.cloududn.dialingtestapp.service.taskmanagement.orchestration.TaskOrchestratorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 外部回调控制器：接收异步任务结果并驱动状态机。
 *
 * <p>最小实现：期望body包含 main_task_id, status(SUCCESS/FAILED)。</p>
 *
 * @author g00940940
 * @since 2025-10-24
 */
@RestController
@RequestMapping("/api")
public class CallbackController {
    private static final Logger logger = LoggerFactory.getLogger(CallbackController.class);

    @Autowired
    private TaskOrchestratorService orchestratorService;

    @PostMapping("/callbacks/notify")
    public ResponseEntity<Object> notifyCallback(@RequestBody Map<String, Object> body) {
        if (body == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error("VALIDATION_ERROR", "请求体不能为空", 400));
        } else {
            Object idObj = body.get("mainTaskId");
            if (idObj == null) {
                idObj = body.get("main_task_id");
            }
            if (idObj == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(error("VALIDATION_ERROR", "参数验证失败: mainTaskId 不能为null; ", 400));
            } else {
                Long mainTaskId;
                try {
                    if (idObj instanceof Number) {
                        mainTaskId = ((Number) idObj).longValue();
                    } else {
                        mainTaskId = Long.parseLong(String.valueOf(idObj));
                    }
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error("VALIDATION_ERROR", "参数格式错误: mainTaskId", 400));
                }

                Object statusObj = body.get("status");
                String status = statusObj == null ? null : String.valueOf(statusObj);
                boolean success = "SUCCESS".equalsIgnoreCase(status);
                if (!success && (status == null || !"FAILED".equalsIgnoreCase(status))) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(error("VALIDATION_ERROR", "参数验证失败: status 必须为 SUCCESS 或 FAILED; ", 400));
                } else {
                    logger.info("Received task callback: mainTaskId={}, status={}", mainTaskId, status);
                    Object rd = body.get("result_data");
                    if (rd == null) {
                        rd = body.get("resultData");
                    }
                    java.util.Map<String, Object> resultData = null;
                    if (rd instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> tempMap = (java.util.Map<String, Object>) rd;
                        resultData = tempMap;
                    }
                    if (resultData == null) {
                        orchestratorService.sendResultEvent(mainTaskId, success);
                    } else {
                        orchestratorService.sendResultEvent(mainTaskId, success, resultData);
                    }
                    return ResponseEntity.ok().build();
                }
            }
        }
    }

    private Map<String, Object> error(String code, String message, int status) {
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("success", false);
        resp.put("errorCode", code);
        resp.put("message", message);
        resp.put("statusCode", status);
        resp.put("timestamp", System.currentTimeMillis());
        return resp;
    }
}


