package com.aaa.easyagent.core.domain.request;

import lombok.Data;

import java.util.List;

/**
 * 浮选提示词请求对象（列表查询只需 agentId；保存时携带 prompts 全量替换）
 *
 * @author liuzhen.tian
 */
@Data
public class QuickPromptReq {
    /**
     * 关联的Agent ID
     */
    private Long agentId;

    /**
     * 浮选按钮列表（保存时全量覆盖该 Agent 的配置）
     */
    private List<QuickPromptItem> prompts;
}
