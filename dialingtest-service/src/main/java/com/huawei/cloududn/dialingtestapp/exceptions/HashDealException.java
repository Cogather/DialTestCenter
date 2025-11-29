package com.huawei.cloududn.dialingtestapp.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Hash处理异常处理器
 * 主要处理数据库唯一性约束等数据完整性相关的异常
 *
 * @author DialTestCenter
 * @version 1.0.0
 * @since 2024-01-01
 */
@ControllerAdvice
public class HashDealException {

    private static final Logger logger = LoggerFactory.getLogger(HashDealException.class);

    /**
     * 处理数据库唯一约束违反异常（重复键）
     *
     * @param e 重复键异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateKeyException(
            DuplicateKeyException e, HttpServletRequest request) {
        logger.warn("重复键异常 - URI: {}, Method: {}, Message: {}",
                request.getRequestURI(), request.getMethod(), e.getMessage());

        String message = "数据已存在，请检查唯一性约束";
        String errorMessage = e.getMessage();
        if (errorMessage != null && errorMessage.contains("uk_template_task_name")) {
            message = "模板名称已存在";
        }
        Map<String, Object> response = createErrorResponse("DUPLICATE_KEY", message, 409);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * 创建错误响应对象
     *
     * @param errorCode 错误代码
     * @param message 错误消息
     * @param statusCode HTTP状态码
     * @return 错误响应Map
     */
    private Map<String, Object> createErrorResponse(String errorCode, String message, int statusCode) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("errorCode", errorCode);
        response.put("message", message);
        response.put("statusCode", statusCode);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}

