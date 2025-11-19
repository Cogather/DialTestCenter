package com.huawei.cloududn.dialingtestapp.config;

import com.huawei.cloududn.dialingtestapp.dao.basicDataManage.UserRoleDao;
import com.huawei.cloududn.dialingtest.model.UserRole;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AdminUserInitializer 单元测试
 *
 * @author g00940940
 * @since 2025-01-28
 */
@RunWith(MockitoJUnitRunner.class)
public class AdminUserInitializerTest {
    
    @Mock
    private UserRoleDao userRoleDao;
    
    @InjectMocks
    private AdminUserInitializer adminUserInitializer;
    
    @Before
    public void setUp() {
        // 初始化测试对象
    }
    
    /**
     * 测试管理员用户已存在的情况
     */
    @Test
    public void testInitializeAdminUser_UserExists() {
        // 模拟管理员用户已存在
        when(userRoleDao.existsByUsernameAndRole("admin", "ADMIN")).thenReturn(true);
        
        // 执行初始化
        adminUserInitializer.initializeAdminUser();
        
        // 验证不会调用 insert 方法
        verify(userRoleDao, times(1)).existsByUsernameAndRole("admin", "ADMIN");
        verify(userRoleDao, never()).insert(any(UserRole.class));
    }
    
    /**
     * 测试管理员用户不存在的情况
     */
    @Test
    public void testInitializeAdminUser_UserNotExists() {
        // 模拟管理员用户不存在
        when(userRoleDao.existsByUsernameAndRole("admin", "ADMIN")).thenReturn(false);
        when(userRoleDao.insert(any(UserRole.class))).thenReturn(1);
        
        // 执行初始化
        adminUserInitializer.initializeAdminUser();
        
        // 验证会调用 existsByUsernameAndRole 和 insert
        verify(userRoleDao, times(1)).existsByUsernameAndRole("admin", "ADMIN");
        verify(userRoleDao, times(1)).insert(any(UserRole.class));
    }
    
    /**
     * 测试初始化组件不为空
     */
    @Test
    public void testAdminUserInitializerNotNull() {
        assertNotNull(adminUserInitializer);
    }
}

