package com.aaa.easyagent.biz.agent;

import com.aaa.easyagent.biz.agent.context.SseHelper;
import com.aaa.easyagent.biz.agent.data.AgentContext;
import com.aaa.easyagent.biz.agent.data.AgentFinish;
import com.aaa.easyagent.biz.agent.data.AgentOutput;
import com.aaa.easyagent.biz.agent.data.FunctionUseAction;
import com.aaa.easyagent.biz.agent.parser.OutputParserException;
import com.aaa.easyagent.biz.agent.service.ChatRecordSaver;
import com.aaa.easyagent.biz.agent.wrapper.SceneWrapper;
import com.aaa.easyagent.common.config.exception.AgentToolException;
import com.aaa.easyagent.common.util.ChatResponseUtil;
import com.aaa.easyagent.core.domain.enums.ModelTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * @link com.aaa.easyagent.biz.agent.ReActAgentExecutor
 * <pre>
 * 是的，从多个角度来看，XML解析方式确实比之前的解析方式更加友好：
 *
 * 1. **结构化程度更高**：
 * - XML格式具有清晰的标签结构，例如`<Action>`、`<ActionInput>`、`<Final Answer>`，使得大模型的输出更容易解析和处理。
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
 * - 当XML格式不完整或错误时，更容易检测和定位问题，例如只存在`<Action>`标签而缺少`<ActionInput>`标签。
 *
 * @author liuzhen.tian
 * @version 1.0 ReActAgentExecutor.java  2025/2/23 20:45
 */
@Slf4j
public class ReActAgentXmlExecutor extends BaseReActAgent {

    protected static final String DefaultTemplate = """
             ## 任务指令
             请尽最大可能回答以下问题。请记住，在没有执行工具的情况下，不得编造工具执行的结果。
             你可以使用以下工具：{tools}
            
             ## 使用工具的响应格式要求
             如果需要调用工具，请严格按照以下 XML 格式进行回答：
             如果尚未获取工具执行结果，则响应中不应包含 <Final Answer> 和 <Thought> 标签。
             允许重复 <Thought> / <Action> / <ActionInput> / Observation 这一流程多次。
            
             <Question> 你需要回答的输入问题 </Question>
             <Thought> 你应该始终思考接下来该做什么 </Thought>
             <Action> 要执行的操作，必须是 [{tool_names}] 中的一个 </Action>
             <ActionInput> 操作的输入参数（需严格依据输入类型的描述进行分析并提供） </ActionInput>
             Observation: 操作执行的结果（请勿猜测答案。如无结果，则返回空）
             ...（上述 <Thought>/<Action>/<ActionInput>/Observation 步骤可重复 N 次）
             <Thought> 我现在知道了最终答案（如果不知道答案，请不要展示此项） </Thought>
             <Final Answer> 对原始输入问题的最终回答（如果没有可展示的结果，则无需展示此项） </Final Answer>
            
             ## 回答过程必须遵守的规则
             1. <Action> 标签内只能包含工具名称，不得有任何其他字符,<ActionInput> 必须基于输入类型的描述进行分析后再提供
             2. 请勿猜测答案。如需执行 Action，请等待用户将执行结果作为下一步的 Observation 提供给你，并且不要在本次响应中提供后续的 <Thought> 和 <Final Answer>。
             3. 如果结果不足，请考虑使用其他工具或再次查询知识库。
             4. 调用工具并得到结果后，在返回的结果中不得出现格式要求中的 <Action> 和 <ActionInput> 标签，以防干扰再次调用工具。
             5. 如果需要调用工具才需要始终使用类似 XML 的标签来构建你的响应：<Thought>、<Action>、<ActionInput>、<Final Answer>。
             6. 如果只是简单问题，如：你好，你能干啥，快速响应即可
            
             ## 响应前的分析与规划框架
             请在组织最终回答前，按以下框架进行内部思考（此部分无需在最终响应中展示，但用于指导你的回答逻辑）：
            
             总结用户的问题：
             [在此处提供问题的简要概述]
             [列出回答问题所需的主要信息]
             [列出可能有帮助的工具，并解释其原因]
             [如需使用多个工具，请概述计划使用的顺序]
             [注意与所访问工具或数据相关的任何潜在隐私或安全问题]
            
             开始！
            """;

    public static final PromptTemplate reactSystemPromptTemplate = new PromptTemplate(DefaultTemplate);

    public ReActAgentXmlExecutor(AgentContext agentContext) {
        super(agentContext);
    }


