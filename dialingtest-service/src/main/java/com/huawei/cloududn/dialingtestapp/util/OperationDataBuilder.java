/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.util;

import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Field;

/**
 * 操作数据构建器
 * 用于构建各种操作的数据，支持实体类参数和链式调用
 * 重构后消除了重复代码，使用通用方法处理所有业务操作
 *
 * @author g00940940
 * @since 2025-01-24
 */
public class OperationDataBuilder {
    
    // ==================== 操作类型常量 ====================
    public static final String OPERATION_CREATE = "CREATE";
    public static final String OPERATION_UPDATE = "UPDATE";
    public static final String OPERATION_DELETE = "DELETE";
    public static final String OPERATION_VIEW = "VIEW";
    public static final String OPERATION_LOGIN = "LOGIN";
    public static final String OPERATION_LOGOUT = "LOGOUT";
    public static final String OPERATION_OVERWRITE = "OVERWRITE";
    public static final String OPERATION_BATCH_CREATE = "BATCH_CREATE";
    
    // ==================== 操作目标常量 ====================
    public static final String TARGET_USER = "USER";
    public static final String TARGET_USER_ROLE = "USER_ROLE";
    public static final String TARGET_TEST_CASE_SET = "TEST_CASE_SET";
    public static final String TARGET_SOFTWARE_PACKAGE = "SOFTWARE_PACKAGE";
    public static final String TARGET_SYSTEM = "SYSTEM";
    
    private Map<String, Object> data;

    public OperationDataBuilder() {
        this.data = new HashMap<>();
    }

    /**
     * 添加数据字段
     *
     * @param key 字段名
     * @param value 字段值
     * @return 当前构建器，支持链式调用
     */
    public OperationDataBuilder add(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

    /**
     * 构建操作数据
     *
     * @return 操作数据Map
     */
    public Map<String, Object> build() {
        return new HashMap<>(this.data);
    }

    // 通用方法：将实体类转换为操作数据
    public OperationDataBuilder fromEntity(Object entity) {
        if (entity == null) {
            return this;
        }
        
        try {
            Class<?> clazz = entity.getClass();
            Field[] fields = clazz.getDeclaredFields();
            
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(entity);
                
                // 对于 DialUser 对象，隐藏 password 字段
                if (clazz.getName().equals("com.huawei.cloududn.dialingtest.model.DialUser") && "password".equals(field.getName())) {
                    this.data.put(field.getName(), "*******");
                } else if (value != null) {
                    this.data.put(field.getName(), value);
                }
            }
        } catch (Exception e) {
            // 如果反射失败，至少添加对象的字符串表示
            this.data.put("entity", entity.toString());
        }
        
        return this;
    }

    // 通用操作构建方法 - 支持单实体操作
    public OperationDataBuilder buildOperation(Object entity, String operationType, String operationTarget) {
        return fromEntity(entity)
               .add("operationType", operationType)
               .add("operationTarget", operationTarget);
    }

    // 通用更新操作构建方法 - 支持更新操作
    public OperationDataBuilder buildUpdateOperation(
            Object oldValues, Object newValues, String operationType, String operationTarget) {
        Map<String, Object> oldData = maskSensitiveFields(oldValues);
        Map<String, Object> newData = maskSensitiveFields(newValues);
        
        return add("operationType", operationType)
               .add("operationTarget", operationTarget)
               .add("oldValues", oldData)
               .add("newValues", newData);
    }
    
    /**
     * 屏蔽敏感字段（如 password）
     * 用于在记录用户更新操作时隐藏敏感信息
     * 对于 DialUser 对象的 password 字段，返回 "*******" 替代实际值
     *
     * @param entity 实体对象
     * @return 包含屏蔽后字段的Map
     */
    private Map<String, Object> maskSensitiveFields(Object entity) {
        Map<String, Object> data = new HashMap<>();
        
        if (entity == null) {
            return data;
        }
        
        try {
            Class<?> clazz = entity.getClass();
            Field[] fields = clazz.getDeclaredFields();
            
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(entity);
                
                // 对于 DialUser 对象，隐藏 password 字段
                if (clazz.getName().equals("com.huawei.cloududn.dialingtest.model.DialUser") && "password".equals(field.getName())) {
                    data.put(field.getName(), "*******");
                } else if (value != null) {
                    data.put(field.getName(), value);
                }
            }
        } catch (Exception e) {
            // 如果反射失败，至少添加对象的字符串表示
            data.put("entity", entity.toString());
        }
        
