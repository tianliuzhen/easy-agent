# ToolAgentExecutor 会话记忆（ChatMemory）问题排查与方案

> 状态：**待决策**（当前代码可编译，但记忆逻辑仍有 bug，等确定方向后再改）

## 一、需求

ToolAgentExecutor（基于 Spring AI ChatClient + Advisor）需要两种记忆，**两者都要**：

1. **单请求内多轮工具调用**：一次 `doExec()` 循环内 `tool_call → tool_result → 再推理`。
   本来就靠 `this.messages` 自然累加，不依赖 ChatMemory。
2. **跨请求多轮对话**：用户问完一个问题，下次再问时模型能记得上次对话（含工具结果）。
   需要 ChatMemory 持久化。

## 二、报错现象

发给 DeepSeek 的请求消息结构错乱，报：

```
Messages with role 'tool' must be a response to a preceding message with 'tool_calls'
```

观察到的错误请求体特征（多次出现，形态不一）：
- `[system, tool]` —— user 和 assistant(tool_calls) 丢失，只剩孤立 tool
- `[user, assistant(tool_calls), tool, system, tool]` —— system 位置错乱 + tool 重复
- user 消息在历史里被重复累加多次

## 三、根因分析

### 3.1 两套记忆机制在同一个 prompt 上叠加（**原判为核心冲突，已订正**）

- `ToolAgentExecutor` 用 `this.messages`（进程内、单请求累加）经 `spec.messages()` 决定 prompt
- Advisor 想用 `ChatMemory`（跨请求持久化）在 `before()` 里往 prompt 合并历史
- 两者叠加 → 消息重复 / 丢失 / 顺序错乱

> ⚠️ **订正（结合 5.6）**：此「叠加冲突」**只有在 advisor 真的于 `before()` 合并历史时才发生**。
> 当前 `ToolChatMemoryAdvisor.before()` 是**空实现、不碰 prompt** → 该冲突**当前并不成立**。
> 而 `[system, tool]` 400 的**真正核心成因**是自动注册的内置 `ToolCallingAdvisor` 截断（见 5.6），与「两套记忆叠加」无关。
> 因此 3.1 应降级为「**若改用官方 `MessageChatMemoryAdvisor`（其 `before()` 会合并历史）才会遇到的冲突**」，
> 而不是当前 bug 的根因。

### 3.2 线程时序问题（**原判理由错误，已订正**）

- `run()` 里 `spec.messages(this.messages)` 在**主线程**先执行
- Advisor 的 `before()` 在 **scheduler 线程**（`.publishOn`）后执行
- ~~结论：**历史加载不能放在 `before()`**，否则 prompt 已经定型，改 `this.messages` 已来不及影响本轮~~
- 正确位置：`ToolAgentExecutor` **构造时**加载

> ⚠️ **订正**：上面的「线程时序」因果是错的。
> `before()` 在 reactive 链中的执行顺序由**流编排**保证（`Mono.just(req).publishOn(scheduler).map(before).flatMapMany(nextStream)`），
> 它**必定先于模型调用**，与「在哪个线程」无关；且 `before()` 返回的 request **就是**发给模型的那个
> —— 官方 `MessageChatMemoryAdvisor` 正是在 `before()` 改 request 把历史注入 prompt 的。故「历史加载不能放在 `before()`」是**伪命题**。
>
> **唯一成立的窄结论**：在 `before()` 里改 `this.messages` 这个 List **没用**，因为 `run()` 里 `spec.messages(messages.toArray())` 已取快照。
>
> **构造时加载的真实理由**（保留该做法，但换正确理由）：本项目自定规则「**advisor 不直接改 request，历史只走 `this.messages` 通道**」，
> 所以历史必须在 `spec.messages()` 取快照**之前**（即构造时）灌好；这是**设计选择**，不是被线程时序所迫。

### 3.3 官方 MessageChatMemoryAdvisor 不认识 tool 消息

官方实现（纯聊天场景设计）：
- `before()`：加载历史 → 合并 prompt → system 置顶 → 存**当前 user 消息**（`getLastUserOrToolResponseMessage()`）
- `after()`：只存**聚合后的最终 assistant 消息**

