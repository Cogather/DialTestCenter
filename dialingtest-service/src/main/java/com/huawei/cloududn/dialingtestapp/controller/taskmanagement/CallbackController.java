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
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(error("VALIDATION_ERROR", "请求体不能为空", 400));
        }

        ResponseEntity<Object> mainTaskIdError = extractMainTaskId(body);
        if (mainTaskIdError != null) {
            return mainTaskIdError;
        }

        Long mainTaskId;
        try {
            mainTaskId = parseMainTaskId(body);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(error("VALIDATION_ERROR", "参数格式错误: mainTaskId", 400));
        }

        ResponseEntity<Object> statusError = validateStatus(body);
        if (statusError != null) {
            return statusError;
        }
        boolean success = "SUCCESS".equalsIgnoreCase(String.valueOf(body.get("status")));

        logger.info("Received task callback: mainTaskId={}, status={}", mainTaskId, body.get("status"));

        Map<String, Object> resultData = extractResultData(body);
        boolean sent = sendEventToOrchestrator(mainTaskId, success, resultData);

        if (sent) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(404)
                    .body(error("TASK_NOT_FOUND", "Task not found: " + mainTaskId, 404));
        }
    }

    /**
     * 提取并验证主任务ID
     *
     * @param body 请求体
     * @return 验证失败时返回错误响应，否则返回null
     */
    private ResponseEntity<Object> extractMainTaskId(Map<String, Object> body) {
        Object idObj = body.get("mainTaskId");
        if (idObj == null) {
            idObj = body.get("main_task_id");
        }
        if (idObj == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(error("VALIDATION_ERROR", "参数验证失败: mainTaskId 不能为null; ", 400));
        }
        return null;
    }

    /**
     * 解析主任务ID为Long类型
     *
     * @param body 请求体
     * @return 主任务ID
     */
    private Long parseMainTaskId(Map<String, Object> body) {
        Object idObj = body.get("mainTaskId");
        if (idObj == null) {
            idObj = body.get("main_task_id");
        }
        if (idObj instanceof Number) {
            return ((Number) idObj).longValue();
        } else {
            return Long.parseLong(String.valueOf(idObj));
        }
    }

    /**
     * 验证状态参数
     *
     * @param body 请求体
     * @return 验证失败时返回错误响应，否则返回null
     */
    private ResponseEntity<Object> validateStatus(Map<String, Object> body) {
        Object statusObj = body.get("status");
        String status = statusObj == null ? null : String.valueOf(statusObj);
        boolean success = "SUCCESS".equalsIgnoreCase(status);
        if (!success && (status == null || !"FAILED".equalsIgnoreCase(status))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(error("VALIDATION_ERROR", "参数验证失败: status 必须为 SUCCESS 或 FAILED; ", 400));
        }
        return null;
    }

    /**
     * 提取结果数据
     *
     * @param body 请求体
     * @return 结果数据Map，不存在时返回null
     */
    private Map<String, Object> extractResultData(Map<String, Object> body) {
        Object rd = body.get("result_data");
        if (rd == null) {
            rd = body.get("resultData");
        }
        if (rd instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> tempMap = (Map<String, Object>) rd;
            return tempMap;
        }
        return null;
    }

    /**
     * 发送事件到任务编排器
     *
     * @param mainTaskId 主任务ID
     * @param success 是否成功
     * @param resultData 结果数据
     * @return 是否发送成功
     */
    private boolean sendEventToOrchestrator(Long mainTaskId, boolean success, Map<String, Object> resultData) {
        if (resultData == null) {
            return orchestratorService.sendResultEvent(mainTaskId, success);
        } else {
            return orchestratorService.sendResultEvent(mainTaskId, success, resultData);
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


