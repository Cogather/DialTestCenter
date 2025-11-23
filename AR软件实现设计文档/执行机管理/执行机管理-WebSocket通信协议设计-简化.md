# 执行机管理 - WebSocket通信协议设计

## 1. 概述

本设计提供稳定的WSS长连接与**JSON信令编解码能力与二进制分片传输能力**。

## 2. 系统架构 (V4 单链路版本)

### 2.1 总体架构图

```plantuml
@startuml
title 拨测设备管理通信层 (WSS+JSON+Binary) - V4 单链路架构

skinparam componentStyle uml2
skinparam linetype ortho

cloud "ADCA Agent\n(客户端)" as Agent
package "业务逻辑层 (Biz)" as Biz

package "controller.executormanagement.websocket" as WSS {
    component "ExecutorWebsocketEndpoint\n(WSS端点)" as Endpoint
    component "WssMessageDispatcher\n(入站分发)" as Dispatcher
    component "WebSocketSessionRegistry\n(会话注册表)" as Registry
    
    package "flow\n(流控与文件传输)" {
        component "WssMessageSenderImpl\n(出站实现)" as SenderImpl
        component "SessionSendQueue\n(高/低优队列)" as Queue
        component "InboundFileHandler\n(入站文件处理)" as InboundHandler
        component "InboundFileCompleteEvent\n(文件完成事件)" as FileEvent
    }
}

' 关系
Agent <.> Endpoint : WSS 连接
Endpoint .> Dispatcher : onMessage(String/ByteBuffer)
Dispatcher .> Biz : handleXxx(DTO)
Dispatcher .> InboundHandler : handleChunk(buffer)

Biz .> SenderImpl : sendJson/File
SenderImpl .> Queue : 入队 (Hi/Lo)
Queue .> Endpoint : sendText/Binary (Loop)
@enduml
```

### 2.2 包结构设计

```plaintext
com.huawei.cloududn.dialingtestapp
│
├── controller.executormanagement.websocket
│   ├── ExecutorWebsocketEndpoint.java        // JSR-356 端点，处理 onOpen/Close/Message
│   ├── WssMessageDispatcher.java           // 入站消息协调：解析JSON信封，分发文件分片
│   ├── WebSocketSessionRegistry.java       // 会话注册表 (SessionId <-> Session)
│   │
│   ├── flow                                // 消息流控、排队、文件传输核心包
│   │   ├── WssMessageSender.java           // (接口) 业务层依赖的出站发送器
│   │   ├── WssMessageSenderImpl.java       // (实现) 管理SessionSendQueue，拆解消息入队
│   │   │
│   │   ├── SessionSendQueue.java         // [核心] 单会话的高/低优先级队列管理器
│   │   │                                   // 包含 Hi-Priority (JSON) 和 Lo-Priority (Binary) 队列
│   │   │
│   │   ├── QueuedMessage.java            // 队列中的消息包装类 (Text/Binary)
│   │   ├── InboundFileHandler.java         // 入站文件处理器，组装分片
│   │   ├── InboundFileCompleteEvent.java // 文件接收完成事件
│   │   └── InboundFileState.java         // 单个入站文件的接收状态上下文
│   │
│   └── dto
│       ├── JsonMessageEnvelope.java        // JSON消息信封 (type, token, payload)
│       ├── MessageType.java              // 消息ID枚举
│       ├── RegisterRequestDto.java       // [示例] 注册请求
│       ├── ReportMsgDto.java             // [示例] 心跳/状态报告
│       ├── ScriptUpdateNotifyDto.java    // [示例] 脚本下发 (带文件)
│       ├── TaskStopRequestDto.java       // [示例] 任务停止 (高优信令)
│       └── ... (其他业务DTO)
│
├── config
│   └── WebSocketJsr356Config.java        // WebSocket 配置
└── util
    └── Sha256HashUtil.java               // 工具类
```

### 2.3 系统类图

