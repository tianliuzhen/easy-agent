package com.aaa.springai.config.vectorstore;

import io.micrometer.observation.ObservationRegistry;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.ai.autoconfigure.vectorstore.elasticsearch.ElasticsearchVectorStoreProperties;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStoreOptions;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchConnectionDetails;
import org.springframework.boot.autoconfigure.elasticsearch.RestClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

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
     * @see ElasticsearchRestClientConfigurations.RestClientConfiguration#elasticsearchRestClient(org.elasticsearch.client.RestClientBuilder)
     * es连接参数初始化
     * @see ElasticsearchRestClientConfigurations.RestClientBuilderConfiguration#elasticsearchRestClientBuilder(ElasticsearchConnectionDetails, ObjectProvider, ObjectProvider)
     */
    @Bean
    ElasticsearchVectorStore esVectorStore(ElasticsearchVectorStoreProperties properties, RestClient restClient,
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
    RestClientBuilderCustomizer restClientBuilderCustomizer() {
        return new RestClientBuilderCustomizer() {
            @Override
            public void customize(RestClientBuilder builder) {

            }

            /**
             * 在这里加入自定义逻辑，比如跳过SSL的证书检查和hostname检查
             */
            @Override
            public void customize(HttpAsyncClientBuilder builder) {
                SSLContextBuilder sscb = SSLContexts.custom();
                try {
                    sscb.loadTrustMaterial((chain, authType) -> {
                        // 在这里跳过证书信息校验
                        // System.out.println("暂时isTrusted|" + authType + "|" + Arrays.toString(chain));
                        return true;
                    });
                } catch (NoSuchAlgorithmException | KeyStoreException e) {
                    e.printStackTrace();
                }
                try {
                    builder.setSSLContext(sscb.build());
                } catch (KeyManagementException | NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
                // 这里跳过主机名称校验
                builder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
            }
        };
    }

}

