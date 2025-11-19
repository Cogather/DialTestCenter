package com.huawei.cloududn.dialingtestapp.controller.basicDataManage;

import com.huawei.cloududn.dialingtest.api.DialusersApi;
import com.huawei.cloududn.dialingtest.model.*;
import com.huawei.cloududn.dialingtestapp.service.basicDataManage.DialUserService;
import com.huawei.cloududn.dialingtestapp.service.basicDataManage.UserRoleService;
import com.huawei.cloududn.dialingtestapp.util.PermissionValidator;
import com.huawei.cloududn.dialingtestapp.util.PermissionValidator.PermissionValidationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


/**
 * 拨测用户控制器
 * 实现DialusersApi接口
 * 
 * @author Generated
 */
@RestController
@RequestMapping("/api")
public class DialUserController implements DialusersApi {
    
    @Autowired
    private DialUserService dialUserService;
    
    @Autowired
    private UserRoleService userRoleService;
    
    @Autowired
    private PermissionValidator permissionValidator;
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 创建成功响应
     *
     * @param data 响应数据
     * @param message 成功消息
     * @return 成功响应
     */
    private ResponseEntity<DialUserResponse> createSuccessResponse(DialUser data, String message) {
        DialUserResponse response = new DialUserResponse();
        response.setSuccess(true);
        response.setData(data);
        response.setMessage(message);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }
    
    /**
     * 创建成功响应（带状态码）
     *
     * @param data 响应数据
     * @param message 成功消息
     * @param status HTTP状态码
     * @return 成功响应
     */
    private ResponseEntity<DialUserResponse> createSuccessResponse(DialUser data, String message, HttpStatus status) {
        DialUserResponse response = new DialUserResponse();
        response.setSuccess(true);
        response.setData(data);
        response.setMessage(message);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }
    
    /**
     * 创建分页成功响应
     *
     * @param data 分页响应数据
     * @param message 成功消息
     * @return 成功响应
     */
    private ResponseEntity<DialUserPageResponse> createPageSuccessResponse(DialUserPageResponseData data, String message) {
        DialUserPageResponse response = new DialUserPageResponse();
        response.setSuccess(true);
        response.setData(data);
        response.setMessage(message);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }
    
    /**
     * 创建错误响应
     *
     * @param message 错误消息
     * @param status HTTP状态码
     * @return 错误响应
     */
    private ResponseEntity<DialUserResponse> createErrorResponse(String message, HttpStatus status) {
        DialUserResponse response = new DialUserResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }
    
    /**
     * 创建分页错误响应
     *
     * @param message 错误消息
     * @param status HTTP状态码
     * @return 错误响应
     */
    private ResponseEntity<DialUserPageResponse> createPageErrorResponse(String message, HttpStatus status) {
        DialUserPageResponse response = new DialUserPageResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }
    
    /**
     * 处理权限校验错误
     *
     * @param permissionResult 权限校验结果
     * @return 错误响应，如果权限通过则返回null
     */
    private ResponseEntity<DialUserResponse> handlePermissionError(
            PermissionValidationResult permissionResult) {
        if (permissionResult.isValid()) {
            return null;
        }
        HttpStatus status = permissionResult.getErrorMessage().contains("未提供用户名") 
            ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN;
        return createErrorResponse(permissionResult.getErrorMessage(), status);
    }
    