工具场景一次问答有多轮 `run()`：

| doExec 轮次 | before 存什么 | after 存什么 |
|---|---|---|
| 第1轮 (tool call) | user | 聚合的 assistant（tool_calls 可能在聚合中丢失）|
| 第2轮 (最终答案) | `getLastUserOrToolResponseMessage()` → 取到 **tool 消息** | 最终 assistant |

第2轮 `before()` 存的是 tool 消息，但它前面对应的 `assistant(tool_calls)` 官方从没成功存过
→ ChatMemory 出现「孤立 tool」→ 下次加载 → **400**。

### 3.4 ChatClientMessageAggregator 的 response context 不含 MESSAGES_KEY

`MESSAGES_KEY` 是**请求级** advisor 参数，聚合后的 `ChatClientResponse` context 里没有它。
所以 `after()` 里 `response.context().get(MESSAGES_KEY)` 可能拿不到，需从 **request context** 捕获。

### 3.5 最终答案（纯文本）从没进 this.messages

`ToolExecutionAdvisor` 只在**有 tool_calls 时**往 `this.messages` 写。
最后一轮纯文本回复只进了 `run()` 里的 `resStr`（本地 StringBuilder），**没进 this.messages**。
→ 保存到 ChatMemory 的历史缺 AI 的总结回复。

### 3.6 MessageWindowChatMemory.add() 不是纯存储

`MessageWindowChatMemory`（`InMemoryChatMemory` bean，默认 `maxMessages=20`）的 `add()` 内部有：
- **SystemMessage 去重**：新增 system 会删掉旧 system
- **窗口截断**：超 20 条从头砍非 system 消息，snap 到 USER 边界

对连续多个 tool_calls 回合，截断的 snap 逻辑不一定安全（可能砍掉 assistant 却留下 tool）。
用 `clear() + add()` 做「快照替换」时会被这套逻辑干扰。当前问题消息数没到 20，非主因，但长对话时是隐患。

### 3.7 ToolExecutionAdvisor 重复执行工具（次要）

流式场景下 `doOnNext` 对带 tool_calls 的 chunk 可能触发多次 → 同一工具执行两次 → this.messages 里出现重复的 assistant+tool 对。

## 四、当前代码状态（已定的部分）

已确定并落地的职责划分（用户已确认「advisor 只负责跨请求持久化」）：

### ToolAgentExecutor（管 prompt）
- **构造时**：`chatMemory.get(sessionId)` → `messages.addAll(history)`（绕开线程时序）
- `run()`：`spec.messages(this.messages)` 是**唯一** prompt 来源，advisor 不碰
- `run()` 的 `spec.advisors()` 里补 `ChatMemory.CONVERSATION_ID = sessionId`
  （request 级会覆盖 builder 级默认参数，必须重设）

### ToolChatMemoryAdvisor（只管持久化，当前实现）
- `before()`：直接透传，**不改 prompt**
- `adviseStream()`：从 request context 捕获 `messages` 引用 → 用 `ChatClientMessageAggregator` 聚合
  → 回调里 `appendFinalAssistantIfAbsent()` 补最终答案 → `saveMessages()`（clear + add 全量）
- `after()`：非流式路径同理

**当前状态：可编译，但用户实测仍有 bug**（未定位到最后的具体形态，等决策后重测）。

## 五、待决策：tool 消息 / 保存策略怎么定

三个候选方向，需要用户拍板：

### 方案 A：this.messages 唯一真相，advisor 只做加载+整存
- 加载：构造时
- 保存：结束时把完整 this.messages 整存（含 tool_calls）
- 需解决：最终答案补进 this.messages（3.5）、避免多轮重复保存、绕开 window 截断
- 现状最接近这个方案

### 方案 B：before 照官方，after 改为存全量 this.messages
- `before()` 一字不改照抄官方（加载历史 + 合并 prompt + system 置顶 + 存 user）
- 只改 `after()`：从 context 取 this.messages，把本轮新增的 assistant(tool_calls)+tool+最终答案存进 ChatMemory
- 风险：before 合并 prompt 会和 ToolAgentExecutor 的 `spec.messages()` 叠加（回到 3.1 冲突）

