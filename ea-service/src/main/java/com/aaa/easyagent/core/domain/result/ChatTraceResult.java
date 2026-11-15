package com.aaa.easyagent.core.domain.result;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 历史回放用的单条执行轨迹。
 * <p>
 * 工具调用的名称、入参、结果分字段返回，前端不必再用正则从一整段文本里解析。
 */
@Data
@Accessors(chain = true)
public class ChatTraceResult {

    /** 同一条消息内的顺序号，从1开始 */
    private Integer seq;

    /** 轨迹类型，取值见 ChatTraceType：THINKING/DATA/TOOL/FINAL_ANSWER/ERROR */
    private String traceType;

    /** 文本内容：思考过程、回答片段、错误信息 */
    private String content;

    /** 工具名，仅 TOOL 类型 */
    private String toolName;

    /** 工具入参原文，仅 TOOL 类型 */
    private String toolInput;

    /** 工具执行结果，仅 TOOL 类型 */
    private String toolOutput;

    /** 产生该条轨迹的 Agent 名称，多 Agent 编排用于区分来源 */
    private String agentName;

    /** Flow 节点ID，非编排场景为 null */
    private String flowNodeId;

    /** 该步骤耗时，单位毫秒，仅 TOOL 类型 */
    private Long durationMs;
}
