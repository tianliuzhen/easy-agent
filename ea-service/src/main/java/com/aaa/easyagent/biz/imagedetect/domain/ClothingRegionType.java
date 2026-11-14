package com.aaa.easyagent.biz.imagedetect.domain;

import lombok.Getter;

/**
 * 服装分割模型（segformer_b0_clothes）的输出类别。
 * 顺序与模型 config.json 的 id2label 严格一致，不可调整。
 *
 * @author liuzhen.tian
 */
@Getter
public enum ClothingRegionType {

    /** 背景，不作为候选块返回 */
    BACKGROUND(0, "背景", false, false),
    /** 帽子 */
    HAT(1, "帽子", true, false),
    /** 头发 */
    HAIR(2, "头发", false, true),
    /** 墨镜 */
    SUNGLASSES(3, "墨镜", true, false),
    /** 上衣 */
    UPPER_CLOTHES(4, "上衣", true, false),
    /** 半身裙 */
    SKIRT(5, "半身裙", true, false),
    /** 裤子 */
    PANTS(6, "裤子", true, false),
    /** 连衣裙 */
    DRESS(7, "连衣裙", true, false),
    /** 腰带 */
    BELT(8, "腰带", true, false),
    /** 左鞋 */
    LEFT_SHOE(9, "左鞋", true, false),
    /** 右鞋 */
    RIGHT_SHOE(10, "右鞋", true, false),
    /** 脸 */
    FACE(11, "脸", false, true),
    /** 左腿 */
    LEFT_LEG(12, "左腿", false, true),
    /** 右腿 */
    RIGHT_LEG(13, "右腿", false, true),
    /** 左臂 */
    LEFT_ARM(14, "左臂", false, true),
    /** 右臂 */
    RIGHT_ARM(15, "右臂", false, true),
    /** 包 */
    BAG(16, "包", true, false),
    /** 围巾 */
    SCARF(17, "围巾", true, false);

    /** 模型输出通道下标 */
    private final int channelIndex;

    /** 中文名，直接给前端展示 */
    private final String displayName;

    /** 是否属于可售商品类别，只有这类才值得拿去图搜 */
    private final boolean sellable;

    /** 是否属于人体部位，电商场景视为噪音 */
    private final boolean bodyPart;

    ClothingRegionType(int channelIndex, String displayName, boolean sellable, boolean bodyPart) {
        this.channelIndex = channelIndex;
        this.displayName = displayName;
        this.sellable = sellable;
        this.bodyPart = bodyPart;
    }

    private static final ClothingRegionType[] BY_CHANNEL = values();

    /**
     * 按模型输出通道下标取类别。
     *
     * @param channelIndex 通道下标，越界时返回 BACKGROUND
     */
    public static ClothingRegionType ofChannel(int channelIndex) {
        if (channelIndex < 0 || channelIndex >= BY_CHANNEL.length) {
            return BACKGROUND;
        }
        return BY_CHANNEL[channelIndex];
    }

    /** 模型总类别数，用于校验模型输出维度 */
    public static int channelCount() {
        return BY_CHANNEL.length;
    }
}
