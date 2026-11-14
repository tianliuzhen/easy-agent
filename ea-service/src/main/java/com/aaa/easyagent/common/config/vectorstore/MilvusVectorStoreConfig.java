package com.aaa.easyagent.common.config.vectorstore;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.StringUtils;

/**
 * Milvus 客户端配置，仅当 easy-agent.vectorstore.provider=milvus 时生效。
 * 具体的按集合创建 VectorStore 逻辑由 {@link VectorStoreRegister} 处理。
 *
 * @author liuzhen.tian
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "easy-agent.vectorstore.provider", havingValue = "milvus")
public class MilvusVectorStoreConfig {

    /**
     * 首次使用向量功能时才连接 Milvus，避免外部服务不可用阻断应用启动。
     * 调用方通过 ObjectProvider 按需获取；初始化失败仍向调用方抛出，后续获取可重新尝试。
     */
    @Lazy
    @Bean(destroyMethod = "close")
    public MilvusServiceClient milvusServiceClient(MilvusVectorProperties properties) {
        ConnectParam.Builder builder = ConnectParam.newBuilder()
                .withHost(properties.getHost())
                .withPort(properties.getPort())
                .withDatabaseName(properties.getDatabaseName())
                .withSecure(properties.isSecure());

        if (StringUtils.hasText(properties.getUsername())) {
            builder.withAuthorization(properties.getUsername(),
                    properties.getPassword() == null ? "" : properties.getPassword());
        }

        log.info("初始化 Milvus 客户端: {}:{}, database={}", properties.getHost(),
                properties.getPort(), properties.getDatabaseName());
        return new MilvusServiceClient(builder.build());
    }
}
