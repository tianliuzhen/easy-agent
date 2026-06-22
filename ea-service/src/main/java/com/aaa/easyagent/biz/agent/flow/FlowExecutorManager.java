package com.aaa.easyagent.biz.agent.flow;

import com.aaa.easyagent.common.config.exception.AgentException;
import com.aaa.easyagent.core.domain.enums.FlowStrategyEnum;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 按 {@link FlowStrategyEnum} 分发到具体 {@link FlowExecutor}。
 * <p>
 * Spring 注入所有 {@link FlowExecutor} 实现，构建策略 → 执行器映射，沿用项目「按枚举注册执行器」模式。
 *
 * @author liuzhen.tian
 */
@Component
public class FlowExecutorManager {

    private final Map<FlowStrategyEnum, FlowExecutor> executorMap = new EnumMap<>(FlowStrategyEnum.class);

    public FlowExecutorManager(List<FlowExecutor> executors) {
        for (FlowExecutor executor : executors) {
            executorMap.put(executor.strategy(), executor);
        }
    }

    public String exec(FlowContext ctx, String question) {
        FlowExecutor executor = executorMap.get(ctx.getStrategy());
        if (executor == null) {
            throw new AgentException("不支持的编排策略：" + ctx.getStrategy());
        }
        return executor.exec(ctx, question);
    }
}
