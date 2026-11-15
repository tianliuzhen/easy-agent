# 多 Agent 编排（Flow）设计说明

代码位置：`ea-service/src/main/java/com/aaa/easyagent/biz/flow/`
前端对应：`ea-web/src/views/page/flow/FlowStudioPage.tsx`、`views/api/FlowApi.ts`

---

## 1. 要解决的问题

单个 Agent 能力有上限：一个提示词、一组工具、一条 ReAct 循环。真实业务往往是「先分类，再分别处理，最后汇总」这种多步骤协作。Flow 的目标是让用户在画布上把已有的 Agent 连起来，不写代码就得到一条可运行、可观测、可回溯的多 Agent 链路。

设计上有三条约束：

1. **复用 Agent 的全部能力。** Flow 不另造一套模型调用、工具加载、记忆和轨迹机制，节点内部直接执行 `ToolAgentExecutor` / `ReActAgentExecutor`。因此 `biz/flow` 与 `biz/agent` 是同级包，依赖方向为 `flow → agent`，单向。
2. **错画的图不能跑。** 端口类型、必填项、控制流成环、节点可达性在发布前就要给出明确报错，而不是跑到一半抛 NPE。
3. **跑崩的图必须收口。** 失败、超时、客户端断开之后，不能留下永远停在 `RUNNING` 的审计记录，也不能让落后的节点往已关闭的 SSE 继续写数据。

---

## 2. 领域模型

定义态的数据结构在 `core/domain/flow/`，与数据库和前端共用一套 JSON：

| 概念 | 类 | 说明 |
|------|----|------|
| 图 | `FlowDefinition` | `schemaVersion` + 节点列表 + 边列表，整体 JSON 存 `ea_agent_flow_revision.definition_json` |
| 节点 | `FlowNodeDefinition` | `id` / `type` / `version` / `config`（节点私有配置）/ 输入输出端口 / 画布坐标 |
| 端口 | `PortDefinition` | `key` / `kind` / `valueType` / `required` / `multiple` / `defaultValue` |
| 边 | `FlowEdgeDefinition` | `kind` + 来源端口 `PortRef` + 目标端口 `PortRef` |

### 三种边，各管一件事

把「谁先执行」和「数据怎么流」拆成两种边，是整套设计的地基。多数编排产品只有一种边，于是「并行后汇总」和「拿上游的输出当入参」只能靠同一根线表达，语义互相打架。

| `EdgeKind` | 含义 | 调度语义 |
|-----------|------|---------|
| `CONTROL` | 执行顺序 | 参与三态传播，决定节点跑不跑 |
| `DATA` | 数据传递 | 不决定执行顺序，只决定入参从哪取；来源节点未结束则等待 |
| `WORKER` | Worker 归属 | 不触发执行，只声明「这个 Agent 由该 SUPERVISOR 按需调用」 |

`ValueType` 有 8 种（`STRING`、`JSON`、`IMAGE`、`MESSAGE` 等），兼容规则在 `PortTypeSystem`：`ANY` 与任意类型互通，`STRING` 与 `MESSAGE` 双向互通，其余要求完全相等。

### 内置节点

全部 code 定义在 `FlowNodeTypes`，与插件 `metadata().type` 及前端节点库一一对应：

| 类型 | 分类 | 作用 |
|------|------|------|
| `START` | 基础 | 唯一的无控制入边起点，输出 Flow 的 `input` / `image` |
| `END` | 基础 | 声明最终输出；允许多个，以第一个到达的为最终答案 |
| `AGENT` | Agent | 调用一个已配置的 Agent，也可作为 SUPERVISOR 的 Worker |
| `SUPERVISOR` | 控制 | 把 Worker Agent 包装成工具，由模型自己决定调谁、调几次 |
| `ROUTER` | 控制 | 用模型选一个控制出口，出口端口由 `branches` 配置动态生成 |
| `FORK` | 控制 | 同时激活所有控制分支 |
| `JOIN` | 控制 | 等待已激活分支并聚合数据 |
| `TEMPLATE` | 数据 | 把多个输入按 `{{ 变量 }}` 渲染成文本 |

---

## 3. 包结构与依赖方向