```plantuml
@startuml
title 通信层主要类关系 (V4 详细版)

skinparam componentStyle uml2
skinparam linetype ortho

' --- 顶层 WebSocket 组件 ---
package "websocket" {
    class ExecutorWebsocketEndpoint {
      + onOpen(session)
      + onClose(session)
      + onMessage(message: String, session)
      + onMessage(buffer: ByteBuffer, session)
      + sendText(sessionId, data: String)
      + sendBinary(sessionId, data: ByteBuffer)
    }

    class WssMessageDispatcher {
      - objectMapper: ObjectMapper
      - inboundFileHandler: InboundFileHandler
      - businessLayer: ... (Callbacks)
      + dispatch(message: String, session)
      + dispatch(buffer: ByteBuffer, session)
    }

    class WebSocketSessionRegistry {
      + register(id, session)
      + unregister(id)
      + getSession(id)
    }
}

' --- V4 核心：流控与文件传输包 ---
package "websocket.flow" {

    interface WssMessageSender {
      + sendJsonMessage(sessionId, dto)
      + sendFile(sessionId, dto, fileStream)
    }

    class WssMessageSenderImpl implements WssMessageSender {
      - sessionQueues: Map<String, SessionSendQueue>
      + sendJsonMessage(...)
      + sendFile(...)
    }

    class SessionSendQueue {
      - hiPriorityQueue: Queue<QueuedMessage>
      - loPriorityQueue: Queue<QueuedMessage>
      - endpoint: ExecutorWebsocketEndpoint
      + enqueueMessage(message)
      + runSendLoop()
    }

    class InboundFileHandler {
      - inboundStates: Map<String, InboundFileState>
      + startReceiving(sessionId, fileInfo)
      + handleChunk(sessionId, buffer)
    }

    ' [V4 修正] 移除 ... 占位符
    class QueuedMessage {}
    class InboundFileState {}
}

' --- 依赖关系 ---
' 1. Endpoint 是所有操作的起点和终点
ExecutorWebsocketEndpoint .down.> WssMessageDispatcher : "dispatch()"
ExecutorWebsocketEndpoint .down.> WebSocketSessionRegistry : "register()"

' 2. WssMessageDispatcher (入站)
WssMessageDispatcher .down.> InboundFileHandler : "handleChunk()"

' 3. WssMessageSender (出站)
WssMessageSenderImpl .down.> SessionSendQueue : "enqueueMessage()"
SessionSendQueue .up.> ExecutorWebsocketEndpoint : "sendText/Binary()"

' 4. 业务层依赖 (未在图中显示)
' Biz -> WssMessageSender (Interface)
' Biz -> InboundFileHandler (startReceiving)

@enduml
```

## 3. 接口定义与通信机制

### 3.1 基础机制

#### 3.1.1 连接与心跳
*   **URL**: `wss://{server}:{port}/ws/executor`
*   **心跳**: 30s 周期；连续 3 周期未达判离线。
*   **会话**: 认证成功生成 `token` (8字节)，后续所有 JSON 信令必须携带。

### 3.2 数据格式

**1. JSON 信令 (Text Message)**
所有控制消息使用 JSON 格式，包含统一信封：
```json
{
  "type": "RegisterRequest",
  "token": 1234567890123456,
  "payload": { ... } // 业务DTO
}
```

**2. 二进制分片 (Binary Message)**
仅用于大文件传输。必须紧跟在关联的 JSON 信令（含 `filelen`, `crc`）之后发送。格式为原始二进制流。

### 3.3 关键消息定义示例

#### (1) 注册消息：Register-Request (0x01)
*   **用途**：客户端发起注册，开始 CHAP 认证流程。
*   **Payload**: `hostname` (string)

#### (2) 心跳/状态：Report-Msg (0x11)
*   **用途**：Agent 定期上报状态（心跳）。
*   **Payload**: `token` (long), `state` (string), `ue-list` (array)

#### (3) 脚本下发：ScriptUpdate-Notify (0x31) - [带文件]
*   **用途**：服务端下发脚本文件。**发送此信令后，立即发送 N 个二进制分片。**
*   **Payload**:
    *   `script-name`: string
    *   `version`: string
    *   `filelen`: int (文件总字节数)
    *   `crc`: string (校验码)

#### (4) 任务停止：TaskStop-Request (0x35) - [紧急信令]
*   **用途**：服务端下发停止指令。此消息为纯信令，需**插队**优先发送。
*   **Payload**: `taskid` (int), `script-name` (string)

## 4. 核心业务流程 (V4)

### 4.1 WSS 连接建立与认证流程

此流程建立会话并分发 Token。

