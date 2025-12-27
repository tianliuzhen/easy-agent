package com.aaa.easyagent.web.biz.agent;

import com.aaa.easyagent.core.service.AgentManagerService;
import com.aaa.easyagent.core.domain.base.BaseResult;
import com.aaa.easyagent.core.domain.request.EaAgentReq;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author liuzhen.tian
 * @version 1.0 EaAgentController.java  2025/6/14 21:07
 */
@RestController
@RequestMapping("eaAgent")
@RequiredArgsConstructor
public class EaAgentController {

    private final AgentManagerService agentManagerService;

    @PostMapping("/ai/saveAgent")
    public BaseResult saveAgent(@RequestBody EaAgentReq req) {
        return BaseResult.buildSuc(agentManagerService.save(req));
    }

    @PostMapping("/ai/listAgent")
    public BaseResult listAgent() {
        return BaseResult.buildSuc(agentManagerService.selectAll());
    }

    @PostMapping("/ai/delAgent")
    public BaseResult delAgent(@RequestBody EaAgentReq req) {
        return BaseResult.buildSuc(agentManagerService.delAgent(req));
    }

    @PostMapping("/ai/queryChatModelList")
    public BaseResult queryChatModelList() {
        return BaseResult.buildSuc(List.of("qwen2.5:3b", "QwQ-32B"));
    }
}
