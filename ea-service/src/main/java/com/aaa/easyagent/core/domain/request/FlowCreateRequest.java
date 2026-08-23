package com.aaa.easyagent.core.domain.request;

import lombok.Data;

@Data
public class FlowCreateRequest {
    private String flowName;
    private String flowKey;
    private String flowDesc;
    private String avatar;
    private String welcomeMessage;
}
