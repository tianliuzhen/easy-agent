package com.aaa.easyagent.biz.agent;

import com.aaa.easyagent.biz.agent.model.AgentOutput;
import com.aaa.easyagent.biz.agent.model.FunctionUseAction;
import com.aaa.easyagent.biz.agent.parser.AgentOutputParser;
import com.aaa.easyagent.common.util.ChatResponseUtil;
import com.aaa.easyagent.core.domain.model.AgentModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author liuzhen.tian
 * @version 1.0 ReActAgentExecutor.java  2025/2/23 20:45
 */
@Slf4j
public class ReActAgentExecutor extends BaseReActAgent {

    protected static final String DefaultTemplate = """
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

    public static final PromptTemplate reactSystemPromptTemplate = new PromptTemplate(DefaultTemplate);

    public ReActAgentExecutor(AgentModel agentModel) {
        super(agentModel);
    }


    /**
     * 构建提示词
     *
     * @return
     */
    public Prompt buildPrompt() {

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

    @Override
    public AgentOutput think() {
        ChatResponse chatResponse = chatModel.call(prompt);

        // 添加助手执行记忆
        addAssistantMessage(prompt, chatResponse);

        // reAct-决策模式
        String resStr = ChatResponseUtil.getResStr(chatResponse);

        // 解析Action入参或者解析成功
        AgentOutput agentOutput = AgentOutputParser.parse(resStr);
        return agentOutput;
    }

    @Override
    public String act(FunctionUseAction functionUseAction) {
        toolExecute(functionUseAction);
        return "";
    }

    /**
     * react 执行模式：  思考/行动/观察...思考/行动/观察
     *
     * @return
     */
    @Override
    public AgentOutput run() {
        // 思考：构建当前思考的上下文
        AgentOutput agentOutput = think();

        // 行动：基于思考决定行动
        if (agentOutput instanceof FunctionUseAction functionUseAction) {
            this.act(functionUseAction);
        }

        // 观察...

        return agentOutput;
    }


}
