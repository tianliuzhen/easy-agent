package com.aaa.easyagent.web.biz.chat;

import com.aaa.easyagent.core.domain.base.BaseResult;
import com.aaa.easyagent.core.domain.request.ChatConversationReq;
import com.aaa.easyagent.core.domain.request.ChatMessageReq;
import com.aaa.easyagent.core.domain.result.StartNewConversationResp;
import com.aaa.easyagent.core.service.ChatRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 聊天记录控制器
 *
 * @author EasyAgent系统
 * @version 1.0 ChatRecordController.java  2026/2/10
 */
@Slf4j
@RestController
@RequestMapping("chatRecord")
@RequiredArgsConstructor
public class ChatRecordController {

    private final ChatRecordService chatRecordService;

    // ==================== 会话管理接口 ====================

    /**
     * 创建新的聊天会话
     *
     * @param req 会话请求对象
     * @return 创建的会话ID
     */
    @PostMapping("/conversation/create")
    public BaseResult<Long> createConversation(@RequestBody ChatConversationReq req) {
        try {
            Long conversationId = chatRecordService.createConversation(req);
            return BaseResult.buildSuc(conversationId);
        } catch (Exception e) {
            log.error("创建会话失败", e);
            return BaseResult.buildFail("创建会话失败: " + e.getMessage());
        }
    }

    /**
     * 更新会话信息
     *
     * @param req 会话请求对象
     * @return 更新结果
     */
    @PostMapping("/conversation/update")
    public BaseResult<Integer> updateConversation(@RequestBody ChatConversationReq req) {
        try {
            int result = chatRecordService.updateConversation(req);
            return BaseResult.buildSuc(result);
        } catch (Exception e) {
            log.error("更新会话失败", e);
            return BaseResult.buildFail("更新会话失败: " + e.getMessage());
        }
    }

    /**
     * 删除会话（软删除）
     *
     * @param conversationId 会话ID
     * @return 删除结果
     */
    @PostMapping("/conversation/delete/{conversationId}")
    public BaseResult<Integer> deleteConversation(@PathVariable Long conversationId) {
        try {
            int result = chatRecordService.deleteConversation(conversationId);
            return BaseResult.buildSuc(result);
        } catch (Exception e) {
            log.error("删除会话失败", e);
            return BaseResult.buildFail("删除会话失败: " + e.getMessage());
        }
    }

