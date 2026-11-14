package com.aaa.easyagent.biz.imagedetect;

import com.aaa.easyagent.biz.imagedetect.domain.ClothingRegionType;
import com.aaa.easyagent.biz.imagedetect.domain.DetectedRegion;
import com.aaa.easyagent.biz.imagedetect.domain.ImageDetectResult;
import com.aaa.easyagent.biz.imagedetect.service.ImageDetectService;
import com.aaa.easyagent.biz.imagedetect.service.impl.ImageDetectServiceImpl;
import com.aaa.easyagent.common.config.imagedetect.ImageDetectProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 使用随包模型与真实样图验证 JPEG、WebP、EXIF 方向和小目标过滤规则。
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ImageDetectServiceTest {

    private AnnotationConfigApplicationContext applicationContext;
    private ImageDetectService imageDetectService;

    @BeforeAll
    void loadClasspathModel() {
        ImageDetectProperties properties = new ImageDetectProperties();
        properties.setEnabled(true);
        applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.registerBean(ImageDetectProperties.class, () -> properties);
        applicationContext.register(ImageDetectServiceImpl.class);
        applicationContext.refresh();
        imageDetectService = applicationContext.getBean(ImageDetectService.class);
        assertThat(imageDetectService.isEnabled()).isTrue();
    }

    @AfterAll
    void releaseModel() {
        if (applicationContext != null) {
            applicationContext.close();
        }
    }

    @Test
    void detectsClothesAndShoesWithNormalizedCoordinates() throws IOException {
        ImageDetectResult result = imageDetectService.detect(sample("t_model2.jpg"), null, null);

        assertThat(result.getImageWidth()).isEqualTo(1200);
        assertThat(result.getImageHeight()).isEqualTo(1800);
        assertThat(typesOf(result)).contains(ClothingRegionType.UPPER_CLOTHES.name())
                .doesNotContain(ClothingRegionType.FACE.name());
        assertThat(hasShoe(result)).isTrue();
        assertThat(result.getRegions()).hasSizeLessThanOrEqualTo(8);
        assertNormalizedCoordinates(result);
    }

    @Test
    void decodesWebpWithSameImageDimensions() throws IOException {
        ImageDetectResult result = imageDetectService.detect(sample("t_model2.webp"), false, 0.004f);

        assertThat(result.getImageWidth()).isEqualTo(1200);
        assertThat(result.getImageHeight()).isEqualTo(1800);
        assertThat(typesOf(result)).contains(ClothingRegionType.UPPER_CLOTHES.name());
        assertThat(hasShoe(result)).isTrue();
        assertNormalizedCoordinates(result);
    }

    @Test
    void correctsExifOrientationBeforeReturningImageDimensions() throws IOException {
        ImageDetectResult result = imageDetectService.detect(sample("t_model2_exif6.jpg"), false, null);

        assertThat(result.getImageWidth()).isEqualTo(1800);
        assertThat(result.getImageHeight()).isEqualTo(1200);
        assertThat(typesOf(result)).contains(ClothingRegionType.UPPER_CLOTHES.name());
        assertNormalizedCoordinates(result);
    }

    @Test
    void retainsSmallShoesAtDefaultThresholdAndFiltersAtHigherThreshold() throws IOException {
        byte[] imageBytes = sample("t_model2.jpg");

        assertThat(hasShoe(imageDetectService.detect(imageBytes, false, 0.004f))).isTrue();
        assertThat(hasShoe(imageDetectService.detect(imageBytes, false, 0.025f))).isFalse();
    }

    @Test
    void rejectsEmptyOversizedCorruptImagesAndInvalidAreaRatios() throws IOException {
        assertThatThrownBy(() -> imageDetectService.detect(new byte[0], null, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("为空");
        assertThatThrownBy(() -> imageDetectService.detect(new byte[5 * 1024 * 1024 + 1], null, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("字节限制");
        assertThatThrownBy(() -> imageDetectService.detect(new byte[]{1, 2, 3}, null, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("格式不支持");

        byte[] imageBytes = sample("t_model2.jpg");
        for (float invalidRatio : new float[]{-0.1f, 1.1f, Float.NaN, Float.POSITIVE_INFINITY}) {
            assertThatThrownBy(() -> imageDetectService.detect(imageBytes, null, invalidRatio))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("minAreaRatio");
        }
    }

    private byte[] sample(String fileName) throws IOException {
        return new ClassPathResource("image-detect/" + fileName).getContentAsByteArray();
    }

    private List<String> typesOf(ImageDetectResult result) {
        return result.getRegions().stream().map(DetectedRegion::getType).toList();
    }

    private boolean hasShoe(ImageDetectResult result) {
        return typesOf(result).stream().anyMatch(regionType -> ClothingRegionType.LEFT_SHOE.name().equals(regionType)
                || ClothingRegionType.RIGHT_SHOE.name().equals(regionType));
    }

    private void assertNormalizedCoordinates(ImageDetectResult result) {
        for (DetectedRegion region : result.getRegions()) {
            assertThat(region.getX()).isBetween(0f, 1f);
            assertThat(region.getY()).isBetween(0f, 1f);
            assertThat(region.getWidth()).isPositive();
            assertThat(region.getHeight()).isPositive();
            assertThat(region.getX() + region.getWidth()).isLessThanOrEqualTo(1.001f);
            assertThat(region.getY() + region.getHeight()).isLessThanOrEqualTo(1.001f);
        }
    }
}
