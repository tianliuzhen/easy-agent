package com.aaa.easyagent.biz.agent.flow;

import com.aaa.easyagent.core.domain.enums.FlowStrategyEnum;

/**
 * 多 Agent 编排执行器。每种 {@link FlowStrategyEnum} 对应一个实现，
 * 由 {@link FlowExecutorManager} 按策略自注册并分发。
 *
 * @author liuzhen.tian
 */
public interface FlowExecutor {

    /**
     * 该执行器负责的编排策略（自注册用）。
     */
    FlowStrategyEnum strategy();

    /**
     * 执行编排。
     *
     * @param ctx      编排运行期上下文
     * @param question 用户问题
     * @return 编排最终答案
     */
    String exec(FlowContext ctx, String question);
}
