package com.aaa.easyagent.common.config.vectorstore;

import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import co.elastic.clients.transport.rest5_client.low_level.Rest5ClientBuilder;
import io.micrometer.observation.ObservationRegistry;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.ssl.SSLContexts;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStoreOptions;
import org.springframework.ai.vectorstore.elasticsearch.autoconfigure.ElasticsearchVectorStoreProperties;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.elasticsearch.autoconfigure.Rest5ClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

/**
 * clone :
 *
 * @author liuzhen.tian
 * @version 1.0 ElasticsearchVectorStoreAutoConfiguration.java  2025/11/9 20:56
 * @see org.springframework.ai.autoconfigure.vectorstore.elasticsearch.ElasticsearchVectorStoreAutoConfiguration
 */
@Configuration
public class ElasticsearchVectorStoreAutoConfiguration {

    public ElasticsearchVectorStoreAutoConfiguration() {
        System.out.println();
    }

    @Bean
    @ConditionalOnMissingBean(BatchingStrategy.class)
    BatchingStrategy batchingStrategy() {
        return new TokenCountBatchingStrategy();
    }

    /**
     * @param properties
     * @param restClient
     * @param ollamaEmbeddingModel
     * @param observationRegistry
     * @param customObservationConvention
     * @param batchingStrategy
     * @return
     * es连接客户端初始化
     *  ElasticsearchRestClientConfigurations.RestClientConfiguration#elasticsearchRestClient(org.elasticsearch.client.RestClientBuilder)
     * es连接参数初始化
     * ElasticsearchRestClientConfigurations.RestClientBuilderConfiguration#elasticsearchRestClientBuilder(ElasticsearchConnectionDetails, ObjectProvider, ObjectProvider)
     */
    @Bean
    ElasticsearchVectorStore esVectorStore(ElasticsearchVectorStoreProperties properties, Rest5Client restClient,
                                           EmbeddingModel ollamaEmbeddingModel, ObjectProvider<ObservationRegistry> observationRegistry,
                                           ObjectProvider<VectorStoreObservationConvention> customObservationConvention,
                                           BatchingStrategy batchingStrategy) {
        ElasticsearchVectorStoreOptions elasticsearchVectorStoreOptions = new ElasticsearchVectorStoreOptions();

        if (StringUtils.hasText(properties.getIndexName())) {
            elasticsearchVectorStoreOptions.setIndexName(properties.getIndexName());
        }
        if (properties.getDimensions() != null) {
            elasticsearchVectorStoreOptions.setDimensions(properties.getDimensions());
        }
        if (properties.getSimilarity() != null) {
            elasticsearchVectorStoreOptions.setSimilarity(properties.getSimilarity());
        }

        return ElasticsearchVectorStore.builder(restClient, ollamaEmbeddingModel)
                .options(elasticsearchVectorStoreOptions)
                .initializeSchema(properties.isInitializeSchema())
                .observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
                .customObservationConvention(customObservationConvention.getIfAvailable(() -> null))
                .batchingStrategy(batchingStrategy)
                .build();
    }



    /**
     * 跳过es SSL的证书检查和hostname检查
     *
     * @return
     */
    @Bean
    Rest5ClientBuilderCustomizer rest5ClientBuilderCustomizer() {
        return new Rest5ClientBuilderCustomizer() {
            @Override
            public void customize(Rest5ClientBuilder builder) {
            }

            /**
             * 在这里加入自定义逻辑，跳过SSL的证书检查和hostname检查
             */
            @Override
            public void customize(PoolingAsyncClientConnectionManagerBuilder builder) {
                try {
                    SSLContext sslContext = SSLContexts.custom()
                            .loadTrustMaterial((chain, authType) -> true)
                            .build();
                    builder.setTlsStrategy(ClientTlsStrategyBuilder.create()
                            .setSslContext(sslContext)
                            .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                            .build());
                } catch (NoSuchAlgorithmException | KeyManagementException e) {
                    throw new RuntimeException("Failed to configure SSL context", e);
                } catch (KeyStoreException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

}

