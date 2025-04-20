package com.aaa.springai.agent.model;

import lombok.Data;

/**
 * @author liuzhen.tian
 * @version 1.0 AgentFinish.java  2025/4/19 21:46
 */
@Data
public class AgentFinish extends AgentOutput {
    private String result;

    private String llmResponse;
}
