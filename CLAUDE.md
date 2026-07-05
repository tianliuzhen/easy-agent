# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 提供该代码仓库的工作指南。

## 项目概述

Easy Agent 是一个多模块 AI 智能体平台，React 19 前端 + Spring Boot 后端。

- **ea-web**: React 19 + TypeScript + Vite 前端 (端口 5170)，独立 npm 构建
- **ea-service**: Spring Boot 4.1.0 + Spring AI 2.0.0-RC2 后端 (端口 8080)
- **ea-mcp**: 独立 MCP Server 应用 (Spring Boot 4.1.0-M4 + Spring AI 2.0.0-M4，端口 8083)，提供天气查询等工具

根 pom.xml 管理 `ea-service` 和 `ea-mcp` 两个 Maven 模块，ea-web 不在 Maven 管理范围内。各子模块各自声明 Spring Boot parent 版本（根 pom 的 `spring-boot.version` 属性为遗留值，不生效）。

## 常用命令

### 后端 (ea-service/)

```bash
# 启动应用
./mvnw spring-boot:run

# 清理编译
./mvnw clean compile

# 运行全部测试
./mvnw test

# 运行单个测试类
./mvnw test -Dtest=ReActAgentXmlExecutorTest

# 运行单个测试方法
./mvnw test -Dtest=ReActAgentXmlExecutorTest#testMethodName

# 生成 MyBatis 映射器
./mvnw mybatis-generator:generate
```

### 前端 (ea-web/)

```bash
npm install        # 安装依赖
npm run dev        # 启动开发服务器 (端口 5170)
npm run build      # 生产构建 (tsc + vite)
npm run lint       # ESLint 检查
npm run preview    # 预览生产构建
```

### 多模块构建 (根目录)

```bash
./mvnw clean compile    # 编译所有 Maven 模块
./mvnw test             # 运行所有模块测试
```

### 数据库

MySQL 连接配置在 `ea-service/src/main/resources/config/mysql-config.yml`：端口 3306，库名 `easy-agent`，用户 root / 密码 12345678。

```bash
mysql -h localhost -P 3306 -u root -p12345678
use easy-agent;
source ea-service/config/initsql/model_platform_table.sql;
```

## 架构

### 智能体执行引擎（核心）

后端有**多套**智能体执行路径，均位于 `biz/agent/`：

- **BaseAgent**: 抽象基类，定义 `buildPrompt()` / `run()`
- **BaseReActAgent**: 继承 BaseAgent，追加 `think()` / `act()`
- **ReActAgentExecutor**: 继承 BaseReActAgent，基于 XML 标签的 ReAct 实现（`<Thought>`、`<Action>`、`<ActionInput>`、`<Observation>`、`<FinalAnswer>`），内含 `XmlTagFilter`
- **ReActAgentOldExecutor**: 继承 BaseReActAgent，旧版基于纯文本的实现
- **ToolAgentExecutor**: 继承 BaseAgent，基于 Spring AI `ChatClient` + Advisor 模式，含多模态 Media 处理

工具通过 `context/FunctionCallbackAdapter` 动态加载。

**Advisor** (`biz/agent/advisor/`)：
- `ToolExecutionAdvisor` — 拦截 tool_call 自动执行工具
- `SlidingWindowAdvisor` — 控制上下文窗口
- `SseAdvisor` — 将执行过程通过 SSE 推送

### 多 Agent 编排 (`biz/agent/flow/`)

- `FlowExecutor`（策略接口）+ `FlowExecutorManager`（注入 `List<FlowExecutor>`，按 `FlowStrategyEnum` 分发）
- `impl/RouterFlowExecutor`、`impl/WorkflowFlowExecutor`
- `FlowStrategyEnum`：**SUPERVISOR、ROUTER、WORKFLOW**（映射 `ea_agent_flow.strategy`）
- `FlowContext`、`FlowManagerService(Impl)`、`AgentContextFactory`
- 控制器 `web/biz/agent/FlowController`（`@RequestMapping("eaAgent/flow")`：`list`、`detail/{id}`、`save`）
- 前端对应 `views/api/FlowApi.ts`

### 后端分层

