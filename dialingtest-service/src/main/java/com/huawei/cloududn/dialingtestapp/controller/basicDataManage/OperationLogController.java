/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.controller.basicDataManage;

import com.huawei.cloududn.dialingtest.api.OperationLogsApi;
import com.huawei.cloududn.dialingtest.model.CreateOperationLogRequest;
import com.huawei.cloududn.dialingtest.model.OperationLogPageResponse;
import com.huawei.cloududn.dialingtest.model.OperationLogResponse;
import com.huawei.cloududn.dialingtest.model.OperationLogStatisticsResponse;
import com.huawei.cloududn.dialingtestapp.service.basicDataManage.OperationLogService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 操作记录控制器，提供操作记录管理的REST API接口
 * 支持操作记录的创建、查询、统计、导出等操作
 *
 * @author g00940940
 * @since 2025-01-15
 */
@RestController
@RequestMapping("/api")
public class OperationLogController implements OperationLogsApi {
    private static final Logger logger = LoggerFactory.getLogger(OperationLogController.class);
    
    @Autowired
    private OperationLogService operationLogService;
    
    /**
     * 分页查询操作记录
     *
     * @param page 页码，从0开始
     * @param size 每页大小
     * @param username 用户名筛选
     * @param operationType 操作类型筛选
     * @param operationTarget 操作目标筛选
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 分页操作记录响应
     */
    @Override
    public ResponseEntity<OperationLogPageResponse> getOperationLogs(
            Integer page, Integer size, String username, String operationType, 
            String operationTarget, String startTime, String endTime) {
        try {
            logger.info("Querying operation logs with page: {}, size: {}, username: {}", page, size, username);
            
            OperationLogPageResponse response = operationLogService.getOperationLogs(
                page, size, username, operationType, operationTarget, startTime, endTime);
            
            logger.info("Successfully queried operation logs, total elements: {}", 
                       response.getData().getTotalElements());
            
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
                
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request parameters for operation logs query: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(createErrorPageResponse("Invalid request parameters: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Error querying operation logs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(createErrorPageResponse("Internal server error"));
        }
    }
    
    /**
     * 创建操作记录
     *
     * @param xCsrfToken CSRF防护令牌
     * @param xUsername 操作用户名
     * @param body 操作记录创建请求
     * @return 操作记录响应
     */
    @Override
    public ResponseEntity<OperationLogResponse> createOperationLog(
            @RequestHeader("X-Csrf-Token") String xCsrfToken,
            @RequestHeader("X-Username") String xUsername,
            @Valid CreateOperationLogRequest body) {
        try {
            logger.info("Creating operation log for user: {}, operation: {} {}", 
                       body.getUsername(), body.getOperationType(), body.getOperationTarget());
            
            OperationLogResponse response = operationLogService.createOperationLog(body);
            
            logger.info("Successfully created operation log with ID: {}", 
                       response.getData().getId());
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
                
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request parameters for operation log creation: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(createErrorResponse("Invalid request parameters: " + e.getMessage()));
        } catch (IllegalStateException e) {
            logger.error("Failed to create operation log: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(createErrorResponse("Failed to create operation log"));
        } catch (Exception e) {
            logger.error("Error creating operation log", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(createErrorResponse("Internal server error"));
        }
    }
    
    /**
     * 查询操作记录详情
     *
     * @param id 操作记录ID
     * @return 操作记录响应
     */
    @Override
    public ResponseEntity<OperationLogResponse> getOperationLogById(Integer id) {
        try {
            logger.info("Querying operation log details for ID: {}", id);
            
            OperationLogResponse response = operationLogService.getOperationLogById(id);
            
            logger.info("Successfully queried operation log details for ID: {}", id);
            
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
                
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid operation log ID: {}", id);
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(createErrorResponse("Invalid operation log ID"));
        } catch (Exception e) {
            logger.error("Error querying operation log details for ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(createErrorResponse("Operation log not found"));
        }
    }
    
    /**
     * 获取操作记录统计信息
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计信息响应
     */
    @Override
    public ResponseEntity<OperationLogStatisticsResponse> getOperationLogStatistics(
            String startTime, String endTime) {
        try {
            logger.info("Querying operation log statistics from {} to {}", startTime, endTime);
            
            OperationLogStatisticsResponse response = operationLogService.getOperationLogStatistics(startTime, endTime);
            
            logger.info("Successfully queried operation log statistics");
            
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
                
        } catch (Exception e) {
            logger.error("Error querying operation log statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(createErrorStatisticsResponse("Internal server error"));
        }
    }
    
    /**
     * 导出操作记录
     *
     * @param username 用户名筛选
     * @param operationType 操作类型筛选
     * @param operationTarget 操作目标筛选
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return Excel文件资源
     */
    @Override
    public ResponseEntity<Resource> exportOperationLogs(
            String username, String operationType, String operationTarget, 
            String startTime, String endTime) {
        try {
            logger.info("Exporting operation logs with filters: username={}, operationType={}", username, operationType);
            
            Resource resource = operationLogService.exportOperationLogs(
                username, operationType, operationTarget, startTime, endTime);
            
            logger.info("Successfully exported operation logs");
            
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
                
        } catch (Exception e) {
            logger.error("Error exporting operation logs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 创建错误响应
     *
     * @param message 错误消息
     * @return 错误响应
     */
    private OperationLogResponse createErrorResponse(String message) {
        OperationLogResponse response = new OperationLogResponse();
        response.setSuccess(false);
        response.setMessage(message);
        response.setData(null);
        return response;
    }
    
    /**
     * 创建分页错误响应
     *
     * @param message 错误消息
     * @return 分页错误响应
     */
    private OperationLogPageResponse createErrorPageResponse(String message) {
        OperationLogPageResponse response = new OperationLogPageResponse();
        response.setSuccess(false);
        response.setMessage(message);
        response.setData(null);
        return response;
    }
    
    /**
     * 创建错误统计响应
     *
     * @param message 错误消息
     * @return 错误统计响应
     */
    private OperationLogStatisticsResponse createErrorStatisticsResponse(String message) {
        OperationLogStatisticsResponse response = new OperationLogStatisticsResponse();
        response.setSuccess(false);
        response.setMessage(message);
        response.setData(null);
        return response;
    }
}
