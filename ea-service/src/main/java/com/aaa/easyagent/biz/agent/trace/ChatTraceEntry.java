package com.aaa.easyagent.biz.agent.trace;

import com.aaa.easyagent.core.domain.enums.ChatTraceType;
import lombok.Getter;

import java.util.Date;

/**
 * 一条执行轨迹的内存表示。
 * <p>
 * 流式输出会把思考和回答拆成大量 chunk，连续同类型的 chunk 由
 * {@link ChatTraceRecorder} 合并追加到同一条 entry，落库行数按「阶段」而不是 chunk 数增长。
 */
@Getter
public class ChatTraceEntry {

    /** 轨迹类型 */
    private final ChatTraceType type;

    /** 产生该条轨迹的来源：Agent 与 Flow 节点 */
    private final ChatTraceSource source;

    /** 文本内容，流式片段会持续追加 */
    private final StringBuilder content = new StringBuilder();

    /** 工具名，仅 TOOL 类型 */
    private String toolName;

    /** 工具入参原文，仅 TOOL 类型 */
    private String toolInput;

    /** 工具执行结果，仅 TOOL 类型 */
    private String toolOutput;

    /** 该步骤耗时，单位毫秒，仅 TOOL 类型 */
    private Long durationMs;

    /** 首次产生时间 */
    private final Date createdAt = new Date();

    ChatTraceEntry(ChatTraceType type, ChatTraceSource source, String content) {
        this.type = type;
        this.source = source;
        if (content != null) {
            this.content.append(content);
        }
    }

    /** 追加流式片段 */
    void append(String chunk) {
        if (chunk != null) {
            content.append(chunk);
        }
    }

    /** 补齐工具调用的结构化字段 */
    void fillTool(String toolName, String toolInput, String toolOutput, Long durationMs) {
        this.toolName = toolName;
        this.toolInput = toolInput;
        this.toolOutput = toolOutput;
        this.durationMs = durationMs;
    }

    public String contentText() {
        return content.toString();
    }

    /**
     * 是否可以与新到的片段合并：类型相同且来源相同。
     * TOOL 与 FINAL_ANSWER 每次都是独立一条，不合并。
     */
    boolean mergeable(ChatTraceType incomingType, ChatTraceSource incomingSource) {
        if (type != incomingType) {
            return false;
        }
        if (type == ChatTraceType.TOOL || type == ChatTraceType.FINAL_ANSWER) {
            return false;
        }
        return source.sameAs(incomingSource);
    }
}
