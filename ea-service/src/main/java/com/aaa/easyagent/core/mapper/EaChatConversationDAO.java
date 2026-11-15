package com.aaa.easyagent.core.mapper;

import com.aaa.easyagent.core.domain.DO.EaChatConversationDO;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

/**
 * 会话 Mapper。
 * 复杂与并发敏感的 SQL 写在 resources/mapper/EaChatConversationDAO.xml，
 * 单条 CRUD 复用 tk.mybatis 内置方法。
 */
public interface EaChatConversationDAO extends Mapper<EaChatConversationDO> {

    /**
     * 累加会话维度的 token 用量。
     * 必须用 SQL 自增：先查出当前值、在内存相加、再写回，
     * 同一会话并发两轮对话时会互相覆盖导致统计偏小。
     *
     * @param conversationId 会话ID
     * @param inputTokens    本轮输入 token 增量
     * @param outputTokens   本轮输出 token 增量
     * @return 更新行数
     */
    int accumulateTokens(@Param("conversationId") Long conversationId,
                         @Param("inputTokens") long inputTokens,
                         @Param("outputTokens") long outputTokens);
}