    /**
     * 归档会话
     *
     * @param conversationId 会话ID
     * @return 归档结果
     */
    @PostMapping("/conversation/archive/{conversationId}")
    public BaseResult<Integer> archiveConversation(@PathVariable Long conversationId) {
        try {
            int result = chatRecordService.archiveConversation(conversationId);
            return BaseResult.buildSuc(result);
        } catch (Exception e) {
            log.error("归档会话失败", e);
            return BaseResult.buildFail("归档会话失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取会话信息
     *
     * @param conversationId 会话ID
     * @return 会话信息
     */
    @GetMapping("/conversation/{conversationId}")
    public BaseResult<Object> getConversationById(@PathVariable Long conversationId) {
        try {
            return BaseResult.buildSuc(chatRecordService.getConversationById(conversationId));
        } catch (Exception e) {
            log.error("获取会话失败", e);
            return BaseResult.buildFail("获取会话失败: " + e.getMessage());
        }
    }

    /**
     * 根据Agent ID查询会话列表
     *
     * @param agentId Agent ID
     * @param status  会话状态（可选）
     * @return 会话列表
     */
    @GetMapping("/conversation/listByAgent/{agentId}")
    public BaseResult<Object> listConversationsByAgentId(
            @PathVariable Long agentId,
            @RequestParam(required = false) String status) {
        try {
            return BaseResult.buildSuc(chatRecordService.listConversationsByAgentId(agentId, status));
        } catch (Exception e) {
            log.error("查询Agent会话列表失败", e);
            return BaseResult.buildFail("查询Agent会话列表失败: " + e.getMessage());
        }
    }

    /**
     * 查询用户的会话列表
     *
     * @param userId 用户ID
     * @param status 会话状态（可选）
     * @return 会话列表
     */
    @GetMapping("/conversation/listByUser/{userId}")
    public BaseResult listConversationsByUserId(
            @PathVariable String userId,
            @RequestParam(required = false) Long agentId,
            @RequestParam(required = false) String status) {
        try {
            return BaseResult.buildSuc(chatRecordService.listConversationsByUserId(userId, agentId, status));
        } catch (Exception e) {
            log.error("查询用户会话列表失败", e);
            return BaseResult.buildFail("查询用户会话列表失败: " + e.getMessage());
        }
    }

    // ==================== 消息管理接口 ====================

    /**
     * 保存聊天消息
     *
     * @param req 消息请求对象
     * @return 保存的消息ID
     */
    @PostMapping("/message/save")
    public BaseResult<Long> saveMessage(@RequestBody ChatMessageReq req) {
        try {
            Long messageId = chatRecordService.saveMessage(req);
            return BaseResult.buildSuc(messageId);
        } catch (Exception e) {
            log.error("保存消息失败", e);
            return BaseResult.buildFail("保存消息失败: " + e.getMessage());
        }
    }

    /**
     * 批量保存聊天消息
     *
     * @param messages 消息请求对象列表
     * @return 保存成功的消息数量
     */
    @PostMapping("/message/saveBatch")
    public BaseResult<Integer> saveMessages(@RequestBody List<ChatMessageReq> messages) {
        try {
            int count = chatRecordService.saveMessages(messages);
            return BaseResult.buildSuc(count);
        } catch (Exception e) {
            log.error("批量保存消息失败", e);
            return BaseResult.buildFail("批量保存消息失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取消息
     *
     * @param messageId 消息ID
     * @return 消息信息
     */
    @GetMapping("/message/{messageId}")
    public BaseResult<Object> getMessageById(@PathVariable Long messageId) {
        try {
            return BaseResult.buildSuc(chatRecordService.getMessageById(messageId));
        } catch (Exception e) {
            log.error("获取消息失败", e);
            return BaseResult.buildFail("获取消息失败: " + e.getMessage());
        }
    }

    /**
     * 根据会话ID查询消息列表
     *
     * @param conversationId 会话ID
     * @return 消息列表
     */
    @GetMapping("/message/listByConversation/{conversationId}")
    public BaseResult listMessagesByConversationId(@PathVariable Long conversationId) {
        try {
            return BaseResult.buildSuc(chatRecordService.listMessagesByConversationId(conversationId));
        } catch (Exception e) {
            log.error("查询会话消息列表失败", e);
            return BaseResult.buildFail("查询会话消息列表失败: " + e.getMessage());
        }
    }

    /**
     * 统计会话中的消息数量
     *
     * @param conversationId 会话ID
     * @return 消息数量
     */
    @GetMapping("/message/count/{conversationId}")
    public BaseResult<Integer> countMessagesByConversationId(@PathVariable Long conversationId) {
        try {
            int count = chatRecordService.countMessagesByConversationId(conversationId);
            return BaseResult.buildSuc(count);
        } catch (Exception e) {
            log.error("统计消息数量失败", e);
            return BaseResult.buildFail("统计消息数量失败: " + e.getMessage());
        }
    }

    // ==================== 业务接口 ====================

    /**
     * 开始新的聊天会话
     *
     * @param agentId       Agent ID
     * @param userId        用户ID
     * @param firstQuestion 第一个问题
     * @return 创建的会话ID
     */
    @PostMapping("/business/startNewConversation")
    public BaseResult startNewConversation(
            @RequestParam Long agentId,
            @RequestParam String userId,
            @RequestParam String sessionId,
            @RequestParam String firstQuestion) {
        try {
            StartNewConversationResp resp = chatRecordService.startNewConversation(agentId, sessionId, userId, firstQuestion);
            return BaseResult.buildSuc(resp);
        } catch (Exception e) {
            log.error("开始新会话失败", e);
            return BaseResult.buildFail("开始新会话失败: " + e.getMessage());
        }
    }


    /**
     * 获取会话的完整聊天记录
     *
     * @param conversationId 会话ID
     * @return 完整的聊天记录
     */
    @GetMapping("/business/fullChatHistory/{conversationId}")
    public BaseResult getFullChatHistory(@PathVariable Long conversationId) {
        try {
            return BaseResult.buildSuc(chatRecordService.getFullChatHistory(conversationId));
        } catch (Exception e) {
            log.error("获取完整聊天记录失败", e);
            return BaseResult.buildFail("获取完整聊天记录失败: " + e.getMessage());
        }
    }

    /**
     * 导出会话聊天记录
     *
     * @param conversationId 会话ID
     * @return 格式化后的聊天记录文本
     */
    @GetMapping("/business/exportChatHistory/{conversationId}")
    public BaseResult<String> exportChatHistory(@PathVariable Long conversationId) {
        try {
            String exportText = chatRecordService.exportChatHistory(conversationId);
            return BaseResult.buildSuc(exportText);
        } catch (Exception e) {
            log.error("导出聊天记录失败", e);
            return BaseResult.buildFail("导出聊天记录失败: " + e.getMessage());
        }
    }

    /**
     * 清理过期的聊天记录
     *
     * @param days 保留天数
     * @return 清理的记录数量
     */
    @PostMapping("/business/cleanupExpiredRecords")
    public BaseResult<Integer> cleanupExpiredRecords(@RequestParam int days) {
        try {
            int count = chatRecordService.cleanupExpiredRecords(days);
            return BaseResult.buildSuc(count);
        } catch (Exception e) {
            log.error("清理过期记录失败", e);
            return BaseResult.buildFail("清理过期记录失败: " + e.getMessage());
        }
    }

    /**
     * 获取会话中的用户提问消息
     *
     * @param conversationId 会话ID
     * @return 用户提问消息列表
     */
    @GetMapping("/business/userQuestions/{conversationId}")
    public BaseResult listUserQuestionsByConversationId(@PathVariable Long conversationId) {
        try {
            return BaseResult.buildSuc(chatRecordService.listUserQuestionsByConversationId(conversationId));
        } catch (Exception e) {
            log.error("获取用户提问失败", e);
            return BaseResult.buildFail("获取用户提问失败: " + e.getMessage());
        }
    }

    /**
     * 获取会话中的AI回答消息
     *
     * @param conversationId 会话ID
     * @return AI回答消息列表
     */
    @GetMapping("/business/aiAnswers/{conversationId}")
    public BaseResult listAiAnswersByConversationId(@PathVariable Long conversationId) {
        try {
            return BaseResult.buildSuc(chatRecordService.listAiAnswersByConversationId(conversationId));
        } catch (Exception e) {
            log.error("获取AI回答失败", e);
            return BaseResult.buildFail("获取AI回答失败: " + e.getMessage());
        }
    }
}
