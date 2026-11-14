package com.aaa.easyagent.biz.imagedetect.service.impl;

import com.aaa.easyagent.common.config.imagedetect.ImageDetectProperties;
import com.aaa.easyagent.biz.imagedetect.domain.ClothingRegionType;
import com.aaa.easyagent.biz.imagedetect.domain.DetectedRegion;
import lombok.extern.slf4j.Slf4j;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * SegFormer 服装语义分割检测器，使用 seg_clothes.onnx 对应的 18 类标签。
 *
 * <p>链路：拉伸到 512×512 → ImageNet 归一化 → 推理 → 逐像素 argmax
 * → 连通域拆实例 → 碎块合并 → 换算归一化坐标。
 *
 * @author liuzhen.tian
 */
@Slf4j
class SegformerClothesDetector implements RegionDetector {

    private final ImageDetectProperties properties;
    private final ClothingSegmentModel model;
    private volatile long lastInferMillis;

    SegformerClothesDetector(ImageDetectProperties properties, InputStream modelStream) throws Exception {
        this.properties = properties;
        this.model = new ClothingSegmentModel(properties, modelStream);
    }

    @Override
    public List<DetectedRegion> detect(BufferedImage image, boolean includeNoise, float minAreaRatio) {
        // 1. 拉伸到模型输入尺寸，并使用训练时的 ImageNet 参数归一化。
        int inputSize = model.getInputSize();
        float[] input = model.normalize(resizeToSquare(image, inputSize));

        // 2. 执行本地推理，耗时只统计 ONNX 调用，单位毫秒。
        long inferStartedAtNanos = System.nanoTime();
        float[][][][] logits;
        try {
            logits = model.infer(input);
        } catch (Exception exception) {
            throw new IllegalStateException("服装分割推理失败: " + rootMessage(exception), exception);
        }
        lastInferMillis = (System.nanoTime() - inferStartedAtNanos) / 1_000_000;

        // 3. 逐像素分类，按连通域拆分和合并碎块，换算为归一化坐标。
        SegmentationMaskParser.Mask mask = SegmentationMaskParser.argmax(logits, ClothingRegionType.channelCount());
        int minPixels = Math.max(4, Math.round(mask.pixelCount() * minAreaRatio));
        List<SegmentationMaskParser.Component> components =
                SegmentationMaskParser.splitComponents(mask, minPixels);

        return toRegions(components, mask, includeNoise);
    }

    /**
     * 缩放到 size × size。
     *
     * <p>直接拉伸、不保持长宽比，与模型 preprocessor_config.json 的 size 512 对齐，
     * 也和前端验证页一致。换成补黑边会让坐标换算多一层偏移，且与已验证的效果不符。
     */
    private int[] resizeToSquare(BufferedImage source, int size) {
        BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaled.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            // TYPE_INT_RGB 默认黑底，先铺白再画，PNG/WebP 的透明区域才不会被当成黑色
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, size, size);
            graphics.drawImage(source, 0, 0, size, size, null);
        } finally {
            graphics.dispose();
        }
        return scaled.getRGB(0, 0, size, size, null, 0, size);
    }

    /** 把连通域换算成归一化坐标，过滤噪音类别，按面积降序截断 */
    private List<DetectedRegion> toRegions(List<SegmentationMaskParser.Component> components,
                                           SegmentationMaskParser.Mask mask,
                                           boolean includeNoise) {
        float gridWidth = mask.width();
        float gridHeight = mask.height();
        float gridPixels = gridWidth * gridHeight;

        List<DetectedRegion> regions = new ArrayList<>(components.size());
        for (SegmentationMaskParser.Component component : components) {
            DetectedRegion region = describe(component.channelIndex(), includeNoise);
            if (region == null) {
                continue;
            }
            // 网格坐标直接除以网格尺寸就是归一化比例，和原图分辨率无关
            region.setX(component.minX() / gridWidth);
            region.setY(component.minY() / gridHeight);
            region.setWidth((component.maxX() - component.minX() + 1) / gridWidth);
            region.setHeight((component.maxY() - component.minY() + 1) / gridHeight);
            region.setAreaRatio(component.pixelCount() / gridPixels);
            regions.add(region);
        }

        regions.sort(Comparator.comparingDouble(
                (DetectedRegion region) -> (double) region.getWidth() * region.getHeight()).reversed());
        int limit = Math.max(1, properties.getMaxRegions());
        return regions.size() > limit ? new ArrayList<>(regions.subList(0, limit)) : regions;
    }

    /**
     * 按当前类别表把通道下标翻成类别信息。
     *
     * @return 噪音类别且不保留时返回 null
     */
    private DetectedRegion describe(int channelIndex, boolean includeNoise) {
        DetectedRegion region = new DetectedRegion();
        ClothingRegionType type = ClothingRegionType.ofChannel(channelIndex);
        if (type == ClothingRegionType.BACKGROUND || (!includeNoise && type.isBodyPart())) {
            return null;
        }
        region.setType(type.name());
        region.setTypeName(type.getDisplayName());
        region.setSellable(type.isSellable());
        return region;
    }

    @Override
    public long getLastInferMillis() {
        return lastInferMillis;
    }

    @Override
    public void close() {
        model.close();
    }

    private static String rootMessage(Throwable failure) {
        Throwable current = failure;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
