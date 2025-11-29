package com.huawei.cloududn.dialingtestapp.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Excel处理异常处理器
 * 处理文件上传大小超限等与文件处理相关的异常
 *
 * @author DialTestCenter
 * @version 1.0.0
 * @since 2024-01-01
 */
@ControllerAdvice
public class ExcelDealException {

    private static final Logger logger = LoggerFactory.getLogger(ExcelDealException.class);

    /**
     * 处理文件上传大小超限异常
     *
     * @param e 文件上传大小超限异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException e, HttpServletRequest request) {
        logger.error("文件上传大小超限异常 - URI: {}, Method: {}, Max Size: {}",
                request.getRequestURI(), request.getMethod(), e.getMaxUploadSize(), e);

        String message = "文件上传大小超过限制，最大允许: " + (e.getMaxUploadSize() / 1024 / 1024) + "MB";
        Map<String, Object> response = createErrorResponse("FILE_SIZE_EXCEEDED", message, 413);
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
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

