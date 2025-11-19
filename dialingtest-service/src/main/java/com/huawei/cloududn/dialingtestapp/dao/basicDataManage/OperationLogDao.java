/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.dao.basicDataManage;

import com.huawei.cloududn.dialingtest.model.OperationLog;
import com.huawei.cloududn.dialingtest.model.OperationLogStatistics;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 操作记录数据访问对象
 * 提供操作记录的数据库操作功能
 *
 * @author g00940940
 * @since 2025-09-19
 */
@Mapper
public interface OperationLogDao {

    /**
     * 分页查询操作记录
     */
    @Select({
        "<script>",
        "SELECT ",
        "    ol.id, ",
        "    ol.username, ",
        "    ol.operation_type as operationType, ",
        "    ol.operation_target as operationTarget, ",
        "    ol.operation_description_zh as operationDescriptionZh, ",
        "    ol.operation_description_en as operationDescriptionEn, ",
        "    ol.operation_data as operationData, ",
        "    TO_CHAR(ol.operation_time, 'YYYY-MM-DD\"T\"HH24:MI:SS.MS') as operationTime ",
        "FROM operation_logs ol ",
        "WHERE 1=1 ",
        "<if test='username != null and username != &quot;&quot;'>",
        "    AND ol.username = #{username} ",
        "</if>",
        "<if test='operationType != null and operationType != &quot;&quot;'>",
        "    AND ol.operation_type = #{operationType} ",
        "</if>",
        "<if test='operationTarget != null and operationTarget != &quot;&quot;'>",
        "    AND ol.operation_target = #{operationTarget} ",
        "</if>",
        "<if test='startTime != null and startTime != &quot;&quot;'>",
        "    AND ol.operation_time &gt;= TO_TIMESTAMP(#{startTime}, 'YYYY-MM-DD&quot;T&quot;HH24:MI:SS.MS&quot;Z&quot;') ",
        "</if>",
        "<if test='endTime != null and endTime != &quot;&quot;'>",
        "    AND ol.operation_time &lt;= TO_TIMESTAMP(#{endTime}, 'YYYY-MM-DD&quot;T&quot;HH24:MI:SS.MS&quot;Z&quot;') ",
        "</if>",
        "ORDER BY ol.operation_time DESC ",
        "LIMIT #{size} OFFSET #{page} * #{size}",
        "</script>"
    })
    List<OperationLog> findOperationLogsWithPagination(
            @Param("page") Integer page,
            @Param("size") Integer size,
            @Param("username") String username,
            @Param("operationType") String operationType,
            @Param("operationTarget") String operationTarget,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime);
    
    /**
     * 统计操作记录总数
     */
    @Select({
        "<script>",
        "SELECT COUNT(*) ",
        "FROM operation_logs ol ",
        "WHERE 1=1 ",
        "<if test='username != null and username != &quot;&quot;'>",
        "    AND ol.username = #{username} ",
        "</if>",
        "<if test='operationType != null and operationType != &quot;&quot;'>",
        "    AND ol.operation_type = #{operationType} ",
        "</if>",
        "<if test='operationTarget != null and operationTarget != &quot;&quot;'>",
        "    AND ol.operation_target = #{operationTarget} ",
        "</if>",
        "<if test='startTime != null and startTime != &quot;&quot;'>",
        "    AND ol.operation_time &gt;= TO_TIMESTAMP(#{startTime}, 'YYYY-MM-DD&quot;T&quot;HH24:MI:SS.MS&quot;Z&quot;') ",
        "</if>",
        "<if test='endTime != null and endTime != &quot;&quot;'>",
        "    AND ol.operation_time &lt;= TO_TIMESTAMP(#{endTime}, 'YYYY-MM-DD&quot;T&quot;HH24:MI:SS.MS&quot;Z&quot;') ",
        "</if>",
        "</script>"
    })
    Long countOperationLogs(
            @Param("username") String username,
            @Param("operationType") String operationType,
            @Param("operationTarget") String operationTarget,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime);
    
