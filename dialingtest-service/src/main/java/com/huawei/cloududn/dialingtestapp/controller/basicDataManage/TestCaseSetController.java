/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.controller.basicDataManage;

import com.huawei.cloududn.dialingtest.api.TestCaseSetsApi;
import com.huawei.cloududn.dialingtestapp.entity.basicDataManage.ValidationTask;
import com.huawei.cloududn.dialingtest.model.*;
import com.huawei.cloududn.dialingtestapp.model.ValidationResult;
import com.huawei.cloududn.dialingtestapp.service.basicDataManage.TestCaseSetService;
import com.huawei.cloududn.dialingtestapp.service.basicDataManage.TestCaseValidationService;
import com.huawei.cloududn.dialingtestapp.service.basicDataManage.UserRoleService;
import com.huawei.cloududn.dialingtestapp.util.OperationLogUtil;
import com.huawei.cloududn.dialingtestapp.util.PermissionValidator;
import com.huawei.cloududn.dialingtestapp.util.PermissionValidator.PermissionValidationResult;
import com.huawei.cloududn.dialingtestapp.util.ExcelUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 拨测用例集管理控制器
 * 
 * @author g00940940
 * @since 2025-09-23
 */
@RestController
@RequestMapping("/api")
public class TestCaseSetController implements TestCaseSetsApi {
    
    private static final Logger logger = LoggerFactory.getLogger(TestCaseSetController.class);
    
    @Autowired
    private TestCaseSetService testCaseSetService;
    
    @Autowired
    private OperationLogUtil operationLogUtil;
    
    @Autowired
    private TestCaseValidationService testCaseValidationService;
    
    @Autowired
    private UserRoleService userRoleService;
    
    @Autowired
    private PermissionValidator permissionValidator;
    
