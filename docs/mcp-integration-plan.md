# Easy-Agent MCP 集成计划

> 基于当前项目架构分析，为 Agent 系统集成 MCP（Model Context Protocol）的完整实施规划。
> 生成日期：2026-04-02

---

## 一、项目现状

| 维度 | 现状 |
|------|------|
| **Agent 系统** | 已成熟运行，`ReActAgentXmlExecutor` 为主力执行器，`ToolAgentExecutor` 为辅助执行器 |
| **工具类型** | 已支持 `HTTP`、`SQL` 两种类型，通过 `ToolDefinition` + `FunctionToolManager` + `ToolExecutor` 架构进行分发 |
| **MCP 现状** | 独立的 `ea-mcp` 模块（天气示例 Server），**未与 `ea-service` 打通**；前端有 MCP UI 占位；`ToolTypeEnum` 尚未声明 MCP 类型 |
| **Spring AI 版本** | `ea-service` 使用 `1.0.0-M6`，`ea-mcp` 使用 `2.0.0-M2`，存在版本差异 |
| **前端** | React 19 + TypeScript + Vite + @ant-design/x 2.4.0，已构建现代化 Chat UI，支持 think / data / tool / log SSE 事件 |

---

## 二、MCP 集成计划

### 阶段一：基础能力铺垫（1-2 周）

#### 1.1 统一 Spring AI 版本
- **问题**：`ea-service`（1.0.0-M6）和 `ea-mcp`（2.0.0-M2）版本不一致，MCP Client API 存在差异。
- **建议方案 A（推荐）**：将 `ea-service` 升级至 `2.0.0-M2` 或更新的 `2.0.0-M3`，与 `ea-mcp` 对齐。
  - 需评估升级对 OpenAI、Ollama、Anthropic、VectorStore 等模块的影响。
- **建议方案 B**：保持 `ea-service` 在 `1.0.0-M6`，手动引入 `spring-ai-mcp-client` 的兼容版本，但后续维护成本高，不推荐。

#### 1.2 数据模型扩展
- **修改 `ToolTypeEnum`**（`ea-service/src/main/java/com/aaa/easyagent/core/domain/enums/ToolTypeEnum.java:16`）
  - 新增 `MCP("MCP", "MCP调用", McpParamsTemplate.class)`
- **新增 `McpParamsTemplate`**（继承 `ParamsTemplate`）
  - 字段设计：
    ```java
    private String serverName;        // MCP Server 名称/标识
    private String serverUrl;         // MCP Server URL (stdio/sse)
    private String transportType;     // STDIO / SSE
    private String command;           // stdio 模式下的启动命令
    private List<String> envVars;     // 环境变量
    private String toolName;          // 要调用的具体工具名
    ```
- **数据库兼容**：`ea_tool_config` 的 `tool_type` 字段已预留 `(SQL, HTTP, MCP, GRPC等)`，无需 DDL 变更。

#### 1.3 新增 `McpToolExecutor`
- 在 `com.aaa.easyagent.biz.tool.instance` 包下新增 `McpExecutor.java`
- 实现 `ToolExecutor<McpParamsTemplate>` 接口
- 使用 `@ToolTypeChooser(ToolTypeEnum.MCP)` 注册
- **核心职责**：
  1. 根据 `McpParamsTemplate` 初始化 MCP Client（`McpClient`）
  2. 支持两种传输层：
     - **STDIO**：通过 `StdioClientTransport` 启动子进程
     - **SSE**：通过 `HttpClientSseClientTransport` 连接远程 MCP Server
  3. 调用 MCP Server 的 `tools/call` 方法
  4. 将结果返回给 Agent

---

### 阶段二：MCP Client 与 Agent 执行链路打通（2 周）

#### 2.1 MCP Server 配置管理
- **新增领域对象**：
  - `EaMcpServerConfigDO`：MCP Server 连接配置表
  - `McpServerConfigService`：CRUD + 连接测试（ping / list tools）
