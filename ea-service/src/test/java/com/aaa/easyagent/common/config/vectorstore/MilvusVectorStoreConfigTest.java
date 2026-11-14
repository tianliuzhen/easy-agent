package com.aaa.easyagent.common.config.vectorstore;

import io.milvus.client.MilvusServiceClient;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;

/**
 * 验证 Milvus 按需连接，不阻断容器启动，并在初始化失败后允许重新获取。
 */
class MilvusVectorStoreConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(MilvusVectorStoreConfig.class)
            .withBean(MilvusVectorProperties.class)
            .withPropertyValues("easy-agent.vectorstore.provider=milvus");

    @Test
    void startsWithoutConnectingAndClosesClientAfterFirstUse() {
        try (MockedConstruction<MilvusServiceClient> clientConstruction = mockConstruction(MilvusServiceClient.class)) {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(clientConstruction.constructed()).isEmpty();

                MilvusServiceClient milvusClient = context.getBeanProvider(MilvusServiceClient.class).getObject();

                assertThat(clientConstruction.constructed()).containsExactly(milvusClient);
                assertThat(context.getBeanProvider(MilvusServiceClient.class).getObject()).isSameAs(milvusClient);
            });

            verify(clientConstruction.constructed().get(0)).close();
        }
    }

    @Test
    void keepsContextActiveAndRetriesAfterConnectionFailure() {
        IllegalStateException connectionFailure = new IllegalStateException("Milvus 连接超时");
        try (MockedConstruction<MilvusServiceClient> clientConstruction = mockConstruction(
                MilvusServiceClient.class, (milvusClient, constructionContext) -> {
                    if (constructionContext.getCount() == 1) {
                        throw connectionFailure;
                    }
                })) {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(clientConstruction.constructed()).isEmpty();

                assertThatThrownBy(() -> context.getBeanProvider(MilvusServiceClient.class).getObject())
                        .isInstanceOf(BeanCreationException.class)
                        .hasRootCause(connectionFailure);
                assertThat(context.isActive()).isTrue();

                MilvusServiceClient milvusClient = context.getBeanProvider(MilvusServiceClient.class).getObject();
                assertThat(clientConstruction.constructed()).containsExactly(milvusClient);
            });
        }
    }
}
