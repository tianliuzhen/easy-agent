# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 提供该代码仓库的工作指南。

## 项目概述

Easy Agent (ea-service) 是一个基于 Spring Boot 的 AI 智能体服务，支持多种 LLM 提供商（Ollama、OpenAI、DeepSeek、SiliconFlow），实现了用于工具使用和知识库管理的 ReAct 模式。

## 构建命令

### Maven 命令（使用 wrapper）

```bash
# 构建项目
./mvnw clean compile

# 运行测试
./mvnw test

# 运行单个测试类
./mvnw test -Dtest=ReActAgentXmlExecutorTest

# 运行单个测试方法
./mvnw test -Dtest=ReActAgentXmlExecutorTest#testMethodName

# 打包应用
./mvnw clean package

# 运行应用
./mvnw spring-boot:run

# MyBatis 代码生成（从数据库模式生成 DO 类和映射器）
./mvnw mybatis-generator:generate
```

### Windows（使用 mvnw.cmd）

```cmd
mvnw.cmd clean compile
mvnw.cmd test
mvnw.cmd spring-boot:run
```

## 架构概述

### 分层结构

```
web/        - 控制器（REST API 端点）
core/       - 领域模型（DO）、映射器和服务接口
biz/        - 业务逻辑实现（智能体、工具、函数）
common/     - 工具类、配置和共享组件
```

### 关键架构组件

**ReAct 智能体系统** (`biz/agent/`)
- `BaseAgent` - 所有智能体的基类
- `BaseReActAgent` - ReAct 模式智能体的抽象基类，包含 `think()` 和 `act()` 方法
- `ReActAgentExecutor` - 基于 XML 的 ReAct 执行器，使用结构化提示，包含 `<Thought>`、`<Action>`、`<ActionInput>`、`<Observation>`、`<FinalAnswer>` 标签
- `ReActAgentOldExecutor` - 遗留的基于文本的 ReAct 实现

**工具系统** (`biz/tool/`、`biz/function/`)
- 工具由智能体动态加载和执行
- 工具类型：HTTP 工具、SQL 工具、MCP（模型上下文协议）工具
- `FunctionCallback` 接口用于工具执行回调

**知识库** (`common/document/`)
- 支持 Elasticsearch、Redis 和本地文件的文档加载器
- 语雀集成，用于文档同步
- 通过 Tesseract (Tess4J) 实现 OCR 支持，用于图像文本提取
- 向量存储：Elasticsearch 和 Redis

**LLM 集成** (`common/llm/`)
- 多提供商支持：Ollama、OpenAI、DeepSeek、SiliconFlow
- 在 `application.yml` 中配置，每个提供商有独立的配置文件

**认证** (`web/auth/`、`common/config/security/`)
- 基于 JWT 的认证，使用 jjwt 0.12.6
- Spring Security 配置

## 数据库和 MyBatis

### 数据库模式

MySQL 数据库 `easy-agent` 包含以下表：
- `ea_agent` - 智能体定义
- `ea_function` - 工具/函数定义
- `ea_tool_config` - 工具配置
- `ea_knowledge_base` - 知识库条目
- `ea_knowledge_relation` - 知识库关系
- `ea_tool_relation` - 工具关系
- `ea_chat_conversation` / `ea_chat_message` - 聊天历史
- `ea_model_platform` - LLM 平台配置
- `ea_iam_*` - 用户认证和授权表
- `ea_mcp_config` / `ea_mcp_relation` - MCP 服务器配置

### MyBatis 代码生成

生成器配置：`mbg/generatorConfig.xml`

生成的文件位置：
- DO 类：`core/domain/DO/`
- 映射器：`core/mapper/`
- XML 映射器：`resources/mbg/mapper/`

使用 tk.mybatis 映射器插件，支持 Lombok。

## 配置文件

- `application.properties` - Spring Boot 主配置，导入外部配置
- `application.yml` - LLM 提供商配置（Ollama、OpenAI、DeepSeek）
- `config/mysql-config.yml` - 数据库连接
- `config/redis-config.properties` - Redis 配置
- `config/elasticSearch-config.properties` - Elasticsearch 配置
- `log4j2-spring.xml` - 日志配置

## 关键技术

- Spring Boot 4.1.0-M4（里程碑版本）
- Spring AI 2.0.0-M4
- Java 17
- MyBatis 4.0.1 with MyBatis Generator
- MySQL 8.0
- JWT (jjwt 0.12.6)
- Tesseract OCR (Tess4J 5.13.0)
- 模板引擎：Jinjava (Jinja)、FreeMarker

## OCR 设置

请参阅 `OCR-SETUP.md` 获取 Tesseract 安装说明。应用程序可在 Windows、Linux 和 macOS 上自动检测 Tesseract 安装路径。

## 测试

测试文件位于 `src/test/java/com/aaa/easyagent/`：
- `biz/agent/ReActAgentXmlExecutorTest` - ReAct 智能体测试
- `llm/deepseek/` - DeepSeek LLM 集成测试
- `document/YuQueApiTest` - 语雀 API 测试
- `util/` - 工具测试（Jinja、FreeMarker、WebClient）

## MCP（模型上下文协议）

项目包含 MCP 服务器支持，用于连接外部工具服务器：
- `ea_mcp_config` 表存储 MCP 服务器配置
- `ea_mcp_relation` 将 MCP 工具映射到智能体
- 包含 Spring AI MCP starter 依赖