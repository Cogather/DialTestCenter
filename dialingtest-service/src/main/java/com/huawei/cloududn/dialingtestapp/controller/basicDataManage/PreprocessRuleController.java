/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.controller.basicDataManage;

import com.huawei.cloududn.dialingtest.api.PreprocessRulePackagesApi;
import com.huawei.cloududn.dialingtest.api.PreprocessRulesApi;
import com.huawei.cloududn.dialingtest.model.*;
import com.huawei.cloududn.dialingtestapp.service.basicDataManage.PreprocessRuleService;
import com.huawei.cloududn.dialingtestapp.service.basicDataManage.UserRoleService;
import com.huawei.cloududn.dialingtestapp.util.OperationLogUtil;
import com.huawei.cloududn.dialingtestapp.util.PermissionValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 预处理规则管理控制器
 * 
 * @author g00940940
 * @since 2025-01-27
 */
@RestController
@RequestMapping("/api")
public class PreprocessRuleController implements PreprocessRulePackagesApi, PreprocessRulesApi {
    
    @Autowired
    private PreprocessRuleService preprocessRuleService;
    
    @Autowired
    private OperationLogUtil operationLogUtil;
    
    @Autowired
    private UserRoleService userRoleService;
    
    @Autowired
    private PermissionValidator permissionValidator;
    
    // ==================== ZIP包管理接口 ====================
    
    @Override
    public ResponseEntity<PreprocessRulePackageListResponse> getPreprocessRulePackages(
            Integer page, Integer pageSize, String businessZh, String keyword) {
        try {
            // 设置默认值
            if (page == null) page = 1;
            if (pageSize == null) pageSize = 10;
            
            PreprocessRulePackageListResponse response = preprocessRuleService.getPreprocessRulePackages(
                page, pageSize, businessZh, keyword);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse(PreprocessRulePackageListResponse.class, "获取ZIP包列表失败: " + e.getMessage());
        }
    }
    
