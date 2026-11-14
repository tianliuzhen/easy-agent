package com.aaa.easyagent.biz.imagedetect.service.impl;


import java.util.ArrayList;
import java.util.List;

/**
 * 分割结果解析：把模型输出的逐像素分类分数，转成一个个带外接框的候选块。
 *
 * <p>语义分割给的是按类别合并的区域，不区分实例。所以同类像素还要按连通域拆开，
 * 否则一张图里两个人的上衣会被合成一个横跨整图的大框。
 *
 * @author liuzhen.tian
 */
final class SegmentationMaskParser {

    private SegmentationMaskParser() {
    }

    /**
     * 一个连通区域的外接框与像素数，坐标仍在模型输出网格的尺度上。
     * 只保留通道下标，类别映射交给检测器处理。
     */
    record Component(int channelIndex, int minX, int minY, int maxX, int maxY, int pixelCount) {
    }

    /** 逐像素取最高分类别后的类别图 */
    record Mask(byte[] classes, int width, int height) {

        int pixelCount() {
            return width * height;
        }
    }

    /**
     * 对每个格子取分数最高的类别。
     *
     * @param logits 模型输出，形状 [1, C, H, W]
     */
    static Mask argmax(float[][][][] logits, int expectedChannelCount) {
        float[][][] channels = logits[0];
        int channelCount = channels.length;
        if (channelCount != expectedChannelCount) {
            throw new IllegalStateException("模型类别数与枚举不一致: " + channelCount
                    + " != " + expectedChannelCount);
        }
        int height = channels[0].length;
        int width = channels[0][0].length;

        byte[] classes = new byte[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int bestChannel = 0;
                float bestScore = Float.NEGATIVE_INFINITY;
                for (int channelIndex = 0; channelIndex < channelCount; channelIndex++) {
                    float score = channels[channelIndex][y][x];
                    if (score > bestScore) {
                        bestScore = score;
                        bestChannel = channelIndex;
                    }
                }
                classes[y * width + x] = (byte) bestChannel;
            }
        }
        return new Mask(classes, width, height);
    }

    /**
     * 把类别图按连通域拆成独立实例，再把同类别中互相重叠或紧贴的碎块合并回去。
     *
     * <p>必须合并的原因：低分辨率输入下，袖子与衣身之间的连接会变细甚至断开，
     * 纯连通域会把一件上衣拆成两块，导致最大块只覆盖半件衣服，
     * 同一张图缩放前后拿到的框位置就不一致了。
     *
     * @param minPixels 小于这个像素数的区域丢弃
     */
    static List<Component> splitComponents(Mask mask, int minPixels) {
        return mergeFragments(findRawComponents(mask, minPixels));
    }

    /** 纯连通域扫描，不做任何合并 */
    private static List<Component> findRawComponents(Mask mask, int minPixels) {
        int width = mask.width();
        int height = mask.height();
        int total = width * height;
        byte[] classes = mask.classes();

        boolean[] visited = new boolean[total];
        int[] queue = new int[total];
        List<Component> components = new ArrayList<>();

        for (int start = 0; start < total; start++) {
            int classIndex = classes[start] & 0xFF;
            // 下标 0 固定是背景，不作为候选区域返回。
            if (visited[start] || classIndex == 0) {
                continue;
            }
            visited[start] = true;
            int head = 0;
            int tail = 0;
            queue[tail++] = start;

            int minX = width;
            int minY = height;
            int maxX = -1;
            int maxY = -1;
            int count = 0;

            while (head < tail) {
                int current = queue[head++];
                int x = current % width;
                int y = current / width;
                count++;
                if (x < minX) {
                    minX = x;
                }
                if (x > maxX) {
                    maxX = x;
                }
                if (y < minY) {
                    minY = y;
                }
                if (y > maxY) {
                    maxY = y;
                }

                if (x > 0) {
                    tail = enqueueIfSame(classes, visited, queue, tail, current - 1, classIndex);
                }
                if (x < width - 1) {
                    tail = enqueueIfSame(classes, visited, queue, tail, current + 1, classIndex);
                }
                if (y > 0) {
                    tail = enqueueIfSame(classes, visited, queue, tail, current - width, classIndex);
                }
                if (y < height - 1) {
                    tail = enqueueIfSame(classes, visited, queue, tail, current + width, classIndex);
                }
            }

            if (count >= minPixels) {
                components.add(new Component(
                        classIndex, minX, minY, maxX, maxY, count));
            }
        }
        return components;
    }

    private static int enqueueIfSame(byte[] classes, boolean[] visited, int[] queue,
                                     int tail, int neighbor, int classIndex) {
        if (!visited[neighbor] && (classes[neighbor] & 0xFF) == classIndex) {
            visited[neighbor] = true;
            queue[tail++] = neighbor;
        }
        return tail;
    }

    /**
     * 同类别的碎块若外接框重叠或接近，合并成一个。
     * 反复扫描直到不再有可合并的对，因为一次合并后可能与第三块产生新的重叠。
     */
    private static List<Component> mergeFragments(List<Component> components) {
        List<Component> working = new ArrayList<>(components);
        boolean merged = true;
        while (merged) {
            merged = false;
            outer:
            for (int i = 0; i < working.size(); i++) {
                for (int j = i + 1; j < working.size(); j++) {
                    Component left = working.get(i);
                    Component right = working.get(j);
                    if (left.channelIndex() != right.channelIndex() || !shouldMerge(left, right)) {
                        continue;
                    }
                    working.set(i, union(left, right));
                    working.remove(j);
                    merged = true;
                    break outer;
                }
            }
        }
        return working;
    }

    /**
     * 判断两个同类块是否属于同一件物品。
     *
     * <p>两种情况合并：外接框有交叠，或者间隙小于较大那块尺寸的两成。
     * 后者覆盖「袖子断开」这类被细缝切断的场景，同时避免把左右两只鞋误并成一块
     * （左右鞋是不同类别，本身也不会进这里）。
     */
    private static boolean shouldMerge(Component left, Component right) {
        int gapX = Math.max(0, Math.max(left.minX() - right.maxX(), right.minX() - left.maxX()));
        int gapY = Math.max(0, Math.max(left.minY() - right.maxY(), right.minY() - left.maxY()));
        if (gapX == 0 && gapY == 0) {
            return true;
        }
        int spanX = Math.max(left.maxX() - left.minX(), right.maxX() - right.minX()) + 1;
        int spanY = Math.max(left.maxY() - left.minY(), right.maxY() - right.minY()) + 1;
        return gapX <= Math.max(2, spanX / 5) && gapY <= Math.max(2, spanY / 5);
    }

    private static Component union(Component left, Component right) {
        return new Component(
                left.channelIndex(),
                Math.min(left.minX(), right.minX()),
                Math.min(left.minY(), right.minY()),
                Math.max(left.maxX(), right.maxX()),
                Math.max(left.maxY(), right.maxY()),
                left.pixelCount() + right.pixelCount());
    }
}
