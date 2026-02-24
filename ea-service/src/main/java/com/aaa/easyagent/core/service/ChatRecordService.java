package com.aaa.easyagent.core.service;

import com.aaa.easyagent.core.domain.DO.EaChatConversationDO;
import com.aaa.easyagent.core.domain.DO.EaChatMessageDO;
import com.aaa.easyagent.core.domain.request.ChatConversationReq;
import com.aaa.easyagent.core.domain.request.ChatMessageReq;
import com.aaa.easyagent.core.domain.result.ChatConversationResult;
import com.aaa.easyagent.core.domain.result.ChatMessageResult;

import java.util.List;

/**
 * 聊天记录管理服务接口
 *
 * @author EasyAgent系统
 * @version 1.0 ChatRecordService.java  2026/2/10
 */
public interface ChatRecordService {

    // ==================== 会话管理 ====================

    /**
     * 创建新的聊天会话
     *
     * @param req 会话请求对象
     * @return 创建的会话ID
     */
    Long createConversation(ChatConversationReq req);

    /**
     * 更新会话信息
     *
     * @param req 会话请求对象
     * @return 更新成功返回1，否则返回0
     */
    int updateConversation(ChatConversationReq req);

    /**
     * 删除会话（软删除）
     *
     * @param conversationId 会话ID
     * @return 删除成功返回1，否则返回0
     */
    int deleteConversation(Long conversationId);

    /**
     * 归档会话
     *
     * @param conversationId 会话ID
     * @return 归档成功返回1，否则返回0
     */
    int archiveConversation(Long conversationId);

    /**
     * 根据ID获取会话信息
     *
     * @param conversationId 会话ID
     * @return 会话结果对象
     */
    ChatConversationResult getConversationById(Long conversationId);

    /**
     * 根据Agent ID查询会话列表
     *
     * @param agentId Agent ID
     * @param status 会话状态（可选）
     * @return 会话结果列表
     */
    List<ChatConversationResult> listConversationsByAgentId(Long agentId, String status);

    /**
     * 查询用户的会话列表
     *
     * @param userId 用户ID
     * @param status 会话状态（可选）
     * @return 会话结果列表
     */
    List<ChatConversationResult> listConversationsByUserId(String userId, String status);

    // ==================== 消息管理 ====================

    /**
     * 保存聊天消息
     *
     * @param req 消息请求对象
     * @return 保存的消息ID
     */
    Long saveMessage(ChatMessageReq req);

    /**
     * 批量保存聊天消息
     *
     * @param messages 消息请求对象列表
     * @return 保存成功的消息数量
     */
    int saveMessages(List<ChatMessageReq> messages);

    /**
     * 根据ID获取消息
     *
     * @param messageId 消息ID
     * @return 消息结果对象
     */
    ChatMessageResult getMessageById(Long messageId);

    /**
     * 根据会话ID查询消息列表
     *
     * @param conversationId 会话ID
     * @return 消息结果列表（按sequence排序）
     */
    List<ChatMessageResult> listMessagesByConversationId(Long conversationId);

    /**
     * 查询会话中的用户提问消息
     *
     * @param conversationId 会话ID
     * @return 用户提问消息列表
     */
    List<ChatMessageResult> listUserQuestionsByConversationId(Long conversationId);

    /**
     * 查询会话中的AI回答消息
     *
     * @param conversationId 会话ID
     * @return AI回答消息列表
     */
    List<ChatMessageResult> listAiAnswersByConversationId(Long conversationId);

    /**
     * 统计会话中的消息数量
     *
     * @param conversationId 会话ID
     * @return 消息数量
     */
    int countMessagesByConversationId(Long conversationId);

    /**
     * 删除会话中的所有消息
     *
     * @param conversationId 会话ID
     * @return 删除的消息数量
     */
    int deleteMessagesByConversationId(Long conversationId);

    // ==================== 业务方法 ====================

    /**
     * 开始新的聊天会话（创建会话并保存第一条消息）
     *
     * @param agentId Agent ID
     * @param userId 用户ID
     * @param firstQuestion 第一个问题（用于生成标题）
     * @return 创建的会话ID
     */
    Long startNewConversation(Long agentId, String userId, String firstQuestion);

    /**
     * 保存完整的聊天交互（用户提问 + AI回答）
     *
     * @param conversationId 会话ID
     * @param userQuestion 用户提问
     * @param aiAnswer AI回答
     * @param thinkingLog 思考过程（可选）
     * @param toolCalls 工具调用信息（可选）
     * @param modelUsed 使用的模型
     * @param tokensUsed 消耗的token数
     * @param responseTime 响应时间（毫秒）
     * @return 保存的消息ID列表
     */
    List<Long> saveChatInteraction(Long conversationId, String userQuestion, String aiAnswer,
                                   String thinkingLog, String toolCalls, String modelUsed,
                                   Integer tokensUsed, Integer responseTime);

    /**
     * 获取会话的完整聊天记录
     *
     * @param conversationId 会话ID
     * @return 完整的聊天记录（包含所有消息）
     */
    List<ChatMessageResult> getFullChatHistory(Long conversationId);

    /**
     * 导出会话聊天记录
     *
     * @param conversationId 会话ID
     * @return 格式化后的聊天记录文本
     */
    String exportChatHistory(Long conversationId);

    /**
     * 清理过期的聊天记录
     *
     * @param days 保留天数
     * @return 清理的记录数量
     */
    int cleanupExpiredRecords(int days);
}
