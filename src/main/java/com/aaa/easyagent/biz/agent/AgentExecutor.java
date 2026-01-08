package com.aaa.easyagent.biz.agent;

import com.aaa.easyagent.biz.function.FunctionToolManager;
import com.aaa.easyagent.biz.agent.model.AgentFinish;
import com.aaa.easyagent.biz.agent.model.AgentOutput;
import com.aaa.easyagent.biz.agent.model.FunctionUseAction;
import com.aaa.easyagent.biz.agent.parser.AgentOutputParser;
import com.aaa.easyagent.common.llm.ModelSelector;
import com.aaa.easyagent.core.domain.enums.ToolRunMode;
import com.aaa.easyagent.core.domain.model.AgentModel;
import com.aaa.easyagent.core.domain.model.ToolModel;
import com.aaa.easyagent.common.config.exception.AgentException;
import com.aaa.easyagent.common.config.exception.AgentToolException;
import com.aaa.easyagent.common.llm.deepseek.OpenAiChatOptions;
import com.aaa.easyagent.common.util.ChatResponseUtil;
import com.aaa.easyagent.common.util.JsonSchemaGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * @author liuzhen.tian
 * @version 1.0 AgentExecutor.java  2025/2/23 20:45
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class AgentExecutor {
    public static final int DECISION_CNT_LIMIT = 20;
    private final ModelSelector modelSelector;
    private final AgentOutputParser agentOutputParser;
    private final FunctionToolManager functionToolManager;


    private static final String DefaultTemplate = """
            Answer the following questions as best you can. 
            Remember not to fabricate tool execution results without execution tools.
            You have access to the following tools: {tools}
            
            Use the following format:
            If the tool execution result is not obtained, <Final Answer:> and <Thought:> should not appear
                Question: the input question you must answer
                Thought: you should always think about what to do
                Action: the action to take, should be one of [{tool_names}]
                Action Input: the input to the action
                Observation: the result of the action (If there is no result, return empty)
                ... (this Thought/Action/Action Input/Observation can repeat N times)
                Thought: I now know the final answer (If you don't know the answer, don't show it)
                Final Answer: the final answer to the original input question (If there are no results, there is no need to show them)
            
            
            During the process of answering questions, you need to follow the rules below:
                1. In the "Action" line, only include the name of the tool used, without any other characters.
                2. Do not guess the answer, if you need to use an Action, wait for the user to provide the results of the Action as the next step's Observation. And do not provide the subsequent Thought and Final Answer.
                3. Action Input must Analyze based on the description in the input type schema
                4. If you need more information, use the query_knowledge_base tool.
                5. If the result is insufficient, consider using another tool or querying the knowledge base again.
                6. Once you have all necessary information, provide a final answer.
                7. 关于查询当前时间的问题要调用工具拿到结果再回答
                8. 当调用工具后得到结果后，返回结果中不用出现 Use the following format 里面的 [Action /  Action Input]，防止干扰再次调用工具
            
            Use the following format for your response:
              <analysis>
                1. 总结用户的问题:
                   [提供问题的简要概述]
            
                2. 所需关键信息:
                   - [列出回答问题所需的主要信息]
            
                3. 潜在有用的工具:
                   - [列出可能有帮助的工具，并解释其原因]
            
                4. 计划中的工具使用顺序:
                   [如果需要使用多个工具，请概述您计划使用它们的顺序]
            
                5. 数据隐私与安全考虑:
                   注意与所访问工具或数据相关的任何潜在隐私或安全问题]
                <analysis>
            Begin!
            """;

    public static final String DEFAULT_FUNCTION_TEMPLATE = """
            Tool name: {name}, description: {description}, input type schema: {schema}
            """;


    public static final PromptTemplate functionTemplate = new PromptTemplate(DEFAULT_FUNCTION_TEMPLATE);

    public static final PromptTemplate reactSystemPromptTemplate = new PromptTemplate(DefaultTemplate);


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
        ChatModel chatModel = modelSelector.getModel(agentModel);
        if (chatModel == null) {
            throw new AgentException(agentModel.getModelType() + "无法匹配大模型");
        }
        log.info("使用【{}】大模型开始决策=========》Begin", agentModel.getModelType());

        // 根据agent构造回调工具
        List<ToolModel> toolModels = agentModel.getToolModels();
        Map<String, FunctionCallback> callbackMap = buildToolFunction(toolModels);

        // 提示词构建
        List<Message> messages = new ArrayList<>();
        Prompt prompt = buildDefaultPrompt(agentModel, callbackMap, messages);
        messages.add(new UserMessage(agentModel.getQuestion()));

        // 限制决策轮数，防止无限调用
        int decisionCnt = 1;
        while (decisionCnt < DECISION_CNT_LIMIT) {
            log.info("第{}次大模型决策", decisionCnt);
            ChatResponse chatResponse = chatModel.call(prompt);

            // tool-决策模式
            if (prompt.getOptions() != null && chatResponse.hasToolCalls()) {
                List<AssistantMessage.ToolCall> toolCalls = chatResponse.getResults().stream().flatMap(e -> e.getOutput().getToolCalls().stream()).toList();
                for (AssistantMessage.ToolCall toolCall : toolCalls) {
                    // 可并行调用 todo
                    callBackForTool(new FunctionUseAction(toolCall.name(), toolCall.arguments()), callbackMap, messages);
                }
                continue;
            }

            // reAct-决策模式
            String resStr = ChatResponseUtil.getResStr(chatResponse);
            AgentOutput agentOutput = agentOutputParser.parse(resStr);

            // 需要调用工具
            if (agentOutput instanceof FunctionUseAction functionUseAction) {
                callBackForTool(functionUseAction, callbackMap, messages);
            }

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

    private static void callBackForTool(FunctionUseAction functionUseAction, Map<String, FunctionCallback> callbackMap, List<Message> messages) {
        FunctionCallback functionToolCallback = callbackMap.get(functionUseAction.getAction());
        if (functionToolCallback == null) {
            throw new AgentToolException("无法匹配toolFunction");
        }
        String callToolResult = functionToolCallback.call(functionUseAction.getActionInput());

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
     * @param toolModels
     * @return
     */
    private Map<String, FunctionCallback> buildToolFunction(List<ToolModel> toolModels) {
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

    /**
     * 构建提示词
     *
     * @param agentModel
     * @param callbackMap
     * @param messages
     * @return
     */
    protected Prompt buildDefaultPrompt(AgentModel agentModel, Map<String, FunctionCallback> callbackMap,
                                        List<Message> messages) {
        // tool 模式
        if (agentModel.getToolRunMode() == ToolRunMode.tool) {
            OpenAiChatOptions chatOptions = OpenAiChatOptions.builder()
                    .toolCallbacks(callbackMap.values().stream().toList())
                    .internalToolExecutionEnabled(false) // 禁用内部工具执行
                    .build();
            return new Prompt(messages, chatOptions);
        }

        // reAct 模式
        if (agentModel.getToolRunMode() == ToolRunMode.reAct) {
            if (CollectionUtils.isEmpty(callbackMap)) {
                return null;
            }

            Map<String, Object> renderModel = new HashMap<>();
            Collection<FunctionCallback> values = callbackMap.values();
            renderModel.put("tool_names", values.stream().map(FunctionCallback::getName).toList().toString());
            StringBuilder tools = new StringBuilder();
            for (FunctionCallback callbackRequest : values) {
                // 生成 Agent 的工具的描述
                String toolValue = functionTemplate.render(Map.of(
                        "name", callbackRequest.getName(),
                        "description", callbackRequest.getDescription(),
                        "schema", callbackRequest.getInputTypeSchema()));
                tools.append(toolValue);
                tools.append("\n");
            }
            renderModel.put("tools", tools.toString());
            messages.add(new SystemMessage(reactSystemPromptTemplate.render(renderModel)));
            return new Prompt(messages);
        }

        throw new AgentException(agentModel.getToolRunMode() + "<UNK>");
    }

}
