package com.aaa.easyagent.core.domain.request;

import lombok.Data;

/**
 * 知识库解绑请求
 *
 * @author liuzhen.tian
 * @version 1.0 KnowledgeBaseUnbindRequest.java  2026/3/21
 */
@Data
public class KnowledgeBaseUnbindRequest {

    /**
     * Agent ID
     */
    private String agentId;

    /**
     * 知识库ID
     */
    private Long knowledgeBaseId;
}