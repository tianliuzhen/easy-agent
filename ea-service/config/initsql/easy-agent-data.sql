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

 Date: 06/03/2026 00:08:04
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
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'agent管理表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ea_chat_conversation
-- ----------------------------
DROP TABLE IF EXISTS `ea_chat_conversation`;
CREATE TABLE `ea_chat_conversation`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '会话标题（取第一次问题的前50个字符）',
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
) ENGINE = InnoDB AUTO_INCREMENT = 89 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '聊天会话表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ea_chat_message
-- ----------------------------
DROP TABLE IF EXISTS `ea_chat_message`;
CREATE TABLE `ea_chat_message`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `conversation_id` bigint(0) NOT NULL COMMENT '会话ID',
  `message_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息类型：user_question-用户提问, ai_answer-AI回答, system_thinking-系统思考, system_tool_call-系统工具调用',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '消息内容',
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
  INDEX `idx_message_type`(`message_type`) USING BTREE,
  INDEX `idx_created_at`(`created_at`) USING BTREE,
  CONSTRAINT `fk_chat_message_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `ea_chat_conversation` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 89 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '聊天消息表' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 42 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '工具通用模板配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Triggers structure for table ea_chat_message
-- ----------------------------
DROP TRIGGER IF EXISTS `after_chat_message_insert`;
delimiter ;;
CREATE TRIGGER `after_chat_message_insert` AFTER INSERT ON `ea_chat_message` FOR EACH ROW BEGIN
    -- 更新消息总数
    UPDATE ea_chat_conversation 
    SET message_count = message_count + 1,
        last_message_time = NEW.created_at,
        updated_at = NOW()
    WHERE id = NEW.conversation_id;
    
    -- 如果是第一条消息（用户提问），自动设置会话标题
    IF NEW.message_type = 'user_question' THEN
        UPDATE ea_chat_conversation 
        SET title = CASE 
            WHEN title = '' THEN 
                CASE 
                    WHEN LENGTH(NEW.content) > 50 THEN CONCAT(SUBSTRING(NEW.content, 1, 47), '...')
                    ELSE NEW.content
                END
            ELSE title
        END
        WHERE id = NEW.conversation_id;
    END IF;
END
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table ea_chat_message
-- ----------------------------
DROP TRIGGER IF EXISTS `after_chat_message_delete`;
delimiter ;;
CREATE TRIGGER `after_chat_message_delete` AFTER DELETE ON `ea_chat_message` FOR EACH ROW BEGIN
    -- 更新消息总数
    UPDATE ea_chat_conversation 
    SET message_count = GREATEST(0, message_count - 1),
        updated_at = NOW()
    WHERE id = OLD.conversation_id;
    
    -- 如果删除了最后一条消息，更新最后消息时间
    UPDATE ea_chat_conversation c
    SET last_message_time = (
        SELECT MAX(created_at) 
        FROM ea_chat_message 
        WHERE conversation_id = c.id
    )
    WHERE c.id = OLD.conversation_id;
END
;;
delimiter ;

SET FOREIGN_KEY_CHECKS = 1;
