======================================
模型平台配置功能实现说明
======================================

## 功能概述
将原有的从枚举读取模型平台类型改为从数据库读取，实现动态配置管理。

## 实现步骤

### 1. 数据库表创建
执行以下 SQL 脚本创建表并初始化数据：
```sql
-- 文件位置：ea-service/config/initsql/model_platform_table.sql
```

**操作步骤:**
1. 打开 MySQL 客户端
2. 连接到数据库 easy-agent (端口 3301)
3. 执行 `source F:\WorkSpace\MyGithub\easy-agent\ea-service\config\initsql\model_platform_table.sql;`

### 2. 代码生成 (已完成)
DO 和 DAO 已通过 MyBatis Generator 生成:
- EaModelPlatformDO.java
- EaModelPlatformDAO.java

### 3. 后端实现 (已完成)
已创建以下文件:
- ModelPlatformService.java - 服务接口
- ModelPlatformServiceImpl.java - 服务实现
- ModelPlatformController.java - 控制器
- EaModelPlatformReq.java - 请求对象
- EaModelPlatformResult.java - 结果对象

### 4. 前端实现 (已完成)
已创建以下文件:
- ModelPlatformApi.ts - API 接口
- ModelPlatformConfig.tsx - 配置页面

### 5. 接口修改 (已完成)
已修改 EaAgentController.java 中的 queryChatModelTypeList 方法，改为从数据库读取。

## 功能特性

### 后端 CRUD 接口
1. POST /eaAgent/modelPlatform/list - 查询所有模型平台
2. POST /eaAgent/modelPlatform/getById - 根据 ID 查询详情
3. POST /eaAgent/modelPlatform/save - 保存 (新增/更新)
4. POST /eaAgent/modelPlatform/delete - 删除
5. POST /eaAgent/modelPlatform/updateActiveStatus - 更新启用状态
6. POST /eaAgent/ai/queryChatModelTypeList - 查询模型平台列表 (兼容旧接口)

### 前端功能
- 列表展示 (表格形式)
- 新增模型平台
- 编辑模型平台
- 删除模型平台
- 复制模型平台配置
- 切换启用/禁用状态
- 排序设置
- 模型版本管理 (支持数组)

## 数据表字段说明

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | bigint | 主键 ID |
| model_platform | varchar(100) | 模型平台标识 (如 deepseek/siliconflow/openai/ollama) |
| model_desc | varchar(255) | 模型平台描述 |
| icon | varchar(500) | 模型平台图标 URL |
| official_website | varchar(500) | 官网链接 |
| base_url | varchar(500) | 基础 API URL |
| model_versions | json | 模型版本数组 (JSON 格式存储) |
| is_active | tinyint(1) | 是否启用 (1=启用，0=禁用) |
| sort_order | int(11) | 排序顺序 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

## 初始化数据
默认包含 4 个模型平台:
1. deepseek - DeepSeek 大模型
2. siliconflow - 硅基流动
3. openai - OpenAI
4. ollama - Ollama 本地部署

## 使用方式

### 后端测试
启动服务后，可以通过 Postman 或 curl 测试接口:
```bash
# 查询所有模型平台
curl -X POST http://localhost:8080/eaAgent/modelPlatform/list

# 查询模型平台类型 (兼容旧接口)
curl -X POST http://localhost:8080/eaAgent/ai/queryChatModelTypeList
```

### 前端访问
需要在路由中添加 ModelPlatformConfig 页面的路由配置。
建议添加到 AgentManager 或单独的管理菜单中。

## 注意事项
1. 数据库连接信息在 mbg/generatorConfig.xml 中配置
2. 模型版本以 JSON 数组格式存储在数据库中
3. 前端输入时支持每行一个版本号，自动转换为 JSON 数组
4. 平台标识 (modelPlatform) 具有唯一性约束
5. 原有枚举 ModelTypeEnum 不再使用，但保留作为参考

## 下一步
1. 执行 SQL 脚本创建表和初始数据
2. 启动后端服务
3. 在前端路由中添加模型平台配置页面
4. 测试 CRUD 功能
5. 验证原有功能是否正常 (Agent配置等)