    @Override
    public ResponseEntity<SuccessResponse> deletePreprocessRulePackage(Long id, String xUsername) {
        try {
            // 检查权限（需要ADMIN或OPERATOR权限）
            PermissionValidator.PermissionValidationResult permissionResult = 
                permissionValidator.checkAdminOrOperator(xUsername, "删除预处理规则包");
            if (!permissionResult.isValid()) {
                HttpStatus status = permissionResult.getErrorMessage().contains("未提供用户名") 
                    ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN;
                return createSuccessResponseError(permissionResult.getErrorMessage(), status);
            }
            
            preprocessRuleService.deletePreprocessRulePackage(id, xUsername);
            
            SuccessResponse response = new SuccessResponse();
            response.setSuccess(true);
            response.setMessage("删除ZIP包成功");
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return createSuccessResponseError(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return createSuccessResponseError("删除ZIP包失败: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @Override
    public ResponseEntity<Resource> downloadPreprocessRulePackage(Long id) {
        try {
            byte[] fileContent = preprocessRuleService.downloadPreprocessRulePackage(id);
            
            ByteArrayResource resource = new ByteArrayResource(fileContent);
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=preprocess_rule_package_" + id + ".zip")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(fileContent.length)
                .body(resource);
        } catch (IllegalArgumentException e) {
            return createResourceError(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return createResourceError(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // ==================== 规则管理接口 ====================
    
    @Override
    public ResponseEntity<PreprocessRuleListResponse> getPreprocessRules(
            Integer page, Integer pageSize, String businessZh, String category, 
            String appName, String ruleName, String keyword) {
        try {
            // 设置默认值
            if (page == null) page = 1;
            if (pageSize == null) pageSize = 10;
            
            PreprocessRuleListResponse response = preprocessRuleService.getPreprocessRules(
                page, pageSize, businessZh, category, appName, ruleName, keyword);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse(PreprocessRuleListResponse.class, "获取预处理规则列表失败: " + e.getMessage());
        }
    }
    
    // ==================== 筛选选项接口 ====================
    
    @Override
    public ResponseEntity<BusinessTypeListResponse> getBusinessTypes() {
        try {
            BusinessTypeListResponse response = preprocessRuleService.getAllBusinessTypes();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse(BusinessTypeListResponse.class, "获取业务类型列表失败: " + e.getMessage());
        }
    }
    
    @Override
    public ResponseEntity<CategoryListResponse> getCategories(String businessZh) {
        try {
            CategoryListResponse response = preprocessRuleService.getCategoriesByBusiness(businessZh);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse(CategoryListResponse.class, "获取分类列表失败: " + e.getMessage());
        }
    }
    
    @Override
    public ResponseEntity<AppNameListResponse> getAppNames(String businessZh, String category) {
        try {
            AppNameListResponse response = preprocessRuleService.getAppNamesByBusinessAndCategory(businessZh, category);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse(AppNameListResponse.class, "获取应用名称列表失败: " + e.getMessage());
        }
    }
    
    @Override
    public ResponseEntity<RuleNameListResponse> getRuleNames(
            String businessZh, String category, String appName) {
        try {
            RuleNameListResponse response = preprocessRuleService.getRuleNamesByBusinessAndCategoryAndApp(
                businessZh, category, appName);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse(RuleNameListResponse.class, "获取规则名称列表失败: " + e.getMessage());
        }
    }
    
    @Override
    public ResponseEntity<FilterOptionsResponse> getFilterOptions() {
        try {
            FilterOptionsResponse response = preprocessRuleService.getFilterOptions();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse(FilterOptionsResponse.class, "获取筛选选项失败: " + e.getMessage());
        }
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 创建通用错误响应
     *
     * @param responseClass 响应类型
     * @param errorMessage 错误消息
     * @param <T> 响应类型，必须包含setSuccess和setMessage方法
     * @return ResponseEntity<T>
     */
    private <T> ResponseEntity<T> createErrorResponse(Class<T> responseClass, String errorMessage) {
        try {
            T response = responseClass.getDeclaredConstructor().newInstance();
            if (response instanceof PreprocessRulePackageListResponse) {
                ((PreprocessRulePackageListResponse) response).setSuccess(false);
                ((PreprocessRulePackageListResponse) response).setMessage(errorMessage);
            } else if (response instanceof PreprocessRuleListResponse) {
                ((PreprocessRuleListResponse) response).setSuccess(false);
                ((PreprocessRuleListResponse) response).setMessage(errorMessage);
            } else if (response instanceof BusinessTypeListResponse) {
                ((BusinessTypeListResponse) response).setSuccess(false);
                ((BusinessTypeListResponse) response).setMessage(errorMessage);
            } else if (response instanceof CategoryListResponse) {
                ((CategoryListResponse) response).setSuccess(false);
                ((CategoryListResponse) response).setMessage(errorMessage);
            } else if (response instanceof AppNameListResponse) {
                ((AppNameListResponse) response).setSuccess(false);
                ((AppNameListResponse) response).setMessage(errorMessage);
            } else if (response instanceof RuleNameListResponse) {
                ((RuleNameListResponse) response).setSuccess(false);
                ((RuleNameListResponse) response).setMessage(errorMessage);
            } else if (response instanceof FilterOptionsResponse) {
                ((FilterOptionsResponse) response).setSuccess(false);
                ((FilterOptionsResponse) response).setMessage(errorMessage);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 创建SuccessResponse错误响应
     *
     * @param errorMessage 错误消息
     * @param status HTTP状态码
     * @return ResponseEntity<SuccessResponse>
     */
    private ResponseEntity<SuccessResponse> createSuccessResponseError(String errorMessage, HttpStatus status) {
        SuccessResponse response = new SuccessResponse();
        response.setSuccess(false);
        response.setMessage(errorMessage);
        return ResponseEntity.status(status).body(response);
    }
    
    /**
     * 创建Resource错误响应
     *
     * @param status HTTP状态码
     * @return ResponseEntity<Resource>
     */
    private ResponseEntity<Resource> createResourceError(HttpStatus status) {
        return ResponseEntity.status(status).build();
    }
}

