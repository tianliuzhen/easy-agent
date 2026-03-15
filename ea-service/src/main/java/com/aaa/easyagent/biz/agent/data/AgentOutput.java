package com.aaa.easyagent.biz.agent.data;

import lombok.Data;

/**
 * @author liuzhen.tian
 * @version 1.0 AgentOutput.java  2025/4/19 21:45
 */
@Data
public class AgentOutput {

    /**
     * 思考内容
     */
    private String reasoningContent;

    /**
     * 模型结果
     */
    private String llmResponse;

}
