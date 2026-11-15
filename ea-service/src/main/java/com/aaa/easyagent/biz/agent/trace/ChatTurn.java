package com.aaa.easyagent.biz.agent.trace;

/**
 * 一轮对话的持久化句柄。
 * <p>
 * 提问在对话开始时就落库（即使模型随后失败，提问也不丢），
 * 答案与执行轨迹在对话结束时回填到同一条消息上。
 *
 * @param conversationId 会话ID
 * @param messageId      本轮消息ID，答案与轨迹都挂在它下面
 * @param question       用户提问
 */
public record ChatTurn(Long conversationId, Long messageId, String question) {

    /** 句柄是否可用于回填：缺少任一标识都无法定位要更新的记录 */
    public boolean persistable() {
        return conversationId != null && messageId != null;
    }
}
