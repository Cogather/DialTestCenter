package com.huawei.cloududn.dialingtestapp.dao.basicDataManage;

import com.huawei.cloududn.dialingtest.model.TestCaseSet;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 用例集数据访问对象
 * 
 * @author Generated
 */
@Mapper
public interface TestCaseSetDao {
    
    /**
     * 插入用例集
     */
    @Insert("INSERT INTO test_case_set " +
            "(name, version, file_content, file_size, description, sha256, business_zh, business_en) " +
            "VALUES (#{name}, #{version}, #{fileContent}, #{fileSize}, " +
            "#{description}, #{sha256}, #{businessZh}, #{businessEn})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TestCaseSet testCaseSet);
    
    /**
     * 根据ID查询用例集
     */
    @Select("SELECT * FROM test_case_set WHERE id = #{id}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "name", column = "name"),
        @Result(property = "version", column = "version"),
        @Result(property = "fileContent", column = "file_content"),
        @Result(property = "fileSize", column = "file_size"),
        @Result(property = "description", column = "description"),
        @Result(property = "sha256", column = "sha256"),
        @Result(property = "businessZh", column = "business_zh"),
        @Result(property = "businessEn", column = "business_en")
    })
    TestCaseSet findById(Long id);
    
    /**
     * 分页查询用例集列表
     */
    @Select("SELECT * FROM test_case_set ORDER BY id DESC LIMIT #{pageSize} OFFSET #{offset}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "name", column = "name"),
        @Result(property = "version", column = "version"),
        @Result(property = "fileContent", column = "file_content"),
        @Result(property = "fileSize", column = "file_size"),
        @Result(property = "description", column = "description"),
        @Result(property = "sha256", column = "sha256"),
        @Result(property = "businessZh", column = "business_zh"),
        @Result(property = "businessEn", column = "business_en")
    })
    List<TestCaseSet> findWithPagination(@Param("offset") int offset, @Param("pageSize") int pageSize);
    
    /**
     * 统计用例集总数
     */
    @Select("SELECT COUNT(*) FROM test_case_set")
    long count();
    
    /**
     * 根据名称和版本查询用例集
     */
    @Select("SELECT * FROM test_case_set WHERE name = #{name} AND version = #{version}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "name", column = "name"),
        @Result(property = "version", column = "version"),
        @Result(property = "fileContent", column = "file_content"),
        @Result(property = "fileSize", column = "file_size"),
        @Result(property = "description", column = "description"),
        @Result(property = "sha256", column = "sha256"),
        @Result(property = "businessZh", column = "business_zh"),
        @Result(property = "businessEn", column = "business_en")
    })
    TestCaseSet findByNameAndVersion(@Param("name") String name, @Param("version") String version);
    
    /**
     * 更新用例集
     */
    @Update("UPDATE test_case_set " +
            "SET description = #{description}, business_zh = #{businessZh}, business_en = #{businessEn} " +
            "WHERE id = #{id}")
    int update(TestCaseSet testCaseSet);
    
    /**
     * 删除用例集
     */
    @Delete("DELETE FROM test_case_set WHERE id = #{id}")
    int deleteById(Long id);
    
    /**
     * 检查名称和版本是否存在
     */
    @Select("SELECT COUNT(*) FROM test_case_set WHERE name = #{name} AND version = #{version}")
    int existsByNameAndVersion(@Param("name") String name, @Param("version") String version);
    
    /**
     * 检查名称和版本是否存在（排除指定ID）
     */
    @Select("SELECT COUNT(*) FROM test_case_set WHERE name = #{name} AND version = #{version} AND id != #{id}")
    int existsByNameAndVersionExcludeId(@Param("name") String name, @Param("version") String version, @Param("id") Long id);
}
