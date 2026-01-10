package com.aaa.easyagent.biz.agent;

import com.aaa.easyagent.biz.agent.model.AgentFinish;
import com.aaa.easyagent.biz.agent.model.AgentOutput;
import com.aaa.easyagent.biz.agent.model.FunctionUseAction;
import com.aaa.easyagent.biz.agent.parser.AgentOutputParser;
import com.aaa.easyagent.biz.function.FunctionToolManager;
import com.aaa.easyagent.common.config.exception.AgentException;
import com.aaa.easyagent.common.config.exception.AgentToolException;
import com.aaa.easyagent.common.llm.LLmModelSelector;
import com.aaa.easyagent.common.util.ChatResponseUtil;
import com.aaa.easyagent.common.util.JsonSchemaGenerator;
import com.aaa.easyagent.core.domain.model.AgentModel;
import com.aaa.easyagent.core.domain.model.ToolModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.model.function.FunctionCallback;

import java.util.*;

/**
 * @author liuzhen.tian
 * @version 1.0 BaseAgent.java  2026/1/8 21:14
 */
@Slf4j
public abstract class BaseAgent {

    protected final LLmModelSelector LLmModelSelector;
    protected final FunctionToolManager functionToolManager;


    public static final int DECISION_CNT_LIMIT = 20;

    public static final String DEFAULT_FUNCTION_TEMPLATE = """
            Tool name: {name}, description: {description}, input type schema: {schema}
            """;

    public static final PromptTemplate functionTemplate = new PromptTemplate(DEFAULT_FUNCTION_TEMPLATE);

    public BaseAgent(LLmModelSelector LLmModelSelector, FunctionToolManager functionToolManager) {
        this.LLmModelSelector = LLmModelSelector;
        this.functionToolManager = functionToolManager;
    }


    /**
     * 提示词构建
     *
     * @param agentModel
     * @return
     */
    public abstract Prompt buildPrompt(AgentModel agentModel);

    /**
     * 执行
     *
     * @param callbackMap
     * @return
     */
    public abstract AgentOutput run(ChatModel chatModel, Map<String, FunctionCallback> callbackMap, Prompt prompt);

    /**
     * agent 执行
     * 1. 支持ReAct方式实现agent【Thought-Action-ActionInput-Observation...Thought-Action-ActionInput-Observation】
     * 为了支持轻量级大模型对tool调用，如deepseek-R1，qwen-max 不支持tool，但是成本低和推理逻辑性强.
     * <p>
     * 2. 支持了传统大模型tool形式交互
     *
     * @return
     */
    public String exec(AgentModel agentModel) {
        // 根据agent选择模型
        ChatModel chatModel = LLmModelSelector.getModel(agentModel);
        if (chatModel == null) {
            throw new AgentException(agentModel.getModelType() + "无法匹配大模型");
        }
        log.info("使用【{}】大模型开始决策=========》Begin", agentModel.getModelType());

        // 根据agent构造回调工具
        List<ToolModel> toolModels = agentModel.getToolModels();
        Map<String, FunctionCallback> callbackMap = buildToolFun(toolModels);

        // 提示词构建
        Prompt prompt = this.buildPrompt(agentModel);

        // 限制决策轮数，防止无限调用
        int decisionCnt = 1;
        while (decisionCnt < DECISION_CNT_LIMIT) {
            log.info("第{}次大模型决策", decisionCnt);


            // tool/react
            AgentOutput agentOutput = this.run(chatModel, callbackMap, prompt);

            // 思考完成
            if (agentOutput instanceof AgentFinish) {
                log.info("第{}次大模型决策结束：=========》end", decisionCnt);
                String llmResponse = ((AgentFinish) agentOutput).getResult();
                log.info("第{}次大模型决策结果：{}", decisionCnt, llmResponse);
                return llmResponse;
            }

            decisionCnt++;
        }


        return null;
    }


    protected static void callBackForTool(FunctionUseAction functionUseAction, Map<String, FunctionCallback> callbackMap, Prompt prompt) {
        FunctionCallback functionToolCallback = callbackMap.get(functionUseAction.getAction());
        if (functionToolCallback == null) {
            throw new AgentToolException("无法匹配toolFunction");
        }
        String callToolResult = functionToolCallback.call(functionUseAction.getActionInput());

        // 添加工具执行结果记忆
        List<ToolResponseMessage.ToolResponse> responses = new ArrayList<>();
        responses.add(new ToolResponseMessage.ToolResponse(
                UUID.randomUUID().toString(),
                functionUseAction.getAction(),
                callToolResult));
        ToolResponseMessage toolResponseMessage = new ToolResponseMessage(responses);
        prompt.getInstructions().add(toolResponseMessage);
    }


    /**
     * 构建函数回调
     *
     * @param toolModels
     * @return
     */
    protected Map<String, FunctionCallback> buildToolFun(List<ToolModel> toolModels) {
        Map<String, FunctionCallback> callbackMap = new HashMap<>();
        // 解析成函数
        toolModels.forEach(e -> {
            callbackMap.put(e.getToolName(),
                    new FunctionCallback() {

                        @Override
                        public String getName() {
                            return e.getToolName();
                        }

                        @Override
                        public String getDescription() {
                            return e.getToolDesc();
                        }

                        @Override
                        public String getInputTypeSchema() {
                            return JsonSchemaGenerator.generateJsonSchema(e.getInputTypeSchemas());
                        }

                        @Override
                        public String call(String functionInput) {
                            return functionToolManager.call(functionInput, e);
                        }
                    });

        });
        return callbackMap;
    }

    protected static void addAssistantMessage(Prompt prompt, ChatResponse chatResponse) {
        AssistantMessage toolResponseMessage = new AssistantMessage(ChatResponseUtil.getResStr(chatResponse));
        prompt.getInstructions().add(toolResponseMessage);
    }

}
