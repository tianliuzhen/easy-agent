package com.aaa.easyagent.core.domain.request;

import lombok.Data;

/**
 * 知识库搜索请求
 *
 * @author liuzhen.tian
 * @version 1.0 KnowledgeBaseSearchRequest.java  2026/5/10
 */
@Data
public class KnowledgeBaseSearchRequest {

    /**
     * Agent ID
     */
    private String agentId;

    /**
     * 搜索内容
     */
    private String query;

    /**
     * 返回结果数量
     */
    private Integer topK = 5;

    /**
     * 分类筛选(可选)
     */
    private String catalog;

    /**
     * 相似度阈值(0-1,可选)
     */
    private Double threshold;
}
