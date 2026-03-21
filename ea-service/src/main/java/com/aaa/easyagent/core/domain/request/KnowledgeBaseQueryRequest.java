package com.aaa.easyagent.core.domain.request;

import lombok.Data;

/**
 * 知识库查询请求
 *
 * @author liuzhen.tian
 * @version 1.0 KnowledgeBaseQueryRequest.java  2026/3/21
 */
@Data
public class KnowledgeBaseQueryRequest {

    /**
     * Agent ID
     */
    private String agentId;

    /**
     * 知识库名称（模糊查询）
     */
    private String kbName;

    /**
     * 知识库类型
     */
    private String type;

    /**
     * 状态
     */
    private String status;

    /**
     * 页码
     */
    private Integer pageNum;

    /**
     * 每页大小
     */
    private Integer pageSize;
}