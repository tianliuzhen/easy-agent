package com.aaa.easyagent.core.service.impl;

import com.aaa.easyagent.common.config.exception.AgentException;
import com.aaa.easyagent.common.util.BeanConvertUtil;
import com.aaa.easyagent.common.util.FunFiledHelper;
import com.aaa.easyagent.core.domain.DO.EaChatConversationDO;
import com.aaa.easyagent.core.domain.DO.EaChatMessageDO;
import com.aaa.easyagent.core.domain.request.ChatConversationReq;
import com.aaa.easyagent.core.domain.request.ChatMessageReq;
import com.aaa.easyagent.core.domain.result.ChatConversationResult;
import com.aaa.easyagent.core.domain.result.ChatMessageResult;
import com.aaa.easyagent.core.domain.result.StartNewConversationResp;
import com.aaa.easyagent.core.mapper.EaChatConversationDAO;
import com.aaa.easyagent.core.mapper.EaChatMessageDAO;
import com.aaa.easyagent.core.domain.DO.EaChatMessageTraceDO;
import com.aaa.easyagent.core.domain.result.ChatTraceResult;
import com.aaa.easyagent.core.mapper.EaChatMessageTraceDAO;
import com.aaa.easyagent.core.service.ChatRecordService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tk.mybatis.mapper.entity.Example;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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

    @Resource
    private EaChatMessageTraceDAO eaChatMessageTraceDAO;

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

        if (req.getId() != null) {
            EaChatConversationDO eaChatConversationDO = eaChatConversationDAO.selectByPrimaryKey(req.getId());
            if (eaChatConversationDO != null) {
                if (eaChatConversationDO.getFlowRevisionId() == null && req.getFlowRevisionId() != null) {
                    eaChatConversationDO.setFlowRevisionId(req.getFlowRevisionId());
                    eaChatConversationDO.setSessionId(req.getSessionId());
                    eaChatConversationDAO.updateByPrimaryKeySelective(eaChatConversationDO);
                }
                return eaChatConversationDO.getId();
            }
        }


        // 插入数据库
        int result = eaChatConversationDAO.insertSelective(req);
        if (result > 0) {
            return req.getId();
        }
        throw new IllegalArgumentException("创建会话失败");
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
    public List<ChatConversationResult> listConversationsByUserId(String userId, Long agentId, Long flowId, String status) {
        Example example = new Example(EaChatConversationDO.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo(FunFiledHelper.getFieldName(EaChatConversationDO::getUserId), userId);

        if (flowId != null) {
            // 编排会话：仅按 flow_id 过滤
            criteria.andEqualTo(FunFiledHelper.getFieldName(EaChatConversationDO::getFlowId), flowId);
        } else {
            // 单 Agent 会话：按 agent_id 过滤，并排除编排会话
            criteria.andEqualTo(FunFiledHelper.getFieldName(EaChatConversationDO::getAgentId), agentId);
            criteria.andIsNull(FunFiledHelper.getFieldName(EaChatConversationDO::getFlowId));
        }

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

        if (req.getId() != null) {
            // 插入数据库
            eaChatMessageDAO.updateByPrimaryKeySelective(req);
            return req.getId();
        } else {
            eaChatMessageDAO.insertSelective(req);
            return req.getId();
        }
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

        // 按消息序号正序排列
        example.orderBy("id").asc();

        List<EaChatMessageDO> messageDOs = eaChatMessageDAO.selectByExample(example);
        return BeanConvertUtil.beanTo(messageDOs, ChatMessageResult.class);
    }

    @Override
    public List<ChatMessageResult> listAiAnswersByConversationId(Long conversationId) {
        Example example = new Example(EaChatMessageDO.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("conversationId", conversationId);

        // 按消息序号正序排列
        example.orderBy("id").asc();

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
    public StartNewConversationResp startNewConversation(Long agentId, Long flowId, Long flowRevisionId,
                                                         String sessionId, String userId, String firstQuestion) {
        // 创建会话
        ChatConversationReq conversationReq = new ChatConversationReq();
        conversationReq.setAgentId(agentId);
        conversationReq.setFlowId(flowId);
        conversationReq.setFlowRevisionId(flowRevisionId);
        conversationReq.setSessionId(sessionId);
        conversationReq.setUserId(userId);
        conversationReq.setTitle(conversationReq.generateTitleIfEmpty(firstQuestion));
        conversationReq.setId(Long.valueOf(sessionId));
        Long conversationId = createConversation(conversationReq);

        if (conversationId == null || firstQuestion == null || firstQuestion.isEmpty()) {
            throw new AgentException("开启会话失败，会话ID或提问为空：sessionId=" + sessionId);
        }
        // 先落用户提问：答案与轨迹在本轮结束时回填到这条消息上，
        // 即使模型随后失败，提问也不会丢。序号按会话内已有消息数递增
        int nextSequence = countMessagesByConversationId(conversationId) + 1;
        Long messageId = saveMessage(ChatMessageReq.createUserQuestion(
                conversationId, firstQuestion, nextSequence));
        return new StartNewConversationResp(conversationId, messageId);
    }


    @Override
    @Transactional
    public void completeChatMessage(Long conversationId, Long messageId, String question, String answer,
                                    String modelUsed, long inputTokensUsed, long outputTokensUsed,
                                    BigDecimal responseTime) {
        // 1. 回填答案与本轮用量到提问所在的那条消息
        EaChatMessageDO message = new EaChatMessageDO()
                .setId(messageId)
                .setAnswer(answer)
                .setModelUsed(modelUsed)
                .setInputTokensUsed(inputTokensUsed)
                .setOutputTokensUsed(outputTokensUsed)
                .setResponseTime(responseTime);
        eaChatMessageDAO.updateByPrimaryKeySelective(message);

        // 2. 会话维度累加 token。用 SQL 自增而不是「查出来加一下再写回」，
        //    否则同一会话并发两轮对话时会丢更新
        eaChatConversationDAO.accumulateTokens(conversationId, inputTokensUsed, outputTokensUsed);

        // 3. 会话标题为空时用首个提问补全
        fillConversationTitleIfBlank(conversationId, question);
    }

    /** 标题为空才写入，避免每轮都把标题改成最新提问 */
    private void fillConversationTitleIfBlank(Long conversationId, String question) {
        if (question == null || question.isBlank()) {
            return;
        }
        EaChatConversationDO conversation = eaChatConversationDAO.selectByPrimaryKey(conversationId);
        if (conversation == null || StringUtils.hasText(conversation.getTitle())) {
            return;
        }
        String trimmed = question.trim();
        String title = trimmed.length() > 50 ? trimmed.substring(0, 47) + "..." : trimmed;
        eaChatConversationDAO.updateByPrimaryKeySelective(
                new EaChatConversationDO().setId(conversationId).setTitle(title).setUpdatedAt(new Date()));
    }

    /**
     * 获取会话完整记录，并把执行轨迹一次性挂到各条消息上。
     * <p>
     * 1. 查出会话的全部消息
     * 2. 用一次 IN 查询取回所有消息的轨迹，避免按消息逐条查库
     * 3. 按 messageId 分组挂载，轨迹内部已由 SQL 按 seq 升序
     */
    @Override
    public List<ChatMessageResult> getFullChatHistory(Long conversationId) {
        List<ChatMessageResult> messages = listMessagesByConversationId(conversationId);
        if (messages.isEmpty()) {
            return messages;
        }
        List<Long> messageIds = messages.stream().map(EaChatMessageDO::getId).filter(Objects::nonNull).toList();
        if (messageIds.isEmpty()) {
            return messages;
        }
        Map<Long, List<ChatTraceResult>> tracesByMessage = eaChatMessageTraceDAO.selectByMessageIds(messageIds)
                .stream()
                .collect(Collectors.groupingBy(EaChatMessageTraceDO::getMessageId,
                        LinkedHashMap::new,
                        Collectors.mapping(this::toTraceResult, Collectors.toList())));
        messages.forEach(message ->
                message.setTraces(tracesByMessage.getOrDefault(message.getId(), List.of())));
        return messages;
    }

    private ChatTraceResult toTraceResult(EaChatMessageTraceDO trace) {
        return new ChatTraceResult()
                .setSeq(trace.getSeq())
                .setTraceType(trace.getTraceType())
                .setContent(trace.getContent())
                .setToolName(trace.getToolName())
                .setToolInput(trace.getToolInput())
                .setToolOutput(trace.getToolOutput())
                .setAgentName(trace.getAgentName())
                .setFlowNodeId(trace.getFlowNodeId())
                .setDurationMs(trace.getDurationMs());
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

            // 优先显示回答
            String answer = message.getAnswer();
            if (answer != null && !answer.isEmpty()) {
                sb.append(answer);
            } else {
                // 其次显示问题
                String question = message.getQuestion();
                if (question != null && !question.isEmpty()) {
                    sb.append(question);
                }
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

    @Override
    public EaChatConversationDO getBySessionId(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        Example example = new Example(EaChatConversationDO.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("sessionId", sessionId);
        List<EaChatConversationDO> list = eaChatConversationDAO.selectByExample(example);
        if (!list.isEmpty()) return list.get(0);
        try {
            return eaChatConversationDAO.selectByPrimaryKey(Long.valueOf(sessionId));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
