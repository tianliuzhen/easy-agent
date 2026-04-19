package com.aaa.easyagent.biz.agent.data;

import lombok.Data;

/**
 * @author liuzhen.tian
 * @version 1.0 FunctionUseAction.java  2025/4/19 21:54
 */
@Data
public class FunctionUseAction extends AgentOutput {
    private String callId;
    private String action;
    private String actionInput;


    public FunctionUseAction(String action, String actionInput) {
        this.action = action;
        this.actionInput = actionInput;
    }

    public FunctionUseAction(String callId, String action, String actionInput) {
        this.callId = callId;
        this.action = action;
        this.actionInput = actionInput;
    }
}
