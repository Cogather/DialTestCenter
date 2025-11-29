package com.huawei.cloududn.dialingtestapp.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统通用异常处理器 (DefaultGlobalException)
 * 处理运行时异常、空指针、404等通用系统异常
 *
 * @author DialTestCenter
 * @version 1.0.0
 * @since 2024-01-01
 */
@ControllerAdvice
public class DefaultGlobalException {

    private static final Logger logger = LoggerFactory.getLogger(DefaultGlobalException.class);

    /**
     * 处理404异常
     *
     * @param e 404异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoHandlerFoundException(
            NoHandlerFoundException e, HttpServletRequest request) {
        logger.error("404异常 - URI: {}, Method: {}", request.getRequestURI(), request.getMethod(), e);

        String message = "请求的资源不存在: " + e.getRequestURL();
        Map<String, Object> response = createErrorResponse("NOT_FOUND", message, 404);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * 处理ResponseStatusException（保留原始HTTP状态码）
     *
     * @param e ResponseStatusException
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(
            ResponseStatusException e, HttpServletRequest request) {
        HttpStatus status = e.getStatus();
        logger.warn("ResponseStatusException - URI: {}, Method: {}, Status: {}, Message: {}",
                request.getRequestURI(), request.getMethod(), status, e.getReason());

        String message = e.getReason() != null ? e.getReason() : status.getReasonPhrase();
        Map<String, Object> response = createErrorResponse(
                status.name().replace(" ", "_"),
                message,
                status.value());
        return ResponseEntity.status(status).body(response);
    }

    /**
     * 处理空指针异常
     *
     * @param e 空指针异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Map<String, Object>> handleNullPointerException(
            NullPointerException e, HttpServletRequest request) {
        logger.error("空指针异常 - URI: {}, Method: {}", request.getRequestURI(), request.getMethod(), e);

        Map<String, Object> response = createErrorResponse("NULL_POINTER", "系统内部错误", 500);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * 处理运行时异常
     *
     * @param e 运行时异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        logger.error("运行时异常 - URI: {}, Method: {}, Message: {}",
                request.getRequestURI(), request.getMethod(), e.getMessage(), e);

        Map<String, Object> response = createErrorResponse("RUNTIME_ERROR", "系统运行时错误", 500);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * 处理所有其他异常
     *
     * @param e 异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e, HttpServletRequest request) {
        logger.error("未知异常 - URI: {}, Method: {}, Message: {}",
                request.getRequestURI(), request.getMethod(), e.getMessage(), e);

        Map<String, Object> response = createErrorResponse("INTERNAL_ERROR", "系统内部错误", 500);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
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

