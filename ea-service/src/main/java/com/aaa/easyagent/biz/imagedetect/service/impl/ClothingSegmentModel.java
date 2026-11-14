package com.aaa.easyagent.biz.imagedetect.service.impl;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.aaa.easyagent.common.config.imagedetect.ImageDetectProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.Collections;

/**
 * 服装分割模型的加载与推理封装。
 *
 * <p>ONNX Runtime 的 session 本身线程安全，可并发调用，所以这里只加载一次并常驻。
 * 内存几乎全在堆外（约 320MB），JVM 堆监控看不到，运维要盯进程 RSS。
 *
 * @author liuzhen.tian
 */
@Slf4j
class ClothingSegmentModel implements AutoCloseable {

    /** ImageNet 均值，取自模型自带的 preprocessor_config.json，不可改 */
    private static final float[] MEAN = {0.485f, 0.456f, 0.406f};

    /** ImageNet 标准差，同上 */
    private static final float[] STD = {0.229f, 0.224f, 0.225f};

    private final OrtEnvironment environment;
    private final OrtSession session;
    private final String inputName;
    private final int inputSize;

    ClothingSegmentModel(ImageDetectProperties properties, InputStream modelStream) throws OrtException, IOException {
        this.inputSize = properties.getInputSize();
        this.environment = OrtEnvironment.getEnvironment();

        byte[] modelBytes = modelStream.readAllBytes();
        if (modelBytes.length < 1024 * 1024) {
            throw new IllegalStateException("模型文件异常，仅 " + modelBytes.length + " 字节");
        }

        try (OrtSession.SessionOptions options = new OrtSession.SessionOptions()) {
            options.setIntraOpNumThreads(Math.max(1, properties.getThreads()));
            options.setInterOpNumThreads(1);
            options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
            if (!properties.isArenaAllocator()) {
                // 关掉内存池与复用：省约 130MB 常驻，单次推理只多 2ms
                options.setCPUArenaAllocator(false);
                options.setMemoryPatternOptimization(false);
            }
            this.session = environment.createSession(modelBytes, options);
        }

        this.inputName = session.getInputNames().iterator().next();
        log.info("[ImageDetect] 模型加载完成 size={}MB input={} threads={} arena={}",
                modelBytes.length / 1048576, inputName, properties.getThreads(), properties.isArenaAllocator());
    }

    int getInputSize() {
        return inputSize;
    }

    /**
     * 对已归一化的输入跑一次推理。
     *
     * @param input NCHW 排列的归一化像素，长度必须是 3 * inputSize * inputSize
     * @return 模型原始输出，形状 [1, 18, H, W]
     */
    float[][][][] infer(float[] input) throws OrtException {
        long[] shape = {1, 3, inputSize, inputSize};
        try (OnnxTensor tensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(input), shape);
             OrtSession.Result result = session.run(Collections.singletonMap(inputName, tensor))) {
            Object value = result.get(0).getValue();
            if (!(value instanceof float[][][][] logits)) {
                throw new IllegalStateException("模型输出类型异常: " + value.getClass().getName());
            }
            return logits;
        }
    }

    /**
     * 把图片像素转成模型要的输入：拉伸到 inputSize、除 255、再做 ImageNet 均值标准差归一化，
     * 最后按 NCHW（通道分离）排列。
     *
     * <p>这套参数必须和训练时一致，错一个值模型输出就是垃圾，而不是精度下降。
     *
     * @param argbPixels 已缩放到 inputSize x inputSize 的 ARGB 像素
     */
    float[] normalize(int[] argbPixels) {
        int pixelCount = inputSize * inputSize;
        if (argbPixels.length != pixelCount) {
            throw new IllegalArgumentException("像素数不匹配: " + argbPixels.length + " != " + pixelCount);
        }
        float[] normalizedPixels = new float[3 * pixelCount];
        for (int i = 0; i < pixelCount; i++) {
            int pixel = argbPixels[i];
            normalizedPixels[i] = (((pixel >> 16) & 0xFF) / 255f - MEAN[0]) / STD[0];
            normalizedPixels[i + pixelCount] = (((pixel >> 8) & 0xFF) / 255f - MEAN[1]) / STD[1];
            normalizedPixels[i + 2 * pixelCount] = ((pixel & 0xFF) / 255f - MEAN[2]) / STD[2];
        }
        return normalizedPixels;
    }

    /** 解析 classpath: / file: 前缀，返回模型输入流。 */
    static InputStream openModel(String location) throws IOException {
        if (!StringUtils.hasText(location)) {
            throw new IllegalArgumentException("easy-agent.image-detect.model-location 未配置");
        }
        String trimmed = location.trim();
        if (trimmed.startsWith("classpath:")) {
            String path = trimmed.substring("classpath:".length());
            InputStream stream = ClothingSegmentModel.class.getClassLoader().getResourceAsStream(path);
            if (stream == null) {
                throw new IOException("classpath 下找不到模型: " + path);
            }
            return stream;
        }
        String path = trimmed.startsWith("file:") ? trimmed.substring("file:".length()) : trimmed;
        java.io.File file = new java.io.File(path);
        if (!file.isFile()) {
            throw new IOException("模型文件不存在: " + file.getAbsolutePath());
        }
        return new java.io.FileInputStream(file);
    }

    @Override
    public void close() {
        try {
            session.close();
            log.info("[ImageDetect] 模型已释放");
        } catch (Exception exception) {
            log.warn("[ImageDetect] 模型释放失败: {}", exception.getMessage());
        }
    }
}
