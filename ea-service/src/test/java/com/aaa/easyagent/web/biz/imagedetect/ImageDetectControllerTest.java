package com.aaa.easyagent.web.biz.imagedetect;

import com.aaa.easyagent.biz.imagedetect.domain.ImageDetectResult;
import com.aaa.easyagent.biz.imagedetect.service.ImageDetectService;
import com.aaa.easyagent.common.config.imagedetect.ImageDetectProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证迁移后的路由、Multipart 与原始字节参数，以及项目统一响应格式。
 */
class ImageDetectControllerTest {

    private ImageDetectService imageDetectService;
    private ImageDetectProperties properties;
    private MockMvc mockMvc;

    @BeforeEach
    void configureController() {
        imageDetectService = mock(ImageDetectService.class);
        properties = new ImageDetectProperties();
        mockMvc = MockMvcBuilders.standaloneSetup(new ImageDetectController(imageDetectService, properties)).build();
    }

    @Test
    void returnsActualModelAvailability() throws Exception {
        mockMvc.perform(get("/imageDetect/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(false));
    }

    @Test
    void bindsMultipartImageAndOptionalFilters() throws Exception {
        byte[] imageBytes = {1, 2, 3};
        when(imageDetectService.detect(imageBytes, true, 0.01f)).thenReturn(new ImageDetectResult());

        mockMvc.perform(multipart("/imageDetect/regions")
                        .file(new MockMultipartFile("file", "sample.jpg", MediaType.IMAGE_JPEG_VALUE, imageBytes))
                        .param("includeBodyParts", "true").param("minAreaRatio", "0.01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(imageDetectService).detect(imageBytes, true, 0.01f);
    }

    @Test
    void returnsFailureWhenRawImageDetectionIsDisabled() throws Exception {
        byte[] imageBytes = {1, 2, 3};
        when(imageDetectService.detect(imageBytes, null, null))
                .thenThrow(new IllegalStateException("服装识别未启用"));

        mockMvc.perform(post("/imageDetect/regions/bytes")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM).content(imageBytes))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("服装识别未启用"));
    }

    @Test
    void rejectsOversizedMultipartBeforeReadingImageBytes() throws Exception {
        properties.setMaxImageBytes(2);

        mockMvc.perform(multipart("/imageDetect/regions")
                        .file(new MockMultipartFile("file", new byte[]{1, 2, 3})))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(imageDetectService);
    }
}