```plantuml
@startuml
title 4.1 WSS 连接建立与认证流程 (通信层视角)

actor "ADCA\nAgent" as Agent
participant "ExecutorWebsocketEndpoint" as Endpoint
participant "WssMessageDispatcher" as Dispatcher
participant "AuthSessionService\n[业务层]" as AuthService
participant "WssMessageSender\n[通信层]" as Sender

Agent -> Endpoint : 1. 建立 WSS 连接 (onOpen)
Endpoint -> Dispatcher : 2. 触发 onOpen(session)
Dispatcher -> Dispatcher : 3. 注册会话到 SessionRegistry

Agent -> Endpoint : 4. 发送 Register-Request (JSON Text)
Endpoint -> Dispatcher : 5. onMessage(String, session)
Dispatcher -> Dispatcher : 6. (Jackson) 解析JSON信封, 识别 type="RegisterRequest"
Dispatcher -> AuthService : 7. 回调 handleRegisterRequest(dto, session)

activate AuthService
    AuthService -> AuthService : 8. (业务逻辑) 生成 Challenge
    AuthService -> Sender : 9. (业务调用) 发送 Register-Challenge (DTO)
deactivate AuthService

activate Sender
    Sender -> Sender : 10. (队列) 消息(JSON)进入高优队列
    Sender -> Endpoint : 11. (出队) sendText(session, json)
deactivate Sender

Endpoint -> Agent : 12. 发送 Register-Challenge (JSON Text)

' ... 省略 Agent 计算 Response ...

Agent -> Endpoint : 13. 发送 Register-Response (JSON Text)
Endpoint -> Dispatcher : 14. onMessage(String, session)
Dispatcher -> AuthService : 15. 回调 handleRegisterResponse(dto, session)

activate AuthService
    AuthService -> AuthService : 16. (业务逻辑) 校验 Response, 生成 Token
    AuthService -> Sender : 17. (业务调用) 发送 Register-Result (DTO)
deactivate AuthService

activate Sender
    Sender -> Sender : 18. (队列) 消息(JSON)进入高优队列
    Sender -> Endpoint : 19. (出队) sendText(session, json)
deactivate Sender

Endpoint -> Agent : 20. 发送 Register-Result (JSON Text)
@enduml
```

### 4.2 入站消息处理：JSON 信令 (Text)

此流程用于通信层处理标准JSON消息（如心跳`Report-Msg`）。

*   **解析逻辑**：`Endpoint` 收到 Text 消息后，`Dispatcher` 解析 JSON 信封，识别 `type` 字段。
*   **业务分发**：根据 `type` 将 Payload 转换为对应的 DTO，并回调业务层接口（如 `handleReportMsg`）。

```plantuml
@startuml
title 4.2 入站消息处理：JSON 信令

actor "Agent" as Agent
participant "Endpoint" as Endpoint
participant "Dispatcher" as Dispatcher
participant "ExecutorMgmtService\n[业务层]" as Biz

Agent -> Endpoint : 1. 发送 Report-Msg (JSON)
Endpoint -> Dispatcher : 2. onMessage(json)
Dispatcher -> Dispatcher : 3. 解析信封 & DTO
Dispatcher -> Biz : 4. 回调 handleReportMsg(dto)
Biz -> Biz : 5. 业务处理
@enduml
```

### 4.3 入站消息处理：二进制分片 (Binary)

此流程是V4的核心，展示了通信层如何接收Agent发送的大文件（如`ScreanCap-Response`）。

```plantuml
@startuml
title 4.3 入站消息处理：二进制分片 (Binary)

actor "ADCA\nAgent" as Agent
participant "ExecutorWebsocketEndpoint" as Endpoint
participant "WssMessageDispatcher" as Dispatcher
participant "InboundFileHandler" as InboundHandler
participant "TaskInterfaceService\n[业务层]" as TaskService
participant "文件存储" as Storage

' 步骤 1: JSON 信令先到达
Agent -> Endpoint : 1. 发送 ScreanCap-Response (JSON Text)\n(含 filelen, crc)
Endpoint -> Dispatcher : 2. onMessage(String, session)
activate Dispatcher
    Dispatcher -> TaskService : 3. 回调 handleScreanCapResponse(dto)
deactivate Dispatcher

activate TaskService
    ' 业务层告知通信层准备接收文件
    TaskService -> InboundHandler : 4. (业务调用) startReceiving\n(sessionId, fileInfo)
deactivate TaskService

' 步骤 2: 二进制分片连续到达
loop N 次 (文件分片)
    Agent -> Endpoint : 5. 发送 Binary Message (图片分片 k/N)
    Endpoint -> Dispatcher : 6. onMessage(ByteBuffer, session)

    activate Dispatcher
        Dispatcher -> InboundHandler : 7. handleChunk(sessionId, buffer)
    deactivate Dispatcher

    activate InboundHandler
        InboundHandler -> InboundHandler : 8. (内部) 查找会话状态
        InboundHandler -> Storage : 9. (内部) 将 buffer 写入临时文件
        InboundHandler -> InboundHandler : 10. (内部) 检查: totalReceived == filelen?

        alt 文件接收完毕
            InboundHandler -> InboundHandler : 11. (内部) 校验 CRC
            InboundHandler -> TaskService : 12. 回调 onInboundFileComplete(state)
            InboundHandler -> InboundHandler : 13. (内部) 清理会话状态
        end
    deactivate InboundHandler
end
@enduml
```

