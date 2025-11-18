package com.huawei.cloududn.dialingtestapp.dao;

import com.huawei.cloududn.dialingtest.model.PreprocessRule;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 预处理规则数据访问对象
 * 
 * @author g00940940
 * @since 2025-01-27
 */
@Mapper
public interface PreprocessRuleDao {
    
    /**
     * 插入预处理规则
     */
    @Insert("INSERT INTO preprocess_rules " +
            "(rule_name, business_zh, business_en, category, app_name, content, is_custom, package_id) " +
            "VALUES (#{ruleName}, #{businessZh}, #{businessEn}, #{category}, " +
            "#{appName}, #{content}, #{isCustom}, #{packageId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PreprocessRule preprocessRule);
    
    /**
     * 批量插入预处理规则
     */
    @Insert("<script>" +
            "INSERT INTO preprocess_rules " +
            "(rule_name, business_zh, business_en, category, app_name, content, is_custom, package_id) " +
            "VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.ruleName}, #{item.businessZh}, #{item.businessEn}, #{item.category}, " +
            "#{item.appName}, #{item.content}, #{item.isCustom}, #{item.packageId})" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("list") List<PreprocessRule> preprocessRules);
    
    /**
     * 根据ID查询预处理规则
     */
    @Select("SELECT * FROM preprocess_rules WHERE id = #{id}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "ruleName", column = "rule_name"),
        @Result(property = "businessZh", column = "business_zh"),
        @Result(property = "businessEn", column = "business_en"),
        @Result(property = "category", column = "category"),
        @Result(property = "appName", column = "app_name"),
        @Result(property = "content", column = "content"),
        @Result(property = "isCustom", column = "is_custom"),
        @Result(property = "packageId", column = "package_id")
    })
    PreprocessRule findById(Long id);
    
