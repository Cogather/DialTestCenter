package com.huawei.cloududn.finetuningapp.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 样本统计实体（对应 t_sample_statistics 表）
 *
 * @author FinetuningApp Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SampleStatisticsEntity {

    /**
     * ID
     */
    private Long id;

    /**
     * 应用类型
     */
    private String appType;

    /**
     * 累计样本总数
     */
    private Long totalCount;

    /**
     * 最后更新时间
     */
    private LocalDateTime lastUpdateTime;
}
