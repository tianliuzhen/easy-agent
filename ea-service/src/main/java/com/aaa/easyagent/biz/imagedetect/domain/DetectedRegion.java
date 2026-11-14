package com.aaa.easyagent.biz.imagedetect.domain;

import lombok.Data;

/**
 * 一个候选块。
 *
 * <p>坐标全部是相对原图的归一化比例（0~1），不是像素。这样前端无论把图压缩成多大、
 * 或按什么尺寸显示，都用自己的尺寸乘一遍即可对齐，不需要和后端约定同一个分辨率：
 * <pre>
 *   realX = x * image.naturalWidth
 *   realY = y * image.naturalHeight
 * </pre>
 *
 * @author liuzhen.tian
 */
@Data
public class DetectedRegion {

    /** 类别枚举名，如 UPPER_CLOTHES，供前端做逻辑判断 */
    private String type;

    /** 类别中文名，如「上衣」，供前端直接展示 */
    private String typeName;

    /** 是否属于可售商品类别。人体部位为 false。 */
    private boolean sellable;

    /** 左上角横坐标占原图宽度的比例，0~1 */
    private float x;

    /** 左上角纵坐标占原图高度的比例，0~1 */
    private float y;

    /** 宽度占原图宽度的比例，0~1 */
    private float width;

    /** 高度占原图高度的比例，0~1 */
    private float height;

    /** 该区域像素数占全图的比例；这是面积占比，不是模型置信度。 */
    private float areaRatio;
}
