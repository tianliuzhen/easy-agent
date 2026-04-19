# Easy Agent - 智能体开发平台

<div align="center">
  <p><strong>基于 Spring AI 的企业级 AI 智能体开发平台</strong></p>
</div>
<img width="1869" height="782" alt="image" src="https://github.com/user-attachments/assets/5a373e50-b282-4c75-96ec-eb230c9b7f34" />
<img width="1825" height="792" alt="image" src="https://github.com/user-attachments/assets/4f2d749d-a7a2-4830-a040-ff657e7ad20e" />
<img width="1869" height="850" alt="image" src="https://github.com/user-attachments/assets/39afc40c-4519-46db-bccf-2c6c29285e58" />
<img width="1884" height="812" alt="image" src="https://github.com/user-attachments/assets/3f388941-d311-47c1-a777-983d103ad01b" />


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

### 环境要求

在开始之前，请确保您的系统已安装以下软件：

- **JDK**: 21 或更高版本（推荐 JDK 21 LTS）
- **Maven**: 3.8+ 
- **Node.js**: 18.x 或更高版本
- **npm**: 9.x 或更高版本
- **MySQL**: 8.0+
- **Elasticsearch**: 8.x（用于向量检索，可选）
- **Redis**: 7.x+（用于缓存和会话管理，可选）

### 第一步：克隆项目

```bash
git clone https://github.com/your-username/easy-agent.git
cd easy-agent
```

### 第二步：数据库初始化

#### 1. 创建 MySQL 数据库

```sql
CREATE DATABASE easy_agent DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

#### 2. 导入初始化脚本

执行 `ea-service/config/initsql/` 目录下的 SQL 脚本：

```bash
# 进入 SQL 脚本目录
cd ea-service/config/initsql/

# 使用 MySQL 命令行导入
mysql -u root -p easy_agent < easy-agent.sql

# 如果需要认证模块，额外导入
mysql -u root -p easy_agent < auth.sql
```

**SQL 脚本说明：**
- `easy-agent.sql`: 核心业务表结构（智能体、工具、知识库等）
- `auth.sql`: 用户认证相关表（用户、角色、权限）
- `auth-refactored.sql`: 重构后的认证表结构（推荐使用）
- `skill-tables.sql`: 技能相关表结构

### 第三步：后端服务配置与启动

#### 1. 配置文件修改

编辑 `ea-service/src/main/resources/application.yml`：

```yaml
server:
  port: 8080

spring:
  # 数据源配置
  datasource:
    url: jdbc:mysql://localhost:3306/easy_agent?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  # Redis 配置（可选）
  data:
    redis:
      host: localhost
      port: 6379
      password: 
      database: 0
  
  # Elasticsearch 配置（可选，用于向量检索）
  elasticsearch:
    uris: http://localhost:9200

# LLM 提供商配置
llm:
  # Ollama 配置（本地模型）
  ollama:
    base-url: http://localhost:11434
    model: qwen2.5:7b
  
  # OpenAI 配置
  openai:
    api-key: your_openai_api_key
    base-url: https://api.openai.com/v1
    model: gpt-3.5-turbo
  
  # DeepSeek 配置（通过 SiliconFlow）
  deepseek:
    api-key: your_siliconflow_api_key
    base-url: https://api.siliconflow.cn/v1
    model: deepseek-ai/DeepSeek-V3

# JWT 配置
jwt:
  secret: your_jwt_secret_key_at_least_32_characters
  expiration: 86400000  # 24小时（毫秒）
```

#### 2. 启动后端服务

```bash
# 进入后端服务目录
cd ea-service

# 使用 Maven 编译并运行
mvn clean package -DskipTests
java -jar target/easy-agent-0.0.1-SNAPSHOT.jar

# 或者直接使用 Maven 运行（开发模式）
mvn spring-boot:run
```

**验证后端启动成功：**
- 访问 `http://localhost:8080/api/health` 应返回健康检查信息
- 查看控制台日志，确认无错误信息

### 第四步：前端应用配置与启动

#### 1. 安装依赖

```bash
# 进入前端目录
cd ea-web

# 安装 npm 依赖
npm install
```

#### 2. 环境变量配置（可选）

