package com.aaa.easyagent.core.domain.flow;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
public class FlowDefinition {
    public static final String CURRENT_SCHEMA_VERSION = "1.0";

    private String schemaVersion = CURRENT_SCHEMA_VERSION;
    private Long flowId;
    private Long revisionId;
    private String name;
    private List<FlowNodeDefinition> nodes = new ArrayList<>();
    private List<FlowEdgeDefinition> edges = new ArrayList<>();
}
