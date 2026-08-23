package com.aaa.easyagent.core.domain.request;

import com.aaa.easyagent.core.domain.flow.FlowDefinition;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class FlowDebugRunRequest {
    private FlowDefinition definition;
    private Map<String, Object> input = new LinkedHashMap<>();
    private String sessionId;
}