```
biz/flow/
├── compile/    编译期：FlowDefinition → 可执行图
│   FlowCompiler, CompiledFlowPlan, PortTypeSystem, FlowValidationException
├── engine/     运行期调度
│   FlowEngine, DefaultFlowEngine, FlowExecutorConfig, FlowOrchestrationProperties
├── node/       节点扩展点（SPI）与执行契约
│   FlowNodePlugin, FlowNodePluginRegistry, FlowNodeTypes,
│   NodeExecutionContext, NodeExecutionResult
├── plugin/     8 种内置节点实现 + NodeMetadataSupport, FlowTemplateRenderer
├── event/      事件出口
│   FlowEventType, FlowEventPublisher, FlowNodeSseEmitter
└── runtime/    单次运行的上下文、状态与审计
    FlowRunContext, FlowState, FlowRunStatus, FlowExecutionResult, FlowRunRecorder
```

依赖是单向的，没有包级循环：

```
engine ──→ compile ──→ node ──→ event ──→ runtime
   │                     ↑
   └─────────────────────┤
                      plugin
```

关键一条：**plugin 不依赖 engine**。新增一种节点类型不需要碰调度代码，这是插件化能成立的前提。每个子包都有 `package-info.java` 写明职责与依赖，IDE 里点开包就能看到。

### 与外部的边界

- `biz/flow` → `biz/agent`：`AgentNodePlugin` / `SupervisorNodePlugin` 通过 `AgentContextFactory` 构造上下文并执行 `BaseAgent` 子类，复用模型调用、工具加载、滑动窗口和 `ChatTraceRecorder` 轨迹。
- `core/service/FlowStudioService`：图的增删改查、草稿与版本发布，只依赖 `FlowCompiler` 做校验。
- `biz/agent/service/impl/AgentChatServiceImpl`：聊天入口按 `request.getFlowId()` 是否为空，分流到单 Agent 或 Flow，这是唯一一处 `agent → flow` 的反向引用，属于入口分发而非能力依赖。

---

## 4. 用到的设计模式

只列真实落在代码里的，标注位置和解决的具体问题。

| 模式 | 位置 | 为什么用它 |
|------|------|-----------|
| **策略 + 注册表** | `FlowNodePlugin` / `FlowNodePluginRegistry` | 节点类型是开放集合。Spring 注入 `List<FlowNodePlugin>`，按 `type@version` 建表，`version` 让同一类型的新旧行为能共存，老图不会因为节点升级而跑出不同结果 |
| **两阶段：编译 + 执行** | `FlowCompiler` → `CompiledFlowPlan` → `DefaultFlowEngine` | 校验、端口补全、边索引都是纯函数，一次算完缓存成只读产物；调度循环里只做状态推进，不再重复解析图。`CompiledFlowPlan` 全字段 final，并发读无锁 |
| **模板方法** | `FlowNodePlugin.prepare()` / `validate()` 的 default 实现 | 静态节点（START、FORK）什么都不用写；只有 ROUTER 这类动态端口节点才覆盖 `prepare()` 把配置翻译成端口 |
| **适配器** | `FlowNodeSseEmitter` | 单 Agent 执行器只认 `SseEmitter`，Flow 需要每个事件带 `nodeId`。它继承 `SseEmitter` 并实现 `ScopedEventEmitter`，把 `think`/`data`/`tool` 翻译成对应的 `NODE_*` 事件。关键是覆盖了 `complete()` 为空实现——节点不拥有整条 Flow 的 SSE 生命周期，一个节点跑完不能把流关掉 |
| **发布-订阅** | `FlowEventPublisher` | 调度与插件只管 `publish(事件类型, 节点, 数据)`，不关心对面是 SSE 还是别的。内部一个 `AtomicBoolean closed`：终态事件发出后丢弃所有后续事件 |
| **建造者** | `FlowRunContext` / `NodeExecutionContext`（Lombok `@Builder`） | 字段多且全部 final，构造器参数会排到十几个 |
| **命令 + Future** | `ExecutorCompletionService<NodeTaskResult>` | 节点执行被包成任务提交，调度线程 `poll(超时)` 收结果。用 CompletionService 而不是遍历 Future 列表，是因为需要「任一节点完成就立刻推进」，同时保留超时让出控制权去检查取消标志 |
| **递归委托** | `NodeExecutionContext.workerInvoker` | SUPERVISOR 在工具回调里同步调用 Worker，而 Worker 本身可能又是 SUPERVISOR。`workerInvoker` 指向 `DefaultFlowEngine.executeWorker` 并在构造子上下文时继续传递，嵌套主管因此能继续派发 |
| **黑板（共享状态）** | `FlowState` | 以 `nodeId:portKey` 为键的 `ConcurrentHashMap`。并行节点各写自己的键，天然无竞争；下游按数据边取值，不需要知道上游是谁 |
| **作用域视图** | `ChatTraceRecorder.scopedTo(source)`（在 `biz/agent/trace`） | 整条编排共用一个轨迹收集器，每个节点取一个带自身 `nodeId`/`agentId` 标识的视图写入。于是多节点的思考与工具调用既汇总进同一轮对话，又能在前端按节点区分 |

