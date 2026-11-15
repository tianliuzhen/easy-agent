package com.aaa.easyagent.biz.imagesearch;

import com.aaa.easyagent.core.domain.result.ImageSearchResult;

import java.util.List;

/**
 * 图片向量存储接口，由 easy-agent.vectorstore.provider 选择 ES 或 Milvus。
 * 图片原文件仍交给 ImageStorage，向量存储仅负责检索与图片元数据。
 */
public interface ImageVectorDao {

    /** 保存图片元数据及多模态向量，sourceUrl 为空表示本地上传。 */
    void insert(String id, String name, String ext, String sourceUrl, float[] vector);

    /** 按向量相似度降序返回候选图片，分数使用余弦相似度口径。 */
    List<ImageSearchResult> search(float[] vector, int topK);

    /** 分页读取图片元数据，不返回向量。 */
    List<ImageSearchResult> listAll(int offset, int limit);

    /** 返回当前存储中的图片总数。 */
    long countAll();

    /** 按图片标识删除向量与元数据，重复删除不报错。 */
    void deleteById(String id);
}
