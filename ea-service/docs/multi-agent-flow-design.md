# 多 Agent 编排（Flow）系统分析与设计文档

> 状态：**待确认**　|　作者：协作产出　|　目标版本：Phase 0 ~ Phase 4

---

## 1. 背景与目标

### 1.1 现状

当前系统仅支持**单 Agent** 对话。整条链路把 `agentId` 当成单一标量穿透到底：

- **唯一选 Agent 入口**：`AgentChatServiceImpl.streamChatWith()` —— 加载一个 Agent → 构建一个 `AgentContext` → 按 `toolRunMode` 直接 `new ReActAgentExecutor` / `new ToolAgentExecutor`。上方没有任何路由 / 编排层。
- `AgentContext`、`EaAgentDO` 所有字段都是标量，**没有任何字段指向另一个 Agent**。
- 关系表 `ea_tool_relation` / `ea_knowledge_relation` / `ea_skill_relation` / `ea_mcp_relation` 全是「资源 → 单 agent_id」，**不存在 agent↔agent 关系**。
- SSE 在 `BaseAgent.doExec()` 内部直接 `sse.complete()`，执行器与 SSE 生命周期强耦合。

### 1.2 目标

在**不破坏现有单 Agent 行为**的前提下，新增「多 Agent 编排（Flow）」能力，一套底座同时支撑三种编排策略：

| 策略 | 含义 | 典型场景 |
|------|------|----------|
| **WORKFLOW**（流水线） | 成员 Agent 按固定顺序串行，前者输出作为后者输入 | 文档生成 → 审校 → 翻译 |
| **SUPERVISOR**（主管） | 主管 Agent 把成员 Agent 当工具按需调用，可多轮 | 专家分工 |
| **ROUTER**（路由/分诊） | 路由 Agent 判断意图后把整个对话转交给某个成员 Agent | 客服分流 |

### 1.3 设计原则

1. **零侵入复用**：成员 Agent 仍是普通 `ea_agent`，可独立使用，也可被编排引用。
2. **底座一次性投入**：三种策略共用同一套数据模型 / 上下文 / 执行器接口；策略本身是薄实现类。
3. **向后兼容**：请求不带 `flowId` 时，走原单 Agent 路径，行为完全不变。

---

## 2. 数据模型

> 注：以下两张表 DDL 已落库并通过 MyBatis 生成 DO/DAO（见 §6 进度）。

### 2.1 `ea_agent_flow`（编排主表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 主键 |
| flow_name | varchar | 编排名称 |
| flow_key | varchar | 编排 Key（可选） |
| avatar | longtext | 头像（前端展示，对齐 ea_agent） |
| flow_desc | text | 备注 |
| **strategy** | varchar(64) | 编排策略：SUPERVISOR/ROUTER/WORKFLOW |
| supervisor_agent_id | bigint | 主管/路由 Agent ID；仅 SUPERVISOR、ROUTER 使用，WORKFLOW 留空 |
| prompt | text | 编排级提示词（主管/路由指令） |
| welcome_message | text | 欢迎语 |
| created_at / updated_at | timestamp | 时间戳 |

### 2.2 `ea_agent_flow_node`（编排成员节点表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 主键 |
| flow_id | bigint | 关联 ea_agent_flow.id |
| agent_id | bigint | 成员 Agent ID（ea_agent.id） |
| node_role | varchar | 节点角色描述：供主管选工具 / 路由分类用 |
| order_index | int | 执行顺序：WORKFLOW 按此串行 |
| created_at / updated_at | timestamp | 时间戳 |

### 2.3 关系图

```
ea_agent_flow (1) ───< ea_agent_flow_node (N) >─── (1) ea_agent
       │                                                  
       └── supervisor_agent_id ───────────────────────> ea_agent
```

---

## 3. 后端设计

### 3.1 分层与新增类一览

