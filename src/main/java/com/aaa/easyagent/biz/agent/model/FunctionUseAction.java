package com.aaa.easyagent.biz.agent.model;

import lombok.Data;

/**
 * @author liuzhen.tian
 * @version 1.0 FunctionUseAction.java  2025/4/19 21:54
 */
@Data
public class FunctionUseAction extends AgentOutput{
    private String action;
    private String actionInput;

    private String llmResponse;

    public FunctionUseAction(String action, String actionInput) {
        this.action = action;
        this.actionInput = actionInput;
    }
}
