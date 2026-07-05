package com.aaa.easyagent.common.config.vectorstore;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Milvus 向量库配置，前缀 easy-agent.vectorstore.milvus
 *
 * @author liuzhen.tian
 */
@Data
@Component
@ConfigurationProperties(prefix = "easy-agent.vectorstore.milvus")
public class MilvusVectorProperties {

    private String host = "127.0.0.1";
    private int port = 19530;
    private String username;
    private String password;
    private String databaseName = "default";
    private boolean secure = false;

    private int embeddingDimension = 1536;
    private boolean initializeSchema = true;
    private String metricType = "COSINE";
    private String indexType = "IVF_FLAT";
}
