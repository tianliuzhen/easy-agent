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

 Date: 10/03/2026 00:03:35
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
  `model_config` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模型配置:{}',
  `created_at` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'agent管理表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ea_agent
-- ----------------------------
INSERT INTO `ea_agent` VALUES (1, '需求意图分析识别', 'xu_qiu_yi_tu_fen_xi_shi_bie_', '🎯', '分析文档主题  Qwen/QwQ-32B', 'siliconflow', 'siliconflow', 'deepseek', 'ReAct', '{\"apiKey\":\"sk-ewykgprysbswuqagkpbuglgodbxshtucznttazynawykgsds\",\"baseUrl\":\"https://api.siliconflow.cn/\",\"modelVersion\":\"moonshotai/Kimi-K2-Thinking\",\"completionsPath\":\"\"}', '2025-12-28 22:40:45', '2026-03-07 23:18:35');
INSERT INTO `ea_agent` VALUES (3, '查询时间demo2', 'cha_xun_shi_jian_DEMO2', '🤖', '测试', 'siliconflow', 'siliconflow', NULL, 'ReAct', '{\"apiKey\":\"sk-ewykgprysbswuqagkpbuglgodbxshtucznttazynawykgsds\",\"baseUrl\":\"https://api.siliconflow.cn\",\"modelVersion\":\"deepseek-ai/DeepSeek-V3\",\"completionsPath\":\"\"}', '2026-03-07 23:30:07', '2026-03-08 21:28:41');
INSERT INTO `ea_agent` VALUES (7, '查询时间demo', 'cha_xun_shi_jian_DEMO', '🤖', '测试', 'deepseek', 'deepseek', NULL, 'ReAct', '{\"apiKey\":\"sk-f26c256e24e6423ebceafab78ffe6878\",\"baseUrl\":\"https://api.deepseek.com/\",\"modelVersion\":\"deepseek-chat\",\"completionsPath\":\"\"}', '2026-03-07 23:30:07', '2026-03-08 21:28:46');

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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '聊天会话表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ea_chat_conversation
-- ----------------------------
INSERT INTO `ea_chat_conversation` VALUES (1, '你好', '3bbc629a-2e9c-4aa0-9c7b-bbd61825be5b', 7, '1', 0, NULL, 'active', '2026-03-09 16:02:32', '2026-03-09 16:02:32');

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
  `response_time` int(0) NULL DEFAULT NULL COMMENT '响应时间（毫秒）',
  `sequence` int(0) NOT NULL DEFAULT 1 COMMENT '消息序号（用于排序，从1开始）',
  `created_at` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_conversation_id_sequence`(`conversation_id`, `sequence`) USING BTREE,
  INDEX `idx_conversation_id_created_at`(`conversation_id`, `created_at`) USING BTREE,
  INDEX `idx_created_at`(`created_at`) USING BTREE,
  CONSTRAINT `fk_chat_message_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `ea_chat_conversation` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 25 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '聊天消息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ea_chat_message
-- ----------------------------
INSERT INTO `ea_chat_message` VALUES (25, 1, '你好', '', '', '', '', 0, 0, 1, '2026-03-09 16:02:32');
INSERT INTO `ea_chat_message` VALUES (26, 1, '查询当前时间', '', '', '', '', 0, 0, 1, '2026-03-09 16:02:50');

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
-- Table structure for ea_knowledge_base
-- ----------------------------
DROP TABLE IF EXISTS `ea_knowledge_base`;
CREATE TABLE `ea_knowledge_base`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `agent_id` bigint(0) NULL DEFAULT NULL COMMENT 'agentId',
  `kb_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '知识库名称',
  `kb_desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '知识库描述',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件名',
  `file_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件类型（txt/pdf）',
  `file_size` bigint(0) NULL DEFAULT NULL COMMENT '文件大小（字节）',
  `doc_count` int(0) NULL DEFAULT NULL COMMENT '切分后的文档数量',
  `doc_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '文档分片ID列表，JSON数组格式，如：[\"doc_id_1\",\"doc_id_2\"]',
  `status` tinyint(0) NULL DEFAULT 1 COMMENT '状态：1-正常，0-已删除',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_kb_name`(`kb_name`) USING BTREE,
  INDEX `idx_create_time`(`create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识库管理表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ea_knowledge_base
