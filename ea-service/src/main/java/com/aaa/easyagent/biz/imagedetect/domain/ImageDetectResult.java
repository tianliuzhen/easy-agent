package com.aaa.easyagent.biz.imagedetect.domain;

import lombok.Data;

import java.util.List;

/**
 * 服装主体分割结果。
 *
 * @author liuzhen.tian
 */
@Data
public class ImageDetectResult {

    /** 原图宽度（像素），供前端核对自己拿到的是不是同一张图 */
    private int imageWidth;

    /** 原图高度（像素） */
    private int imageHeight;

    /** 候选块，按面积从大到小 */
    private List<DetectedRegion> regions;

    /** 推理耗时（毫秒），不含读图和前处理 */
    private long inferMillis;

    /** 整体耗时（毫秒），含解码、前处理、推理、后处理 */
    private long totalMillis;
}
