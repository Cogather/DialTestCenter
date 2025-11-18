package com.huawei.cloududn.dialingtestapp.dao;

import com.huawei.cloududn.dialingtest.model.TestCase;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 测试用例数据访问对象
 * 
 * @author Generated
 */
@Mapper
public interface TestCaseDao {
    
    /**
     * 插入测试用例
     */
    @Insert("INSERT INTO test_case (test_case_set_id, case_name, case_number, test_steps, expected_result, " +
            "business_category, app_name, dependencies_package, dependencies_rule, environment_config, script_exists) " +
            "VALUES (#{testCaseSetId}, #{caseName}, #{caseNumber}, #{testSteps}, #{expectedResult}, " +
            "#{businessCategory}, #{appName}, #{dependenciesPackage}, #{dependenciesRule}, #{environmentConfig}, #{scriptExists})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TestCase testCase);
    
    /**
     * 批量插入测试用例
     */
    @Insert("<script>" +
            "INSERT INTO test_case (test_case_set_id, case_name, case_number, test_steps, expected_result, " +
            "business_category, app_name, dependencies_package, dependencies_rule, environment_config, script_exists) VALUES " +
            "<foreach collection='testCases' item='testCase' separator=','>" +
            "(#{testCase.testCaseSetId}, #{testCase.caseName}, #{testCase.caseNumber}, #{testCase.testSteps}, " +
            "#{testCase.expectedResult}, #{testCase.businessCategory}, #{testCase.appName}, #{testCase.dependenciesPackage}, " +
            "#{testCase.dependenciesRule}, #{testCase.environmentConfig}, #{testCase.scriptExists})" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("testCases") List<TestCase> testCases);
    
    /**
     * 根据用例集ID分页查询测试用例
     */
    @Select("SELECT * FROM test_case " +
            "WHERE test_case_set_id = #{testCaseSetId} ORDER BY id " +
            "LIMIT #{pageSize} OFFSET #{offset}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "testCaseSetId", column = "test_case_set_id"),
        @Result(property = "caseName", column = "case_name"),
        @Result(property = "caseNumber", column = "case_number"),
        @Result(property = "testSteps", column = "test_steps"),
        @Result(property = "expectedResult", column = "expected_result"),
        @Result(property = "businessCategory", column = "business_category"),
        @Result(property = "appName", column = "app_name"),
        @Result(property = "dependenciesPackage", column = "dependencies_package"),
        @Result(property = "dependenciesRule", column = "dependencies_rule"),
        @Result(property = "environmentConfig", column = "environment_config"),
        @Result(property = "scriptExists", column = "script_exists")
    })
    List<TestCase> findByTestCaseSetIdWithPagination(@Param("testCaseSetId") Long testCaseSetId, 
                                                     @Param("offset") int offset, 
                                                     @Param("pageSize") int pageSize);
    
    /**
     * 统计用例集下的测试用例总数
     */
    @Select("SELECT COUNT(*) FROM test_case WHERE test_case_set_id = #{testCaseSetId}")
    long countByTestCaseSetId(Long testCaseSetId);

    /**
     * 根据用例集ID查询全部测试用例
     */
    @Select("SELECT * FROM test_case WHERE test_case_set_id = #{testCaseSetId} ORDER BY id")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "testCaseSetId", column = "test_case_set_id"),
        @Result(property = "caseName", column = "case_name"),
        @Result(property = "caseNumber", column = "case_number"),
        @Result(property = "testSteps", column = "test_steps"),
        @Result(property = "expectedResult", column = "expected_result"),
        @Result(property = "businessCategory", column = "business_category"),
        @Result(property = "appName", column = "app_name"),
        @Result(property = "dependenciesPackage", column = "dependencies_package"),
        @Result(property = "dependenciesRule", column = "dependencies_rule"),
        @Result(property = "environmentConfig", column = "environment_config"),
        @Result(property = "scriptExists", column = "script_exists")
    })
    List<TestCase> findAllByTestCaseSetId(@Param("testCaseSetId") Long testCaseSetId);
    
    /**
     * 根据用例集ID删除测试用例
     */
    @Delete("DELETE FROM test_case WHERE test_case_set_id = #{testCaseSetId}")
    int deleteByTestCaseSetId(Long testCaseSetId);
    
    /**
     * 根据ID查询测试用例
     */
    @Select("SELECT * FROM test_case WHERE id = #{id}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "testCaseSetId", column = "test_case_set_id"),
        @Result(property = "caseName", column = "case_name"),
        @Result(property = "caseNumber", column = "case_number"),
        @Result(property = "testSteps", column = "test_steps"),
        @Result(property = "expectedResult", column = "expected_result"),
        @Result(property = "businessCategory", column = "business_category"),
        @Result(property = "appName", column = "app_name"),
        @Result(property = "dependenciesPackage", column = "dependencies_package"),
        @Result(property = "dependenciesRule", column = "dependencies_rule"),
        @Result(property = "environmentConfig", column = "environment_config"),
        @Result(property = "scriptExists", column = "script_exists")
    })
    TestCase findById(Long id);
}
