package com.huawei.cloududn.finetuningapp.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 清洗规则配置实体（对应 t_cleaning_rule 表）
 *
 * @author FinetuningApp Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CleaningRuleEntity {

    /**
     * 规则 ID
     */
    private Long id;

    /**
     * 规则名称
     */
    private String name;

    /**
     * 适用的应用类型 (e.g., DouYin_Live)
     */
    private String appType;

    /**
     * 是否为该应用的默认规则
     */
    private Boolean isDefault;

    /**
     * 核心规则体 (JSON 格式字符串)
     */
    private String ruleConfig;

    /**
     * 描述
     */
    private String description;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
