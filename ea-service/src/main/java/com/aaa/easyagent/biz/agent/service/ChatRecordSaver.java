package com.aaa.easyagent.biz.agent.service;

import com.aaa.easyagent.biz.agent.data.AgentContext;
import com.aaa.easyagent.biz.agent.data.AgentFinish;
import com.aaa.easyagent.biz.agent.data.FunctionUseAction;
import com.aaa.easyagent.common.context.UserContextHolder;
import com.aaa.easyagent.common.util.JacksonUtil;
import com.aaa.easyagent.common.util.SpringContextUtil;
import com.aaa.easyagent.core.domain.enums.ChatContextTypeEnum;
import com.aaa.easyagent.core.domain.result.StartNewConversationResp;
import com.aaa.easyagent.core.service.ChatRecordService;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 聊天记录保存服务
 * 负责在Agent执行过程中保存聊天记录到数据库
 *
 * @author EasyAgent系统
 * @version 1.0 ChatRecordSaver.java  2026/2/10
 */
@Slf4j
@Getter
@Setter
@Service
public class ChatRecordSaver {

    private static ChatRecordService chatRecordService;

    // 聊天上下文
    public static final ThreadLocal<List<ChatContext>> messageContext = ThreadLocal.withInitial(ArrayList::new);

    // 存储当前会话的会话 ID
    public static final ThreadLocal<Long> currentConversationId = new ThreadLocal<>();
    public static final ThreadLocal<Long> currentMessageId = new ThreadLocal<>();
    // 存储当前会话的用户问题
    public static final ThreadLocal<String> currentUserQuestion = new ThreadLocal<>();

    @PostConstruct
    public void init() {
        chatRecordService = SpringContextUtil.getBean(ChatRecordService.class);
    }

    /**
     * 聊天上下文
     *
     * @param type
     * @param value
     * @param time
     */
    public record ChatContext(ChatContextTypeEnum type, String value, Date time) {
        // 额外构造函数：只传 type 和 value，time 自动使用当前时间
        public ChatContext(ChatContextTypeEnum type, String value) {
            this(type, value, new Date());
        }
    }

    /**
     * 开始新的聊天会话
     *
     * @param agentContext Agent上下文
     * @param userQuestion 用户提问
     * @return 会话ID
     */
    public static Long startNewConversation(AgentContext agentContext, String userQuestion) {
        try {
            // 清理线程本地变量
            clearThreadLocal();

            // 生成用户ID（如果没有用户体系，使用默认值） todo 暂时等于1
            String userId = UserContextHolder.getUserId();

            // 开始新的会话
            StartNewConversationResp startNewConversationResp = chatRecordService.startNewConversation(
                    agentContext.getAgentId(),
                    agentContext.getSessionId(),
                    userId,
                    userQuestion
            );

            // messageId
            currentMessageId.set(startNewConversationResp.messageId());
            // 会话topicId
            currentConversationId.set(startNewConversationResp.conversationId());
            // 用户问题
            currentUserQuestion.set(userQuestion);
            log.info("开始新的聊天会话，会话 ID: {}, Agent ID: {}, 用户提问：{}",
                    startNewConversationResp.messageId(), agentContext.getAgentId(), userQuestion);

            return startNewConversationResp.messageId();
        } catch (Exception e) {
            log.error("开始新会话失败", e);
            return null;
        }
    }

    /**
     * 添加思考过程日志
     *
     * @param thinkingLog 思考过程日志
     */
    public static void addThinking(String thinkingLog) {
        if (thinkingLog != null && !thinkingLog.trim().isEmpty()) {
            messageContext.get().add(new ChatContext(ChatContextTypeEnum.thinking, thinkingLog));
        }
    }

    /**
     * 添加工具调用信息
     *
     * @param toolCall 工具调用信息
     */
    public static void addToolCall(String toolCall) {
        if (toolCall != null && !toolCall.trim().isEmpty()) {
            messageContext.get().add(new ChatContext(ChatContextTypeEnum.tool, toolCall));
        }
    }

