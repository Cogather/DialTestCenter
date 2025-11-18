package com.huawei.cloududn.dialingtestapp.dao;

import com.huawei.cloududn.dialingtestapp.entity.PreprocessRulePackageEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 预处理规则ZIP包数据访问对象
 * 
 * @author g00940940
 * @since 2025-01-27
 */
@Mapper
public interface PreprocessRulePackageDao {
    
    /**
     * 插入ZIP包
     */
    @Insert("INSERT INTO preprocess_rule_packages " +
            "(package_name, business_zh, business_en, file_content, file_size, description) " +
            "VALUES (#{packageName}, #{businessZh}, #{businessEn}, " +
            "#{fileContent}, #{fileSize}, #{description})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PreprocessRulePackageEntity preprocessRulePackage);
    
    /**
     * 根据ID查询ZIP包
     */
    @Select("SELECT * FROM preprocess_rule_packages WHERE id = #{id}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "packageName", column = "package_name"),
        @Result(property = "businessZh", column = "business_zh"),
        @Result(property = "businessEn", column = "business_en"),
        @Result(property = "fileContent", column = "file_content"),
        @Result(property = "fileSize", column = "file_size"),
        @Result(property = "description", column = "description")
    })
    PreprocessRulePackageEntity findById(Long id);
    
    /**
     * 根据条件查询ZIP包列表
     */
    @Select("<script>" +
            "SELECT * FROM preprocess_rule_packages " +
            "<where>" +
            "<if test='businessZh != null and businessZh != \"\"'>" +
            "AND business_zh = #{businessZh} " +
            "</if>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND package_name LIKE CONCAT('%', #{keyword}, '%') " +
            "</if>" +
            "</where>" +
            "ORDER BY id DESC " +
            "LIMIT #{pageSize} OFFSET #{offset}" +
            "</script>")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "packageName", column = "package_name"),
        @Result(property = "businessZh", column = "business_zh"),
        @Result(property = "businessEn", column = "business_en"),
        @Result(property = "fileContent", column = "file_content"),
        @Result(property = "fileSize", column = "file_size"),
        @Result(property = "description", column = "description")
    })
    List<PreprocessRulePackageEntity> findByConditions(@Param("businessZh") String businessZh, 
                                                      @Param("keyword") String keyword, 
                                                      @Param("offset") int offset, 
                                                      @Param("pageSize") int pageSize);
    
    /**
     * 根据条件统计ZIP包数量
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM preprocess_rule_packages " +
            "<where>" +
            "<if test='businessZh != null and businessZh != \"\"'>" +
            "AND business_zh = #{businessZh} " +
            "</if>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND package_name LIKE CONCAT('%', #{keyword}, '%') " +
            "</if>" +
            "</where>" +
            "</script>")
    int countByConditions(@Param("businessZh") String businessZh, @Param("keyword") String keyword);
    
    /**
     * 根据ID删除ZIP包
     */
    @Delete("DELETE FROM preprocess_rule_packages WHERE id = #{id}")
    int deleteById(Long id);
    
    /**
     * 检查包名和业务类型是否已存在
     */
    @Select("SELECT COUNT(*) FROM preprocess_rule_packages " +
            "WHERE package_name = #{packageName} AND business_zh = #{businessZh}")
    int countByPackageNameAndBusiness(
            @Param("packageName") String packageName, @Param("businessZh") String businessZh);
    
    /**
     * 根据包名和业务类型查询ZIP包
     */
    @Select("SELECT * FROM preprocess_rule_packages WHERE package_name = #{packageName} AND business_zh = #{businessZh}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "packageName", column = "package_name"),
        @Result(property = "businessZh", column = "business_zh"),
        @Result(property = "businessEn", column = "business_en"),
        @Result(property = "fileContent", column = "file_content"),
        @Result(property = "fileSize", column = "file_size"),
        @Result(property = "description", column = "description")
    })
    PreprocessRulePackageEntity findByPackageNameAndBusiness(
            @Param("packageName") String packageName, @Param("businessZh") String businessZh);
    
    /**
     * 更新ZIP包
     */
    @Update("UPDATE preprocess_rule_packages SET business_zh = #{businessZh}, business_en = #{businessEn}, " +
            "file_content = #{fileContent}, file_size = #{fileSize}, description = #{description} WHERE id = #{id}")
    int update(PreprocessRulePackageEntity preprocessRulePackage);
}