        return data;
    }

    // 通用批量操作构建方法 - 支持批量操作
    public OperationDataBuilder buildBatchOperation(Object entityList, String operationType, String operationTarget) {
        return fromEntity(entityList)
               .add("operationType", operationType)
               .add("operationTarget", operationTarget);
    }

    // 通用操作构建方法 - 支持自定义操作
    public OperationDataBuilder buildCustomOperation(
            Object entity, String operationType, String operationTarget, 
            Map<String, Object> additionalData) {
        OperationDataBuilder builder = fromEntity(entity)
               .add("operationType", operationType)
               .add("operationTarget", operationTarget);
        
        if (additionalData != null) {
            additionalData.forEach(builder::add);
        }
        
        return builder;
    }

    // ==================== 业务操作方法 - 使用通用方法 ====================
    
    // 用户操作相关方法
    public OperationDataBuilder userCreate(Object userEntity) {
        return buildOperation(userEntity, OPERATION_CREATE, TARGET_USER);
    }

    public OperationDataBuilder userUpdate(Object oldValues, Object newValues) {
        return buildUpdateOperation(oldValues, newValues, OPERATION_UPDATE, TARGET_USER);
    }

    public OperationDataBuilder userDelete(Object userEntity) {
        return buildOperation(userEntity, OPERATION_DELETE, TARGET_USER);
    }

    public OperationDataBuilder userView(Object userEntity) {
        return buildOperation(userEntity, OPERATION_VIEW, TARGET_USER);
    }

    public OperationDataBuilder userLogin(Object userEntity) {
        return buildOperation(userEntity, OPERATION_LOGIN, TARGET_SYSTEM);
    }

    public OperationDataBuilder userLogout(Object userEntity) {
        return buildOperation(userEntity, OPERATION_LOGOUT, TARGET_SYSTEM);
    }

    // 用户角色操作相关方法
    public OperationDataBuilder userRoleCreate(Object userRoleEntity) {
        return buildOperation(userRoleEntity, OPERATION_CREATE, TARGET_USER_ROLE);
    }

    public OperationDataBuilder userRoleUpdate(Object oldValues, Object newValues) {
        return buildUpdateOperation(oldValues, newValues, OPERATION_UPDATE, TARGET_USER_ROLE);
    }

    public OperationDataBuilder userRoleDelete(Object userRoleEntity) {
        return buildOperation(userRoleEntity, OPERATION_DELETE, TARGET_USER_ROLE);
    }

    public OperationDataBuilder userRoleNoChanges(Object userRoleEntity) {
        Map<String, Object> additionalData = new HashMap<>();
        additionalData.put("changes", "none");
        return buildCustomOperation(userRoleEntity, OPERATION_UPDATE, TARGET_USER_ROLE, additionalData);
    }

    // 用例集操作相关方法
    public OperationDataBuilder testCaseSetCreate(Object testCaseSetEntity) {
        return buildOperation(testCaseSetEntity, OPERATION_CREATE, TARGET_TEST_CASE_SET);
    }

    public OperationDataBuilder testCaseSetUpdate(Object oldValues, Object newValues) {
        return buildUpdateOperation(oldValues, newValues, OPERATION_UPDATE, TARGET_TEST_CASE_SET);
    }

    public OperationDataBuilder testCaseSetDelete(Object testCaseSetEntity) {
        return buildOperation(testCaseSetEntity, OPERATION_DELETE, TARGET_TEST_CASE_SET);
    }
    
    /**
     * 构建用例集校验操作数据
     */
    public OperationDataBuilder testCaseSetValidation(Object testCaseSetEntity) {
        return buildOperation(testCaseSetEntity, "VALIDATE", TARGET_TEST_CASE_SET);
    }

    // 软件包操作相关方法
    public OperationDataBuilder softwarePackageCreate(Object softwarePackageEntity) {
        return buildOperation(softwarePackageEntity, OPERATION_CREATE, TARGET_SOFTWARE_PACKAGE);
    }

    public OperationDataBuilder softwarePackageUpdate(Object oldValues, Object newValues) {
        return buildUpdateOperation(oldValues, newValues, OPERATION_UPDATE, TARGET_SOFTWARE_PACKAGE);
    }

    public OperationDataBuilder softwarePackageDelete(Object softwarePackageEntity) {
        return buildOperation(softwarePackageEntity, OPERATION_DELETE, TARGET_SOFTWARE_PACKAGE);
    }

    public OperationDataBuilder softwarePackageOverwrite(Object oldPackage, Object newPackage) {
        return buildUpdateOperation(oldPackage, newPackage, OPERATION_OVERWRITE, TARGET_SOFTWARE_PACKAGE);
    }

    public OperationDataBuilder softwarePackageBatchCreate(Object softwarePackagesList) {
        return buildBatchOperation(softwarePackagesList, OPERATION_BATCH_CREATE, TARGET_SOFTWARE_PACKAGE);
    }

    // ==================== 通用操作方法和便捷方法 ====================
    
    /**
     * 通用业务操作方法 - 支持所有业务操作类型
     * 
     * @param entity 操作实体
     * @param operationType 操作类型 (CREATE, UPDATE, DELETE, VIEW, LOGIN, LOGOUT, OVERWRITE, BATCH_CREATE)
     * @param operationTarget 操作目标 (USER, USER_ROLE, TEST_CASE_SET, SOFTWARE_PACKAGE, SYSTEM)
     * @return OperationDataBuilder
     */
    public OperationDataBuilder businessOperation(Object entity, String operationType, String operationTarget) {
        return buildOperation(entity, operationType, operationTarget);
    }

    /**
     * 通用业务更新操作方法
     * 
     * @param oldValues 更新前的值
     * @param newValues 更新后的值
     * @param operationType 操作类型
     * @param operationTarget 操作目标
     * @return OperationDataBuilder
     */
    public OperationDataBuilder businessUpdateOperation(
            Object oldValues, Object newValues, String operationType, String operationTarget) {
        return buildUpdateOperation(oldValues, newValues, operationType, operationTarget);
    }

    /**
     * 通用业务批量操作方法
     * 
     * @param entityList 实体列表
     * @param operationType 操作类型
     * @param operationTarget 操作目标
     * @return OperationDataBuilder
     */
    public OperationDataBuilder businessBatchOperation(Object entityList, String operationType, String operationTarget) {
        return buildBatchOperation(entityList, operationType, operationTarget);
    }

    // 便捷方法：添加操作描述
    public OperationDataBuilder withDescription(String descriptionZh, String descriptionEn) {
        return add("descriptionZh", descriptionZh)
               .add("descriptionEn", descriptionEn);
    }

    // 便捷方法：添加时间戳
    public OperationDataBuilder withTimestamp() {
        return add("timestamp", System.currentTimeMillis());
    }

    // 便捷方法：添加操作者信息
    public OperationDataBuilder withOperator(String operatorUsername) {
        return add("operatorUsername", operatorUsername);
    }

    // 便捷方法：添加自定义数据
    public OperationDataBuilder withCustomData(String key, Object value) {
        return add(key, value);
    }
    
    /**
     * 预处理规则ZIP包上传操作数据构建器
     */
    public OperationDataBuilder preprocessRulePackageUpload(String packageName, String businessZh, String businessEn) {
        return add("packageName", packageName)
               .add("businessZh", businessZh)
               .add("businessEn", businessEn)
               .withTimestamp();
    }
    
    /**
     * 预处理规则ZIP包删除操作数据构建器
     */
    public OperationDataBuilder preprocessRulePackageDelete(String packageName) {
        return add("packageName", packageName)
               .withTimestamp();
    }
}

