package com.aaa.easyagent.biz.agent.context;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * 将自定义 FunctionCallback 适配为 Spring AI ToolCallback
 *
 * @author liuzhen.tian
 * @version 1.0 FunctionCallbackAdapter.java  2026/4/4
 */
public class FunctionCallbackAdapter implements ToolCallback {

    private final FunctionCallback functionCallback;
    private final ToolDefinition toolDefinition;

    public FunctionCallbackAdapter(FunctionCallback functionCallback) {
        this.functionCallback = functionCallback;
        this.toolDefinition = ToolDefinition.builder()
                .name(functionCallback.getName())
                .description(functionCallback.getDescription())
                .inputSchema(functionCallback.getInputTypeSchema())
                .build();
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return toolDefinition;
    }

    @Override
    public String call(String toolInput) {
        return functionCallback.call(toolInput);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        return functionCallback.call(toolInput);
    }
}
