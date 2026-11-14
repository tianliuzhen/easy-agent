package com.aaa.easyagent.biz.imagedetect.service.impl;

import lombok.extern.slf4j.Slf4j;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * EXIF 方向校正。
 *
 * <p>手机竖拍时传感器仍按横向出图，不旋转像素，只在 EXIF 里写一个 Orientation 标记。
 * 浏览器会按这个标记显示成竖图，而 {@code ImageIO} 完全忽略它，读到的是躺倒的原始像素。
 * 结果是后端拿到 5712x4284、前端看到 4284x5712，框算在躺倒的坐标系里，
 * 贴回竖图后位置全错（上衣框会跑到脸上）。
 *
 * <p>这里自己扫 JPEG 的 APP1 段取 Orientation，避免为一个整数引入 metadata-extractor 依赖。
 *
 * @author liuzhen.tian
 */
@Slf4j
final class ExifOrientation {

    /** 方向正常，无需处理 */
    static final int NORMAL = 1;

    private ExifOrientation() {
    }

    /**
     * 从 JPEG 字节里读 EXIF Orientation。
     *
     * @return 1~8 的方向值；非 JPEG、无 EXIF 或解析失败时返回 1
     */
    static int read(byte[] imageBytes) {
        try {
            return scanJpegOrientation(imageBytes);
        } catch (Exception exception) {
            log.debug("[ImageDetect] 读取 EXIF 方向失败，按正常方向处理: {}", exception.getMessage());
            return NORMAL;
        }
    }

    /**
     * 按 EXIF 方向把图转正。
     *
     * @param orientation {@link #read} 的返回值
     * @return 已转正的图；orientation 为 1 或不识别时原样返回
     */
    static BufferedImage apply(BufferedImage source, int orientation) {
        if (orientation <= NORMAL || orientation > 8) {
            return source;
        }
        int width = source.getWidth();
        int height = source.getHeight();
        // 5~8 含 90 度旋转，画布宽高要互换
        boolean swapSides = orientation >= 5;
        int targetWidth = swapSides ? height : width;
        int targetHeight = swapSides ? width : height;

        BufferedImage target = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setColor(java.awt.Color.WHITE);
            graphics.fillRect(0, 0, targetWidth, targetHeight);
            graphics.setTransform(buildTransform(orientation, width, height));
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    /** EXIF 的 8 种方向对应的仿射变换，编号含义见 TIFF 规范的 Orientation 字段 */
    private static AffineTransform buildTransform(int orientation, int width, int height) {
        AffineTransform transform = new AffineTransform();
        switch (orientation) {
            case 2 -> {
                // 水平镜像
                transform.scale(-1, 1);
                transform.translate(-width, 0);
            }
            case 3 -> {
                // 旋转 180 度
                transform.translate(width, height);
                transform.rotate(Math.PI);
            }
            case 4 -> {
                // 垂直镜像
                transform.scale(1, -1);
                transform.translate(0, -height);
            }
            case 5 -> {
                // 顺时针 90 度 + 水平镜像
                transform.rotate(Math.PI / 2);
                transform.scale(1, -1);
            }
            case 6 -> {
                // 顺时针 90 度，手机竖拍最常见的一种
                transform.translate(height, 0);
                transform.rotate(Math.PI / 2);
            }
            case 7 -> {
                // 逆时针 90 度 + 水平镜像
                transform.scale(-1, 1);
                transform.translate(-height, 0);
                transform.translate(0, width);
                transform.rotate(3 * Math.PI / 2);
            }
            case 8 -> {
                // 逆时针 90 度
                transform.translate(0, width);
                transform.rotate(3 * Math.PI / 2);
            }
            default -> {
                // orientation 1 已在调用方拦掉，这里兜底不做变换
            }
        }
        return transform;
    }

    /**
     * 扫 JPEG 的段结构找 APP1(EXIF)，再按 TIFF 头解析 IFD0 里的 Orientation(tag 0x0112)。
     */
    private static int scanJpegOrientation(byte[] data) {
        if (data == null || data.length < 4) {
            return NORMAL;
        }
        // JPEG 必须以 SOI(FFD8) 开头
        if ((data[0] & 0xFF) != 0xFF || (data[1] & 0xFF) != 0xD8) {
            return NORMAL;
        }

        int offset = 2;
        while (offset + 4 <= data.length) {
            if ((data[offset] & 0xFF) != 0xFF) {
                return NORMAL;
            }
            int marker = data[offset + 1] & 0xFF;
            // SOS 之后是压缩数据，不会再有 EXIF
            if (marker == 0xDA || marker == 0xD9) {
                return NORMAL;
            }
            int segmentLength = ((data[offset + 2] & 0xFF) << 8) | (data[offset + 3] & 0xFF);
            if (segmentLength < 2 || offset + 2 + segmentLength > data.length) {
                return NORMAL;
            }
            if (marker == 0xE1 && isExifHeader(data, offset + 4)) {
                return parseTiffOrientation(data, offset + 10, offset + 2 + segmentLength);
            }
            offset += 2 + segmentLength;
        }
        return NORMAL;
    }

    /** APP1 段以 "Exif\0\0" 开头才是 EXIF，否则可能是 XMP */
    private static boolean isExifHeader(byte[] data, int at) {
        return at + 6 <= data.length
                && data[at] == 'E' && data[at + 1] == 'x' && data[at + 2] == 'i' && data[at + 3] == 'f'
                && data[at + 4] == 0 && data[at + 5] == 0;
    }

    private static int parseTiffOrientation(byte[] data, int tiffStart, int limit) {
        if (tiffStart + 8 > limit) {
            return NORMAL;
        }
        // 字节序标记：II 小端，MM 大端
        boolean littleEndian;
        if (data[tiffStart] == 'I' && data[tiffStart + 1] == 'I') {
            littleEndian = true;
        } else if (data[tiffStart] == 'M' && data[tiffStart + 1] == 'M') {
            littleEndian = false;
        } else {
            return NORMAL;
        }

        long ifdOffset = readUnsignedInt(data, tiffStart + 4, littleEndian);
        int ifdStart = (int) (tiffStart + ifdOffset);
        if (ifdStart + 2 > limit) {
            return NORMAL;
        }

        int entryCount = readUnsignedShort(data, ifdStart, littleEndian);
        for (int i = 0; i < entryCount; i++) {
            int entry = ifdStart + 2 + i * 12;
            if (entry + 12 > limit) {
                return NORMAL;
            }
            int tag = readUnsignedShort(data, entry, littleEndian);
            if (tag == 0x0112) {
                // SHORT 类型，值直接存在偏移字段的前两字节
                int value = readUnsignedShort(data, entry + 8, littleEndian);
                return value >= 1 && value <= 8 ? value : NORMAL;
            }
        }
        return NORMAL;
    }

    private static int readUnsignedShort(byte[] data, int at, boolean littleEndian) {
        int first = data[at] & 0xFF;
        int second = data[at + 1] & 0xFF;
        return littleEndian ? (second << 8) | first : (first << 8) | second;
    }

    private static long readUnsignedInt(byte[] data, int at, boolean littleEndian) {
        long b0 = data[at] & 0xFFL;
        long b1 = data[at + 1] & 0xFFL;
        long b2 = data[at + 2] & 0xFFL;
        long b3 = data[at + 3] & 0xFFL;
        return littleEndian
                ? (b3 << 24) | (b2 << 16) | (b1 << 8) | b0
                : (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
    }
}
