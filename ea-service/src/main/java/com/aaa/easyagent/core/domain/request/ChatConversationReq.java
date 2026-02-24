package com.aaa.easyagent.core.domain.request;

import com.aaa.easyagent.core.domain.DO.EaChatConversationDO;

/**
 * 聊天会话请求对象
 *
 * @author EasyAgent系统
 * @version 1.0 ChatConversationReq.java  2026/2/10
 */
public class ChatConversationReq extends EaChatConversationDO {
    
    // 可以添加一些业务特定的字段或方法
    
    /**
     * 生成会话标题（如果标题为空）
     * 
     * @param firstQuestion 第一个问题
     * @return 生成的标题
     */
    public String generateTitleIfEmpty(String firstQuestion) {
        if (this.getTitle() == null || this.getTitle().isEmpty()) {
            if (firstQuestion != null && !firstQuestion.isEmpty()) {
                if (firstQuestion.length() > 50) {
                    return firstQuestion.substring(0, 47) + "...";
                } else {
                    return firstQuestion;
                }
            }
        }
        return this.getTitle();
    }
    
    /**
     * 验证请求参数是否有效
     * 
     * @return 是否有效
     */
    public boolean isValid() {
        return this.getAgentId() != null && this.getAgentId() > 0;
    }
    
    /**
     * 设置默认值
     */
    public void setDefaults() {
        if (this.getStatus() == null) {
            this.setStatus("active");
        }
        if (this.getMessageCount() == null) {
            this.setMessageCount(0);
        }
        if (this.getTitle() == null) {
            this.setTitle("");
        }
    }
}
