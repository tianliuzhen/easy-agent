package com.aaa.easyagent.web.example;

import com.aaa.easyagent.biz.agent.ReActAgentExecutor;
import com.aaa.easyagent.biz.agent.ReActAgentXmlExecutor;
import com.aaa.easyagent.biz.agent.data.AgentContext;
import com.aaa.easyagent.biz.agent.data.AgentModelConfig;
import com.aaa.easyagent.biz.agent.data.ToolDefinition;
import com.aaa.easyagent.core.domain.enums.ModelTypeEnum;
import com.aaa.easyagent.core.domain.enums.ToolRunMode;
import com.aaa.easyagent.core.domain.enums.ToolTypeEnum;
import com.aaa.easyagent.core.domain.template.HttpReqParamsTemplate;
import com.aaa.easyagent.core.domain.template.InputTypeSchema;
import com.google.common.collect.Lists;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author liuzhen.tian
 * @version 1.0 ReactAgentController.java  2025/5/26 21:23
 */
@RestController
@RequestMapping("/reactAgent")
public class ReactAgentController {


    @GetMapping(value = "/test")
    public void test() {
        AgentContext agentContext = new AgentContext();
        agentContext.setAgentId(1L);
        agentContext.setAgentName("查询时间助手");
        // agentContext.setQuestion("查询北京当前时间");
        agentContext.setModelType(ModelTypeEnum.ollama);

        List<ToolDefinition> toolDefinitions = new ArrayList<>();
        ToolDefinition toolDefinition = new ToolDefinition();
        toolDefinition.setToolId(1L);
        toolDefinition.setToolName("查询当前时间");
        toolDefinition.setToolDesc("输入地点查询当地时间");
        toolDefinition.setToolType(ToolTypeEnum.HTTP);
        HttpReqParamsTemplate httpReqParamsTemplate = new HttpReqParamsTemplate();
        httpReqParamsTemplate.setUrl("http://localhost:8080/example/getCurrentDate");
        httpReqParamsTemplate.setMethod("get");
        toolDefinition.setParamsTemplate(httpReqParamsTemplate);
        List<InputTypeSchema> inputTypeSchemas = new ArrayList<>();
        inputTypeSchemas.add(new InputTypeSchema("type", "北京时间：beijing，东京时间：dongjing", "string"));
        toolDefinition.setInputTypeSchemas(inputTypeSchemas);
        toolDefinitions.add(toolDefinition);
        agentContext.setToolDefinitions(toolDefinitions);

        AgentModelConfig agentModelConfig = AgentModelConfig.builder()
                .modelVersion("deepseek-ai/DeepSeek-V3")
                .apiKey("sk-ewykgprysbswuqagkpbuglgodbxshtucznttazynawykgsds")
                .baseUrl("https://api.siliconflow.cn/")
                .build();
        agentContext.setAgentModelConfig(agentModelConfig);
        agentContext.setModelType(ModelTypeEnum.siliconflow);

        // 工具决策-tool
        agentContext.setToolRunMode(ToolRunMode.Tool);

        new ReActAgentExecutor(agentContext).exec("查询北京当前时间");
    }
    @GetMapping(value = "/testXml")
    public void testXml() {
        AgentContext agentContext = new AgentContext();
        agentContext.setAgentId(1L);
        agentContext.setAgentName("查询时间助手");
        // agentContext.setQuestion("查询北京当前时间");
        agentContext.setModelType(ModelTypeEnum.ollama);

        // mock工具：描述
        List<ToolDefinition> toolDefinitions = new ArrayList<>();
        ToolDefinition toolDefinition = new ToolDefinition();
        toolDefinition.setToolId(1L);
        toolDefinition.setToolName("查询贵金属实时价格");
        toolDefinition.setToolDesc("查询黄金白银的价格");
        toolDefinition.setToolType(ToolTypeEnum.HTTP);
        // mock工具：http调用
        HttpReqParamsTemplate httpReqParamsTemplate = new HttpReqParamsTemplate();
        httpReqParamsTemplate.setUrl("http://localhost:8080/example/queryPreciousMetalsPrice");
        httpReqParamsTemplate.setMethod("get");
        httpReqParamsTemplate.setRequestParams(Lists.newArrayList(Map.of("key", "type","value","")));
        toolDefinition.setParamsTemplate(httpReqParamsTemplate);
        // mock工具：入参解析模板
        List<InputTypeSchema> inputTypeSchemas = new ArrayList<>();
        inputTypeSchemas.add(new InputTypeSchema("type", "silver or gold", "string","","$.requestParams.type"));
        toolDefinition.setInputTypeSchemas(inputTypeSchemas);
        toolDefinitions.add(toolDefinition);


        agentContext.setToolDefinitions(toolDefinitions);

        AgentModelConfig agentModelConfig = AgentModelConfig.builder()
                .modelVersion("Pro/Qwen/Qwen2.5-7B-Instruct")
                .apiKey("sk-ewykgprysbswuqagkpbuglgodbxshtucznttazynawykgsds")
                .baseUrl("https://api.siliconflow.cn/")
                .build();
        agentContext.setAgentModelConfig(agentModelConfig);
        agentContext.setModelType(ModelTypeEnum.siliconflow);


        // 工具决策-tool
        agentContext.setToolRunMode(ToolRunMode.Tool);

        new ReActAgentXmlExecutor(agentContext).exec("查询黄金的价格");
    }

    @GetMapping(value = "/test2")
    public void test2() {
        AgentContext agentContext = new AgentContext();
        agentContext.setAgentId(1L);
        agentContext.setAgentName("查询时间助手");
        // agentContext.setQuestion("查询当前时间");
        agentContext.setModelType(ModelTypeEnum.deepseek);

        List<ToolDefinition> toolDefinitions = new ArrayList<>();
        ToolDefinition toolDefinition = new ToolDefinition();
        toolDefinition.setToolId(1L);
        toolDefinition.setToolName("查询当前时间");
        toolDefinition.setToolDesc("无需入参可查询当前系统时间");
        toolDefinition.setToolType(ToolTypeEnum.HTTP);
        HttpReqParamsTemplate httpReqParamsTemplate = new HttpReqParamsTemplate();
        httpReqParamsTemplate.setUrl("http://localhost:8080/example/getCurrentDate");
        httpReqParamsTemplate.setMethod("get");
        toolDefinition.setParamsTemplate(httpReqParamsTemplate);
        toolDefinition.setInputTypeSchemas(new ArrayList<>());
        toolDefinitions.add(toolDefinition);
        agentContext.setToolDefinitions(toolDefinitions);


        AgentModelConfig agentModelConfig = AgentModelConfig.builder()
                .modelVersion("Qwen/QwQ-32B")
                .apiKey("sk-ewykgprysbswuqagkpbuglgodbxshtucznttazynawykgsds")
                .baseUrl("https://api.siliconflow.cn/")
                .build();
        agentContext.setAgentModelConfig(agentModelConfig);
        agentContext.setModelType(ModelTypeEnum.siliconflow);


        new ReActAgentExecutor(agentContext).exec("查询当前时间");
    }

}
