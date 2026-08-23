package com.aaa.easyagent.biz.agent.orchestration;

public interface FlowEngine {
    FlowExecutionResult execute(CompiledFlowPlan plan, FlowRunContext context);
}
