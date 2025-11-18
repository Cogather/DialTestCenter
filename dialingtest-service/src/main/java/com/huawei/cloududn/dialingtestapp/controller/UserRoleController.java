package com.huawei.cloududn.dialingtestapp.controller;

import com.huawei.cloududn.dialingtest.api.UserRolesApi;
import com.huawei.cloududn.dialingtest.model.UserPermissionResponse;
import com.huawei.cloududn.dialingtest.model.UserPermissionResponseData;
import com.huawei.cloududn.dialingtest.model.UserPermissionResponseDataPagePermissions;
import com.huawei.cloududn.dialingtest.model.UserRolePageResponse;
import com.huawei.cloududn.dialingtest.model.UserRoleResponse;
import com.huawei.cloududn.dialingtest.model.CreateUserRoleRequest;
import com.huawei.cloududn.dialingtest.model.UpdateUserRoleRequest;
import com.huawei.cloududn.dialingtest.model.UserRole;
import com.huawei.cloududn.dialingtest.model.ExecutorCountResponse;
import com.huawei.cloududn.dialingtest.model.UserRolePageResponseData;
import com.huawei.cloududn.dialingtestapp.util.OperationLogUtil;
import com.huawei.cloududn.dialingtestapp.util.PermissionValidator;
import com.huawei.cloududn.dialingtestapp.util.PermissionValidator.PermissionValidationResult;
import com.huawei.cloududn.dialingtestapp.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户角色管理控制器
 */
@RestController
@RequestMapping("/api")
public class UserRoleController implements UserRolesApi {
    
    @Autowired
    private UserRoleService userRoleService;
    
    @Autowired
    private OperationLogUtil operationLogUtil;
    
    @Autowired
    private PermissionValidator permissionValidator;
    
