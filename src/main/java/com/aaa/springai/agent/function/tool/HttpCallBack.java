package com.aaa.springai.agent.function.tool;

import com.aaa.springai.domain.model.ToolModel;
import com.aaa.springai.util.WebClientUtil;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 *
 * org.springframework.ai.tool.function.FunctionToolCallback
 *
 * @author liuzhen.tian
 * @version 1.0 HttpCallBack.java  2025/2/23 19:34
 */
public class HttpCallBack implements BaseCallback {
    private final ToolDefinition toolDefinition;
    private final ToolModel toolModel;

    public HttpCallBack(ToolDefinition toolDefinition, ToolModel toolModel) {
        this.toolDefinition = toolDefinition;
        this.toolModel = toolModel;
    }

    @Override
    public String getName() {
        return toolDefinition.name();
    }

    @Override
    public String getDescription() {
        return toolDefinition.description();
    }

    @Override
    public String getInputTypeSchema() {
        return toolDefinition.inputSchema();
    }

    @Override
    public String call(String functionInput) {

        String res = WebClientUtil.get("http://localhost:8080/example/getCurrentDate", String.class);

        return res;
    }

}
