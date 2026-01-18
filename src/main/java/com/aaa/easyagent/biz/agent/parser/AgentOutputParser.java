package com.aaa.easyagent.biz.agent.parser;

import com.aaa.easyagent.biz.agent.data.AgentFinish;
import com.aaa.easyagent.biz.agent.data.AgentOutput;
import com.aaa.easyagent.biz.agent.data.FunctionUseAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author liuzhen.tian
 * @version 1.0 AgentOutputParser.java  2025/4/19 21:41
 */
@Slf4j
public class AgentOutputParser {

    public static final String FINAL_ANSWER_ACTION = "Final Answer:";


    public static final Pattern ACTION_PATTERN = Pattern.compile(
            "Action\\s*\\d*\\s*:[\\s]*(.*?)\\s*Action\\s*\\d*\\s*Input\\s*\\d*\\s*:[\\s]*(.*?)(?=\\s*```|\\s*Action\\s*\\d*\\s*:|\\s*Observation\\s*:|\\s*$)",
            Pattern.DOTALL
    );

    /**
     * FunctionUseAction 需要使用工具
     * AgentFinish       不需要使用工具
     *
     * @param text
     * @return
     */
    public static AgentOutput parse(String text) {
        if (!StringUtils.hasText(text)) {
            log.info("Parse LLM response to ReactAgentModel, result is empty");
            // LLM 返回空值，此时模型有问题，提前结束
            return new AgentFinish();
        }

        boolean includesAnswer = text.contains(FINAL_ANSWER_ACTION);
        Matcher actionMatcher = ACTION_PATTERN.matcher(text);

        if (actionMatcher.find()) {
            // 提取动作和动作输入，并去除前后的空白字符
            String action = actionMatcher.group(1).trim();
            String actionInput = actionMatcher.group(2).trim();
            // 创建一个FunctionUseAction对象，表示需要使用工具执行的动作，并设置LLM的响应文本
            FunctionUseAction functionUseAction = new FunctionUseAction(action, actionInput);
            functionUseAction.setLlmResponse(text);
            // 返回表示需要使用工具的动作对象
            return functionUseAction;
        }
        else if (includesAnswer) {
            // 提取最终答案的内容，并去除前后的空白字符
            String output = text.split(FINAL_ANSWER_ACTION)[1].trim();
            // 记录日志，表示下一步是最终答案
            log.info("Parse LLM response to ReactAgentModel, next step is final answer");
            // 创建一个AgentFinish对象，表示不需要使用工具，并设置最终答案和LLM的响应文本
            AgentFinish agentFinish = new AgentFinish();
            agentFinish.setResult(output);
            agentFinish.setLlmResponse(text);
            // 返回表示结束并包含最终答案的对象
            return agentFinish;
        }
        else {
            // 检查是否存在只有Action没有Action Input的情况
            if (Pattern.compile("Action\\s*\\d*\\s*:[\\s]*(.*?)", Pattern.DOTALL).matcher(text)
                    .find()
                    && !Pattern
                    .compile("[\\s]*Action\\s*\\d*\\s*Input\\s*\\d*\\s*:[\\s]*(.*)", Pattern.DOTALL)
                    .matcher(text).find()) {
                // 如果存在，则抛出异常，表示格式无效
                throw new OutputParserException("Invalid Format: Missing 'Action Input:' after 'Action:'"
                        + ": " + text);
            }
            else {
                // 如果LLM的响应既不包含答案，也没有选择工具（即没有有效的Action），则表示缺少信息无法继续执行
                // 此时也结束掉agent，将LLM的返回结果给用户，提示用户补全信息重新提问
                log.info("解析对ReactAgentModel的LLM响应，没有足够的信息继续");
                AgentFinish agentFinish = new AgentFinish();
                agentFinish.setLlmResponse(text);
                agentFinish.setResult(text);
                // 返回表示结束的对象
                return agentFinish;
            }
        }
    }
}
