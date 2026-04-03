package com.aaa.easyagent.biz.agent;

import com.aaa.easyagent.biz.agent.context.FunctionCallback;
import com.aaa.easyagent.biz.agent.context.SseHelper;
import com.aaa.easyagent.biz.agent.data.*;
import com.aaa.easyagent.biz.agent.service.ChatRecordSaver;
import com.aaa.easyagent.biz.function.FunctionToolManager;
import com.aaa.easyagent.common.config.exception.AgentException;
import com.aaa.easyagent.common.config.exception.AgentToolException;
import com.aaa.easyagent.common.llm.LLmModelSelector;
import com.aaa.easyagent.common.util.ChatResponseUtil;
import com.aaa.easyagent.common.util.JsonSchemaGenerator;
import com.aaa.easyagent.common.util.LoopDetector;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.util.StopWatch;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
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

    protected SseEmitter sse;

    /**
     * 决策轮数限制
     */
    public static final int DECISION_CNT_LIMIT = 20;
    /**
     * 卡住轮数限制
     */
    public static final int STUCK_CNT_LIMIT = 3;

    /**
     * 默认函数模板
     */
    public static final String DEFAULT_FUNCTION_TEMPLATE = """
            Tool name: {name}, description: {description}, input type schema: {schema}
            """;

    public static final PromptTemplate functionTemplate = new PromptTemplate(DEFAULT_FUNCTION_TEMPLATE);


    /**
     * 相同工具调用统计
     */
    protected Map<String, Integer> toolSameCallCountMap = new HashMap<>();

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

        sse = agentContext.getSseEmitter();
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
        return doExec(question);
    }


    private String doExec(String question) {
        // 根据agent选择模型
        if (chatModel == null) {
            throw new AgentException(agentContext.getModelType() + "无法匹配大模型");
        }

        SseHelper.sendLog(sse, "power by【{}】{} ...", agentContext.getModelType(), agentContext.getAgentModelConfig().getModelVersion());

        // 提示词构建
        prompt = this.buildPrompt();

        // 添加用户信息
        addUserMessage(question);

        // 限制决策轮数，防止无限调用
        int decisionCnt = 1;
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        while (decisionCnt < DECISION_CNT_LIMIT) {
            SseHelper.sendLog(sse, "第{}次大模型决策开始执行...", decisionCnt);
            AgentOutput agentOutput = null;

            try {
                // tool/react ... 等等多模式执行
                agentOutput = this.run();

                // 思考完成
                if (agentOutput instanceof AgentFinish agentFinish) {
                    SseHelper.sendLog(sse, "第{}次大模型决策结束...：", decisionCnt);
                    String llmResponse = agentFinish.getResult();
                    SseHelper.sendLog(sse, "第{}次大模型决策结果...：", decisionCnt, llmResponse);

                    // SseHelper.sendFinalAnswer(sse, agentFinish.getResult());
                    // ChatRecordSaver.addFinalAnswer(agentFinish.getResult());
                    stopWatch.stop();
                    ChatRecordSaver.saveAgentFinish(
                            agentFinish, agentContext.getAgentModelConfig().getModelVersion(),
                            null,
                            BigDecimal.valueOf(stopWatch.getTotalTimeSeconds()));

                    if (sse != null) {
                        sse.complete();
                    }
                    return llmResponse;
                }
            } catch (Exception e) {
                log.error("大模型执行异常:", e);
                SseHelper.sendData(sse, "第{}次大模型决策异常：{}", decisionCnt, e.getMessage());
                return null;
            }


            // 多次调用工具，并且工具返回的结果都一样，则认为模型卡住
            if (isStuck()) {
                SseHelper.sendData(sse, "模型可能卡住了，请检查!");
                return null;
            }

            decisionCnt++;
        }


        return null;
    }

    /**
     * 场景：
     * 捞取日志分析，先按月捞查不到，再按周捞查不到，再按天捞 ... 模型可能一直处于无限循环状态，
     * 这时检测 模型触发的工具参数是否重复，如果重复超3次，认为模型可能卡住了。
     *
     * @return true
     */
    private boolean isStuck() {
        for (Map.Entry<String, Integer> entry : toolSameCallCountMap.entrySet()) {
            if (entry.getValue() >= STUCK_CNT_LIMIT) {
                return true;
            }
        }

        return false;
    }


    protected void toolExecute(FunctionUseAction functionUseAction) {
        FunctionCallback functionToolCallback = callbackMap.get(functionUseAction.getAction());
        if (functionToolCallback == null) {
            throw new AgentToolException("无法匹配 toolFunction");
        }
        String callToolResult = functionToolCallback.call(functionUseAction.getActionInput());

        // 添加工具执行结果记忆
        List<ToolResponseMessage.ToolResponse> responses = new ArrayList<>();
        responses.add(new ToolResponseMessage.ToolResponse(
                UUID.randomUUID().toString(),
                functionUseAction.getAction(),
                callToolResult));

        ToolResponseMessage toolResponseMessage = ToolResponseMessage.builder().responses(responses).build();

        /*
         * deepseek： Messages with role 'tool' must be a response to a preceding message with 'tool_calls
         * 你当前的设计其实是 ReAct 模式的工具调用（用 XML 标签控制流程），但混合了 OpenAI 的 tool calling 格式，导致冲突。
         * deepseek 这里不能是tool
         */
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

    protected void addAssistantMessage(ChatResponse chatResponse) {
        AssistantMessage toolResponseMessage = new AssistantMessage(ChatResponseUtil.getResStr(chatResponse));
        prompt.getInstructions().add(toolResponseMessage);
    }

    protected void addAssistantMessage(String chatResponse) {
        AssistantMessage toolResponseMessage = new AssistantMessage(chatResponse);
        prompt.getInstructions().add(toolResponseMessage);
    }

    protected void addAssistantMessage(AgentOutput agentOutput) {
        if (agentOutput instanceof FunctionUseAction functionUseAction) {
            AssistantMessage toolResponseMessage = new AssistantMessage(agentOutput.getLlmResponse());

            String action = functionUseAction.getAction() + ":"
                    + LoopDetector.normalizeAndSHA256(JSONObject.parseObject(functionUseAction.getActionInput()));
            toolSameCallCountMap.put(action,
                    toolSameCallCountMap.getOrDefault(action, 0) + 1);

            prompt.getInstructions().add(toolResponseMessage);
            return;
        }
        AssistantMessage toolResponseMessage = new AssistantMessage(agentOutput.getLlmResponse());
        prompt.getInstructions().add(toolResponseMessage);
    }

    protected void addUserMessage(String question) {
        prompt.getInstructions().add(new UserMessage(question));
    }
}
