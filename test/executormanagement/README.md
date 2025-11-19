# 执行机管理 - Python 集成测试框架

## 概述

本测试框架提供了执行机管理组件的完整集成测试套件，覆盖 WebSocket 通信、认证与会话、执行机状态管理、任务通道、北向 API 等核心功能。

## 目录结构

```
test/executormanagement/
├── __init__.py                      # 包初始化
├── config.py                        # 全局配置（API/WS/DB/超时等）
├── utils.py                         # 工具函数（APIClient、DB、断言、WS辅助）
├── base.py                          # 基础测试类（所有测试的父类）
├── test_sc01_register_auth.py       # SC-01: WSS注册与认证（CHAP四阶段）
├── test_sc02_heartbeat.py           # SC-02: 心跳与UE状态维护
├── test_sc03_rest_api.py            # SC-03: 北向API（列表/刷新）
├── test_sc04_task_channel.py        # SC-04: 任务下发与状态上报
├── test_sc05_env_mgmt.py            # SC-05: 环境管理（脚本与App）
├── test_sc06_screencap.py           # SC-06: UE截屏（占位）
├── test_sc07_offline.py             # SC-07: 离线处理
├── test_sc08_db_consistency.py      # SC-08: 数据库一致性与约束
├── test_sc09_concurrency.py         # SC-09: 并发与可靠性（可开关）
└── test_sc10_security.py            # SC-10: 安全性（占位）
```

## 测试场景覆盖

### SC-01: Agent 注册与认证（CHAP 四阶段）
- ✅ TC-01-001: 成功注册并获取token
- ✅ TC-01-002: 用户名不存在
- ✅ TC-01-003: 响应摘要不匹配
- ✅ TC-01-004: 注销（deregister）

### SC-02: 心跳与状态维护
- ✅ TC-02-001: 心跳更新状态
- ✅ TC-02-002: UE清单变更
- ✅ TC-02-003: 离线处理

### SC-03: 北向 API
- ✅ TC-03-001: 查询执行机列表
- ✅ TC-03-002: 刷新执行机信息

### SC-04: 任务通道
- ✅ TC-04-001: 任务下发消息格式验证
- ✅ TC-04-002: 任务状态上报
- ✅ TC-04-003: 任务消息字段验证

### SC-05: 环境管理
- ✅ TC-05-001: 脚本更新通知与回执
- ✅ TC-05-002: 应用安装（URL方式）
- ✅ TC-05-003: 应用安装（脚本方式）
- ✅ TC-05-004: 环境管理消息格式验证

### SC-07: 离线处理
- ✅ TC-07-001: 断开连接时设置离线状态

### SC-08: 数据库一致性
- ✅ TC-08-001: UE信息JSON合法性校验
- ✅ TC-08-002: 外键和唯一约束验证
- ✅ TC-08-003: 时间戳字段存在性验证

## 快速开始

### 0. 前置条件

#### 创建测试用户（仅需执行一次）

```powershell
# Windows PowerShell
cd D:\code\DialTestCenter
$env:PGPASSWORD='postgres'
psql -h localhost -p 5432 -U postgres -d dialingtest -f test\executormanagement\setup_test_user.sql
```

执行成功后会创建测试用户：
- 用户名：`test_agent`
- SHA256 Hash：`ECD71870D1963316A97E3AC3408C9835AD8CF0F3C1BC703527C30265534F75AE`（对应密码 `test123`）

**注意**：配置文件已包含此默认 SHA256 Hash，无需额外设置环境变量！

### 1. 安装依赖

```bash
cd test
pip install -r requirements.txt
```

必需依赖：
- `requests` - HTTP API 测试
- `psycopg2-binary` - PostgreSQL 数据库驱动
- `pytest` - 测试框架
- `websocket-client` - WebSocket 测试

### 2. 环境配置

创建环境变量配置文件或直接设置：

```bash
# API 配置
export EXEC_API_BASE_URL=http://localhost:8080
export EXEC_WS_URL=ws://localhost:8080/ws/executor

# WebSocket 测试开关（默认关闭）
export EXEC_WS_ENABLE=1

# Agent 认证凭据（WebSocket 测试必需）
export EXEC_AGENT_NAME=Executor_PC_001
export EXEC_AGENT_USERNAME=test_agent
export EXEC_AGENT_SHA256_HASH=ECD71870D1963316A97E3AC3408C9835AD8CF0F3C1BC703527C30265534F75AE  # 已有默认值，可不设置

# 数据库配置
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=dialingtest_test
export DB_USER=postgres
export DB_PASSWORD=postgres

# 超时配置（可选）
export EXEC_WS_TIMEOUT=10
export EXEC_WS_WAIT_OFFLINE_SEC=5

# 并发测试开关（默认关闭）
export EXEC_CONCURRENCY_ENABLE=0
```

