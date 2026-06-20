# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 提供 ea-web 前端模块的工作指南。

## 构建命令

```bash
npm install        # 安装依赖
npm run dev        # 启动开发服务器 (端口 5170)
npm run build      # 生产构建 (tsc -b && vite build)
npm run lint       # ESLint 检查
npm run preview    # 预览生产构建
```

## 架构

### 路由拓扑 (App.tsx)

按 pathname 前缀分流，不使用 React Router 嵌套路由匹配：

| 路径前缀 | 渲染内容 |
|----------|----------|
| `/login`, `/register` | 独立页面，无布局 |
| `/home/*` | 带顶部导航的聊天布局 |
| `/agentChat/*` | 纯聊天页（无导航） |
| `/pageTool/*` | Agent 多 Tab 配置页 |
| `/` | IndexLayout（侧边栏管理界面） |

### IndexLayout 路由 (views/IndexLayout.tsx)

侧边栏菜单 → 路由映射均在 `routes` 数组中定义，使用 `AuthGuard` 包裹需认证的组件：
- **Agent 应用**: AgentManager
- **知识库管理**: KnowledgeBaseList
- **工具管理**: ToolManager (mode="default" / mode="my")
- **MCP 管理**: MyMCP / MCPMarket
- **Skill 技能**: MySkill / SkillMarket
- **模型平台**: ChatModelConfig
- **用户配置**: User

工具管理的子路由 `/toolManager/tool/:type` 和 `/myToolManager/tool/:type` 用于配置具体工具类型。

### 聊天系统

**SSE 连接** (`api/ChatApi.ts`): 使用 `@microsoft/fetch-event-source` 发起 POST 到 `/eaAgent/ai/chat`，解析 JSON 事件的 `type` 字段：
- `think` → 思考过程
- `data` → 流式回答片段
- `tool` → 工具调用及结果
- `log` → 日志
- `finalAnswer` → 最终完整答案
- `error` → 错误

`openWhenHidden: true` 参数是必需的，否则浏览器会限制后台页面的 SSE 连接。

**聊天组件** (`views/page/chat/`):
- `ChatComponents.tsx` — 主聊天组件，组合消息渲染
- `ThinkComponent.tsx` — 思考过程折叠展示
- `ToolComponent.tsx` — 工具调用信息展示
- `LogComponent.tsx` — 日志条目展示
- `MessageBubble.tsx` — 消息气泡（集成 Markdown 渲染）
- `messageUtils.ts` — 消息内容解析工具
- `types.ts` — 消息类型定义（ChatMessage、ThinkingProcess、ToolCall 等）

**Markdown 渲染**: 使用 `react-markdown` + `remark-gfm` + `rehype-raw`，在消息气泡中渲染富文本内容。

### API 层 (`views/api/`)

每个文件导出对象字面量方法，直接使用全局 `fetch`（经 `apiInterceptor.ts` 拦截增强），基地址为 `http://localhost:8080`：

- `AuthApi.ts` — 登录、注册、用户信息、登出
- `ChatApi.ts` — SSE 聊天流
- `ChatConversationApi.ts` — 会话历史
- `EaAgentApi.ts` — Agent CRUD、快捷提示词
- `EaToolApi.ts` — 工具配置
- `KnowledgeBaseApi.ts` — 知识库管理
- `McpApi.ts` — MCP 配置
- `ModelPlatformApi.ts` — 模型平台配置
- `SkillApi.ts` — Skill 管理

### Agent 配置页 (`views/page/agent/`)

AgentConfig 是多 Tab 编辑界面，子面板包括：
- `prompt/PromptConfig.tsx` + `PromptInputPanel.tsx` — 提示词配置
- `tool/` — 工具绑定 (AgentToolBinding、HTTP/SQL/MCP/GRPC/Skill 配置)
- `knowledge/AgentKnowledgeBinding.tsx` — 知识库绑定
- `skill/SkillList.tsx` + `SkillSelector.tsx` — Skill 绑定
- `mcp/MCPSkillList.tsx` — MCP 绑定
- `memory/MemoryConfig.tsx` — 记忆配置
- `personalization/AgentPersonalization.tsx` — 个性化设置
- `settings/AgentSettings.tsx` — 通用设置
- `feedback/FeedbackConfig.tsx` — 反馈配置
- `common/AddResourceModal.tsx` — 通用资源添加弹窗

### 认证机制

- JWT token 存储在 `localStorage.AUTH_TOKEN`
- `utils/apiInterceptor.ts` 全局拦截 `window.fetch`，自动注入 `Authorization: Bearer` 头
- 401 响应时清除 token，`AuthGuard` 组件检测无 token 时重定向到 `/login`

### 关键类型定义

- `views/page/chat/types.ts` — ChatMessage、ThinkingProcess、ToolCall、LogEntry、typeConfig
- `views/types/ToolConfig.ts` — 工具配置类型

## 关键依赖

React 19、TypeScript 5.8、Vite 6.3、Ant Design 6.3、Ant Design X 2.4、React Router 7、`@microsoft/fetch-event-source`、react-markdown + remark-gfm + rehype-raw、jsoneditor
