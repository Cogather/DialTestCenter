package com.huawei.cloududn.dialingtestapp.config;

import com.huawei.cloududn.dialingtestapp.dao.basicDataManage.UserRoleDao;
import com.huawei.cloududn.dialingtest.model.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 管理员用户初始化组件
 * 在应用启动时自动创建默认的管理员用户
 *
 * @author g00940940
 * @since 2025-01-28
 */
@Component
public class AdminUserInitializer {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminUserInitializer.class);
    
    @Autowired
    private UserRoleDao userRoleDao;
    
    /**
     * 应用启动完成后，检查并创建默认管理员用户
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeAdminUser() {
        logger.info("Checking admin user existence...");
        
        try {
            String username = "admin";
            String role = "ADMIN";
            
            // 检查管理员用户是否存在
            if (!userRoleDao.existsByUsernameAndRole(username, role)) {
                logger.info("Admin user not found, creating default admin user...");
                
                // 创建管理员用户角色
                UserRole adminUserRole = new UserRole();
                adminUserRole.setUsername(username);
                adminUserRole.setRole(UserRole.RoleEnum.fromValue(role));
                
                int result = userRoleDao.insert(adminUserRole);
                
                if (result > 0) {
                    logger.info("Successfully created default admin user: username={}, role={}", username, role);
                } else {
                    logger.error("Failed to create default admin user");
                }
            } else {
                logger.info("Admin user already exists, skipping creation");
            }
            
        } catch (Exception e) {
            logger.error("Error occurred while initializing admin user", e);
        }
    }
}