### 方案 C：完全不用 advisor，ChatMemory 读写直接写在 ToolAgentExecutor
- 构造时 `chatMemory.get()` 加载
- `exec()`/`doExec` 结束后手动保存完整 this.messages（含补上的最终答案）
- 最可控，无线程 / 无聚合 / 无 advisor 链干扰
- 代价：不复用 Spring AI advisor 生态

## 五点五、重大发现：官方 ToolCallingAdvisor（2.0.0 新增）

### 背景
当前这套「外部 doExec 循环 + ToolExecutionAdvisor 手动拦截 + this.messages 手写拼接」，
是**当年 Spring AI 版本还没有 `ToolCallingAdvisor` 时的自造方案**。
`CommonLlmChatModel.internalStream` 里的框架递归被 `if(true) return`（第 218-221 行）**人为短路**
（作者当时觉得框架递归方式不合适），才改走外部手动方案。

Spring AI 2.0.0-RC2 已内置 `org.springframework.ai.chat.client.advisor.ToolCallingAdvisor`
（`spring-ai-client-chat-2.0.0-RC2.jar` 已确认存在），它就是官方版的「外部循环 + 工具执行 + 递归」，
且**实现正确**。

### ToolCallingAdvisor 逐条对上本项目痛点

| 手写方案 | ToolCallingAdvisor 官方能力 |
|---|---|
| `doExec` 外层 while 循环 | `adviseCall` 的 `do...while(isToolCall)` / `adviseStream` 的 `handleToolCallRecursion` 递归 |
| `ToolExecutionAdvisor` 手动拦截执行工具 | `toolCallingManager.executeToolCalls()` 官方执行 |
| `this.messages` 手写拼 assistant(tc)+tool（**拼错→400**） | `toolExecutionResult.conversationHistory()` 官方拼接，**结构合规不会 400** |
| 记忆 advisor 和工具循环打架 | `DEFAULT_ORDER = HIGHEST_PRECEDENCE + 300`，特意排在记忆 advisor **外层**，让记忆 advisor 不参与每轮工具迭代（源码 73 行注释）——天然兼容 `MessageChatMemoryAdvisor` |
| 中间 tool 响应要不要推前端 | `streamToolCallResponses(boolean)` 开关（默认 false=只推最终答案） |
| 自己管历史 vs 交给 ChatMemory | `conversationHistoryEnabled(false)` + 链上挂 ChatMemory advisor |
| 工具执行前后插钩子（SSE / 卡住检测 / 参数改写） | protected 钩子可继承覆盖：`doInitializeLoopStream` / `doBeforeStream` / `doAfterStream` / `doFinalizeLoopStream` / `doGetNextInstructionsForToolCallStream` |

### 关键设计点（源码要点）
- 只有 `prompt.getOptions() instanceof ToolCallingChatOptions` 才介入，否则透传（源码 129/235 行）
- 流式：`ChatClientMessageAggregator` 聚合本轮 → `handleToolCallRecursion` 判断有无 tool_calls
  → 有则 `executeToolCalls` + 用 `conversationHistory()` 递归 `internalStream`；无则结束
- `returnDirect` 的工具可直接把结果返回给应用端，不回灌模型
- 工具执行在 `Schedulers.boundedElastic()`（阻塞操作隔离）

### 方案 D（新增，倾向推荐）：改用 ToolCallingAdvisor
- `CommonLlmChatModel` 保持 `if(true) return`（工具执行不在 ChatModel 内做，等价官方 `internalToolExecutionEnabled=false`）
- **删除** `ToolExecutionAdvisor` + `ToolAgentExecutor.doExec` 外层循环 + `this.messages` 手写拼接
- **改用** `ToolCallingAdvisor`（或**继承它**覆盖 `doBeforeStream`/`doAfterStream` 等钩子接入 SSE 分类型推送、卡住检测）
- 记忆回归官方 `MessageChatMemoryAdvisor`（order 默认，排在 ToolCallingAdvisor 外层，历史结构由 `conversationHistory()` 保证合规，不再 400）
- 优点：根治 400（消息拼接交给框架）、记忆与工具循环天然分层、大幅简化代码
- 待确认：
  1. SSE 的 think/tool/data/log 分类型推送能否用 protected 钩子接入（当前 SSE 靠 `ToolExecutionAdvisor` 手动发）
  2. 项目工具（走 `callbackMap` / `FunctionCallbackAdapter`）能否被 `toolCallingManager.executeToolCalls` 正确解析执行
  3. 卡住检测（`toolSameCallCountMap`）、最大决策轮数、token 累计如何迁移到钩子里
  4. 作者当年觉得「框架递归不合适」的具体原因是否在新版 ToolCallingAdvisor 里已被钩子机制解决

