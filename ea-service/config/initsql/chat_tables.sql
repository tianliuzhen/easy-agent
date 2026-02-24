-- EasyAgent 聊天对话记录表结构
-- 创建时间: 2026-02-10
-- 作者: EasyAgent系统

-- 如果表已存在，先删除（仅用于开发环境）
-- DROP TABLE IF EXISTS ea_chat_message;
-- DROP TABLE IF EXISTS ea_chat_conversation;

-- ============================================
-- 表1: ea_chat_conversation (聊天会话表)
-- 存储每个聊天会话的元数据信息
-- ============================================
CREATE TABLE IF NOT EXISTS ea_chat_conversation (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    title VARCHAR(255) NOT NULL DEFAULT '' COMMENT '会话标题（取第一次问题的前50个字符）',
    agent_id BIGINT NOT NULL COMMENT '关联的Agent ID',
    user_id VARCHAR(100) DEFAULT NULL COMMENT '用户ID（预留字段，未来扩展用）',
    message_count INT NOT NULL DEFAULT 0 COMMENT '消息总数',
    last_message_time TIMESTAMP NULL DEFAULT NULL COMMENT '最后消息时间',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '会话状态：active-活跃, archived-已归档, deleted-已删除',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_agent_id_created_at (agent_id, created_at),
    INDEX idx_status_updated_at (status, updated_at),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天会话表';

-- ============================================
-- 表2: ea_chat_message (聊天消息表)
-- 存储每条具体的聊天消息
-- ============================================
CREATE TABLE IF NOT EXISTS ea_chat_message (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    conversation_id BIGINT NOT NULL COMMENT '会话ID',
    message_type VARCHAR(20) NOT NULL COMMENT '消息类型：user_question-用户提问, ai_answer-AI回答, system_thinking-系统思考, system_tool_call-系统工具调用',
    content LONGTEXT COMMENT '消息内容',
    thinking_log LONGTEXT COMMENT '思考过程日志（单独存储）',
    tool_calls JSON COMMENT '工具调用信息（JSON格式）',
    model_used VARCHAR(100) DEFAULT NULL COMMENT '使用的模型',
    tokens_used INT DEFAULT NULL COMMENT '消耗的token数',
    response_time INT DEFAULT NULL COMMENT '响应时间（毫秒）',
    sequence INT NOT NULL DEFAULT 1 COMMENT '消息序号（用于排序，从1开始）',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_conversation_id_sequence (conversation_id, sequence),
    INDEX idx_conversation_id_created_at (conversation_id, created_at),
    INDEX idx_message_type (message_type),
    INDEX idx_created_at (created_at),
    CONSTRAINT fk_chat_message_conversation FOREIGN KEY (conversation_id)
        REFERENCES ea_chat_conversation(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息表';

-- ============================================
-- 外键关系说明
-- ============================================
-- 1. ea_chat_conversation.agent_id 关联 ea_agent.id
-- 2. ea_chat_message.conversation_id 关联 ea_chat_conversation.id

-- ============================================
-- 触发器：自动更新会话统计信息
-- ============================================

-- 触发器1：插入消息时更新会话统计
DELIMITER //
CREATE TRIGGER after_chat_message_insert
AFTER INSERT ON ea_chat_message
FOR EACH ROW
BEGIN
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
END//
DELIMITER ;

-- 触发器2：删除消息时更新会话统计
DELIMITER //
CREATE TRIGGER after_chat_message_delete
AFTER DELETE ON ea_chat_message
FOR EACH ROW
BEGIN
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
END//
DELIMITER ;

-- ============================================
-- 示例数据（用于测试）
-- ============================================

-- 示例会话1
INSERT INTO ea_chat_conversation (title, agent_id, user_id, message_count, status)
VALUES ('如何配置Agent的工具调用？', 1, 'user_001', 4, 'active');

-- 示例消息1-1（用户提问）
INSERT INTO ea_chat_message (conversation_id, message_type, content, sequence, created_at)
VALUES (1, 'user_question', '请问如何配置Agent的工具调用功能？', 1, NOW() - INTERVAL 30 MINUTE);

-- 示例消息1-2（AI思考过程）
INSERT INTO ea_chat_message (conversation_id, message_type, content, thinking_log, sequence, created_at)
VALUES (1, 'system_thinking', '', '用户询问Agent工具调用配置。需要检查当前Agent的配置，查看是否有可用的工具，然后提供配置步骤。', 2, NOW() - INTERVAL 29 MINUTE);

-- 示例消息1-3（AI回答）
INSERT INTO ea_chat_message (conversation_id, message_type, content, model_used, tokens_used, response_time, sequence, created_at)
VALUES (1, 'ai_answer', '配置Agent工具调用需要以下步骤：1. 在Agent管理页面选择目标Agent；2. 进入工具配置选项卡；3. 添加或编辑工具配置；4. 保存并测试工具调用。', 'gpt-4', 150, 1200, 3, NOW() - INTERVAL 28 MINUTE);

-- 示例消息1-4（用户追问）
INSERT INTO ea_chat_message (conversation_id, message_type, content, sequence, created_at)
VALUES (1, 'user_question', '具体支持哪些类型的工具？', 4, NOW() - INTERVAL 25 MINUTE);

-- 示例会话2
INSERT INTO ea_chat_conversation (title, agent_id, user_id, message_count, status)
VALUES ('API调用失败问题排查', 2, 'user_002', 3, 'active');

-- 示例消息2-1
INSERT INTO ea_chat_message (conversation_id, message_type, content, sequence, created_at)
VALUES (2, 'user_question', '我的API调用一直失败，错误码是500，请问怎么排查？', 1, NOW() - INTERVAL 15 MINUTE);

-- ============================================
-- 常用查询示例
-- ============================================

-- 查询某个Agent的所有会话（按最后消息时间倒序）
-- SELECT c.*
-- FROM ea_chat_conversation c
-- WHERE c.agent_id = 1 AND c.status = 'active'
-- ORDER BY c.last_message_time DESC;

-- 查询某个会话的所有消息（按序号正序）
-- SELECT m.*
-- FROM ea_chat_message m
-- WHERE m.conversation_id = 1
-- ORDER BY m.sequence ASC;

-- 查询包含工具调用的消息
-- SELECT m.*
-- FROM ea_chat_message m
-- WHERE m.message_type = 'system_tool_call'
-- AND JSON_LENGTH(m.tool_calls) > 0;

-- 统计每个Agent的会话数量和消息数量
-- SELECT
--     c.agent_id,
--     COUNT(DISTINCT c.id) as conversation_count,
--     SUM(c.message_count) as total_messages
-- FROM ea_chat_conversation c
-- WHERE c.status = 'active'
-- GROUP BY c.agent_id;

-- ============================================
-- 表结构变更记录
-- ============================================
-- 2026-02-10: 初始创建
-- 表1: ea_chat_conversation - 聊天会话表
-- 表2: ea_chat_message - 聊天消息表
