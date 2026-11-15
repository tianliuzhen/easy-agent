package com.aaa.easyagent.core.domain.DO;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * 表名：ea_chat_message_trace
 * 表注释：聊天消息执行轨迹表
 * <p>
 * 一条 ea_chat_message 对应多条轨迹，按 seq 还原思考、工具调用、回答片段的真实先后顺序。
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
@Table(name = "ea_chat_message_trace")
public class EaChatMessageTraceDO {

    @Id
    @Column(name = "id")
    @GeneratedValue(generator = "JDBC")
    private Long id;

    /** 会话ID */
    @Column(name = "conversation_id")
    private Long conversationId;

    /** 所属消息ID */
    @Column(name = "message_id")
    private Long messageId;

    /** 同一条消息内的产生顺序，从1开始 */
    @Column(name = "seq")
    private Integer seq;

    /** 轨迹类型，取值见 ChatTraceType */
    @Column(name = "trace_type")
    private String traceType;

    /** 文本内容：思考过程、回答片段、错误信息 */
    @Column(name = "content")
    private String content;

    /** 工具名，仅 TOOL 类型 */
    @Column(name = "tool_name")
    private String toolName;

    /** 工具入参原文，仅 TOOL 类型 */
    @Column(name = "tool_input")
    private String toolInput;

    /** 工具执行结果，仅 TOOL 类型 */
    @Column(name = "tool_output")
    private String toolOutput;

    /** 产生该条轨迹的 Agent ID */
    @Column(name = "agent_id")
    private Long agentId;

    /** 产生该条轨迹的 Agent 名称 */
    @Column(name = "agent_name")
    private String agentName;

    /** Flow 节点ID，非编排场景为 null */
    @Column(name = "flow_node_id")
    private String flowNodeId;

    /** 该步骤耗时，单位毫秒 */
    @Column(name = "duration_ms")
    private Long durationMs;

    /** 创建时间 */
    @Column(name = "created_at")
    private Date createdAt;
}
