package com.aaa.easyagent.biz.imagedetect.service.impl;

import com.aaa.easyagent.biz.imagedetect.domain.DetectedRegion;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * 服装候选区域检测器，负责模型前处理、推理和结果解码。
 *
 * @author liuzhen.tian
 */
interface RegionDetector extends AutoCloseable {

    /**
     * 识别候选块。
     *
     * @param image        已按 EXIF 方向转正的原图
     * @param includeNoise 是否保留噪音类别（分割模型指人体部位，检测模型指衣物部件）
     * @param minAreaRatio 候选块的最小面积占比
     * @return 归一化坐标的候选块，按面积从大到小
     */
    List<DetectedRegion> detect(BufferedImage image, boolean includeNoise, float minAreaRatio);

    /** 本次推理耗时（毫秒），只含 {@code session.run}，不含前后处理 */
    long getLastInferMillis();

    @Override
    void close();
}
