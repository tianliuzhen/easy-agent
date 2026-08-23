# Easy Agent 可视化多 Agent 编排设计

> 状态：重新设计，旧版 `FlowStrategyEnum + FlowExecutor` 方案废弃  
> 目标：用户在页面拖拽节点、连接类型化端口、配置参数并发布，后端动态编译和执行编排图。

## 1. 设计结论

多 Agent 编排不再建模为“一个 Flow 选择一种策略”，而是建模为一张有向图：

- 节点定义能力，例如 Agent、路由、并行网关、聚合、主管和结束。
- 控制边定义节点的触发关系。
- 数据边把上游输出端口映射到下游输入端口。
- Worker 边把普通 Agent 挂载为 Supervisor 可按需调用的子 Agent。
- 前端保存图定义，后端负责校验、编译、调度和观测。
- Flow 发布后生成不可变 Revision，运行永远引用明确版本。

旧版 `ea_agent_flow_node` 成员列表、`FlowStrategyEnum`、`FlowExecutorManager`、`WorkflowFlowExecutor` 和 `RouterFlowExecutor` 不作为兼容基础，实施新方案时直接删除。

## 2. 产品形态

编排页面采用四区布局：

1. 顶部操作栏：名称、草稿状态、保存、校验、试运行、发布。
2. 左侧节点库：基础节点、Agent 节点、控制节点、数据处理节点。
3. 中央画布：拖拽、缩放、删除、端口连线和运行状态覆盖。
4. 右侧配置面板：节点参数、Agent 选择、Router 动态分支和端口查看。

底部可展开调试面板，展示本次运行的节点时间线、输入输出、Token、工具调用和错误。

### 2.1 核心交互

- 从节点库拖入画布时创建节点。
- 从输出端口拖线到兼容的输入端口。
- 连线时高亮兼容端口，禁止不兼容连接。
- 选择节点后，右侧面板根据节点的 JSON Schema 自动生成配置表单。
- 数据输入既可以连接上游端口，也可以填写常量或模板表达式。
- 点击“校验”只编译不运行，并把错误定位到节点或边。
- 点击“试运行”输入测试参数，通过 SSE 在画布上显示执行状态。
- 点击“发布”生成不可变 Revision；修改已发布 Flow 会创建新草稿。

## 3. 图领域模型

### 3.1 FlowDefinition

```java
public record FlowDefinition(
        String schemaVersion,
        Long flowId,
        Long revisionId,
        String name,
        List<FlowNodeDefinition> nodes,
        List<FlowEdgeDefinition> edges
) {}
```

### 3.2 FlowNodeDefinition

```java
public record FlowNodeDefinition(
        String id,
        String nodeType,
        String nodeVersion,
        String name,
        NodePosition position,
        Map<String, Object> config,
        List<PortDefinition> inputPorts,
        List<PortDefinition> outputPorts
) {}
```

### 3.3 PortDefinition

```java
public record PortDefinition(
        String key,
        String name,
        PortKind kind,
        ValueType valueType,
        boolean required,
        boolean multiple,
        Object defaultValue
) {}

public enum PortKind {
    CONTROL,
    DATA,
    WORKER
}
```

端口类型第一版支持：

- `STRING`
- `NUMBER`
- `BOOLEAN`
- `JSON`
- `IMAGE`
- `MESSAGE`
- `MESSAGE_LIST`
- `ANY`

复杂类型携带可选 JSON Schema。连接规则由后端 `PortTypeSystem` 统一判断，前端通过节点元数据复用相同规则。

### 3.4 FlowEdgeDefinition

```java
public record FlowEdgeDefinition(
        String id,
        EdgeKind kind,
        PortRef source,
        PortRef target,
        Map<String, Object> config
) {}

public record PortRef(String nodeId, String portKey) {}

public enum EdgeKind {
    CONTROL,
    DATA,
    WORKER
}
```

边不保存业务执行代码，只表达连接关系和少量映射配置。数据转换应使用显式的 Template/Transform 节点，避免把复杂表达式隐藏在线上。

## 4. 节点目录

### 4.1 第一阶段节点

| 节点 | 类型 | 作用 |
|---|---|---|
| 开始 | `START` | 声明 Flow 输入并触发执行 |
| Agent | `AGENT` | 引用已有 `ea_agent`，复用其模型、Prompt、工具、MCP、Skill |
| 路由 | `ROUTER` | 使用指定 Agent/模型输出分支 Key，只激活一个控制出口 |
| 并行开始 | `FORK` | 同时激活多个控制出口 |
| 聚合 | `JOIN` | 等待所有已激活上游分支并合并输入（当前为 ALL） |
| 主管 | `SUPERVISOR` | 把 Worker 边连接的 Agent 动态包装为工具，按需多轮调用 |
| 模板转换 | `TEMPLATE` | 用 Jinjava/FreeMarker 把多个输入转换成下游参数 |
| 结束 | `END` | 声明 Flow 最终输出 |

