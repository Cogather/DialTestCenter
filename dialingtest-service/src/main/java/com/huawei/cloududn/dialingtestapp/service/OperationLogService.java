/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.service;

import com.huawei.cloududn.dialingtestapp.dao.OperationLogDao;
import com.huawei.cloududn.dialingtest.model.CreateOperationLogRequest;
import com.huawei.cloududn.dialingtest.model.OperationLog;
import com.huawei.cloududn.dialingtest.model.OperationLogPageResponse;
import com.huawei.cloududn.dialingtest.model.OperationLogPageResponseData;
import com.huawei.cloududn.dialingtest.model.OperationLogResponse;
import com.huawei.cloududn.dialingtest.model.OperationLogStatistics;
import com.huawei.cloududn.dialingtest.model.OperationLogStatisticsResponse;
import com.huawei.cloududn.dialingtestapp.util.ExcelUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 操作记录服务类，提供操作记录的业务逻辑处理
 * 包括操作记录的创建、查询、统计、导出等功能
 *
 * @author g00940940
 * @since 2025-01-15
 */
@Service
public class OperationLogService {
    private static final Logger logger = LoggerFactory.getLogger(OperationLogService.class);
    
    @Autowired
    private OperationLogDao operationLogDao;
    
    /**
     * 分页查询操作记录
     *
     * @param page 页码
     * @param size 每页大小
     * @param username 用户名筛选
     * @param operationType 操作类型筛选
     * @param operationTarget 操作目标筛选
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 分页响应
     */
    public OperationLogPageResponse getOperationLogs(
            Integer page, Integer size, String username, String operationType, 
            String operationTarget, String startTime, String endTime) {
        
        logger.debug("Querying operation logs with parameters: page={}, size={}, username={}", 
                    page, size, username);
        
        // 参数验证
        if (page == null || page < 0) {
            page = 0;
        }
        if (size == null || size <= 0) {
            size = 20;
        }
        
        // 查询数据
        List<OperationLog> logs = operationLogDao.findOperationLogsWithPagination(
            page, size, username, operationType, operationTarget, startTime, endTime);
        
        // 查询总数
        Long totalElements = operationLogDao.countOperationLogs(
            username, operationType, operationTarget, startTime, endTime);
        
        // 构建响应
        OperationLogPageResponse response = new OperationLogPageResponse();
        response.setSuccess(true);
        response.setMessage("查询成功");
        
        OperationLogPageResponseData data = new OperationLogPageResponseData();
        data.setContent(logs);
        data.setTotalElements(totalElements.intValue());
        data.setTotalPages((int) Math.ceil((double) totalElements / size));
        data.setSize(size);
        data.setNumber(page);
        
        response.setData(data);
        
        logger.debug("Successfully queried {} operation logs", logs.size());
        return response;
    }
    
    /**
     * 创建操作记录
     *
     * @param request 创建请求
     * @return 操作记录响应
     */
    public OperationLogResponse createOperationLog(CreateOperationLogRequest request) {
        logger.debug("Creating operation log for user: {}", request.getUsername());
        
        // 参数验证
        validateCreateRequest(request);
        
        // 构建操作记录对象
        OperationLog operationLog = new OperationLog();
        operationLog.setUsername(request.getUsername());
        operationLog.setOperationType(request.getOperationType());
        operationLog.setOperationTarget(request.getOperationTarget());
        operationLog.setOperationDescriptionZh(request.getOperationDescriptionZh());
        operationLog.setOperationDescriptionEn(request.getOperationDescriptionEn());
        operationLog.setOperationData(request.getOperationData());
        // 使用数据库默认时间戳，避免精度问题
        // operationLog.setOperationTime(ZonedDateTime.now(ZoneId.of("GMT+8"))
        //         .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")));
        
        // 保存到数据库
        int result = operationLogDao.save(operationLog);
        
        if (result <= 0) {
            throw new IllegalStateException("创建操作记录失败，数据库操作未生效");
        }
        
        // 构建响应
        OperationLogResponse response = new OperationLogResponse();
        response.setSuccess(true);
        response.setMessage("创建成功");
        response.setData(operationLog);
        
        logger.info("Successfully created operation log with ID: {}", operationLog.getId());
        return response;
    }
    
    /**
     * 根据ID查询操作记录详情
     *
     * @param id 操作记录ID
     * @return 操作记录响应
     */
    public OperationLogResponse getOperationLogById(Integer id) {
        logger.debug("Querying operation log details for ID: {}", id);
        
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid operation log ID");
        }
        
        OperationLog operationLog = operationLogDao.findById(id);
        
        if (operationLog == null) {
            throw new IllegalArgumentException("Operation log not found");
        }
        
        OperationLogResponse response = new OperationLogResponse();
        response.setSuccess(true);
        response.setMessage("查询成功");
        response.setData(operationLog);
        
        logger.debug("Successfully queried operation log details for ID: {}", id);
        return response;
    }
    
    /**
     * 获取操作记录统计信息
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计信息响应
     */
    public OperationLogStatisticsResponse getOperationLogStatistics(String startTime, String endTime) {
        logger.debug("Querying operation log statistics from {} to {}", startTime, endTime);
        
        OperationLogStatistics statistics = operationLogDao.getStatistics(startTime, endTime);
        
        OperationLogStatisticsResponse response = new OperationLogStatisticsResponse();
        response.setSuccess(true);
        response.setMessage("查询成功");
        response.setData(statistics);
        
        logger.debug("Successfully queried operation log statistics");
        return response;
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
    public Resource exportOperationLogs(
            String username, String operationType, String operationTarget, 
            String startTime, String endTime) {
        
        logger.debug("Exporting operation logs with filters");
        
        try {
            // 查询数据
            List<OperationLog> logs = operationLogDao.findOperationLogsForExport(
                username, operationType, operationTarget, startTime, endTime);
            
            // 生成Excel文件
            Resource resource = ExcelUtil.generateOperationLogsExcel(logs);
            
            logger.info("Successfully exported {} operation logs", logs.size());
            return resource;
            
        } catch (Exception e) {
            logger.error("Failed to export operation logs", e);
            return null;
        }
    }
    
    /**
     * 验证创建请求参数
     *
     * @param request 创建请求
     */
    private void validateCreateRequest(CreateOperationLogRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (request.getOperationType() == null || request.getOperationType().trim().isEmpty()) {
            throw new IllegalArgumentException("Operation type is required");
        }
        if (request.getOperationTarget() == null || request.getOperationTarget().trim().isEmpty()) {
            throw new IllegalArgumentException("Operation target is required");
        }
    }
}
