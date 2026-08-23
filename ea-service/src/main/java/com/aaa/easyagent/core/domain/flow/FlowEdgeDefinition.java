package com.aaa.easyagent.core.domain.flow;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Accessors(chain = true)
public class FlowEdgeDefinition {
    private String id;
    private EdgeKind kind;
    private PortRef source;
    private PortRef target;
    private Map<String, Object> config = new LinkedHashMap<>();
}
