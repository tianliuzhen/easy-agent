package com.aaa.easyagent.biz.agent;

import com.aaa.easyagent.biz.agent.data.AgentFinish;
import com.aaa.easyagent.biz.agent.data.AgentOutput;
import com.aaa.easyagent.biz.agent.data.FunctionUseAction;
import com.aaa.easyagent.biz.function.FunctionToolManager;
import com.aaa.easyagent.common.config.exception.AgentException;
import com.aaa.easyagent.common.config.exception.AgentToolException;
import com.aaa.easyagent.common.llm.LLmModelSelector;
import com.aaa.easyagent.common.util.ChatResponseUtil;
import com.aaa.easyagent.common.util.JsonSchemaGenerator;
import com.aaa.easyagent.biz.agent.data.AgentContext;
import com.aaa.easyagent.biz.agent.data.ToolDefinition;
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

    protected final AgentContext agentContext;

    /**
     * llm模型
     */
    protected ChatModel chatModel;
    /**
     * 工具回调
     */
    protected Map<String, FunctionCallback> callbackMap;

    protected List<Message> messages;

    protected Prompt prompt;

    /**
     * 决策轮数限制
     */
    public static final int DECISION_CNT_LIMIT = 20;

    /**
     * 默认函数模板
     */
    public static final String DEFAULT_FUNCTION_TEMPLATE = """
            Tool name: {name}, description: {description}, input type schema: {schema}
            """;

    public static final PromptTemplate functionTemplate = new PromptTemplate(DEFAULT_FUNCTION_TEMPLATE);

    /**
     * agent初始化
     *
     * @param agentContext
     */
    public BaseAgent(AgentContext agentContext) {
        this.agentContext = agentContext;

        chatModel = LLmModelSelector.buildChatModel(agentContext);

        // 根据agent构造回调工具
        List<ToolDefinition> toolDefinitions = agentContext.getToolDefinitions();
        callbackMap = buildToolFun(toolDefinitions);

        messages = new ArrayList<>();
    }


    /**
     * 提示词构建
     *
     * @return
     */
    public abstract Prompt buildPrompt();

    /**
     * 执行
     *
     * @return
     */
    public abstract AgentOutput run();

    /**
     * agent 执行
     * 1. 支持ReAct方式实现agent【Thought-Action-ActionInput-Observation...Thought-Action-ActionInput-Observation】
     * 为了支持轻量级大模型对tool调用，如deepseek-R1，qwen-max 不支持tool，但是成本低和推理逻辑性强.
     * <p>
     * 2. 支持了传统大模型tool形式交互
     *
     * @return
     */
    public String exec(String question) {
        // 根据agent选择模型
        if (chatModel == null) {
            throw new AgentException(agentContext.getModelType() + "无法匹配大模型");
        }
        log.info("使用【{}】{}大模型开始决策=========》Begin...", agentContext.getModelType(), agentContext.getAgentModelConfig().getModelVersion());
        // 提示词构建
        prompt = this.buildPrompt();

        // 添加用户信息
        addUserMessage(question);

        // 限制决策轮数，防止无限调用
        int decisionCnt = 1;
        while (decisionCnt < DECISION_CNT_LIMIT) {
            log.info("第{}次大模型决策", decisionCnt);

            // tool/react ... 等等多模式执行
            AgentOutput agentOutput = this.run();

            // 思考完成
            if (agentOutput instanceof AgentFinish) {
                log.info("第{}次大模型决策结束：=========》end", decisionCnt);
                String llmResponse = ((AgentFinish) agentOutput).getResult();
                log.info("第{}次大模型决策结果：{}", decisionCnt, llmResponse);
                return llmResponse;
            }

            // todo 检查是否卡住
            // if (isStuck()) {
            //     handleStuckState();
            // }

            decisionCnt++;
        }


        return null;
    }


    protected void toolExecute(FunctionUseAction functionUseAction) {
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
        messages.add(toolResponseMessage);
    }


    /**
     * 构建函数回调
     *
     * @param toolDefinitions
     * @return
     */
    protected Map<String, FunctionCallback> buildToolFun(List<ToolDefinition> toolDefinitions) {
        Map<String, FunctionCallback> callbackMap = new HashMap<>();
        // 解析成函数
        toolDefinitions.forEach(e -> {
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
                            return FunctionToolManager.call(functionInput, e);
                        }
                    });

        });
        return callbackMap;
    }

    protected void addAssistantMessage(Prompt prompt, ChatResponse chatResponse) {
        AssistantMessage toolResponseMessage = new AssistantMessage(ChatResponseUtil.getResStr(chatResponse));
        prompt.getInstructions().add(toolResponseMessage);
    }

    protected void addUserMessage(String question) {
        prompt.getInstructions().add(new UserMessage(question));
    }
}
