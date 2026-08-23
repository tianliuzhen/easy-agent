package com.aaa.easyagent.biz.agent.orchestration.plugin;

import com.aaa.easyagent.biz.agent.orchestration.*;
import com.aaa.easyagent.core.domain.flow.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ForkNodePlugin implements FlowNodePlugin {
    @Override
    public NodeMetadata metadata() {
        return NodeMetadataSupport.metadata("FORK", "控制", "并行", "同时激活所有控制分支", "BranchesOutlined",
                List.of(NodeMetadataSupport.port("control", "执行", PortKind.CONTROL, ValueType.ANY, true, false)),
                List.of(NodeMetadataSupport.port("control", "并行分支", PortKind.CONTROL, ValueType.ANY, false, true)),
                Map.of(), List.of());
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        return new NodeExecutionResult();
    }
}
