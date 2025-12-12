package com.huawei.cloududn.finetuningapp.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 微调任务实体（对应 t_finetuning_task 表）
 * 既是业务流水日志，也是分布式任务队列
 *
 * @author FinetuningApp Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinetuningTaskEntity {

    /**
     * 任务唯一标识 (Job ID)
     */
    private Long id;

    /**
     * 关联的拨测控制中心任务 ID
     */
    private Long dialingTaskId;

    /**
     * 任务类型: PREPROCESS, TRAIN, REPLAY
     */
    private String jobType;

    /**
     * 任务状态: PENDING, RUNNING, SUCCESS, FAILED, CANCELED
     */
    private String status;

    /**
     * 任务入参快照 (UE, TimeRange, TempRule) - JSON字符串
     */
    private String requestParams;

    /**
     * 任务产出 (Stats, ModelPath) - JSON字符串
     */
    private String resultData;

    /**
     * 当前抢占该任务的服务实例 ID (e.g., pod-ip:port)
     */
    private String workerInstance;

    /**
     * 任务最后一次保活心跳时间
     */
    private LocalDateTime heartbeatTime;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 失败堆栈
     */
    private String errorMsg;

    /**
     * 入列时间
     */
    private LocalDateTime createTime;

    /**
     * 开始执行时间
     */
    private LocalDateTime startTime;

    /**
     * 完成时间
     */
    private LocalDateTime endTime;

    /**
     * 任务类型枚举
     */
    public enum JobType {
        PREPROCESS,
        TRAIN,
        REPLAY
    }

    /**
     * 任务状态枚举
     */
    public enum Status {
        PENDING,
        RUNNING,
        SUCCESS,
        FAILED,
        CANCELED
    }
}
