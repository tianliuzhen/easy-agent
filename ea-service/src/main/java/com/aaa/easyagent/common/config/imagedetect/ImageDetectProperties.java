package com.aaa.easyagent.common.config.imagedetect;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 服装主体分割配置，由 config/image-detect-config.yml 独立维护。
 */
@Data
@Component
@ConfigurationProperties(prefix = "easy-agent.image-detect")
public class ImageDetectProperties {

    /** 关闭时不加载模型，模型加载失败也不会阻断应用启动。 */
    private boolean enabled = false;

    /** 支持 classpath: 与 file:，默认使用随 JAR 打包的 18 类服装模型。 */
    private String modelLocation = "classpath:model/seg_clothes.onnx";

    /** 模型输入边长，单位像素；保持 512 以保留鞋、包等小目标。 */
    private int inputSize = 512;

    /** ONNX 单次推理的 CPU 线程数。 */
    private int threads = 2;

    /** 是否启用 ONNX 内存池，默认关闭以降低常驻堆外内存。 */
    private boolean arenaAllocator = false;

    /** 候选区域像素占整图的最小比例，默认 0.4%。 */
    private float minAreaRatio = 0.004f;

    /** 按外接框面积降序最多返回的候选区域数量。 */
    private int maxRegions = 8;

    /** 是否返回脸、头发和四肢等人体部位，默认过滤。 */
    private boolean includeBodyParts = false;

    /** 单张图片的字节数上限，默认 5 MiB。 */
    private long maxImageBytes = 5L * 1024 * 1024;
}
