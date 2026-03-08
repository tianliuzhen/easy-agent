package com.aaa.easyagent.core.service.impl;

import com.aaa.easyagent.common.util.BeanConvertUtil;
import com.aaa.easyagent.common.util.FunFiledHelper;
import com.aaa.easyagent.core.domain.DO.EaChatConversationDO;
import com.aaa.easyagent.core.domain.DO.EaChatMessageDO;
import com.aaa.easyagent.core.domain.request.ChatConversationReq;
import com.aaa.easyagent.core.domain.request.ChatMessageReq;
import com.aaa.easyagent.core.domain.result.ChatConversationResult;
import com.aaa.easyagent.core.domain.result.ChatMessageResult;
import com.aaa.easyagent.core.mapper.EaChatConversationDAO;
import com.aaa.easyagent.core.mapper.EaChatMessageDAO;
import com.aaa.easyagent.core.service.ChatRecordService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 聊天记录管理服务实现类
 *
 * @author EasyAgent系统
 * @version 1.0 ChatRecordServiceImpl.java  2026/2/10
 */
@Service
public class ChatRecordServiceImpl implements ChatRecordService {

    @Resource
    private EaChatConversationDAO eaChatConversationDAO;

    @Resource
    private EaChatMessageDAO eaChatMessageDAO;

    // ==================== 会话管理 ====================

    @Override
    @Transactional
    public Long createConversation(ChatConversationReq req) {
        // 设置默认值
        req.setDefaults();

        // 验证参数
        if (!req.isValid()) {
            throw new IllegalArgumentException("创建会话参数无效");
        }

        // 设置创建时间和更新时间
        Date now = new Date();
        req.setCreatedAt(now);
        req.setUpdatedAt(now);

        // 插入数据库
        int result = eaChatConversationDAO.insertSelective(req);
        if (result > 0) {
            return req.getId();
        }
        return null;
    }

    @Override
    public int updateConversation(ChatConversationReq req) {
        if (req.getId() == null) {
            throw new IllegalArgumentException("更新会话需要ID");
        }

        // 设置更新时间
        req.setUpdatedAt(new Date());

        return eaChatConversationDAO.updateByPrimaryKeySelective(req);
    }

    @Override
    public int deleteConversation(Long conversationId) {
        // 软删除：更新状态为deleted
        ChatConversationReq req = new ChatConversationReq();
        req.setId(conversationId);
        req.setStatus("deleted");
        req.setUpdatedAt(new Date());

        return updateConversation(req);
    }

    @Override
    public int archiveConversation(Long conversationId) {
        // 归档：更新状态为archived
        ChatConversationReq req = new ChatConversationReq();
        req.setId(conversationId);
        req.setStatus("archived");
        req.setUpdatedAt(new Date());

        return updateConversation(req);
    }

    @Override
    public ChatConversationResult getConversationById(Long conversationId) {
        EaChatConversationDO conversationDO = eaChatConversationDAO.selectByPrimaryKey(conversationId);
        if (conversationDO == null) {
            return null;
        }

        return BeanConvertUtil.beanTo(conversationDO, ChatConversationResult.class);
    }

