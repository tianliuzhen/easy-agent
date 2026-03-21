package com.aaa.easyagent.core.domain.request;

import lombok.Data;

/**
 * 知识库绑定请求
 *
 * @author liuzhen.tian
 * @version 1.0 KnowledgeBaseBindRequest.java  2026/3/21
 */
@Data
public class KnowledgeBaseBindRequest {

    /**
     * Agent ID
     */
    private String agentId;

    /**
     * 知识库ID
     */
    private Long knowledgeBaseId;

    /**
     * 知识库名称（冗余存储，方便查询）
     */
    private String kbName;

    /**
     * 创建者
     */
    private String creator;
}