package com.aaa.easyagent.core.domain.flow;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class PortRef {
    private String nodeId;
    private String portKey;

    public static PortRef of(String nodeId, String portKey) {
        return new PortRef().setNodeId(nodeId).setPortKey(portKey);
    }
}
