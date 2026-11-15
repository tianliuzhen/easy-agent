package com.aaa.easyagent.biz.imagesearch;

import com.aaa.easyagent.biz.imagesearch.service.impl.ImageSearchServiceImpl;
import com.aaa.easyagent.common.config.exception.AgentException;
import com.aaa.easyagent.common.embedding.DashScopeMultimodalClient;
import com.aaa.easyagent.common.storage.ImageStorage;
import com.aaa.easyagent.core.domain.result.ImageSearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 验证更换存储接口后，上传、图片地址组装、分页与删除失败语义保持一致。
 */
class ImageSearchServiceImplTest {

    private DashScopeMultimodalClient multimodalClient;
    private ImageVectorDao imageVectorDao;
    private ImageStorage imageStorage;
    private ImageSearchServiceImpl imageSearchService;

    @BeforeEach
    void configureService() {
        multimodalClient = mock(DashScopeMultimodalClient.class);
        imageVectorDao = mock(ImageVectorDao.class);
        imageStorage = mock(ImageStorage.class);
        imageSearchService = new ImageSearchServiceImpl(multimodalClient, imageVectorDao, imageStorage);
    }

    @Test
    void uploadsLocalImageThroughSelectedVectorStore() {
        byte[] imageBytes = {1, 2, 3};
        float[] vector = {1, 0, 0};
        when(multimodalClient.embedImage(imageBytes, "image/jpeg")).thenReturn(vector);

        ImageSearchResult result = imageSearchService.upload(
                new MockMultipartFile("file", "sample.jpg", "image/jpeg", imageBytes), "本地图");

        verify(imageStorage).save(result.getId(), imageBytes, "jpg");
        verify(imageVectorDao).insert(result.getId(), "本地图", "jpg", null, vector);
    }

    @Test
    void uploadsRemoteImagePreservingSourceUrl() {
        String sourceUrl = "https://example.test/image.webp";
        float[] vector = {1, 0, 0};
        when(multimodalClient.embedImageByUrl(sourceUrl)).thenReturn(vector);

        ImageSearchResult result = imageSearchService.uploadByUrl(sourceUrl, "远程图");

        verify(imageVectorDao).insert(result.getId(), "远程图", "webp", sourceUrl, vector);
        assertThat(result.getImageUrl()).isEqualTo(sourceUrl);
        verifyNoInteractions(imageStorage);
    }

    @Test
    void keepsRemoteAndLocalImageUrlsInSearchAndListResponses() {
        byte[] imageBytes = {1, 2, 3};
        float[] vector = {1, 0, 0};
        ImageSearchResult remoteImage = image("remote", "https://example.test/image.jpg");
        ImageSearchResult localImage = image("local", null);
        when(multimodalClient.embedImage(imageBytes, "image/jpeg")).thenReturn(vector);
        when(imageVectorDao.search(vector, 10)).thenReturn(List.of(remoteImage, localImage));
        when(imageStorage.publicUrl("local")).thenReturn("/imageSearch/file/local");

        List<ImageSearchResult> results = imageSearchService.searchByImage(
                new MockMultipartFile("file", "query.jpg", "image/jpeg", imageBytes), 0);

        assertThat(results).extracting(ImageSearchResult::getImageUrl)
                .containsExactly("https://example.test/image.jpg", "/imageSearch/file/local");
        when(imageVectorDao.countAll()).thenReturn(2L);
        when(imageVectorDao.listAll(0, 200)).thenReturn(results);
        Map<String, Object> page = imageSearchService.list(0, 1000);
        assertThat(page).containsEntry("total", 2L).containsEntry("limit", 200).containsEntry("data", results);
    }

    @Test
    void doesNotDeleteLocalFileWhenVectorDeletionFails() {
        doThrow(new AgentException("ES 删除失败")).when(imageVectorDao).deleteById("image-id");

        assertThatThrownBy(() -> imageSearchService.delete("image-id"))
                .isInstanceOf(AgentException.class).hasMessage("ES 删除失败");
        verifyNoInteractions(imageStorage);
    }

    private ImageSearchResult image(String id, String sourceUrl) {
        ImageSearchResult result = new ImageSearchResult();
        result.setId(id);
        result.setSourceUrl(sourceUrl);
        return result;
    }
}
