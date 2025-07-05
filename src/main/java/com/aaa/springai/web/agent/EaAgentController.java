package com.aaa.springai.web.agent;

import com.aaa.springai.domain.DO.EaAgentDO;
import com.aaa.springai.mapper.EaAgentDAO;
import com.aaa.springai.util.base.BaseResult;
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

    private final EaAgentDAO eaAgentDAO;

    @PostMapping("/ai/saveAgent")
    public BaseResult saveAgent(@RequestBody EaAgentDO eaAgentDO) {
        return BaseResult.buildSuc(eaAgentDAO.save(eaAgentDO));
    }

    @PostMapping("/ai/listAgent")
    public BaseResult listAgent() {
        return BaseResult.buildSuc(eaAgentDAO.selectAll());
    }

    @PostMapping("/ai/delAgent")
    public BaseResult delAgent(@RequestBody EaAgentDO eaAgentDO) {
        return BaseResult.buildSuc(eaAgentDAO.deleteByPrimaryKey(eaAgentDO.getId()));
    }

    @PostMapping("/ai/queryChatModelList")
    public BaseResult queryChatModelList() {
        return BaseResult.buildSuc(List.of("qwen2.5:3b", "QwQ-32B"));
    }
}
