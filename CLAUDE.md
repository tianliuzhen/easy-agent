# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 提供该代码仓库的工作指南。

## 项目概述

Easy Agent 是一个多模块 AI 智能体平台，采用 React 前端和 Spring Boot 后端架构。

- **ea-web**: React 19 + TypeScript + Vite 前端 (端口 5170)
- **ea-service**: Spring Boot 4.1.0-M4 + Spring AI 2.0.0-M4 后端 (端口 8080)
- **ea-mcp**: MCP (模型上下文协议) 模块

## 常用命令

### 前端 (ea-web/)

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 生产环境构建
npm run build

# 运行 ESLint
npm run lint

# 预览生产构建
npm run preview
```

### 后端 (ea-service/)

```bash
# 启动 Spring Boot 应用
mvn spring-boot:run

# 清理并编译
mvn clean compile

# 运行测试
mvn test

# 从数据库生成 MyBatis 映射器
mvn mybatis-generator:generate
```

### 数据库设置

MySQL 运行在端口 3301。初始设置：

```bash
mysql -h localhost -P 3301 -u root -p123456
use easy-agent;
source ea-service/config/initsql/model_platform_table.sql;
```

## 架构

### 前端架构

- **框架**: React 19 + TypeScript 5.8 + Vite 6.3
- **UI 库**: Ant Design 6.3 + Ant Design X 2.4
- **路由**: React Router DOM 7
- **HTTP 客户端**: Axios，自定义拦截器位于 `src/utils/apiInterceptor.ts`
- **状态管理**: React Context (未使用 Redux)

**关键目录**:
- `src/views/page/`: 页面组件 (AgentConfig, ChatDemo, Login 等)
- `src/views/page/agent/`: 智能体配置子页面 (工具配置)
- `src/views/api/`: API 层 (AuthApi, ChatApi, EaAgentApi)
- `src/views/types/`: TypeScript 类型定义

**路由结构**:
- `/` - IndexLayout (主管理界面)
- `/home/chatdemo` - 带导航的聊天界面
- `/agentChat/chatDemo/*` - 不带导航的聊天界面
- `/pageTool/AgentConfig/*` - 智能体配置及嵌套工具路由
- `/login`, `/register` - 认证页面

### 后端架构

**包结构**:
- `com.aaa.easyagent.biz.agent`: ReAct 智能体实现
- `com.aaa.easyagent.biz.tool`: 工具执行器 (SQL, HTTP, GRPC, MCP)
- `com.aaa.easyagent.biz.function`: 函数调用和工具管理
- `com.aaa.easyagent.core`: 领域模型、DTO、枚举、映射器
- `com.aaa.easyagent.web`: 控制器
- `com.aaa.easyagent.common`: 工具类、配置、文档处理

**智能体实现**:
ReAct (推理 + 行动) 模式实现于：
- `ReActAgentExecutor`: 主执行器，使用基于 XML 的提示/响应格式
- `BaseReActAgent`: 基础类，包含通用功能
- `AgentOutputParser`: 解析 LLM 响应 (思考/行动/观察/最终答案)

**工具系统**:
工具按智能体配置，通过以下方式执行：
- `HttpExecutor`: HTTP 请求工具
- `McpExecutor`: MCP (模型上下文协议) 工具
- SQL 和 GRPC 工具执行器

**LLM 集成**:
Spring AI 支持多种提供商 (配置于 `application.yml`)：
- Ollama (本地模型)
- OpenAI
- DeepSeek (通过 SiliconFlow)

### 数据库层

- **ORM**: MyBatis 4.0.1 + tk.mybatis 5.0.1
- **生成器**: MyBatis Generator 用于实体/映射器生成
- **映射器位置**: `com.aaa.easyagent.core.mapper`

## 关键配置文件

- `ea-web/vite.config.ts`: Vite 配置 (端口 5170)
- `ea-web/eslint.config.js`: ESLint 扁平配置
- `ea-service/src/main/resources/application.yml`: 多环境 Spring 配置
- `ea-service/mbg/generatorConfig.xml`: MyBatis 生成器配置

## 测试

**后端**: JUnit 5.12 + Spring Boot Test
- 测试文件位于 `ea-service/src/test/java/`

**前端**: 当前未配置测试框架

## API 约定

后端控制器使用 `/eaAgent/*` 基础路径：
- `POST /eaAgent/modelPlatform/list` - 列出模型平台
- `POST /eaAgent/ai/queryChatModelTypeList` - 查询可用模型
- 聊天端点通过 SSE 实现流式响应

## 重要说明

- 前端在展示给用户之前会过滤 ReAct 响应中的 XML 标签
- 认证使用存储在 localStorage 中的 JWT 令牌
- 使用 SSE (Server-Sent Events) 实现流式聊天响应
- 使用 Log4j2 替代 Spring Boot 默认日志