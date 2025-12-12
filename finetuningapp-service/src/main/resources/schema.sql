-- Fine-tuning Application Service - Database Schema
-- PostgreSQL DDL Script
-- Version: 1.0.0
-- Description: 微调中心应用服务数据库表结构

-- ========================================
-- 1. 任务流水表 (t_finetuning_task)
-- 用途：既是业务流水日志，也是分布式任务队列
-- ========================================
CREATE TABLE IF NOT EXISTS t_finetuning_task
(
    id                BIGSERIAL PRIMARY KEY,
    dialing_task_id   BIGINT,
    job_type          VARCHAR(32)  NOT NULL,
    status            VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    request_params    JSONB,
    result_data       JSONB,
    worker_instance   VARCHAR(64),
    heartbeat_time    TIMESTAMP,
    retry_count       INTEGER               DEFAULT 0,
    error_msg         TEXT,
    create_time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    start_time        TIMESTAMP,
    end_time          TIMESTAMP,
    CONSTRAINT chk_job_type CHECK (job_type IN ('PREPROCESS', 'TRAIN', 'REPLAY')),
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'RUNNING', 'SUCCESS', 'FAILED', 'CANCELED'))
);

-- 索引：用于 Poller 扫描和 Watchdog 监控
CREATE INDEX idx_status_heartbeat ON t_finetuning_task (status, heartbeat_time);
CREATE INDEX idx_dialing_task_id ON t_finetuning_task (dialing_task_id);
CREATE INDEX idx_create_time ON t_finetuning_task (create_time DESC);

-- 注释
COMMENT ON TABLE t_finetuning_task IS '微调任务流水表（分布式任务队列）';
COMMENT ON COLUMN t_finetuning_task.id IS '任务唯一标识 (Job ID)';
COMMENT ON COLUMN t_finetuning_task.dialing_task_id IS '关联的拨测控制中心任务 ID';
COMMENT ON COLUMN t_finetuning_task.job_type IS '任务类型: PREPROCESS/TRAIN/REPLAY';
COMMENT ON COLUMN t_finetuning_task.status IS '任务状态: PENDING/RUNNING/SUCCESS/FAILED/CANCELED';
COMMENT ON COLUMN t_finetuning_task.request_params IS '任务入参快照 (UE, TimeRange, TempRule)';
COMMENT ON COLUMN t_finetuning_task.result_data IS '任务产出 (Stats, ModelPath)';
COMMENT ON COLUMN t_finetuning_task.worker_instance IS '当前抢占该任务的服务实例 ID';
COMMENT ON COLUMN t_finetuning_task.heartbeat_time IS '任务最后一次保活心跳时间';
COMMENT ON COLUMN t_finetuning_task.retry_count IS '重试次数';

-- ========================================
-- 2. 清洗规则配置表 (t_cleaning_rule)
-- 用途：存储数据清洗和标注的逻辑规则
-- ========================================
CREATE TABLE IF NOT EXISTS t_cleaning_rule
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(64)  NOT NULL,
    app_type    VARCHAR(64)  NOT NULL,
    is_default  BOOLEAN               DEFAULT FALSE,
    rule_config JSONB        NOT NULL,
    description VARCHAR(255),
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_app_type ON t_cleaning_rule (app_type);
CREATE INDEX idx_is_default ON t_cleaning_rule (is_default);

-- 注释
COMMENT ON TABLE t_cleaning_rule IS '清洗规则配置表';
COMMENT ON COLUMN t_cleaning_rule.name IS '规则名称';
COMMENT ON COLUMN t_cleaning_rule.app_type IS '适用的应用类型 (e.g., DouYin_Live)';
COMMENT ON COLUMN t_cleaning_rule.is_default IS '是否为该应用的默认规则';
COMMENT ON COLUMN t_cleaning_rule.rule_config IS '核心规则体 (JSON 格式)';

-- ========================================
-- 3. 模型/检索库版本表 (t_model_version)
-- 用途：管理生成的资产（模型文件或向量检索库）
-- ========================================
CREATE TABLE IF NOT EXISTS t_model_version
(
    id             BIGSERIAL PRIMARY KEY,
    version_no     VARCHAR(64)  NOT NULL UNIQUE,
    model_type     VARCHAR(32)  NOT NULL,
    storage_path   VARCHAR(255) NOT NULL,
    metrics        JSONB,
    source_task_id BIGINT,
    is_active      BOOLEAN               DEFAULT FALSE,
    create_time    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_model_type CHECK (model_type IN ('SMALL_MODEL', 'RAG_LIB'))
);

-- 索引
CREATE INDEX idx_version_no ON t_model_version (version_no);
CREATE INDEX idx_source_task_id ON t_model_version (source_task_id);
CREATE INDEX idx_is_active ON t_model_version (is_active);
CREATE INDEX idx_create_time_model ON t_model_version (create_time DESC);

-- 注释
COMMENT ON TABLE t_model_version IS '模型/检索库版本表';
COMMENT ON COLUMN t_model_version.version_no IS '版本号 (e.g., v20231027.1)';
COMMENT ON COLUMN t_model_version.model_type IS '模型类型: SMALL_MODEL/RAG_LIB';
COMMENT ON COLUMN t_model_version.storage_path IS 'DFS/S3 物理路径';
COMMENT ON COLUMN t_model_version.metrics IS '质量指标快照';
COMMENT ON COLUMN t_model_version.source_task_id IS '来源任务 ID';
COMMENT ON COLUMN t_model_version.is_active IS '是否为当前推荐版本';

-- ========================================
-- 4. 样本统计表 (t_sample_statistics)
-- 用途：记录各类应用的样本积累情况（聚合结果）
-- ========================================
CREATE TABLE IF NOT EXISTS t_sample_statistics
(
    id               BIGSERIAL PRIMARY KEY,
    app_type         VARCHAR(64) NOT NULL UNIQUE,
    total_count      BIGINT               DEFAULT 0,
    last_update_time TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE UNIQUE INDEX idx_app_type_stats ON t_sample_statistics (app_type);

-- 注释
COMMENT ON TABLE t_sample_statistics IS '样本统计表';
COMMENT ON COLUMN t_sample_statistics.app_type IS '应用类型';
COMMENT ON COLUMN t_sample_statistics.total_count IS '累计样本总数';
COMMENT ON COLUMN t_sample_statistics.last_update_time IS '最后更新时间';

-- ========================================
-- 初始化数据
-- ========================================

-- 插入默认清洗规则示例
INSERT INTO t_cleaning_rule (name, app_type, is_default, rule_config, description)
VALUES ('DouYin_Live Default Rule',
        'DouYin_Live',
        true,
        '{
          "protos": [
            {
              "id": "douyin_live_001",
              "name": "DouYin_Live"
            }
          ],
          "clean_rule": {
            "min_traffic": 10240,
            "min_duration": 5,
            "pure_ratio": 0.8
          },
          "label_proto_rule": [
            {
              "rule_type": "top_flow",
              "top": 10,
              "to_proto_id": "douyin_live_001",
              "aggre_by": [
                "l4proto",
                "dstip",
                "srcport"
              ],
              "condition": {
                "traffic": 100,
                "effective_proportion": 0.6,
                "domain_regex": ".*\\.douyin\\.com"
              }
            }
          ]
        }',
        '抖音直播默认清洗规则')
ON CONFLICT DO NOTHING;

-- 插入初始样本统计数据
INSERT INTO t_sample_statistics (app_type, total_count)
VALUES ('DouYin_Live', 0),
       ('WeChat_Video', 0),
       ('TikTok_Live', 0)
ON CONFLICT DO NOTHING;
