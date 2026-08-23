package com.aaa.easyagent.core.domain.flow;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class PortDefinition {
    private String key;
    private String name;
    private PortKind kind;
    private ValueType valueType = ValueType.ANY;
    private boolean required;
    private boolean multiple;
    private Object defaultValue;
    private String schema;
}
