package com.huawei.cloududn.dialingtestapp.dao.executormanagement;

import com.huawei.cloududn.dialingtest.model.Executor;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.time.Instant;

/**
 * DAO for executor table.
 */
@Repository
@Mapper
public interface ExecutorDao {

    @Select("SELECT name, ip, token, proxy, description, status, " +
            "last_online_time AS lastOnlineTime " +
            "FROM executor WHERE name = #{name}")
    Executor findByName(@Param("name") String name);

    @Insert("INSERT INTO executor(name, ip, token, proxy, description, status, last_online_time) VALUES(" +
            "#{name}, #{ip}, #{token}, #{proxy}, #{description}, #{status}, #{lastOnlineTime}")
    int insert(Executor entity);

    @Update("INSERT INTO executor(name, token, status, last_online_time) " +
            "VALUES(#{name}, #{token}, #{status}, #{lastOnlineTime}) " +
            "ON CONFLICT (name) DO UPDATE SET " +
            "token = #{token}, status = #{status}, last_online_time = #{lastOnlineTime}")
    int updateTokenAndStatus(@Param("name") String name,
                             @Param("token") String token,
                             @Param("status") Integer status,
                             @Param("lastOnlineTime") Instant lastOnlineTime);

    @Insert("INSERT INTO executor(name, status, last_online_time) " +
            "VALUES(#{name}, #{status}, #{lastOnlineTime}) " +
            "ON CONFLICT (name) DO UPDATE SET " +
            "status = EXCLUDED.status, last_online_time = EXCLUDED.last_online_time")
    int updateStatus(@Param("name") String name,
                     @Param("status") Integer status,
                     @Param("lastOnlineTime") Instant lastOnlineTime);

    @Select({
            "<script>",
            "SELECT name, ip, token, proxy, description, status, last_online_time FROM executor",
            "<where>",
            "<if test='status != null'>",
            "status = #{status}",
            "</if>",
            "<if test='keyword != null and keyword != \"\"'>",
            "AND name ILIKE CONCAT('%', #{keyword}, '%')",
            "</if>",
            "</where>",
            "ORDER BY last_online_time DESC NULLS LAST",
            "LIMIT #{size} OFFSET #{offset}",
            "</script>"
    })
    java.util.List<Executor> findPage(@Param("status") Integer status,
                                      @Param("keyword") String keyword,
                                      @Param("offset") int offset,
                                      @Param("size") int size);

    @Select({
            "<script>",
            "SELECT COUNT(*) FROM executor",
            "<where>",
            "<if test='status != null'>",
            "status = #{status}",
            "</if>",
            "<if test='keyword != null and keyword != \"\"'>",
            "AND name ILIKE CONCAT('%', #{keyword}, '%')",
            "</if>",
            "</where>",
            "</script>"
    })
    int count(@Param("status") Integer status, @Param("keyword") String keyword);
    
    /**
     * Save or update executor with long token (V3 TLV protocol).
     *
     * @param name   executor name
     * @param token  8-byte token as long
     * @param status status string ("ONLINE", "OFFLINE", etc.)
     */
    @Insert("INSERT INTO executor(name, token, status, last_online_time) " +
            "VALUES(#{name}, #{token}::TEXT, " +
            "CASE WHEN #{status} = 'ONLINE' THEN 1 WHEN #{status} = 'OFFLINE' THEN 0 ELSE 2 END, " +
            "NOW()) " +
            "ON CONFLICT (name) DO UPDATE SET " +
            "token = #{token}::TEXT, " +
            "status = CASE WHEN #{status} = 'ONLINE' THEN 1 " +
            "WHEN #{status} = 'OFFLINE' THEN 0 ELSE 2 END, " +
            "last_online_time = NOW()")
    int saveOrUpdateExecutor(@Param("name") String name,
                             @Param("token") long token,
                             @Param("status") String status);
}