### 4.4 出站消息处理：高/低优先级队列 (V4 核心)

此流程是V4设计的**灵魂**。它展示了`WssMessageSenderImpl`如何使用两个队列来确保**控制信令（高优）总能优先于文件分片（低优）**。

```plantuml
@startuml
title 4.4 出站消息处理：高/低优先级队列 (V4 核心)

participant "业务层\n(TaskService, etc.)" as Biz
participant "ExecutorWebsocketEndpoint" as Endpoint

' -- WssMessageSenderImpl 内部实现 --
participant "WssMessageSenderImpl" as Sender
queue "高优队列 (JSON 信令)" as HiQ
queue "低优队列 (Binary 分片)" as LoQ
participant "Sender 异步处理循环" as Loop
' ---------------------------------------------------

Biz -> Sender : 1. (业务调用) sendXxx(dto)
activate Sender
    alt DTO 是纯 JSON (如 TaskStop)
        Sender -> HiQ : 2a. JSON 字符串加入 [高优队列]
    else DTO 含大文件 (如 ScriptUpdate)
        Sender -> HiQ : 2b. JSON 信令加入 [高优队列]
        Sender -> LoQ : 2c. 所有文件分片 (1..N) 加入 [低优队列]
    end
deactivate Sender

' 单独的发送循环 (每个Session一个)
Loop -> Loop : 3. 循环检查队列
activate Loop
    alt 高优队列 (HiQ) 非空
        Loop -> HiQ : 4. (出队)
        HiQ --> Loop : 5. msg (JSON)
        Loop -> Endpoint : 6. sendText(msg)
    else 低优队列 (LoQ) 非空
        Loop -> LoQ : 7. (出队)
        LoQ --> Loop : 8. chunk (Binary)
        Loop -> Endpoint : 9. sendBinary(chunk)
    else 队列均为空
        Loop -> Loop : 10. (休眠 / 等待唤醒)
    end
deactivate Loop
@enduml
```

### 4.5 出站大文件传输 (队列应用)

此流程演示了5.5中的高/低优队列设计，在实际大文件传输过程中，如何被高优先级消息（如`TaskStop`）**插队**。

```plantuml
@startuml
title 4.5 出站大文件传输 (队列应用示例)

participant "TaskService\n[业务层]" as TaskService
participant "WssMessageSenderImpl" as Sender
participant "ExecutorWebsocketEndpoint" as Endpoint
actor "ADCA\nAgent" as Agent

' 时间 T1: 业务层下发脚本
TaskService -> Sender : 1. sendScriptUpdate(dto)\n(含10MB文件)
activate Sender
    Sender -> Sender : 2. (入队) ScriptUpdate-Notify(JSON) -> 高优
    Sender -> Sender : 3. (入队) [Chunk 1..100] -> 低优
deactivate Sender

' 时间 T2: 发送循环开始
Sender -> Endpoint : 4. (出队) sendText(ScriptUpdate-Notify)
Endpoint -> Agent : 5. 收到 JSON 信令 (准备接文件)

Sender -> Endpoint : 6. (出队) sendBinary(Chunk 1)
Sender -> Endpoint : 7. (出队) sendBinary(Chunk 2)

' 时间 T3: 此时 (Chunk 2 正在发送), 业务层紧急停止任务
TaskService -> Sender : 8. (业务调用) sendTaskStop(dto)
activate Sender
    Sender -> Sender : 9. (入队) TaskStop-Request(JSON) -> 高优
deactivate Sender

' 时间 T4: 发送循环 (刚发完 Chunk 2)
Sender -> Sender : 10. (检查队列) 发现高优队列非空
Sender -> Endpoint : 11. (高优出队) sendText(TaskStop-Request)
Endpoint -> Agent : 12. 收到 TaskStop 命令 (立即执行)

' 时间 T5: 发送循环 (刚发完 TaskStop)
Sender -> Sender : 13. (检查队列) 高优队列为空
Sender -> Endpoint : 14. (低优出队) sendBinary(Chunk 3)
Sender -> Endpoint : 15. (低优出队) sendBinary(Chunk 4)
' ... 继续发送剩余文件 ...
@enduml
```
