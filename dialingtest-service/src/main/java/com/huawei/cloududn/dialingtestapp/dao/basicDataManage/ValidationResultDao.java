package com.huawei.cloududn.dialingtestapp.dao.basicDataManage;

import org.apache.ibatis.annotations.*;

/**
 * 校验结果数据访问对象
 * 
 * @author g00940940
 * @since 2025-10-30
 */
@Mapper
public interface ValidationResultDao {
    
    /**
     * 保存校验结果
     */
    @Insert("INSERT INTO test_case_set_validation_result " +
            "(test_case_set_id, task_id, validation_result, created_time, updated_time) " +
            "VALUES (#{testCaseSetId}, #{taskId}, #{validationResult}::jsonb, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
            "ON CONFLICT (test_case_set_id) DO UPDATE " +
            "SET task_id = #{taskId}, validation_result = #{validationResult}::jsonb, updated_time = CURRENT_TIMESTAMP")
    int save(@Param("testCaseSetId") Long testCaseSetId,
            @Param("taskId") String taskId,
            @Param("validationResult") String validationResult);
    
    /**
     * 根据用例集ID查询校验结果
     */
    @Select("SELECT validation_result FROM test_case_set_validation_result " +
            "WHERE test_case_set_id = #{testCaseSetId}")
    String findByTestCaseSetId(Long testCaseSetId);
    
    /**
     * 删除校验结果
     */
    @Delete("DELETE FROM test_case_set_validation_result WHERE test_case_set_id = #{testCaseSetId}")
    int deleteByTestCaseSetId(Long testCaseSetId);
}

