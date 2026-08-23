package com.aaa.easyagent.biz.agent.orchestration.plugin;

import com.aaa.easyagent.core.domain.flow.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class NodeMetadataSupport {
    private NodeMetadataSupport() {
    }

    static PortDefinition port(String key, String name, PortKind kind, ValueType type,
                               boolean required, boolean multiple) {
        return new PortDefinition().setKey(key).setName(name).setKind(kind).setValueType(type)
                .setRequired(required).setMultiple(multiple);
    }

    static NodeMetadata metadata(String type, String category, String name, String description, String icon,
                                 List<PortDefinition> inputs, List<PortDefinition> outputs,
                                 Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        return new NodeMetadata().setType(type).setCategory(category).setDisplayName(name)
                .setDescription(description).setIcon(icon).setInputPorts(inputs).setOutputPorts(outputs)
                .setConfigSchema(schema);
    }

    static Map<String, Object> stringField(String title, String description) {
        return Map.of("type", "string", "title", title, "description", description);
    }

    static Map<String, Object> numberField(String title, int minimum, int defaultValue) {
        return Map.of("type", "integer", "title", title, "minimum", minimum, "default", defaultValue);
    }
}
