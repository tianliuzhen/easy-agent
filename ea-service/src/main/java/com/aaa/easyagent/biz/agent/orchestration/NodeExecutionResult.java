package com.aaa.easyagent.biz.agent.orchestration;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Accessors(chain = true)
public class NodeExecutionResult {
    private Map<String, Object> outputs = new LinkedHashMap<>();
    private String routeKey;
    private long inputTokens;
    private long outputTokens;

    public static NodeExecutionResult of(String key, Object value) {
        NodeExecutionResult result = new NodeExecutionResult();
        result.getOutputs().put(key, value);
        return result;
    }

    public Object primaryOutput() {
        if (outputs.containsKey("result")) {
            return outputs.get("result");
        }
        return outputs.values().stream().findFirst().orElse(null);
    }
}
