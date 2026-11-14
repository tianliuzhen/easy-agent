package com.aaa.easyagent.web.biz.agent;

import com.aaa.easyagent.biz.agent.orchestration.CompiledFlowPlan;
import com.aaa.easyagent.biz.agent.orchestration.FlowCompiler;
import com.aaa.easyagent.biz.agent.orchestration.FlowEngine;
import com.aaa.easyagent.biz.agent.orchestration.FlowExecutorConfig;
import com.aaa.easyagent.biz.agent.orchestration.FlowRunContext;
import com.aaa.easyagent.core.domain.base.BaseResult;
import com.aaa.easyagent.core.domain.flow.FlowDefinition;
import com.aaa.easyagent.core.domain.request.FlowCreateRequest;
import com.aaa.easyagent.core.domain.request.FlowDebugRunRequest;
import com.aaa.easyagent.core.service.FlowStudioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

@Slf4j
@RestController
@RequestMapping("eaAgent/flow")
public class FlowStudioController {
    /** 调试 SSE 连接超时，单位：毫秒 */
    private static final long DEBUG_SSE_TIMEOUT_MILLIS = 5 * 60_000L;

    private final FlowStudioService flowStudioService;
    private final FlowCompiler compiler;
    private final FlowEngine flowEngine;
    /** 与节点执行共用的有界线程池，不再为调试单独维护一个固定池 */
    private final Executor flowExecutor;

    public FlowStudioController(FlowStudioService flowStudioService, FlowCompiler compiler, FlowEngine flowEngine,
                                @Qualifier(FlowExecutorConfig.FLOW_NODE_EXECUTOR) Executor flowExecutor) {
        this.flowStudioService = flowStudioService;
        this.compiler = compiler;
        this.flowEngine = flowEngine;
        this.flowExecutor = flowExecutor;
    }

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

    /**
     * 画布试运行。
     * <p>
     * 1. 取待运行的图定义（优先用前端传来的未保存草稿）
     * 2. 编译一次并复用编译产物（编译会做全量校验并计算校验和，且 prepare 会回写节点端口，不能编译两遍）
     * 3. 把客户端断开、超时、异常都接到取消标志上，避免用户关掉页面后节点继续跑、继续计费
     * 4. 在共享线程池里异步执行，SSE 收口统一放在 finally
     */
    @PostMapping("{flowId}/debug-run")
    public SseEmitter debugRun(@PathVariable Long flowId, @RequestBody FlowDebugRunRequest request) {
        FlowDefinition definition = request.getDefinition() == null
                ? flowStudioService.draft(flowId) : request.getDefinition();
        if (definition.getRevisionId() == null) {
            definition.setRevisionId(flowStudioService.draft(flowId).getRevisionId());
        }
        CompiledFlowPlan plan = compiler.compile(definition);
        String runId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(DEBUG_SSE_TIMEOUT_MILLIS);
        FlowRunContext context = FlowRunContext.builder().runId(runId).flowId(flowId)
                .revisionId(definition.getRevisionId()).sessionId(request.getSessionId())
                .input(request.getInput() == null ? Map.of() : request.getInput())
                .debug(true).syncMode(false).sseEmitter(emitter).build();
        emitter.onCompletion(() -> context.cancel("调试连接已关闭"));
        emitter.onTimeout(() -> context.cancel("调试连接超时"));
        emitter.onError(error -> context.cancel("调试连接异常：" + error.getMessage()));
        flowExecutor.execute(() -> {
            try {
                flowEngine.execute(plan, context);
            } catch (RuntimeException runtimeException) {
                log.error("Flow 试运行失败，runId={}", runId, runtimeException);
                emitter.completeWithError(runtimeException);
            } finally {
                emitter.complete();
            }
        });
        return emitter;
    }
}
