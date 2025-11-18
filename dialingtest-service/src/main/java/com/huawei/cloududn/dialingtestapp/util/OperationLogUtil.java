/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.util;

import com.huawei.cloududn.dialingtestapp.entity.SoftwarePackage;
import com.huawei.cloududn.dialingtest.model.CreateOperationLogRequest;
import com.huawei.cloududn.dialingtest.model.DialUser;
import com.huawei.cloududn.dialingtest.model.SoftwarePackageInfo;
import com.huawei.cloududn.dialingtest.model.TestCaseSet;
import com.huawei.cloududn.dialingtest.model.UserRole;
import com.huawei.cloududn.dialingtestapp.service.OperationLogService;

import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * 操作记录工具类
 * 提供统一的操作日志记录功能
 *
 * @author g00940940
 * @since 2025-01-15
 */
@Component
public class OperationLogUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(OperationLogUtil.class);
    
    @Autowired
    private OperationLogService operationLogService;
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ObjectWriter objectWriter = objectMapper.writer();
    
    /**
     * 将对象转换为JSON字符串
     *
     * @param obj 要转换的对象
     * @return JSON字符串
     */
    private String objectToJson(Object obj) {
        try {
            return objectWriter.writeValueAsString(obj);
        } catch (Exception e) {
            logger.warn("Failed to convert object to JSON: {}", e.getMessage());
            return "{}";
        }
    }
    
    /**
     * 通用的操作记录方法
     *
     * @param operatorUsername 操作用户名
     * @param operationType 操作类型
     * @param operationTarget 操作目标
     * @param descriptionZh 中文描述
     * @param descriptionEn 英文描述
     * @param operationDataBuilder 操作数据构建器函数
     * @param debugMessage 调试日志消息
     */
    private void logOperation(String operatorUsername, String operationType, String operationTarget,
                             String descriptionZh, String descriptionEn,
                             Function<OperationDataBuilder, OperationDataBuilder> operationDataBuilder,
                             String debugMessage) {
        try {
            CreateOperationLogRequest request = new CreateOperationLogRequest();
            request.setUsername(operatorUsername);
            request.setOperationType(operationType);
            request.setOperationTarget(operationTarget);
            request.setOperationDescriptionZh(descriptionZh);
            request.setOperationDescriptionEn(descriptionEn);
            
            Map<String, Object> operationData = operationDataBuilder.apply(new OperationDataBuilder()).build();
            request.setOperationData(objectToJson(operationData));
            
            operationLogService.createOperationLog(request);
            logger.debug(debugMessage);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid parameters for {} operation: {}", operationType, e.getMessage());
        } catch (RuntimeException e) {
            logger.warn("Failed to log {} operation: {}", operationType, e.getMessage());
        }
    }
    
    

      /**
     * 记录用户创建操作
     *
     * @param operatorUsername 操作用户名
     * @param targetUsername 目标用户名
     * @param userDetails 用户详细信息对象
     */
    public void logUserCreate(String operatorUsername, String targetUsername, DialUser userDetails) {
        logOperation(operatorUsername, "CREATE", "USER",
            "创建用户: " + targetUsername,
            "Create user: " + targetUsername,
            builder -> builder.userCreate(userDetails),
            "Logged user create operation for user: " + targetUsername);
    }
    

    
    /**
     * 记录用户更新操作
     *
     * @param operatorUsername 操作用户名
     * @param targetUsername 目标用户名
     * @param oldValues 更新前的DialUser对象
     * @param newValues 更新后的DialUser对象
     */
    public void logUserUpdate(String operatorUsername, String targetUsername, DialUser oldValues, DialUser newValues) {
        logOperation(operatorUsername, "UPDATE", "USER",
            "更新用户: " + targetUsername,
            "Update user: " + targetUsername,
            builder -> builder.userUpdate(oldValues, newValues),
            "Logged user update operation for user: " + targetUsername);
    }
    

    
    /**
     * 记录用户删除操作
     *
     * @param operatorUsername 操作用户名
     * @param targetUsername 目标用户名
     * @param userInfo 被删除用户的DialUser对象
     */
    public void logUserDelete(String operatorUsername, String targetUsername, DialUser userInfo) {
        logOperation(operatorUsername, "DELETE", "USER",
            "删除用户: " + targetUsername,
            "Delete user: " + targetUsername,
            builder -> builder.userDelete(userInfo),
            "Logged user delete operation for user: " + targetUsername);
    }
    
    
    /**
     * 记录用例集上传操作
     *
     * @param operatorUsername 操作用户名
     * @param testCaseSet 用例集信息
     */
    public void logTestCaseSetUpload(String operatorUsername, TestCaseSet testCaseSet) {
        if (testCaseSet == null) {
            logger.warn("TestCaseSet is null, skipping upload log");
            return;
        }
        
        String displayName = testCaseSet.getName() + "_" + testCaseSet.getVersion();
        logOperation(operatorUsername, OperationDataBuilder.OPERATION_CREATE, OperationDataBuilder.TARGET_TEST_CASE_SET,
                "上传用例集: " + displayName,
                "Upload test case set: " + displayName,
                builder -> builder
                        .add("id", testCaseSet.getId())
                        .add("name", testCaseSet.getName())
                        .add("version", testCaseSet.getVersion())
                        .add("description", testCaseSet.getDescription())
                        .add("businessZh", testCaseSet.getBusinessZh())
                        .add("businessEn", testCaseSet.getBusinessEn())
                        .add("fileSize", testCaseSet.getFileSize())
                        .add("sha256", testCaseSet.getSha256())
                        .withTimestamp(),
                "Logged test case set upload operation for: " + displayName);
    }
    
    /**
     * 记录用例集覆盖操作
     */
    public void logTestCaseSetOverwrite(String operatorUsername, TestCaseSet oldTestCaseSet, TestCaseSet newTestCaseSet) {
        if (oldTestCaseSet == null || newTestCaseSet == null) {
            logger.warn("TestCaseSet is null, skipping overwrite log");
            return;
        }
        
        String displayName = newTestCaseSet.getName() + "_" + newTestCaseSet.getVersion();
        logOperation(operatorUsername, OperationDataBuilder.OPERATION_OVERWRITE, OperationDataBuilder.TARGET_TEST_CASE_SET,
                "覆盖用例集: " + displayName,
                "Overwrite test case set: " + displayName,
                builder -> builder
                        .add("oldValues", new OperationDataBuilder()
                                .add("id", oldTestCaseSet.getId())
                                .add("name", oldTestCaseSet.getName())
                                .add("version", oldTestCaseSet.getVersion())
                                .add("description", oldTestCaseSet.getDescription())
                                .add("businessZh", oldTestCaseSet.getBusinessZh())
                                .add("businessEn", oldTestCaseSet.getBusinessEn())
                                .add("fileSize", oldTestCaseSet.getFileSize())
                                .add("sha256", oldTestCaseSet.getSha256())
                                .build())
                        .add("newValues", new OperationDataBuilder()
                                .add("id", newTestCaseSet.getId())
                                .add("name", newTestCaseSet.getName())
                                .add("version", newTestCaseSet.getVersion())
                                .add("description", newTestCaseSet.getDescription())
                                .add("businessZh", newTestCaseSet.getBusinessZh())
                                .add("businessEn", newTestCaseSet.getBusinessEn())
                                .add("fileSize", newTestCaseSet.getFileSize())
                                .add("sha256", newTestCaseSet.getSha256())
                                .build())
                        .withTimestamp(),
                "Logged test case set overwrite operation for: " + displayName);
    }
    
 /**
     * 记录用例集更新操作
     *
     * @param operatorUsername 操作用户名
     * @param oldValues 更新前的TestCaseSet对象
     * @param newValues 更新后的TestCaseSet对象
     */
    public void logTestCaseSetUpdate(String operatorUsername, TestCaseSet oldValues, TestCaseSet newValues) {
        logOperation(operatorUsername, "UPDATE", "TEST_CASE_SET",
            "更新用例集: " + newValues.getName() + " v" + newValues.getVersion(),
            "Update test case set: " + newValues.getName() + " v" + newValues.getVersion(),
            builder -> builder.testCaseSetUpdate(oldValues, newValues),
            "Logged test case set update: " + newValues.getName() + " v" + newValues.getVersion() + 
                    " by user: " + operatorUsername);
    }
    
    /**
     * 记录用例集删除操作
     *
     * @param operatorUsername 操作用户名
     * @param testCaseSet 用例集信息
     */
    public void logTestCaseSetDelete(String operatorUsername, TestCaseSet testCaseSet) {
        logOperation(operatorUsername, "DELETE", "TEST_CASE_SET",
            "删除用例集: " + testCaseSet.getName() + " v" + testCaseSet.getVersion(),
            "Delete test case set: " + testCaseSet.getName() + " v" + testCaseSet.getVersion(),
            builder -> builder.testCaseSetDelete(testCaseSet),
            "Logged test case set delete: " + testCaseSet.getName() + " v" + testCaseSet.getVersion() + 
                    " by user: " + operatorUsername);
    }
    
    /**
     * 记录用户角色创建操作
     *
     * @param operatorUsername 操作用户名
     * @param userRole 用户角色对象
     */
    public void logUserRoleCreate(String operatorUsername, UserRole userRole) {
        logOperation(operatorUsername, "CREATE", "USER_ROLE",
            "创建用户角色: " + userRole.getUsername() + " -> " + userRole.getRole(),
            "Create user role: " + userRole.getUsername() + " -> " + userRole.getRole(),
            builder -> builder.userRoleCreate(userRole),
            "Logged user role create operation for user: " + userRole.getUsername() + " with role: " + userRole.getRole());
    }
    
    /**
     * 记录用户角色更新操作
     *
     * @param operatorUsername 操作用户名
     * @param oldValues 更新前的UserRole对象
     * @param newValues 更新后的UserRole对象
     */
    public void logUserRoleUpdate(String operatorUsername, UserRole oldValues, UserRole newValues) {
        logOperation(operatorUsername, "UPDATE", "USER_ROLE",
            "更新用户角色: " + oldValues.getUsername() + " " + oldValues.getRole() + " -> " + newValues.getRole(),
            "Update user role: " + oldValues.getUsername() + " " + oldValues.getRole() + " -> " + newValues.getRole(),
            builder -> builder.userRoleUpdate(oldValues, newValues),
            "Logged user role update operation: " + oldValues + " -> " + newValues);
    }
    
    /**
   * 记录用户角色删除操作
   *
   * @param operatorUsername 操作用户名
   * @param userRole 被删除的用户角色对象
   */
  public void logUserRoleDelete(String operatorUsername, UserRole userRole) {
    logOperation(operatorUsername, "DELETE", "USER_ROLE",
        "删除用户角色: " + userRole.getUsername() + " -> " + userRole.getRole(),
        "Delete user role: " + userRole.getUsername() + " -> " + userRole.getRole(),
        builder -> builder.userRoleDelete(userRole),
        "Logged user role delete operation for user: " + userRole.getUsername() + " with role: " + userRole.getRole());
  }

  /**
   * 记录用户登录操作
   *
   * @param username 用户名
   */
  public void logUserLogin(String username) {
    logOperation(username, "LOGIN", "SYSTEM",
        "用户登录: " + username,
        "User login: " + username,
        builder -> builder.userLogin(username),
        "Logged user login operation for user: " + username);
  }

  /**
   * 记录软件包上传操作
   *
   * @param operatorUsername 操作用户名
   * @param softwarePackage 软件包信息
     */
    public void logSoftwarePackageCreate(String operatorUsername, SoftwarePackage softwarePackage) {
    logOperation(operatorUsername, "CREATE", "SOFTWARE_PACKAGE",
        "上传软件包: " + softwarePackage.getSoftwareName(),
        "Upload software package: " + softwarePackage.getSoftwareName(),
        builder -> builder.softwarePackageCreate(softwarePackage),
        "Logged software package upload: " + softwarePackage.getSoftwareName() + " by user: " + operatorUsername);
  }

  /**
   * 记录软件包批量上传操作
   *
   * @param operatorUsername 操作用户名
   * @param softwarePackages 软件包列表
   */
  public void logSoftwarePackageBatchCreate(String operatorUsername, java.util.List<SoftwarePackage> softwarePackages) {
    String packageNames = softwarePackages.stream()
        .map(SoftwarePackage::getSoftwareName)
        .reduce((a, b) -> a + ", " + b)
        .orElse("");
    
    logOperation(operatorUsername, "BATCH_CREATE", "SOFTWARE_PACKAGE",
        "批量上传软件包: " + packageNames + " (共" + softwarePackages.size() + "个)",
        "Batch upload software packages: " + packageNames + " (" + softwarePackages.size() + " packages)",
        builder -> builder.softwarePackageBatchCreate(softwarePackages),
        "Logged software package batch upload: " + softwarePackages.size() + " packages by user: " + operatorUsername);
    }
    
    /**
     * 记录软件包更新操作
   *
   * @param operatorUsername 操作用户名
   * @param oldPackage 更新前的软件包信息
   * @param newPackage 更新后的软件包信息
     */
  public void logSoftwarePackageUpdate(
          String operatorUsername, SoftwarePackageInfo oldPackage, SoftwarePackageInfo newPackage) {
    logOperation(operatorUsername, "UPDATE", "SOFTWARE_PACKAGE",
        "更新软件包: " + newPackage.getSoftwareName(),
        "Update software package: " + newPackage.getSoftwareName(),
        builder -> builder.softwarePackageUpdate(oldPackage, newPackage),
        "Logged software package update: " + newPackage.getSoftwareName() + " by user: " + operatorUsername);
    }
    
    /**
     * 记录软件包删除操作
   *
   * @param operatorUsername 操作用户名
   * @param softwarePackage 被删除的软件包信息
     */
  public void logSoftwarePackageDelete(String operatorUsername, SoftwarePackageInfo softwarePackage) {
    logOperation(operatorUsername, "DELETE", "SOFTWARE_PACKAGE",
        "删除软件包: " + softwarePackage.getSoftwareName(),
        "Delete software package: " + softwarePackage.getSoftwareName(),
        builder -> builder.softwarePackageDelete(softwarePackage),
        "Logged software package delete: " + softwarePackage.getSoftwareName() + " by user: " + operatorUsername);
  }
  
  /**
   * 记录用例集校验操作
   *
   * @param operatorUsername 操作用户名
   * @param testCaseSet 用例集信息
   */
  public void logTestCaseSetValidation(String operatorUsername, TestCaseSet testCaseSet) {
    logOperation(operatorUsername, "VALIDATE", "TEST_CASE_SET",
        "校验用例集: " + testCaseSet.getName() + " v" + testCaseSet.getVersion(),
        "Validate test case set: " + testCaseSet.getName() + " v" + testCaseSet.getVersion(),
        builder -> builder.testCaseSetValidation(testCaseSet),
        "Logged test case set validation: " + testCaseSet.getName() + " v" + testCaseSet.getVersion() + 
                " by user: " + operatorUsername);
    }
    
    /**
     * 记录软件包覆盖操作
   *
   * @param operatorUsername 操作用户名
   * @param oldPackage 被覆盖的软件包信息
   * @param newPackage 新的软件包信息
     */
  public void logSoftwarePackageOverwrite(
          String operatorUsername, SoftwarePackageInfo oldPackage, SoftwarePackage newPackage) {
    logOperation(operatorUsername, "OVERWRITE", "SOFTWARE_PACKAGE",
        "覆盖软件包: " + newPackage.getSoftwareName(),
        "Overwrite software package: " + newPackage.getSoftwareName(),
        builder -> builder.softwarePackageOverwrite(oldPackage, newPackage),
        "Logged software package overwrite: " + newPackage.getSoftwareName() + " by user: " + operatorUsername);
    }
    
    /**
     * 记录预处理规则ZIP包上传操作
     */
  public void logPreprocessRulePackageUpload(
          String operatorUsername, String packageName, String businessZh, String businessEn) {
    logOperation(operatorUsername, "UPLOAD", "PREPROCESS_RULE_PACKAGE",
        "上传预处理规则ZIP包: " + packageName + " (业务类型: " + businessZh + ")",
        "Upload preprocess rule package: " + packageName + " (Business: " + businessEn + ")",
        builder -> builder.preprocessRulePackageUpload(packageName, businessZh, businessEn),
        "Logged preprocess rule package upload: " + packageName + " by user: " + operatorUsername);
    }
    
    /**
     * 记录预处理规则ZIP包删除操作
     */
    public void logPreprocessRulePackageDelete(String operatorUsername, String packageName) {
    logOperation(operatorUsername, "DELETE", "PREPROCESS_RULE_PACKAGE",
        "删除预处理规则ZIP包: " + packageName,
        "Delete preprocess rule package: " + packageName,
        builder -> builder.preprocessRulePackageDelete(packageName),
        "Logged preprocess rule package delete: " + packageName + " by user: " + operatorUsername);
  }
}
