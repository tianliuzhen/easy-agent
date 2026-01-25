package com.aaa.easyagent.web.biz.agent;

import com.aaa.easyagent.biz.agent.service.AgentChatService;
import com.aaa.easyagent.core.domain.base.BaseResult;
import com.aaa.easyagent.core.domain.enums.ModelTypeEnum;
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

    @PostMapping("/ai/saveAgent")
    public BaseResult saveAgent(@RequestBody EaAgentReq req) {
        return BaseResult.buildSuc(agentManagerService.save(req));
    }

    @PostMapping("/ai/listAgent")
    public BaseResult listAgent(@RequestBody EaAgentReq req) {
        return BaseResult.buildSuc(agentManagerService.listAgent(req));
    }

    @PostMapping("/ai/delAgent")
    public BaseResult delAgent(@RequestBody EaAgentReq req) {
        return BaseResult.buildSuc(agentManagerService.delAgent(req));
    }

    @PostMapping("/ai/queryChatModelTypeList")
    public BaseResult queryChatModelTypeList() {
        return BaseResult.buildSuc(ModelTypeEnum.getAll());
    }

    /**
     * 对话
     *
     * @param conversationId
     * @param msg
     * @return
     */
    @GetMapping("/ai/streamChatWith")
    public SseEmitter streamChatWith(
            @RequestParam(defaultValue = "110") String conversationId,
            @RequestParam(defaultValue = "你好") String msg,
            @RequestParam(defaultValue = "1", required = false) String agentId) {
        // 默认设置60分钟
        SseEmitter sseEmitter = new SseEmitterUTF8(1000 * 60L);


        // todo 改为线程池
        new Thread(() -> {
            try {
                agentChatService.streamChatWith(conversationId, msg, agentId, sseEmitter);
            } catch (Throwable e) {
                log.error("streamChatWith:{}", e.getMessage(), e);
            }
        }).start();

        return sseEmitter;
    }

}
