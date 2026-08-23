package com.aaa.easyagent.biz.agent.orchestration.plugin;

import com.aaa.easyagent.biz.agent.orchestration.*;
import com.aaa.easyagent.core.domain.flow.*;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class StartNodePlugin implements FlowNodePlugin {
    @Override
    public NodeMetadata metadata() {
        return NodeMetadataSupport.metadata("START", "基础", "开始", "声明 Flow 输入", "PlayCircleOutlined",
                List.of(), List.of(
                        NodeMetadataSupport.port("control", "开始", PortKind.CONTROL, ValueType.ANY, false, true),
                        NodeMetadataSupport.port("input", "用户输入", PortKind.DATA, ValueType.ANY, false, true),
                        NodeMetadataSupport.port("image", "图片", PortKind.DATA, ValueType.IMAGE, false, true)),
                Map.of(), List.of());
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        Map<String, Object> outputs = new LinkedHashMap<>(context.getRunContext().getInput());
        Object input = outputs.getOrDefault("input", outputs.getOrDefault("question", ""));
        outputs.put("input", input);
        if (context.getRunContext().getImageBase64() != null) {
            outputs.put("image", context.getRunContext().getImageBase64());
        }
        return new NodeExecutionResult().setOutputs(outputs);
    }
}