-- ----------------------------
INSERT INTO `ea_knowledge_base` VALUES (1, 1, '我爱学AI', 'ai知识库', '1', 'txt', 252, 1, '[\"EZlRXbU52mJgHeCN_0\"]', 0, '2026-02-01 13:02:21', '2026-02-08 09:36:28');
INSERT INTO `ea_knowledge_base` VALUES (2, 1, '简历模板', '简历模板', '李博文简历.pdf', 'pdf', 302698, 2, '[\"6822b4e0-57c2-4c68-b07d-a57be8c66142_0\",\"738538c1-d85e-4748-aff4-619e270c68b4_0\"]', 0, '2026-02-01 13:03:40', '2026-02-08 09:36:30');
INSERT INTO `ea_knowledge_base` VALUES (3, 1, 'qoder文档', 'qoder文档', 'image.png', 'png', 84014, 1, '[\"b32b842d-75e9-494c-ba53-c68b8936962b_0\"]', 0, '2026-02-01 14:20:05', '2026-02-08 09:36:19');
INSERT INTO `ea_knowledge_base` VALUES (4, 1, '1', '1', 'image.png', 'png', 79475, 1, '[\"4dfb1384-1a2f-4588-a6aa-78b768520dce_0\"]', 0, '2026-02-01 14:42:24', '2026-02-07 22:07:19');
INSERT INTO `ea_knowledge_base` VALUES (5, 1, '155规划', '155规划', 'image.png', 'png', 129370, 1, '[\"ed6e4c64-23ad-4626-a064-b4dd0ba87e3d_0\"]', 0, '2026-02-02 14:28:28', '2026-02-08 09:36:31');
INSERT INTO `ea_knowledge_base` VALUES (6, 1, 'es使用说明', 'es使用说明', 'image.png', 'png', 26008, 1, '[\"a7c03969-ad41-4e50-9762-380200ec69a3_0\"]', 1, '2026-02-08 09:58:55', '2026-02-08 09:58:55');

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
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '模型平台配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ea_model_platform
-- ----------------------------
INSERT INTO `ea_model_platform` VALUES (1, 'deepseek', 'deepseek', 'https://www.deepseek.com/favicon.ico', 'https://api.deepseek.com', 'https://api.deepseek.com', '[\"deepseek-chat\", \"deepseek-reasoner\"]', 1, 1, '2026-03-07 21:42:27', '2026-03-08 13:43:32');
INSERT INTO `ea_model_platform` VALUES (2, 'siliconflow', '硅基流动', 'https://cloud.siliconflow.cn/favicon.ico', 'https://cloud.siliconflow.cn/v1', 'https://api.siliconflow.cn', '[\"Pro/Qwen/Qwen2.5-7B-Instruct\", \"deepseek-ai/DeepSeek-R1-Distill-Qwen-32B\", \"fnlp/MOSS-TTSD-v0.5\", \"Qwen/Qwen3-VL-235B-A22B-Thinking\", \"Qwen/Qwen3-VL-32B-Thinking\", \"Pro/THUDM/glm-4-9b-chat\", \"black-forest-labs/FLUX.1-schnell\", \"Qwen/Qwen2.5-Coder-32B-Instruct\", \"LoRA/Qwen/Qwen2.5-7B-Instruct\", \"RVC-Boss/GPT-SoVITS\", \"LoRA/Qwen/Qwen2.5-14B-Instruct\", \"Qwen/Qwen3-Reranker-8B\", \"Qwen/Qwen3-30B-A3B-Instruct-2507\", \"Qwen/Qwen3-Omni-30B-A3B-Instruct\", \"Qwen/Qwen2.5-7B-Instruct\", \"Pro/BAAI/bge-m3\", \"deepseek-ai/DeepSeek-R1-Distill-Qwen-7B\", \"THUDM/GLM-4-32B-0414\", \"THUDM/GLM-Z1-32B-0414\", \"Qwen/Qwen3-Reranker-0.6B\", \"moonshotai/Kimi-Dev-72B\", \"TeleAI/TeleSpeechASR\", \"Qwen/Qwen2.5-32B-Instruct\", \"MiniMaxAI/MiniMax-M1-80k\", \"Wan-AI/Wan2.2-I2V-A14B\", \"Qwen/Qwen3-Omni-30B-A3B-Thinking\", \"Qwen/Qwen3-VL-30B-A3B-Instruct\", \"MiniMaxAI/MiniMax-M2\", \"Pro/Qwen/Qwen2-7B-Instruct\", \"Pro/Qwen/Qwen2.5-Coder-7B-Instruct\", \"Qwen/Qwen2.5-72B-Instruct\", \"Qwen/Qwen2.5-Coder-7B-Instruct\", \"Pro/deepseek-ai/DeepSeek-R1-Distill-Qwen-7B\", \"Qwen/Qwen3-Embedding-0.6B\", \"Pro/THUDM/GLM-4.1V-9B-Thinking\", \"Qwen/Qwen3-Next-80B-A3B-Instruct\", \"deepseek-ai/DeepSeek-R1\", \"Qwen/Qwen3-VL-30B-A3B-Thinking\", \"LoRA/Qwen/Qwen2.5-72B-Instruct\", \"ascend-tribe/pangu-pro-moe\", \"stepfun-ai/step3\", \"zai-org/GLM-4.5V\", \"Qwen/Qwen-Image\", \"BAAI/bge-reranker-v2-m3\", \"THUDM/GLM-Z1-9B-0414\", \"Qwen/Qwen3-Embedding-4B\", \"baidu/ERNIE-4.5-VL-424B-A47B-Paddle\", \"Wan-AI/Wan2.2-T2V-A14B\", \"deepseek-ai/DeepSeek-V3\", \"Qwen/Qwen3-VL-8B-Instruct\", \"zai-org/GLM-4.6V\", \"deepseek-ai/DeepSeek-V2.5\", \"Qwen/Qwen3-32B\", \"baidu/ERNIE-4.5-21B-A3B-Paddle\", \"THUDM/GLM-4.1V-9B-Thinking\", \"zai-org/GLM-4.5\", \"zai-org/GLM-4.6\", \"internlm/internlm2_5-7b-chat\", \"LoRA/Qwen/Qwen2.5-32B-Instruct\", \"Qwen/QVQ-72B-Preview\", \"Qwen/Qwen3-30B-A3B-Thinking-2507\", \"Qwen/Qwen3-Coder-30B-A3B-Instruct\", \"Qwen/QwQ-32B\", \"baidu/ERNIE-4.5-300B-A47B\", \"ByteDance-Seed/Seed-OSS-36B-Instruct\", \"Kwaipilot/KAT-Dev\", \"FunAudioLLM/CosyVoice2-0.5B\", \"netease-youdao/bce-reranker-base_v1\", \"Qwen/Qwen2.5-72B-Instruct-128K\", \"Pro/black-forest-labs/FLUX.1-schnell\", \"Qwen/Qwen3-Coder-480B-A35B-Instruct\", \"zai-org/GLM-4.5-Air\", \"Qwen/Qwen-Image-Edit\", \"Qwen/Qwen3-VL-8B-Thinking\", \"Qwen/Qwen2.5-VL-72B-Instruct\", \"SeedLLM/Seed-Rice-7B\", \"deepseek-ai/DeepSeek-OCR\", \"Qwen/Qwen2-VL-72B-Instruct\", \"deepseek-ai/DeepSeek-R1-0528-Qwen3-8B\", \"deepseek-ai/DeepSeek-R1-Distill-Qwen-14B\", \"Tongyi-Zhiwen/QwenLong-L1-32B\", \"inclusionAI/Ling-mini-2.0\", \"Qwen/Qwen3-VL-235B-A22B-Instruct\", \"Qwen/Qwen3-Omni-30B-A3B-Captioner\", \"netease-youdao/bce-embedding-base_v1\", \"Pro/Qwen/Qwen2.5-VL-7B-Instruct\", \"Qwen/Qwen3-235B-A22B\", \"deepseek-ai/DeepSeek-V3.2\", \"deepseek-ai/deepseek-vl2\", \"Qwen/Qwen2.5-VL-32B-Instruct\", \"baidu/ERNIE-4.5-VL-28B-A3B-Paddle\", \"THUDM/glm-4-9b-chat\", \"Qwen/Qwen3-14B\", \"Qwen/Qwen3-Embedding-8B\", \"Qwen/Qwen3-Reranker-4B\", \"tencent/Hunyuan-MT-7B\", \"Qwen/Qwen2.5-14B-Instruct\", \"Pro/BAAI/bge-reranker-v2-m3\", \"Qwen/Qwen3-235B-A22B-Thinking-2507\", \"inclusionAI/Ling-flash-2.0\", \"moonshotai/Kimi-K2-Instruct-0905\", \"deepseek-ai/DeepSeek-V3.1-Terminus\", \"moonshotai/Kimi-K2-Thinking\", \"THUDM/GLM-Z1-Rumination-32B-0414\", \"Qwen/Qwen3-Next-80B-A3B-Thinking\", \"Qwen/Qwen-Image-Edit-2509\", \"IndexTeam/IndexTTS-2\", \"Qwen/Qwen3-VL-32B-Instruct\", \"Qwen/Qwen2-7B-Instruct\", \"black-forest-labs/FLUX.1-dev\", \"BAAI/bge-m3\", \"Kwai-Kolors/Kolors\", \"THUDM/GLM-4-9B-0414\", \"Qwen/Qwen3-30B-A3B\", \"tencent/Hunyuan-A13B-Instruct\", \"Qwen/Qwen3-235B-A22B-Instruct-2507\", \"inclusionAI/Ring-flash-2.0\", \"Qwen/Qwen3-8B\", \"FunAudioLLM/SenseVoiceSmall\"]', 1, 2, '2026-03-07 21:42:27', '2026-03-07 23:18:13');
INSERT INTO `ea_model_platform` VALUES (3, 'openai', 'openai', 'https://openai.com/favicon.ico', 'https://openai.com/', 'https://api.openai.com/v1', '[\"gpt-3.5-turbo\", \"gpt-4\"]', 1, 3, '2026-03-07 21:42:27', '2026-03-07 21:42:27');
INSERT INTO `ea_model_platform` VALUES (4, 'ollama', 'ollama', 'https://ollama.com/public/ollama.png', 'https://ollama.com/', 'http://localhost:11434', '[\"llama2\", \"mistral\"]', 1, 4, '2026-03-07 21:42:27', '2026-03-07 21:42:27');

