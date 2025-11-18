/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.dao.taskmanagement;

import com.huawei.cloududn.dialingtest.model.TaskEntity;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 任务表DAO（表8-16）。基于MyBatis注解实现。
 *
 * @author g00940940
 * @since 2025-10-24
 */
@Mapper
public interface TaskDao {

    @Insert({
        "INSERT INTO task (main_task_id, parent_task_id, creator, status, result, " +
        "input, output, context, template_task_id)",
        "VALUES (#{mainTaskId}, #{parentTaskId}, #{creator}, #{status}, #{result}, " +
        "#{input}, #{output}, #{context}, #{templateTaskId})"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TaskEntity task);

    @Update({
        "UPDATE task SET status=#{status}, result=#{result}, context=#{context},",
        " end_time=CASE WHEN #{status} <> 'RUNNING' THEN now() ELSE end_time END WHERE id=#{id}"
    })
    int updateStatusAndContext(@Param("id") Long id,
                               @Param("status") String status,
                               @Param("result") String result,
                               @Param("context") String context);

    @Update({
        "UPDATE task SET main_task_id=id WHERE id=#{id} AND main_task_id IS NULL"
    })
    int initMainTaskId(@Param("id") Long id);

    @Select({
        "SELECT id, main_task_id as mainTaskId, parent_task_id as parentTaskId, creator, ",
        "TO_CHAR(start_time, 'YYYY-MM-DD\"T\"HH24:MI:SS.MS\"Z\"') as startTime, ",
        "TO_CHAR(end_time, 'YYYY-MM-DD\"T\"HH24:MI:SS.MS\"Z\"') as endTime, ",
        "status, result, input, output, context, template_task_id as templateTaskId ",
        "FROM task WHERE id=#{id}"
    })
    TaskEntity findById(@Param("id") Long id);

    @Select({
        "SELECT id, main_task_id as mainTaskId, parent_task_id as parentTaskId, creator, ",
        "TO_CHAR(start_time, 'YYYY-MM-DD\"T\"HH24:MI:SS.MS\"Z\"') as startTime, ",
        "TO_CHAR(end_time, 'YYYY-MM-DD\"T\"HH24:MI:SS.MS\"Z\"') as endTime, ",
        "status, result, input, output, context, template_task_id as templateTaskId ",
        "FROM task WHERE main_task_id=id ORDER BY id DESC LIMIT #{size} OFFSET #{offset}"
    })
    List<TaskEntity> findMainTasks(@Param("offset") int offset, @Param("size") int size);

    @Select({
        "SELECT COUNT(*) FROM task WHERE main_task_id=id"
    })
    long countMainTasks();

    @Select({
        "SELECT id, main_task_id as mainTaskId, parent_task_id as parentTaskId, creator, ",
        "TO_CHAR(start_time, 'YYYY-MM-DD\"T\"HH24:MI:SS.MS\"Z\"') as startTime, ",
        "TO_CHAR(end_time, 'YYYY-MM-DD\"T\"HH24:MI:SS.MS\"Z\"') as endTime, ",
        "status, result, input, output, context, template_task_id as templateTaskId ",
        "FROM task WHERE (main_task_id=#{mainTaskId} OR parent_task_id=#{mainTaskId}) " +
        "AND id<>#{mainTaskId} ORDER BY start_time ASC"
    })
    List<TaskEntity> findSubTasksByMainTaskId(@Param("mainTaskId") Long mainTaskId);
}