### 3. 运行测试

#### 使用测试脚本（推荐）

**Linux/macOS:**
```bash
cd test/executormanagement
chmod +x run_tests.sh
./run_tests.sh              # 运行所有测试
./run_tests.sh api          # 仅运行 API 测试
./run_tests.sh ws           # 运行 WebSocket 测试
./run_tests.sh sc01         # 运行特定场景
```

**Windows PowerShell:**
```powershell
cd test/executormanagement
.\run_tests.ps1              # 运行所有测试
.\run_tests.ps1 api          # 仅运行 API 测试
.\run_tests.ps1 ws           # 运行 WebSocket 测试
.\run_tests.ps1 sc01         # 运行特定场景
```

**Windows CMD:**
```cmd
cd test\executormanagement
run_tests.bat                # 运行所有测试
run_tests.bat api            # 仅运行 API 测试
run_tests.bat ws             # 运行 WebSocket 测试
run_tests.bat sc01           # 运行特定场景
```

#### 直接使用 Python 命令

```bash
cd test/executormanagement
python -m pytest -v                              # 运行所有测试
python -m pytest test_sc01_register_auth.py -v   # 运行认证测试
python -m pytest test_sc02_heartbeat.py -v       # 运行心跳测试
python -m pytest test_sc03_rest_api.py -v        # 运行 API 测试
```

#### 使用 unittest 运行

```bash
python -m unittest discover -p 'test_*.py' -v
```

#### 生成测试报告

```bash
# 生成 HTML 报告
python -m pytest --html=report.html --self-contained-html

# 生成覆盖率报告
python -m pytest --cov=. --cov-report=html
```

## 测试数据准备

### 数据库初始化

测试前需要在数据库中准备基础数据：

```sql
-- 创建 Agent 用户（用于 CHAP 认证）
INSERT INTO dial_users (username, password)
VALUES ('test_agent', '<SHA256_HASH>')
ON CONFLICT (username) DO UPDATE SET password = EXCLUDED.password;

-- 创建测试执行机
INSERT INTO executor (name, ip, token, proxy, description, status, last_online_time)
VALUES ('Executor_PC_001', '10.0.0.10', NULL, NULL, '测试执行机', 0, NULL)
ON CONFLICT (name) DO NOTHING;
```

**注意：** `password` 字段存储 SHA256 Hash（64位十六进制），需要使用工具生成对应的哈希值。

### SHA256 Hash 生成

可以使用以下 Python 脚本生成 SHA256 Hash：

```python
import hashlib

def generate_sha256_hash(password: str) -> str:
    """生成 SHA256 Hash（大写）"""
    return hashlib.sha256(password.encode('utf-8')).hexdigest().upper()

# 示例
password = "your_password_here"
sha256_hash = generate_sha256_hash(password)
print(f"SHA256 Hash: {sha256_hash}")
```

## 工具类说明

### APIClient
HTTP API 客户端，支持：
- GET/POST/PUT/DELETE 请求
- 自动添加认证头
- 响应时间测量
- 错误处理

### DatabaseHelper
数据库操作辅助类，支持：
- 执行机查询（按名称/ID/列表）
- UE 查询（按 MSISDN/序列号/执行机）
- 执行机状态更新
- 测试数据清理
- 表结构验证
- 约束检查

### Assertions
测试断言工具类，支持：
- HTTP 状态码断言
- JSON 字段断言
- 响应时间断言
- 执行机状态断言
- UE 存在性断言
- WebSocket 消息断言

## 配置项说明

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `EXEC_API_BASE_URL` | API 基础URL | `http://localhost:8080` |
| `EXEC_WS_URL` | WebSocket URL | `ws://localhost:8080/ws/executor` |
| `EXEC_WS_ENABLE` | WebSocket 测试开关 | `0` (关闭) |
| `EXEC_AGENT_NAME` | 执行机名称 | `Executor_PC_001` |
| `EXEC_AGENT_USERNAME` | Agent 用户名 | `test_agent` |
| `EXEC_AGENT_SHA256_HASH` | SHA256 Hash | `ECD71870D1963316A97E3AC3408C9835AD8CF0F3C1BC703527C30265534F75AE` |
| `EXEC_WS_TIMEOUT` | WebSocket 超时（秒） | `10` |
| `EXEC_WS_WAIT_OFFLINE_SEC` | 等待离线超时（秒） | `5` |
| `DB_HOST` | 数据库主机 | `localhost` |
| `DB_PORT` | 数据库端口 | `5432` |
| `DB_NAME` | 数据库名称 | `dialingtest_test` |
| `DB_USER` | 数据库用户 | `postgres` |
| `DB_PASSWORD` | 数据库密码 | `postgres` |
| `EXEC_CONCURRENCY_ENABLE` | 并发测试开关 | `0` (关闭) |