- **REST API**：
  | 接口 | 说明 |
  |------|------|
  | `POST /mcp/server` | 新增配置 |
  | `PUT /mcp/server/{id}` | 更新配置 |
  | `GET /mcp/server` | 列表查询 |
  | `POST /mcp/server/{id}/test` | 测试连接并返回可用工具列表 |
  | `GET /mcp/server/{id}/tools` | 获取 Server 提供的工具清单 |

#### 2.2 Agent 绑定 MCP 工具
- 在 `ToolMangerService.listBoundToolsByAgentId(agentId)` 中，当工具类型为 `MCP` 时：
  - 解析 `EaToolConfigDO.toolValue` -> `McpParamsTemplate`
  - 通过 `McpServerConfigService` 补全 Server 连接信息
  - 生成 `ToolDefinition` 并注入 Agent 上下文
- **关键兼容性处理**：MCP 工具需要动态从远程 Server 获取 `inputSchema`，因此绑定流程中需要“同步/缓存”工具元数据。

#### 2.3 Agent 执行器适配
- `BaseAgent.buildToolFun()` 中，`ToolDefinition` 转 `FunctionCallback` 的逻辑对 MCP 类型保持透明（已统一通过 `FunctionToolManager.call()` 分发）
- `ReActAgentXmlExecutor` 和 `ToolAgentExecutor` 无需改动即可支持 MCP 调用
- **需验证**：
  - MCP 工具执行的超时控制
  - 异常时向前端发送 `tool` 类型的失败事件
  - MCP Server 断开后的重连/降级策略

---

### 阶段三：前端 UI 与交互完善（1-2 周）

#### 3.1 MCP 配置页面
- 现有 `MCPConfig.tsx` 已有表单 UI，但对接的是 mock 数据
- **对接后端真实 API**：
  - 配置列表、增删改查
  - 连接测试按钮，测试成功后展示可用工具列表
  - 支持 STDIO / SSE 两种传输方式切换

#### 3.2 Agent 工具绑定页面
- 现有 `AgentToolBinding` 和 `AddResourceModal` 已包含 `MCP` 选项
- **实现 MCP 类型的真实绑定流程**：
  - 选择 MCP Server -> 加载该 Server 的工具列表 -> 选择具体工具 -> 自动填充 inputSchema
  - 保存到 `ea_tool_config` 表（`tool_type='MCP'`）

#### 3.3 Chat 消息展示
- 现有 `ToolComponent.tsx` 已支持工具卡片展示
- **增强 MCP 工具展示**：
  - 在工具卡片中标识工具来源为 MCP
  - 展示 MCP Server 名称和传输类型
  - 支持失败重试（通过 SSE 发送 `retry` 指令）

---

### 阶段四：MCP Server 自身演进（1 周）

#### 4.1 扩展现有 `ea-mcp` 模块
- 当前 `ea-mcp` 仅有一个 `WeatherService` 示例
- **将 `ea-mcp` 发展为 Easy-Agent 自身的对外能力出口**：
  - 把现有内部能力（如 SQL 查询、HTTP 代理、知识库检索）封装为 MCP Server 暴露出去
  - 其他外部 Agent（如 Cursor、Claude Desktop）可通过 MCP 协议调用 Easy-Agent 的能力

#### 4.2 部署方式
- `ea-mcp` 作为独立服务通过 SSE 暴露
- 或提供 `npx` / CLI 方式的 STDIO Server，供本地 IDE 插件接入

---

## 三、关键技术决策建议

| 决策项 | 推荐方案 | 理由 |
|--------|----------|------|
| Spring AI 版本 | 统一升级至 `2.0.0-M2+` | MCP Client 在 2.x 中更稳定，API 更成熟 |
| 传输协议 | 优先实现 **SSE**，再补充 **STDIO** | SSE 更适合 Server-to-Server 架构；STDIO 适合本地 IDE |
| MCP Client 生命周期 | 按 `serverUrl` 做连接池缓存 | 避免每次工具调用都新建连接 |
| 工具元数据同步 | 绑定 Agent 时从 MCP Server 拉取并缓存 | 减少运行时依赖，提升容错性 |
| 错误处理 | 统一走 `SseHelper.sendTool()` 向前端 | 保持与 HTTP/SQL 工具一致的体验 |

---

