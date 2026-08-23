package com.aaa.easyagent.biz.agent.orchestration;

import com.aaa.easyagent.core.domain.flow.PortDefinition;
import com.aaa.easyagent.core.domain.flow.ValueType;
import org.springframework.stereotype.Component;

@Component
public class PortTypeSystem {
    public boolean compatible(PortDefinition source, PortDefinition target) {
        if (source == null || target == null) {
            return false;
        }
        ValueType from = source.getValueType();
        ValueType to = target.getValueType();
        return from == ValueType.ANY || to == ValueType.ANY || from == to
                || (from == ValueType.STRING && to == ValueType.MESSAGE)
                || (from == ValueType.MESSAGE && to == ValueType.STRING);
    }
}
