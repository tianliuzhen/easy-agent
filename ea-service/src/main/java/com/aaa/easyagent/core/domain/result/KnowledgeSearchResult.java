package com.aaa.easyagent.core.domain.result;

import lombok.Data;

/**
 * 知识搜索返回结果
 *
 * @author liuzhen.tian
 * @version 1.0 KnowledgeSearchResult.java  2026/5/10
 */
@Data
public class KnowledgeSearchResult {

    /**
     * 相似度得分
     */
    private Double score;

    /**
     * 知识库名称
     */
    private String kbName;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 分类
     */
    private String catalog;

    /**
     * 文档内容
     */
    private String text;
}
