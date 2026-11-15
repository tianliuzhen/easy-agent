package com.aaa.easyagent.biz.flow.engine;

import com.aaa.easyagent.biz.flow.compile.CompiledFlowPlan;
import com.aaa.easyagent.biz.flow.runtime.FlowExecutionResult;
import com.aaa.easyagent.biz.flow.runtime.FlowRunContext;

public interface FlowEngine {
    FlowExecutionResult execute(CompiledFlowPlan plan, FlowRunContext context);
}
