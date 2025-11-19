package com.huawei.cloududn.dialingtestapp.dao.basicDataManage;

import com.huawei.cloududn.dialingtest.model.UserRole;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户角色关系DAO
 */
@Repository
@Mapper
public interface UserRoleDao {
    
    /**
     * 根据用户名查找用户角色关系
     */
    @Select("SELECT id, username, role FROM user_roles WHERE username = #{username}")
    List<UserRole> findByUsername(@Param("username") String username);
    
    /**
     * 检查用户角色关系是否存在
     */
    @Select("SELECT COUNT(*) > 0 FROM user_roles WHERE username = #{username} AND role = #{role}")
    boolean existsByUsernameAndRole(@Param("username") String username, @Param("role") String role);
    
    /**
     * 根据用户名和角色查找用户角色关系
     */
    @Select("SELECT id, username, role FROM user_roles WHERE username = #{username} AND role = #{role}")
    UserRole findByUsernameAndRole(@Param("username") String username, @Param("role") String role);
    
    /**
     * 分页查询用户角色关系，支持用户名搜索
     */
    @Select({
        "<script>",
        "SELECT id, username, role FROM user_roles",
        "<where>",
        "<if test='search != null and search != \"\"'>",
        "AND username LIKE CONCAT('%', #{search}, '%')",
        "</if>",
        "</where>",
        "ORDER BY id DESC",
        "LIMIT #{size} OFFSET #{offset}",
        "</script>"
    })
    List<UserRole> findByUsernameContainingIgnoreCase(@Param("search") String search, 
                                                     @Param("offset") int offset, 
                                                     @Param("size") int size);
    
    /**
     * 统计总数，支持用户名搜索
     */
    @Select({
        "<script>",
        "SELECT COUNT(*) FROM user_roles",
        "<where>",
        "<if test='search != null and search != \"\"'>",
        "AND username LIKE CONCAT('%', #{search}, '%')",
        "</if>",
        "</where>",
        "</script>"
    })
    int countByUsernameContainingIgnoreCase(@Param("search") String search);
    
    /**
     * 检查用户是否拥有指定角色中的任意一个
     */
    @Select({
        "<script>",
        "SELECT COUNT(*) > 0 FROM user_roles WHERE username = #{username} AND role IN",
        "<foreach item='role' collection='roles' open='(' separator=',' close=')'>",
        "#{role}",
        "</foreach>",
        "</script>"
    })
    boolean existsByUsernameAndRoleIn(@Param("username") String username, @Param("roles") List<String> roles);
    
    /**
     * 插入用户角色关系
     */
    @Insert("INSERT INTO user_roles (username, role) VALUES (#{username}, #{role})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserRole userRole);
    
    /**
     * 更新用户角色关系
     */
    @Update("UPDATE user_roles SET username = #{username}, role = #{role} WHERE id = #{id}")
    int update(UserRole userRole);
    
    /**
     * 删除用户角色关系
     */
    @Delete("DELETE FROM user_roles WHERE id = #{id}")
    int deleteById(@Param("id") Integer id);
    
    /**
     * 根据ID查找用户角色关系
     */
    @Select("SELECT id, username, role FROM user_roles WHERE id = #{id}")
    UserRole findById(@Param("id") Integer id);
    
    /**
     * 统计指定角色的数量
     */
    @Select("SELECT COUNT(*) FROM user_roles WHERE role = #{role}")
    int countByRole(@Param("role") String role);
}
