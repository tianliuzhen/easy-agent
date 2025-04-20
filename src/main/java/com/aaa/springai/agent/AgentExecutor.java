package com.aaa.springai.agent;

import com.aaa.springai.util.JSONUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author liuzhen.tian
 * @version 1.0 AgentExecutor.java  2025/2/23 20:45
 */

@Component
public class AgentExecutor {
    private static String DefaultTemplate = """
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
     * 为了支持轻量级大模型对tool调用，如deepseek-R1，qwen-max 不支持tool，但是成本低和推理逻辑性强
     *
     * @return
     */
    public String execute() {


        return null;
    }


    protected String generateAssistantMessage(List<FunctionCallback> toolCallbackRequests, String toolPrompt) {
        if (CollectionUtils.isEmpty(toolCallbackRequests)) {
            return null;
        }

        Map<String, Object> renderModel = new HashMap<>();
        renderModel.put("tool_names", toolCallbackRequests.stream().map(FunctionCallback::getName).toList().toString());
        StringBuilder tools = new StringBuilder();
        for (FunctionCallback callbackRequest : toolCallbackRequests) {
            // 生成 Agent 的工具的描述
            String toolValue = functionTemplate.render(Map.of(
                    "name", callbackRequest.getName(),
                    "description", callbackRequest.getDescription(),
                    "schema", JSONUtil.toStr(callbackRequest.getInputTypeSchema())));
            tools.append(toolValue);
            tools.append("\n");
        }
        renderModel.put("tools", tools.toString());
        return reactSystemPromptTemplate.render(renderModel);
    }

}
