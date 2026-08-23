package com.aaa.easyagent.core.domain.flow;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class NodePosition {
    private double x;
    private double y;
}