-- ----------------------------
-- Table structure for ea_tool_config
-- ----------------------------
DROP TABLE IF EXISTS `ea_tool_config`;
CREATE TABLE `ea_tool_config`  (
  `id` bigint(0) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `agent_id` bigint(0) NULL DEFAULT NULL COMMENT 'agentId',
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
) ENGINE = InnoDB AUTO_INCREMENT = 48 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '工具通用模板配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ea_tool_config
-- ----------------------------
INSERT INTO `ea_tool_config` VALUES (1, 0, 'SQL', NULL, '查询mysql 官方-demo', '查询sql', '{\"sql\": \"SELECT * FROM ea_agent\", \"host\": \"localhost\", \"port\": \"3301\", \"dialect\": \"mysql\", \"maxRows\": 1000, \"timeout\": 30, \"database\": \"easy-agent\", \"password\": \"123456\", \"username\": \"root\"}', '[{\"name\":\"id\",\"type\":\"int\",\"description\":\"用户ID\",\"required\":true,\"defaultValue\":\"1\"}]', '[{\"name\":\"result\",\"type\":\"object\",\"description\":\"查询结果\",\"required\":true}]', 0, NULL, 1, NULL, '2025-12-28 22:23:45', '2026-03-07 22:35:58');
INSERT INTO `ea_tool_config` VALUES (2, 0, 'HTTP', NULL, '查询http 官方-demo', '查询黄金白银等价格', '{\"url\": \"http://localhost:8080/example/queryPreciousMetalsPrice\", \"method\": \"GET\", \"headers\": [{\"key\": \"h1\", \"value\": \"123\", \"description\": \"\"}], \"requestParams\": [{\"key\": \"type\", \"value\": \"gold\", \"description\": \"黄金\"}], \"toolInstanceDesc\": \"查询黄金白银等价格\"}', '[{\"name\":\"type\",\"type\":\"string\",\"description\":\"如：gold，gold/黄金，silver/白银\",\"required\":true,\"defaultValue\":\"\",\"referenceValue\":\"$.requestParams.type\"}]', '[{\"name\":\"result\",\"type\":\"object\",\"description\":\"响应结果\",\"required\":true}]', 0, NULL, 1, NULL, '2025-12-28 22:23:38', '2026-03-08 21:30:42');
INSERT INTO `ea_tool_config` VALUES (21, 1, 'SQL', NULL, '查询mysql demo', '查询sql', '{\"sql\": \"SELECT * FROM ea_agent\", \"host\": \"localhost\", \"port\": \"3301\", \"dialect\": \"mysql\", \"maxRows\": 1000, \"timeout\": 30, \"database\": \"easy-agent\", \"password\": \"123456\", \"username\": \"root\"}', '[{\"name\":\"id\",\"type\":\"int\",\"description\":\"用户ID\",\"required\":true,\"defaultValue\":\"1\"}]', '[{\"name\":\"result\",\"type\":\"object\",\"description\":\"查询结果\",\"required\":true}]', 0, NULL, 1, NULL, '2025-12-28 22:23:45', '2026-01-08 21:46:47');
INSERT INTO `ea_tool_config` VALUES (31, 1, 'HTTP', NULL, '查询贵金属价格', '查询黄金白银等价格', '{\"url\": \"http://localhost:8080/example/queryPreciousMetalsPrice\", \"method\": \"GET\", \"headers\": [{\"key\": \"h1\", \"value\": \"123\", \"description\": \"\"}], \"requestParams\": [{\"key\": \"type\", \"value\": \"gold\", \"description\": \"黄金\"}], \"toolInstanceDesc\": \"查询黄金白银等价格\"}', '[{\"name\":\"type\",\"type\":\"string\",\"description\":\"如：gold，gold/黄金，silver/白银\",\"required\":true,\"defaultValue\":\"\",\"referenceValue\":\"$.requestParams.type\"}]', '[{\"name\":\"result\",\"type\":\"object\",\"description\":\"响应结果\",\"required\":true}]', 0, NULL, 1, NULL, '2025-12-28 22:23:38', '2026-03-05 23:53:31');
INSERT INTO `ea_tool_config` VALUES (42, 7, 'HTTP', NULL, 'HTTP请求', '此时间查询工具不需要任何入参，默认给{}，然后直接请求即可。', '{\"url\": \"http://localhost:8080/example/getCurrentDate\", \"method\": \"GET\", \"headers\": [], \"requestParams\": [], \"toolInstanceDesc\": \"此时间查询工具不需要任何入参，默认给{}，然后直接请求即可。\"}', '[{\"name\":\"\",\"type\":\"\",\"description\":\"\",\"required\":false,\"defaultValue\":\"\",\"referenceValue\":\"\"}]', '[{\"name\":\"result\",\"type\":\"object\",\"description\":\"响应结果\",\"required\":true}]', 0, 0, 1, NULL, '2026-03-08 00:32:22', '2026-03-08 01:04:00');
INSERT INTO `ea_tool_config` VALUES (43, 3, 'HTTP', NULL, 'HTTP请求', '此时间查询工具不需要任何入参，默认给{}，然后直接请求即可。', '{\"url\": \"http://localhost:8080/example/getCurrentDate\", \"method\": \"GET\", \"headers\": [], \"requestParams\": [], \"toolInstanceDesc\": \"此时间查询工具不需要任何入参，默认给{}，然后直接请求即可。\"}', '[{\"name\":\"\",\"type\":\"\",\"description\":\"\",\"required\":false,\"defaultValue\":\"\",\"referenceValue\":\"\"}]', '[{\"name\":\"result\",\"type\":\"object\",\"description\":\"响应结果\",\"required\":true}]', 0, 0, 1, NULL, '2026-03-08 00:32:22', '2026-03-08 01:01:29');
INSERT INTO `ea_tool_config` VALUES (44, 3, 'SQL', NULL, '查询mysql 官方-demo', '查询sql', '{\"sql\": \"SELECT * FROM ea_agent\", \"host\": \"localhost\", \"port\": \"3301\", \"dialect\": \"mysql\", \"maxRows\": 1000, \"timeout\": 30, \"database\": \"easy-agent\", \"password\": \"123456\", \"username\": \"root\"}', '[{\"name\":\"id\",\"type\":\"int\",\"description\":\"用户ID\",\"required\":true,\"defaultValue\":\"1\"}]', '[{\"name\":\"result\",\"type\":\"object\",\"description\":\"查询结果\",\"required\":true}]', 0, 0, 1, NULL, '2025-12-28 22:23:45', '2026-03-07 22:35:58');
INSERT INTO `ea_tool_config` VALUES (45, 3, 'SQL', NULL, '查询mysql 官方-demo', '查询sql', '{\"sql\": \"SELECT * FROM ea_agent\", \"host\": \"localhost\", \"port\": \"3301\", \"dialect\": \"mysql\", \"maxRows\": 1000, \"timeout\": 30, \"database\": \"easy-agent\", \"password\": \"123456\", \"username\": \"root\"}', '[{\"name\":\"id\",\"type\":\"int\",\"description\":\"用户ID\",\"required\":true,\"defaultValue\":\"1\"}]', '[{\"name\":\"result\",\"type\":\"object\",\"description\":\"查询结果\",\"required\":true}]', 0, 0, 1, NULL, '2025-12-28 22:23:45', '2026-03-07 22:35:58');
INSERT INTO `ea_tool_config` VALUES (46, 7, 'SQL', NULL, '查询mysql 官方-demo', '查询sql', '{\"sql\": \"SELECT * FROM ea_agent\", \"host\": \"localhost\", \"port\": \"3301\", \"dialect\": \"mysql\", \"maxRows\": 1000, \"timeout\": 30, \"database\": \"easy-agent\", \"password\": \"123456\", \"username\": \"root\"}', '[{\"name\":\"id\",\"type\":\"int\",\"description\":\"用户ID\",\"required\":true,\"defaultValue\":\"1\"}]', '[{\"name\":\"result\",\"type\":\"object\",\"description\":\"查询结果\",\"required\":true}]', 0, 0, 1, NULL, '2025-12-28 22:23:45', '2026-03-07 22:35:58');
INSERT INTO `ea_tool_config` VALUES (47, 7, 'HTTP', NULL, '查询http 官方-demo', '查询黄金白银等价格', '{\"url\": \"http://localhost:8080/example/queryPreciousMetalsPrice\", \"method\": \"GET\", \"headers\": [{\"key\": \"h1\", \"value\": \"123\", \"description\": \"\"}], \"requestParams\": [{\"key\": \"type\", \"value\": \"gold\", \"description\": \"黄金\"}], \"toolInstanceDesc\": \"查询黄金白银等价格\"}', '[{\"name\":\"type\",\"type\":\"string\",\"description\":\"如：gold，gold/黄金，silver/白银\",\"required\":true,\"defaultValue\":\"\",\"referenceValue\":\"$.requestParams.type\"}]', '[{\"name\":\"result\",\"type\":\"object\",\"description\":\"响应结果\",\"required\":true}]', 0, 0, 1, NULL, '2025-12-28 22:23:38', '2026-03-08 21:30:42');

SET FOREIGN_KEY_CHECKS = 1;