```
core/domain/enums/
  └─ FlowStrategyEnum                 # SUPERVISOR / ROUTER / WORKFLOW   ✅已建

core/domain/DO/
  ├─ EaAgentFlowDO                    # ✅已生成
  └─ EaAgentFlowNodeDO                # ✅已生成
core/mapper/
  ├─ EaAgentFlowDAO                   # ✅已生成
  └─ EaAgentFlowNodeDAO               # ✅已生成

core/service/(impl)
  └─ FlowManagerService               # 加载 flow + 有序成员 Agent，CRUD

biz/agent/flow/
  ├─ FlowContext                      # 编排运行期上下文（flow + 成员 + sse/session）
  ├─ FlowExecutor (接口)              # strategy() + exec(FlowContext, question)
  ├─ FlowExecutorManager              # 按 strategy 分发到具体编排器
  ├─ AgentContextFactory             # EaAgentResult → AgentContext（单/多 Agent 共用）
  └─ impl/
      ├─ WorkflowFlowExecutor         # 流水线   [Phase 1]
      ├─ SupervisorFlowExecutor       # 主管     [Phase 2]
      └─ RouterFlowExecutor           # 路由     [Phase 3]

biz/agent/
  └─ BaseAgent (改造)                 # 新增 ownSse 开关，解耦 sse.complete  ✅已改

biz/function/(tool)
  ├─ ToolTypeEnum (改造)              # 新增 AGENT 类型   [Phase 2]
  └─ AgentToolExecutor                # 把子 Agent 的 exec() 适配成工具   [Phase 2]
```

### 3.2 核心抽象

**FlowExecutor 接口**

```java
public interface FlowExecutor {
    FlowStrategyEnum strategy();              // 自注册用
    String exec(FlowContext ctx, String question);
}
```

**FlowExecutorManager**：Spring 注入 `List<FlowExecutor>`，构建 `Map<FlowStrategyEnum, FlowExecutor>`，对外 `exec(FlowContext, question)` 按 `ctx.strategy` 分发。（沿用项目已有的「按枚举注册执行器」模式，与 `FunctionToolManager` 一致。）

**FlowContext**：持有 `EaAgentFlowDO flow`、有序成员 `List<EaAgentResult> members`、`List<EaAgentFlowNodeDO> nodes`、共享 `SseEmitter sse`、`sessionId`、`imageBase64`、`strategy`。

**AgentContextFactory**（关键复用点）：把 `streamChatWith` 中「`EaAgentResult` → `AgentContext`（含工具 + MCP 工具加载）」的逻辑抽成 `@Component` 方法 `build(agent, sse, sessionId, imageBase64, syncMode)`。单 Agent 路径与编排节点共用，避免逻辑分叉。

### 3.3 SSE 收尾解耦（已落地）

`BaseAgent` 新增 `protected boolean ownSse = true` 与 `setOwnSse()`；`doExec()` 中 finish / 异常分支的 `sse.complete()` 改为 `if (ownSse && sse != null)`。

- 单 Agent：`ownSse=true`，行为不变。
- 编排子 Agent：编排器创建执行器后 `setOwnSse(false)`，由编排器在全部节点完成后统一 `sse.complete()`。

### 3.4 三种编排器执行逻辑

**WorkflowFlowExecutor（Phase 1）**
```
input = question
for node in nodes (按 order_index 升序):
    ctx = AgentContextFactory.build(node.agent, sse, ...)
    executor = new (ReAct|Tool)AgentExecutor(ctx); executor.setOwnSse(false)
    SseHelper.sendStep(sse, node)                 # 标识当前节点
    output = executor.exec(input)
    input = 拼接(上一步输出, 作为下一步输入)
finalAnswer = 最后一个 node 的 output
SseHelper.sendFinalAnswer(sse, finalAnswer); sse.complete()
```

**SupervisorFlowExecutor（Phase 2）**
```
把每个成员 Agent 包装成 ToolDefinition(type=AGENT, name=agentKey, desc=node_role)
注入主管 AgentContext.toolDefinitions
主管正常跑 ToolAgentExecutor 循环 —— 主管自行决定调用哪个子 Agent
子 Agent 调用经 AgentToolExecutor → new ToolAgentExecutor(subCtx, ownSse=false).exec(input)
```
> 复用现有 `ToolTypeEnum` + `FunctionToolManager` 分发机制，几乎不碰核心循环。

**RouterFlowExecutor（Phase 3）**
```
用路由 prompt + 成员 node_role 列表，让路由 LLM 输出选中的成员 index/key（一次决策）
SseHelper.sendHandoff(sse, 选中成员)
直接 new (ReAct|Tool)AgentExecutor(选中成员 ctx, ownSse=true 或由编排器收尾).exec(question)
```

### 3.5 入口改造（向后兼容）

- `StreamChatPostRequest` 新增可选字段 `Long flowId`。
- `AgentChatServiceImpl.streamChatWith()`：
  - `flowId != null` → `FlowManagerService.loadFlow(flowId)` 构建 `FlowContext`（填充 sse/session）→ `flowExecutorManager.exec(ctx, question)`。
  - 否则走现有单 Agent 路径（改为复用 `AgentContextFactory`，逻辑等价）。

### 3.6 聊天记录（ChatRecordSaver）