    /**
     * 构建提示词
     *
     * @return
     */
    public Prompt buildPrompt() {

        if (CollectionUtils.isEmpty(callbackMap)) {
            return new Prompt(messages);
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

    /**
     * reAct-决策模式
     *
     * @return
     */
    @Override
    public AgentOutput think() {
        StringBuilder resStr = new StringBuilder();
        StringBuilder reasoningContent = new StringBuilder();


        if (this.agentContext.isWithStream()) {
            CountDownLatch runOver = new CountDownLatch(1);

            chatModel.stream(prompt)
                    .bufferTimeout(20, Duration.ofMillis(100))  // 批量处理
                    .subscribe(SceneWrapper.wrapper(chatRes -> {
                                // 思考
                                String lineThinks = ChatResponseUtil.getReasoningContent(chatRes);
                                reasoningContent.append(lineThinks);
                                SseHelper.sendThink(sse, lineThinks);

                                // 结果
                                String lineResStr = ChatResponseUtil.getResStr(chatRes);
                                resStr.append(lineResStr);
                                SseHelper.sendData(sse, lineResStr);
                            }),
                            error -> {
                                // 执行异常
                                log.error("ReActAgentXmlExecutor.think.error:" + error.getMessage(), error);
                                runOver.countDown();
                                SseHelper.sendData(sse, "系统异常：" + error.getMessage());
                            }, () -> {
                                // 执行结束
                                runOver.countDown();
                                log.info("ReActAgentXmlExecutor.think.runOver");
                            });

            try {
                // 串行等待
                runOver.await();

                ChatRecordSaver.addThinking(reasoningContent.toString());
                ChatRecordSaver.addData(resStr.toString());
            } catch (InterruptedException e) {
                log.error("ReActAgentXmlExecutor.runOver.await.error:" + e.getMessage(), e);
            }
        } else {
            ChatResponse chatResponse = chatModel.call(prompt);
            // 结果
            String data = ChatResponseUtil.getResStr(chatResponse);
            resStr.append(data);

            // 思考过程
            String things = ChatResponseUtil.getReasoningContent(chatResponse);
            reasoningContent.append(things);

            ChatRecordSaver.addThinking(reasoningContent.toString());
            ChatRecordSaver.addData(data);
        }

        // 添加助手执行记忆
        addAssistantMessage(prompt, resStr.toString());

        // 使用XML格式解析Action入参或者解析成功
        AgentOutput agentOutput = parseXmlResponse(resStr.toString());
        agentOutput.setReasoningContent(reasoningContent.toString());
        return agentOutput;
    }

    /**
     * 解析XML格式的响应
     *
     * @param text
     * @return
     */
    public static AgentOutput parseXmlResponse(String text) {
        if (!org.springframework.util.StringUtils.hasText(text)) {
            log.info("Parse LLM response to AgentOutput, result is empty");
            // LLM 返回空值，此时模型有问题，提前结束
            return new AgentFinish();
        }

        // 检查是否已经有 Observation（表示已经执行过工具）
        boolean hasObservation = text.contains("Observation:");

        // 如果有 Observation，优先检查 Final Answer（这是工具执行后的最后一轮）
        if (hasObservation) {
            java.util.regex.Pattern xmlFinalAnswerPattern = java.util.regex.Pattern.compile(
                    "<Final Answer>(.*?)</Final Answer>",
                    java.util.regex.Pattern.DOTALL
            );
            java.util.regex.Matcher finalAnswerMatcher = xmlFinalAnswerPattern.matcher(text);

            if (finalAnswerMatcher.find()) {
                // 提取最终答案的内容，并去除前后的空白字符
                String output = finalAnswerMatcher.group(1).trim();
                // 记录日志，表示下一步是最终答案
                log.info("Parse LLM response to AgentOutput (with Observation), next step is final answer");
                // 创建一个AgentFinish对象，表示不需要使用工具，并设置最终答案和LLM的响应文本
                AgentFinish agentFinish = new AgentFinish();
                agentFinish.setResult(output);
                agentFinish.setLlmResponse(text);
                // 返回表示结束并包含最终答案的对象
                return agentFinish;
            }

            // 如果有 Observation 但没有 Final Answer，说明工具执行后还需要继续思考
            // 这种情况下继续尝试匹配 Action
            log.info("Has Observation but no Final Answer, continue to check for Action");
        }

        // 尝试匹配XML格式的Action和ActionInput（已修改为不带空格的标签名）
        java.util.regex.Pattern xmlActionPattern = java.util.regex.Pattern.compile(
                "<Action>(.*?)</Action>\\s*<ActionInput>(.*?)</ActionInput>",
                java.util.regex.Pattern.DOTALL
        );

        java.util.regex.Matcher actionMatcher = xmlActionPattern.matcher(text);

        if (actionMatcher.find()) {
            // 提取动作和动作输入，并去除前后的空白字符
            String action = actionMatcher.group(1).trim();
            String actionInput = actionMatcher.group(2).trim();

            // 记录日志，区分是否有Observation
            if (hasObservation) {
                log.info("Parse LLM response to AgentOutput (with Observation), next step is action: {}", action);
            } else {
                log.info("Parse LLM response to AgentOutput (no Observation), next step is action: {}", action);
            }

            // 创建一个FunctionUseAction对象，表示需要使用工具执行的动作，并设置LLM的响应文本
            FunctionUseAction functionUseAction = new FunctionUseAction(action, actionInput);
            functionUseAction.setLlmResponse(text);
            // 返回表示需要使用工具的动作对象
            return functionUseAction;
        } else {
            // 如果没有匹配到Action，尝试匹配XML格式的Final Answer
            // （注意：这里是没有Observation的情况下的Final Answer）
            java.util.regex.Pattern xmlFinalAnswerPattern = java.util.regex.Pattern.compile(
                    "<Final Answer>(.*?)</Final Answer>",
                    java.util.regex.Pattern.DOTALL
            );

            java.util.regex.Matcher finalAnswerMatcher = xmlFinalAnswerPattern.matcher(text);
            if (finalAnswerMatcher.find()) {
                // 提取最终答案的内容，并去除前后的空白字符
                String output = finalAnswerMatcher.group(1).trim();
                // 记录日志，表示下一步是最终答案
                log.info("Parse LLM response to AgentOutput (no Observation), next step is final answer");
                // 创建一个AgentFinish对象，表示不需要使用工具，并设置最终答案和LLM的响应文本
                AgentFinish agentFinish = new AgentFinish();
                agentFinish.setResult(output);
                agentFinish.setLlmResponse(text);
                // 返回表示结束并包含最终答案的对象
                return agentFinish;
            } else {
                // 检查是否存在只有Action没有ActionInput的情况
                if (java.util.regex.Pattern.compile("<Action>(.*?)</Action>", java.util.regex.Pattern.DOTALL).matcher(text)
                        .find()
                        && !java.util.regex.Pattern
                        .compile("<ActionInput>(.*)</ActionInput>", java.util.regex.Pattern.DOTALL)  // 修改为正则表达式
                        .matcher(text).find()) {
                    // 如果存在，则抛出异常，表示格式无效
                    throw new OutputParserException("Invalid Format: Missing '<ActionInput>' after '<Action>'"
                            + ": " + text);
                } else {
                    // 如果LLM的响应既不包含答案，也没有选择工具（即没有有效的Action），则表示缺少信息无法继续执行
                    // 此时也结束掉agent，将LLM的返回结果给用户，提示用户补全信息重新提问
                    log.info("Parse LLM response to AgentOutput, no valid Action or Final Answer found, return as is");
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

    protected void toolExecute(FunctionUseAction functionUseAction) {
        FunctionCallback functionToolCallback = callbackMap.get(functionUseAction.getAction());
        if (functionToolCallback == null) {
            throw new AgentToolException("无法匹配 toolFunction");
        }

        SseHelper.sendTool(sse, String.format("正在执行工具：%s，\n工具入参：%s", functionUseAction.getAction(), functionUseAction.getActionInput()));
        String callToolResult = functionToolCallback.call(functionUseAction.getActionInput());
        SseHelper.sendTool(sse, String.format("执行结果：%s", callToolResult));

        ChatRecordSaver.addToolCall(functionUseAction, callToolResult);
        if (ModelTypeEnum.deepseek == this.agentContext.getModelType()) {
            UserMessage userMessage = new UserMessage("Observation：" + callToolResult,
                    new ArrayList<>());
            messages.add(userMessage);

            // 所有工具结果用 role: "user" 返回，格式为 Observation: 结果
            /*
             * deepseek： Messages with role 'tool' must be a response to a preceding message with 'tool_calls
             * 你当前的设计其实是 ReAct 模式的工具调用（用 XML 标签控制流程），但混合了 OpenAI 的 tool calling 格式，导致冲突。
             * deepseek 这里不能是tool
             */
        } else {
            // 添加工具执行结果记忆
            List<ToolResponseMessage.ToolResponse> responses = new ArrayList<>();
            responses.add(new ToolResponseMessage.ToolResponse(
                    UUID.randomUUID().toString(),
                    functionUseAction.getAction(),
                    callToolResult));
            ToolResponseMessage toolResponseMessage = new ToolResponseMessage(responses);
            messages.add(toolResponseMessage);
        }


    }

    /**
     * react 执行模式：  思考/行动/观察...思考/行动/观察
     *
     * <pre>
     *     [
     *     // 第1轮：用户提问
     *     {
     *         "role": "user",
     *         "content": "直接查询当前时间"
     *     },
     *
     *     // 第2轮：模型要求调用工具
     *     {
     *         "role": "assistant",
     *         "content": "<Thought>需要查询当前时间</Thought>\n<Action>HTTP请求</Action>\n<ActionInput>{}</ActionInput>"
     *     },
     *
     *     // 第3轮：系统返回工具结果（用 user！）
     *     {
     *         "role": "user",
     *         "content": "Observation: Sun Mar 08 01:30:02 CST 2026"
     *     },
     *
     *     // 第4轮：模型给出最终答案
     *     {
     *         "role": "assistant",
     *         "content": "<Thought>已获取到时间</Thought>\n<Final Answer>当前时间是 Sun Mar 08 01:30:02 CST 2026</Final Answer>"
     *     }
     * ]
     * </pre>
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