```
web/        - REST 控制器 (业务端点 + 示例端点)
biz/        - 业务逻辑 (agent/、tool/、function/)
core/       - 领域模型 (DO、DTO、枚举、Mapper、Service 接口)
common/     - 工具类、配置、文档处理、LLM 适配、向量库配置
```

控制器命名空间：
- `web/biz/` — 业务端点 (agent、chat、function、knowledge、mcp、skill)
- `web/example/` — 示例和调试端点 (llm、sse、react)
- `web/auth/` — 认证 (`AuthController`)

### 工具类型

`core/domain/enums/ToolTypeEnum` 定义**四种**：**HTTP、SQL、MCP、SKILL**，各带一个 `ParamsTemplate`。执行器在 `biz/tool/instance/`：`HttpExecutor`、`SqlExecutor`、`McpExecutor`、`SkillExecutor`，通过 `@ToolTypeChooser` 注解 + `FunctionToolManager` 分发。

> 注意：前端有 GRPC 配置 Tab（`/pageTool/AgentConfig/tool/grpc`），但**后端没有 GRPC 工具类型或执行器**，GRPC 目前仅是前端配置项。

### 向量库 (`common/config/vectorstore/`)

支持 **Elasticsearch** 与 **Milvus**，运行时由 `easy-agent.vectorstore.provider`（`es` / `milvus`）切换。

- `VectorStoreRegister` — 中央注册表，按 index/collection 用 Guava 缓存动态创建 ES 或 Milvus `VectorStore`
- `MilvusVectorStoreConfig` / `MilvusVectorProperties`、`ElasticsearchVectorStoreAutoConfiguration`（本项目自定义）、`SimpleVectorStoreConfig`、`EmbeddingModelConfig`
- Milvus 连接配置在 `config/milvus-config.properties`（维度 2048、COSINE、IVF_FLAT）
- 当前 provider 为 **milvus**
- Redis 有依赖与 `RedisDocumentService`，但 `RedisVectorStoreAutoConfiguration` 已排除，非活跃向量库

### SSE 流式协议

聊天使用 `POST /eaAgent/ai/chat` 的 SSE 端点（另有 `GET /eaAgent/ai/streamChatWith`），见 `web/biz/agent/EaAgentController`。POST body `StreamChatPostRequest` 含 `sessionId`、`msg`、`agentId`、`streamEnabled`（流式/同步开关）、`imageBase64`（多模态图片）。

事件由 `biz/agent/context/SseHelper` 发出，`type` 字段为以下之一：

| type | 含义 |
|------|------|
| `think` | 思考过程 |
| `data` | 流式回答片段 |
| `tool` | 工具调用及结果 |
| `log` | 日志信息 |
| `finalAnswer` | 最终完整答案 |
| `error` | 错误信息 |
| `step` | 多 Agent 编排步骤（agentName/index/total）|
| `handoff` | 多 Agent 交接（agentName/reason）|

前端通过 `@microsoft/fetch-event-source` 连接 SSE，在 `ChatApi.ts` 的 `sendMessage()` 中解析事件类型并分发到对应回调。`openWhenHidden: true` 参数是必需的，否则浏览器会限制后台页面的 SSE 连接。

### 前端架构

**路由拓扑**（在 `App.tsx` 中按 pathname 前缀分流）：
- `/login`、`/register` — 认证页，无布局
- `/` — IndexLayout（侧边栏管理界面，包含 Agent 管理、知识库、工具、MCP、Skill、模型平台、用户配置）
- `/home/chatdemo` — 带顶部导航栏的聊天页
- `/agentChat/chatDemo/*` — 纯聊天页（无导航）
- `/pageTool/AgentConfig/*` — Agent 配置页（多 Tab 编辑，工具子路由 `tool/sql`、`tool/http`、`tool/mcp`、`tool/grpc`）

**认证机制**：JWT 存储在 localStorage (`AUTH_TOKEN`)。`apiInterceptor.ts` 全局拦截 `fetch`，自动注入 `Authorization: Bearer` 头并处理 401 响应。`AuthGuard` 组件包裹需要登录的路由。

