/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.dao.taskmanagement;

import com.huawei.cloududn.dialingtest.model.TemplateEntity;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 模板任务表DAO（表8-17）。
 *
 * @author g00940940
 * @since 2025-10-24
 */
@Mapper
public interface TemplateDao {

    @Insert({
        "INSERT INTO template_task (name, cron, is_enabled, input, creator, description)",
        "VALUES (#{name}, #{cron}, #{enabled}, #{input}, #{creator}, #{description})"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TemplateEntity template);

    @Update({
        "UPDATE template_task SET name=#{name}, cron=#{cron}, is_enabled=#{enabled}, " +
        "input=#{input}, description=#{description}, update_time=now() WHERE id=#{id}"
    })
    int update(TemplateEntity template);

    @Delete("DELETE FROM template_task WHERE id=#{id}")
    int delete(@Param("id") Long id);

    @Select({
        "SELECT id, name, cron, is_enabled as enabled, input, creator, ",
        "TO_CHAR(create_time, 'YYYY-MM-DD\"T\"HH24:MI:SS.MS\"Z\"') as createTime, ",
        "TO_CHAR(update_time, 'YYYY-MM-DD\"T\"HH24:MI:SS.MS\"Z\"') as updateTime, ",
        "description FROM template_task WHERE id=#{id}"
    })
    TemplateEntity findById(@Param("id") Long id);

    @Select({
        "SELECT id, name, cron, is_enabled as enabled, input, creator, ",
        "TO_CHAR(create_time, 'YYYY-MM-DD\"T\"HH24:MI:SS.MS\"Z\"') as createTime, ",
        "TO_CHAR(update_time, 'YYYY-MM-DD\"T\"HH24:MI:SS.MS\"Z\"') as updateTime, ",
        "description FROM template_task ORDER BY id DESC"
    })
    List<TemplateEntity> findAll();

    @Select({
        "SELECT id, name, cron, is_enabled as enabled, input, creator, ",
        "TO_CHAR(create_time, 'YYYY-MM-DD\"T\"HH24:MI:SS.MS\"Z\"') as createTime, ",
        "TO_CHAR(update_time, 'YYYY-MM-DD\"T\"HH24:MI:SS.MS\"Z\"') as updateTime, ",
        "description FROM template_task WHERE is_enabled=true ORDER BY id DESC"
    })
    List<TemplateEntity> findEnabled();
}


