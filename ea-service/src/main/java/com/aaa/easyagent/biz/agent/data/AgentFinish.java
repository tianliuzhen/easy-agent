package com.aaa.easyagent.biz.agent.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author liuzhen.tian
 * @version 1.0 AgentFinish.java  2025/4/19 21:46
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentFinish extends AgentOutput {
    private String result;

    private String llmResponse;
}