---

## 5. 编译期：什么样的图算合法

`FlowCompiler.validate()` 返回 `FlowValidationResult`（错误 + 警告清单，每条带 `target`/`code`/`message`，前端点一下能定位到节点）；`compile()` 校验不通过直接抛 `FlowValidationException`。

节点级，逐个节点：

1. ID 非空且不重复
2. `type@version` 已注册
3. 端口补全：节点没带端口时从插件元数据复制一份（`fillPorts`）
4. 调 `plugin.prepare()`，再调 `plugin.validate()` 收集节点自己的规则（如 AGENT 必须配 `agentId`、ROUTER 至少两个分支且 key 不能用保留字 `route`）

图级：

5. 必须且只有一个 `START`，至少一个 `END`
6. 每条边：ID 唯一、两端节点与端口存在、`edge.kind` 与两端 `port.kind` 一致
7. 数据边的两端类型兼容（`PortTypeSystem`）
8. Worker 边只能 `SUPERVISOR → AGENT`
9. 非 `multiple` 的目标端口不允许多条入边
10. 必填输入端口必须有连线或默认值
11. 同一个 Agent 不能既当 Worker 又接控制边（两种调度语义会冲突）
12. 控制流无环——循环必须用专用 LOOP 节点（尚未实现，见第 10 节）
13. 从 `START` 不可达的节点报**警告**（不是错误，允许用户先摆着慢慢连）。Worker 节点由主管动态调用、本身不接控制边，排除在可达性检查之外

最后按 `EdgeKind` 把入边、出边各建一次索引（`Map<EdgeKind, Map<nodeId, List<Edge>>>`），调度时 `incoming(CONTROL, nodeId)` 是 O(1) 查表。

---

## 6. 运行期：三态控制边调度

### 为什么不用拓扑排序

拓扑序是静态的，而这张图的执行路径在运行时才确定：ROUTER 要等模型返回才知道走哪个分支，没被选中的分支及其整条下游都不该执行。所以调度按状态机推进，而不是按预排的顺序表。

### 状态

```
控制边   PENDING ──→ ACTIVE | SKIPPED          （三态，一旦有结论不再变）
节点     PENDING ──→ RUNNING ──→ SUCCEEDED | FAILED
                 └──→ SKIPPED
```

### 就绪判定（`readiness`）

对一个还是 `PENDING` 的节点：

- 没有控制入边：只有 `START` 可执行，其他节点一律 `WAIT`（不接控制边的 Worker 由主管调用，走另一条路径）
- 有控制入边：**任一条还是 `PENDING` → WAIT**；全部有结论且**至少一条 `ACTIVE` → 继续判数据**；**全部 `SKIPPED` → SKIP**
- 数据入边的来源节点仍在 `PENDING`/`RUNNING` → `WAIT`，等它产出

这套「全部有结论 + 至少一条激活」的规则同时覆盖了顺序、分支和汇聚：JOIN 不需要特殊代码，它的多条控制入边里只要有一条激活就执行，被跳过的分支不会把它卡死。

### 调度循环

```
while true:
    1. 检查取消标志与 Flow 整体超时        → 命中则进入收敛
    2. 反复扫描节点：SKIP 的立即跳过并向下传播，READY 的提交执行
       （跳过不占线程，一轮扫到底；提交受单次运行并发上限约束）
       没有在飞节点 → 跳出循环
    3. poll(pollIntervalMillis) 等任一节点完成
       没等到 → 检查单节点超时，回到第 1 步
    4. 节点失败 → 收敛整条 Flow
       节点成功 → 落数据、累加 token、写审计、按 routeKey 激活控制出边
跳出后：到达过 END 则 SUCCEEDED，否则 FAILED（图被分支截断）
```

两个细节值得说明：

- **入参收集、审计建记录、`NODE_STARTED` 事件都在调度线程完成**，节点线程只负责执行插件。这样失败收敛时调度线程手里一定有 `nodeRunId`，能把在飞节点的审计记录收口。
- **`poll` 带超时而不是阻塞等待**，为的是定期回到循环顶部检查取消标志和超时。节点跑 5 分钟不代表 5 分钟内不能响应客户端断开。