顺序执行不需要 `WORKFLOW` 节点：一条控制链天然就是顺序流程。并行也不是执行器策略，而是 `FORK + 多分支 + JOIN` 的图拓扑。

### 4.2 后续节点

- `CONDITION`：非 LLM 条件判断。
- `LOOP`：显式循环边界、最大次数和结束条件。
- `SUB_FLOW`：引用一个已发布 Flow Revision。
- `HUMAN_APPROVAL`：人工确认后恢复。
- `HTTP/SQL/MCP/SKILL`：允许工具作为独立节点执行，而不仅绑定在 Agent 内。

## 5. 动态节点插件

后端节点能力不写在 `switch` 中，使用 Spring Bean 自注册：

```java
public interface FlowNodePlugin {

    NodeMetadata metadata();

    default void prepare(FlowNodeDefinition definition) {}

    default void validate(FlowNodeDefinition definition, FlowValidationResult result) {}

    NodeExecutionResult execute(NodeExecutionContext context);
}
```

`NodeMetadata` 是前后端共同契约：

```java
public record NodeMetadata(
        String type,
        String version,
        String category,
        String displayName,
        String description,
        String icon,
        List<PortMetadata> ports,
        JsonNode configSchema,
        JsonNode uiSchema
) {}
```

启动时由 `FlowNodePluginRegistry` 注入所有插件并按 `type + version` 注册。新增节点类型只需增加插件；前端调用 `/eaAgent/flow/node-types` 后自动显示节点和配置表单。

前端不应维护另一套写死的节点参数定义。后端元数据是节点能力的唯一事实来源，前端仅为少量复杂节点提供定制 Inspector。

## 6. 图存储与版本

画布图是一个强一致整体，节点/边频繁增删，适合按 Revision 保存 JSON，而不是把每个节点拆成 CRUD 表。

### 6.1 `ea_agent_flow`

```sql
CREATE TABLE ea_agent_flow (
    id                    BIGINT PRIMARY KEY AUTO_INCREMENT,
    flow_key              VARCHAR(128) NOT NULL,
    flow_name             VARCHAR(255) NOT NULL,
    flow_desc             TEXT,
    avatar                LONGTEXT,
    welcome_message       TEXT,
    current_draft_id      BIGINT,
    published_revision_id BIGINT,
    created_by            VARCHAR(100),
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_flow_key (flow_key)
);
```

### 6.2 `ea_agent_flow_revision`

```sql
CREATE TABLE ea_agent_flow_revision (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    flow_id         BIGINT NOT NULL,
    revision_no     INT NOT NULL,
    status          VARCHAR(32) NOT NULL COMMENT 'DRAFT/PUBLISHED/ARCHIVED',
    schema_version  VARCHAR(32) NOT NULL,
    definition_json LONGTEXT NOT NULL,
    checksum        VARCHAR(64) NOT NULL,
    validation_json LONGTEXT,
    created_by      VARCHAR(100),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at    TIMESTAMP NULL,
    UNIQUE KEY uk_flow_revision (flow_id, revision_no),
    KEY idx_flow_status (flow_id, status)
);
```

### 6.3 运行记录

新增：

- `ea_agent_flow_run`：一次 Flow 运行的状态、Revision、输入输出、Token、耗时。
- `ea_agent_flow_node_run`：每次节点尝试的状态、输入输出、Agent、错误、Token、开始结束时间。

聊天会话继续保留 `flow_id`，并增加 `flow_revision_id`，保证历史会话可以追溯到当时发布版本。

## 7. 图编译

保存草稿只做 JSON Schema 基础校验；校验和发布时执行完整编译。

编译步骤：

1. 解析 `definition_json`。
2. 校验节点类型和版本是否存在。
3. 根据插件规则校验节点配置，并向前端暴露 JSON Schema 表单描述。
4. 校验端口存在、方向、类型和多重连接限制。
5. 校验 START/END 数量与可达性。
6. 校验必填数据端口有连线、默认值或表达式。
7. 校验普通控制图无环；循环只能经过 LOOP 节点。
8. 校验 Router 分支 Key 唯一且 fallback 有效。
9. 校验 Worker 边只能由 Supervisor 指向 Agent，且 Worker Agent 不混入普通控制流。
10. 生成不可变 `CompiledFlowPlan` 和 checksum。

