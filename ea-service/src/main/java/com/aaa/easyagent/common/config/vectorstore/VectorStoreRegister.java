package com.aaa.easyagent.common.config.vectorstore;

import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.micrometer.observation.ObservationRegistry;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStoreOptions;
import org.springframework.ai.vectorstore.elasticsearch.autoconfigure.ElasticsearchVectorStoreProperties;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * 向量存储注册中心：根据 easy-agent.vectorstore.provider（es / milvus）按索引/集合动态创建 VectorStore。
 *
 * @author liuzhen.tian
 * @version 1.0 ElasticsearchRegister.java  2026/2/7 22:11
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VectorStoreRegister {

    final EmbeddingModel activeEmbeddingModel;
    final ObjectProvider<ObservationRegistry> observationRegistry;
    final ObjectProvider<VectorStoreObservationConvention> customObservationConvention;
    final BatchingStrategy batchingStrategy;

    /** ES 相关依赖，provider=milvus 时可能不存在，使用 ObjectProvider 惰性获取 */
    final ObjectProvider<Rest5Client> restClientProvider;
    final ObjectProvider<ElasticsearchVectorStoreProperties> esPropertiesProvider;

    /** Milvus 相关依赖，provider=es 时 client 不存在 */
    final ObjectProvider<MilvusServiceClient> milvusClientProvider;
    final MilvusVectorProperties milvusProperties;

    @Value("${easy-agent.vectorstore.provider:es}")
    private String provider;

    // Guava缓存：缓存VectorStore实例，key为indexName
    private Cache<String, VectorStore> vectorStoreCache;


    @PostConstruct
    public void init() {
        // 初始化Guava缓存
        // 设置缓存最大容量为100，过期时间为30分钟
        vectorStoreCache = CacheBuilder.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .recordStats()
                .build();
        log.info("VectorStore缓存初始化完成，当前向量库 provider={}", provider);
    }

    /**
     * 根据索引生成索引向量存储（带Guava缓存优化）
     *
     * @param indexName 索引名称（ES 索引 / Milvus 集合）
     * @return VectorStore实例
     */
    public VectorStore register(String indexName) {
        try {
            // 使用缓存获取VectorStore
            return vectorStoreCache.get(indexName, () -> {
                log.info("创建新的VectorStore实例，provider={}, 索引名: {}", provider, indexName);
                return createVectorStore(indexName);
            });
        } catch (Exception e) {
            log.error("获取VectorStore缓存失败，索引名: {}", indexName, e);
            // 缓存获取失败时直接创建
            return createVectorStore(indexName);
        }
    }

    /**
     * 创建VectorStore实例的核心逻辑，根据 provider 选择 ES 或 Milvus
     */
    private VectorStore createVectorStore(String indexName) {
        if ("milvus".equalsIgnoreCase(provider)) {
            return createMilvusVectorStore(indexName);
        }
        return createElasticsearchVectorStore(indexName);
    }

    /**
     * 创建 Elasticsearch VectorStore
     */
    private VectorStore createElasticsearchVectorStore(String indexName) {
        Rest5Client restClient = restClientProvider.getIfAvailable();
        ElasticsearchVectorStoreProperties properties = esPropertiesProvider.getIfAvailable();
        if (restClient == null || properties == null) {
            throw new IllegalStateException("provider=es 但缺少 Elasticsearch 相关 Bean（Rest5Client / ElasticsearchVectorStoreProperties）");
        }

        ElasticsearchVectorStoreOptions options = new ElasticsearchVectorStoreOptions();
        if (StringUtils.hasText(indexName)) {
            options.setIndexName(indexName);
        }
        if (properties.getDimensions() != null) {
            options.setDimensions(properties.getDimensions());
        }
        if (properties.getSimilarity() != null) {
            options.setSimilarity(properties.getSimilarity());
        }

        ElasticsearchVectorStore vectorStore = ElasticsearchVectorStore.builder(restClient, activeEmbeddingModel)
                .options(options)
                .initializeSchema(properties.isInitializeSchema())
                .observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
                .customObservationConvention(customObservationConvention.getIfAvailable(() -> null))
                .batchingStrategy(batchingStrategy)
                .build();

        vectorStore.afterPropertiesSet();
        log.debug("Elasticsearch VectorStore 创建完成，索引名: {}", indexName);
        return vectorStore;
    }

    /**
     * 创建 Milvus VectorStore
     */
    private VectorStore createMilvusVectorStore(String indexName) {
        MilvusServiceClient client = milvusClientProvider.getIfAvailable();
        if (client == null) {
            throw new IllegalStateException("provider=milvus 但未找到 MilvusServiceClient Bean，请检查 easy-agent.vectorstore.milvus 配置");
        }

        String collectionName = sanitizeCollectionName(indexName);

        MilvusVectorStore vectorStore = MilvusVectorStore.builder(client, activeEmbeddingModel)
                .databaseName(milvusProperties.getDatabaseName())
                .collectionName(collectionName)
                .embeddingDimension(milvusProperties.getEmbeddingDimension())
                .indexType(IndexType.valueOf(milvusProperties.getIndexType()))
                .metricType(MetricType.valueOf(milvusProperties.getMetricType()))
                .initializeSchema(milvusProperties.isInitializeSchema())
                .observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
                .customObservationConvention(customObservationConvention.getIfAvailable(() -> null))
                .batchingStrategy(batchingStrategy)
                .build();

        try {
            vectorStore.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException("初始化 Milvus 集合失败: " + collectionName, e);
        }
        log.debug("Milvus VectorStore 创建完成，集合名: {}", collectionName);
        return vectorStore;
    }

    /**
     * Milvus 集合名规则：仅允许字母、数字、下划线，且以字母或下划线开头。
     * ES 的索引名可能包含 '-' 等字符（如 ea_user_common_xx、easy-agent-demo1），此处统一转换。
     */
    private String sanitizeCollectionName(String indexName) {
        if (!StringUtils.hasText(indexName)) {
            return "ea_default";
        }
        String sanitized = indexName.replaceAll("[^a-zA-Z0-9_]", "_");
        if (!sanitized.matches("^[a-zA-Z_].*")) {
            sanitized = "c_" + sanitized;
        }
        if (sanitized.length() > 255) {
            sanitized = sanitized.substring(0, 255);
        }
        return sanitized;
    }

    /**
     * 获取缓存统计信息
     */
    public String getCacheStats() {
        return vectorStoreCache.stats().toString();
    }

    /**
     * 清除指定索引的缓存
     */
    public void invalidateCache(String indexName) {
        vectorStoreCache.invalidate(indexName);
        log.info("已清除索引 {} 的缓存", indexName);
    }

    /**
     * 清除所有缓存
     */
    public void invalidateAllCache() {
        vectorStoreCache.invalidateAll();
        log.info("已清除所有VectorStore缓存");
    }

}