创建 `.env.local` 文件：

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_WS_URL=ws://localhost:8080
```

#### 3. 启动前端开发服务器

```bash
# 开发模式启动（支持热更新）
npm run dev

# 或者构建生产版本
npm run build
```

**验证前端启动成功：**
- 浏览器访问 `http://localhost:5173`（Vite 默认端口）
- 应该能看到 Easy Agent 的登录页面或主界面

### 第五步：首次使用配置

#### 1. 注册/登录管理员账号

首次使用时，需要创建管理员账号：

```sql
-- 插入默认管理员账号（密码为 admin123，需根据实际加密方式调整）
INSERT INTO sys_user (username, password, nickname, email, status, create_time)
VALUES ('admin', '$2a$10$...', '系统管理员', 'admin@example.com', 1, NOW());
```

#### 2. 配置 LLM 提供商

登录系统后，进入「系统设置」→「模型配置」：

**选项 A：使用 Ollama（推荐本地测试）**
1. 安装 Ollama：https://ollama.ai
2. 拉取模型：`ollama pull qwen2.5:7b`
3. 在系统中配置 Ollama 地址：`http://localhost:11434`

**选项 B：使用 OpenAI**
1. 获取 API Key：https://platform.openai.com/api-keys
2. 在系统中填入 API Key 和模型名称

**选项 C：使用 DeepSeek（SiliconFlow）**
1. 注册 SiliconFlow：https://siliconflow.cn
2. 获取 API Key
3. 配置 API Key 和模型名称

#### 3. 创建第一个智能体

1. 进入「智能体管理」→「新建智能体」
2. 填写基本信息：
   - 名称：客服助手
   - 描述：一个友好的客户服务助手
   - 选择模型：选择已配置的 LLM
3. 配置提示词模板：
   ```
   你是一个专业的客户服务助手，请友好、耐心地回答用户问题。
   如果不确定答案，请诚实地告知用户。
   ```
4. 保存并测试

#### 4. 添加工具（可选）

为智能体添加工具能力：

**HTTP 工具示例：**
- 工具名称：天气查询
- 工具类型：HTTP
- 请求 URL：`https://api.weather.com/query?city={{city}}`
- 请求方法：GET

**SQL 工具示例：**
- 工具名称：订单查询
- 工具类型：SQL
- SQL 模板：`SELECT * FROM orders WHERE user_id = {{user_id}} LIMIT 10`

### 第六步：测试对话功能

1. 进入「智能体对话」页面
2. 选择刚才创建的「客服助手」智能体
3. 在对话框输入：`你好，请问你们的服务时间是什么时候？`
4. 观察智能体的思考和响应过程
5. 查看 SSE 流式输出效果

---

## 📝 常见问题

### Q1: 后端启动时提示数据库连接失败？

**解决方案：**
1. 确认 MySQL 服务已启动
2. 检查 `application.yml` 中的数据库地址、用户名、密码是否正确
3. 确认数据库 `easy_agent` 已创建并导入了 SQL 脚本
4. 检查防火墙是否阻止了 3306 端口

### Q2: 前端无法连接到后端 API？

**解决方案：**
1. 确认后端服务已在 `http://localhost:8080` 启动
2. 检查浏览器控制台是否有 CORS 错误
3. 确认 `ea-web` 中的 API 基础URL配置正确
4. 后端已配置 CORS，允许跨域访问

### Q3: Ollama 模型调用超时？

**解决方案：**
1. 确认 Ollama 服务已启动：`ollama serve`
2. 检查模型是否已下载：`ollama list`
3. 增加超时时间配置
4. 考虑使用更小的模型（如 `qwen2.5:1.5b`）

### Q4: 向量检索功能不工作？

**解决方案：**
1. 确认 Elasticsearch 服务已启动
2. 检查 `application.yml` 中的 ES 地址配置
3. 确认已创建向量索引
4. 查看后端日志中的 ES 连接信息

### Q5: JWT Token 验证失败？

**解决方案：**
1. 清除浏览器 LocalStorage 中的旧 token
2. 重新登录获取新 token
3. 检查 `jwt.secret` 配置是否与生成 token 时一致
4. 确认 token 未过期

---

## 🔧 开发指南

