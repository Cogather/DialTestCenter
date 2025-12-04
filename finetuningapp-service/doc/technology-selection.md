# 微调中心应用服务 (FinetuningApp Service) 技术选型说明书

## 1. 概述
本文档详细描述了微调中心应用服务 (`finetuningapp-service`) 的技术栈选型及第三方依赖版本。所有开发、构建与部署工作均应遵循本文档规定的版本标准。

## 2. 开发语言与环境
* **Java Development Kit (JDK)**: OpenJDK 21
  * *说明*: 采用 LTS 版本，利用新特性提升性能与开发效率。
* **Build Tool**: Maven 3.8+
  * *说明*: 标准化构建工具，用于依赖管理和打包。

## 3. 核心框架 (Spring Boot Ecosystem)
基于 **Spring Boot 2.7.18** 构建，利用其自动配置和成熟的生态系统。

| 组件 | Artifact ID | 版本 | 说明 |
| :--- | :--- | :--- | :--- |
| **核心容器** | `spring-boot-starter-web` | 2.7.18 | 提供 RESTful API 支持，内置 Tomcat 容器。 |
| **日志框架** | `spring-boot-starter-log4j2` | 2.7.18 | 替代默认的 Logback，提供更高性能的异步日志能力。 |
| **系统监控** | `spring-boot-starter-actuator` | 2.7.18 | 提供健康检查 (Health Check)、指标监控 (Metrics) 等运维接口。 |
| **安全框架** | `spring-boot-starter-security` | 2.7.18 | 负责认证与授权。 |
| **参数校验** | `spring-boot-starter-validation`| 2.7.18 | 基于 Hibernate Validator 实现 Bean Validation (JSR 380)。 |

## 4. 数据存储与访问层
| 组件 | Artifact ID | 版本 | 说明 |
| :--- | :--- | :--- | :--- |
| **ORM 框架** | `mybatis-spring-boot-starter` | 2.3.2 | 轻量级持久层框架，支持定制化 SQL。 |
| **数据库驱动** | `postgresql` | (Managed) | PostgreSQL JDBC 驱动。 |
| **事务管理** | `spring-tx` | (Managed) | 声明式事务管理。 |
| **分页插件** | `spring-data-commons` | (Managed) | 提供分页接口支持。 |

## 5. 接口定义与文档
| 组件 | Artifact ID | 版本 | 说明 |
| :--- | :--- | :--- | :--- |
| **API 文档 UI** | `springfox-swagger-ui` | 2.9.2 | 提供可视化的接口调试页面。 |
| **API 文档核心**| `springfox-swagger2` | 2.9.2 | Swagger 2.0 规范实现。 |
| **OpenAPI 注解**| `swagger-annotations` | 2.2.8 | 使用 OpenAPI 3 风格注解增强文档描述。 |
| **公共接口包** | `dialingtest-interface` | 1.0.1 | 内部定义的公共 DTO 与接口契约。 |

## 6. 通用工具库
| 组件 | Artifact ID | 版本 | 说明 |
| :--- | :--- | :--- | :--- |
| **Office 处理** | `poi`, `poi-ooxml` | 5.4.0 | 处理 Excel 报表导入导出。 |
| **压缩解压** | `commons-compress` | 1.26.1 | 处理文件归档与解压缩。 |
| **加密算法** | `bcprov-jdk15on` | 1.70 | BouncyCastle，提供标准 JDK 不支持的加密算法 (如 MD4)。 |

## 7. 测试框架
项目采用 **JUnit 4** 作为主要测试框架。

| 组件 | Artifact ID | 版本 | 说明 |
| :--- | :--- | :--- | :--- |
| **单元测试** | `junit` | 4.13.2 | 排除 Spring Boot 默认的 JUnit 5，强制使用 JUnit 4。 |
| **Mock 工具** | `mockito-core` | 4.11.0 | 模拟对象与行为验证。 |
| **静态 Mock** | `mockito-inline` | 4.11.0 | 支持 Static Method 和 Final Class 的 Mock。 |
| **测试集成** | `spring-boot-starter-test` | 2.7.18 | 集成 Spring TestContext Framework。 |
| **代码覆盖率** | `jacoco-maven-plugin` | 0.8.10 | 生成测试覆盖率报告。 |

## 8. 构建插件配置
在 `pom.xml` 中配置的关键 Maven 插件：

*   **maven-compiler-plugin**: 版本 `3.11.0`
    *   配置 `source` 和 `target` 为 `21`。
*   **spring-boot-maven-plugin**: 版本 `2.7.18`
    *   用于打包可执行 Jar 文件。
*   **maven-surefire-plugin**: 版本 `2.22.2`
    *   配置 `-Xmx1024m` 避免测试 OOM。
    *   `forkMode=once` 提高测试稳定性。


