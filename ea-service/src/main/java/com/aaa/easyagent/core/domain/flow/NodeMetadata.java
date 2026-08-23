package com.aaa.easyagent.core.domain.flow;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
public class NodeMetadata {
    private String type;
    private String version = "1";
    private String category;
    private String displayName;
    private String description;
    private String icon;
    private List<PortDefinition> inputPorts = new ArrayList<>();
    private List<PortDefinition> outputPorts = new ArrayList<>();
    private Map<String, Object> configSchema = new LinkedHashMap<>();
    private Map<String, Object> uiSchema = new LinkedHashMap<>();
}
