/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.controller.basicDataManage;

import com.huawei.cloududn.dialingtest.api.SoftwarePackagesApi;
import com.huawei.cloududn.dialingtest.model.*;
import com.huawei.cloududn.dialingtestapp.service.basicDataManage.SoftwarePackagesService;
import com.huawei.cloududn.dialingtestapp.service.basicDataManage.UserRoleService;
import com.huawei.cloududn.dialingtestapp.util.OperationLogUtil;
import com.huawei.cloududn.dialingtestapp.util.PermissionValidator;
import com.huawei.cloududn.dialingtestapp.util.PermissionValidator.PermissionValidationResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * 软件包管理控制器
 * 
 * @author g00940940
 * @since 2025-09-29
 */
@RestController
@RequestMapping("/api")
public class SoftwarePackageController implements SoftwarePackagesApi {
    
    private static final Logger logger = LoggerFactory.getLogger(SoftwarePackageController.class);
    
    @Autowired
    private SoftwarePackagesService softwarePackageService;
    
    @Autowired
    private UserRoleService userRoleService;
    
    @Autowired
    private OperationLogUtil operationLogUtil;
    
    @Autowired
    private PermissionValidator permissionValidator;
    
    @Override
    public ResponseEntity<SoftwarePackageListResponse> getSoftwarePackages(Integer page, Integer pageSize, String keyword) {
        try {
            // 设置默认值
            if (page == null) page = 1;
            if (pageSize == null) pageSize = 10;
            
            SoftwarePackageListResponseData data = softwarePackageService.getSoftwarePackageList(page, pageSize, keyword);
            
            SoftwarePackageListResponse response = new SoftwarePackageListResponse();
            response.setSuccess(true);
            response.setMessage("获取软件包列表成功");
            response.setData(data);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            SoftwarePackageListResponse errorResponse = new SoftwarePackageListResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("获取软件包列表失败: " + e.getMessage());
            errorResponse.setData(null);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @Override
    public ResponseEntity<SoftwarePackagePayload> getSoftwarePackageById(Long id) {
        try {
            SoftwarePackageInfo packageInfo = checkPackageExists(id);
            if (packageInfo == null) {
                return createSoftwarePackagePayloadError("软件包不存在", HttpStatus.NOT_FOUND);
            }
            
            SoftwarePackagePayload response = new SoftwarePackagePayload();
            response.setSuccess(true);
            response.setMessage("获取软件包详情成功");
            response.setData(packageInfo);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return createSoftwarePackagePayloadError("获取软件包详情失败: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @Override
    public ResponseEntity<SoftwarePackagePayload> updateSoftwarePackage(
            String xUsername, Long id, UpdateSoftwarePackageBody body) {
        try {
            // 检查权限（需要ADMIN或OPERATOR权限）
            PermissionValidationResult permissionResult = 
                    permissionValidator.checkAdminOrOperator(xUsername, "更新软件包信息");
            if (!permissionResult.isValid()) {
                SoftwarePackagePayload response = new SoftwarePackagePayload();
                response.setSuccess(false);
                response.setMessage(permissionResult.getErrorMessage());
                HttpStatus status = permissionResult.getErrorMessage().contains("未提供用户名") 
                    ? HttpStatus.BAD_REQUEST : HttpStatus.FORBIDDEN;
                return ResponseEntity.status(status).body(response);
            }
            
            // 检查软件包是否存在
            SoftwarePackageInfo oldPackageInfo = checkPackageExists(id);
            if (oldPackageInfo == null) {
                return createSoftwarePackagePayloadError("软件包不存在", HttpStatus.NOT_FOUND);
            }
            
            SoftwarePackageInfo newPackageInfo = softwarePackageService.updateSoftwarePackage(id, body.getDescription());
            if (newPackageInfo == null) {
                return createSoftwarePackagePayloadError("软件包不存在", HttpStatus.NOT_FOUND);
            }
            
            // 记录操作日志
            operationLogUtil.logSoftwarePackageUpdate(xUsername, oldPackageInfo, newPackageInfo);
            
            SoftwarePackagePayload response = new SoftwarePackagePayload();
            response.setSuccess(true);
            response.setMessage("更新软件包成功");
            response.setData(newPackageInfo);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return createSoftwarePackagePayloadError("更新软件包失败: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @Override
    public ResponseEntity<SuccessResponse> deleteSoftwarePackage(Long id, String xUsername) {
        try {
            // 检查权限（需要ADMIN或OPERATOR权限）
            PermissionValidationResult permissionResult = 
                    permissionValidator.checkAdminOrOperator(xUsername, "删除软件包");
            if (!permissionResult.isValid()) {
                SuccessResponse response = new SuccessResponse();
                response.setSuccess(false);
                response.setMessage(permissionResult.getErrorMessage());
                HttpStatus status = permissionResult.getErrorMessage().contains("未提供用户名") 
                    ? HttpStatus.BAD_REQUEST : HttpStatus.FORBIDDEN;
                return ResponseEntity.status(status).body(response);
            }
            
            // 检查软件包是否存在
            SoftwarePackageInfo packageInfo = checkPackageExists(id);
            if (packageInfo == null) {
                return createSuccessResponseError("软件包不存在", HttpStatus.NOT_FOUND);
            }
            
            // 检查是否被引用
            if (softwarePackageService.isReferencedByTestCaseSet(id)) {
                return createSuccessResponseError("软件包被测试用例集引用，无法删除", HttpStatus.BAD_REQUEST);
            }
            
            boolean deleted = softwarePackageService.deleteSoftwarePackage(id);
            if (!deleted) {
                return createSuccessResponseError("删除软件包失败", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            
            // 记录操作日志
            operationLogUtil.logSoftwarePackageDelete(xUsername, packageInfo);
            
            SuccessResponse response = new SuccessResponse();
            response.setSuccess(true);
            response.setMessage("删除软件包成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return createSuccessResponseError("删除软件包失败: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @Override
    public ResponseEntity<Resource> downloadSoftwarePackages(
            @RequestHeader("X-Csrf-Token") String xCsrfToken,
            @RequestHeader("X-Username") String xUsername,
            BatchDownloadRequest body) {
        try {
            // 检查权限（需要ADMIN或OPERATOR权限）
            PermissionValidationResult permissionResult = 
                    permissionValidator.checkAdminOrOperator(xUsername, "下载软件包");
            if (!permissionResult.isValid()) {
                HttpStatus status = permissionResult.getErrorMessage().contains("未提供用户名") 
                    ? HttpStatus.BAD_REQUEST : HttpStatus.FORBIDDEN;
                return ResponseEntity.status(status).body(null);
            }
            
            List<Long> packageIds = body.getPackageIds();
            if (packageIds == null || packageIds.isEmpty()) {
                logger.warn("Empty package IDs list for download request by user: {}", xUsername);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }
            
            String zipFileName = body.getZipFileName();
            if (zipFileName == null || zipFileName.trim().isEmpty()) {
                zipFileName = "software_packages_batch";
            }
            
            // 调用服务层处理批量下载
            Resource resource = softwarePackageService.downloadSoftwarePackages(packageIds);
            if (resource == null) {
                logger.warn("No software packages found for download request by user: {}", xUsername);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            
            HttpHeaders headers = new HttpHeaders();
            if (packageIds.size() == 1) {
                // 单个文件下载
                SoftwarePackageInfo packageInfo = softwarePackageService.getSoftwarePackageById(packageIds.get(0));
                if (packageInfo != null) {
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + packageInfo.getSoftwareName() + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
                }
                logger.info("Software package single download by user: {}, package ID: {}", xUsername, packageIds.get(0));
            } else {
                // 批量下载，返回ZIP文件
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipFileName + ".zip\"");
                headers.add(HttpHeaders.CONTENT_TYPE, "application/zip");
                logger.info("Software packages batch download by user: {}, count: {}, zip file: {}.zip", 
                           xUsername, packageIds.size(), zipFileName);
            }
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(resource.contentLength())
                    .body(resource);
        } catch (Exception e) {
            logger.error("Software package download failed for user: {}", xUsername, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 创建SoftwarePackagePayload错误响应
     *
     * @param message 错误消息
     * @param status HTTP状态码
     * @return ResponseEntity<SoftwarePackagePayload>
     */
    private ResponseEntity<SoftwarePackagePayload> createSoftwarePackagePayloadError(String message, HttpStatus status) {
        SoftwarePackagePayload errorResponse = new SoftwarePackagePayload();
        errorResponse.setSuccess(false);
        errorResponse.setMessage(message);
        errorResponse.setData(null);
        
        return ResponseEntity.status(status).body(errorResponse);
    }
    
    /**
     * 创建SuccessResponse错误响应
     *
     * @param message 错误消息
     * @param status HTTP状态码
     * @return ResponseEntity<SuccessResponse>
     */
    private ResponseEntity<SuccessResponse> createSuccessResponseError(String message, HttpStatus status) {
        SuccessResponse errorResponse = new SuccessResponse();
        errorResponse.setSuccess(false);
        errorResponse.setMessage(message);
        
        return ResponseEntity.status(status).body(errorResponse);
    }
    
    /**
     * 检查软件包是否存在
     *
     * @param id 软件包ID
     * @return 软件包信息，null表示不存在
     */
    private SoftwarePackageInfo checkPackageExists(Long id) {
        return softwarePackageService.getSoftwarePackageById(id);
    }
}