现状：`streamChatWith` 调一次 `startNewConversation`，`BaseAgent.doExec` 在 finish 时调 `saveAgentFinish`（基于 ThreadLocal）。

编排下多个子 Agent 在同一线程串行，会多次 `saveAgentFinish`。

- **Phase 1 处理方案（MVP）**：编排开始时 `startNewConversation` 一次；每个子 Agent 的 finish 作为一条消息追加（天然记录每个 Agent 的产出）。
- **待确认点**：是否需要把「编排级会话」与「单 Agent 会话」在 `ea_chat_conversation` 上区分（例如增加 `flow_id` 字段）？见 §7 开放问题。

---

## 4. SSE 协议扩展

### 4.1 现有事件与问题

后端 `SseHelper` 现有事件，前端 `ChatApi.ts` 的 `onmessage` 逐一分发（`log` 仅后端打日志、不下发）：

| type | 含义 | 前端回调 |
|------|------|----------|
| `think` | 思考过程 | `onThink` |
| `data` | 流式回答片段 | `onData` |
| `tool` | 工具调用及结果 | `onTool` |
| `finalAnswer` | 最终完整答案 | `onFinalAnswer` |
| `error` | 错误 | `onError` |
| `log` | 日志（不下发前端） | — |

事件载荷固定为 `{"type":"...","content":"..."}`。

**核心问题**：这 6 种事件**全是「无 Agent 身份」的**——前端默认整条流只有一个发言者，把 `think/data/tool` 直接拼进同一个气泡。多 Agent 编排下，多个子 Agent 的 `think/data/tool` 会在同一条 SSE 流里交替出现，**前端无法区分「这段是谁说的」**，会糊成一团。

### 4.2 方案：内容事件带 Agent 身份 + 新增转场事件

**(A) 给所有内容事件补充可选 Agent 身份字段**（向后兼容：单 Agent 不带，前端忽略即可）

载荷扩展为：
```json
{ "type": "data", "content": "...", "agentId": 7, "agentName": "翻译Agent", "avatar": "..." }
```
前端据此把片段**归属到对应子 Agent**，无需依赖事件顺序维护「当前 Agent」状态，对 SUPERVISOR 的嵌套调用尤其稳。

**(B) 新增「转场」事件**（驱动 UI 分隔/标头）

| type | 含义 | 触发策略 |
|------|------|----------|
| `step` | 流水线进入某节点（含 agentName / 序号 / 总数） | WORKFLOW |
| `handoff` | 对话转交给某成员（含 agentName / 原因） | ROUTER |
| `agent` | 当前发言子 Agent 切换（含 agentId/Name/avatar） | SUPERVISOR |

`SseHelper` 增加 `sendStep / sendHandoff / sendAgent`，并提供「带 agent 身份」的 `sendData/sendThink/sendTool` 重载。

> **待确认**（见 §7-7）：身份归属用 **(A) 每事件带 agentName**（健壮、但改 JSON schema）还是 **仅 (B) 转场事件 + 前端维护当前 Agent 状态**（改动小、但交替/嵌套时易错）。建议 **(A)+(B) 并用**。

---

## 5. 前端设计（Phase 4）

### 5.1 SSE 解析层（`ChatApi.ts`）

现状：`sendMessage` 用一长串**位置回调**（`onLog/onFinalAnswer/onThink/onData/onTool/onDone/onError`），`onmessage` 里 `if/else` 逐个 type 分发。新增 `step/handoff/agent` 会让位置参数继续膨胀。

- **重构建议**：把位置回调改为单一 `onEvent(evt: SseEvent)` 或 handlers 对象 `{ onData, onStep, ... }`，新增事件不再改函数签名。
- `onmessage` 增加 `step / handoff / agent` 分支；并从每个事件载荷读取可选 `agentId/agentName/avatar` 透传给上层。
- 请求体增加可选 `flowId`（与 `agentId` 二选一）。

### 5.2 聊天展示层（`views/page/chat/`）

现有组件假设单一发言者：`ChatComponents.tsx` 组合渲染、`MessageBubble.tsx` 富文本、`ThinkComponent/ToolComponent/LogComponent` 分别展示思考/工具/日志，类型定义在 `chat/types.ts`。

多 Agent 改造：
- **按 Agent 分组渲染**：消息模型增加 `agentId/agentName/avatar` 维度；同一 Agent 的连续 `think/data/tool` 归到同一「子 Agent 段」，段首显示头像+名称。
- **转场视觉**：`step` → 流水线步骤条/分隔符（「① 生成 → ② 审校 → ③ 翻译」）；`handoff` → 「已转接至 XX」提示条；`agent` → 主管调用子 Agent 的嵌套折叠块。
- `chat/types.ts` 的 `ChatMessage / ThinkingProcess / ToolCall` 增加 Agent 身份字段。