### 出边激活

```java
boolean active = result.getRouteKey() == null
        || Objects.equals(result.getRouteKey(), edge.getSource().getPortKey());
```

普通节点 `routeKey` 为 null，所有控制出边 `ACTIVE`（FORK 因此不需要任何特殊逻辑，它的「并行」就是多条出边同时激活）；ROUTER 返回 `routeKey`，只有同名端口的出边激活，其余 `SKIPPED` 并向下传播。

### 入参收集

按数据入边从 `FlowState` 取值：`multiple` 端口聚合成 `List`，单值端口直接放入，最后补上端口默认值。

执行前再校验一次必填数据入参（`requireDataInputs`）。编译期只能保证端口连了线，运行期上游分支被跳过时值仍然可能缺失——不校验的话插件会把 `null` 拼成字符串 `"null"` 发给模型，拿回一个看起来成功的垃圾结果。注意空字符串不算缺失：那是上游真实产出的结果。

### 收敛：失败、超时、取消

三者走同一套收口逻辑，区别只在终态和原因：

| 触发 | 终态 | 处理 |
|------|------|------|
| 节点抛异常 | `FAILED` | 该节点记失败，其余在飞节点按取消收口 |
| 单节点超时（默认 300s） | `FAILED` | `future.cancel(true)`，同上 |
| Flow 整体超时（默认 1800s） | `CANCELLED` | 全部在飞节点按取消收口 |
| 客户端断开（`SseEmitter` 回调置位取消标志） | `CANCELLED` | 同上 |
| 调度线程被中断 / 调度自身异常 | `CANCELLED` / `FAILED` | 不做可中断等待，直接放弃在飞节点 |

`drainRunningNodes` 的三步：打断全部在飞线程 → 有界等待（默认 10s）它们退出，期间按真实结果收口审计（已经跑完的仍记 `SUCCEEDED`，不冤枉它）→ 超时仍未退出的按取消收口，错误信息里标明「节点可能仍在后台运行」。

这里有个必须说清的边界：**大模型的 HTTP 调用不一定响应线程中断**。收敛保证的是审计不残留在 `RUNNING`、不再调度新节点、不再往 SSE 写事件，而不是瞬间掐断已经发出的那一次模型请求。

`FlowRunContext.cancel(reason)` 用 CAS 保证幂等，只保留第一个原因——否则「客户端断开」会被后续的「上游节点失败」覆盖，排查时分不清谁是因谁是果。

---

## 7. SUPERVISOR 与 Worker

ROUTER 是静态分支（分支集合画在图上），SUPERVISOR 是动态调度：Worker 边连的每个 Agent 被包装成一个工具交给主管 Agent，由模型自己决定调哪个、调几次、是否串联。

```
SUPERVISOR 节点
  ├─ worker 边 ─→ AGENT A  ──┐
  ├─ worker 边 ─→ AGENT B  ──┤  包装成 FunctionCallback 交给主管模型
  └─ worker 边 ─→ AGENT C  ──┘
```

工具名取自 Worker 自己的 Agent 配置（`agentKey`，重名时追加序号），工具说明取 Agent 名称 + 描述。主管选谁完全依赖这两个字段，所以它们必须是人能读懂的业务语义——用 `nodeId`（形如 `agent_3f2a1b`）当工具名等于让模型盲猜。

Worker 的执行走 `DefaultFlowEngine.executeWorker`：同步执行、独立写一条节点运行审计、产出落进 `FlowState`、token 累加回主管节点。Worker 的 `workerInvoker` 继续指向同一方法，嵌套主管因此可以继续派发。

---

## 8. 可观测性

### 事件协议

`FlowEventType` 维护「事件名 → 前端消息类型」的映射，前端按 `type` 选择展示组件：

| 事件 | 前端 type | 含义 |
|------|----------|------|
| `flow_started` | `flow` | Flow 开始 |
| `flow_succeeded` | `finalAnswer` | 全部成功，data 为最终答案 |
| `flow_failed` | `error` | 失败或取消 |
| `node_ready` / `node_started` / `node_succeeded` / `node_failed` / `node_skipped` | `flow` | 节点状态流转，画布据此染色 |
| `node_data` | `data` | 节点产生的流式回答片段 |
| `node_think` | `think` | 节点的思考过程 |
| `node_tool` | `tool` | 节点的工具调用及结果 |
| `node_log` | `flow` | 普通运行日志（如 ROUTER 未命中走 fallback） |

