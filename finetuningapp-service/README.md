# Fine-tuning Application Service

微调中心应用服务 - 业务执行引擎

## 项目概述

`finetuningapp-service` 是微调中心的业务执行引擎，作为原子能力提供者，负责对接拨测控制中心指令，调度底层 C++ 服务（ProtocolMiner），并采用"流式批处理"模式回放 Kafka 历史数据进行业务清洗。

### 核心职责

1. **原子任务执行**：响应北向指令，执行预处理、训练、回放等原子任务
2. **离线回放与清洗**：按需回放 Kafka 历史数据，在内存中执行流量特征分析与样本清洗
3. **资产版本管理**：管理模型、检索库的版本元数据及样本库统计信息

## 技术栈

- **Java**: OpenJDK 21
- **框架**: Spring Boot 2.7.18
- **持久层**: MyBatis 2.3.2
- **数据库**: PostgreSQL
- **消息队列**: Apache Kafka
- **缓存**: Caffeine (内存缓存)
- **构建工具**: Maven 3.8+

## 项目结构

```
finetuningapp-service/
├── src/main/java/com/huawei/cloududn/finetuningapp/
│   ├── FinetuningApplication.java          # 启动类
│   ├── common/                             # 通用工具类
│   │   ├── Result.java                     # 统一响应封装
│   │   ├── BusinessException.java          # 业务异常
│   │   └── GlobalExceptionHandler.java     # 全局异常处理
│   ├── controller/                         # 控制器层
│   ├── service/                            # 业务逻辑层
│   │   ├── scheduler/                      # 调度器
│   │   ├── executor/                       # 任务执行器
│   │   ├── engine/                         # 清洗引擎
│   │   ├── asset/                          # 资产管理
│   │   └── external/                       # 外部系统适配
│   ├── dao/                                # 数据访问层
│   ├── model/                              # 数据模型
│   │   ├── entity/                         # 实体类
│   │   ├── kafka/                          # Kafka消息定义
│   │   └── domain/                         # 领域模型
│   └── infra/                              # 基础设施
│       ├── kafka/                          # Kafka配置
│       └── config/                         # 配置类
├── src/main/resources/
│   ├── application.yml                      # 应用配置
│   ├── log4j2.xml                          # 日志配置
│   ├── schema.sql                          # 数据库表结构
│   └── mapper/                             # MyBatis映射文件
└── doc/                                     # 设计文档
    ├── service-design.md                    # 服务设计说明书
    └── technology-selection.md              # 技术选型说明书
```

## 快速开始

### 前置条件

- JDK 21+
- Maven 3.8+
- PostgreSQL 12+
- Apache Kafka 3.3+

### 构建项目

```bash
mvn clean install
```

### 运行应用

```bash
mvn spring-boot:run
```

### 访问 Swagger 文档

启动后访问：http://localhost:8081/swagger-ui.html

### 健康检查

http://localhost:8081/actuator/health

## 数据库初始化

执行 `src/main/resources/schema.sql` 创建数据库表：

```bash
psql -U postgres -d finetuning_db -f src/main/resources/schema.sql
```

## 核心模块说明

### 1. 分布式任务调度

采用基于数据库的"Pull-Based"抢占模型：
- **TaskPoller**: 每5秒扫描一次PENDING任务，使用 `FOR UPDATE SKIP LOCKED` 抢占
- **TaskWatchdog**: 监控运行中任务的心跳，防止死锁

### 2. Kafka 回放机制

使用 `assign` + `seek` 模式精确读取指定 Partition 的历史数据，而非传统的 Group Consumer。

### 3. 内存清洗引擎

流式处理模式，边读取边聚合：
- TopFlow 排序基于聚合后的统计特征
- 蓄水池采样保留有限采样明细
- 内存熔断保护机制

### 4. 缓存策略

使用 Caffeine 作为应用内存缓存：
- 清洗规则配置缓存
- 热点数据缓存
- 启动时加载，支持热更新

## 配置说明

主要配置项在 `application.yml` 中：

- **数据库连接**: `spring.datasource.*`
- **Kafka配置**: `spring.kafka.*`
- **外部服务**: `app.dialing-service.*` 和 `app.protocol-miner.*`
- **调度配置**: `app.scheduler.*`

## 测试

运行单元测试：

```bash
mvn test
```

生成测试覆盖率报告：

```bash
mvn jacoco:report
```

报告位置：`target/site/jacoco/index.html`

## 参考文档

详细设计文档请参阅：
- [服务设计说明书](doc/service-design.md)
- [技术选型说明书](doc/technology-selection.md)

## 许可证

Copyright © 2024 Huawei Cloud UDN Team
