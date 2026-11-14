package com.aaa.easyagent.biz.imagedetect.service;

import com.aaa.easyagent.biz.imagedetect.domain.ImageDetectResult;

/**
 * 服装主体分割服务：从一张图里找出上衣、裤子、鞋、包等候选块，返回归一化坐标。
 *
 * <p>只做定位，不做检索。调用方拿到坐标后自行裁图，再走图搜。
 *
 * @author liuzhen.tian
 */
public interface ImageDetectService {

    /** 是否可用。模型未配置或加载失败时返回 false。 */
    boolean isEnabled();

    /**
     * 识别图片中的服装候选块。
     *
     * @param imageBytes       图片字节，支持 jpg/png/webp 等 ImageIO 能解码的格式
     * @param includeBodyParts 是否保留脸、头发、四肢等人体部位；传 null 时取配置默认值
     * @param minAreaRatio     最小面积占比，低于此值的块丢弃；传 null 时取配置默认值
     * @return 候选块列表，按面积从大到小
     */
    ImageDetectResult detect(byte[] imageBytes, Boolean includeBodyParts, Float minAreaRatio);
}