    @Override
    public ResponseEntity<UserRolePageResponse> getUserRoles(Integer page, Integer size, String search) {
        try {
            UserRolePageResponseData data = userRoleService.getUserRolesWithPagination(page, size, search);
            
            UserRolePageResponse response = new UserRolePageResponse();
            response.setSuccess(true);
            response.setData(data);
            response.setMessage("获取用户角色列表成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            UserRolePageResponse response = new UserRolePageResponse();
            response.setSuccess(false);
            response.setMessage("获取用户角色列表失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @Override
    public ResponseEntity<UserRoleResponse> createUserRole(
            @RequestHeader("X-Csrf-Token") String xCsrfToken,
            @RequestHeader("X-Username") String xUsername,
            CreateUserRoleRequest body) {
        try {
            // 检查权限（需要ADMIN权限）
            PermissionValidationResult permissionResult = 
                    permissionValidator.checkAdmin(xUsername, "创建用户角色");
            if (!permissionResult.isValid()) {
                UserRoleResponse response = new UserRoleResponse();
                response.setSuccess(false);
                response.setMessage(permissionResult.getErrorMessage());
                HttpStatus status = permissionResult.getErrorMessage().contains("未提供用户名") 
                    ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN;
                return ResponseEntity.status(status).body(response);
            }
            
            UserRole userRole = userRoleService.createUserRole(
                body.getUsername(), 
                body.getRole().toString(), 
                xUsername
            );
            
            UserRoleResponse response = new UserRoleResponse();
            response.setSuccess(true);
            response.setData(userRole);
            response.setMessage("创建用户角色成功");
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            UserRoleResponse response = new UserRoleResponse();
            response.setSuccess(false);
            response.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            UserRoleResponse response = new UserRoleResponse();
            response.setSuccess(false);
            response.setMessage("创建用户角色失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @Override
    public ResponseEntity<UserRoleResponse> updateUserRole(
            String xUsername, Integer id, UpdateUserRoleRequest body) {
        try {
            // 检查权限（需要ADMIN权限）
            PermissionValidationResult permissionResult = 
                    permissionValidator.checkAdmin(xUsername, "update user role");
            if (!permissionResult.isValid()) {
                UserRoleResponse response = new UserRoleResponse();
                response.setSuccess(false);
                response.setMessage(permissionResult.getErrorMessage());
                HttpStatus status = permissionResult.getErrorMessage().contains("未提供用户名") 
                    ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN;
                return ResponseEntity.status(status).body(response);
            }
            
            UserRole userRole = userRoleService.updateUserRole(
                id, 
                body.getUsername(), 
                body.getRole().toString(), 
                xUsername
            );
            
            UserRoleResponse response = new UserRoleResponse();
            response.setSuccess(true);
            response.setData(userRole);
            response.setMessage("Update user role successful");
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            UserRoleResponse response = new UserRoleResponse();
            response.setSuccess(false);
            response.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (DataIntegrityViolationException e) {
            // Catch database unique constraint violation exception
            String errorMessage = e.getMessage();
            if (errorMessage != null && (errorMessage.contains("user_roles_username_role_key") 
                    || errorMessage.contains("duplicate key") 
                    || errorMessage.contains("unique constraint"))) {
                UserRoleResponse response = new UserRoleResponse();
                response.setSuccess(false);
                response.setMessage("User role already exists");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            } else {
                UserRoleResponse response = new UserRoleResponse();
                response.setSuccess(false);
                response.setMessage("Failed to update user role: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } catch (Exception e) {
            UserRoleResponse response = new UserRoleResponse();
            response.setSuccess(false);
            response.setMessage("Failed to update user role: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @Override
    public ResponseEntity<Void> deleteUserRole(Integer id, String xUsername) {
        try {
            // 检查权限（需要ADMIN权限）
            PermissionValidationResult permissionResult = 
                    permissionValidator.checkAdmin(xUsername, "删除用户角色");
            if (!permissionResult.isValid()) {
                HttpStatus status = permissionResult.getErrorMessage().contains("未提供用户名") 
                    ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN;
                return ResponseEntity.status(status).build();
            }
            
            userRoleService.deleteUserRole(id, xUsername);
            
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Override
    public ResponseEntity<UserPermissionResponse> getUserPermission(String xUsername) {
        try {
            List<String> userRoles = userRoleService.getUserRolesByUsername(xUsername);
            Map<String, UserPermissionResponseDataPagePermissions> pagePermissions = buildPagePermissions(userRoles);
            
            UserPermissionResponseData data = new UserPermissionResponseData();
            data.setUsername(xUsername);
            data.setRoles(userRoles);
            data.setPagePermissions(pagePermissions);
            
            UserPermissionResponse response = new UserPermissionResponse();
            response.setSuccess(true);
            response.setData(data);
            response.setMessage("获取用户权限信息成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            UserPermissionResponse response = new UserPermissionResponse();
            response.setSuccess(false);
            response.setMessage("获取用户权限信息失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 构建所有页面的权限映射
     *
     * @param userRoles 用户角色列表
     * @return 页面权限映射
     */
    private Map<String, UserPermissionResponseDataPagePermissions> buildPagePermissions(List<String> userRoles) {
        Map<String, UserPermissionResponseDataPagePermissions> pagePermissions = new HashMap<>();
        
        pagePermissions.put("user-management", buildUserManagementPermissions(userRoles));
        pagePermissions.put("test-case-set", buildTestCaseSetPermissions(userRoles));
        pagePermissions.put("software-package", buildSoftwarePackagePermissions(userRoles));
        pagePermissions.put("user-role-management", buildUserRoleManagementPermissions(userRoles));
        pagePermissions.put("preprocess-rule-management", buildPreprocessRuleManagementPermissions(userRoles));
        
        return pagePermissions;
    }
    
    /**
     * 构建用户管理页面权限
     *
     * @param userRoles 用户角色列表
     * @return 页面权限配置
     */
    private UserPermissionResponseDataPagePermissions buildUserManagementPermissions(List<String> userRoles) {
        UserPermissionResponseDataPagePermissions perms = new UserPermissionResponseDataPagePermissions();
        perms.setHasAccess(true);
        if (userRoles.contains("ADMIN")) {
            perms.setOperations(Arrays.asList("create", "edit", "delete", "view"));
        } else {
            perms.setOperations(Arrays.asList("view"));
        }
        return perms;
    }
    
    /**
     * 构建用例集管理页面权限
     *
     * @param userRoles 用户角色列表
     * @return 页面权限配置
     */
    private UserPermissionResponseDataPagePermissions buildTestCaseSetPermissions(List<String> userRoles) {
        UserPermissionResponseDataPagePermissions perms = new UserPermissionResponseDataPagePermissions();
        perms.setHasAccess(true);
        if (userRoles.contains("ADMIN") || userRoles.contains("OPERATOR")) {
            perms.setOperations(Arrays.asList("upload", "download", "delete", "view", "edit", "validate"));
        } else {
            perms.setOperations(Arrays.asList("view"));
        }
        return perms;
    }
    
    /**
     * 构建软件包管理页面权限
     *
     * @param userRoles 用户角色列表
     * @return 页面权限配置
     */
    private UserPermissionResponseDataPagePermissions buildSoftwarePackagePermissions(List<String> userRoles) {
        UserPermissionResponseDataPagePermissions perms = new UserPermissionResponseDataPagePermissions();
        perms.setHasAccess(true);
        if (userRoles.contains("ADMIN") || userRoles.contains("OPERATOR")) {
            perms.setOperations(Arrays.asList("upload", "download", "delete", "view", "edit"));
        } else {
            perms.setOperations(Arrays.asList("view"));
        }
        return perms;
    }
    
    /**
     * 构建用户角色管理页面权限
     *
     * @param userRoles 用户角色列表
     * @return 页面权限配置
     */
    private UserPermissionResponseDataPagePermissions buildUserRoleManagementPermissions(List<String> userRoles) {
        UserPermissionResponseDataPagePermissions perms = new UserPermissionResponseDataPagePermissions();
        perms.setHasAccess(true);
        if (userRoles.contains("ADMIN")) {
            perms.setOperations(Arrays.asList("create", "edit", "delete", "view"));
        } else {
            perms.setOperations(Arrays.asList("view"));
        }
        return perms;
    }
    
    /**
     * 构建预处理规则管理页面权限
     *
     * @param userRoles 用户角色列表
     * @return 页面权限配置
     */
    private UserPermissionResponseDataPagePermissions buildPreprocessRuleManagementPermissions(List<String> userRoles) {
        UserPermissionResponseDataPagePermissions perms = new UserPermissionResponseDataPagePermissions();
        perms.setHasAccess(true);
        if (userRoles.contains("ADMIN") || userRoles.contains("OPERATOR")) {
            perms.setOperations(Arrays.asList("create", "edit", "delete", "view"));
        } else {
            perms.setOperations(Arrays.asList("view"));
        }
        return perms;
    }
    
    @Override
    public ResponseEntity<ExecutorCountResponse> getExecutorCount() {
        try {
            int executorCount = userRoleService.getExecutorCount();
            
            ExecutorCountResponse response = new ExecutorCountResponse();
            response.setSuccess(true);
            response.setData(executorCount);
            response.setMessage("获取执行机数量成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ExecutorCountResponse response = new ExecutorCountResponse();
            response.setSuccess(false);
            response.setMessage("获取执行机数量失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
