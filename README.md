# Easy Agent - 智能体开发平台

<div align="center">
  <p><strong>基于 Spring AI 的企业级 AI 智能体开发平台</strong></p>
</div>

<img width="1875" height="785" alt="image" src="https://github.com/user-attachments/assets/fb664f78-faa7-4f16-b411-43dfd5966f06" />

<img width="1876" height="828" alt="image" src="https://github.com/user-attachments/assets/fdf6a3f0-e0d4-44a0-97e0-794e9320747c" />

---

## 📖 项目背景

Easy Agent 是一个企业级的 AI 智能体开发与管理平台，旨在降低 AI 应用开发门槛，提供可视化的智能体配置、工具编排和对话交互能力。

### 核心目标

- **可视化配置**: 通过 Web 界面轻松创建和管理 AI 智能体，无需编写代码
- **工具生态**: 支持 HTTP、SQL、gRPC、MCP 等多种工具类型，扩展智能体能力边界
- **多模型支持**: 集成 Ollama、OpenAI、DeepSeek 等主流大语言模型提供商
- **RAG 增强**: 内置向量数据库支持，实现知识库检索增强生成
- **ReAct 模式**: 采用推理+行动的智能体执行模式，提升复杂任务处理能力

### 应用场景

- 企业内部知识问答系统
- 自动化业务流程助手
- 数据查询与分析助手
- 多工具协同的复杂任务执行

---

## 🏗️ 技术架构

### 整体架构图

```
┌─────────────────────────────────────────────────────────────┐
│                        前端层 (ea-web)                       │
│  React 19 + TypeScript + Ant Design + Vite                 │
│  - 智能体配置管理                                            │
│  - 对话交互界面                                              │
│  - 工具可视化编排                                            │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTP/SSE
┌────────────────────────▼────────────────────────────────────┐
│                     后端服务层 (ea-service)                  │
│  Spring Boot 4.1 + Spring AI 2.0 + MyBatis                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │  Controller  │  │   Biz Layer  │  │   Core Layer     │  │
│  │   REST API   │  │  Agent执行器  │  │  领域模型/映射器  │  │
│  └──────────────┘  └──────┬───────┘  └──────────────────┘  │
│                           │                                  │
│  ┌────────────────────────▼──────────────────────────────┐  │
│  │              智能体引擎 (ReAct Pattern)                │  │
│  │  - ReActAgentExecutor (推理+行动循环)                  │  │
│  │  - ToolExecutor (HTTP/SQL/gRPC/MCP)                   │  │
│  │  - FunctionToolManager (工具管理)                      │  │
│  └────────────────────────┬──────────────────────────────┘  │
└───────────────────────────┼──────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        ▼                   ▼                   ▼
┌───────────────┐  ┌───────────────┐  ┌────────────────┐
│    MySQL      │  │ Elasticsearch │  │   Redis        │
│  业务数据存储  │  │  向量数据库    │  │  缓存/会话存储  │
└───────────────┘  └───────────────┘  └────────────────┘
        
┌──────────────────────────────────────────────────────────┐
│                    LLM 模型提供商                         │
│  Ollama (本地) | OpenAI | DeepSeek | SiliconFlow        │
└──────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────┐
│                  MCP Server (ea-mcp)                      │
│  Model Context Protocol 协议实现                          │
└──────────────────────────────────────────────────────────┘
```

### 技术栈详情

#### 前端技术栈 (ea-web)

| 技术 | 版本 | 用途 |
|------|------|------|
| React | 19.x | UI 框架 |
| TypeScript | 5.8.x | 类型安全 |
| Vite | 6.3.x | 构建工具 |
| Ant Design | 6.3.x | UI 组件库 |
| Ant Design X | 2.4.x | AI 专用组件 |
| React Router | 7.x | 路由管理 |
| Axios | 1.9.x | HTTP 客户端 |
| React Markdown | 10.x | Markdown 渲染 |

#### 后端技术栈 (ea-service)

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 4.1.0-M4 | 应用框架 |
| Spring AI | 2.0.0-M4 | AI 集成框架 |
| MyBatis | 4.0.1 | ORM 框架 |
| tk.mybatis | 5.0.1 | MyBatis 增强 |
| MySQL | - | 关系型数据库 |
| Elasticsearch | - | 向量数据库 |
| Redis | - | 缓存与会话存储 |
| Log4j2 | - | 日志框架 |
| JWT | 0.12.6 | 身份认证 |
| Jinjava | 2.8.3 | 模板引擎 |
| FreeMarker | - | 模板引擎 |

#### 支持的 LLM 提供商

- **Ollama**: 本地部署的大语言模型
- **OpenAI**: GPT 系列模型
- **DeepSeek**: 通过 SiliconFlow API 访问
- 可扩展支持其他 Spring AI 兼容的模型提供商

### 核心模块说明

#### 1. 智能体引擎 (Agent Engine)

采用 **ReAct (Reasoning + Acting)** 模式实现智能体执行逻辑：

- **ReActAgentExecutor**: 主执行器，使用 XML 格式的提示词和响应解析
- **BaseReActAgent**: 基础智能体类，提供通用功能
- **AgentOutputParser**: 解析 LLM 输出（思考/行动/观察/最终答案）

执行流程：
```
用户输入 → 智能体推理 → 选择工具 → 执行工具 → 观察结果 → 再次推理 → ... → 输出最终答案
```

#### 2. 工具系统 (Tool System)

支持多种工具类型，通过 `ToolExecutor` 统一调度：

- **HttpExecutor**: 执行 HTTP 请求，调用外部 API
- **SqlExecutor**: 执行 SQL 查询，访问数据库
- **GrpcExecutor**: 调用 gRPC 服务
- **McpExecutor**: 连接 MCP 服务器，扩展工具能力

工具通过 `FunctionToolManager` 进行注册和管理，可按智能体灵活配置。

#### 3. RAG 增强 (Retrieval-Augmented Generation)

- **向量存储**: 基于 Elasticsearch 实现向量相似度检索
- **文档处理**: 支持 PDF、文本等格式的知识库导入
- **OCR 识别**: 集成 Tess4J 实现图片文字提取
- **语义检索**: 结合向量检索和关键词匹配提升召回率

#### 4. MCP 集成 (Model Context Protocol)

- **ea-mcp 模块**: 独立的 MCP Server 实现
- **协议支持**: 遵循 Anthropic MCP 标准协议
- **工具扩展**: 通过 MCP 动态发现和调用外部工具

#### 5. 安全认证

- **JWT Token**: 基于 JSON Web Token 的无状态认证
- **Spring Security**: 集成 Spring Security 实现权限控制
- **RBAC**: 基于角色的访问控制（可扩展）

### 数据流架构

```
┌──────────┐     ┌──────────┐     ┌──────────────┐     ┌──────────┐
│  用户输入  │────▶│  Frontend │────▶│  Controller  │────▶│  Agent   │
└──────────┘     └──────────┘     └──────────────┘     └────┬─────┘
                                                            │
                                                            ▼
┌──────────┐     ┌──────────┐     ┌──────────────┐     ┌──────────┐
│ SSE 流式  │◀────│ Response │◀────│ LLM Provider │◀────│  Tool    │
│  响应输出  │     │  Parser  │     │   (API Call) │     │ Executor │
└──────────┘     └──────────┘     └──────────────┘     └────┬─────┘
                                                            │
                                                            ▼
                                                     ┌──────────────┐
                                                     │ External APIs│
                                                     │  Databases   │
                                                     │   MCP Tools  │
                                                     └──────────────┘
```

---

## 🚀 快速开始
