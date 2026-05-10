package com.aaa.easyagent.core.domain.request;

import lombok.Data;

/**
 * 知识库上传请求
 *
 * @author liuzhen.tian
 * @version 1.0 KnowledgeBaseUploadRequest.java  2026/5/10
 */
@Data
public class KnowledgeBaseUploadRequest {

    /**
     * Agent ID
     */
    private String agentId;

    /**
     * 知识库名称
     */
    private String kbName;

    /**
     * 知识库描述
     */
    private String kbDesc;

    /**
     * 分类
     */
    private String catalog;

}