    /**
     * 根据ID查询操作记录
     */
    @Select("SELECT ol.id, ol.username, " +
            "ol.operation_type as operationType, " +
            "ol.operation_target as operationTarget, " +
            "ol.operation_description_zh as operationDescriptionZh, " +
            "ol.operation_description_en as operationDescriptionEn, " +
            "ol.operation_data as operationData, " +
            "TO_CHAR(ol.operation_time, 'YYYY-MM-DD\"T\"HH24:MI:SS.MS') as operationTime " +
            "FROM operation_logs ol WHERE ol.id = #{id}")
    OperationLog findById(@Param("id") Integer id);
    
    /**
     * 保存操作记录
     */
    @Insert("INSERT INTO operation_logs " +
            "(username, operation_type, operation_target, operation_description_zh, " +
            "operation_description_en, operation_data) " +
            "VALUES (#{username}, #{operationType}, #{operationTarget}, " +
            "#{operationDescriptionZh}, #{operationDescriptionEn}, #{operationData}::jsonb)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int save(OperationLog operationLog);
    
    /**
     * 获取操作记录统计信息
     */
    @Select({
        "<script>",
        "SELECT ",
        "    COUNT(*) as totalOperations, ",
        "    COUNT(DISTINCT username) as uniqueUsers, ",
        "    COUNT(DISTINCT operation_type) as uniqueOperationTypes, ",
        "    COUNT(DISTINCT operation_target) as uniqueOperationTargets ",
        "FROM operation_logs ol ",
        "WHERE 1=1 ",
        "<if test='startTime != null and startTime != &quot;&quot;'>",
        "    AND ol.operation_time &gt;= TO_TIMESTAMP(#{startTime}, 'YYYY-MM-DD&quot;T&quot;HH24:MI:SS.MS&quot;Z&quot;') ",
        "</if>",
        "<if test='endTime != null and endTime != &quot;&quot;'>",
        "    AND ol.operation_time &lt;= TO_TIMESTAMP(#{endTime}, 'YYYY-MM-DD&quot;T&quot;HH24:MI:SS.MS&quot;Z&quot;') ",
        "</if>",
        "</script>"
    })
    OperationLogStatistics getStatistics(
            @Param("startTime") String startTime,
            @Param("endTime") String endTime);
    
    /**
     * 导出操作记录（用于Excel生成）
     */
    @Select({
        "<script>",
        "SELECT ",
        "    ol.id, ",
        "    ol.username, ",
        "    ol.operation_type as operationType, ",
        "    ol.operation_target as operationTarget, ",
        "    ol.operation_description_zh as operationDescriptionZh, ",
        "    ol.operation_description_en as operationDescriptionEn, ",
        "    ol.operation_data as operationData, ",
        "    TO_CHAR(ol.operation_time, 'YYYY-MM-DD\"T\"HH24:MI:SS.MS') as operationTime ",
        "FROM operation_logs ol ",
        "WHERE 1=1 ",
        "<if test='username != null and username != &quot;&quot;'>",
        "    AND ol.username = #{username} ",
        "</if>",
        "<if test='operationType != null and operationType != &quot;&quot;'>",
        "    AND ol.operation_type = #{operationType} ",
        "</if>",
        "<if test='operationTarget != null and operationTarget != &quot;&quot;'>",
        "    AND ol.operation_target = #{operationTarget} ",
        "</if>",
        "<if test='startTime != null and startTime != &quot;&quot;'>",
        "    AND ol.operation_time &gt;= TO_TIMESTAMP(#{startTime}, 'YYYY-MM-DD&quot;T&quot;HH24:MI:SS.MS&quot;Z&quot;') ",
        "</if>",
        "<if test='endTime != null and endTime != &quot;&quot;'>",
        "    AND ol.operation_time &lt;= TO_TIMESTAMP(#{endTime}, 'YYYY-MM-DD&quot;T&quot;HH24:MI:SS.MS&quot;Z&quot;') ",
        "</if>",
        "ORDER BY ol.operation_time DESC",
        "</script>"
    })
    List<OperationLog> findOperationLogsForExport(
            @Param("username") String username,
            @Param("operationType") String operationType,
            @Param("operationTarget") String operationTarget,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime);
}