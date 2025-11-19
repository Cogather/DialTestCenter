/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.dao.basicDataManage;

import com.huawei.cloududn.dialingtestapp.entity.basicDataManage.SoftwarePackage;
import com.huawei.cloududn.dialingtestapp.model.SoftwarePackageFileContent;
import com.huawei.cloududn.dialingtest.model.SoftwarePackageInfo;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 软件包数据访问对象
 * 
 * @author Generated
 */
@Mapper
public interface SoftwarePackageDao {
    
    /**
     * 插入软件包
     */
    @Insert("INSERT INTO software_package (software_name, description, file_content, file_sha256, file_size) " +
            "VALUES (#{softwareName}, #{description}, #{fileContent,jdbcType=BINARY}, #{fileSha256}, #{fileSize})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SoftwarePackage softwarePackage);
    
    /**
     * 检查软件名称是否存在
     */
    @Select("SELECT COUNT(*) FROM software_package WHERE software_name = #{softwareName}")
    int checkSoftwareNameExists(String softwareName);
    
    /**
     * 分页查询软件包列表
     */
    @Select("<script>" +
            "SELECT id, software_name, description, file_size, file_sha256 " +
            "FROM software_package " +
            "<where>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (software_name LIKE CONCAT('%', #{keyword}, '%') OR description LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if>" +
            "</where>" +
            "ORDER BY id DESC " +
            "LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "softwareName", column = "software_name"),
            @Result(property = "description", column = "description"),
            @Result(property = "fileSize", column = "file_size"),
            @Result(property = "fileSha256", column = "file_sha256")
    })
    List<SoftwarePackageInfo> getSoftwarePackageList(@Param("keyword") String keyword, 
                                                    @Param("offset") int offset, 
                                                    @Param("limit") int limit);
    
    /**
     * 统计软件包总数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM software_package " +
            "<where>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (software_name LIKE CONCAT('%', #{keyword}, '%') OR description LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if>" +
            "</where>" +
            "</script>")
    int countSoftwarePackageCount(@Param("keyword") String keyword);
    
    /**
     * 根据ID获取软件包详细信息
     */
    @Select("SELECT id, software_name, description, file_size, file_sha256 FROM software_package WHERE id = #{id}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "softwareName", column = "software_name"),
            @Result(property = "description", column = "description"),
            @Result(property = "fileSize", column = "file_size"),
            @Result(property = "fileSha256", column = "file_sha256")
    })
    SoftwarePackageInfo getSoftwarePackageById(Long id);
    
    /**
     * 根据软件名称获取软件包详细信息
     */
    @Select("SELECT id, software_name, description, file_size, file_sha256 " +
            "FROM software_package WHERE software_name = #{softwareName}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "softwareName", column = "software_name"),
            @Result(property = "description", column = "description"),
            @Result(property = "fileSize", column = "file_size"),
            @Result(property = "fileSha256", column = "file_sha256")
    })
    SoftwarePackageInfo getSoftwarePackageByName(String softwareName);
    
    /**
     * 根据软件名称查询软件包（含file_content）
     * 用于APP推送到执行机
     *
     * @param softwareName 软件名称
     * @return 软件包完整信息，包含文件内容
     */
    @Select("SELECT * FROM software_package WHERE software_name = #{softwareName}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "softwareName", column = "software_name"),
            @Result(property = "description", column = "description"),
            @Result(property = "fileContent", column = "file_content",
                    javaType = byte[].class,
                    jdbcType = org.apache.ibatis.type.JdbcType.BINARY),
            @Result(property = "fileSha256", column = "file_sha256"),
            @Result(property = "fileSize", column = "file_size")
    })
    SoftwarePackage findBySoftwareName(String softwareName);
    
    /**
     * 根据ID获取软件包文件内容（封装对象）
     */
    @Select("SELECT file_content FROM software_package WHERE id = #{id}")
    @Results({
            @Result(property = "fileContent", column = "file_content",
                    javaType = byte[].class,
                    jdbcType = org.apache.ibatis.type.JdbcType.BINARY)
    })
    SoftwarePackageFileContent selectSoftwarePackageFileContent(Long id);

    /**
     * 简化方法：直接返回字节数组
     */
    default byte[] getSoftwarePackageFileContent(Long id) {
        SoftwarePackageFileContent row = selectSoftwarePackageFileContent(id);
        return row != null ? row.getFileContent() : null;
    }
    
    /**
     * 更新软件包描述
     */
    @Update("UPDATE software_package SET description = #{description} WHERE id = #{id}")
    int updateSoftwarePackageDescription(@Param("id") Long id, @Param("description") String description);
    
    /**
     * 删除软件包
     */
    @Delete("DELETE FROM software_package WHERE id = #{id}")
    int deleteSoftwarePackage(Long id);
    
    /**
     * 根据软件名称删除软件包
     */
    @Delete("DELETE FROM software_package WHERE software_name = #{softwareName}")
    int deleteSoftwarePackageByName(String softwareName);
    
    /**
     * 检查软件包是否被用例集引用
     */
    @Select("SELECT COUNT(*) FROM test_case WHERE dependencies_package LIKE CONCAT('%', #{softwareName}, '%')")
    int isSoftwarePackageReferenced(String softwareName);
}