package com.aaa.easyagent.core.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 聊天执行轨迹类型。
 * <p>
 * 落库到 ea_chat_message_trace.trace_type，并作为前端选择渲染组件的依据。
 */
@Getter
@AllArgsConstructor
public enum ChatTraceType {

    /** 模型的思考过程（reasoning content） */
    THINKING("THINKING"),

    /** 流式回答片段。最终答案由 FINAL_ANSWER 单独记录，这里保留生成过程 */
    DATA("DATA"),

    /** 一次工具调用，含工具名、入参、结果与耗时 */
    TOOL("TOOL"),

    /** 本轮最终答案，历史回放时优先取它作为气泡正文 */
    FINAL_ANSWER("FINAL_ANSWER"),

    /** 执行过程中的错误信息 */
    ERROR("ERROR");

    /** 落库与接口使用的 code */
    private final String code;

    /**
     * 按 code 解析类型。
     *
     * @param code 落库的 trace_type 值
     * @return 对应类型；无法识别时返回 null，由调用方决定是否跳过
     */
    public static ChatTraceType ofCode(String code) {
        if (code == null) {
            return null;
        }
        for (ChatTraceType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
