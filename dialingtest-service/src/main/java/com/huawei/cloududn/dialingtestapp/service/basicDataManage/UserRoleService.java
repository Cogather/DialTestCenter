package com.huawei.cloududn.dialingtestapp.service.basicDataManage;

import com.huawei.cloududn.dialingtestapp.dao.basicDataManage.UserRoleDao;
import com.huawei.cloududn.dialingtest.model.*;
import com.huawei.cloududn.dialingtestapp.util.OperationLogUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户角色管理服务
 */
@Service
@Transactional
public class UserRoleService {
    
    @Autowired
    private UserRoleDao userRoleDao;
    
    @Autowired
    private OperationLogUtil operationLogUtil;
    
    /**
     * 创建用户角色关系
     */
    public UserRole createUserRole(String username, String role, String operatorUsername) {
        // Validate role
        if (!isValidRole(role)) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }
        
        // Check if already exists
        if (userRoleDao.existsByUsernameAndRole(username, role)) {
            throw new IllegalArgumentException("User role relationship already exists");
        }
        
        // Create user role relationship
        UserRole userRole = new UserRole();
        userRole.setUsername(username);
        userRole.setRole(UserRole.RoleEnum.fromValue(role));
        
        userRoleDao.insert(userRole);
        
        // Log operation to operation log module
        operationLogUtil.logUserRoleCreate(operatorUsername, userRole);
        
        return userRole;
    }
    
    /**
     * 分页查询用户角色关系
     */
    public UserRolePageResponseData getUserRolesWithPagination(int page, int size, String search) {
        int offset = page * size;
        List<UserRole> content = userRoleDao.findByUsernameContainingIgnoreCase(search, offset, size);
        int totalElements = userRoleDao.countByUsernameContainingIgnoreCase(search);
        int totalPages = (int) Math.ceil((double) totalElements / size);
        
        UserRolePageResponseData data = new UserRolePageResponseData();
        data.setContent(content);
        data.setTotalElements(totalElements);
        data.setTotalPages(totalPages);
        data.setSize(size);
        data.setNumber(page);
        data.setFirst(page == 0);
        data.setLast(page >= totalPages - 1);
        
        return data;
    }
    
    /**
     * 根据用户名获取用户角色
     */
    public List<String> getUserRolesByUsername(String username) {
        List<UserRole> userRoles = userRoleDao.findByUsername(username);
        return userRoles.stream()
                .map(userRole -> userRole.getRole().toString())
                .collect(Collectors.toList());
    }
    
    /**
     * 检查用户是否拥有指定角色
     */
    public boolean hasRole(String username, String role) {
        return userRoleDao.existsByUsernameAndRole(username, role);
    }
    
    /**
     * 检查用户是否拥有指定角色中的任意一个
     */
    public boolean hasAnyRole(String username, List<String> roles) {
        return userRoleDao.existsByUsernameAndRoleIn(username, roles);
    }
    
    /**
     * 更新用户角色关系
     */
    public UserRole updateUserRole(Integer id, String username, String role, String operatorUsername) {
        // Validate role
        if (!isValidRole(role)) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }
        
        // Find existing record
        UserRole existingUserRole = userRoleDao.findById(id);
        if (existingUserRole == null) {
            throw new IllegalArgumentException("User role relationship does not exist");
        }
        
        String oldUsername = existingUserRole.getUsername();
        String oldRole = existingUserRole.getRole().toString();
        
        // If username or role changed, check if new username and role combination already exists (excluding current record)
        boolean usernameChanged = !oldUsername.equals(username);
        boolean roleChanged = !oldRole.equals(role);
        if (usernameChanged || roleChanged) {
            UserRole existingWithSameKey = userRoleDao.findByUsernameAndRole(username, role);
            if (existingWithSameKey != null && !existingWithSameKey.getId().equals(id)) {
                throw new IllegalArgumentException("User role already exists");
            }
        }
        
        // Update user role relationship
        existingUserRole.setUsername(username);
        existingUserRole.setRole(UserRole.RoleEnum.fromValue(role));
        
        userRoleDao.update(existingUserRole);
        
        UserRole newUserRole = new UserRole();
        newUserRole.setId(existingUserRole.getId());
        newUserRole.setUsername(username);
        newUserRole.setRole(UserRole.RoleEnum.fromValue(role));
        
        // Log operation
        operationLogUtil.logUserRoleUpdate(operatorUsername, existingUserRole, newUserRole);
        
        return existingUserRole;
    }
    
    /**
     * 删除用户角色关系
     */
    public void deleteUserRole(Integer id, String operatorUsername) {
        // Find existing record
        UserRole existingUserRole = userRoleDao.findById(id);
        if (existingUserRole == null) {
            throw new IllegalArgumentException("User role relationship does not exist");
        }
        
        String username = existingUserRole.getUsername();
        String role = existingUserRole.getRole().toString();
        
        // Delete user role relationship
        userRoleDao.deleteById(id);
        
        // Log operation
        operationLogUtil.logUserRoleDelete(operatorUsername, existingUserRole);
    }
    
    /**
     * 统计EXECUTOR角色的数量
     */
    public int getExecutorCount() {
        return userRoleDao.countByRole("EXECUTOR");
    }
    
    /**
     * 验证角色是否有效
     */
    private boolean isValidRole(String role) {
        return Arrays.asList("ADMIN", "OPERATOR", "BROWSER", "EXECUTOR").contains(role);
    }
}