    /**
     * 根据条件查询预处理规则列表
     */
    @Select("<script>" +
            "SELECT * FROM preprocess_rules " +
            "<where>" +
            "<if test='businessZh != null and businessZh != \"\"'>" +
            "AND business_zh = #{businessZh} " +
            "</if>" +
            "<if test='category != null and category != \"\"'>" +
            "AND category = #{category} " +
            "</if>" +
            "<if test='appName != null and appName != \"\"'>" +
            "AND app_name = #{appName} " +
            "</if>" +
            "<if test='ruleName != null and ruleName != \"\"'>" +
            "AND rule_name = #{ruleName} " +
            "</if>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (rule_name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR category LIKE CONCAT('%', #{keyword}, '%') " +
            "OR app_name LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "</where>" +
            "ORDER BY id DESC " +
            "LIMIT #{pageSize} OFFSET #{offset}" +
            "</script>")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "ruleName", column = "rule_name"),
        @Result(property = "businessZh", column = "business_zh"),
        @Result(property = "businessEn", column = "business_en"),
        @Result(property = "category", column = "category"),
        @Result(property = "appName", column = "app_name"),
        @Result(property = "content", column = "content"),
        @Result(property = "isCustom", column = "is_custom"),
        @Result(property = "packageId", column = "package_id")
    })
    List<PreprocessRule> findByConditions(@Param("businessZh") String businessZh, 
                                        @Param("category") String category, 
                                        @Param("appName") String appName, 
                                        @Param("ruleName") String ruleName, 
                                        @Param("keyword") String keyword, 
                                        @Param("offset") int offset, 
                                        @Param("pageSize") int pageSize);
    
    /**
     * 根据条件统计预处理规则数量
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM preprocess_rules " +
            "<where>" +
            "<if test='businessZh != null and businessZh != \"\"'>" +
            "AND business_zh = #{businessZh} " +
            "</if>" +
            "<if test='category != null and category != \"\"'>" +
            "AND category = #{category} " +
            "</if>" +
            "<if test='appName != null and appName != \"\"'>" +
            "AND app_name = #{appName} " +
            "</if>" +
            "<if test='ruleName != null and ruleName != \"\"'>" +
            "AND rule_name = #{ruleName} " +
            "</if>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (rule_name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR category LIKE CONCAT('%', #{keyword}, '%') " +
            "OR app_name LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "</where>" +
            "</script>")
    int countByConditions(@Param("businessZh") String businessZh, 
                         @Param("category") String category, 
                         @Param("appName") String appName, 
                         @Param("ruleName") String ruleName, 
                         @Param("keyword") String keyword);
    
    /**
     * 根据包ID删除预处理规则
     */
    @Delete("DELETE FROM preprocess_rules WHERE package_id = #{packageId}")
    int deleteByPackageId(Long packageId);
    
    /**
     * 根据规则名称和业务类型删除预处理规则
     */
    @Delete("DELETE FROM preprocess_rules WHERE rule_name = #{ruleName} AND business_zh = #{businessZh}")
    int deleteByRuleNameAndBusiness(@Param("ruleName") String ruleName, @Param("businessZh") String businessZh);
    
    /**
     * 检查规则名称和业务类型是否已存在
     */
    @Select("SELECT COUNT(*) FROM preprocess_rules WHERE rule_name = #{ruleName} AND business_zh = #{businessZh}")
    int countByRuleNameAndBusiness(@Param("ruleName") String ruleName, @Param("businessZh") String businessZh);
    
    /**
     * 获取所有不重复的业务类型
     */
    @Select("SELECT DISTINCT business_zh FROM preprocess_rules ORDER BY business_zh")
    List<String> findDistinctBusinessTypes();
    
    /**
     * 获取所有不重复的分类
     */
    @Select("SELECT DISTINCT category FROM preprocess_rules ORDER BY category")
    List<String> findDistinctCategories();
    
    /**
     * 根据业务类型获取不重复的分类
     */
    @Select("SELECT DISTINCT category FROM preprocess_rules WHERE business_zh = #{businessZh} ORDER BY category")
    List<String> findDistinctCategoriesByBusiness(String businessZh);
    
    /**
     * 获取所有不重复的应用名称
     */
    @Select("SELECT DISTINCT app_name FROM preprocess_rules ORDER BY app_name")
    List<String> findDistinctAppNames();
    
    /**
     * 根据业务类型获取不重复的应用名称
     */
    @Select("SELECT DISTINCT app_name FROM preprocess_rules WHERE business_zh = #{businessZh} ORDER BY app_name")
    List<String> findDistinctAppNamesByBusiness(String businessZh);
    
    /**
     * 根据分类获取不重复的应用名称
     */
    @Select("SELECT DISTINCT app_name FROM preprocess_rules WHERE category = #{category} ORDER BY app_name")
    List<String> findDistinctAppNamesByCategory(String category);
    
    /**
     * 根据业务类型和分类获取不重复的应用名称
     */
    @Select("SELECT DISTINCT app_name FROM preprocess_rules " +
            "WHERE business_zh = #{businessZh} AND category = #{category} ORDER BY app_name")
    List<String> findDistinctAppNamesByBusinessAndCategory(
            @Param("businessZh") String businessZh, @Param("category") String category);
    
    /**
     * 获取所有不重复的规则名称
     */
    @Select("SELECT DISTINCT rule_name FROM preprocess_rules ORDER BY rule_name")
    List<String> findDistinctRuleNames();
    
    /**
     * 根据业务类型获取不重复的规则名称
     */
    @Select("SELECT DISTINCT rule_name FROM preprocess_rules WHERE business_zh = #{businessZh} ORDER BY rule_name")
    List<String> findDistinctRuleNamesByBusiness(String businessZh);
    
    /**
     * 根据分类获取不重复的规则名称
     */
    @Select("SELECT DISTINCT rule_name FROM preprocess_rules WHERE category = #{category} ORDER BY rule_name")
    List<String> findDistinctRuleNamesByCategory(String category);
    
    /**
     * 根据应用名称获取不重复的规则名称
     */
    @Select("SELECT DISTINCT rule_name FROM preprocess_rules WHERE app_name = #{appName} ORDER BY rule_name")
    List<String> findDistinctRuleNamesByApp(String appName);
    
    /**
     * 根据业务类型和分类获取不重复的规则名称
     */
    @Select("SELECT DISTINCT rule_name FROM preprocess_rules " +
            "WHERE business_zh = #{businessZh} AND category = #{category} ORDER BY rule_name")
    List<String> findDistinctRuleNamesByBusinessAndCategory(
            @Param("businessZh") String businessZh, @Param("category") String category);
    /**
     * 根据规则名称与中文业务类型查询
     */
    @Select("SELECT * FROM preprocess_rules WHERE rule_name = #{ruleName} AND business_zh = #{businessZh} LIMIT 1")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "ruleName", column = "rule_name"),
        @Result(property = "businessZh", column = "business_zh"),
        @Result(property = "businessEn", column = "business_en"),
        @Result(property = "category", column = "category"),
        @Result(property = "appName", column = "app_name"),
        @Result(property = "content", column = "content"),
        @Result(property = "isCustom", column = "is_custom"),
        @Result(property = "packageId", column = "package_id")
    })
    PreprocessRule findByRuleNameAndBusinessZh(@Param("ruleName") String ruleName, @Param("businessZh") String businessZh);

    /**
     * 根据规则名称与英文业务类型查询
     */
    @Select("SELECT * FROM preprocess_rules WHERE rule_name = #{ruleName} AND business_en = #{businessEn} LIMIT 1")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "ruleName", column = "rule_name"),
        @Result(property = "businessZh", column = "business_zh"),
        @Result(property = "businessEn", column = "business_en"),
        @Result(property = "category", column = "category"),
        @Result(property = "appName", column = "app_name"),
        @Result(property = "content", column = "content"),
        @Result(property = "isCustom", column = "is_custom"),
        @Result(property = "packageId", column = "package_id")
    })
    PreprocessRule findByRuleNameAndBusinessEn(
            @Param("ruleName") String ruleName, @Param("businessEn") String businessEn);
    
    /**
     * 根据业务类型和应用名称获取不重复的规则名称
     */
    @Select("SELECT DISTINCT rule_name FROM preprocess_rules " +
            "WHERE business_zh = #{businessZh} AND app_name = #{appName} ORDER BY rule_name")
    List<String> findDistinctRuleNamesByBusinessAndApp(
            @Param("businessZh") String businessZh, @Param("appName") String appName);
    
    /**
     * 根据分类和应用名称获取不重复的规则名称
     */
    @Select("SELECT DISTINCT rule_name FROM preprocess_rules " +
            "WHERE category = #{category} AND app_name = #{appName} ORDER BY rule_name")
    List<String> findDistinctRuleNamesByCategoryAndApp(
            @Param("category") String category, @Param("appName") String appName);
    
    /**
     * 根据业务类型、分类和应用名称获取不重复的规则名称
     */
    @Select("SELECT DISTINCT rule_name FROM preprocess_rules " +
            "WHERE business_zh = #{businessZh} AND category = #{category} AND app_name = #{appName} " +
            "ORDER BY rule_name")
    List<String> findDistinctRuleNamesByBusinessAndCategoryAndApp(
            @Param("businessZh") String businessZh, 
            @Param("category") String category, 
            @Param("appName") String appName);
}

