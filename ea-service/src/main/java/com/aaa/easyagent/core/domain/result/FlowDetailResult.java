package com.aaa.easyagent.core.domain.result;

import com.aaa.easyagent.core.domain.flow.FlowDefinition;
import lombok.Data;

import java.util.Date;

@Data
public class FlowDetailResult {
    private Long id;
    private String flowName;
    private String flowKey;
    private String flowDesc;
    private String avatar;
    private String welcomeMessage;
    private Long currentDraftId;
    private Long publishedRevisionId;
    private Integer publishedRevisionNo;
    private FlowDefinition draft;
    private Date createdAt;
    private Date updatedAt;
}
