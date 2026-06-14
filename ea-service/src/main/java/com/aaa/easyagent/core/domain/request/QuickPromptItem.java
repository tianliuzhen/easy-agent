package com.aaa.easyagent.core.domain.request;

import lombok.Data;

import java.util.List;

/**
 * 浮选提示词单个按钮（分类）及其推荐问题
 *
 * @author liuzhen.tian
 */
@Data
public class QuickPromptItem {
    /**
     * 主键ID（新增时为空）
     */
    private Long id;

    /**
     * 浮选按钮名称，如 售后/物流/投诉/咨询
     */
    private String label;

    /**
     * 该按钮下的推荐问题列表
     */
    private List<String> questions;

    /**
     * 排序顺序
     */
    private Integer sortOrder;
}
