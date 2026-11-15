package com.aaa.easyagent.biz.imagesearch;

import co.elastic.clients.transport.rest5_client.low_level.Request;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import com.aaa.easyagent.common.config.exception.AgentException;
import com.aaa.easyagent.common.config.imagesearch.ImageSearchProperties;
import com.aaa.easyagent.core.domain.result.ImageSearchResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 真正访问 ES 9 的集成验证。显式指定专用测试实例，只清理每个用例的临时索引。
 */
@EnabledIfEnvironmentVariable(named = "IMAGE_SEARCH_ES_TEST_URI", matches = ".+")
class ImageElasticsearchDaoIntegrationTest {

    private Rest5Client restClient;
    private ImageSearchProperties properties;
    private ImageElasticsearchDao imageVectorDao;

    @BeforeEach
    void configureDao() {
        restClient = Rest5Client.builder(URI.create(System.getenv("IMAGE_SEARCH_ES_TEST_URI"))).build();
        properties = new ImageSearchProperties();
        properties.setIndexName("ea_image_test_" + UUID.randomUUID().toString().replace("-", ""));
        properties.setDimension(3);
        imageVectorDao = newDao(properties);
    }

    @AfterEach
    void removeOnlyTestIndexAndCloseClient() throws IOException {
        try {
            Request request = new Request("DELETE", "/" + properties.getIndexName());
            restClient.performRequest(request);
        } finally {
            restClient.close();
        }
    }

    @Test
    void insertsAndSearchesReal2560DimensionalImageVectors() {
        properties.setDimension(2560);
        float[] queryVector = new float[2560];
        queryVector[0] = 1;
        float[] otherVector = new float[2560];
        otherVector[1] = 1;
        imageVectorDao.insert("matching", "同图", "jpg", "https://example.test/image.jpg", queryVector);
        imageVectorDao.insert("other", "其他图", "png", null, otherVector);

        List<ImageSearchResult> results = imageVectorDao.search(queryVector, 2);

        assertThat(imageVectorDao.countAll()).isEqualTo(2);
        assertThat(results).extracting(ImageSearchResult::getId).containsExactly("matching", "other");
        assertThat(results.get(0).getScore()).isCloseTo(1f, org.assertj.core.data.Offset.offset(0.001f));
        assertThat(results.get(1).getScore()).isCloseTo(0f, org.assertj.core.data.Offset.offset(0.001f));
        assertThat(results.get(0).getSourceUrl()).isEqualTo("https://example.test/image.jpg");
    }

    @Test
    void listsStablePagesAndReusesExistingIndexAcrossDaoInstances() {
        float[] vector = {1, 0, 0};
        imageVectorDao.insert("b", "图片B", "jpg", null, vector);
        imageVectorDao.insert("a", "图片A", "png", null, vector);
        imageVectorDao.insert("c", "图片C", "webp", "https://example.test/c.webp", vector);

        ImageElasticsearchDao reloadedDao = newDao(properties);
        List<ImageSearchResult> page = reloadedDao.listAll(1, 2);

        assertThat(page).extracting(ImageSearchResult::getId).containsExactly("b", "c");
        assertThat(page).extracting(ImageSearchResult::getScore).containsOnlyNulls();
        assertThat(page.get(1).getExt()).isEqualTo("webp");
        assertThat(reloadedDao.countAll()).isEqualTo(3);
    }

    @Test
    void deletesIdempotentlyAndUpdatesCountBeforeReturning() {
        imageVectorDao.insert("image", "图片", "jpg", null, new float[]{1, 0, 0});

        imageVectorDao.deleteById("image");
        imageVectorDao.deleteById("image");

        assertThat(imageVectorDao.countAll()).isZero();
        assertThat(imageVectorDao.search(new float[]{1, 0, 0}, 10)).isEmpty();
    }

    @Test
    void rejectsDimensionMismatchWithoutRecreatingOrDeletingExistingData() {
        imageVectorDao.insert("existing", "保留的图片", "jpg", null, new float[]{1, 0, 0});
        ImageSearchProperties otherModelProperties = new ImageSearchProperties();
        otherModelProperties.setIndexName(properties.getIndexName());
        otherModelProperties.setDimension(4);

        assertThatThrownBy(() -> newDao(otherModelProperties).countAll())
                .isInstanceOf(AgentException.class).hasMessageContaining("映射不兼容");
        assertThat(imageVectorDao.countAll()).isEqualTo(1);
        assertThat(imageVectorDao.listAll(0, 10)).extracting(ImageSearchResult::getId).containsExactly("existing");
    }

    private ImageElasticsearchDao newDao(ImageSearchProperties configuration) {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory(Map.of("restClient", restClient));
        return new ImageElasticsearchDao(beanFactory.getBeanProvider(Rest5Client.class), configuration);
    }
}
