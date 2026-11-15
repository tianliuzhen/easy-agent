package com.aaa.easyagent.biz.imagesearch;

import co.elastic.clients.transport.rest5_client.low_level.Request;
import co.elastic.clients.transport.rest5_client.low_level.Response;
import co.elastic.clients.transport.rest5_client.low_level.ResponseException;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import com.aaa.easyagent.common.config.exception.AgentException;
import com.aaa.easyagent.common.config.imagesearch.ImageSearchProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.hc.core5.http.message.RequestLine;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 验证错误输入与连接失败边界，不把存储失败伪装成空列表或成功结果。
 */
class ImageElasticsearchDaoTest {

    private Rest5Client restClient;
    private ImageSearchProperties properties;
    private ImageElasticsearchDao imageVectorDao;

    @BeforeEach
    void configureDao() {
        restClient = mock(Rest5Client.class);
        properties = new ImageSearchProperties();
        properties.setDimension(3);
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory(Map.of("restClient", restClient));
        imageVectorDao = new ImageElasticsearchDao(beanFactory.getBeanProvider(Rest5Client.class), properties);
    }

    @Test
    void rejectsInvalidVectorsBeforeSendingRequests() {
        for (float[] invalidVector : new float[][]{{1, 0}, {0, 0, 0}, {Float.NaN, 0, 1}, {Float.POSITIVE_INFINITY, 0, 1}}) {
            assertThatThrownBy(() -> imageVectorDao.search(invalidVector, 10))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        verifyNoInteractions(restClient);
    }

    @Test
    void rejectsInvalidPaginationAndUnsafeIndexNames() {
        assertThatThrownBy(() -> imageVectorDao.listAll(-1, 24)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> imageVectorDao.listAll(9999, 24)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> imageVectorDao.search(new float[]{1, 0, 0}, 10001))
                .isInstanceOf(IllegalArgumentException.class);
        properties.setIndexName("../_all");
        assertThatThrownBy(() -> imageVectorDao.countAll()).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(restClient);
    }

    @Test
    void preservesConnectionFailureCauseAndRetriesInitializationOnLaterCalls() throws IOException {
        IOException connectionFailure = new IOException("ES connection unavailable");
        when(restClient.performRequest(any(Request.class))).thenThrow(connectionFailure);

        assertThatThrownBy(() -> imageVectorDao.countAll())
                .isInstanceOf(AgentException.class).hasCause(connectionFailure);
        assertThatThrownBy(() -> imageVectorDao.countAll())
                .isInstanceOf(AgentException.class).hasCause(connectionFailure);

        org.mockito.Mockito.verify(restClient, org.mockito.Mockito.times(2)).performRequest(any(Request.class));
    }

    @Test
    void treatsNonSuccessfulSdkResponsesAsFailuresEvenWhenSdkDoesNotThrow() throws IOException {
        Response unavailableResponse = mock(Response.class);
        RequestLine requestLine = mock(RequestLine.class);
        when(requestLine.getMethod()).thenReturn("HEAD");
        when(requestLine.getUri()).thenReturn("/ea_image_search");
        when(unavailableResponse.getRequestLine()).thenReturn(requestLine);
        when(unavailableResponse.getStatusCode()).thenReturn(503);
        when(restClient.performRequest(any(Request.class))).thenReturn(unavailableResponse);

        assertThatThrownBy(() -> imageVectorDao.countAll())
                .isInstanceOf(AgentException.class).hasCauseInstanceOf(ResponseException.class);
    }
}