    /**
     * 处理IllegalArgumentException异常
     *
     * @param e 异常对象
     * @return 错误响应
     */
    private ResponseEntity<DialUserResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        String message = e.getMessage();
        if (message != null) {
            if (message.contains("User not found") || message.contains("用户不存在")) {
                return createErrorResponse(message, HttpStatus.NOT_FOUND);
            } else if (message.contains("Username already exists") || message.contains("用户名已存在")) {
                return createErrorResponse(message, HttpStatus.CONFLICT);
            }
        }
        return createErrorResponse(message != null ? message : "Invalid request", HttpStatus.BAD_REQUEST);
    }
    
    /**
     * 处理IllegalStateException异常
     *
     * @return 错误响应
     */
    private ResponseEntity<DialUserResponse> handleIllegalStateException() {
        return createErrorResponse("Internal server error, please try again later", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * 处理通用异常
     *
     * @param e 异常对象
     * @param operation 操作名称（用于错误消息）
     * @return 错误响应
     */
    private ResponseEntity<DialUserResponse> handleException(Exception e, String operation) {
        String message = operation + " failed: " + (e.getMessage() != null ? e.getMessage() : "Unknown error");
        return createErrorResponse(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * 处理通用异常（分页响应）
     *
     * @param e 异常对象
     * @param operation 操作名称（用于错误消息）
     * @return 错误响应
     */
    private ResponseEntity<DialUserPageResponse> handlePageException(Exception e, String operation) {
        String message = operation + " failed: " + (e.getMessage() != null ? e.getMessage() : "Unknown error");
        return createPageErrorResponse(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    // ==================== 公共接口方法 ====================
    
    /**
     * 分页查询拨测用户
     * 
     * @param page 页码，从0开始
     * @param size 每页大小
     * @param username 用户名过滤条件
     * @return 分页用户列表
     */
    @Override
    public ResponseEntity<DialUserPageResponse> getDialUsers(Integer page, Integer size, String username) {
        try {
            // 设置默认值
            if (page == null) {
                page = 0;
            }
            if (size == null) {
                size = 10;
            }
            
            // 查询用户列表和总数
            List<DialUser> users = dialUserService.findUsersWithPagination(page, size, username);
            long totalElements = dialUserService.countUsers(username);
            int totalPages = (int) Math.ceil((double) totalElements / size);
            
            // 构建分页响应数据
            DialUserPageResponseData data = new DialUserPageResponseData();
            data.setContent(users);
            data.setTotalElements((int) totalElements);
            data.setTotalPages(totalPages);
            data.setSize(size);
            data.setNumber(page);
            
            return createPageSuccessResponse(data, "Query successful");
            
        } catch (Exception e) {
            return handlePageException(e, "Query");
        }
    }
    
    /**
     * 根据ID查询拨测用户
     * 
     * @param id 用户ID
     * @return 用户信息
     */
    @Override
    public ResponseEntity<DialUserResponse> getDialUserById(Integer id) {
        try {
            DialUser user = dialUserService.findById(id);
            
            if (user == null) {
                return createErrorResponse("User not found", HttpStatus.NOT_FOUND);
            }
            
            return createSuccessResponse(user, "Query successful");
            
        } catch (Exception e) {
            return handleException(e, "Query");
        }
    }
    
    /**
     * 修改拨测用户
     * 
     * @param xUsername 操作用户名
     * @param id 用户ID
     * @param body 更新请求
     * @return 更新后的用户信息
     */
    @Override
    public ResponseEntity<DialUserResponse> updateDialUser(String xUsername, Integer id, UpdateDialUserRequest body) {
        try {
            // 检查权限（需要ADMIN权限）
            PermissionValidationResult permissionResult = 
                    permissionValidator.checkAdmin(xUsername, "update dial user");
            ResponseEntity<DialUserResponse> permissionError = handlePermissionError(permissionResult);
            if (permissionError != null) {
                return permissionError;
            }
            
            DialUser updatedUser = dialUserService.updateUser(id, body.getUsername(), body.getPassword(), xUsername);
            return createSuccessResponse(updatedUser, "Update successful");
            
        } catch (IllegalArgumentException e) {
            return handleIllegalArgumentException(e);
            
        } catch (IllegalStateException e) {
            return handleIllegalStateException();
            
        } catch (Exception e) {
            return handleException(e, "Update");
        }
    }
    
    /**
     * 删除拨测用户
     * 
     * @param id 用户ID
     * @param xUsername 操作用户名
     * @return 删除结果
     */
    @Override
    public ResponseEntity<Void> deleteDialUser(Integer id, String xUsername) {
        try {
            // 检查权限（需要ADMIN权限）
            PermissionValidationResult permissionResult = 
                    permissionValidator.checkAdmin(xUsername, "delete dial user");
            if (!permissionResult.isValid()) {
                HttpStatus status = permissionResult.getErrorMessage().contains("未提供用户名") 
                    ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN;
                return ResponseEntity.status(status).build();
            }
            
            dialUserService.deleteUser(id, xUsername);
            return ResponseEntity.noContent().build();
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
            
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 新增拨测用户
     * 
     * @param xCsrfToken CSRF防护令牌
     * @param xUsername 操作用户名
     * @param body 创建请求
     * @return 创建的用户信息
     */
    @Override
    public ResponseEntity<DialUserResponse> createDialUser(
            @RequestHeader("X-Csrf-Token") String xCsrfToken,
            @RequestHeader("X-Username") String xUsername,
            CreateDialUserRequest body) {
        try {
            // 检查权限（需要ADMIN权限）
            PermissionValidationResult permissionResult = 
                    permissionValidator.checkAdmin(xUsername, "create dial user");
            ResponseEntity<DialUserResponse> permissionError = handlePermissionError(permissionResult);
            if (permissionError != null) {
                return permissionError;
            }
            
            DialUser createdUser = dialUserService.createUser(body.getUsername(), body.getPassword(), xUsername);
            return createSuccessResponse(createdUser, "Create successful", HttpStatus.CREATED);
            
        } catch (IllegalArgumentException e) {
            return handleIllegalArgumentException(e);
            
        } catch (IllegalStateException e) {
            return handleIllegalStateException();
            
        } catch (Exception e) {
            return handleException(e, "Create");
        }
    }
}