### 项目结构说明

```
easy-agent/
├── ea-service/          # 后端服务
│   ├── src/main/java/com/aaa/easyagent/
│   │   ├── biz/         # 业务逻辑层
│   │   │   ├── agent/   # 智能体相关业务
│   │   │   ├── tool/    # 工具管理业务
│   │   │   └── rag/     # RAG 相关业务
│   │   ├── common/      # 通用组件
│   │   │   ├── config/  # 配置类
│   │   │   ├── util/    # 工具类
│   │   │   └── exception/ # 异常处理
│   │   ├── core/        # 核心引擎
│   │   │   ├── agent/   # 智能体执行器
│   │   │   ├── tool/    # 工具执行器
│   │   │   └── parser/  # 输出解析器
│   │   └── web/         # Web 层
│   │       ├── controller/ # REST API 控制器
│   │       ├── dto/     # 数据传输对象
│   │       └── interceptor/ # 拦截器
│   └── src/main/resources/
│       ├── application.yml  # 主配置文件
│       └── config/      # 其他配置文件
│
├── ea-web/              # 前端应用
│   ├── src/
│   │   ├── views/       # 页面组件
│   │   │   ├── page/    # 主要页面
│   │   │   └── api/     # API 管理页面
│   │   ├── components/  # 公共组件
│   │   ├── utils/       # 工具函数
│   │   └── assets/      # 静态资源
│   └── package.json     # 依赖配置
│
└── ea-mcp/              # MCP 服务器（可选）
    └── src/main/java/com/aaa/mcp/
```

### 添加新的工具类型

1. 实现 `ToolExecutor` 接口：

```java
@Component
public class CustomToolExecutor implements ToolExecutor {
    @Override
    public String execute(ToolConfig config, Map<String, Object> params) {
        // 实现自定义工具逻辑
        return result;
    }
    
    @Override
    public String getToolType() {
        return "CUSTOM";
    }
}
```

2. 在 `ToolExecutorFactory` 中注册

3. 前端添加对应的配置界面

### 自定义智能体执行器

继承 `BaseReActAgent` 类并重写相关方法：

```java
@Component
public class CustomAgentExecutor extends BaseReActAgent {
    @Override
    protected String buildSystemPrompt(AgentConfig config) {
        // 自定义系统提示词
        return super.buildSystemPrompt(config);
    }
    
    @Override
    protected AgentOutput parseOutput(String llmResponse) {
        // 自定义输出解析逻辑
        return super.parseOutput(llmResponse);
    }
}
```

---

## 📊 性能优化建议

### 后端优化

1. **启用 Redis 缓存**
   - 缓存智能体配置
   - 缓存常用工具的响应结果
   - 存储会话状态

2. **数据库优化**
   - 为常用查询字段添加索引
   - 使用连接池（HikariCP 已默认启用）
   - 定期清理历史对话记录

3. **异步处理**
   - 文档导入使用异步任务
   - 向量索引构建后台执行
   - 日志异步写入

### 前端优化

1. **代码分割**
   - 路由级别的懒加载
   - 大型组件动态导入

2. **状态管理**
   - 合理使用 React Context
   - 避免不必要的重渲染

3. **网络优化**
   - 启用 HTTP/2
   - 使用 Gzip 压缩
   - 合理设置缓存策略

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

### 开发流程

1. Fork 本仓库
2. 创建特性分支：`git checkout -b feature/AmazingFeature`
3. 提交更改：`git commit -m 'Add some AmazingFeature'`
4. 推送到分支：`git push origin feature/AmazingFeature`
5. 提交 Pull Request

### 代码规范

- 后端：遵循阿里巴巴 Java 开发手册
- 前端：遵循 ESLint + Prettier 配置
- 提交信息：使用语义化提交（feat/fix/docs/style/refactor/test/chore）

---

## 📄 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

---

## 📞 联系方式

- **GitHub Issues**: [提交问题](https://github.com/your-username/easy-agent/issues)
- **Email**: your-email@example.com
- **文档**: [在线文档](https://your-docs-site.com)

---

<div align="center">
  <p>如果这个项目对你有帮助，请给一个 ⭐️ Star 支持！</p>
</div>
