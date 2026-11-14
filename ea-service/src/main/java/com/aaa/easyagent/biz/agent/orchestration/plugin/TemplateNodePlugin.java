package com.aaa.easyagent.biz.agent.orchestration.plugin;

import com.aaa.easyagent.biz.agent.orchestration.*;
import com.aaa.easyagent.core.domain.flow.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TemplateNodePlugin implements FlowNodePlugin {
    private final FlowTemplateRenderer renderer;

    @Override
    public NodeMetadata metadata() {
        return NodeMetadataSupport.metadata(FlowNodeTypes.TEMPLATE, "数据", "模板转换", "把多个输入渲染为文本", "FileTextOutlined",
                List.of(
                        NodeMetadataSupport.port("control", "执行", PortKind.CONTROL, ValueType.ANY, true, false),
                        NodeMetadataSupport.port("data", "模板变量", PortKind.DATA, ValueType.ANY, false, true)),
                List.of(
                        NodeMetadataSupport.port("control", "继续", PortKind.CONTROL, ValueType.ANY, false, true),
                        NodeMetadataSupport.port("result", "文本", PortKind.DATA, ValueType.STRING, false, true)),
                Map.of("template", NodeMetadataSupport.stringField("模板", "使用 {{ data }} 引用输入变量")), List.of("template"));
    }

    @Override
    public void validate(FlowNodeDefinition node, FlowValidationResult result) {
        if (String.valueOf(node.getConfig().getOrDefault("template", "")).isBlank()) {
            result.addError("NODE", node.getId(), "TEMPLATE_EMPTY", "模板转换节点必须配置模板");
        }
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        String template = String.valueOf(context.getNode().getConfig().get("template"));
        return NodeExecutionResult.of("result", renderer.render(template, context.getInputs()));
    }
}