    /**
     * 添加data
     *
     * @param data 工具调用信息
     */
    public static void addData(String data) {
        if (data != null && !data.trim().isEmpty()) {
            messageContext.get().add(new ChatContext(ChatContextTypeEnum.data, data));
        }
    }

    public static void addFinalAnswer(String data) {
        if (data != null && !data.trim().isEmpty()) {
            messageContext.get().add(new ChatContext(ChatContextTypeEnum.finalAnswer, data));
        }
    }

    /**
     * 添加工具调用信息（从FunctionUseAction）
     *
     * @param functionUseAction 工具调用动作
     */
    public static void addToolCall(FunctionUseAction functionUseAction, String result) {
        String toolCall = String.format("工具名称: %s, 输入参数: %s, 输出参数: %s",
                functionUseAction.getAction(),
                functionUseAction.getActionInput(),
                result);
        if (functionUseAction != null) {
            addToolCall(toolCall);
        }
    }

    /**
     * 保存完整的聊天交互
     *
     * @param aiAnswer     AI回答
     * @param modelUsed    使用的模型
     * @param tokensUsed   消耗的token数
     * @param responseTime 响应时间（毫秒）
     * @return 保存的消息ID列表
     */
    public static List<Long> saveChatInteraction(String aiAnswer, String modelUsed,
                                                 Integer tokensUsed, BigDecimal responseTime) {
        try {
            Long conversationId = currentConversationId.get();
            Long messageId = currentMessageId.get();
            String userQuestion = currentUserQuestion.get();

            if (conversationId == null || userQuestion == null) {
                log.warn("无法保存聊天交互，会话 ID 或用户问题为空");
                return new ArrayList<>();
            }

            String messageContextJson = JacksonUtil.beanToStr(messageContext.get());

            // 保存聊天交互（一条记录包含问题和回答）
            List<Long> messageIds = chatRecordService.saveChatInteraction(
                    conversationId,
                    messageId,
                    userQuestion,
                    aiAnswer,
                    messageContextJson,
                    modelUsed,
                    tokensUsed,
                    responseTime
            );

            log.info("保存聊天交互成功，会话 ID: {}, 消息数量：{}", conversationId, messageIds.size());

            return messageIds;
        } catch (Exception e) {
            log.error("保存聊天交互失败", e);
            return new ArrayList<>();
        } finally {
            // 清理线程本地变量（保留会话 ID，因为可能还有后续交互）
            clearThreadLocal();
        }
    }

    /**
     * 保存Agent完成结果
     *
     * @param agentFinish  Agent完成结果
     * @param modelUsed    使用的模型
     * @param tokensUsed   消耗的token数
     * @param responseTime 响应时间（毫秒）
     * @return 保存的消息ID列表
     */
    public static List<Long> saveAgentFinish(AgentFinish agentFinish, String modelUsed,
                                             Integer tokensUsed, BigDecimal responseTime) {
        if (agentFinish == null) {
            log.warn("Agent完成结果为空，无法保存");
            return new ArrayList<>();
        }

        String aiAnswer = agentFinish.getResult();
        if (aiAnswer == null || aiAnswer.trim().isEmpty()) {
            aiAnswer = agentFinish.getLlmResponse();
        }

        return saveChatInteraction(aiAnswer, modelUsed, tokensUsed, responseTime);
    }

    /**
     * 获取当前会话 ID
     *
     * @return 当前会话 ID
     */
    public static Long getCurrentConversationId() {
        return currentConversationId.get();
    }

    /**
     * 获取当前用户问题
     *
     * @return 当前用户问题
     */
    public static String getCurrentUserQuestion() {
        return currentUserQuestion.get();
    }

    /**
     * 清理线程本地变量
     */
    public static void clearThreadLocal() {
        currentConversationId.remove();
        messageContext.get().clear();
    }

}