## 四、实施优先级

```
P0: 升级 Spring AI 版本 + 新增 MCP 类型枚举 + McpParamsTemplate
P1: 实现 McpExecutor（SSE 优先）+ MCP Server 配置管理 API
P2: Agent 绑定 MCP 工具 + 前端配置页面对接真实 API
P3: 扩展 ea-mcp 模块能力 + 对外暴露 Easy-Agent 的 MCP Server
```

---

## 五、数据库表结构

### 1. MCP 服务配置表 (`ea_mcp_config`)
存储 MCP Server 配置和工具元数据。

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint | 主键ID |
| `server_name` | varchar(100) | 服务器名称 |
| `server_url` | varchar(500) | 服务器URL（SSE模式使用） |
| `transport_type` | varchar(20) | 传输类型：SSE/STDIO |
| `command` | text | 启动命令（STDIO模式使用） |
| `env_vars` | json | 环境变量（JSON数组） |
| `tool_name` | varchar(100) | 工具名称（MCP Server中的原始名称） |
| `tool_display_name` | varchar(100) | 工具显示名称 |
| `tool_description` | text | 工具描述 |
| `input_schema` | json | 输入参数Schema（JSON格式） |
| `output_schema` | json | 输出参数Schema（JSON格式） |
| `tool_metadata` | json | 工具元数据（JSON格式） |
| `connection_timeout` | int | 连接超时时间（秒） |
| `max_retries` | int | 最大重试次数 |
| `status` | varchar(20) | 状态：active/inactive/error |
| `last_connected_at` | datetime | 最后连接时间 |
| `last_error` | text | 最后错误信息 |
| `description` | text | 描述信息 |
| `created_at` | datetime | 创建时间 |
| `updated_at` | datetime | 更新时间 |

**唯一索引**：`uk_server_tool` (`server_name`, `tool_name`)

### 2. MCP 关系表 (`ea_mcp_relation`)
存储 Agent 与 MCP 工具的绑定关系。

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint | 主键ID |
| `agent_id` | bigint | Agent ID |
| `mcp_config_id` | bigint | MCP配置ID |
| `binding_config` | json | 绑定配置（JSON格式，可覆盖默认参数） |
| `sort_order` | int | 排序顺序 |
| `is_active` | tinyint | 是否启用：1=启用，0=禁用 |
| `created_at` | datetime | 创建时间 |
| `updated_at` | datetime | 更新时间 |

**唯一索引**：`uk_agent_mcp` (`agent_id`, `mcp_config_id`)

**外键约束**：
- `fk_mcp_relation_agent` → `ea_agent_config` (`id`)
- `fk_mcp_relation_config` → `ea_mcp_config` (`id`)

## 六、附录：核心文件路径

| 文件 | 路径 |
|------|------|
| `ToolTypeEnum.java` | `ea-service/src/main/java/com/aaa/easyagent/core/domain/enums/ToolTypeEnum.java` |
| `ToolDefinition.java` | `ea-service/src/main/java/com/aaa/easyagent/biz/agent/data/ToolDefinition.java` |
| `FunctionToolManager.java` | `ea-service/src/main/java/com/aaa/easyagent/biz/function/FunctionToolManager.java` |
| `ToolExecutor.java` | `ea-service/src/main/java/com/aaa/easyagent/biz/tool/ToolExecutor.java` |
| `HttpExecutor.java` | `ea-service/src/main/java/com/aaa/easyagent/biz/tool/instance/HttpExecutor.java` |
| `EaToolConfigDO.java` | `ea-service/src/main/java/com/aaa/easyagent/core/domain/DO/EaToolConfigDO.java` |
| `ReActAgentXmlExecutor.java` | `ea-service/src/main/java/com/aaa/easyagent/biz/agent/ReActAgentXmlExecutor.java` |
| `MCPConfig.tsx` | `ea-web/src/views/page/agent/tool/MCPConfig.tsx` |
| `MCPSkillList.tsx` | `ea-web/src/views/page/agent/mcp/MCPSkillList.tsx` |
| `ToolComponent.tsx` | `ea-web/src/views/page/chat/ToolComponent.tsx` |