每个事件都带 `runId` / `flowId` / `revisionId`，节点事件额外带 `nodeId` / `nodeType` / `nodeName` / `agentId`。

### 审计表

| 表 | 内容 |
|----|------|
| `ea_agent_flow` | 图的基本信息 |
| `ea_agent_flow_revision` | 版本化的 `definition_json`，发布即固化 |
| `ea_agent_flow_run` | 每次运行：`runId`、`runType`（DEBUG/CHAT）、状态、入出参 JSON、token、起止时间 |
| `ea_agent_flow_node_run` | 每个节点每次执行：节点 ID/类型、`agentId`、状态、入出参、token、错误信息 |

`FlowRunRecorder` 的所有写入都 `try/catch` 只告警：审计失败不能中断用户的编排执行。

会话固化：一个 `sessionId` 第一次运行时把 `revisionId` 钉进 `ea_chat_conversation`，后续追问复用同一版本。中途发布新版不会让同一个会话前后半段跑在两张不同的图上。

### 执行轨迹

节点内部的思考与工具调用通过 `ChatTraceRecorder` 的节点作用域视图写入 `ea_chat_message_trace`，按 `seq` 保序。刷新页面后前端仍能按真实时序回放「思考 → 调工具 → 看结果 → 继续思考」。

---

## 9. 配置项

前缀 `easy-agent.flow`（`FlowOrchestrationProperties`），默认值写在代码里，不配也能启动。

| key | 默认 | 说明 |
|-----|------|------|
| `core-pool-size` / `max-pool-size` / `queue-capacity` | 4 / 32 / 256 | 节点执行线程池，**全局共享一个**。队列满后 `CallerRunsPolicy` 由调度线程直接执行，保证不丢任务 |
| `max-parallelism-per-run` | 8 | 单个 Flow 内并发节点上限，防止一次 FORK 打满共享池 |
| `node-timeout-seconds` | 300 | 单节点超时，判失败 |
| `flow-timeout-seconds` | 1800 | 整体超时，判取消 |
| `cancel-drain-seconds` | 10 | 收敛时等待在飞节点退出的时间 |
| `poll-interval-millis` | 200 | 调度循环检查取消与超时的间隔 |

---

## 10. 扩展：新增一种节点类型

以加一个 `CONDITION`（按表达式选分支，不调模型）为例：

1. `FlowNodeTypes` 加常量 `CONDITION`
2. `biz/flow/plugin/` 新建 `ConditionNodePlugin implements FlowNodePlugin`，标 `@Component`
3. `metadata()` 用 `NodeMetadataSupport.metadata(...)` 声明分类、图标、输入输出端口和 `configSchema`（前端据此自动渲染配置表单）
4. 端口随配置变化就覆盖 `prepare()`（参考 `RouterNodePlugin` 按 `branches` 生成出口端口）
5. 配置有约束就覆盖 `validate()`，用 `result.addError(target, nodeId, code, message)` 给出能定位的报错
6. `execute()` 返回 `NodeExecutionResult`：`outputs` 是端口产出，`routeKey` 用于选分支，`inputTokens`/`outputTokens` 用于计费

不需要改动 `DefaultFlowEngine`、`FlowCompiler` 或前端节点库——注册表、`node-types` 接口和画布节点列表都是自动发现的。

---

## 11. 已知限制

当前实现的边界，按优先级排列：

- **缺少 CONDITION / LOOP / TOOL 节点。** 控制流校验已经禁止成环并提示「循环必须使用专用 LOOP 节点」，但该节点还没实现，所以目前表达不了循环。
- **JOIN 只有一种聚合策略。** `configSchema` 里 `mode` 的枚举只有 `ALL`，`execute()` 直接把入参 Map 原样输出，没有 ANY / 带权重 / 自定义合并。
- **ROUTER 用 `choice.contains(key)` 匹配分支。** 分支 key 之间存在前缀包含关系时（如 `pay` 与 `payment`）可能误匹配，且取第一个命中。未命中会发 `node_log` 留痕后走 fallback。
- **没有节点级重试。** 单个节点失败即整条 Flow 失败，模型偶发超时也要整条重跑。
- **取消不能真正打断模型请求。** 原因见第 6 节收敛部分。
- **一次运行全程占用一个调度线程。** 调度线程在 `poll` 上等待，没有做成完全事件驱动。并发会话数受线程数约束，当前规模下不是瓶颈。
