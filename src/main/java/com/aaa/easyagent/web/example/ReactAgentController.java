package com.aaa.easyagent.web.example;

import com.aaa.easyagent.biz.agent.AgentExecutor;
import com.aaa.easyagent.core.domain.enums.ModelTypeEnum;
import com.aaa.easyagent.core.domain.enums.ToolRunMode;
import com.aaa.easyagent.core.domain.enums.ToolTypeEnum;
import com.aaa.easyagent.core.domain.model.AgentModel;
import com.aaa.easyagent.core.domain.model.ToolModel;
import com.aaa.easyagent.core.domain.template.HttpReqParamsTemplate;
import com.aaa.easyagent.core.domain.template.InputTypeSchema;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * @author liuzhen.tian
 * @version 1.0 ReactAgentController.java  2025/5/26 21:23
 */
@RestController
@RequestMapping("/reactAgent")
public class ReactAgentController {

    @Resource
    private AgentExecutor agentExecutor;

    @GetMapping(value = "/test")
    public void test() {
        AgentModel agentModel = new AgentModel();
        agentModel.setAgentId(1L);
        agentModel.setAgentName("查询时间助手");
        agentModel.setQuestion("查询北京当前时间");
        agentModel.setModelType(ModelTypeEnum.ollama);

        List<ToolModel> toolModels = new ArrayList<>();
        ToolModel toolModel = new ToolModel();
        toolModel.setToolId(1L);
        toolModel.setToolName("查询当前时间");
        toolModel.setToolDesc("输入地点查询当地时间");
        toolModel.setToolType(ToolTypeEnum.common_http);
        HttpReqParamsTemplate httpReqParamsTemplate = new HttpReqParamsTemplate();
        httpReqParamsTemplate.setUrl("http://localhost:8080/example/getCurrentDate");
        httpReqParamsTemplate.setMethodType("get");
        toolModel.setParamsTemplate(httpReqParamsTemplate);
        List<InputTypeSchema> inputTypeSchemas = new ArrayList<>();
        inputTypeSchemas.add(new InputTypeSchema("type","北京时间：beijing，东京时间：dongjing","string"));
        toolModel.setInputTypeSchemas(inputTypeSchemas);
        toolModels.add(toolModel);
        agentModel.setToolModels(toolModels);

        // 工具决策-tool
        agentModel.setToolRunMode(ToolRunMode.tool);
        agentExecutor.exec(agentModel);
    }


    @GetMapping(value = "/test2")
    public void test2() {
        AgentModel agentModel = new AgentModel();
        agentModel.setAgentId(1L);
        agentModel.setAgentName("查询时间助手");
        agentModel.setQuestion("查询当前时间");
        agentModel.setModelType(ModelTypeEnum.deepseek);

        List<ToolModel> toolModels = new ArrayList<>();
        ToolModel toolModel = new ToolModel();
        toolModel.setToolId(1L);
        toolModel.setToolName("查询当前时间");
        toolModel.setToolDesc("无需入参可查询当前系统时间");
        toolModel.setToolType(ToolTypeEnum.common_http);
        HttpReqParamsTemplate httpReqParamsTemplate = new HttpReqParamsTemplate();
        httpReqParamsTemplate.setUrl("http://localhost:8080/example/getCurrentDate");
        httpReqParamsTemplate.setMethodType("get");
        toolModel.setParamsTemplate(httpReqParamsTemplate);
        toolModel.setInputTypeSchemas(new ArrayList<>());
        toolModels.add(toolModel);
        agentModel.setToolModels(toolModels);
        agentExecutor.exec(agentModel);
    }

}
