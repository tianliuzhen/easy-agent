package com.aaa.easyagent.biz.agent.orchestration;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Accessors(chain = true)
public class FlowExecutionResult {
    private String runId;
    private String status;
    private Object result;
    private Map<String, Object> state = new LinkedHashMap<>();
    private long inputTokens;
    private long outputTokens;
}
