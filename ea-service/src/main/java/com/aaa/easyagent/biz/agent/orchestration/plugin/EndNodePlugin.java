package com.aaa.easyagent.biz.agent.orchestration.plugin;

import com.aaa.easyagent.biz.agent.orchestration.*;
import com.aaa.easyagent.core.domain.flow.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class EndNodePlugin implements FlowNodePlugin {
    @Override
    public NodeMetadata metadata() {
        return NodeMetadataSupport.metadata(FlowNodeTypes.END, "基础", "结束", "声明 Flow 最终输出", "StopOutlined",
                List.of(
                        NodeMetadataSupport.port("control", "执行", PortKind.CONTROL, ValueType.ANY, true, true),
                        NodeMetadataSupport.port("result", "最终结果", PortKind.DATA, ValueType.ANY, true, false)),
                List.of(NodeMetadataSupport.port("result", "最终结果", PortKind.DATA, ValueType.ANY, false, true)),
                Map.of(), List.of());
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        return NodeExecutionResult.of("result", context.getInputs().get("result"));
    }
}
