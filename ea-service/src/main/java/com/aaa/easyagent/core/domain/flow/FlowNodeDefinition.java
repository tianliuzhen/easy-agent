package com.aaa.easyagent.core.domain.flow;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
public class FlowNodeDefinition {
    private String id;
    private String type;
    private String version = "1";
    private String name;
    private NodePosition position = new NodePosition();
    private Map<String, Object> config = new LinkedHashMap<>();
    private List<PortDefinition> inputPorts = new ArrayList<>();
    private List<PortDefinition> outputPorts = new ArrayList<>();
}
