package com.huawei.cloududn.dialingtestapp.dao.executormanagement;

import com.huawei.cloududn.dialingtest.model.Ue;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * DAO for ue table.
 */
@Repository
@Mapper
public interface UeDao {

    @Select("SELECT msisdn, executor_name AS executorName, vendor, os, info, " +
            "task_info AS taskInfo " +
            "FROM ue WHERE executor_name = #{executorName}")
    List<Ue> findByExecutorName(@Param("executorName") String executorName);

    @Insert({
            "INSERT INTO ue(msisdn, executor_name, vendor, os, info, task_info)",
            "VALUES(#{msisdn}, #{executorName}, #{vendor}, #{os}, #{info}, #{taskInfo})",
            "ON CONFLICT (msisdn) DO UPDATE SET",
            "executor_name = EXCLUDED.executor_name,",
            "vendor = EXCLUDED.vendor,",
            "os = EXCLUDED.os,",
            "info = EXCLUDED.info,",
            "task_info = EXCLUDED.task_info"
    })
    int upsert(Ue entity);
}


