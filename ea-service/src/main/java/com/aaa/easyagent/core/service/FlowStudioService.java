package com.aaa.easyagent.core.service;

import com.aaa.easyagent.core.domain.flow.FlowDefinition;
import com.aaa.easyagent.core.domain.flow.FlowValidationResult;
import com.aaa.easyagent.core.domain.flow.NodeMetadata;
import com.aaa.easyagent.core.domain.request.FlowCreateRequest;
import com.aaa.easyagent.core.domain.result.FlowDetailResult;

import java.util.List;

public interface FlowStudioService {
    List<FlowDetailResult> list();
    FlowDetailResult detail(Long flowId);
    Long create(FlowCreateRequest request);
    void delete(Long flowId);
    FlowDefinition draft(Long flowId);
    FlowDefinition published(Long flowId);
    FlowDefinition revision(Long flowId, Long revisionId);
    void saveDraft(Long flowId, FlowDefinition definition);
    FlowValidationResult validate(Long flowId, FlowDefinition definition);
    Long publish(Long flowId);
    List<NodeMetadata> nodeTypes();
}