## 五点六、反编译实证：内置 ToolCallingAdvisor **当前已在生效**，且是 `[system, tool]` 400 的直接成因

> 结论先行：五点五 把 ToolCallingAdvisor 当成「将来可选的方案 D」，但实测反编译发现——
> **它现在就已经被自动注册进 ChatClient 链路了**，即便 `CommonLlmChatModel.internalStream` 短路了
> ChatModel 级递归也拦不住它（那是两层不同的东西）。文档「二」里错误形态 #1
> `[system, tool]`（孤立 tool → DeepSeek 400）的**直接成因就是它**，而不仅是自造机制拼错。

### 5.6.1 两层工具执行，短路的只是其中一层
- `CommonLlmChatModel.internalStream` 的 `if(true) return`（180 行）短路的是 **ChatModel 级**的工具递归。
- 但 Spring AI 2.0 把工具执行**上移到了 ChatClient 的 advisor 层**：`ToolCallingAdvisor`。
  它由 `DefaultChatClientBuilder` 恒定持有、并在 `DefaultChatClient$DefaultChatClientRequestSpec.autoRegisterToolCallingAdvisor()` 里**自动注册**进链。
- 两层独立：短路了 ChatModel 层，ChatClient 层的 `ToolCallingAdvisor` 照样跑 → 工具被**执行两次**（它一次 + 自定义 `ToolExecutionAdvisor` 一次），且它负责的回灌把上下文截断。

### 5.6.2 为什么必然被自动注册（源码实证）
`autoRegisterToolCallingAdvisor()` 只有两个「不注册」的出口：
1. advisor param `spring.ai.chat.client.tool.calling.advisor.auto-register` 显式为 `Boolean.FALSE`（默认没设 → 注册）。
   - 常量：`ChatClientAttributes.TOOL_CALLING_ADVISOR_AUTO_REGISTER`，key = `spring.ai.chat.client.tool.calling.advisor.auto-register`。
2. 链上已存在实现了标记接口 `org.springframework.ai.chat.client.advisor.api.ToolAdvisor` 的 advisor。
   - 自定义 `ToolExecutionAdvisor` **没有实现** `ToolAdvisor` → 拦不住 → 仍会注册官方那个。

### 5.6.3 为什么 `conversationHistoryEnabled` 必然被设成 `false`（截断的开关）
注册时这行决定它：`conversationHistoryEnabled = !advisors.anyMatch(a -> a.getOrder() > toolOrder)`
- `ToolCallingAdvisor.DEFAULT_ORDER = -2147483348`（`Ordered.HIGHEST_PRECEDENCE + 300`，特意排在记忆 advisor 外层）。
- 本项目自定义 advisor(`ToolExecutionAdvisor` / `ToolChatMemoryAdvisor` / `SlidingWindowAdvisor`)都是 `order = 0`。
- `0 > -2147483348` 恒真 → `anyMatch` 为真 → `conversationHistoryEnabled = false`。
- **实质**：只要链上还有任意一个别的 advisor，官方就默认把它翻成 `false`（激进默认）。

### 5.6.4 `false` 分支如何截断出 `[system, tool]`（源码逐行）
`ToolCallingAdvisor.doGetNextInstructionsForToolCallStream`（源码 L398-404）:
```java
if (!this.conversationHistoryEnabled) {
    List<Message> history = toolExecutionResult.conversationHistory();
    if (history.isEmpty()) return history;
    return List.of(chatClientRequest.prompt().getSystemMessage(),   // 头：第一条 SystemMessage
                   history.get(history.size() - 1));                // 尾：conversationHistory 最后一条
}
return toolExecutionResult.conversationHistory();                    // true：完整、结构合规
```
- `conversationHistory()` 本是 **完整且合规** 的 `prompt.instructions + assistant(tool_calls) + toolResponse`。
- `false` 分支只留「头 + 尾」→ 工具轮的尾正是 tool 响应 → **`[system, tool]`**，
  中间的 `assistant(tool_calls)` 和用户本轮问题 `user` 全被丢弃 → 就是文档「二」的形态 #1。