```java
public record CompiledFlowPlan(
        Long flowId,
        Long revisionId,
        String checksum,
        Map<String, CompiledNode> nodes,
        DependencyGraph controlGraph,
        DependencyGraph dataGraph
) {}
```

编译计划按 `revisionId + checksum` 缓存。SSE、session、用户、图片等请求数据只存在于 `FlowRunContext`，不能进入缓存。

## 8. 执行引擎

执行引擎采用依赖驱动调度，不再按 Flow 策略分发。

```java
public interface FlowEngine {

    FlowExecutionResult execute(
            CompiledFlowPlan plan,
            FlowInput input,
            FlowRunContext context
    );
}
```

每个节点有独立状态：

```text
PENDING → READY → RUNNING → SUCCEEDED
                         ├→ FAILED
                         ├→ SKIPPED
                         └→ CANCELLED
```

调度规则：

- 控制依赖满足后节点才可 READY。
- 所有 required 数据端口有值后节点才可执行。
- FORK 分支进入有界线程池并行执行。
- JOIN 等待所有相关控制边进入 ACTIVE/SKIPPED 终态，并在至少一个分支激活时继续。
- Router 只激活命中的控制出口，其他分支标记 SKIPPED。
- 请求取消会终止调度；节点异常默认采用 FAIL_FLOW。

节点级超时、重试、ANY/N 聚合和可配置错误策略属于下一阶段能力，不在当前首版中伪装成已实现功能。

运行值以端口为地址保存：

```java
Map<PortRef, FlowValue> values;
```

避免所有 Agent 共享一份无类型全局字符串。

## 9. Agent 节点适配

Agent 节点复用当前 Easy Agent 能力，但执行生命周期由 Flow Engine 管理：

- 通过 `AgentContextFactory` 加载模型、工具、MCP 和 Skill。
- 根据 Agent 配置选择 `ToolAgentExecutor` 或 `ReActAgentExecutor`。
- 设置 `ownSse=false`、`saveRecord=false`。
- 返回统一 `NodeExecutionResult`，不在节点内部结束 Flow。
- 节点记忆 Session 使用 `flowSessionId:nodeId`，禁止不同 Agent 共享同一个 ChatMemory Key。
- Flow 级对话历史与节点私有记忆分开管理。

主管节点在运行时把 Worker Agent 包装成临时 `ToolCallback`/`FunctionCallback`。它们不是持久化工具，因此不加入 `ToolTypeEnum`，也不写入 HTTP/SQL/MCP/SKILL 工具表。

## 10. 事件与调试

节点不能直接持有 `SseEmitter` 拼装 JSON，而是发布结构化事件：

```java
public record FlowEvent(
        String runId,
        Long flowId,
        Long revisionId,
        String nodeId,
        String nodeType,
        Long agentId,
        String eventType,
        Object data,
        Instant timestamp
) {}
```

事件类型包括：

- `flow_started`
- `node_ready`
- `node_started`
- `node_data`
- `node_think`
- `node_tool`
- `node_succeeded`
- `node_failed`
- `node_skipped`
- `flow_succeeded`
- `flow_failed`

`FlowEventPublisher` 可以有 SSE、日志、数据库追踪等多个订阅器。前端根据 `nodeId` 更新画布节点状态，并在调试面板展示输入输出。

## 11. 前端技术设计

### 11.1 技术选型

采用 `@xyflow/react`：

- React 原生自定义节点和端口。
- 支持拖拽、缩放、连线、多选、MiniMap、Controls。
- 与当前 React 19、TypeScript、Ant Design 组件体系结合成本较低。
- 节点内部可以直接复用 Ant Design 表单和图标。

第一阶段不引入另一套低代码平台或 iframe 编辑器。

### 11.2 目录建议

```text
ea-web/src/views/page/flow/
├── FlowListPage.tsx
├── FlowStudioPage.tsx
├── api/
│   └── FlowStudioApi.ts
├── components/
│   ├── FlowToolbar.tsx
│   ├── NodePalette.tsx
│   ├── GraphCanvas.tsx
│   ├── NodeInspector.tsx
│   ├── EdgeInspector.tsx
│   └── FlowDebugPanel.tsx
├── nodes/
│   ├── BaseFlowNode.tsx
│   ├── AgentNode.tsx
│   ├── RouterNode.tsx
│   ├── GatewayNode.tsx
│   └── SupervisorNode.tsx
├── hooks/
│   ├── useFlowDocument.ts
│   ├── useFlowValidation.ts
│   └── useFlowDebugRun.ts
└── types/
    └── flowStudio.ts
```

