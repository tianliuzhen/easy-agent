package com.aaa.easyagent.core.domain.request;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 智能体流式对话请求对象（POST 方式）
 *
 * @author liuzhen.tian
 * @version 1.0 StreamChatPostRequest.java  2026/3/22
 */
@Data
@Accessors(chain = true)
public class StreamChatPostRequest {
    
    /**
     * 会话 ID
     */
    private String sessionId;
    
    /**
     * 消息内容
     */
    private String msg;
    
    /**
     * 智能体 ID
     */
    private String agentId;
    
    /**
     * 默认构造函数
     */
    public StreamChatPostRequest() {
        this.sessionId = "110";
        this.msg = "你好";
        this.agentId = "1";
    }
    
    /**
     * 验证请求参数是否有效
     *
     * @return 是否有效
     */
    public boolean isValid() {
        return this.sessionId != null && !this.sessionId.isEmpty()
                && this.msg != null && !this.msg.isEmpty();
    }
}
