package com.aaa.springai.agent;

import com.aaa.springai.agent.function.FunctionToolManager;
import com.aaa.springai.agent.model.AgentFinish;
import com.aaa.springai.agent.model.AgentOutput;
import com.aaa.springai.agent.model.FunctionUseAction;
import com.aaa.springai.agent.parser.AgentOutputParser;
import com.aaa.springai.domain.model.AgentModel;
import com.aaa.springai.domain.model.ToolModel;
import com.aaa.springai.exception.AgentException;
import com.aaa.springai.exception.AgentToolException;
import com.aaa.springai.util.ChatResponseUtil;
import com.aaa.springai.util.JacksonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
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
            Answer the following questions as best you can. You have access to the following tools:
                        {tools}
                        
                        Use the following format:
                        
                        Question: the input question you must answer
                        Thought: you should always think about what to do
                        Action: the action to take, should be one of [{tool_names}]
                        Action Input: the input to the action
                        Observation: the result of the action
                        ... (this Thought/Action/Action Input/Observation can repeat N times)
                        Thought: I now know the final answer
                        Final Answer: the final answer to the original input question
                        
                        During the process of answering questions, you need to follow the rules below:
                        1. In the "Action" line, only include the name of the tool used, without any other characters.
                        2. Do not guess the answer, if you need to use an Action, wait for the user to provide the results of the Action as the next step's Observation. And do not provide the subsequent Thought and Final Answer.
                        3. Action Input must Analyze based on the description in the input type schema
                        4. If you need more information, use the query_knowledge_base tool.
                        5. If the result is insufficient, consider using another tool or querying the knowledge base again.
                        6. Once you have all necessary information, provide a final answer.
                        7. 关于查询当前时间的问题要调用工具拿到结果再回答
                        
                        Use the following format for your response:
                        <analysis>
                        1. Summarize the user's question:
                           [Provide a brief summary of the question]
                                    
                        2. Key information needed:
                           - [List the main pieces of information required to answer the question]
                                    
                        3. Potentially useful tools:
                           - [List tools that might be helpful and explain why]
                                    
                        4. Planned sequence of tool usage:
                           [If multiple tools are needed, outline the order in which you plan to use them]
                                    
                        5. Data privacy and security considerations:
                           [Note any potential privacy or security concerns related to the tools or data being accessed]
                        </analysis>
                        
                        Begin!
            """;

    public static final String DEFAULT_FUNCTION_TEMPLATE = """
            Tool name: {name}, description: {description}, input type schema: {schema}
                       """;


    public static final PromptTemplate functionTemplate = new PromptTemplate(DEFAULT_FUNCTION_TEMPLATE);

    public static final PromptTemplate reactSystemPromptTemplate = new PromptTemplate(DefaultTemplate);


    /**
     * 采用React方式实现agent
     * 为了支持轻量级大模型对tool调用，如deepseek-R1，qwen-max 不支持tool，但是成本低和推理逻辑性强
     *
     * @return
     */
    public String exec(AgentModel agentModel) {
        // 根据agent选择模型
        ChatModel chatModel = modelSelector.getModel(agentModel);
        if (chatModel == null) {
            throw new AgentException(agentModel.getModelType() + "无法匹配大模型");
        }

        // 根据agent构造工具
        List<ToolModel> toolModels = agentModel.getToolModels();

        // 提示词构建
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(generateAssistantMessage(toolModels)));
        messages.add(new UserMessage(agentModel.getQuestion()));
        Prompt prompt = new Prompt(messages);

        // 限制决策轮数，防止无限调用
        int decisionCnt = 1;
        while (decisionCnt < DECISION_CNT_LIMIT) {
            log.info("第{}次大模型决策", decisionCnt);
            ChatResponse chatResponse = chatModel.call(prompt);
            String resStr = ChatResponseUtil.getResStr(chatResponse);
            AgentOutput agentOutput = agentOutputParser.parse(resStr);

            // 需要调用工具
            if (agentOutput instanceof FunctionUseAction functionUseAction) {
                ToolModel toolFunction = toolModels.stream()
                        .filter(e -> StringUtils.equals(e.getToolName(), functionUseAction.getAction()))
                        .findAny()
                        .orElseThrow(() -> new AgentToolException("无法匹配toolFunction"));
                String callToolResult = functionToolManager.call(functionUseAction.getActionInput(), toolFunction);

                List<ToolResponseMessage.ToolResponse> responses = new ArrayList<>();
                responses.add(new ToolResponseMessage.ToolResponse(
                        UUID.randomUUID().toString(),
                        functionUseAction.getAction(),
                        callToolResult));
                ToolResponseMessage toolResponseMessage = new ToolResponseMessage(responses);
                messages.add(toolResponseMessage);
            }

            // 思考完成
            if (agentOutput instanceof AgentFinish) {
                log.info("第{}次大模型决策结束", decisionCnt);
                return ((AgentFinish) agentOutput).getLlmResponse();
            }


            decisionCnt++;
        }


        return null;
    }


    protected String generateAssistantMessage(List<ToolModel> toolCallbackRequests) {
        if (CollectionUtils.isEmpty(toolCallbackRequests)) {
            return null;
        }

        Map<String, Object> renderModel = new HashMap<>();
        renderModel.put("tool_names", toolCallbackRequests.stream().map(ToolModel::getToolName).toList().toString());
        StringBuilder tools = new StringBuilder();
        for (ToolModel callbackRequest : toolCallbackRequests) {
            // 生成 Agent 的工具的描述
            String toolValue = functionTemplate.render(Map.of(
                    "name", callbackRequest.getToolName(),
                    "description", callbackRequest.getToolDesc(),
                    "schema", JacksonUtil.toStr(callbackRequest.getInputTypeSchemas())));
            tools.append(toolValue);
            tools.append("\n");
        }
        renderModel.put("tools", tools.toString());
        return reactSystemPromptTemplate.render(renderModel);
    }

}