### 11.3 前端状态边界

- React Flow 管理节点、边和 viewport。
- `useFlowDocument` 管理脏状态、撤销重做、草稿保存和 Revision。
- Inspector 修改节点 config 时生成不可变节点更新。
- 自动保存采用 1~2 秒 debounce，只保存 DRAFT。
- 发布前必须使用服务端编译结果，不能只依赖前端校验。

### 11.4 配置表单

普通节点根据 `configSchema + uiSchema` 自动生成表单，复杂节点可以注册定制组件：

```ts
interface NodeInspectorRegistry {
  [nodeType: string]: React.ComponentType<NodeInspectorProps>;
}
```

输入字段提供三种来源：

1. 连接端口。
2. 常量。
3. 模板表达式，例如 `{{ nodes.writer.text }}`。

画布保存 `position`，运行引擎忽略 UI 位置。

## 12. API

```text
GET    /eaAgent/flow/list
POST   /eaAgent/flow
GET    /eaAgent/flow/{flowId}
DELETE /eaAgent/flow/{flowId}

GET    /eaAgent/flow/node-types
GET    /eaAgent/flow/{flowId}/draft
PUT    /eaAgent/flow/{flowId}/draft
POST   /eaAgent/flow/{flowId}/validate
POST   /eaAgent/flow/{flowId}/publish
POST   /eaAgent/flow/{flowId}/debug-run
```

正式聊天仍可使用 `/eaAgent/ai/chat` 和 `flowId`，但服务端只执行已发布 Revision。调试接口允许执行当前草稿，并明确标记为 DEBUG。

Revision 列表、运行详情、取消和事件回放 API 作为后续观测能力扩展；当前首版已经写入运行表，但不提前暴露空接口。

## 13. 删除范围

新引擎落地时删除：

- `FlowStrategyEnum`
- `FlowExecutor`
- `FlowExecutorManager`
- `FlowContext`
- `WorkflowFlowExecutor`
- `RouterFlowExecutor`
- 旧 `FlowManagerService` 加载逻辑
- 旧 `FlowAdminService` 节点全量替换逻辑
- 旧 `FlowManager.tsx`
- 旧 `FlowApi.ts`
- `ea_agent_flow.strategy`
- `ea_agent_flow.supervisor_agent_id`
- `ea_agent_flow.prompt`
- 旧 `ea_agent_flow_node` 表

可以保留：

- `StreamChatPostRequest.flowId`
- `ea_chat_conversation.flow_id`
- `AgentContextFactory`，但改造成单 Agent/Flow Agent 共同入口
- `BaseAgent` 的 SSE 所有权和记录所有权开关
- Agent、模型、工具、MCP、Skill 的现有配置体系

## 14. 实施状态与后续阶段

### 已完成：图契约与持久化

- 新 DDL、Revision 模型、FlowDefinition JSON。
- Node Plugin SPI、节点元数据 API。
- 图校验器和编译器。
- START/AGENT/END 插件及单链路单元测试。

### 已完成：可视化编辑器

- 引入 `@xyflow/react`。
- 实现节点库、画布、端口连接、Inspector、草稿和发布。
- 服务端校验错误定位到节点/边。

### 已完成：运行引擎和基础观测

- 依赖调度、运行/节点运行表、结构化事件。
- 画布试运行、节点状态和调试面板。
- 修复 Token 为本次增量统计。

### 已完成：多 Agent 控制节点首版

- ROUTER、FORK、JOIN。
- SUPERVISOR + 动态 Worker Agent 工具。
- Router 分支跳过传播、FORK 并发和 JOIN 聚合测试。

### 后续：高级能力

- CONDITION、LOOP、SUB_FLOW、人工审批。
- 独立工具节点和结构化输出。
- Revision 对比、回滚、运行重放。
- 节点超时、重试、失败策略、运行取消和 ANY/N 聚合。

## 15. 第一版验收标准

1. 用户能拖入 START、三个 Agent、FORK、JOIN、END 并完成端口连线。
2. 错误类型、缺失必填输入在发布前被阻断，悬空节点以警告定位。
3. 发布生成不可变 Revision，修改 Flow 不影响正在运行或历史会话。
4. 顺序图按边执行，并行图真实并发，Router 只激活命中分支。
5. 每个节点拥有独立记忆 Key、Token、输入输出和错误记录。
6. SSE 事件带 runId/nodeId/agentId，前端能准确显示每个节点状态。
7. Supervisor 可以动态调用画布 Worker 边连接的 Agent，而不写死 Java 配置。
8. 新增一种节点插件时，前端节点库和普通配置表单无需修改代码即可识别。
