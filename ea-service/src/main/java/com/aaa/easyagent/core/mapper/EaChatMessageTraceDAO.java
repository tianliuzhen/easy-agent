package com.aaa.easyagent.core.mapper;

import com.aaa.easyagent.core.domain.DO.EaChatMessageTraceDO;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

/**
 * 聊天执行轨迹 Mapper。
 * <p>
 * 复杂与批量 SQL 写在 resources/mapper/EaChatMessageTraceDAO.xml，
 * 单条 CRUD 复用 tk.mybatis 内置方法。
 */
public interface EaChatMessageTraceDAO extends Mapper<EaChatMessageTraceDO> {

    /**
     * 批量写入轨迹。
     * 一轮对话会产生几条到几十条轨迹，必须一次写入，不在循环里逐条 insert。
     *
     * @param traces 待写入的轨迹，调用方保证非空
     * @return 写入行数
     */
    int batchInsert(@Param("traces") List<EaChatMessageTraceDO> traces);

    /**
     * 按消息ID批量查询轨迹，按消息与顺序号升序。
     * 历史回放需要一次取回整个会话的轨迹，避免按消息逐条查库。
     *
     * @param messageIds 消息ID集合，调用方保证非空
     * @return 轨迹列表，按 message_id、seq 升序
     */
    List<EaChatMessageTraceDO> selectByMessageIds(@Param("messageIds") List<Long> messageIds);

    /**
     * 删除某个会话的全部轨迹，用于会话删除时级联清理。
     *
     * @param conversationId 会话ID
     * @return 删除行数
     */
    int deleteByConversationId(@Param("conversationId") Long conversationId);
}
