package com.aaa.easyagent.biz.agent;

import com.aaa.easyagent.biz.agent.model.AgentFinish;
import com.aaa.easyagent.biz.agent.model.AgentOutput;
import com.aaa.easyagent.biz.agent.model.FunctionUseAction;
import com.aaa.easyagent.biz.agent.parser.OutputParserException;
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
 * @link com.aaa.easyagent.biz.agent.ReActAgentExecutor
 * <pre>
 * 是的，从多个角度来看，XML解析方式确实比之前的解析方式更加友好：
 *
 * 1. **结构化程度更高**：
 * - XML格式具有清晰的标签结构，例如`<Action>`、`<Action Input>`、`<Final Answer>`，使得大模型的输出更容易解析和处理。
 * - 相比之前的纯文本格式（如"Action: ... Action Input: ..."），XML标签提供了更明确的分隔符，减少了解析错误的可能性。
 *
 * 2. **可读性更强**：
 * - XML标签使输出更易于阅读和理解，开发者能够快速识别各个部分的含义。
 * - 这种结构化的格式便于调试和维护。
 *
 * 3. **解析准确性提高**：
 * - 使用正则表达式解析XML标签比解析自由格式的文本更可靠，因为XML标签具有固定的开始和结束标记。
 * - 减少了因文本格式变化导致的解析失败。
 *
 * 4. **扩展性更好**：
 * - XML格式允许添加更多的元数据或嵌套结构，为未来可能的扩展提供了便利。
 * - 可以轻松地在现有标签内添加属性或其他子标签。
 *
 * 5. **错误处理更精确**：
 * - 当XML格式不完整或错误时，更容易检测和定位问题，例如只存在`<Action>`标签而缺少`<Action Input>`标签。
 *
 * @author liuzhen.tian
 * @version 1.0 ReActAgentExecutor.java  2025/2/23 20:45
 */
@Slf4j
public class ReActAgentXmlExecutor extends BaseReActAgent {

    protected static final String DefaultTemplate = """
            Answer the following questions as best you can. 
            Remember not to fabricate tool execution results without execution tools.
            You have access to the following tools: {tools}
            
            Use the following XML format:
            If the tool execution result is not obtained, <Final Answer> and <Thought> should not appear
                Question: the input question you must answer
                <Thought> you should always think about what to do </Thought>
                <Action> the action to take, should be one of [{tool_names}] </Action>
                <Action Input> the input to the action </Action Input>
                Observation: the result of the action (If there is no result, return empty)
                ... (this <Thought>/<Action>/<Action Input>/Observation can repeat N times)
                <Thought> I now know the final answer (If you don't know the answer, don't show it) </Thought>
                <Final Answer> the final answer to the original input question (If there are no results, there is no need to show them) </Final Answer>
            
            
            During the process of answering questions, you need to follow the rules below:
                1. The "<Action>" tag should only contain the name of the tool used, without any other characters.
                2. Do not guess the answer, if you need to use an Action, wait for the user to provide the results of the Action as the next step's Observation. And do not provide the subsequent <Thought> and <Final Answer>.
                3. <Action Input> must Analyze based on the description in the input type schema
                4. If you need more information, use the query_knowledge_base tool.
                5. If the result is insufficient, consider using another tool or querying the knowledge base again.
                6. Once you have all necessary information, provide a final answer.
                7. 关于查询当前时间的问题要调用工具拿到结果再回答
                8. 当调用工具后得到结果后，返回结果中不用出现 Use the following format 里面的 [<Action> / <Action Input>]，防止干扰再次调用工具
                9. Always use XML-like tags to structure your response: <Thought>, <Action>, <Action Input>, <Final Answer>
            
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

    public ReActAgentXmlExecutor(AgentModel agentModel) {
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

        // 使用XML格式解析Action入参或者解析成功
        AgentOutput agentOutput = parseXmlResponse(resStr);
        return agentOutput;
    }

    /**
     * 解析XML格式的响应
     *
     * @param text
     * @return
     */
    private AgentOutput parseXmlResponse(String text) {
        if (!org.springframework.util.StringUtils.hasText(text)) {
            log.info("Parse LLM response to AgentOutput, result is empty");
            // LLM 返回空值，此时模型有问题，提前结束
            return new AgentFinish();
        }

        // 尝试匹配XML格式的Action和Action Input
        java.util.regex.Pattern xmlActionPattern = java.util.regex.Pattern.compile(
                "<Action>(.*?)</Action>\\s*<Action Input>(.*?)</Action Input>",
                java.util.regex.Pattern.DOTALL
        );

        java.util.regex.Matcher actionMatcher = xmlActionPattern.matcher(text);

        if (actionMatcher.find()) {
            // 提取动作和动作输入，并去除前后的空白字符
            String action = actionMatcher.group(1).trim();
            String actionInput = actionMatcher.group(2).trim();
            // 创建一个FunctionUseAction对象，表示需要使用工具执行的动作，并设置LLM的响应文本
            FunctionUseAction functionUseAction = new FunctionUseAction(action, actionInput);
            functionUseAction.setLlmResponse(text);
            // 返回表示需要使用工具的动作对象
            return functionUseAction;
        } else {
            // 尝试匹配XML格式的Final Answer
            java.util.regex.Pattern xmlFinalAnswerPattern = java.util.regex.Pattern.compile(
                    "<Final Answer>(.*?)</Final Answer>",
                    java.util.regex.Pattern.DOTALL
            );

            java.util.regex.Matcher finalAnswerMatcher = xmlFinalAnswerPattern.matcher(text);
            if (finalAnswerMatcher.find()) {
                // 提取最终答案的内容，并去除前后的空白字符
                String output = finalAnswerMatcher.group(1).trim();
                // 记录日志，表示下一步是最终答案
                log.info("Parse LLM response to AgentOutput, next step is final answer");
                // 创建一个AgentFinish对象，表示不需要使用工具，并设置最终答案和LLM的响应文本
                AgentFinish agentFinish = new AgentFinish();
                agentFinish.setResult(output);
                agentFinish.setLlmResponse(text);
                // 返回表示结束并包含最终答案的对象
                return agentFinish;
            } else {
                // 检查是否存在只有Action没有Action Input的情况
                if (java.util.regex.Pattern.compile("<Action>(.*?)</Action>", java.util.regex.Pattern.DOTALL).matcher(text)
                        .find()
                        && !java.util.regex.Pattern
                        .compile("<Action Input>(.*)</Action Input>", java.util.regex.Pattern.DOTALL)
                        .matcher(text).find()) {
                    // 如果存在，则抛出异常，表示格式无效
                    throw new OutputParserException("Invalid Format: Missing '<Action Input>' after '<Action>'"
                            + ": " + text);
                } else {
                    // 如果LLM的响应既不包含答案，也没有选择工具（即没有有效的Action），则表示缺少信息无法继续执行
                    // 此时也结束掉agent，将LLM的返回结果给用户，提示用户补全信息重新提问
                    log.info("解析对AgentOutput的LLM响应，没有足够的信息继续");
                    AgentFinish agentFinish = new AgentFinish();
                    agentFinish.setLlmResponse(text);
                    agentFinish.setResult(text);
                    // 返回表示结束的对象
                    return agentFinish;
                }
            }
        }
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
