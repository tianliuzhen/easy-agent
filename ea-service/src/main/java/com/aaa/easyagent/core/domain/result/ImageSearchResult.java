package com.aaa.easyagent.core.domain.result;

import lombok.Data;

/**
 * 图片搜索结果 / 图片记录。
 *
 * @author liuzhen.tian
 */
@Data
public class ImageSearchResult {
    /** 记录 id（UUID） */
    private String id;
    /** 展示名称 */
    private String name;
    /** 文件扩展名 */
    private String ext;
    /** 相似度分数（列表场景为空） */
    private Float score;
    /** 图片访问地址（由存储层生成，local 为相对路径，前端补 API_BASE_URL） */
    private String imageUrl;
}
