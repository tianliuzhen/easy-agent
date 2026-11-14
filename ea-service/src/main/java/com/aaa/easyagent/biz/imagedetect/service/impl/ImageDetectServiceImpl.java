package com.aaa.easyagent.biz.imagedetect.service.impl;

import com.aaa.easyagent.biz.imagedetect.domain.DetectedRegion;
import com.aaa.easyagent.biz.imagedetect.domain.ImageDetectResult;
import com.aaa.easyagent.biz.imagedetect.service.ImageDetectService;
import com.aaa.easyagent.common.config.imagedetect.ImageDetectProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 服装主体分割：解码与方向校正 → 本地 ONNX 推理 → 归一化候选区域。
 * 不访问数据库或远程模型，加载失败时仅停用检测功能。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageDetectServiceImpl implements ImageDetectService {

    private final ImageDetectProperties properties;

    /** 模型只加载一次，关闭或加载失败时保持空值。 */
    private volatile RegionDetector detector;

    /** 加载模型；失败保留日志，其他业务继续正常启动。 */
    @PostConstruct
    void init() {
        if (!properties.isEnabled()) {
            log.info("[ImageDetect] 未启用，跳过模型加载");
            return;
        }
        try {
            validateConfiguration();
            try (InputStream modelStream = ClothingSegmentModel.openModel(properties.getModelLocation())) {
                this.detector = new SegformerClothesDetector(properties, modelStream);
            }
            log.info("[ImageDetect] 服装分割已启用，model={}", properties.getModelLocation());
        } catch (Exception | LinkageError failure) {
            log.error("[ImageDetect] 模型加载失败，检测功能不可用，model={}",
                    properties.getModelLocation(), failure);
        }
    }

    /** 校验影响模型形状、资源占用与过滤规则的配置。 */
    private void validateConfiguration() {
        if (properties.getInputSize() <= 0 || properties.getThreads() <= 0
                || properties.getMaxRegions() <= 0 || properties.getMaxImageBytes() <= 0) {
            throw new IllegalArgumentException("服装分割的输入尺寸、线程数、候选区域数和图片字节上限必须大于 0");
        }
        validateMinAreaRatio(properties.getMinAreaRatio());
    }

    /** 释放 ONNX 原生资源，防止热重启或关闭时泄漏堆外内存。 */
    @PreDestroy
    void destroy() {
        RegionDetector currentDetector = this.detector;
        if (currentDetector != null) {
            currentDetector.close();
            this.detector = null;
        }
    }

    @Override
    public boolean isEnabled() {
        return detector != null;
    }

    /** 校验图片与筛选参数，返回相对方向校正后原图的归一化坐标。 */
    @Override
    public ImageDetectResult detect(byte[] imageBytes, Boolean includeBodyParts, Float minAreaRatio) {
        // 1. 校验可用性、单图字节上限和面积比例，避免无效输入进入模型。
        RegionDetector currentDetector = this.detector;
        if (currentDetector == null) {
            throw new IllegalStateException("服装识别未启用或模型加载失败，请检查 easy-agent.image-detect 配置");
        }
        validateImageBytes(imageBytes);
        float areaThreshold = minAreaRatio != null ? minAreaRatio : properties.getMinAreaRatio();
        validateMinAreaRatio(areaThreshold);
        long startedAtNanos = System.nanoTime();

        // 2. 解码并按 EXIF 方向转正，保证返回坐标与浏览器展示方向一致。
        BufferedImage sourceImage = decodeImage(imageBytes);
        int orientation = ExifOrientation.read(imageBytes);
        if (orientation != ExifOrientation.NORMAL) {
            sourceImage = ExifOrientation.apply(sourceImage, orientation);
        }

        // 3. 推理与连通域解析，按配置过滤人体部位并限制候选区域数量。
        boolean includeNoise = includeBodyParts != null ? includeBodyParts : properties.isIncludeBodyParts();
        List<DetectedRegion> regions = currentDetector.detect(sourceImage, includeNoise, areaThreshold);

        // 4. 返回原图尺寸、候选区域和毫秒耗时，不执行图搜或保存图片。
        ImageDetectResult result = new ImageDetectResult();
        result.setImageWidth(sourceImage.getWidth());
        result.setImageHeight(sourceImage.getHeight());
        result.setRegions(regions);
        result.setInferMillis(currentDetector.getLastInferMillis());
        result.setTotalMillis((System.nanoTime() - startedAtNanos) / 1_000_000);
        return result;
    }

    /** 单图上限使用字节口径，不将超限输入当作空检测结果。 */
    private void validateImageBytes(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("图片内容为空");
        }
        if (imageBytes.length > properties.getMaxImageBytes()) {
            throw new IllegalArgumentException("图片超过 " + properties.getMaxImageBytes() + " 字节限制");
        }
    }

    /** 面积占比必须为有限数且处于 0~1，拒绝 NaN 和无穷值。 */
    private void validateMinAreaRatio(float minAreaRatio) {
        if (!Float.isFinite(minAreaRatio) || minAreaRatio < 0 || minAreaRatio > 1) {
            throw new IllegalArgumentException("minAreaRatio 必须是 0~1 的有限数值");
        }
    }

    /** 使用 ImageIO SPI 解码 JPEG、PNG、WebP 等图片，失败保留原始原因。 */
    private BufferedImage decodeImage(byte[] imageBytes) {
        try (ByteArrayInputStream imageStream = new ByteArrayInputStream(imageBytes)) {
            BufferedImage image = ImageIO.read(imageStream);
            if (image == null) {
                throw new IllegalArgumentException("图片格式不支持或内容损坏，当前支持: "
                        + String.join("/", supportedFormats()));
            }
            return image;
        } catch (IOException exception) {
            throw new IllegalArgumentException("图片读取失败", exception);
        }
    }

    /** 返回实际注册的格式，包含随依赖注册的 WebP 解码器。 */
    private Set<String> supportedFormats() {
        return Arrays.stream(ImageIO.getReaderFormatNames())
                .map(formatName -> formatName.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(TreeSet::new));
    }
}