    @Override
    public ResponseEntity<TestCaseSetListResponse> getTestCaseSets(Integer page, Integer pageSize) {
        try {
            // 设置默认值
            if (page == null) page = 1;
            if (pageSize == null) pageSize = 10;
            
            TestCaseSetListResponseData data = testCaseSetService.getTestCaseSets(page, pageSize);
            
            TestCaseSetListResponse response = new TestCaseSetListResponse();
            response.setSuccess(true);
            response.setMessage("获取用例集列表成功");
            response.setData(data);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            TestCaseSetListResponse errorResponse = new TestCaseSetListResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("获取用例集列表失败: " + e.getMessage());
            errorResponse.setData(null);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @Override
    public ResponseEntity<TestCaseSetResponse> getTestCaseSetById(Long id) {
        try {
            TestCaseSet testCaseSet = testCaseSetService.getTestCaseSetById(id);
            if (testCaseSet == null) {
                TestCaseSetResponse response = new TestCaseSetResponse();
                response.setSuccess(false);
                response.setMessage("用例集不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            TestCaseSetResponse response = new TestCaseSetResponse();
            response.setSuccess(true);
            response.setMessage("获取用例集详情成功");
            response.setData(testCaseSet);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            TestCaseSetResponse response = new TestCaseSetResponse();
            response.setSuccess(false);
            response.setMessage("获取用例集详情失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    
    @Override
    public ResponseEntity<Resource> downloadTestCaseSet(
            Long id, @RequestHeader(value = "X-Username", required = true) String xUsername) {
        try {
            // 检查权限（需要ADMIN或OPERATOR权限）
            PermissionValidationResult permissionResult = 
                    permissionValidator.checkAdminOrOperator(xUsername, "下载用例集");
            if (!permissionResult.isValid()) {
                logger.warn("用户权限不足 - username: {}", xUsername);
                HttpStatus status = permissionResult.getErrorMessage().contains("未提供用户名") 
                    ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN;
                return ResponseEntity.status(status).build();
            }
            
            TestCaseSet testCaseSet = testCaseSetService.getTestCaseSetById(id);
            if (testCaseSet == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            
            byte[] fileContent = testCaseSet.getFileContent();
            if (fileContent == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            
            ByteArrayResource resource = new ByteArrayResource(fileContent);
            
            HttpHeaders headers = new HttpHeaders();
            String filename = testCaseSet.getName() + "_" + testCaseSet.getVersion() + ".zip";
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, "application/zip");
            
            logger.info("User {} downloaded test case set: {}", xUsername, id);
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(fileContent.length)
                    .body(resource);
        } catch (Exception e) {
            logger.error("Failed to download test case set: testCaseSetId={}, username={}", id, xUsername, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Override
    public ResponseEntity<TestCaseSetResponse> updateTestCaseSet(
            String xUsername, Long id, UpdateTestCaseSetRequest body) {
        try {
            // 检查权限（需要ADMIN或OPERATOR权限）
            PermissionValidationResult permissionResult = 
                    permissionValidator.checkAdminOrOperator(xUsername, "更新用例集");
            if (!permissionResult.isValid()) {
                TestCaseSetResponse response = new TestCaseSetResponse();
                response.setSuccess(false);
                response.setMessage(permissionResult.getErrorMessage());
                HttpStatus status = permissionResult.getErrorMessage().contains("未提供用户名") 
                    ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN;
                return ResponseEntity.status(status).body(response);
            }
            
            String operatorUsername = (xUsername != null && !xUsername.trim().isEmpty()) ? xUsername : "admin";
            TestCaseSet testCaseSet = testCaseSetService.updateTestCaseSet(id, body, operatorUsername);
            if (testCaseSet == null) {
                TestCaseSetResponse response = new TestCaseSetResponse();
                response.setSuccess(false);
                response.setMessage("用例集不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            TestCaseSetResponse response = new TestCaseSetResponse();
            response.setSuccess(true);
            response.setMessage("更新用例集成功");
            response.setData(testCaseSet);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            TestCaseSetResponse response = new TestCaseSetResponse();
            response.setSuccess(false);
            response.setMessage("更新失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            TestCaseSetResponse response = new TestCaseSetResponse();
            response.setSuccess(false);
            response.setMessage("更新失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @Override
    public ResponseEntity<SuccessResponse> deleteTestCaseSet(Long id, String xUsername) {
        try {
            // 检查权限（需要ADMIN或OPERATOR权限）
            PermissionValidationResult permissionResult = 
                    permissionValidator.checkAdminOrOperator(xUsername, "删除用例集");
            if (!permissionResult.isValid()) {
                SuccessResponse response = new SuccessResponse();
                response.setSuccess(false);
                response.setMessage(permissionResult.getErrorMessage());
                HttpStatus status = permissionResult.getErrorMessage().contains("未提供用户名") 
                    ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN;
                return ResponseEntity.status(status).body(response);
            }
            
            String operatorUsername = (xUsername != null && !xUsername.trim().isEmpty()) ? xUsername : "admin";
            boolean deleted = testCaseSetService.deleteTestCaseSet(id, operatorUsername);
            if (!deleted) {
                SuccessResponse response = new SuccessResponse();
                response.setSuccess(false);
                response.setMessage("用例集不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            SuccessResponse response = new SuccessResponse();
            response.setSuccess(true);
            response.setMessage("删除用例集成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            SuccessResponse response = new SuccessResponse();
            response.setSuccess(false);
            response.setMessage("删除失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @Override
    public ResponseEntity<TestCaseListResponse> getTestCasesByTestCaseSetId(Long id, Integer page, Integer pageSize) {
        try {
            // 设置默认值
            if (page == null) page = 1;
            if (pageSize == null) pageSize = 10;
            
            TestCaseListResponseData data = testCaseSetService.getTestCases(id, page, pageSize);
            
            TestCaseListResponse response = new TestCaseListResponse();
            response.setSuccess(true);
            response.setMessage("获取测试用例列表成功");
            response.setData(data);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            TestCaseListResponse errorResponse = new TestCaseListResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("获取测试用例列表失败: " + e.getMessage());
            errorResponse.setData(null);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 触发用例集校验任务
     *
     * @param id 用例集ID
     * @param xCsrfToken CSRF防护令牌
     * @param xUsername 操作用户名
     * @return 校验任务响应
     */
    @Override
    public ResponseEntity<ValidationTaskResponse> triggerTestCaseSetValidation(
            Long id, String xCsrfToken, String xUsername) {
        logger.info("触发用例集校验任务 - testCaseSetId: {}, username: {}", id, xUsername);
        try {
            // 检查用户权限（ADMIN或OPERATOR）
            PermissionValidationResult permissionResult = 
                    permissionValidator.checkAdminOrOperator(xUsername, "触发用例集校验");
            if (!permissionResult.isValid()) {
                logger.warn("用户权限不足 - username: {}", xUsername);
                ValidationTaskResponse response = new ValidationTaskResponse();
                response.setSuccess(false);
                response.setMessage(permissionResult.getErrorMessage());
                HttpStatus status = permissionResult.getErrorMessage().contains("未提供用户名") 
                    ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN;
                return ResponseEntity.status(status).body(response);
            }
            
            // 触发校验任务
            logger.debug("开始调用 triggerValidation - testCaseSetId: {}", id);
            TestCaseValidationService.ValidationTaskInfo taskInfo = testCaseValidationService.triggerValidation(id);
            logger.info("校验任务已创建 - taskId: {}, testCaseSetId: {}", taskInfo.getTaskId(), id);
            
            // 获取用例集信息用于记录操作日志
            TestCaseSet testCaseSet = testCaseSetService.getTestCaseSetById(id);
            if (testCaseSet != null) {
                operationLogUtil.logTestCaseSetValidation(xUsername, testCaseSet);
            }
            
            // 返回202 Accepted表示任务已提交
            ValidationTaskResponse response = new ValidationTaskResponse();
            response.setSuccess(true);
            response.setMessage("校验任务已提交");
            
            ValidationTaskResponseData data = new ValidationTaskResponseData();
            data.setTaskId(taskInfo.getTaskId());
            data.setTestCaseSetId(taskInfo.getTestCaseSetId());
            data.setStatus(taskInfo.getStatus());
            data.setEstimatedTime(taskInfo.getEstimatedTime());
            response.setData(data);
            
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("触发校验任务失败 - testCaseSetId: {}, 错误: {}", id, e.getMessage(), e);
            ValidationTaskResponse response = new ValidationTaskResponse();
            response.setSuccess(false);
            response.setMessage("用例集不存在");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            logger.error("触发校验任务失败 - testCaseSetId: {}, username: {}, 异常类型: {}, 错误信息: {}", 
                    id, xUsername, e.getClass().getName(), e.getMessage(), e);
            ValidationTaskResponse response = new ValidationTaskResponse();
            response.setSuccess(false);
            response.setMessage("触发校验任务失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 获取用例集校验结果
     *
     * @param id 用例集ID
     * @param xCsrfToken CSRF防护令牌（可选）
     * @param forceRefresh 是否强制刷新
     * @return 校验结果响应
     */
    @Override
    public ResponseEntity<ValidationResponse> getTestCaseSetValidation(Long id, String xCsrfToken, Boolean forceRefresh) {
        try {
            ValidationResult serviceResult = getValidationResult(id, forceRefresh);
            
            if (serviceResult == null) {
                return handleTaskInProgress(id);
            }
            
            ValidationResponse response = buildValidationResponse(serviceResult, id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return createNotFoundResponse("用例集不存在");
        } catch (Exception e) {
            return createInternalServerErrorResponse("获取校验结果失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取校验结果
     *
     * @param id 用例集ID
     * @param forceRefresh 是否强制刷新
     * @return 校验结果
     */
    private ValidationResult getValidationResult(Long id, Boolean forceRefresh) {
        if (forceRefresh != null && forceRefresh) {
            return testCaseValidationService.validateTestCaseSet(id);
        } else {
            return testCaseValidationService.getValidationResult(id);
        }
    }
    
    /**
     * 处理任务执行中的情况
     *
     * @param id 用例集ID
     * @return 响应实体
     */
    private ResponseEntity<ValidationResponse> handleTaskInProgress(Long id) {
        ValidationTask task = testCaseValidationService.getTaskStatus(id);
        if (task == null) {
            return createNotFoundResponse("校验结果不存在，请先触发校验");
        }
        
        ValidationResponse response = new ValidationResponse();
        response.setSuccess(true);
        response.setMessage("校验任务正在执行中，状态: " + task.getStatus());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 构建校验响应
     *
     * @param serviceResult 服务层校验结果
     * @param id 用例集ID
     * @return 校验响应
     */
    private ValidationResponse buildValidationResponse(
            ValidationResult serviceResult, Long id) {
        ValidationResponse response = new ValidationResponse();
        response.setSuccess(true);
        response.setMessage("获取校验结果成功");
        
        ValidationResponseData data = buildValidationResponseData(serviceResult, id);
        response.setData(data);
        
        return response;
    }
    
    /**
     * 构建校验响应数据
     *
     * @param serviceResult 服务层校验结果
     * @param id 用例集ID
     * @return 校验响应数据
     */
    private ValidationResponseData buildValidationResponseData(
            ValidationResult serviceResult, Long id) {
        ValidationResponseData data = new ValidationResponseData();
        
        data.setTestCaseSetId(serviceResult.getTestCaseSetId());
        data.setTestCaseSetName(serviceResult.getTestCaseSetName());
        data.setTestCaseSetVersion(serviceResult.getTestCaseSetVersion());
        data.setBusinessZh(serviceResult.getBusinessZh());
        data.setBusinessEn(serviceResult.getBusinessEn());
        data.setTotalCaseCount(serviceResult.getTotalCaseCount());
        data.setPassedCaseCount(serviceResult.getPassedCaseCount());
        data.setFailedCaseCount(serviceResult.getFailedCaseCount());
        data.setMatchRate(serviceResult.getMatchRate());
        
        setTaskStatusInfo(data, id);
        data.setCaseResults(convertCaseResults(serviceResult.getCaseResults()));
        
        return data;
    }
    
    /**
     * 设置任务状态信息
     *
     * @param data 校验响应数据
     * @param id 用例集ID
     */
    private void setTaskStatusInfo(ValidationResponseData data, Long id) {
        ValidationTask task = testCaseValidationService.getTaskStatus(id);
        if (task != null) {
            data.setValidationTaskStatus(task.getStatus());
            if (task.getCreatedTime() != null) {
                data.setValidationTaskCreatedTime(task.getCreatedTime().toString());
            }
            if (task.getStartedTime() != null) {
                data.setValidationTaskStartedTime(task.getStartedTime().toString());
            }
            if (task.getCompletedTime() != null) {
                data.setValidationTaskCompletedTime(task.getCompletedTime().toString());
            }
        }
    }
    
    /**
     * 转换用例结果列表
     *
     * @param serviceCaseResults 服务层用例结果列表
     * @return API用例结果列表
     */
    private List<CaseValidationResult> convertCaseResults(
            List<CaseValidationResult> serviceCaseResults) {
        List<CaseValidationResult> caseResults = new ArrayList<>();
        for (CaseValidationResult caseResult : serviceCaseResults) {
            CaseValidationResult apiCaseResult = convertSingleCaseResult(caseResult);
            caseResults.add(apiCaseResult);
        }
        return caseResults;
    }
    
    /**
     * 转换单个用例结果
     *
     * @param caseResult 服务层用例结果
     * @return API用例结果
     */
    private CaseValidationResult convertSingleCaseResult(CaseValidationResult caseResult) {
        CaseValidationResult apiCaseResult = new CaseValidationResult();
        apiCaseResult.setCaseNumber(caseResult.getCaseNumber());
        apiCaseResult.setCaseName(caseResult.getCaseName());
        apiCaseResult.setBusinessCategory(caseResult.getBusinessCategory());
        apiCaseResult.setAppName(caseResult.getAppName());
        apiCaseResult.setScriptMatchValid(caseResult.isScriptMatchValid());
        apiCaseResult.setPreprocessRuleValid(caseResult.isPreprocessRuleValid());
        apiCaseResult.setSoftwarePackageValid(caseResult.isSoftwarePackageValid());
        apiCaseResult.setOverallValid(caseResult.isOverallValid());
        apiCaseResult.setValidReasons(caseResult.getValidReasons());
        apiCaseResult.setInvalidReasons(caseResult.getInvalidReasons());
        return apiCaseResult;
    }
    
    /**
     * 创建未找到响应
     *
     * @param message 错误消息
     * @return 响应实体
     */
    private ResponseEntity<ValidationResponse> createNotFoundResponse(String message) {
        ValidationResponse response = new ValidationResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    /**
     * 创建内部服务器错误响应
     *
     * @param message 错误消息
     * @return 响应实体
     */
    private ResponseEntity<ValidationResponse> createInternalServerErrorResponse(String message) {
        ValidationResponse response = new ValidationResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * 导出用例集校验结果Excel
     *
     * @param id 用例集ID
     * @param xCsrfToken CSRF防护令牌（可选）
     * @param xUsername 操作用户名
     * @return Excel文件资源
     */
    @Override
    public ResponseEntity<Resource> exportTestCaseSetValidation(
            Long id, String xCsrfToken,
            @RequestHeader(value = "X-Username", required = true) String xUsername) {
        try {
            // 检查权限（需要ADMIN或OPERATOR权限）
            PermissionValidationResult permissionResult = 
                    permissionValidator.checkAdminOrOperator(xUsername, "导出用例集校验结果");
            if (!permissionResult.isValid()) {
                logger.warn("用户权限不足 - username: {}", xUsername);
                HttpStatus status = permissionResult.getErrorMessage().contains("未提供用户名") 
                    ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN;
                return ResponseEntity.status(status).build();
            }
            
            // 获取校验结果
            ValidationResult serviceResult =
                testCaseValidationService.getValidationResult(id);
            
            if (serviceResult == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            
            // 生成Excel文件
            Resource resource = ExcelUtil.generateValidationResultExcel(serviceResult);
            
            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            // 生成文件名：用例集名称_校验结果_当前时间戳.xlsx
            String testCaseSetName = serviceResult.getTestCaseSetName() != null ? serviceResult.getTestCaseSetName() : "用例集";
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("%s_校验结果_%s.xlsx", testCaseSetName, timestamp);
            headers.setContentDispositionFormData("attachment", filename);
            
            logger.info("User {} exported validation result Excel for test case set: {}", xUsername, id);
            return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
                
        } catch (IllegalArgumentException e) {
            logger.error("Test case set not found: {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            logger.error("Failed to export validation result Excel: testCaseSetId={}, username={}", id, xUsername, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}