- 头到底是什么：`Prompt.getSystemMessage()` 扫第一条 `SystemMessage`，没有则 `new SystemMessage("")`。
  所以保留的是 **system 提示词**（本例「查询黄金白银价格助手」），**不是**用户第一个问题。

### 5.6.5 Spring 的设计前提，以及它在本项目为何不成立
`false` 不是「就发这两条」，而是官方假设：**链上有个 memory advisor 会在递归时把结构合规的完整历史重新注入 prompt**
（递归走 `internalStream` → `chainCopy = streamAdvisorChain.copy(this)`（排除自己）→ `new Prompt(nextInstructions)` 再过其余 advisor）。
即工具 advisor 只带「增量」，历史由 memory advisor 补，避免历史塞两遍。这在官方 `MessageChatMemoryAdvisor`（其 `before()` 会 prepend 历史）下成立。

本项目不成立的原因：`ToolChatMemoryAdvisor.before()` 是**故意的空实现**（「prompt 完全由 this.messages 决定，advisor 绝不修改 prompt」），
递归时没有任何人补历史 → `[system, tool]` 裸奔直达模型 → DeepSeek 立即 **400**（`tool` 前无 `tool_calls`，本项目 `BaseAgent.toolExecute` 注释已记此约束）。

### 5.6.6 对方案的影响与修正
- **修正 5.5「方案 D」表格中一格**：`conversationHistoryEnabled(false) + 链上挂 ChatMemory advisor` 前提写得不全。
  `false` 仅在 memory advisor **真的会 re-inject 结构合规的完整历史** 时才安全;
  否则（空实现 / 顺序脆弱 / 无 memory advisor）`false` 恰恰是 400 的源头。
  → **若走方案 D，应显式 `conversationHistoryEnabled(true)`**（每轮回灌完整 `conversationHistory()`，结构由框架保证），
    而不是依赖 `false` + 记忆 advisor 补。
- **方案 A / C（保持手动路线）的落地开关**：在 `run()` 的**请求级** `spec.advisors(as -> ...)` 里加
  `as.param(ChatClientAttributes.TOOL_CALLING_ADVISOR_AUTO_REGISTER.getKey(), false)`
  （请求级会覆盖 builder 级默认，且 `autoRegisterToolCallingAdvisor` 读的就是 `advisorParams`，必须放这里）。
  禁用后内置 advisor 不介入 → 工具只执行一次、历史与顺序完全由 `doExec` + `this.messages` 掌控。

### 5.6.7 证据文件与关键行
- `ToolCallingAdvisor.java`：`doGetNextInstructionsForToolCallStream` L398-404（截断）、`internalStream`/`handleToolCallRecursion`（递归 + `copy(this)`）、`DEFAULT_ORDER = -2147483348`
- `DefaultChatClient$DefaultChatClientRequestSpec.autoRegisterToolCallingAdvisor()`：注册闸门（auto-register param / `ToolAdvisor` anyMatch / `conversationHistoryEnabled = !anyMatch(order>toolOrder)`）
- `ChatClientAttributes`：枚举常量 `TOOL_CALLING_ADVISOR_AUTO_REGISTER`
- `Prompt.getSystemMessage()`：取第一条 SystemMessage，缺则 `new SystemMessage("")`
- `DefaultChatClientUtils.toChatClientRequest`：`System + messages + user`，**不过滤**（排除 spec.messages 侧丢消息的嫌疑）

### 5.6.8 Spring AI `ToolCallingAdvisor` 关键源码原文摘录（2.0.0-RC2）

> 取自 `spring-ai-client-chat-2.0.0-RC2-sources.jar`，仅摘关键行并标注。

