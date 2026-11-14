package com.aaa.easyagent.biz.agent.orchestration.plugin;

import com.aaa.easyagent.biz.agent.orchestration.*;
import com.aaa.easyagent.core.domain.flow.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class JoinNodePlugin implements FlowNodePlugin {
    @Override
    public NodeMetadata metadata() {
        return NodeMetadataSupport.metadata(FlowNodeTypes.JOIN, "控制", "聚合", "等待已激活分支并聚合数据", "MergeCellsOutlined",
                List.of(
                        NodeMetadataSupport.port("control", "分支", PortKind.CONTROL, ValueType.ANY, true, true),
                        NodeMetadataSupport.port("items", "数据", PortKind.DATA, ValueType.ANY, false, true)),
                List.of(
                        NodeMetadataSupport.port("control", "继续", PortKind.CONTROL, ValueType.ANY, false, true),
                        NodeMetadataSupport.port("result", "聚合结果", PortKind.DATA, ValueType.JSON, false, true)),
                Map.of("mode", Map.of("type", "string", "title", "等待策略", "enum", List.of("ALL"), "default", "ALL")),
                List.of());
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        return NodeExecutionResult.of("result", context.getInputs());
    }
}
