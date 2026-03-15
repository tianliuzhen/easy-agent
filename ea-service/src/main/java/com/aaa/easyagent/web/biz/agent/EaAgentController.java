package com.aaa.easyagent.web.biz.agent;

import com.aaa.easyagent.biz.agent.service.AgentChatService;
import com.aaa.easyagent.core.domain.base.BaseResult;
import com.aaa.easyagent.core.domain.request.EaAgentReq;
import com.aaa.easyagent.core.service.AgentManagerService;
import com.aaa.easyagent.web.example.sse.SseEmitterUTF8;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @author liuzhen.tian
 * @version 1.0 EaAgentController.java  2025/6/14 21:07
 */
@Slf4j
@RestController
@RequestMapping("eaAgent")
@RequiredArgsConstructor
public class EaAgentController {

    private final AgentManagerService agentManagerService;
    private final AgentChatService agentChatService;
    private final ModelPlatformController modelPlatformController;

    @PostMapping("/ai/saveAgent")
    public BaseResult saveAgent(@RequestBody EaAgentReq req) {
        return BaseResult.buildSuc(agentManagerService.save(req));
    }

    @PostMapping("/ai/listAgent")
    public BaseResult listAgent(@RequestBody EaAgentReq req) {
        return BaseResult.buildSuc(agentManagerService.listAgent(req));
    }


    /**
     * 根据id查询
     */
    @PostMapping("/ai/queryAgent")
    public BaseResult queryAgent(@RequestBody EaAgentReq req) {
        return BaseResult.buildSuc(agentManagerService.queryAgent(req));
    }

    @PostMapping("/ai/delAgent")
    public BaseResult delAgent(@RequestBody EaAgentReq req) {
        return BaseResult.buildSuc(agentManagerService.delAgent(req));
    }

    /**
     * 提示词：
     * 1.需要实现 查询模型平台类型 从枚举读，改为从 数据库读
     * 2.需要新增一张表，参考 Config/initsql/下面的表结构
     * 3.前端对接，需要安装当前对接的格式
     * 4.前端需要在大模型配置的菜单中，实现crud功能
     * 5.后端crud接口实现（需要执行mvn 动态生成代码）
     * 6.需要表的字段：模型平台/模型icon/官网链接/基础url/模型版本[存数组]
     * <p>
     * 你需要先生成表结构，然后告诉我去执行，生成代码，然后实现crud功能
     *
     * @return
     */
    @PostMapping("/ai/queryChatModelTypeList")
    public BaseResult queryChatModelTypeList() {
        // 从数据库读取模型平台类型 (直接调用 ModelPlatformController 的方法)
        return modelPlatformController.queryChatModelTypeList();
    }

    /**
     * 对话
     *
     * @param sessionId 会话 ID
     * @param msg       消息
     * @return SSE Emitter
     */
    @GetMapping("/ai/streamChatWith")
    public SseEmitter streamChatWith(
            @RequestParam(defaultValue = "110") String sessionId,
            @RequestParam(defaultValue = "你好") String msg,
            @RequestParam(defaultValue = "1", required = false) String agentId) {
        // 默认设置 5 分钟
        SseEmitter sseEmitter = new SseEmitterUTF8(1000 * 60L * 5);
    
        // 保存当前的 SecurityContext
        var securityContext = org.springframework.security.core.context.SecurityContextHolder.getContext();
    
        // todo 改为线程池
        new Thread(() -> {
            try {
                // 在子线程中设置 SecurityContext
                org.springframework.security.core.context.SecurityContextHolder.setContext(securityContext);
                agentChatService.streamChatWith(sessionId, msg, agentId, sseEmitter);
            } catch (Throwable e) {
                log.error("streamChatWith:{}", e.getMessage(), e);
            } finally {
                // 清理线程上下文
                org.springframework.security.core.context.SecurityContextHolder.clearContext();
            }
        }).start();
    
        return sseEmitter;
    }

}
