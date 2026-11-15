package com.aaa.easyagent.biz.imagesearch;

import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import com.aaa.easyagent.biz.imagesearch.service.ImageSearchService;
import com.aaa.easyagent.biz.imagesearch.service.impl.ImageSearchServiceImpl;
import com.aaa.easyagent.common.config.imagesearch.ImageSearchProperties;
import com.aaa.easyagent.common.embedding.DashScopeMultimodalClient;
import com.aaa.easyagent.common.storage.ImageStorage;
import io.milvus.client.MilvusServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 验证统一开关只装配一种图片存储，容器启动不连接向量数据库。
 */
class ImageVectorDaoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(ImageSearchProperties.class)
            .withBean(DashScopeMultimodalClient.class, () -> mock(DashScopeMultimodalClient.class))
            .withBean(ImageStorage.class, () -> mock(ImageStorage.class))
            .withUserConfiguration(ImageElasticsearchDao.class, ImageMilvusDao.class, ImageSearchServiceImpl.class);

    @Test
    void selectsElasticsearchWithoutRequiringMilvusClient() {
        contextRunner.withPropertyValues("easy-agent.vectorstore.provider=es").run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(ImageVectorDao.class)
                    .hasSingleBean(ImageSearchService.class).doesNotHaveBean(ImageMilvusDao.class)
                    .doesNotHaveBean(MilvusServiceClient.class);
            assertThat(context.getBean(ImageVectorDao.class)).isInstanceOf(ImageElasticsearchDao.class);
        });
    }

    @Test
    void selectsMilvusWithoutRequiringElasticsearchClient() {
        contextRunner.withPropertyValues("easy-agent.vectorstore.provider=milvus").run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(ImageVectorDao.class)
                    .hasSingleBean(ImageSearchService.class).doesNotHaveBean(ImageElasticsearchDao.class)
                    .doesNotHaveBean(Rest5Client.class);
            assertThat(context.getBean(ImageVectorDao.class)).isInstanceOf(ImageMilvusDao.class);
        });
    }

    @Test
    void defaultsToElasticsearchLikeKnowledgeVectorStore() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(ImageVectorDao.class);
            assertThat(context.getBean(ImageVectorDao.class)).isInstanceOf(ImageElasticsearchDao.class);
        });
    }
}
