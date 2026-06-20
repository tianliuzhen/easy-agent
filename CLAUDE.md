# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 提供该代码仓库的工作指南。

## 项目概述

Easy Agent 是一个多模块 AI 智能体平台，React 19 前端 + Spring Boot 后端。

- **ea-web**: React 19 + TypeScript + Vite 前端 (端口 5170)，独立 npm 构建
- **ea-service**: Spring Boot 4.1.0 + Spring AI 2.0.0-RC2 后端 (端口 8080)
- **ea-mcp**: 独立 MCP Server 应用 (Spring AI 2.0.0-M4)，提供天气查询等工具

根 pom.xml 管理 `ea-service` 和 `ea-mcp` 两个 Maven 模块，ea-web 不在 Maven 管理范围内。

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

MySQL 运行在端口 3301：

```bash
mysql -h localhost -P 3301 -u root -p123456
use easy-agent;
source ea-service/config/initsql/model_platform_table.sql;
```

## 架构

### 智能体执行引擎（核心）

后端有**两套**智能体执行路径：

1. **ReActAgentExecutor** (`biz/agent/ReActAgentExecutor`): 基于 XML 格式的传统 ReAct 实现，使用 `<Thought>`、`<Action>`、`<ActionInput>`、`<Observation>`、`<FinalAnswer>` 标签进行推理循环
2. **ToolAgentExecutor** (`biz/agent/ToolAgentExecutor`): 基于 Spring AI `ChatClient` + Advisor 模式的新实现，使用 `ToolExecutionAdvisor` 拦截 tool_call 自动执行工具，`SlidingWindowAdvisor` 控制上下文窗口

两者都继承 `BaseAgent`，由 `doExec()` 控制外部执行循环。工具通过 `FunctionCallbackAdapter` 动态加载，工具类型包括 HTTP、SQL、MCP、GRPC、Skill。

### 后端分层

```
web/        - REST 控制器 (业务端点 + 示例端点)
biz/        - 业务逻辑 (agent/、tool/、function/)
core/       - 领域模型 (DO、DTO、枚举、Mapper、Service 接口)
common/     - 工具类、配置、文档处理、LLM 适配
```

控制器位于两个命名空间：
- `web/biz/` — 业务端点 (agent、chat、function、knowledge、mcp、skill)
- `web/example/` — 示例和调试端点 (llm、sse)

### SSE 流式协议

聊天使用 `POST /eaAgent/ai/chat` 的 SSE 端点。后端发送 JSON 事件，每条的 `type` 字段为以下之一：

| type | 含义 |
|------|------|
| `think` | 思考过程 |
| `data` | 流式回答片段 |
| `tool` | 工具调用及结果 |
| `log` | 日志信息 |
| `finalAnswer` | 最终完整答案 |
| `error` | 错误信息 |

前端通过 `@microsoft/fetch-event-source` 连接 SSE，在 `ChatApi.ts` 的 `sendMessage()` 中解析事件类型并分发到对应回调。`openWhenHidden: true` 参数是必需的，否则浏览器会限制后台页面的 SSE 连接。

### 前端架构

**路由拓扑**（在 `App.tsx` 中按 pathname 前缀分流）：
- `/login`、`/register` — 认证页，无布局
- `/` — IndexLayout（侧边栏管理界面，包含 Agent 管理、知识库、工具、MCP、Skill、模型平台、用户配置）
- `/home/chatdemo` — 带顶部导航栏的聊天页
- `/agentChat/chatDemo/*` — 纯聊天页（无导航）
- `/pageTool/AgentConfig/*` — Agent 配置页（多 Tab 编辑）

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

**API 层** (`views/api/`)：每个 API 文件导出对象字面量，直接使用 `fetch`（经拦截器增强），以 `API_BASE_URL = http://localhost:8080` 为基地址。

### MCP 模块 (ea-mcp)

独立的 Spring Boot 应用，提供 MCP Server 端点。`McpServerConfig` 配置服务器，`WeatherService` 暴露天气查询工具。ea-service 作为 MCP Client 通过 `spring-ai-starter-mcp-server` 连接外部 MCP 服务器。

## 关键配置

- `ea-service/src/main/resources/application.yml`: 多文档 Spring 配置（ollama、openai、deepseek、dp 四个 profile）
- `ea-service/mbg/generatorConfig.xml`: MyBatis 代码生成器配置
- `ea-web/vite.config.ts`: Vite 配置（端口 5170）
- `ea-web/eslint.config.js`: ESLint 扁平配置

## 关键依赖

**后端**：Spring Boot 4.1.0、Spring AI 2.0.0-RC2、MyBatis 4.0.1 + tk.mybatis 5.0.1、MySQL、Elasticsearch、Redis、JWT (jjwt 0.12.6)、Tess4J OCR、Jinjava + FreeMarker 模板引擎、Spring Security

**前端**：React 19、TypeScript 5.8、Vite 6.3、Ant Design 6.3、Ant Design X 2.4、React Router 7、Axios、`@microsoft/fetch-event-source`、react-markdown + remark-gfm + rehype-raw

## 重要说明

- 前端在展示前过滤 ReAct XML 标签，聊天区使用 `XMarkdown` + `Think`/`Tool`/`Log` 组件渲染不同类型消息
- Log4j2 替代 Spring Boot 默认日志，需排除 `spring-boot-starter-logging`
- Spring AI 版本使用 RC/Milestone 仓库 (`spring-milestones`、`spring-snapshots`)
- `spring.ai.retry.autoconfigure.SpringAiRetryAutoConfiguration` 和 `RedisVectorStoreAutoConfiguration` 在 application.yml 中排除自动配置