### 5.3 编排管理与入口

- **编排管理 UI**：新增 Flow 列表 / 编辑页（选策略、选成员 Agent、排序、配主管/路由 prompt、欢迎语/头像）。
- **会话入口**：聊天页支持以 `flowId` 发起对话（URL 参数 `?flowId=` 或选择器）；与现有 `?agentId=` 并存。

### 5.4 类型整治

抽出共享 `Agent` TS 接口（现 `EaAgentApi` 为 `any`，`AgentDetail` 在两处重复），新增 `Flow` / `FlowNode` / `SseEvent` 类型。

---

## 6. 实施阶段与当前进度

| 阶段 | 内容 | 状态 |
|------|------|------|
| **Phase 0** | 建表 + DO/DAO 生成 | ✅ 完成 |
| Phase 0 | `FlowStrategyEnum` | ✅ 完成 |
| Phase 0 | `BaseAgent` ownSse 解耦 | ✅ 完成 |
| Phase 0 | `FlowContext` / `FlowExecutor` / `FlowExecutorManager` / `AgentContextFactory` / `FlowManagerService` 骨架 | ✅ 完成 |
| Phase 0 | `streamChatWith` 接入 flow 分支 | ✅ 完成 |
| **Phase 1** | `WorkflowFlowExecutor` + 端到端验证 | ✅ 编码完成，待编译/联调 |
| Phase 2 | `ToolTypeEnum.AGENT` + `AgentToolExecutor` + `SupervisorFlowExecutor` | ⏸ |
| Phase 3 | `RouterFlowExecutor` | ⏸ |
| **Phase 4** | 前端 `ChatApi` handlers 重构 + `flowId` 入口 + `step` 渲染 | ✅ 完成（简单版） |
| Phase 4 | 后端 Flow CRUD 接口（`FlowController` + `FlowAdminService`） | ✅ 完成，待编译 |
| Phase 4 | 编排管理 UI（Flow 列表/编辑 CRUD + `?flowId=` 入口） | ✅ 完成（简单版） |

> 说明：本文档定稿前，编码已先行落地了无歧义、低风险的底座部分（建表、DO/DAO、ownSse 解耦、枚举）。其余编码均**待本文档确认后**继续。

---

## 7. 开放问题（需确认）

1. **会话归属**：`ea_chat_conversation` 是否需新增 `flow_id` 区分编排会话？还是复用 `agent_id`（指向主管/首节点）？
2. **WORKFLOW 节点间传递格式**：仅传「上一步纯文本输出」，还是保留「原始问题 + 各步输出」累积上下文？
3. **SUPERVISOR 子 Agent 强制 Tool 模式**：子 Agent 被当工具调用时，是否强制走 `ToolAgentExecutor`（忽略其自身 `toolRunMode`）？
4. **ROUTER 失败兜底**：路由 LLM 选不出成员时，默认转交哪个成员（首节点 / 报错）？
5. **前端范围**：Phase 4 是否本期必做，还是先用接口（Postman/默认 flowId）验证后端？
6. **token 统计**：编排下多个子 Agent 的 token 如何汇总到会话累计值？
7. **SSE 身份归属方案**：采用 §4.2 的 **(A) 每个内容事件带 `agentName`**、**仅 (B) 转场事件 + 前端维护当前 Agent**、还是 **(A)+(B) 并用**（建议）？此决定影响 `SseHelper` 是否要改 JSON schema 及前端渲染复杂度。
8. **前端回调重构**：`ChatApi.sendMessage` 的位置回调是否改为单一 `onEvent` / handlers 对象（避免新增事件继续堆参数）？

---

## 8. 风险

- **ThreadLocal 上下文**：`ChatRecordSaver` 依赖 ThreadLocal，子 Agent 必须在同一线程串行执行（当前编排器为同步串行，满足）。若未来引入并行编排需重新设计。
- **SSE 单连接多 Agent**：解耦后由编排器统一收尾；需保证异常路径也能 `complete`，避免连接悬挂。
- **决策轮数 / 卡死检测**：`BaseAgent` 的 `DECISION_CNT_LIMIT=20`、`STUCK_CNT_LIMIT=3` 为单 Agent 粒度；SUPERVISOR 嵌套调用时需评估总轮数上限。
