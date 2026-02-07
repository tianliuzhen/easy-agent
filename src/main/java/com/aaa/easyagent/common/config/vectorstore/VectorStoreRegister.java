package com.aaa.easyagent.common.config.vectorstore;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.micrometer.observation.ObservationRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RestClient;
import org.springframework.ai.autoconfigure.vectorstore.elasticsearch.ElasticsearchVectorStoreProperties;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStoreOptions;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * @author liuzhen.tian
 * @version 1.0 ElasticsearchRegister.java  2026/2/7 22:11
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VectorStoreRegister {

    final ElasticsearchVectorStoreProperties properties;
    final RestClient restClient;
    final EmbeddingModel ollamaEmbeddingModel;
    final ObjectProvider<ObservationRegistry> observationRegistry;
    final ObjectProvider<VectorStoreObservationConvention> customObservationConvention;
    final BatchingStrategy batchingStrategy;

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
        log.info("VectorStore缓存初始化完成");
    }

    /**
     * 根据索引生成索引向量存储（带Guava缓存优化）
     *
     * @param indexName 索引名称
     * @return VectorStore实例
     */
    public VectorStore register(String indexName) {
        try {
            // 使用缓存获取VectorStore
            return vectorStoreCache.get(indexName, () -> {
                log.info("创建新的VectorStore实例，索引名: {}", indexName);
                return createVectorStore(indexName);
            });
        } catch (Exception e) {
            log.error("获取VectorStore缓存失败，索引名: {}", indexName, e);
            // 缓存获取失败时直接创建
            return createVectorStore(indexName);
        }
    }

    /**
     * 创建VectorStore实例的核心逻辑
     *
     * @param indexName 索引名称
     * @return VectorStore实例
     */
    private VectorStore createVectorStore(String indexName) {
        ElasticsearchVectorStoreOptions elasticsearchVectorStoreOptions = new ElasticsearchVectorStoreOptions();

        if (StringUtils.hasText(indexName)) {
            elasticsearchVectorStoreOptions.setIndexName(indexName);
        }
        if (properties.getDimensions() != null) {
            elasticsearchVectorStoreOptions.setDimensions(properties.getDimensions());
        }
        if (properties.getSimilarity() != null) {
            elasticsearchVectorStoreOptions.setSimilarity(properties.getSimilarity());
        }

        ElasticsearchVectorStore vectorStore = ElasticsearchVectorStore.builder(restClient, ollamaEmbeddingModel)
                .options(elasticsearchVectorStoreOptions)
                .initializeSchema(properties.isInitializeSchema())
                .observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
                .customObservationConvention(customObservationConvention.getIfAvailable(() -> null))
                .batchingStrategy(batchingStrategy)
                .build();

        // 手动激活
        vectorStore.afterPropertiesSet();

        log.debug("VectorStore创建完成，索引名: {}", indexName);
        return vectorStore;
    }

    /**
     * 获取缓存统计信息
     *
     * @return 缓存统计信息字符串
     */
    public String getCacheStats() {
        return vectorStoreCache.stats().toString();
    }

    /**
     * 清除指定索引的缓存
     *
     * @param indexName 索引名称
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
