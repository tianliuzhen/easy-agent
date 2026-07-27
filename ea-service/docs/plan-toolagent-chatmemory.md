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

### 3.1 两套记忆机制在同一个 prompt 上叠加（核心冲突）

- `ToolAgentExecutor` 用 `this.messages`（进程内、单请求累加）经 `spec.messages()` 决定 prompt
- Advisor 想用 `ChatMemory`（跨请求持久化）在 `before()` 里往 prompt 合并历史
- 两者叠加 → 消息重复 / 丢失 / 顺序错乱

### 3.2 线程时序问题

- `run()` 里 `spec.messages(this.messages)` 在**主线程**先执行
- Advisor 的 `before()` 在 **scheduler 线程**（`.publishOn`）后执行
- 结论：**历史加载不能放在 `before()`**，否则 prompt 已经定型，改 `this.messages` 已来不及影响本轮
- 正确位置：`ToolAgentExecutor` **构造时**加载

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

## 六、下一步

1. 用户确定方案（A / B / C）
2. 按方案实现
3. 验证三个场景的请求体：
   - 首轮：`[system, user]`
   - 工具后第2轮：`[system, user, assistant(tool_calls), tool]`（不丢、不重复、顺序对）
   - 新请求同 sessionId：历史含上轮完整 `[..., assistant(tool_calls), tool, assistant(答案)]`
4. 关注隐患：长对话 >20 条时 MessageWindowChatMemory 截断（3.6）、工具重复执行（3.7）

## 七、相关文件

- `biz/agent/ToolAgentExecutor.java` —— 执行器，管 this.messages / prompt
- `biz/agent/advisor/ToolChatMemoryAdvisor.java` —— 自定义记忆 advisor
- `biz/agent/advisor/ToolExecutionAdvisor.java` —— 拦截 tool_call 写 this.messages
- `common/config/vectorstore/SimpleVectorStoreConfig.java` —— `InMemoryChatMemory` bean（MessageWindowChatMemory, maxMessages=20）
- 参考：`org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor`（官方源码）