## 性能基准

测试框架包含性能断言，默认阈值：

| API | 阈值（毫秒） |
|-----|-------------|
| 查询执行机列表 | 200 |
| 刷新执行机信息 | 300 |
| CHAP 认证握手 | 300 |
| 心跳处理 | 100 |

可在 `config.py` 中的 `PERFORMANCE_BASELINE` 字典中调整。

## 注意事项

### WebSocket 测试
- WebSocket 测试默认关闭，需要设置 `EXEC_WS_ENABLE=1` 启用
- 配置文件已包含默认 SHA256 Hash（`ECD71870D1963316A97E3AC3408C9835AD8CF0F3C1BC703527C30265534F75AE`），对应测试用户 `test_agent`
- 测试会建立真实的 WebSocket 连接，确保服务端已启动

### 数据库测试
- 测试会在数据库中创建/修改数据，建议使用专用测试数据库
- `test_sc08_db_consistency.py` 会自动跳过不存在的列/约束的验证
- 可使用 `DatabaseHelper.cleanup_test_data()` 清理测试数据

### 任务通道和环境管理
- SC-04 和 SC-05 的测试主要验证消息格式和通信通道
- 实际的任务下发需要配合任务管理模块
- 可以扩展这些测试以包含端到端的集成验证

## 故障排查

### 常见问题

**1. WebSocket 连接失败**
```
ConnectionRefusedError: [Errno 111] Connection refused
```
解决方案：
- 确认服务端已启动
- 检查 `EXEC_WS_URL` 配置是否正确
- 检查防火墙设置

**2. 认证失败**
```
AssertionError: Expected message_type 'register_ack', got 'register_challenge'
```
解决方案：
- 确认已执行 `setup_test_user.sql` 创建测试用户
- 检查数据库中的 dial_users 表是否包含 `test_agent` 用户
- 验证 SHA256 Hash 是否为 `ECD71870D1963316A97E3AC3408C9835AD8CF0F3C1BC703527C30265534F75AE`

**3. 数据库连接失败**
```
psycopg2.OperationalError: could not connect to server
```
解决方案：
- 检查数据库配置（HOST/PORT/NAME/USER/PASSWORD）
- 确认 PostgreSQL 服务已启动
- 验证网络连接和防火墙设置

**4. 测试跳过**
```
SKIPPED [1] WS测试默认关闭
```
说明：这是正常行为，设置 `EXEC_WS_ENABLE=1` 启用 WebSocket 测试

## 扩展建议

### 添加新测试场景
1. 在 `test/executormanagement/` 目录创建新的测试文件
2. 继承 `BaseTestCase` 类
3. 使用 `APIClient`、`DatabaseHelper` 和 `Assertions` 工具
4. 添加适当的 `@unittest.skip` 装饰器

### 扩展工具函数
在 `utils.py` 中添加新的辅助方法：
- 数据库操作方法
- 断言方法
- WebSocket 辅助方法

### 性能测试
1. 设置 `EXEC_CONCURRENCY_ENABLE=1`
2. 修改 `test_sc09_concurrency.py` 实现并发场景
3. 使用 `measure_response_time()` 测量性能

## 参考文档

- [执行机管理后端组件测试设计](../../AR软件实现设计文档/执行机管理/执行机管理后端组件测试设计.md)
- [执行机管理软件实现设计](../../AR软件实现设计文档/执行机管理/执行机管理软件实现设计.md)
- [测试用执行机客户端设计方案](../../AR软件实现设计文档/执行机管理/测试用执行机客户端设计方案.md)

## 版本历史

- **v2.0** (2025-11-09)
  - ✅ 完善 SC-01 认证测试（添加 TC-01-003、TC-01-004）
  - ✅ 完善 SC-02 心跳测试（添加 TC-02-002、TC-02-003）
  - ✅ 增强 SC-04 任务通道测试（实现完整测试用例）
  - ✅ 增强 SC-05 环境管理测试（实现完整测试用例）
  - ✅ 扩展 DatabaseHelper 工具类
  - ✅ 扩展 Assertions 断言工具类
  - ✅ 更新测试文档

- **v1.0** (2025-11-08)
  - 初始版本
  - 基础测试框架搭建
  - 占位测试用例

---

**维护者：** 执行机管理开发团队  
**最后更新：** 2025-11-09