**Agent 配置页** (`views/page/agent/`) 是最复杂的组件，包含多 Tab 子面板：
- `prompt/` — 提示词配置
- `tool/` — 工具绑定 (HTTP、SQL、MCP、GRPC、Skill)
- `knowledge/` — 知识库绑定
- `skill/`、`mcp/` — Skill 和 MCP 绑定
- `memory/` — 记忆配置
- `personalization/` — 个性化设置
- `settings/` — 通用设置
- `feedback/` — 反馈配置
- `debug/` — 在线调试
- `ResourceBindingPanel.tsx`、`AgentConfigContext.tsx`、`AgentEditModal.tsx`

**API 层** (`views/api/`)：每个 API 文件导出对象字面量，直接使用 `fetch`（经拦截器增强），以 `API_BASE_URL = http://localhost:8080` 为基地址。包含 AuthApi、ChatApi、ChatConversationApi、EaAgentApi、EaToolApi、FlowApi、KnowledgeBaseApi、McpApi、ModelPlatformApi、SkillApi。

### 多模态图片支持

- **后端**：`StreamChatPostRequest.imageBase64` 贯穿 `AgentChatServiceImpl` → `AgentContext` → `ToolAgentExecutor` / `BaseAgent`（构造 Media），需 openai profile `image.enabled: true`
- **前端**：`views/page/chat/ChatComponents.tsx` 处理图片上传并写入 `imageBase64`

### MCP 模块 (ea-mcp)

独立 Spring Boot 应用，端口 **8083**，使用 **STREAMABLE** 协议（`spring-ai-starter-mcp-server-streamable-webmvc`，SYNC），端点 `/api/mcp`。`config/McpServerConfig` 通过 `MethodToolCallbackProvider` 注册 `WeatherService`（`@Tool` 天气查询，调用 Open-Meteo REST API）。ea-service 作为 MCP Client 通过 `biz/tool/instance/McpExecutor` 消费。

## 关键配置

- `ea-service/src/main/resources/application.yml`: 多文档 Spring 配置（ollama、openai、deepseek、dp 四个 vendor 块）
- `ea-service/src/main/resources/application.properties`: import 了 milvus/mysql/redis/es 配置及 `optional:file:./.env`，含 JWT 配置
- `ea-service/src/main/resources/config/mysql-config.yml`: 数据库连接
- `ea-service/src/main/resources/config/milvus-config.properties`: Milvus 连接
- `ea-service/mbg/generatorConfig.xml`: MyBatis 代码生成器配置
- `ea-web/vite.config.ts`: Vite 配置（端口 5170）
- `ea-web/eslint.config.js`: ESLint 扁平配置

## 关键依赖

**后端**：Spring Boot 4.1.0、Spring AI 2.0.0-RC2、MyBatis 4.0.1 + tk.mybatis 5.0.1、MySQL、Elasticsearch、Milvus、Redis、JWT (jjwt 0.12.6)、Tess4J OCR、Jinjava + FreeMarker 模板引擎、Spring Security、Guava

**前端**：React 19.1、TypeScript 5.8、Vite 6.3、Ant Design 6.3、Ant Design X 2.4、`@ant-design/x-markdown` 2.8、React Router 7、Axios、`@microsoft/fetch-event-source`、react-markdown + remark-gfm + rehype-raw

## 重要说明

- 前端在展示前过滤 ReAct XML 标签，聊天区使用 `@ant-design/x-markdown` (XMarkdown) + `Think`/`Tool`/`Log` 组件渲染不同类型消息
- Log4j2 替代 Spring Boot 默认日志，需排除 `spring-boot-starter-logging`
- Spring AI 版本使用 RC/Milestone 仓库 (`spring-milestones`、`spring-snapshots`)
- `application.yml` 的 `spring.autoconfigure.exclude` 排除了四个自动配置：`RedisVectorStoreAutoConfiguration`、`SpringAiRetryAutoConfiguration`、`MilvusVectorStoreAutoConfiguration`、官方 `ElasticsearchVectorStoreAutoConfiguration`（后两者改由自定义 `VectorStoreRegister` 按集合动态创建，避免启动时强制连接）
