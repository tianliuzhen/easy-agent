package com.aaa.easyagent.biz.agent.trace;

import com.aaa.easyagent.core.domain.enums.ChatTraceType;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 一轮对话的执行轨迹收集器。
 * <p>
 * 每次对话创建一个实例，挂在 {@code AgentContext} 上随执行链路传递。
 * <p>
 * 为什么不是 static ThreadLocal：Tool 模式下思考与工具调用产生在 reactor 线程
 * （{@code ToolAgentExecutor} 的 stream.subscribe 回调、{@code ToolExecutionAdvisor}
 * 的响应式链），而保存动作发生在请求线程。ThreadLocal 按线程隔离，
 * reactor 线程写入的内容请求线程读不到，表现为「思考与工具记录刷新后全部丢失」。
 * 改成实例后跨线程写入同一个收集器，问题消失。
 * <p>
 * 多 Agent 编排：用 {@link #scopedTo} 派生带来源标识的视图，底层列表共享，
 * 于是各节点的轨迹既汇总在一轮对话里，又各自带着 Agent 与节点标识。
 * <p>
 * 线程安全：写操作在共享列表上加锁。流式 chunk 频率高但单次操作极轻；
 * 用锁而不是并发集合，是因为「能并入最后一条就并入」这个判断本身需要原子性。
 */
public class ChatTraceRecorder {

    /** 轨迹列表，scoped 视图与根收集器共享同一个实例 */
    private final List<ChatTraceEntry> entries;

    /** 本视图写入时使用的来源 */
    @Getter
    private final ChatTraceSource source;

    public ChatTraceRecorder() {
        this(new ArrayList<>(), ChatTraceSource.UNKNOWN);
    }

    public ChatTraceRecorder(ChatTraceSource source) {
        this(new ArrayList<>(), source);
    }

    private ChatTraceRecorder(List<ChatTraceEntry> entries, ChatTraceSource source) {
        this.entries = entries;
        this.source = source == null ? ChatTraceSource.UNKNOWN : source;
    }

    /**
     * 派生一个指定来源的视图，共享同一份轨迹列表。
     *
     * @param scopedSource 该视图的轨迹来源
     * @return 共享底层列表、来源被替换的收集器
     */
    public ChatTraceRecorder scopedTo(ChatTraceSource scopedSource) {
        return new ChatTraceRecorder(entries, scopedSource);
    }

    /** 记录思考过程片段，连续同来源的片段合并为一条 */
    public void recordThinking(String chunk) {
        appendMergeable(ChatTraceType.THINKING, chunk);
    }

    /** 记录流式回答片段，连续同来源的片段合并为一条 */
    public void recordData(String chunk) {
        appendMergeable(ChatTraceType.DATA, chunk);
    }

    /** 记录最终答案，每次独立一条 */
    public void recordFinalAnswer(String answer) {
        addEntry(ChatTraceType.FINAL_ANSWER, answer);
    }

    /** 记录错误信息，每次独立一条 */
    public void recordError(String message) {
        addEntry(ChatTraceType.ERROR, message);
    }

    /**
     * 记录一次工具调用。
     * 入参与结果结构化存储，前端不必再用正则从一整段文本里抠工具名。
     *
     * @param toolName   工具名
     * @param toolInput  入参原文
     * @param toolOutput 执行结果
     * @param durationMs 耗时，单位毫秒
     */
    public void recordToolCall(String toolName, String toolInput, String toolOutput, long durationMs) {
        synchronized (entries) {
            ChatTraceEntry entry = new ChatTraceEntry(ChatTraceType.TOOL, source, null);
            entry.fillTool(toolName, toolInput, toolOutput, durationMs);
            entries.add(entry);
        }
    }

    /** 当前已收集的轨迹快照，按产生顺序 */
    public List<ChatTraceEntry> snapshot() {
        synchronized (entries) {
            return Collections.unmodifiableList(new ArrayList<>(entries));
        }
    }

    public boolean isEmpty() {
        synchronized (entries) {
            return entries.isEmpty();
        }
    }

    /**
     * 本轮要落到 ea_chat_message.answer 的答案文本。
     * <p>
     * 优先取最后一条 FINAL_ANSWER；没有则把全部 DATA 片段按顺序拼接。
     * 实时与回放两边都以此为准，避免出现「刷新后答案和刚才看到的不一样」。
     *
     * @return 答案文本；没有任何可用内容时返回空字符串
     */
    public String resolveAnswer() {
        synchronized (entries) {
            for (int index = entries.size() - 1; index >= 0; index--) {
                ChatTraceEntry entry = entries.get(index);
                if (entry.getType() == ChatTraceType.FINAL_ANSWER) {
                    return entry.contentText();
                }
            }
            StringBuilder data = new StringBuilder();
            for (ChatTraceEntry entry : entries) {
                if (entry.getType() == ChatTraceType.DATA) {
                    data.append(entry.contentText());
                }
            }
            return data.toString();
        }
    }

    /** 追加可合并类型的片段：能并入最后一条就并入，否则新起一条 */
    private void appendMergeable(ChatTraceType type, String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return;
        }
        synchronized (entries) {
            ChatTraceEntry last = entries.isEmpty() ? null : entries.get(entries.size() - 1);
            if (last != null && last.mergeable(type, source)) {
                last.append(chunk);
                return;
            }
            entries.add(new ChatTraceEntry(type, source, chunk));
        }
    }

    /** 追加独立一条轨迹 */
    private void addEntry(ChatTraceType type, String content) {
        if (content == null || content.isBlank()) {
            return;
        }
        synchronized (entries) {
            entries.add(new ChatTraceEntry(type, source, content));
        }
    }
}
