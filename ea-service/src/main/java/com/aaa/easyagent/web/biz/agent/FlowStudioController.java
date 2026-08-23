package com.aaa.easyagent.web.biz.agent;

import com.aaa.easyagent.biz.agent.orchestration.FlowCompiler;
import com.aaa.easyagent.biz.agent.orchestration.FlowEngine;
import com.aaa.easyagent.biz.agent.orchestration.FlowRunContext;
import com.aaa.easyagent.biz.agent.orchestration.FlowExecutionResult;
import com.aaa.easyagent.core.domain.base.BaseResult;
import com.aaa.easyagent.core.domain.flow.FlowDefinition;
import com.aaa.easyagent.core.domain.request.FlowCreateRequest;
import com.aaa.easyagent.core.domain.request.FlowDebugRunRequest;
import com.aaa.easyagent.core.service.FlowStudioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RestController
@RequestMapping("eaAgent/flow")
@RequiredArgsConstructor
public class FlowStudioController {
    private static final AtomicInteger DEBUG_THREAD_SEQUENCE = new AtomicInteger();
    private static final ExecutorService DEBUG_EXECUTOR = Executors.newFixedThreadPool(4, task -> {
        Thread thread = new Thread(task, "flow-debug-" + DEBUG_THREAD_SEQUENCE.incrementAndGet());
        thread.setDaemon(true);
        return thread;
    });
    private final FlowStudioService flowStudioService;
    private final FlowCompiler compiler;
    private final FlowEngine flowEngine;

    @GetMapping("list")
    public BaseResult<?> list() {
        return BaseResult.buildSuc(flowStudioService.list());
    }

    @GetMapping("node-types")
    public BaseResult<?> nodeTypes() {
        return BaseResult.buildSuc(flowStudioService.nodeTypes());
    }

    @PostMapping
    public BaseResult<?> create(@RequestBody FlowCreateRequest request) {
        return BaseResult.buildSuc(flowStudioService.create(request));
    }

    @GetMapping("{flowId}")
    public BaseResult<?> detail(@PathVariable Long flowId) {
        return BaseResult.buildSuc(flowStudioService.detail(flowId));
    }

    @DeleteMapping("{flowId}")
    public BaseResult<?> delete(@PathVariable Long flowId) {
        flowStudioService.delete(flowId);
        return BaseResult.buildSuc(true);
    }

    @GetMapping("{flowId}/draft")
    public BaseResult<FlowDefinition> draft(@PathVariable Long flowId) {
        return BaseResult.buildSuc(flowStudioService.draft(flowId));
    }

    @PutMapping("{flowId}/draft")
    public BaseResult<?> saveDraft(@PathVariable Long flowId, @RequestBody FlowDefinition definition) {
        flowStudioService.saveDraft(flowId, definition);
        return BaseResult.buildSuc(true);
    }

    @PostMapping("{flowId}/validate")
    public BaseResult<?> validate(@PathVariable Long flowId, @RequestBody FlowDefinition definition) {
        return BaseResult.buildSuc(flowStudioService.validate(flowId, definition));
    }

    @PostMapping("{flowId}/publish")
    public BaseResult<?> publish(@PathVariable Long flowId) {
        return BaseResult.buildSuc(flowStudioService.publish(flowId));
    }

    @PostMapping("{flowId}/debug-run")
    public SseEmitter debugRun(@PathVariable Long flowId, @RequestBody FlowDebugRunRequest request) {
        FlowDefinition definition = request.getDefinition() == null
                ? flowStudioService.draft(flowId) : request.getDefinition();
        if (definition.getRevisionId() == null) {
            definition.setRevisionId(flowStudioService.draft(flowId).getRevisionId());
        }
        compiler.compile(definition);
        String runId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(5 * 60_000L);
        FlowRunContext context = FlowRunContext.builder().runId(runId).flowId(flowId)
                .revisionId(definition.getRevisionId()).sessionId(request.getSessionId())
                .input(request.getInput() == null ? Map.of() : request.getInput())
                .debug(true).syncMode(false).sseEmitter(emitter)
                .executor(DEBUG_EXECUTOR).build();
        DEBUG_EXECUTOR.execute(() -> {
            try {
                flowEngine.execute(compiler.compile(definition), context);
            } catch (Throwable e) {
                log.error("Flow debug run failed, runId={}", runId, e);
                emitter.completeWithError(e);
            } finally {
                emitter.complete();
            }
        });
        return emitter;
    }
}
