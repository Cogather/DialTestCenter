package com.huawei.cloududn.dialingtestapp.util;

import com.huawei.cloududn.dialingtestapp.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 权限校验工具类
 * 统一处理用户权限验证逻辑
 *
 * @author Generated
 * @since 2025-01-27
 */
@Component
public class PermissionValidator {
    
    @Autowired
    private UserRoleService userRoleService;
    
    /**
     * 权限验证结果
     */
    public static class PermissionValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        private PermissionValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        /**
         * 创建验证通过的结果
         */
        public static PermissionValidationResult success() {
            return new PermissionValidationResult(true, null);
        }
        
        /**
         * 创建验证失败的结果
         */
        public static PermissionValidationResult failure(String errorMessage) {
            return new PermissionValidationResult(false, errorMessage);
        }
        
        /**
         * 验证是否通过
         */
        public boolean isValid() {
            return valid;
        }
        
        /**
         * 获取错误消息
         */
        public String getErrorMessage() {
            return errorMessage;
        }
    }
    
    /**
     * 验证用户名是否为空
     *
     * @param username 用户名
     * @return 验证结果，如果为空返回失败结果，否则返回成功结果
     */
    public PermissionValidationResult validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return PermissionValidationResult.failure("未提供用户名");
        }
        return PermissionValidationResult.success();
    }
    
    /**
     * 检查用户是否有指定的角色（仅检查单个角色）
     *
     * @param username 用户名
     * @param requiredRole 需要的角色（如 "ADMIN"）
     * @param operationDescription 操作描述（用于错误消息）
     * @return 验证结果
     */
    public PermissionValidationResult checkRole(String username, String requiredRole, String operationDescription) {
        PermissionValidationResult usernameCheck = validateUsername(username);
        if (!usernameCheck.isValid()) {
            return usernameCheck;
        }
        
        List<String> userRoles = userRoleService.getUserRolesByUsername(username);
        if (!userRoles.contains(requiredRole)) {
            String errorMessage = String.format("权限不足，需要%s权限", getRoleDisplayName(requiredRole));
            if (operationDescription != null && !operationDescription.isEmpty()) {
                errorMessage = String.format("权限不足，%s需要%s权限", operationDescription, getRoleDisplayName(requiredRole));
            }
            return PermissionValidationResult.failure(errorMessage);
        }
        
        return PermissionValidationResult.success();
    }
    
    /**
     * 检查用户是否有指定的任一角色（检查多个角色中的任意一个）
     *
     * @param username 用户名
     * @param requiredRoles 需要的角色列表（如 ["ADMIN", "OPERATOR"]）
     * @param operationDescription 操作描述（用于错误消息）
     * @return 验证结果
     */
    public PermissionValidationResult checkAnyRole(
            String username, List<String> requiredRoles, String operationDescription) {
        PermissionValidationResult usernameCheck = validateUsername(username);
        if (!usernameCheck.isValid()) {
            return usernameCheck;
        }
        
        List<String> userRoles = userRoleService.getUserRolesByUsername(username);
        boolean hasRequiredRole = false;
        for (String requiredRole : requiredRoles) {
            if (userRoles.contains(requiredRole)) {
                hasRequiredRole = true;
                break;
            }
        }
        
        if (!hasRequiredRole) {
            String roleNames = String.join("或", requiredRoles.stream()
                    .map(this::getRoleDisplayName)
                    .toArray(String[]::new));
            String errorMessage = String.format("权限不足，需要%s权限", roleNames);
            if (operationDescription != null && !operationDescription.isEmpty()) {
                errorMessage = String.format("权限不足，%s需要%s权限", operationDescription, roleNames);
            }
            return PermissionValidationResult.failure(errorMessage);
        }
        
        return PermissionValidationResult.success();
    }
    
    /**
     * 检查用户是否为ADMIN（常用方法）
     *
     * @param username 用户名
     * @param operationDescription 操作描述（用于错误消息）
     * @return 验证结果
     */
    public PermissionValidationResult checkAdmin(String username, String operationDescription) {
        return checkRole(username, "ADMIN", operationDescription);
    }
    
    /**
     * 检查用户是否为ADMIN或OPERATOR（常用方法）
     *
     * @param username 用户名
     * @param operationDescription 操作描述（用于错误消息）
     * @return 验证结果
     */
    public PermissionValidationResult checkAdminOrOperator(String username, String operationDescription) {
        return checkAnyRole(username, Arrays.asList("ADMIN", "OPERATOR"), operationDescription);
    }
    
    /**
     * 获取角色显示名称
     *
     * @param role 角色代码
     * @return 角色显示名称
     */
    private String getRoleDisplayName(String role) {
        switch (role) {
            case "ADMIN":
                return "管理员";
            case "OPERATOR":
                return "操作员";
            case "BROWSER":
                return "浏览者";
            case "EXECUTOR":
                return "执行机";
            default:
                return role;
        }
    }
}
