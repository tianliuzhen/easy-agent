/*
 Navicat Premium Dump SQL

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80409 (8.4.9)
 Source Host           : localhost:3306
 Source Schema         : easy-agent

 Target Server Type    : MySQL
 Target Server Version : 80409 (8.4.9)
 File Encoding         : 65001

 Date: 14/06/2026 20:59:20
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ea_agent
-- ----------------------------
DROP TABLE IF EXISTS `ea_agent`;
CREATE TABLE `ea_agent` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `agent_name` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'agent名称',
  `agent_key` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'agentKey',
  `avatar` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '头像',
  `agent_desc` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT 'agent备注',
  `model_platform` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '大模型平台: ollama/deepseek/硅基流动',
  `analysis_model` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '决策大模型：todo',
  `tool_model` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '工具大模型：todo',
  `tool_run_mode` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '工具运行模式：reAct/tool',
  `prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '提示词',
  `model_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '模型配置:{}',
  `memory_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '记忆配置:{}',
  `welcome_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '欢迎语：对话开始时展示给用户的开场白',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='agent管理表';

-- ----------------------------
-- Table structure for ea_chat_conversation
-- ----------------------------
DROP TABLE IF EXISTS `ea_chat_conversation`;
CREATE TABLE `ea_chat_conversation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '会话标题（取第一次问题的前50个字符）',
  `session_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `agent_id` bigint NOT NULL COMMENT '关联的Agent ID',
  `user_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户ID（预留字段，未来扩展用）',
  `message_count` int NOT NULL DEFAULT '0' COMMENT '消息总数',
  `last_message_time` timestamp NULL DEFAULT NULL COMMENT '最后消息时间',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'active' COMMENT '会话状态：active-活跃, archived-已归档, deleted-已删除',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `accumulated_input_tokens` bigint DEFAULT '0' COMMENT '累计输入Token数',
  `accumulated_output_tokens` bigint DEFAULT '0' COMMENT '累计输出Token数',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_agent_id_created_at` (`agent_id`,`created_at`) USING BTREE,
  KEY `idx_status_updated_at` (`status`,`updated_at`) USING BTREE,
  KEY `idx_created_at` (`created_at`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=82 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='聊天会话表';

-- ----------------------------
-- Table structure for ea_chat_message
-- ----------------------------
DROP TABLE IF EXISTS `ea_chat_message`;
CREATE TABLE `ea_chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `conversation_id` bigint NOT NULL COMMENT '会话ID',
  `question` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '问题',
  `answer` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '回答',
  `thinking_log` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '思考过程日志（单独存储）',
  `tool_calls` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '工具调用信息（JSON格式）',
  `model_used` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '使用的模型',
  `tokens_used` int DEFAULT NULL COMMENT '消耗的token数',
  `response_time` decimal(10,2) DEFAULT NULL COMMENT '响应时间（毫秒）',
  `output_tokens_used` bigint DEFAULT NULL COMMENT '输出Token',
  `Input_tokens_used` bigint DEFAULT NULL COMMENT '输入Token',
  `sequence` int NOT NULL DEFAULT '1' COMMENT '消息序号（用于排序，从1开始）',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `message_context` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '聊天上下文',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_conversation_id_sequence` (`conversation_id`,`sequence`) USING BTREE,
  KEY `idx_conversation_id_created_at` (`conversation_id`,`created_at`) USING BTREE,
  KEY `idx_created_at` (`created_at`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=143 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='聊天消息表';

-- ----------------------------
-- Table structure for ea_function
-- ----------------------------
DROP TABLE IF EXISTS `ea_function`;
CREATE TABLE `ea_function` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工具名称',
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '工具类型',
  `desc` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工具描述',
  `metadata` varchar(10240) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '元数据',
  `input_template` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '结构化入参模板',
  `output_template` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '结构化出参模板',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for ea_iam_user
-- ----------------------------
DROP TABLE IF EXISTS `ea_iam_user`;
CREATE TABLE `ea_iam_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码（BCrypt加密）',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号',
  `status` tinyint DEFAULT '1' COMMENT '状态：1=正常，0=禁用',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_username` (`username`) USING BTREE,
  KEY `idx_email` (`email`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='用户表';

-- ----------------------------
-- Table structure for ea_iam_user_permission
-- ----------------------------
DROP TABLE IF EXISTS `ea_iam_user_permission`;
CREATE TABLE `ea_iam_user_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `permission_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限编码（如ADMIN/USER或agent:read等细粒度权限）',
  `permission_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限名称（如管理员、普通用户、查看Agent等）',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '权限描述',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_permission` (`user_id`,`permission_code`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_permission_code` (`permission_code`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='用户权限表';

-- ----------------------------
-- Table structure for ea_knowledge_base
-- ----------------------------
DROP TABLE IF EXISTS `ea_knowledge_base`;
CREATE TABLE `ea_knowledge_base` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `agent_id` bigint DEFAULT NULL COMMENT 'agentId',
  `kb_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '知识库名称',
  `kb_desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '知识库描述',
  `kb_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '知识库类型',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件名',
  `file_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件类型（txt/pdf）',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小（字节）',
  `doc_count` int DEFAULT NULL COMMENT '切分后的文档数量',
  `doc_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '文档分片ID列表，JSON数组格式，如：["doc_id_1","doc_id_2"]',
  `status` tinyint DEFAULT '1' COMMENT '状态：1-正常，0-已删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'creator',
  `catalog` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '分类',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_kb_name` (`kb_name`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='知识库管理表';

-- ----------------------------
-- Table structure for ea_knowledge_relation
-- ----------------------------
DROP TABLE IF EXISTS `ea_knowledge_relation`;
CREATE TABLE `ea_knowledge_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `knowledge_base_id` bigint DEFAULT NULL COMMENT 'agentId',
  `agent_id` bigint DEFAULT NULL COMMENT 'agentId',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'creator',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='知识库关系表';

-- ----------------------------
-- Table structure for ea_mcp_config
-- ----------------------------
DROP TABLE IF EXISTS `ea_mcp_config`;
CREATE TABLE `ea_mcp_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户Id',
  `server_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '服务器名称',
  `server_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '服务器URL（SSE模式使用）',
  `transport_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SSE' COMMENT '传输类型：SSE/STDIO',
  `command` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '启动命令（STDIO模式使用）',
  `env_vars` json DEFAULT NULL COMMENT '环境变量（JSON数组）',
  `tool_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工具名称（MCP Server中的原始名称）',
  `tool_display_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工具显示名称',
  `tool_description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '工具描述',
  `input_schema` json DEFAULT NULL COMMENT '输入参数Schema（JSON格式）',
  `output_schema` json DEFAULT NULL COMMENT '输出参数Schema（JSON格式）',
  `tool_metadata` json DEFAULT NULL COMMENT '工具元数据（JSON格式）',
  `connection_timeout` int DEFAULT '30' COMMENT '连接超时时间（秒）',
  `max_retries` int DEFAULT '3' COMMENT '最大重试次数',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'active' COMMENT '状态：active/inactive/error',
  `last_connected_at` datetime DEFAULT NULL COMMENT '最后连接时间',
  `last_error` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '最后错误信息',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '描述信息',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='MCP服务配置表';

-- ----------------------------
-- Table structure for ea_mcp_relation
-- ----------------------------
DROP TABLE IF EXISTS `ea_mcp_relation`;
CREATE TABLE `ea_mcp_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `agent_id` bigint NOT NULL COMMENT 'Agent ID',
  `mcp_config_id` bigint NOT NULL COMMENT 'MCP配置ID',
  `binding_config` json DEFAULT NULL COMMENT '绑定配置（JSON格式，可覆盖默认参数）',
  `sort_order` int DEFAULT '0' COMMENT '排序顺序',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否启用：1=启用，0=禁用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='MCP关系表（Agent绑定）';

-- ----------------------------
-- Table structure for ea_model_platform
-- ----------------------------
DROP TABLE IF EXISTS `ea_model_platform`;
CREATE TABLE `ea_model_platform` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `model_platform` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模型平台标识 (如 deepseek/siliconflow/openai/ollama)',
  `model_desc` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '模型平台描述',
  `icon` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '模型平台图标 URL',
  `official_website` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '官网链接',
  `base_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '基础 API URL',
  `model_versions` json DEFAULT NULL COMMENT '模型版本数组 (JSON 格式存储)',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否启用 (1=启用，0=禁用)',
  `max_token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '最大模型数',
  `sort_order` int DEFAULT '0' COMMENT '排序顺序',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_model_platform` (`model_platform`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='模型平台配置表';

-- ----------------------------
-- Table structure for ea_permission
-- ----------------------------
DROP TABLE IF EXISTS `ea_permission`;
CREATE TABLE `ea_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `permission_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限编码（如agent:read、agent:write）',
  `permission_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限名称',
  `resource_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '资源类型：menu/button/api',
  `resource_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '资源路径',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '权限描述',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_permission_code` (`permission_code`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='权限表';

-- ----------------------------
-- Table structure for ea_skill_config
-- ----------------------------
DROP TABLE IF EXISTS `ea_skill_config`;
CREATE TABLE `ea_skill_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint DEFAULT '0' COMMENT '用户ID（0表示官方技能）',
  `skill_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '技能名称（唯一标识）',
  `skill_display_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '技能显示名称',
  `skill_description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '技能描述',
  `skill_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'INTERNAL' COMMENT '技能类型：INTERNAL(内部)/EXTERNAL(外部)/PLUGIN(插件)',
  `skill_category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'general' COMMENT '技能分类：general/development/data/media/etc',
  `skill_icon` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '技能图标URL或emoji',
  `skill_version` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '1.0.0' COMMENT '技能版本号',
  `skill_provider` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'System' COMMENT '技能提供者',
  `skill_capabilities` json DEFAULT NULL COMMENT '技能能力列表（JSON数组）',
  `input_schema` json DEFAULT NULL COMMENT '输入参数Schema（JSON格式）',
  `output_schema` json DEFAULT NULL COMMENT '输出参数Schema（JSON格式）',
  `skill_metadata` json DEFAULT NULL COMMENT '技能元数据（JSON格式）',
  `skill_config` json DEFAULT NULL COMMENT '技能配置（JSON格式，存储执行所需配置）',
  `execution_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'sync' COMMENT '执行模式：sync(同步)/async(异步)',
  `timeout` int DEFAULT '30' COMMENT '执行超时时间（秒）',
  `max_retries` int DEFAULT '3' COMMENT '最大重试次数',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'active' COMMENT '状态：active/inactive/error/deprecated',
  `last_executed_at` datetime DEFAULT NULL COMMENT '最后执行时间',
  `last_error` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '最后错误信息',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `uk_skill_name` (`skill_name`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='Skill技能配置表';

-- ----------------------------
-- Table structure for ea_skill_relation
-- ----------------------------
DROP TABLE IF EXISTS `ea_skill_relation`;
CREATE TABLE `ea_skill_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `agent_id` bigint NOT NULL COMMENT 'Agent ID',
  `skill_config_id` bigint NOT NULL COMMENT 'Skill配置ID',
  `binding_config` json DEFAULT NULL COMMENT '绑定配置（JSON格式，可覆盖默认参数）',
  `sort_order` int DEFAULT '0' COMMENT '排序顺序',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否启用：1=启用，0=禁用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_agent_skill` (`agent_id`,`skill_config_id`) USING BTREE,
  KEY `idx_agent_id` (`agent_id`) USING BTREE,
  KEY `idx_skill_config_id` (`skill_config_id`) USING BTREE,
  KEY `idx_is_active` (`is_active`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='Skill关系表（Agent绑定）';

-- ----------------------------
-- Table structure for ea_tool_config
-- ----------------------------
DROP TABLE IF EXISTS `ea_tool_config`;
CREATE TABLE `ea_tool_config` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint DEFAULT NULL COMMENT 'agentId',
  `tool_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工具类型 (SQL, HTTP, MCP, GRPC等)',
  `tool_instance_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工具实例ID（可选，用于区分同一类型的多个实例）',
  `tool_instance_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工具实例名称',
  `tool_instance_desc` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工具实例描述',
  `tool_value` json DEFAULT NULL COMMENT '默认值（JSON格式存储）',
  `input_template` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '入参模板',
  `out_template` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '出参模板',
  `is_required` tinyint(1) DEFAULT '1' COMMENT '是否必需 (1=是, 0=否)',
  `sort_order` int DEFAULT '0' COMMENT '排序顺序',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否启用 (1=启用, 0=禁用)',
  `extra_config` json DEFAULT NULL COMMENT '额外配置信息（JSON格式，用于存储工具特定的配置）',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=51 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='工具通用模板配置表';

-- ----------------------------
-- Table structure for ea_tool_relation
-- ----------------------------
DROP TABLE IF EXISTS `ea_tool_relation`;
CREATE TABLE `ea_tool_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tool_config_id` bigint DEFAULT NULL COMMENT '工具id',
  `agent_id` bigint DEFAULT NULL COMMENT 'agentId',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'creator',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_tool_config_id` (`tool_config_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='工具关系表';

-- ----------------------------
-- Table structure for ea_agent_quick_prompt
-- ----------------------------
DROP TABLE IF EXISTS `ea_agent_quick_prompt`;
CREATE TABLE `ea_agent_quick_prompt` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `agent_id` bigint NOT NULL COMMENT '关联的Agent ID',
  `label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '浮选按钮名称，如 售后/物流/投诉/咨询',
  `questions` json DEFAULT NULL COMMENT '推荐问题列表（JSON数组）',
  `sort_order` int DEFAULT '0' COMMENT '排序顺序',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否启用：1=启用，0=禁用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_agent_id` (`agent_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='Agent浮选提示词配置表';

SET FOREIGN_KEY_CHECKS = 1;
