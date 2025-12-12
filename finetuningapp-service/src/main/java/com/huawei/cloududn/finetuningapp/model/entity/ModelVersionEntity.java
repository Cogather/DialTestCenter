package com.huawei.cloududn.finetuningapp.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 模型/检索库版本实体（对应 t_model_version 表）
 *
 * @author FinetuningApp Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelVersionEntity {

    /**
     * 版本 ID
     */
    private Long id;

    /**
     * 版本号 (e.g., v20231027.1)
     */
    private String versionNo;

    /**
     * 模型类型: SMALL_MODEL, RAG_LIB
     */
    private String modelType;

    /**
     * DFS/S3 物理路径
     */
    private String storagePath;

    /**
     * 质量指标快照 - JSON字符串
     */
    private String metrics;

    /**
     * 来源任务 ID
     */
    private Long sourceTaskId;

    /**
     * 是否为当前推荐版本
     */
    private Boolean isActive;

    /**
     * 生成时间
     */
    private LocalDateTime createTime;

    /**
     * 模型类型枚举
     */
    public enum ModelType {
        SMALL_MODEL,
        RAG_LIB
    }
}
