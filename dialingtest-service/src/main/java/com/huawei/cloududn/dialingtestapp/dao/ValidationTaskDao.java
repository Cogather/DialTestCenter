package com.huawei.cloududn.dialingtestapp.dao;

import com.huawei.cloududn.dialingtestapp.entity.ValidationTask;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;

/**
 * 校验任务数据访问对象
 * 
 * @author g00940940
 * @since 2025-10-30
 */
@Mapper
public interface ValidationTaskDao {
    
    /**
     * 插入校验任务
     */
    @Insert("INSERT INTO test_case_set_validation_task " +
            "(test_case_set_id, task_id, status, progress, " +
            "started_time, completed_time, error_message, created_time) " +
            "VALUES (#{testCaseSetId}, #{taskId}, #{status}, #{progress}, " +
            "#{startedTime}, #{completedTime}, #{errorMessage}, #{createdTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ValidationTask task);
    
    /**
     * 根据任务ID查询
     */
    @Select("SELECT * FROM test_case_set_validation_task WHERE task_id = #{taskId}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "testCaseSetId", column = "test_case_set_id"),
        @Result(property = "taskId", column = "task_id"),
        @Result(property = "status", column = "status"),
        @Result(property = "progress", column = "progress"),
        @Result(property = "startedTime", column = "started_time"),
        @Result(property = "completedTime", column = "completed_time"),
        @Result(property = "errorMessage", column = "error_message"),
        @Result(property = "createdTime", column = "created_time")
    })
    ValidationTask findByTaskId(String taskId);
    
    /**
     * 根据用例集ID查询最新的任务
     */
    @Select("SELECT * FROM test_case_set_validation_task " +
            "WHERE test_case_set_id = #{testCaseSetId} " +
            "ORDER BY created_time DESC LIMIT 1")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "testCaseSetId", column = "test_case_set_id"),
        @Result(property = "taskId", column = "task_id"),
        @Result(property = "status", column = "status"),
        @Result(property = "progress", column = "progress"),
        @Result(property = "startedTime", column = "started_time"),
        @Result(property = "completedTime", column = "completed_time"),
        @Result(property = "errorMessage", column = "error_message"),
        @Result(property = "createdTime", column = "created_time")
    })
    ValidationTask findLatestByTestCaseSetId(Long testCaseSetId);
    
    /**
     * 更新任务状态和时间
     */
    @Update("UPDATE test_case_set_validation_task " +
            "SET status = #{status}, progress = #{progress}, " +
            "started_time = #{startedTime}, completed_time = #{completedTime}, " +
            "error_message = #{errorMessage} " +
            "WHERE task_id = #{taskId}")
    int updateStatus(@Param("taskId") String taskId,
                    @Param("status") String status,
                    @Param("progress") Integer progress,
                    @Param("startedTime") LocalDateTime startedTime,
                    @Param("completedTime") LocalDateTime completedTime,
                    @Param("errorMessage") String errorMessage);
}

