package com.huawei.cloududn.dialingtestapp.dao.basicDataManage;

import com.huawei.cloududn.dialingtest.model.DialUser;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 拨测用户数据访问层接口
 * 使用MyBatis注解方式定义SQL
 * 
 * @author Generated
 */
@Mapper
public interface DialUserDao {
    
    /**
     * 分页查询用户
     * 
     * @param offset 偏移量
     * @param size 每页大小
     * @param username 用户名过滤条件
     * @return 用户列表
     */
    @Select({
        "<script>",
        "SELECT id, username, password, " +
        "TO_CHAR(last_login_time, 'YYYY-MM-DD\"T\"HH24:MI:SS.MS\"Z\"') as last_login_time " +
        "FROM dial_users",
        "WHERE 1=1",
        "<if test='username != null and username != \"\"'>",
        "AND username LIKE CONCAT('%', #{username}, '%')",
        "</if>",
        "ORDER BY id DESC",
        "LIMIT #{size} OFFSET #{offset}",
        "</script>"
    })
    List<DialUser> findUsersWithPagination(@Param("offset") int offset, 
                                          @Param("size") int size, 
                                          @Param("username") String username);
    
    /**
     * 统计用户总数
     * 
     * @param username 用户名过滤条件
     * @return 用户总数
     */
    @Select({
        "<script>",
        "SELECT COUNT(*) FROM dial_users",
        "WHERE 1=1",
        "<if test='username != null and username != \"\"'>",
        "AND username LIKE CONCAT('%', #{username}, '%')",
        "</if>",
        "</script>"
    })
    long countUsers(@Param("username") String username);
    
    /**
     * 根据ID查询用户
     * 
     * @param id 用户ID
     * @return 用户信息
     */
    @Select("SELECT id, username, password, " +
            "TO_CHAR(last_login_time, 'YYYY-MM-DD\"T\"HH24:MI:SS.MS\"Z\"') as last_login_time " +
            "FROM dial_users WHERE id = #{id}")
    DialUser findById(@Param("id") Integer id);
    
    /**
     * 根据用户名查询用户
     * 
     * @param username 用户名
     * @return 用户信息
     */
    @Select("SELECT id, username, password, " +
            "TO_CHAR(last_login_time, 'YYYY-MM-DD\"T\"HH24:MI:SS.MS\"Z\"') as last_login_time " +
            "FROM dial_users WHERE username = #{username}")
    DialUser findByUsername(@Param("username") String username);
    
    /**
     * 创建用户
     * 
     * @param user 用户信息
     * @return 创建的用户ID
     */
    @Insert("INSERT INTO dial_users (username, password) VALUES (#{username}, #{password})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int create(DialUser user);
    
    /**
     * 更新用户
     * 
     * @param user 用户信息
     * @return 更新的行数
     */
    @Update("UPDATE dial_users SET username = #{username}, password = #{password} WHERE id = #{id}")
    int update(DialUser user);
    
    /**
     * 删除用户
     * 
     * @param id 用户ID
     * @return 删除的行数
     */
    @Delete("DELETE FROM dial_users WHERE id = #{id}")
    int deleteById(@Param("id") Integer id);
    
    /**
     * 更新最后登录时间
     * 
     * @param id 用户ID
     * @param lastLoginTime 最后登录时间
     * @return 更新的行数
     */
    @Update("UPDATE dial_users SET last_login_time = #{lastLoginTime} WHERE id = #{id}")
    int updateLastLoginTime(@Param("id") Integer id, @Param("lastLoginTime") LocalDateTime lastLoginTime);
}