**(1) 默认不把中间 tool_call 响应输出到下游 stream**（`streamWithToolCallResponses` 的 filter，`streamToolCallResponses` Builder 默认 `false`）
```java
.filter(ccr -> this.streamToolCallResponses                                   // true：全放行
               || !this.toolExecutionEligibilityChecker.isToolCallResponse(ccr.chatResponse())); // false：滤掉 tool_call 响应
```
→ 默认 `false`：带 tool_calls 的响应块被滤掉，下游 `stream()` 只看得到最终答案，工具调用轮对应用层**不可见**。
  项目若要在 `doOnNext` 捕获工具调用（名字/入参/id）做 SSE 推送，需显式 `streamToolCallResponses(true)`。

**(2) 会被 ChatClient 自动注册（内置加载）**（`DefaultChatClient$DefaultChatClientRequestSpec.autoRegisterToolCallingAdvisor`，据反编译还原）
```java
// 仅当满足其一才「不注册」，否则自动把内置 ToolCallingAdvisor 加进链：
if (Boolean.FALSE.equals(advisorParams.get(
        ChatClientAttributes.TOOL_CALLING_ADVISOR_AUTO_REGISTER.getKey()))) return; // ① 显式关掉
if (advisors.stream().anyMatch(a -> a instanceof ToolAdvisor)) return;             // ② 链上已有 ToolAdvisor
advisors.add(toolCallingAdvisorBuilder.copy()... .build());                        // 否则：注册
```
→ 自定义 `ToolExecutionAdvisor` **没实现** `ToolAdvisor` 标记接口 → 闸门 ② 拦不住 → 官方版被自动注册。
  这就是 5.6.1–5.6.5 里 `[system, tool]` 截断的来源；关闭方式见 5.6.6（请求级 param 设 false）。

## 六、下一步

1. 用户确定方案（A / B / C / **D=改用 ToolCallingAdvisor，倾向推荐**）
2. 若选 D，先验证方案 D 的 4 个「待确认」点（尤其 SSE 钩子 + 工具解析）
3. 按方案实现
4. 验证三个场景的请求体：
   - 首轮：`[system, user]`
   - 工具后第2轮：`[system, user, assistant(tool_calls), tool]`（不丢、不重复、顺序对）
   - 新请求同 sessionId：历史含上轮完整 `[..., assistant(tool_calls), tool, assistant(答案)]`
5. 关注隐患：长对话 >20 条时 MessageWindowChatMemory 截断（3.6）、工具重复执行（3.7）

## 七、相关文件

- `ea-service/src/main/java/com/aaa/easyagent/biz/agent/ToolAgentExecutor.java` —— 执行器，管 this.messages / prompt / doExec 外层循环
- `ea-service/src/main/java/com/aaa/easyagent/biz/agent/advisor/ToolChatMemoryAdvisor.java` —— 自定义记忆 advisor
- `ea-service/src/main/java/com/aaa/easyagent/biz/agent/advisor/ToolExecutionAdvisor.java` —— 手动拦截 tool_call 写 this.messages（方案 D 下可删）
- `ea-service/src/main/java/com/aaa/easyagent/common/llm/common/CommonLlmChatModel.java` —— 自定义 ChatModel；`internalStream` 第 218-221 行 `if(true) return` 短路了框架工具递归（作者主动为之）；`internalCall` 递归仍保留
- `ea-service/src/main/java/com/aaa/easyagent/common/llm/LLmModelSelector.java` —— 构建 CommonLlmChatModel，注入 `ToolCallingManager` bean
- `ea-service/src/main/java/com/aaa/easyagent/common/config/vectorstore/SimpleVectorStoreConfig.java` —— `InMemoryChatMemory` bean（MessageWindowChatMemory, maxMessages=20）
- 参考：`org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor`（官方记忆 advisor 源码）
- 参考：`org.springframework.ai.chat.client.advisor.ToolCallingAdvisor`（**官方工具循环 advisor，2.0.0 新增，方案 D 核心**）
- 参考：`org.springframework.ai.ollama.OllamaChatModel`（2.0.0 已移除内部工具递归，Builder.toolCallingManager 标记 `@Deprecated`，注释指向 ToolCallingAdvisor）
