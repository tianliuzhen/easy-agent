// package com.aaa.easyagent.biz.agent.zexample;
//
// /**
//  * @author liuzhen.tian
//  * @version 1.0 BaseReActAgent.java  2026/1/10 23:08
//  */
// import lombok.Data;
//
// import java.util.*;
//
// /**
//  * 真实的ReAct代理实现
//  * 使用大模型自动进行思考-行动循环
//  */
// public abstract class BaseReActAgent extends BaseAgent {
//
//     protected LLMService llmService;
//     protected List<Tool> availableTools;
//
//     protected BaseReActAgent(ReActBuilder builder) {
//         super(builder);
//         this.llmService = new LLMService();
//         this.availableTools = new ArrayList<>();
//     }
//
//     /**
//      * 执行单个步骤：由大模型决定思考和行动
//      */
//     @Override
//     public String step() {
//         try {
//             // 1. 构建当前思考的上下文
//             String thought = awaitThink();
//
//             // 2. 基于思考决定行动
//             if (shouldAct(thought)) {
//                 String action = awaitAct(thought);
//                 return action;
//             }
//
//             return thought;
//
//         } catch (Exception e) {
//             logger.error("Step execution failed", e);
//             return "Error: " + e.getMessage();
//         }
//     }
//
//     /**
//      * 思考阶段：大模型分析当前状态
//      */
//     protected String awaitThink() {
//         // 构建思考提示
//         String thinkPrompt = buildThinkPrompt();
//
//         // 调用大模型进行思考
//         LLMResponse response = llmService.generateThought(
//                 thinkPrompt,
//                 getMessages(),
//                 getSystemPrompt()
//         );
//
//         // 解析思考结果
//         ThoughtResult thoughtResult = parseThought(response);
//
//         // 记录思考过程到内存
//         updateMemory("assistant", "[思考] " + thoughtResult.getReasoning());
//
//         return thoughtResult.getReasoning();
//     }
//
//     /**
//      * 行动阶段：大模型决定并执行行动
//      */
//     protected String awaitAct(String thought) {
//         // 基于思考决定行动
//         ActionDecision decision = decideAction(thought);
//
//         switch (decision.getActionType()) {
//             case TOOL_USE:
//                 return executeTool(decision);
//
//             case RESPONSE:
//                 return generateResponse(decision);
//
//             case QUERY_USER:
//                 return queryUser(decision);
//
//             case FINISH:
//                 setState(AgentState.FINISHED);
//                 return decision.getContent();
//
//             default:
//                 return "No action taken";
//         }
//     }
//
//     /**
//      * 决定是否需要行动
//      */
//     protected boolean shouldAct(String thought) {
//         // 分析思考内容，决定是否需要外部行动
//         // 例如：如果思考中提到需要调用工具或询问用户，则需要行动
//         return thought.contains("需要调用") ||
//                 thought.contains("需要询问") ||
//                 thought.contains("Action:");
//     }
//
//     /**
//      * 构建思考提示
//      */
//     protected String buildThinkPrompt() {
//         StringBuilder prompt = new StringBuilder();
//
//         prompt.append("你是一个AI助手，请分析当前对话状态并思考下一步该做什么。\n");
//         prompt.append("当前状态：").append(getState()).append("\n");
//         prompt.append("可用工具：").append(getAvailableToolsDescription()).append("\n");
//         prompt.append("思考要求：分析当前问题，决定是否需要调用工具、询问用户更多信息，还是直接回答。\n");
//
//         if (getNextStepPrompt() != null) {
//             prompt.append("\n").append(getNextStepPrompt()).append("\n");
//         }
//
//         return prompt.toString();
//     }
//
//     /**
//      * 解析大模型的思考结果
//      */
//     protected ThoughtResult parseThought(LLMResponse response) {
//         // 从大模型响应中提取结构化思考
//         String content = response.getContent();
//
//         ThoughtResult result = new ThoughtResult();
//
//         // 解析思考链 (Chain of Thought)
//         if (content.contains("思考：")) {
//             String reasoning = content.split("思考：")[1].split("\\n")[0].trim();
//             result.setReasoning(reasoning);
//         } else {
//             result.setReasoning(content);
//         }
//
//         // 解析建议的行动
//         if (content.contains("行动：")) {
//             String action = content.split("行动：")[1].split("\\n")[0].trim();
//             result.setSuggestedAction(action);
//         }
//
//         // 解析行动输入
//         if (content.contains("输入：")) {
//             String input = content.split("输入：")[1].split("\\n")[0].trim();
//             result.setActionInput(input);
//         }
//
//         return result;
//     }
//
//     /**
//      * 基于思考决定具体行动
//      */
//     protected ActionDecision decideAction(String thought) {
//         // 使用规则引擎或小模型决定行动类型
//         if (thought.contains("调用工具")) {
//             String toolName = extractToolName(thought);
//             Tool tool = findTool(toolName);
//
//             if (tool != null) {
//                 return ActionDecision.toolUse(tool, extractToolInput(thought));
//             }
//         }
//
//         if (thought.contains("需要更多信息") || thought.contains("询问用户")) {
//             String question = extractQuestion(thought);
//             return ActionDecision.queryUser(question);
//         }
//
//         if (thought.contains("可以回答") || thought.contains("直接回答")) {
//             return ActionDecision.response(generateAnswer(thought));
//         }
//
//         if (thought.contains("任务完成") || thought.contains("结束对话")) {
//             return ActionDecision.finish("任务完成");
//         }
//
//         // 默认：生成响应
//         return ActionDecision.response(thought);
//     }
//
//     /**
//      * 执行工具调用
//      */
//     protected String executeTool(ActionDecision decision) {
//         Tool tool = decision.getTool();
//         String input = decision.getActionInput();
//
//         try {
//             // 调用工具
//             String result = tool.execute(input);
//
//             // 将工具调用和结果添加到内存
//             updateMemory("tool",
//                     String.format("调用工具[%s]，输入：%s", tool.getName(), input));
//             updateMemory("tool",
//                     String.format("工具[%s]返回：%s", tool.getName(), result));
//
//             return result;
//
//         } catch (Exception e) {
//             logger.error("Tool execution failed: " + tool.getName(), e);
//             return String.format("工具[%s]执行失败：%s", tool.getName(), e.getMessage());
//         }
//     }
//
//     /**
//      * 生成最终回答
//      */
//     protected String generateResponse(ActionDecision decision) {
//         String content = decision.getContent();
//
//         // 如果需要，可以让大模型优化回答
//         if (shouldRefineResponse()) {
//             content = llmService.refineResponse(content, getMessages());
//         }
//
//         // 添加到内存
//         updateMemory("assistant", content);
//
//         return content;
//     }
//
//     /**
//      * 向用户提问
//      */
//     protected String queryUser(ActionDecision decision) {
//         String question = decision.getContent();
//
//         // 记录需要用户输入
//         updateMemory("assistant", question);
//
//         // 在实际系统中，这里会等待用户输入
//         // 对于演示，我们模拟一个等待状态
//         setState(AgentState.WAITING_FOR_INPUT);
//
//         return "[等待用户输入] " + question;
//     }
//
//     /**
//      * 获取可用工具描述
//      */
//     protected String getAvailableToolsDescription() {
//         if (availableTools.isEmpty()) {
//             return "无可用工具";
//         }
//
//         StringBuilder desc = new StringBuilder();
//         for (Tool tool : availableTools) {
//             desc.append(String.format("- %s: %s\n",
//                     tool.getName(), tool.getDescription()));
//         }
//         return desc.toString();
//     }
//
//     // 辅助方法
//     protected String extractToolName(String thought) {
//         // 从思考中提取工具名称
//         // 实际实现可能需要更复杂的NLP
//         for (Tool tool : availableTools) {
//             if (thought.contains(tool.getName())) {
//                 return tool.getName();
//             }
//         }
//         return null;
//     }
//
//     protected String extractToolInput(String thought) {
//         // 从思考中提取工具输入
//         // 简化实现
//         if (thought.contains("输入：")) {
//             return thought.split("输入：")[1].split("\\n")[0].trim();
//         }
//         return "{}";
//     }
//
//     protected String extractQuestion(String thought) {
//         // 从思考中提取问题
//         if (thought.contains("询问：")) {
//             return thought.split("询问：")[1].split("\\n")[0].trim();
//         }
//         return "请提供更多信息";
//     }
//
//     protected String generateAnswer(String thought) {
//         // 基于思考生成回答
//         // 在实际系统中，会调用大模型生成
//         return llmService.generateAnswer(thought, getMessages());
//     }
//
//     protected Tool findTool(String toolName) {
//         return availableTools.stream()
//                 .filter(t -> t.getName().equals(toolName))
//                 .findFirst()
//                 .orElse(null);
//     }
//
//     protected boolean shouldRefineResponse() {
//         // 决定是否需要优化回答
//         return getMessages().size() > 2;
//     }
//
//     // 添加工具
//     public void addTool(Tool tool) {
//         availableTools.add(tool);
//     }
//
//     public void addTools(List<Tool> tools) {
//         availableTools.addAll(tools);
//     }
//
//     // 构建器
//     public static abstract class ReActBuilder<T extends ReActBuilder<T>>
//             extends BaseAgent.Builder<T> {
//
//         protected List<Tool> tools = new ArrayList<>();
//
//         public T tools(List<Tool> tools) {
//             this.tools = tools;
//             return self();
//         }
//
//         @Override
//         public abstract BaseReActAgent build();
//     }
// }
//
// // 支持类定义
//
// /**
//  * 大模型服务接口
//  */
// interface LLMService {
//     LLMResponse generateThought(String prompt, List<Message> history, String systemPrompt);
//     String generateAnswer(String thought, List<Message> history);
//     String refineResponse(String response, List<Message> history);
// }
//
// /**
//  * 大模型响应
//  */
// @Data
// class LLMResponse {
//     private String content;
//     private Map<String, Object> metadata;
//
//     // getters and setters
// }
//
// /**
//  * 思考结果
//  */
// @Data
// class ThoughtResult {
//     private String reasoning;          // 推理过程
//     private String suggestedAction;    // 建议的行动
//     private String actionInput;        // 行动输入
//
//     // getters and setters
// }
//
// /**
//  * 行动决策
//  */
// class ActionDecision {
//     enum ActionType {
//         TOOL_USE,      // 使用工具
//         RESPONSE,      // 生成回答
//         QUERY_USER,    // 询问用户
//         FINISH         // 完成任务
//     }
//
//     private ActionType actionType;
//     private Tool tool;                 // 对于TOOL_USE
//     private String content;           // 对于RESPONSE或QUERY_USER
//     private String actionInput;       // 对于TOOL_USE
//
//     // 工厂方法
//     public static ActionDecision toolUse(Tool tool, String input) {
//         ActionDecision decision = new ActionDecision();
//         decision.actionType = ActionType.TOOL_USE;
//         decision.tool = tool;
//         decision.actionInput = input;
//         return decision;
//     }
//
//     public static ActionDecision response(String content) {
//         ActionDecision decision = new ActionDecision();
//         decision.actionType = ActionType.RESPONSE;
//         decision.content = content;
//         return decision;
//     }
//
//     public static ActionDecision queryUser(String question) {
//         ActionDecision decision = new ActionDecision();
//         decision.actionType = ActionType.QUERY_USER;
//         decision.content = question;
//         return decision;
//     }
//
//     public static ActionDecision finish(String message) {
//         ActionDecision decision = new ActionDecision();
//         decision.actionType = ActionType.FINISH;
//         decision.content = message;
//         return decision;
//     }
//
//     // getters
// }
//
// /**
//  * 工具接口
//  */
// interface Tool {
//     String getName();
//     String getDescription();
//     String execute(String input) throws Exception;
// }
//
// /**
//  * 具体的工具实现示例
//  */
// class CalculatorTool implements Tool {
//     @Override
//     public String getName() {
//         return "calculator";
//     }
//
//     @Override
//     public String getDescription() {
//         return "执行数学计算，支持加减乘除";
//     }
//
//     @Override
//     public String execute(String input) {
//         try {
//             // 解析输入（假设是JSON）
//             Map<String, Object> params = parseInput(input);
//             String operation = (String) params.get("operation");
//             double num1 = Double.parseDouble(params.get("num1").toString());
//             double num2 = Double.parseDouble(params.get("num2").toString());
//
//             double result;
//             switch (operation) {
//                 case "add": result = num1 + num2; break;
//                 case "subtract": result = num1 - num2; break;
//                 case "multiply": result = num1 * num2; break;
//                 case "divide": result = num1 / num2; break;
//                 default: throw new IllegalArgumentException("无效的操作: " + operation);
//             }
//
//             return String.format("计算结果: %.2f", result);
//
//         } catch (Exception e) {
//             return "计算失败: " + e.getMessage();
//         }
//     }
//
//     private Map<String, Object> parseInput(String input) {
//         // 简化的JSON解析
//         Map<String, Object> map = new HashMap<>();
//         // 实际应该使用JSON库
//         return map;
//     }
// }
//
// class SearchTool implements Tool {
//     @Override
//     public String getName() {
//         return "search";
//     }
//
//     @Override
//     public String getDescription() {
//         return "搜索互联网信息";
//     }
//
//     @Override
//     public String execute(String input) {
//         // 模拟搜索
//         return "搜索结果：找到10条相关信息";
//     }
// }
//
// /**
//  * 具体的ReAct代理实现示例
//  */
// class SmartAssistantAgent extends BaseReActAgent {
//
//     private SmartAssistantAgent(ReActBuilder builder) {
//         super(builder);
//         // 添加默认工具
//         addTools(builder.tools);
//     }
//
//     @Override
//     protected String buildThinkPrompt() {
//         // 定制思考提示
//         return super.buildThinkPrompt() +
//                 "\n请按照以下格式输出思考结果：\n" +
//                 "思考：[你的推理过程]\n" +
//                 "结论：[是否需要行动]\n" +
//                 "行动：[如果需要，建议的行动]";
//     }
//
//     // 可以覆盖其他方法来自定义行为
//
//     // 构建器
//     public static class Builder extends ReActBuilder<Builder> {
//         @Override
//         protected Builder self() {
//             return this;
//         }
//
//         @Override
//         public BaseReActAgent build() {
//             return new SmartAssistantAgent(this);
//         }
//     }
//
//     /**
//      * 使用示例
//      */
//     public static void main(String[] args) {
//         // 创建工具
//         List<Tool> tools = Arrays.asList(
//                 new CalculatorTool(),
//                 new SearchTool()
//         );
//
//         // 创建ReAct代理
//         SmartAssistantAgent agent = new SmartAssistantAgent.Builder()
//                 .name("SmartAssistant")
//                 .description("智能助手，使用ReAct模式自动思考")
//                 .systemPrompt("你是一个有帮助的AI助手，请仔细思考后再行动")
//                 .maxSteps(20)
//                 .tools(tools)
//                 .build();
//
//         // 运行代理
//         System.out.println("=== 对话开始 ===");
//
//         // 第一轮：用户提问
//         String result1 = agent.run("帮我计算一下：25乘以38等于多少？");
//         System.out.println("Agent: " + result1);
//
//         // 检查代理的状态和思考过程
//         System.out.println("\n=== 代理状态 ===");
//         System.out.println("当前步骤: " + agent.getCurrentStep());
//         System.out.println("当前状态: " + agent.getState());
//
//         System.out.println("\n=== 对话历史 ===");
//         agent.getMessages().forEach(msg ->
//                 System.out.println(msg.getRole() + ": " + msg.getContent())
//         );
//     }
// }
//
// /**
//  * 更高级的ReAct代理，支持工具学习
//  */
// class LearningReActAgent extends BaseReActAgent {
//
//     private ToolLearner toolLearner;
//     private Map<String, Tool> learnedTools;
//
//     private LearningReActAgent(ReActBuilder builder) {
//         super(builder);
//         this.toolLearner = new ToolLearner();
//         this.learnedTools = new HashMap<>();
//     }
//
//     @Override
//     protected String awaitThink() {
//         // 增强的思考：考虑学习新工具
//         String baseThought = super.awaitThink();
//
//         // 分析是否需要学习新工具
//         if (shouldLearnNewTool(baseThought)) {
//             Tool newTool = toolLearner.learnFromContext(baseThought, getMessages());
//             if (newTool != null) {
//                 learnedTools.put(newTool.getName(), newTool);
//                 addTool(newTool);
//                 return baseThought + "\n[已学习新工具: " + newTool.getName() + "]";
//             }
//         }
//
//         return baseThought;
//     }
//
//     @Override
//     protected ActionDecision decideAction(String thought) {
//         // 增强的行动决策：考虑使用学习到的工具
//         ActionDecision decision = super.decideAction(thought);
//
//         if (decision.getActionType() == ActionDecision.ActionType.TOOL_USE) {
//             Tool tool = decision.getTool();
//             if (tool == null) {
//                 // 尝试使用学习到的工具
//                 String toolName = extractToolName(thought);
//                 tool = learnedTools.get(toolName);
//                 if (tool != null) {
//                     return ActionDecision.toolUse(tool, decision.getActionInput());
//                 }
//             }
//         }
//
//         return decision;
//     }
//
//     private boolean shouldLearnNewTool(String thought) {
//         // 判断是否需要学习新工具
//         return thought.contains("需要新功能") ||
//                 thought.contains("没有合适的工具") ||
//                 thought.contains("无法完成");
//     }
//
//     // 工具学习器
//     static class ToolLearner {
//         Tool learnFromContext(String thought, List<Message> history) {
//             // 从上下文中学习新工具
//             // 实际实现会调用大模型分析
//             return null; // 简化
//         }
//     }
//
//     // 构建器
//     public static class Builder extends ReActBuilder<Builder> {
//         @Override
//         protected Builder self() {
//             return this;
//         }
//
//         @Override
//         public BaseReActAgent build() {
//             return new LearningReActAgent(this);
//         }
//     }
// }
