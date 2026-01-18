package com.aaa.easyagent.web.biz.agent;

import com.aaa.easyagent.core.domain.base.BaseResult;
import com.aaa.easyagent.core.domain.enums.ModelTypeEnum;
import com.aaa.easyagent.core.domain.request.EaAgentReq;
import com.aaa.easyagent.core.service.AgentManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
