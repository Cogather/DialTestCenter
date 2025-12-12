package com.huawei.cloududn.finetuningapp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Fine-tuning Application Service - Main Entry Point
 *
 * 微调中心应用服务 - 业务执行引擎
 *
 * 核心职责：
 * 1. 原子任务执行：响应北向指令，执行预处理、训练、回放等原子任务
 * 2. 离线回放与清洗：按需回放 Kafka 历史数据，在内存中执行流量特征分析与样本清洗
 * 3. 资产版本管理：管理模型、检索库的版本元数据及样本库统计信息
 *
 * @author FinetuningApp Team
 * @since 1.0.0
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableCaching
@EnableKafka
@EnableTransactionManagement
@MapperScan("com.huawei.cloududn.finetuningapp.dao")
public class FinetuningApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinetuningApplication.class, args);
    }
}
