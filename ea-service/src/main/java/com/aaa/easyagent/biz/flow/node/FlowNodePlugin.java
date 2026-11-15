package com.aaa.easyagent.biz.flow.node;

import com.aaa.easyagent.core.domain.flow.FlowNodeDefinition;
import com.aaa.easyagent.core.domain.flow.FlowValidationResult;
import com.aaa.easyagent.core.domain.flow.NodeMetadata;

public interface FlowNodePlugin {
    NodeMetadata metadata();

    /**
     * 根据节点配置补全运行期端口。静态节点无需实现；Router 等动态端口节点可在这里
     * 把配置转换为可校验、可执行的端口定义。
     */
    default void prepare(FlowNodeDefinition node) {
    }

    default void validate(FlowNodeDefinition node, FlowValidationResult result) {
    }

    NodeExecutionResult execute(NodeExecutionContext context);
}
