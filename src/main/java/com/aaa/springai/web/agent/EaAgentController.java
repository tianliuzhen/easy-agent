package com.aaa.springai.web.agent;

import com.aaa.springai.domain.DO.EaAgentDO;
import com.aaa.springai.mapper.EaAgentDAO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author liuzhen.tian
 * @version 1.0 EaAgentController.java  2025/6/14 21:07
 */
@RestController
@RequestMapping("eaAgent")
@RequiredArgsConstructor
public class EaAgentController {

    private final EaAgentDAO eaAgentDAO;

    @PostMapping("/ai/insertAgent")
    public Map insertAgent(@RequestBody EaAgentDO eaAgentDO) {

        return null;
    }

}
