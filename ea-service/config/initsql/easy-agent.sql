/*
 Navicat Premium Data Transfer

 Source Server         : localhost_3301 (8.0)
 Source Server Type    : MySQL
 Source Server Version : 80030
 Source Host           : localhost:3301
 Source Schema         : easy-agent

 Target Server Type    : MySQL
 Target Server Version : 80030
 File Encoding         : 65001

 Date: 19/04/2026 14:31:39
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ea_agent
-- ----------------------------
DROP TABLE IF EXISTS `ea_agent`;
CREATE TABLE `ea_agent`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `agent_name` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'agent名称',
  `agent_key` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'agentKey',
  `avatar` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '头像',
  `agent_desc` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'agent备注',
  `model_platform` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '大模型平台: ollama/deepseek/硅基流动',
  `analysis_model` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '决策大模型：todo',
  `tool_model` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '工具大模型：todo',
  `tool_run_mode` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '工具运行模式：reAct/tool',
  `prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '提示词',
  `model_config` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模型配置:{}',
  `created_at` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'agent管理表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ea_agent
-- ----------------------------
INSERT INTO `ea_agent` VALUES (1, '需求意图分析识别agentic', 'xu_qiu_yi_tu_fen_xi_shi_bie_AGENTIC', '🎯', '分析文档主题  Qwen/QwQ-32B', 'siliconflow', 'siliconflow', 'deepseek', 'Tool', '你是一位客服助手，正在处理用户的{{ inquiry_type }}。\n                \n{% if inquiry_type == \"投诉\" %}\n请先表达歉意，然后询问具体问题细节。\n语气要温和诚恳，避免使用推卸责任的表达。\n\n{% elif inquiry_type == \"咨询\" %}\n请提供准确、详细的信息，如果涉及多个方案可以对比说明。\n\n{% elif inquiry_type == \"售后\" %}\n请先确认订单信息，再根据用户问题提供相应的售后流程。\n\n{% else %}\n请礼貌询问用户的具体需求，并表示愿意提供帮助。\n{% endif %}\n\n用户问题：{{ user_message }}', '{\"apiKey\":\"sk-ewykgprysbswuqagkpbuglgodbxshtucznttazynawykgsds\",\"baseUrl\":\"https://api.siliconflow.cn/\",\"modelVersion\":\"deepseek-ai/DeepSeek-V3\"}', '2025-12-28 22:40:45', '2026-04-16 23:23:11');
INSERT INTO `ea_agent` VALUES (3, '查询黄金白银价格', 'cha_xun_huang_jin_bai_yin_jia_ge_', '💾', '测试', 'deepseek', 'deepseek', NULL, 'Tool', '你好\n', '{\"apiKey\":\"sk-5147377fd7b842bba58feec0b03a0bbe\",\"baseUrl\":\"https://api.deepseek.com\",\"modelVersion\":\"deepseek-chat\",\"completionsPath\":\"\"}', '2026-03-07 23:30:07', '2026-04-16 23:25:52');
INSERT INTO `ea_agent` VALUES (7, '查询时间demo', 'cha_xun_shi_jian_DEMO', '🤖', '测试', 'deepseek', 'deepseek', NULL, 'ReAct', NULL, '{\"apiKey\":\"sk-f26c256e24e6423ebceafab78ffe6878\",\"baseUrl\":\"https://api.deepseek.com/\",\"modelVersion\":\"deepseek-chat\",\"completionsPath\":\"\"}', '2026-03-07 23:30:07', '2026-03-08 21:28:46');
INSERT INTO `ea_agent` VALUES (8, '天气助手', 'tian_qi_zhu_shou_', '🤖', NULL, 'siliconflow', 'siliconflow', NULL, 'Tool', NULL, '{\"apiKey\":\"sk-ewykgprysbswuqagkpbuglgodbxshtucznttazynawykgsds\",\"baseUrl\":\"https://api.siliconflow.cn\",\"modelVersion\":\"Qwen/Qwen3-32B\",\"completionsPath\":\"\"}', '2026-04-05 22:10:19', '2026-04-16 22:36:34');
INSERT INTO `ea_agent` VALUES (9, '位置分析', 'wei_zhi_fen_xi_', '🤖', NULL, 'deepseek', 'deepseek', NULL, 'Tool', NULL, '{\"apiKey\":\"sk-f26c256e24e6423ebceafab78ffe6878\",\"baseUrl\":\"https://api.deepseek.com\",\"modelVersion\":\"deepseek-chat\"}', '2026-04-05 22:41:35', '2026-04-13 21:12:48');
INSERT INTO `ea_agent` VALUES (10, '查询贵金属价格', 'cha_xun_gui_jin_shu_jia_ge_', '🤖', NULL, 'qwen', 'qwen', NULL, 'Tool', NULL, '{\"apiKey\":\"sk-0cc836096581452b86b34e2f604d3a90\",\"baseUrl\":\"https://dashscope.aliyuncs.com/compatible-mode/\",\"modelVersion\":\"qwen3.5-35b-a3b\"}', '2026-04-18 22:40:14', '2026-04-18 23:11:55');

-- ----------------------------
-- Table structure for ea_chat_conversation
-- ----------------------------
DROP TABLE IF EXISTS `ea_chat_conversation`;
CREATE TABLE `ea_chat_conversation`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '会话标题（取第一次问题的前50个字符）',
  `session_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `agent_id` bigint(0) NOT NULL COMMENT '关联的Agent ID',
  `user_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户ID（预留字段，未来扩展用）',
  `message_count` int(0) NOT NULL DEFAULT 0 COMMENT '消息总数',
  `last_message_time` timestamp(0) NULL DEFAULT NULL COMMENT '最后消息时间',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'active' COMMENT '会话状态：active-活跃, archived-已归档, deleted-已删除',
  `created_at` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_at` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_agent_id_created_at`(`agent_id`, `created_at`) USING BTREE,
  INDEX `idx_status_updated_at`(`status`, `updated_at`) USING BTREE,
  INDEX `idx_created_at`(`created_at`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 321 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '聊天会话表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ea_chat_conversation
-- ----------------------------
INSERT INTO `ea_chat_conversation` VALUES (1, '查询黄金价格', NULL, 3, '1', 0, NULL, 'active', '2026-04-19 13:27:44', '2026-04-19 13:27:56');
INSERT INTO `ea_chat_conversation` VALUES (2, '查询黄金白银价格', NULL, 3, '1', 0, NULL, 'active', '2026-04-19 13:42:29', '2026-04-19 13:42:41');

-- ----------------------------
-- Table structure for ea_chat_message
-- ----------------------------
DROP TABLE IF EXISTS `ea_chat_message`;
CREATE TABLE `ea_chat_message`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `conversation_id` bigint(0) NOT NULL COMMENT '会话ID',
  `question` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '问题',
  `answer` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '回答',
  `thinking_log` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '思考过程日志（单独存储）',
  `tool_calls` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '工具调用信息（JSON格式）',
  `model_used` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '使用的模型',
  `tokens_used` int(0) NULL DEFAULT NULL COMMENT '消耗的token数',
  `response_time` decimal(10, 2) NULL DEFAULT NULL COMMENT '响应时间（毫秒）',
  `sequence` int(0) NOT NULL DEFAULT 1 COMMENT '消息序号（用于排序，从1开始）',
  `created_at` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `message_context` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '聊天上下文',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_conversation_id_sequence`(`conversation_id`, `sequence`) USING BTREE,
  INDEX `idx_conversation_id_created_at`(`conversation_id`, `created_at`) USING BTREE,
  INDEX `idx_created_at`(`created_at`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 534 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '聊天消息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ea_chat_message
-- ----------------------------
INSERT INTO `ea_chat_message` VALUES (1, 1, '查询黄金价格', '', '', '', 'deepseek-chat', 0, 12.18, 2, '2026-04-19 13:27:56', '[{\"type\":\"data\",\"value\":\"我来帮您查询黄金价格。\",\"time\":1776576472640},{\"type\":\"data\",\"value\":\"根据查询结果，黄金目前的价格是 **1050元每克**。\",\"time\":1776576476334}]');
INSERT INTO `ea_chat_message` VALUES (2, 1, '查询黄金价格\n', '', '', '', 'deepseek-chat', 0, 5.11, 3, '2026-04-19 13:28:25', '[{\"type\":\"data\",\"value\":\"我来为您查询黄金价格。\",\"time\":1776576504511}]');
INSERT INTO `ea_chat_message` VALUES (3, 2, '查询黄金白银价格', '', '', '', 'deepseek-chat', 0, 11.62, 2, '2026-04-19 13:42:41', '[{\"type\":\"data\",\"value\":\"我来帮您查询黄金和白银的价格。\",\"time\":1776577356155},{\"type\":\"data\",\"value\":\"根据查询结果：\\n\\n**黄金价格**：1050元/克\\n**白银价格**：20元/克\\n\\n当前黄金价格是白银价格的52.5倍（1050 ÷ 20 = 52.5）。\",\"time\":1776577360652}]');

-- ----------------------------
-- Table structure for ea_function
-- ----------------------------
DROP TABLE IF EXISTS `ea_function`;
CREATE TABLE `ea_function`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工具名称',
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '工具类型',
  `desc` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工具描述',
  `metadata` varchar(10240) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '元数据',
  `input_template` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '结构化入参模板',
  `output_template` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '结构化出参模板',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ea_function
-- ----------------------------

-- ----------------------------
-- Table structure for ea_iam_user
-- ----------------------------
DROP TABLE IF EXISTS `ea_iam_user`;
CREATE TABLE `ea_iam_user`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码（BCrypt加密）',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '手机号',
  `status` tinyint(0) NULL DEFAULT 1 COMMENT '状态：1=正常，0=禁用',
  `created_at` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_at` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username`) USING BTREE,
  INDEX `idx_email`(`email`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ea_iam_user
-- ----------------------------
INSERT INTO `ea_iam_user` VALUES (1, 'admin', '$2a$10$KqwTeslM5eL8BiomZD0DH.znkLdDf4aNH8xylpRCgS9OVlgg..wRu', 'admin@example.com', '13800138000', 1, '2026-04-19 13:52:51', '2026-04-19 14:00:20');
INSERT INTO `ea_iam_user` VALUES (2, 'user', '$2a$2a$10$KqwTeslM5eL8BiomZD0DH.znkLdDf4aNH8xylpRCgS9OVlgg..wRu', 'user@example.com', '13800138001', 1, '2026-04-19 13:52:51', '2026-04-19 14:00:21');

-- ----------------------------
-- Table structure for ea_iam_user_permission
-- ----------------------------
DROP TABLE IF EXISTS `ea_iam_user_permission`;
CREATE TABLE `ea_iam_user_permission`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(0) NOT NULL COMMENT '用户ID',
  `permission_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限编码（如ADMIN/USER或agent:read等细粒度权限）',
  `permission_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限名称（如管理员、普通用户、查看Agent等）',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '权限描述',
  `created_at` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_permission`(`user_id`, `permission_code`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_permission_code`(`permission_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 22 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户权限表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ea_iam_user_permission
-- ----------------------------
INSERT INTO `ea_iam_user_permission` VALUES (1, 1, 'ADMIN', '管理员', '拥有所有权限', '2026-04-19 13:52:51');
INSERT INTO `ea_iam_user_permission` VALUES (2, 1, 'agent:read', '查看Agent', '查看Agent相关接口', '2026-04-19 13:52:51');
INSERT INTO `ea_iam_user_permission` VALUES (3, 1, 'agent:write', '编辑Agent', '编辑Agent相关接口', '2026-04-19 13:52:51');
INSERT INTO `ea_iam_user_permission` VALUES (4, 1, 'agent:delete', '删除Agent', '删除Agent相关接口', '2026-04-19 13:52:51');
INSERT INTO `ea_iam_user_permission` VALUES (5, 1, 'tool:read', '查看工具', '查看工具相关接口', '2026-04-19 13:52:51');
INSERT INTO `ea_iam_user_permission` VALUES (6, 1, 'tool:write', '编辑工具', '编辑工具相关接口', '2026-04-19 13:52:51');
INSERT INTO `ea_iam_user_permission` VALUES (7, 1, 'tool:delete', '删除工具', '删除工具相关接口', '2026-04-19 13:52:51');
INSERT INTO `ea_iam_user_permission` VALUES (8, 1, 'knowledge:read', '查看知识库', '查看知识库相关接口', '2026-04-19 13:52:51');
INSERT INTO `ea_iam_user_permission` VALUES (9, 1, 'knowledge:write', '编辑知识库', '编辑知识库相关接口', '2026-04-19 13:52:51');
INSERT INTO `ea_iam_user_permission` VALUES (10, 1, 'knowledge:delete', '删除知识库', '删除知识库相关接口', '2026-04-19 13:52:51');
INSERT INTO `ea_iam_user_permission` VALUES (11, 1, 'chat:read', '查看聊天', '查看聊天相关接口', '2026-04-19 13:52:51');
INSERT INTO `ea_iam_user_permission` VALUES (12, 1, 'chat:write', '发送消息', '发送消息相关接口', '2026-04-19 13:52:51');
INSERT INTO `ea_iam_user_permission` VALUES (13, 1, 'user:read', '查看用户', '查看用户相关接口', '2026-04-19 13:52:51');
INSERT INTO `ea_iam_user_permission` VALUES (14, 1, 'user:write', '编辑用户', '编辑用户相关接口', '2026-04-19 13:52:51');
INSERT INTO `ea_iam_user_permission` VALUES (15, 1, 'user:delete', '删除用户', '删除用户相关接口', '2026-04-19 13:52:51');
INSERT INTO `ea_iam_user_permission` VALUES (16, 2, 'USER', '普通用户', '拥有基本权限', '2026-04-19 13:52:51');
INSERT INTO `ea_iam_user_permission` VALUES (17, 2, 'agent:read', '查看Agent', '查看Agent相关接口', '2026-04-19 13:52:51');
INSERT INTO `ea_iam_user_permission` VALUES (18, 2, 'tool:read', '查看工具', '查看工具相关接口', '2026-04-19 13:52:51');
INSERT INTO `ea_iam_user_permission` VALUES (19, 2, 'knowledge:read', '查看知识库', '查看知识库相关接口', '2026-04-19 13:52:51');
INSERT INTO `ea_iam_user_permission` VALUES (20, 2, 'chat:read', '查看聊天', '查看聊天相关接口', '2026-04-19 13:52:51');
INSERT INTO `ea_iam_user_permission` VALUES (21, 2, 'chat:write', '发送消息', '发送消息相关接口', '2026-04-19 13:52:51');

-- ----------------------------
-- Table structure for ea_knowledge_base
-- ----------------------------
DROP TABLE IF EXISTS `ea_knowledge_base`;
CREATE TABLE `ea_knowledge_base`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `agent_id` bigint(0) NULL DEFAULT NULL COMMENT 'agentId',
  `kb_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '知识库名称',
  `kb_desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '知识库描述',
  `kb_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '知识库类型',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件名',
  `file_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件类型（txt/pdf）',
  `file_size` bigint(0) NULL DEFAULT NULL COMMENT '文件大小（字节）',
  `doc_count` int(0) NULL DEFAULT NULL COMMENT '切分后的文档数量',
  `doc_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '文档分片ID列表，JSON数组格式，如：[\"doc_id_1\",\"doc_id_2\"]',
  `status` tinyint(0) NULL DEFAULT 1 COMMENT '状态：1-正常，0-已删除',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `creator` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'creator',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_kb_name`(`kb_name`) USING BTREE,
  INDEX `idx_create_time`(`create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识库管理表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ea_knowledge_base
-- ----------------------------
INSERT INTO `ea_knowledge_base` VALUES (1, 1, '我爱学AI', 'ai知识库', NULL, '1', 'txt', 252, 1, '[\"EZlRXbU52mJgHeCN_0\"]', 1, '2026-02-01 13:02:21', '2026-03-21 22:29:26', '1');
INSERT INTO `ea_knowledge_base` VALUES (2, 1, '简历模板', '简历模板', NULL, '李博文简历.pdf', 'pdf', 302698, 2, '[\"6822b4e0-57c2-4c68-b07d-a57be8c66142_0\",\"738538c1-d85e-4748-aff4-619e270c68b4_0\"]', 1, '2026-02-01 13:03:40', '2026-03-21 22:29:26', '1');
INSERT INTO `ea_knowledge_base` VALUES (3, 1, 'qoder文档', 'qoder文档', NULL, 'image.png', 'png', 84014, 1, '[\"b32b842d-75e9-494c-ba53-c68b8936962b_0\"]', 1, '2026-02-01 14:20:05', '2026-03-21 22:29:26', '1');
INSERT INTO `ea_knowledge_base` VALUES (4, 1, '1', '1', NULL, 'image.png', 'png', 79475, 1, '[\"4dfb1384-1a2f-4588-a6aa-78b768520dce_0\"]', 1, '2026-02-01 14:42:24', '2026-03-21 22:29:26', '1');
INSERT INTO `ea_knowledge_base` VALUES (5, 1, '155规划', '155规划', NULL, 'image.png', 'png', 129370, 1, '[\"ed6e4c64-23ad-4626-a064-b4dd0ba87e3d_0\"]', 1, '2026-02-02 14:28:28', '2026-03-21 22:29:26', '1');
INSERT INTO `ea_knowledge_base` VALUES (6, 1, 'es使用说明', 'es使用说明', NULL, 'image.png', 'png', 26008, 1, '[\"a7c03969-ad41-4e50-9762-380200ec69a3_0\"]', 1, '2026-02-08 09:58:55', '2026-03-21 22:29:26', '1');

-- ----------------------------
-- Table structure for ea_knowledge_relation
-- ----------------------------
DROP TABLE IF EXISTS `ea_knowledge_relation`;
CREATE TABLE `ea_knowledge_relation`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `knowledge_base_id` bigint(0) NULL DEFAULT NULL COMMENT 'agentId',
  `agent_id` bigint(0) NULL DEFAULT NULL COMMENT 'agentId',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `creator` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'creator',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_create_time`(`create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识库关系表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ea_knowledge_relation
-- ----------------------------
INSERT INTO `ea_knowledge_relation` VALUES (7, 1, 3, '2026-03-21 22:21:12', '2026-03-21 22:21:12', 'system');
INSERT INTO `ea_knowledge_relation` VALUES (8, 3, 3, '2026-03-21 22:26:14', '2026-03-21 22:26:14', 'system');
INSERT INTO `ea_knowledge_relation` VALUES (9, 1, 1, '2026-03-21 23:26:00', '2026-03-21 23:26:00', 'system');
INSERT INTO `ea_knowledge_relation` VALUES (10, 2, 1, '2026-03-22 19:54:10', '2026-03-22 19:54:10', 'system');

-- ----------------------------
-- Table structure for ea_mcp_config
-- ----------------------------
DROP TABLE IF EXISTS `ea_mcp_config`;
CREATE TABLE `ea_mcp_config`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户Id',
  `server_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '服务器名称',
  `server_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '服务器URL（SSE模式使用）',
  `transport_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SSE' COMMENT '传输类型：SSE/STDIO',
  `command` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '启动命令（STDIO模式使用）',
  `env_vars` json NULL COMMENT '环境变量（JSON数组）',
  `tool_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '工具名称（MCP Server中的原始名称）',
  `tool_display_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '工具显示名称',
  `tool_description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '工具描述',
  `input_schema` json NULL COMMENT '输入参数Schema（JSON格式）',
  `output_schema` json NULL COMMENT '输出参数Schema（JSON格式）',
  `tool_metadata` json NULL COMMENT '工具元数据（JSON格式）',
  `connection_timeout` int(0) NULL DEFAULT 30 COMMENT '连接超时时间（秒）',
  `max_retries` int(0) NULL DEFAULT 3 COMMENT '最大重试次数',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'active' COMMENT '状态：active/inactive/error',
  `last_connected_at` datetime(0) NULL DEFAULT NULL COMMENT '最后连接时间',
  `last_error` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '最后错误信息',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '描述信息',
  `created_at` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_at` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'MCP服务配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ea_mcp_config
-- ----------------------------
INSERT INTO `ea_mcp_config` VALUES (1, '1', '查询天气', 'http://localhost:8083/api/mcp', 'STREAMABLE', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 30, 3, 'error', '2026-04-08 21:14:21', 'Client failed to initialize by explicit API call', NULL, '2026-04-05 00:15:05', '2026-04-05 00:37:21');
INSERT INTO `ea_mcp_config` VALUES (2, '1', '高德地图Amap', 'https://mcpmarket.cn/mcp/017ea1dc3917044b7949e54d', 'STREAMABLE', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 30, 3, 'active', '2026-04-13 21:24:57', NULL, NULL, '2026-04-05 22:40:11', '2026-04-05 22:40:11');
INSERT INTO `ea_mcp_config` VALUES (4, '1', '文件系统操作', NULL, 'STREAMABLE', 'npx -y @modelcontextprotocol/server-filesystem /allowed/path', NULL, 'filesystem', '文件系统操作', '提供文件读写、目录遍历、文件搜索等功能，支持多种文件格式', NULL, NULL, NULL, 30, 3, 'active', NULL, NULL, NULL, '2026-04-06 22:36:45', '2026-04-06 22:36:45');
INSERT INTO `ea_mcp_config` VALUES (5, '1', '文件系统操作', NULL, 'STREAMABLE', 'npx -y @modelcontextprotocol/server-filesystem /allowed/path', NULL, 'filesystem', '文件系统操作', '提供文件读写、目录遍历、文件搜索等功能，支持多种文件格式', NULL, NULL, NULL, 30, 3, 'active', NULL, NULL, NULL, '2026-04-06 22:42:08', '2026-04-06 22:42:08');
INSERT INTO `ea_mcp_config` VALUES (6, '1', 'Brave 搜索', NULL, 'STREAMABLE', 'npx -y @modelcontextprotocol/server-brave-search', NULL, 'brave-search', 'Brave 搜索', '实时网页搜索和信息提取，基于 Brave Search API', NULL, NULL, NULL, 30, 3, 'active', NULL, NULL, NULL, '2026-04-06 22:42:19', '2026-04-06 22:42:19');

-- ----------------------------
-- Table structure for ea_mcp_relation
-- ----------------------------
DROP TABLE IF EXISTS `ea_mcp_relation`;
CREATE TABLE `ea_mcp_relation`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `agent_id` bigint(0) NOT NULL COMMENT 'Agent ID',
  `mcp_config_id` bigint(0) NOT NULL COMMENT 'MCP配置ID',
  `binding_config` json NULL COMMENT '绑定配置（JSON格式，可覆盖默认参数）',
  `sort_order` int(0) NULL DEFAULT 0 COMMENT '排序顺序',
  `is_active` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用：1=启用，0=禁用',
  `created_at` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_at` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'MCP关系表（Agent绑定）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ea_mcp_relation
-- ----------------------------
INSERT INTO `ea_mcp_relation` VALUES (2, 8, 1, NULL, 0, 1, '2026-04-05 22:10:30', '2026-04-05 22:10:30');
INSERT INTO `ea_mcp_relation` VALUES (3, 9, 2, NULL, 0, 1, '2026-04-05 22:41:45', '2026-04-05 22:41:45');

-- ----------------------------
-- Table structure for ea_model_platform
-- ----------------------------
DROP TABLE IF EXISTS `ea_model_platform`;
CREATE TABLE `ea_model_platform`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `model_platform` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模型平台标识 (如 deepseek/siliconflow/openai/ollama)',
  `model_desc` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模型平台描述',
  `icon` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模型平台图标 URL',
  `official_website` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '官网链接',
  `base_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '基础 API URL',
  `model_versions` json NULL COMMENT '模型版本数组 (JSON 格式存储)',
  `is_active` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用 (1=启用，0=禁用)',
  `sort_order` int(0) NULL DEFAULT 0 COMMENT '排序顺序',
  `created_at` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_at` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_model_platform`(`model_platform`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '模型平台配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ea_model_platform
-- ----------------------------
INSERT INTO `ea_model_platform` VALUES (1, 'deepseek', 'deepseek', 'https://www.deepseek.com/favicon.ico', 'https://api.deepseek.com', 'https://api.deepseek.com', '[\"deepseek-chat\", \"deepseek-reasoner\"]', 1, 1, '2026-03-07 21:42:27', '2026-03-08 13:43:32');
INSERT INTO `ea_model_platform` VALUES (2, 'siliconflow', '硅基流动', 'https://cloud.siliconflow.cn/favicon.ico', 'https://cloud.siliconflow.cn/v1', 'https://api.siliconflow.cn', '[\"Pro/Qwen/Qwen2.5-7B-Instruct\", \"deepseek-ai/DeepSeek-R1-Distill-Qwen-32B\", \"fnlp/MOSS-TTSD-v0.5\", \"Qwen/Qwen3-VL-235B-A22B-Thinking\", \"Qwen/Qwen3-VL-32B-Thinking\", \"Pro/THUDM/glm-4-9b-chat\", \"black-forest-labs/FLUX.1-schnell\", \"Qwen/Qwen2.5-Coder-32B-Instruct\", \"LoRA/Qwen/Qwen2.5-7B-Instruct\", \"RVC-Boss/GPT-SoVITS\", \"LoRA/Qwen/Qwen2.5-14B-Instruct\", \"Qwen/Qwen3-Reranker-8B\", \"Qwen/Qwen3-30B-A3B-Instruct-2507\", \"Qwen/Qwen3-Omni-30B-A3B-Instruct\", \"Qwen/Qwen2.5-7B-Instruct\", \"Pro/BAAI/bge-m3\", \"deepseek-ai/DeepSeek-R1-Distill-Qwen-7B\", \"THUDM/GLM-4-32B-0414\", \"THUDM/GLM-Z1-32B-0414\", \"Qwen/Qwen3-Reranker-0.6B\", \"moonshotai/Kimi-Dev-72B\", \"TeleAI/TeleSpeechASR\", \"Qwen/Qwen2.5-32B-Instruct\", \"MiniMaxAI/MiniMax-M1-80k\", \"Wan-AI/Wan2.2-I2V-A14B\", \"Qwen/Qwen3-Omni-30B-A3B-Thinking\", \"Qwen/Qwen3-VL-30B-A3B-Instruct\", \"MiniMaxAI/MiniMax-M2\", \"Pro/Qwen/Qwen2-7B-Instruct\", \"Pro/Qwen/Qwen2.5-Coder-7B-Instruct\", \"Qwen/Qwen2.5-72B-Instruct\", \"Qwen/Qwen2.5-Coder-7B-Instruct\", \"Pro/deepseek-ai/DeepSeek-R1-Distill-Qwen-7B\", \"Qwen/Qwen3-Embedding-0.6B\", \"Pro/THUDM/GLM-4.1V-9B-Thinking\", \"Qwen/Qwen3-Next-80B-A3B-Instruct\", \"deepseek-ai/DeepSeek-R1\", \"Qwen/Qwen3-VL-30B-A3B-Thinking\", \"LoRA/Qwen/Qwen2.5-72B-Instruct\", \"ascend-tribe/pangu-pro-moe\", \"stepfun-ai/step3\", \"zai-org/GLM-4.5V\", \"Qwen/Qwen-Image\", \"BAAI/bge-reranker-v2-m3\", \"THUDM/GLM-Z1-9B-0414\", \"Qwen/Qwen3-Embedding-4B\", \"baidu/ERNIE-4.5-VL-424B-A47B-Paddle\", \"Wan-AI/Wan2.2-T2V-A14B\", \"deepseek-ai/DeepSeek-V3\", \"Qwen/Qwen3-VL-8B-Instruct\", \"zai-org/GLM-4.6V\", \"deepseek-ai/DeepSeek-V2.5\", \"Qwen/Qwen3-32B\", \"baidu/ERNIE-4.5-21B-A3B-Paddle\", \"THUDM/GLM-4.1V-9B-Thinking\", \"zai-org/GLM-4.5\", \"zai-org/GLM-4.6\", \"internlm/internlm2_5-7b-chat\", \"LoRA/Qwen/Qwen2.5-32B-Instruct\", \"Qwen/QVQ-72B-Preview\", \"Qwen/Qwen3-30B-A3B-Thinking-2507\", \"Qwen/Qwen3-Coder-30B-A3B-Instruct\", \"Qwen/QwQ-32B\", \"baidu/ERNIE-4.5-300B-A47B\", \"ByteDance-Seed/Seed-OSS-36B-Instruct\", \"Kwaipilot/KAT-Dev\", \"FunAudioLLM/CosyVoice2-0.5B\", \"netease-youdao/bce-reranker-base_v1\", \"Qwen/Qwen2.5-72B-Instruct-128K\", \"Pro/black-forest-labs/FLUX.1-schnell\", \"Qwen/Qwen3-Coder-480B-A35B-Instruct\", \"zai-org/GLM-4.5-Air\", \"Qwen/Qwen-Image-Edit\", \"Qwen/Qwen3-VL-8B-Thinking\", \"Qwen/Qwen2.5-VL-72B-Instruct\", \"SeedLLM/Seed-Rice-7B\", \"deepseek-ai/DeepSeek-OCR\", \"Qwen/Qwen2-VL-72B-Instruct\", \"deepseek-ai/DeepSeek-R1-0528-Qwen3-8B\", \"deepseek-ai/DeepSeek-R1-Distill-Qwen-14B\", \"Tongyi-Zhiwen/QwenLong-L1-32B\", \"inclusionAI/Ling-mini-2.0\", \"Qwen/Qwen3-VL-235B-A22B-Instruct\", \"Qwen/Qwen3-Omni-30B-A3B-Captioner\", \"netease-youdao/bce-embedding-base_v1\", \"Pro/Qwen/Qwen2.5-VL-7B-Instruct\", \"Qwen/Qwen3-235B-A22B\", \"deepseek-ai/DeepSeek-V3.2\", \"deepseek-ai/deepseek-vl2\", \"Qwen/Qwen2.5-VL-32B-Instruct\", \"baidu/ERNIE-4.5-VL-28B-A3B-Paddle\", \"THUDM/glm-4-9b-chat\", \"Qwen/Qwen3-14B\", \"Qwen/Qwen3-Embedding-8B\", \"Qwen/Qwen3-Reranker-4B\", \"tencent/Hunyuan-MT-7B\", \"Qwen/Qwen2.5-14B-Instruct\", \"Pro/BAAI/bge-reranker-v2-m3\", \"Qwen/Qwen3-235B-A22B-Thinking-2507\", \"inclusionAI/Ling-flash-2.0\", \"moonshotai/Kimi-K2-Instruct-0905\", \"deepseek-ai/DeepSeek-V3.1-Terminus\", \"moonshotai/Kimi-K2-Thinking\", \"THUDM/GLM-Z1-Rumination-32B-0414\", \"Qwen/Qwen3-Next-80B-A3B-Thinking\", \"Qwen/Qwen-Image-Edit-2509\", \"IndexTeam/IndexTTS-2\", \"Qwen/Qwen3-VL-32B-Instruct\", \"Qwen/Qwen2-7B-Instruct\", \"black-forest-labs/FLUX.1-dev\", \"BAAI/bge-m3\", \"Kwai-Kolors/Kolors\", \"THUDM/GLM-4-9B-0414\", \"Qwen/Qwen3-30B-A3B\", \"tencent/Hunyuan-A13B-Instruct\", \"Qwen/Qwen3-235B-A22B-Instruct-2507\", \"inclusionAI/Ring-flash-2.0\", \"Qwen/Qwen3-8B\", \"FunAudioLLM/SenseVoiceSmall\"]', 1, 2, '2026-03-07 21:42:27', '2026-03-07 23:18:13');
INSERT INTO `ea_model_platform` VALUES (3, 'openai', 'openai', 'https://openai.com/favicon.ico', 'https://openai.com/', 'https://api.openai.com/v1', '[\"gpt-3.5-turbo\", \"gpt-4\"]', 1, 3, '2026-03-07 21:42:27', '2026-03-07 21:42:27');
INSERT INTO `ea_model_platform` VALUES (4, 'ollama', 'ollama', 'https://ollama.com/public/ollama.png', 'https://ollama.com/', 'http://localhost:11434', '[\"llama2\", \"mistral\"]', 1, 4, '2026-03-07 21:42:27', '2026-03-07 21:42:27');
INSERT INTO `ea_model_platform` VALUES (5, 'qwen', '阿里云', 'https://gw.alicdn.com/imgextra/i4/O1CN01vVn7g32134zNZEeAR_!!6000000006928-55-tps-24-24.svg', 'https://bailian.console.aliyun.com/', 'https://dashscope.aliyuncs.com/compatible-mode/v1', '[\"Qwen3-Max\", \"Qwen3.5-Plus\", \"Qwen3.5-Flash\", \"qwq-32b\"]', 1, 0, '2026-03-15 22:14:45', '2026-04-19 13:29:56');

-- ----------------------------
-- Table structure for ea_permission
-- ----------------------------
DROP TABLE IF EXISTS `ea_permission`;
CREATE TABLE `ea_permission`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `permission_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限编码（如agent:read、agent:write）',
  `permission_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限名称',
  `resource_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '资源类型：menu/button/api',
  `resource_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '资源路径',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '权限描述',
  `created_at` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_at` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_permission_code`(`permission_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '权限表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ea_permission
-- ----------------------------
INSERT INTO `ea_permission` VALUES (1, 'agent:read', '查看Agent', 'api', '/eaAgent/**', '查看Agent相关接口', '2026-03-14 22:22:12', '2026-03-14 22:22:12');
INSERT INTO `ea_permission` VALUES (2, 'agent:write', '编辑Agent', 'api', '/eaAgent/**', '编辑Agent相关接口', '2026-03-14 22:22:12', '2026-03-14 22:22:12');
INSERT INTO `ea_permission` VALUES (3, 'agent:delete', '删除Agent', 'api', '/eaAgent/**', '删除Agent相关接口', '2026-03-14 22:22:12', '2026-03-14 22:22:12');
INSERT INTO `ea_permission` VALUES (4, 'tool:read', '查看工具', 'api', '/toolManager/**', '查看工具相关接口', '2026-03-14 22:22:12', '2026-03-14 22:22:12');
INSERT INTO `ea_permission` VALUES (5, 'tool:write', '编辑工具', 'api', '/toolManager/**', '编辑工具相关接口', '2026-03-14 22:22:12', '2026-03-14 22:22:12');
INSERT INTO `ea_permission` VALUES (6, 'tool:delete', '删除工具', 'api', '/toolManager/**', '删除工具相关接口', '2026-03-14 22:22:12', '2026-03-14 22:22:12');
INSERT INTO `ea_permission` VALUES (7, 'knowledge:read', '查看知识库', 'api', '/knowledge/**', '查看知识库相关接口', '2026-03-14 22:22:12', '2026-03-14 22:22:12');
INSERT INTO `ea_permission` VALUES (8, 'knowledge:write', '编辑知识库', 'api', '/knowledge/**', '编辑知识库相关接口', '2026-03-14 22:22:12', '2026-03-14 22:22:12');
INSERT INTO `ea_permission` VALUES (9, 'knowledge:delete', '删除知识库', 'api', '/knowledge/**', '删除知识库相关接口', '2026-03-14 22:22:12', '2026-03-14 22:22:12');
INSERT INTO `ea_permission` VALUES (10, 'chat:read', '查看聊天', 'api', '/chat/**', '查看聊天相关接口', '2026-03-14 22:22:12', '2026-03-14 22:22:12');
INSERT INTO `ea_permission` VALUES (11, 'chat:write', '发送消息', 'api', '/chat/**', '发送消息相关接口', '2026-03-14 22:22:12', '2026-03-14 22:22:12');
INSERT INTO `ea_permission` VALUES (12, 'user:read', '查看用户', 'api', '/auth/**', '查看用户相关接口', '2026-03-14 22:22:12', '2026-03-14 22:22:12');
INSERT INTO `ea_permission` VALUES (13, 'user:write', '编辑用户', 'api', '/auth/**', '编辑用户相关接口', '2026-03-14 22:22:12', '2026-03-14 22:22:12');
INSERT INTO `ea_permission` VALUES (14, 'user:delete', '删除用户', 'api', '/auth/**', '删除用户相关接口', '2026-03-14 22:22:12', '2026-03-14 22:22:12');

-- ----------------------------
-- Table structure for ea_skill_config
-- ----------------------------
DROP TABLE IF EXISTS `ea_skill_config`;
CREATE TABLE `ea_skill_config`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(0) NULL DEFAULT 0 COMMENT '用户ID（0表示官方技能）',
  `skill_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '技能名称（唯一标识）',
  `skill_display_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '技能显示名称',
  `skill_description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '技能描述',
  `skill_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'INTERNAL' COMMENT '技能类型：INTERNAL(内部)/EXTERNAL(外部)/PLUGIN(插件)',
  `skill_category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'general' COMMENT '技能分类：general/development/data/media/etc',
  `skill_icon` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '技能图标URL或emoji',
  `skill_version` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '1.0.0' COMMENT '技能版本号',
  `skill_provider` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'System' COMMENT '技能提供者',
  `skill_capabilities` json NULL COMMENT '技能能力列表（JSON数组）',
  `input_schema` json NULL COMMENT '输入参数Schema（JSON格式）',
  `output_schema` json NULL COMMENT '输出参数Schema（JSON格式）',
  `skill_metadata` json NULL COMMENT '技能元数据（JSON格式）',
  `skill_config` json NULL COMMENT '技能配置（JSON格式，存储执行所需配置）',
  `execution_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'sync' COMMENT '执行模式：sync(同步)/async(异步)',
  `timeout` int(0) NULL DEFAULT 30 COMMENT '执行超时时间（秒）',
  `max_retries` int(0) NULL DEFAULT 3 COMMENT '最大重试次数',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'active' COMMENT '状态：active/inactive/error/deprecated',
  `last_executed_at` datetime(0) NULL DEFAULT NULL COMMENT '最后执行时间',
  `last_error` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '最后错误信息',
  `created_at` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_at` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `uk_skill_name`(`skill_name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Skill技能配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ea_skill_config
-- ----------------------------
INSERT INTO `ea_skill_config` VALUES (1, 0, 'code_review', '代码审查', '自动审查代码质量，检查潜在问题和改进建议', 'INTERNAL', 'development', '🔍', '1.0.0', 'System', '[\"code-analysis\", \"quality-check\", \"suggestion\"]', '{\"type\": \"object\", \"required\": [\"code\"], \"properties\": {\"code\": {\"type\": \"string\", \"description\": \"要审查的代码\"}, \"language\": {\"type\": \"string\", \"description\": \"编程语言\"}}}', '{\"type\": \"object\", \"properties\": {\"score\": {\"type\": \"number\"}, \"issues\": {\"type\": \"array\"}, \"suggestions\": {\"type\": \"array\"}}}', NULL, '{\"timeout\": 60}', 'sync', 30, 3, 'active', NULL, NULL, '2026-04-06 22:11:39', '2026-04-06 22:11:39');
INSERT INTO `ea_skill_config` VALUES (2, 0, 'text_summary', '文本摘要', '将长文本自动摘要为简洁的概述', 'INTERNAL', 'general', '📝', '1.0.0', 'System', '[\"text-processing\", \"summarization\"]', '{\"type\": \"object\", \"required\": [\"text\"], \"properties\": {\"text\": {\"type\": \"string\", \"description\": \"需要摘要的文本\"}, \"max_length\": {\"type\": \"integer\", \"default\": 200, \"description\": \"摘要最大长度\"}}}', '{\"type\": \"object\", \"properties\": {\"summary\": {\"type\": \"string\"}, \"key_points\": {\"type\": \"array\"}}}', NULL, '{}', 'sync', 30, 3, 'active', NULL, NULL, '2026-04-06 22:11:39', '2026-04-06 22:11:39');
INSERT INTO `ea_skill_config` VALUES (3, 0, 'data_analysis', '数据分析', '分析结构化数据，生成统计报告和可视化建议', 'INTERNAL', 'data', '📊', '1.0.0', 'System', '[\"data-processing\", \"statistics\", \"visualization\"]', '{\"type\": \"object\", \"required\": [\"data\"], \"properties\": {\"data\": {\"type\": \"object\", \"description\": \"要分析的数据\"}, \"analysis_type\": {\"type\": \"string\", \"description\": \"分析类型：summary/trend/correlation\"}}}', '{\"type\": \"object\", \"properties\": {\"charts\": {\"type\": \"array\"}, \"report\": {\"type\": \"object\"}}}', NULL, '{}', 'sync', 30, 3, 'active', NULL, NULL, '2026-04-06 22:11:39', '2026-04-06 22:11:39');
INSERT INTO `ea_skill_config` VALUES (4, 0, 'translation', '智能翻译', '多语言文本翻译，支持上下文感知', 'INTERNAL', 'general', '🌐', '1.0.0', 'System', '[\"translation\", \"multilingual\"]', '{\"type\": \"object\", \"required\": [\"text\", \"target_lang\"], \"properties\": {\"text\": {\"type\": \"string\", \"description\": \"要翻译的文本\"}, \"source_lang\": {\"type\": \"string\", \"description\": \"源语言\"}, \"target_lang\": {\"type\": \"string\", \"description\": \"目标语言\"}}}', '{\"type\": \"object\", \"properties\": {\"confidence\": {\"type\": \"number\"}, \"translated_text\": {\"type\": \"string\"}}}', NULL, '{}', 'sync', 30, 3, 'active', NULL, NULL, '2026-04-06 22:11:39', '2026-04-06 22:11:39');
INSERT INTO `ea_skill_config` VALUES (5, 0, 'json_formatter', 'JSON格式化', '格式化、验证和美化JSON数据', 'INTERNAL', 'development', '🔧', '1.0.0', 'System', '[\"json\", \"formatting\", \"validation\"]', '{\"type\": \"object\", \"required\": [\"json_text\"], \"properties\": {\"json_text\": {\"type\": \"string\", \"description\": \"JSON字符串\"}, \"operation\": {\"type\": \"string\", \"default\": \"format\", \"description\": \"操作：format/validate/minify\"}}}', '{\"type\": \"object\", \"properties\": {\"error\": {\"type\": \"string\"}, \"result\": {\"type\": \"string\"}, \"is_valid\": {\"type\": \"boolean\"}}}', NULL, '{}', 'sync', 30, 3, 'active', NULL, NULL, '2026-04-06 22:11:39', '2026-04-06 22:11:39');
INSERT INTO `ea_skill_config` VALUES (6, 0, 'image_analysis', '图像分析', '分析图像内容，提取文字和描述场景', 'INTERNAL', 'media', '🖼️', '1.0.0', 'System', '[\"image\", \"ocr\", \"scene-description\"]', '{\"type\": \"object\", \"required\": [\"image_url\"], \"properties\": {\"image_url\": {\"type\": \"string\", \"description\": \"图像URL或base64\"}, \"analysis_type\": {\"type\": \"string\", \"default\": \"scene\", \"description\": \"分析类型：ocr/scene/objects\"}}}', '{\"type\": \"object\", \"properties\": {\"objects\": {\"type\": \"array\"}, \"description\": {\"type\": \"string\"}, \"text_content\": {\"type\": \"string\"}}}', NULL, '{}', 'sync', 30, 3, 'active', NULL, NULL, '2026-04-06 22:11:39', '2026-04-06 22:11:39');
INSERT INTO `ea_skill_config` VALUES (7, 0, 'unit_converter', '单位转换', '支持多种单位类型的智能转换', 'INTERNAL', 'general', '⚖️', '1.0.0', 'System', '[\"conversion\", \"calculation\"]', '{\"type\": \"object\", \"required\": [\"value\", \"from_unit\", \"to_unit\"], \"properties\": {\"value\": {\"type\": \"number\", \"description\": \"要转换的数值\"}, \"to_unit\": {\"type\": \"string\", \"description\": \"目标单位\"}, \"from_unit\": {\"type\": \"string\", \"description\": \"源单位\"}}}', '{\"type\": \"object\", \"properties\": {\"result\": {\"type\": \"number\"}, \"formula\": {\"type\": \"string\"}}}', NULL, '{}', 'sync', 30, 3, 'active', NULL, NULL, '2026-04-06 22:11:39', '2026-04-06 22:11:39');
INSERT INTO `ea_skill_config` VALUES (8, 0, 'regex_generator', '正则生成器', '根据描述生成正则表达式并提供测试', 'INTERNAL', 'development', '🔤', '1.0.0', 'System', '[\"regex\", \"pattern-matching\", \"code-generation\"]', '{\"type\": \"object\", \"required\": [\"description\"], \"properties\": {\"description\": {\"type\": \"string\", \"description\": \"正则表达式要匹配的描述\"}, \"test_string\": {\"type\": \"string\", \"description\": \"测试字符串\"}}}', '{\"type\": \"object\", \"properties\": {\"regex\": {\"type\": \"string\"}, \"explanation\": {\"type\": \"string\"}, \"test_result\": {\"type\": \"object\"}}}', NULL, '{}', 'sync', 30, 3, 'active', NULL, NULL, '2026-04-06 22:11:39', '2026-04-06 22:11:39');
INSERT INTO `ea_skill_config` VALUES (9, 0, 'sql_generator', 'SQL生成器', '根据自然语言描述生成SQL查询语句', 'INTERNAL', 'data', '🗄️', '1.0.0', 'System', '[\"sql\", \"code-generation\", \"database\"]', '{\"type\": \"object\", \"required\": [\"description\"], \"properties\": {\"schema\": {\"type\": \"object\", \"description\": \"数据库表结构\"}, \"dialect\": {\"type\": \"string\", \"default\": \"mysql\", \"description\": \"SQL方言：mysql/postgresql/sqlite\"}, \"description\": {\"type\": \"string\", \"description\": \"SQL查询描述\"}}}', '{\"type\": \"object\", \"properties\": {\"sql\": {\"type\": \"string\"}, \"parameters\": {\"type\": \"array\"}, \"explanation\": {\"type\": \"string\"}}}', NULL, '{}', 'sync', 30, 3, 'active', NULL, NULL, '2026-04-06 22:11:39', '2026-04-06 22:11:39');
INSERT INTO `ea_skill_config` VALUES (10, 0, 'document_parser', '文档解析', '解析各种文档格式，提取文本内容', 'INTERNAL', 'media', '📄', '1.0.0', 'System', '[\"document\", \"parsing\", \"extraction\"]', '{\"type\": \"object\", \"required\": [\"document_url\", \"format\"], \"properties\": {\"format\": {\"type\": \"string\", \"description\": \"文档格式：pdf/doc/docx/txt\"}, \"document_url\": {\"type\": \"string\", \"description\": \"文档URL或base64\"}, \"extract_options\": {\"type\": \"object\", \"description\": \"提取选项\"}}}', '{\"type\": \"object\", \"properties\": {\"text\": {\"type\": \"string\"}, \"pages\": {\"type\": \"integer\"}, \"metadata\": {\"type\": \"object\"}}}', NULL, '{}', 'sync', 30, 3, 'active', NULL, NULL, '2026-04-06 22:11:39', '2026-04-06 22:11:39');
INSERT INTO `ea_skill_config` VALUES (14, 1, 'code_review', '代码审查', '自动审查代码质量，检查潜在问题和改进建议', 'INTERNAL', 'development', '🔍', '1.0.0', 'System', '[\"code-analysis\", \"quality-check\", \"suggestion\"]', '{\"type\": \"object\", \"required\": [\"code\"], \"properties\": {\"code\": {\"type\": \"string\", \"description\": \"要审查的代码\"}, \"language\": {\"type\": \"string\", \"description\": \"编程语言\"}}}', '{\"type\": \"object\", \"properties\": {\"score\": {\"type\": \"number\"}, \"issues\": {\"type\": \"array\"}, \"suggestions\": {\"type\": \"array\"}}}', NULL, '{\"timeout\": 60}', 'sync', 30, 3, 'active', NULL, NULL, '2026-04-06 23:00:26', '2026-04-06 23:00:26');

-- ----------------------------
-- Table structure for ea_skill_relation
-- ----------------------------
DROP TABLE IF EXISTS `ea_skill_relation`;
CREATE TABLE `ea_skill_relation`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `agent_id` bigint(0) NOT NULL COMMENT 'Agent ID',
  `skill_config_id` bigint(0) NOT NULL COMMENT 'Skill配置ID',
  `binding_config` json NULL COMMENT '绑定配置（JSON格式，可覆盖默认参数）',
  `sort_order` int(0) NULL DEFAULT 0 COMMENT '排序顺序',
  `is_active` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用：1=启用，0=禁用',
  `created_at` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_at` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_agent_skill`(`agent_id`, `skill_config_id`) USING BTREE,
  INDEX `idx_agent_id`(`agent_id`) USING BTREE,
  INDEX `idx_skill_config_id`(`skill_config_id`) USING BTREE,
  INDEX `idx_is_active`(`is_active`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Skill关系表（Agent绑定）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ea_skill_relation
-- ----------------------------
INSERT INTO `ea_skill_relation` VALUES (1, 8, 1, NULL, 0, 1, '2026-04-06 23:21:29', '2026-04-06 23:21:29');
INSERT INTO `ea_skill_relation` VALUES (2, 8, 2, NULL, 0, 1, '2026-04-06 23:21:41', '2026-04-06 23:21:41');

-- ----------------------------
-- Table structure for ea_tool_config
-- ----------------------------
DROP TABLE IF EXISTS `ea_tool_config`;
CREATE TABLE `ea_tool_config`  (
  `id` bigint(0) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(0) NULL DEFAULT NULL COMMENT 'agentId',
  `tool_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工具类型 (SQL, HTTP, MCP, GRPC等)',
  `tool_instance_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '工具实例ID（可选，用于区分同一类型的多个实例）',
  `tool_instance_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '工具实例名称',
  `tool_instance_desc` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '工具实例描述',
  `tool_value` json NULL COMMENT '默认值（JSON格式存储）',
  `input_template` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '入参模板',
  `out_template` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '出参模板',
  `is_required` tinyint(1) NULL DEFAULT 1 COMMENT '是否必需 (1=是, 0=否)',
  `sort_order` int(0) NULL DEFAULT 0 COMMENT '排序顺序',
  `is_active` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用 (1=启用, 0=禁用)',
  `extra_config` json NULL COMMENT '额外配置信息（JSON格式，用于存储工具特定的配置）',
  `created_at` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `updated_at` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 51 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '工具通用模板配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ea_tool_config
-- ----------------------------
INSERT INTO `ea_tool_config` VALUES (1, 0, 'SQL', NULL, 'mysql-query', '查询sql', '{\"sql\": \"SELECT * FROM ea_agent\", \"host\": \"localhost\", \"port\": \"3301\", \"dialect\": \"mysql\", \"maxRows\": 1000, \"timeout\": 30, \"database\": \"easy-agent\", \"password\": \"123456\", \"username\": \"root\"}', '[{\"name\":\"id\",\"type\":\"int\",\"description\":\"用户ID\",\"required\":true,\"defaultValue\":\"1\"}]', '[{\"name\":\"result\",\"type\":\"object\",\"description\":\"查询结果\",\"required\":true}]', 0, NULL, 1, NULL, '2025-12-28 22:23:45', '2026-04-16 23:09:48');
INSERT INTO `ea_tool_config` VALUES (2, 0, 'HTTP', NULL, 'queryPreciousMetalsPrice', '查询黄金白银等价格', '{\"url\": \"http://localhost:8080/example/queryPreciousMetalsPrice\", \"method\": \"GET\", \"headers\": [{\"key\": \"h1\", \"value\": \"123\", \"description\": \"\"}], \"requestParams\": [{\"key\": \"type\", \"value\": \"gold\", \"description\": \"黄金\"}], \"toolInstanceDesc\": \"查询黄金白银等价格\"}', '[{\"name\":\"type\",\"type\":\"string\",\"description\":\"如：gold，gold/黄金，silver/白银\",\"required\":true,\"defaultValue\":\"\",\"referenceValue\":\"$.requestParams.type\"}]', '[{\"name\":\"result\",\"type\":\"object\",\"description\":\"响应结果\",\"required\":true}]', 0, NULL, 1, NULL, '2025-12-28 22:23:38', '2026-04-16 23:11:23');
INSERT INTO `ea_tool_config` VALUES (21, 1, 'SQL', NULL, 'mysql-query', '查询sql', '{\"sql\": \"SELECT * FROM ea_agent\", \"host\": \"localhost\", \"port\": \"3301\", \"dialect\": \"mysql\", \"maxRows\": 1000, \"timeout\": 30, \"database\": \"easy-agent\", \"password\": \"123456\", \"username\": \"root\"}', '[{\"name\":\"id\",\"type\":\"int\",\"description\":\"用户ID\",\"required\":true,\"defaultValue\":\"1\"}]', '[{\"name\":\"result\",\"type\":\"object\",\"description\":\"查询结果\",\"required\":true}]', 0, NULL, 1, NULL, '2025-12-28 22:23:45', '2026-04-17 00:23:07');
INSERT INTO `ea_tool_config` VALUES (31, 1, 'HTTP', NULL, 'queryPreciousMetalsPriceByDate', '查询黄金白银等价格', '{\"url\": \"http://localhost:8080/example/queryPreciousMetalsPriceByDate\", \"method\": \"GET\", \"headers\": [{\"key\": \"h1\", \"value\": \"123\", \"description\": \"\"}], \"requestParams\": [{\"key\": \"type\", \"value\": \"gold\", \"description\": \"黄金\"}, {\"key\": \"date\", \"value\": \"2025-12-12 18:34:12\", \"description\": \"时间\"}], \"toolInstanceDesc\": \"查询黄金白银等价格\"}', '[{\"name\":\"type\",\"type\":\"string\",\"description\":\"如：gold，gold/黄金，silver/白银\",\"required\":true,\"defaultValue\":\"\",\"referenceValue\":\"$.requestParams.type\"},{\"name\":\"date\",\"type\":\"string\",\"description\":\"如：2025-12-12 18:34:12\",\"required\":true,\"defaultValue\":\"\",\"referenceValue\":\"$.requestParams.date\"}]', '[{\"name\":\"result\",\"type\":\"object\",\"description\":\"响应结果\",\"required\":true}]', 0, NULL, 1, NULL, '2025-12-28 22:23:38', '2026-04-17 00:21:36');
INSERT INTO `ea_tool_config` VALUES (42, 7, 'HTTP', NULL, '查询当前时间', '此时间查询工具不需要任何入参，默认给{}，然后直接请求即可。', '{\"url\": \"http://localhost:8080/example/getCurrentDate\", \"method\": \"GET\", \"headers\": [], \"requestParams\": [], \"toolInstanceDesc\": \"此时间查询工具不需要任何入参，默认给{}，然后直接请求即可。\"}', '[{\"name\":\"\",\"type\":\"\",\"description\":\"\",\"required\":false,\"defaultValue\":\"\",\"referenceValue\":\"\"}]', '[{\"name\":\"result\",\"type\":\"object\",\"description\":\"响应结果\",\"required\":true}]', 0, 0, 1, NULL, '2026-03-08 00:32:22', '2026-03-13 23:59:14');
INSERT INTO `ea_tool_config` VALUES (43, 3, 'HTTP', NULL, 'HTTP请求', '此时间查询工具不需要任何入参，默认给{}，然后直接请求即可。', '{\"url\": \"http://localhost:8080/example/getCurrentDate\", \"method\": \"GET\", \"headers\": [], \"requestParams\": [], \"toolInstanceDesc\": \"此时间查询工具不需要任何入参，默认给{}，然后直接请求即可。\"}', '[{\"name\":\"\",\"type\":\"\",\"description\":\"\",\"required\":false,\"defaultValue\":\"\",\"referenceValue\":\"\"}]', '[{\"name\":\"result\",\"type\":\"object\",\"description\":\"响应结果\",\"required\":true}]', 0, 0, 1, NULL, '2026-03-08 00:32:22', '2026-04-04 23:27:16');
INSERT INTO `ea_tool_config` VALUES (44, 3, 'SQL', NULL, '查询mysql 官方-demo', '查询sql', '{\"sql\": \"SELECT * FROM ea_agent\", \"host\": \"localhost\", \"port\": \"3301\", \"dialect\": \"mysql\", \"maxRows\": 1000, \"timeout\": 30, \"database\": \"easy-agent\", \"password\": \"123456\", \"username\": \"root\"}', '[{\"name\":\"id\",\"type\":\"int\",\"description\":\"用户ID\",\"required\":true,\"defaultValue\":\"1\"}]', '[{\"name\":\"result\",\"type\":\"object\",\"description\":\"查询结果\",\"required\":true}]', 0, 0, 1, NULL, '2025-12-28 22:23:45', '2026-03-07 22:35:58');
INSERT INTO `ea_tool_config` VALUES (45, 3, 'SQL', NULL, '查询mysql 官方-demo', '查询sql', '{\"sql\": \"SELECT * FROM ea_agent\", \"host\": \"localhost\", \"port\": \"3301\", \"dialect\": \"mysql\", \"maxRows\": 1000, \"timeout\": 30, \"database\": \"easy-agent\", \"password\": \"123456\", \"username\": \"root\"}', '[{\"name\":\"id\",\"type\":\"int\",\"description\":\"用户ID\",\"required\":true,\"defaultValue\":\"1\"}]', '[{\"name\":\"result\",\"type\":\"object\",\"description\":\"查询结果\",\"required\":true}]', 0, 0, 1, NULL, '2025-12-28 22:23:45', '2026-03-07 22:35:58');
INSERT INTO `ea_tool_config` VALUES (46, 7, 'SQL', NULL, '查询mysql 官方-demo', '查询sql', '{\"sql\": \"SELECT * FROM ea_agent\", \"host\": \"localhost\", \"port\": \"3301\", \"dialect\": \"mysql\", \"maxRows\": 1000, \"timeout\": 30, \"database\": \"easy-agent\", \"password\": \"123456\", \"username\": \"root\"}', '[{\"name\":\"id\",\"type\":\"int\",\"description\":\"用户ID\",\"required\":true,\"defaultValue\":\"1\"}]', '[{\"name\":\"result\",\"type\":\"object\",\"description\":\"查询结果\",\"required\":true}]', 0, 0, 1, NULL, '2025-12-28 22:23:45', '2026-03-07 22:35:58');
INSERT INTO `ea_tool_config` VALUES (47, 7, 'HTTP', NULL, '查询http 官方-demo', '查询黄金白银等价格', '{\"url\": \"http://localhost:8080/example/queryPreciousMetalsPrice\", \"method\": \"GET\", \"headers\": [{\"key\": \"h1\", \"value\": \"123\", \"description\": \"\"}], \"requestParams\": [{\"key\": \"type\", \"value\": \"gold\", \"description\": \"黄金\"}], \"toolInstanceDesc\": \"查询黄金白银等价格\"}', '[{\"name\":\"type\",\"type\":\"string\",\"description\":\"如：gold，gold/黄金，silver/白银\",\"required\":true,\"defaultValue\":\"\",\"referenceValue\":\"$.requestParams.type\"}]', '[{\"name\":\"result\",\"type\":\"object\",\"description\":\"响应结果\",\"required\":true}]', 0, 0, 1, NULL, '2025-12-28 22:23:38', '2026-03-08 21:30:42');
INSERT INTO `ea_tool_config` VALUES (49, 1, 'HTTP', NULL, 'getCurrentDate', '此时间查询工具不需要任何入参，默认给{}，然后直接请求即可。', '{\"url\": \"http://localhost:8080/example/getCurrentDate\", \"method\": \"GET\", \"headers\": [], \"requestParams\": [], \"toolInstanceDesc\": \"此时间查询工具不需要任何入参，默认给{}，然后直接请求即可。\"}', '[{\"name\":\"\",\"type\":\"\",\"description\":\"\",\"required\":false,\"defaultValue\":\"\",\"referenceValue\":\"\"}]', '[{\"name\":\"result\",\"type\":\"object\",\"description\":\"响应结果\",\"required\":true}]', 0, 0, 1, NULL, '2026-03-08 00:32:22', '2026-04-17 00:22:48');
INSERT INTO `ea_tool_config` VALUES (50, 1, 'HTTP', NULL, 'getCurrentDate', '此时间查询工具不需要任何入参，默认给{}，然后直接请求即可。', '{\"url\": \"http://localhost:8080/example/getCurrentDate\", \"method\": \"GET\", \"headers\": [], \"requestParams\": [], \"toolInstanceDesc\": \"此时间查询工具不需要任何入参，默认给{}，然后直接请求即可。\"}', '[{\"name\":\"\",\"type\":\"\",\"description\":\"\",\"required\":false,\"defaultValue\":\"\",\"referenceValue\":\"\"}]', '[{\"name\":\"result\",\"type\":\"object\",\"description\":\"响应结果\",\"required\":true}]', 0, 0, 1, NULL, '2026-03-08 00:32:22', '2026-04-17 00:22:54');

-- ----------------------------
-- Table structure for ea_tool_relation
-- ----------------------------
DROP TABLE IF EXISTS `ea_tool_relation`;
CREATE TABLE `ea_tool_relation`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tool_config_id` bigint(0) NULL DEFAULT NULL COMMENT '工具id',
  `agent_id` bigint(0) NULL DEFAULT NULL COMMENT 'agentId',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `creator` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'creator',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_tool_config_id`(`tool_config_id`) USING BTREE,
  INDEX `idx_create_time`(`create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工具关系表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ea_tool_relation
-- ----------------------------
INSERT INTO `ea_tool_relation` VALUES (4, 2, 1, '2026-03-22 01:12:02', '2026-03-22 01:12:02', 'system');
INSERT INTO `ea_tool_relation` VALUES (6, 2, 3, '2026-04-02 22:28:15', '2026-04-02 22:28:15', 'system');
INSERT INTO `ea_tool_relation` VALUES (7, 2, 8, '2026-04-16 22:37:03', '2026-04-16 22:37:03', 'system');
INSERT INTO `ea_tool_relation` VALUES (8, 2, 10, '2026-04-18 22:40:59', '2026-04-18 22:40:59', 'system');

SET FOREIGN_KEY_CHECKS = 1;
