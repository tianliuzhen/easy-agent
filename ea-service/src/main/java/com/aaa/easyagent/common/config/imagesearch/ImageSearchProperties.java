package com.aaa.easyagent.common.config.imagesearch;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 图片搜索（以图搜图）配置，前缀 easy-agent.image-search。
 *
 * @author liuzhen.tian
 */
@Data
@Component
@ConfigurationProperties(prefix = "easy-agent.image-search")
public class ImageSearchProperties {

    /** 百炼 DASHSCOPE key（默认从 .env 的 DASHSCOPE_API_KEY 读取） */
    private String apiKey;
    /** 百炼原生多模态 embedding 端点 */
    private String endpoint = "https://dashscope.aliyuncs.com/api/v1/services/embeddings/multimodal-embedding/multimodal-embedding";
    /** 多模态模型 */
    private String model = "qwen3-vl-embedding";
    /** 向量维度：2560/2048/1536/1024/768/512/256 */
    private int dimension = 1024;

    /** Milvus 集合名（独立于文本 RAG） */
    private String collectionName = "ea_image_search";
    private String metricType = "COSINE";
    private String indexType = "IVF_FLAT";

    /** 存储方式：local | oss（未来扩展） */
    private String storage = "local";
    /** local 存储目录 */
    private String uploadDir = "./uploads/image-search";
}