    @Override
    public List<ChatConversationResult> listConversationsByAgentId(Long agentId, String status) {
        Example example = new Example(EaChatConversationDO.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("agentId", agentId);

        if (status != null && !status.isEmpty()) {
            criteria.andEqualTo("status", status);
        }

        // 按最后消息时间倒序排列
        example.orderBy("lastMessageTime").desc();

        List<EaChatConversationDO> conversationDOs = eaChatConversationDAO.selectByExample(example);
        return BeanConvertUtil.beanTo(conversationDOs, ChatConversationResult.class);
    }

    @Override
    public List<ChatConversationResult> listConversationsByUserId(String userId, Long agentId, String status) {
        Example example = new Example(EaChatConversationDO.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo(FunFiledHelper.getFieldName(EaChatConversationDO::getUserId), userId);
        criteria.andEqualTo(FunFiledHelper.getFieldName(EaChatConversationDO::getAgentId), agentId);

        if (status != null && !status.isEmpty()) {
            criteria.andEqualTo(FunFiledHelper.getFieldName(EaChatConversationDO::getStatus), status);
        }

        // 按最后消息时间倒序排列
        example.orderBy("id").desc();

        List<EaChatConversationDO> conversationDOs = eaChatConversationDAO.selectByExample(example);
        return BeanConvertUtil.beanTo(conversationDOs, ChatConversationResult.class);
    }

    // ==================== 消息管理 ====================

    @Override
    @Transactional
    public Long saveMessage(ChatMessageReq req) {
        // 设置默认值
        req.setDefaults();

        // 验证参数
        if (!req.isValid()) {
            throw new IllegalArgumentException("保存消息参数无效");
        }

        // 设置创建时间
        req.setCreatedAt(new Date());

        // 插入数据库
        int result = eaChatMessageDAO.insertSelective(req);
        if (result > 0) {
            return req.getId();
        }
        return null;
    }

    @Override
    @Transactional
    public int saveMessages(List<ChatMessageReq> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (ChatMessageReq message : messages) {
            Long messageId = saveMessage(message);
            if (messageId != null) {
                count++;
            }
        }
        return count;
    }

    @Override
    public ChatMessageResult getMessageById(Long messageId) {
        EaChatMessageDO messageDO = eaChatMessageDAO.selectByPrimaryKey(messageId);
        if (messageDO == null) {
            return null;
        }

        return BeanConvertUtil.beanTo(messageDO, ChatMessageResult.class);
    }

    @Override
    public List<ChatMessageResult> listMessagesByConversationId(Long conversationId) {
        Example example = new Example(EaChatMessageDO.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("conversationId", conversationId);

        // 按消息序号正序排列
        example.orderBy("sequence").asc();

        List<EaChatMessageDO> messageDOs = eaChatMessageDAO.selectByExample(example);
        return BeanConvertUtil.beanTo(messageDOs, ChatMessageResult.class);
    }

    @Override
    public List<ChatMessageResult> listUserQuestionsByConversationId(Long conversationId) {
        Example example = new Example(EaChatMessageDO.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("conversationId", conversationId);
        criteria.andEqualTo("messageType", ChatMessageReq.TYPE_USER_QUESTION);

        // 按消息序号正序排列
        example.orderBy("sequence").asc();

        List<EaChatMessageDO> messageDOs = eaChatMessageDAO.selectByExample(example);
        return BeanConvertUtil.beanTo(messageDOs, ChatMessageResult.class);
    }

    @Override
    public List<ChatMessageResult> listAiAnswersByConversationId(Long conversationId) {
        Example example = new Example(EaChatMessageDO.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("conversationId", conversationId);
        criteria.andEqualTo("messageType", ChatMessageReq.TYPE_AI_ANSWER);

        // 按消息序号正序排列
        example.orderBy("sequence").asc();

        List<EaChatMessageDO> messageDOs = eaChatMessageDAO.selectByExample(example);
        return BeanConvertUtil.beanTo(messageDOs, ChatMessageResult.class);
    }

    @Override
    public int countMessagesByConversationId(Long conversationId) {
        Example example = new Example(EaChatMessageDO.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("conversationId", conversationId);

        return eaChatMessageDAO.selectCountByExample(example);
    }

    @Override
    @Transactional
    public int deleteMessagesByConversationId(Long conversationId) {
        Example example = new Example(EaChatMessageDO.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("conversationId", conversationId);

        return eaChatMessageDAO.deleteByExample(example);
    }

    // ==================== 业务方法 ====================

    @Override
    @Transactional
    public Long startNewConversation(Long agentId, String userId, String firstQuestion) {
        // 创建会话
        ChatConversationReq conversationReq = new ChatConversationReq();
        conversationReq.setAgentId(agentId);
        conversationReq.setUserId(userId);
        conversationReq.setTitle(conversationReq.generateTitleIfEmpty(firstQuestion));

        Long conversationId = createConversation(conversationReq);

        if (conversationId != null && firstQuestion != null && !firstQuestion.isEmpty()) {
            // 保存第一条消息
            ChatMessageReq messageReq = ChatMessageReq.createUserQuestion(
                    conversationId, firstQuestion, 1
            );
            saveMessage(messageReq);
        }

        return conversationId;
    }

    @Override
    @Transactional
    public List<Long> saveChatInteraction(Long conversationId, String userQuestion, String aiAnswer,
                                          String thinkingLog, String toolCalls, String modelUsed,
                                          Integer tokensUsed, Integer responseTime) {
        List<Long> messageIds = new ArrayList<>();

        // 获取当前消息序号
        int currentCount = countMessagesByConversationId(conversationId);
        int nextSequence = currentCount + 1;

        // 保存用户提问
        if (userQuestion != null && !userQuestion.isEmpty()) {
            ChatMessageReq userMessage = ChatMessageReq.createUserQuestion(
                    conversationId, userQuestion, nextSequence
            );
            Long userMessageId = saveMessage(userMessage);
            if (userMessageId != null) {
                messageIds.add(userMessageId);
                nextSequence++;
            }
        }

        // 保存思考过程（如果有）
        if (thinkingLog != null && !thinkingLog.isEmpty()) {
            ChatMessageReq thinkingMessage = ChatMessageReq.createSystemThinking(
                    conversationId, thinkingLog, nextSequence
            );
            Long thinkingMessageId = saveMessage(thinkingMessage);
            if (thinkingMessageId != null) {
                messageIds.add(thinkingMessageId);
                nextSequence++;
            }
        }

        // 保存工具调用（如果有）
        if (toolCalls != null && !toolCalls.isEmpty()) {
            ChatMessageReq toolCallMessage = ChatMessageReq.createSystemToolCall(
                    conversationId, toolCalls, nextSequence
            );
            Long toolCallMessageId = saveMessage(toolCallMessage);
            if (toolCallMessageId != null) {
                messageIds.add(toolCallMessageId);
                nextSequence++;
            }
        }

        // 保存AI回答
        if (aiAnswer != null && !aiAnswer.isEmpty()) {
            ChatMessageReq aiMessage = ChatMessageReq.createAiAnswer(
                    conversationId, aiAnswer, nextSequence,
                    modelUsed, tokensUsed, responseTime
            );
            Long aiMessageId = saveMessage(aiMessage);
            if (aiMessageId != null) {
                messageIds.add(aiMessageId);
            }
        }

        return messageIds;
    }

    @Override
    public List<ChatMessageResult> getFullChatHistory(Long conversationId) {
        return listMessagesByConversationId(conversationId);
    }

    @Override
    public String exportChatHistory(Long conversationId) {
        List<ChatMessageResult> messages = getFullChatHistory(conversationId);
        if (messages == null || messages.isEmpty()) {
            return "暂无聊天记录";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== 聊天记录导出 ===\n");
        sb.append("会话ID: ").append(conversationId).append("\n");
        sb.append("导出时间: ").append(new Date()).append("\n");
        sb.append("消息数量: ").append(messages.size()).append("\n\n");

        for (ChatMessageResult message : messages) {
            sb.append("[").append(message.getFormattedTime()).append("] ");
            sb.append(message.getSenderName()).append(": ");

            if (message.isSystemMessage() && message.hasThinkingLog()) {
                sb.append("[思考过程] ").append(message.getThinkingLogSummary());
            } else if (message.isSystemMessage() && message.hasToolCalls()) {
                sb.append("[工具调用] ").append(message.getToolCallCount()).append("个工具");
            } else {
                sb.append(message.getContent());
            }

            // 添加性能指标
            String metrics = message.getPerformanceMetrics();
            if (!metrics.isEmpty()) {
                sb.append(" (").append(metrics).append(")");
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    @Override
    @Transactional
    public int cleanupExpiredRecords(int days) {
        // 计算过期时间
        Date expiredDate = new Date(System.currentTimeMillis() - (long) days * 24 * 60 * 60 * 1000);

        // 1. 删除过期的消息
        Example messageExample = new Example(EaChatMessageDO.class);
        Example.Criteria messageCriteria = messageExample.createCriteria();
        messageCriteria.andLessThan("createdAt", expiredDate);
        int deletedMessages = eaChatMessageDAO.deleteByExample(messageExample);

        // 2. 删除没有消息的会话
        Example conversationExample = new Example(EaChatConversationDO.class);
        Example.Criteria conversationCriteria = conversationExample.createCriteria();
        conversationCriteria.andEqualTo("messageCount", 0);
        conversationCriteria.andLessThan("createdAt", expiredDate);
        int deletedConversations = eaChatConversationDAO.deleteByExample(conversationExample);

        return deletedMessages + deletedConversations;
    }
}
