package com.aaa.easyagent.common.transformer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 递归文本分割器
 * <p>
 * 多层级递归分割文本，按 {@link #SEPARATORS} 优先级逐层尝试，直到所有块 ≤ chunkSize：
 * <ul>
 *   <li>第1层：按 {@code \n\n} 分割为段落</li>
 *   <li>第2层：按 {@code \n} 分割为行</li>
 *   <li>第3层：按 {@link #SENTENCE_SEPARATOR_REGEX} 分割为句子，组合为 ≈chunkSize 的块</li>
 *   <li>第4层：按字符硬分割（兜底）</li>
 * </ul>
 * 分割完成后在相邻块之间添加重叠（overlap），增强 RAG 检索效果。
 * <p>
 * 例如：文章 500 字，chunkSize=100，overlap=100
 * <pre>
 *   第1层：按 \n\n 切 → 段落 150/200/150 字（均＞100，下沉）
 *   第2层：按 \n 切（段落内无 \n，跳过 → 下沉）
 *   第3层：按 。切 → 短句 → 组合为 3+4+2=9 个块（每块≈100 字）
 *   添加重叠 → 9 个块，块间重叠 100 字
 * </pre>
 *
 * @author tlz
 */
public class MyRecursiveTextSplitter extends AbstractTextSplitter {

    private final static int DEFAULT_CHUNK_SIZE = 100;

    private final static int DEFAULT_CHUNK_OVERLAP = 0;

    private final static int DEFAULT_MIN_CHUNK_LENGTH = 5;

    private final static double DEFAULT_MIN_CHUNK_RATIO = 0.5;

    /** 递归分隔符数组：按优先级逐层尝试分割 */
    private final static String[] SEPARATORS = {"\n\n", "\n"};

    /** 句子级分隔符正则（保留标点） */
    private final static String SENTENCE_SEPARATOR_REGEX = "(?<=[。！？!?])";

    private final int chunkSize;

    private final int chunkOverlap;

    private final int minChunkLength;

    /** 最小块比例：块小于 chunkSize * minChunkRatio 时向后合并到前一块 */
    private final double minChunkRatio;

    public MyRecursiveTextSplitter() {
        this(DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP, DEFAULT_MIN_CHUNK_LENGTH, DEFAULT_MIN_CHUNK_RATIO);
    }

    public MyRecursiveTextSplitter(int chunkSize, int chunkOverlap, int minChunkLength) {
        this(chunkSize, chunkOverlap, minChunkLength, DEFAULT_MIN_CHUNK_RATIO);
    }

    public MyRecursiveTextSplitter(int chunkSize, int chunkOverlap, int minChunkLength, double minChunkRatio) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
        this.minChunkLength = minChunkLength;
        this.minChunkRatio = minChunkRatio;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected List<String> splitText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // 1. 递归分割，得到基础块
        List<String> baseChunks = recursiveSplit(text.trim(), 0);

        // 2. 合并太小的分块（< chunkSize * minChunkRatio），向后合并到前一块
        baseChunks = mergeSmallChunks(baseChunks);

        // 3. 在相邻块之间添加重叠
        return applyChunkOverlap(baseChunks);
    }

    // ==================== 递归分割 ====================

    /**
     * 递归分割文本
     *
     * @param text  待分割文本
     * @param level 当前层级（0=段落, 1=行, 2=句子, 3+=字符兜底）
     */
    private List<String> recursiveSplit(String text, int level) {
        if (text.isEmpty()) {
            return Collections.emptyList();
        }

        // 已在目标大小内，直接返回
        if (text.length() <= chunkSize) {
            String trimmed = text.trim();
            if (trimmed.length() >= minChunkLength) {
                return Collections.singletonList(trimmed);
            }
            return Collections.emptyList();
        }

        String separator = getSeparator(level);

        // 句子层级：按句号分割后组合为 ≈chunkSize 的块
        if (separator == null) {
            return splitBySentenceGrouping(text);
        }

        List<String> parts = splitBySeparator(text, separator);

        if (parts.isEmpty()) {
            return Collections.emptyList();
        }

        // 分隔符未实际分割文本（只有一段），下沉到下一层
        if (parts.size() == 1) {
            return recursiveSplit(parts.get(0), level + 1);
        }

        // 多段：小段合并成组，大段递归下沉
        List<String> result = new ArrayList<>();
        List<String> buffer = new ArrayList<>();
        int bufferLength = 0;

        for (String part : parts) {
            if (part.length() > chunkSize) {
                // 清空缓冲区
                flushBuffer(buffer, bufferLength, separator, result);
                buffer.clear();
                bufferLength = 0;
                // 大段下沉到下一层级递归处理
                result.addAll(recursiveSplit(part, level + 1));
            } else {
                // 累积小段，达到 chunkSize 时 flush 为一组
                if (bufferLength + part.length() > chunkSize && !buffer.isEmpty()) {
                    flushBuffer(buffer, bufferLength, separator, result);
                    buffer.clear();
                    bufferLength = 0;
                }
                buffer.add(part);
                bufferLength += part.length();
            }
        }

        // 处理剩余缓冲区
        flushBuffer(buffer, bufferLength, separator, result);

        return result;
    }

    // ==================== 句子级别处理 ====================

    /**
     * 按句号分割句子，然后组合为 ≈chunkSize 的块。
     * 如果文本无标点可分割，降级为字符级硬分割。
     */
    private List<String> splitBySentenceGrouping(String text) {
        String[] sentences = text.split(SENTENCE_SEPARATOR_REGEX);

        // 无标点可分割 → 按字符硬分割
        if (sentences.length <= 1) {
            return splitByCharacter(text);
        }

        List<String> groups = new ArrayList<>();
        List<String> buffer = new ArrayList<>();
        int bufferLength = 0;

        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (sentence.isEmpty()) {
                continue;
            }

            // 单个句子超过目标大小，独立成块（不做句内切割，保留语义完整性）
            if (sentence.length() > chunkSize) {
                flushSentenceBuffer(buffer, bufferLength, groups);
                buffer.clear();
                bufferLength = 0;
                if (sentence.length() >= minChunkLength) {
                    groups.add(sentence);
                }
                continue;
            }

            // 组合短句到 ≈chunkSize
            if (bufferLength + sentence.length() > chunkSize && !buffer.isEmpty()) {
                flushSentenceBuffer(buffer, bufferLength, groups);
                buffer.clear();
                bufferLength = 0;
            }

            buffer.add(sentence);
            bufferLength += sentence.length();
        }

        flushSentenceBuffer(buffer, bufferLength, groups);

        // 未形成任何块时降级
        return groups.isEmpty() ? splitByCharacter(text) : groups;
    }

    private void flushSentenceBuffer(List<String> buffer, int bufferLength, List<String> result) {
        if (buffer.isEmpty()) {
            return;
        }
        String joined = String.join("", buffer);
        if (joined.length() >= minChunkLength) {
            result.add(joined);
        }
    }

    // ==================== 字符级兜底 ====================

    /**
     * 按字符硬分割（兜底策略），每块固定 chunkSize 字符。
     */
    private List<String> splitByCharacter(String text) {
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < text.length(); i += chunkSize) {
            int end = Math.min(i + chunkSize, text.length());
            String chunk = text.substring(i, end).trim();
            if (chunk.length() >= minChunkLength) {
                chunks.add(chunk);
            }
        }
        return chunks;
    }

    // ==================== 工具方法 ====================

    /**
     * 获取当前层级的分隔符，超出分隔符数组范围返回 null（进入句子级/字符级处理）
     */
    private String getSeparator(int level) {
        if (level < SEPARATORS.length) {
            return SEPARATORS[level];
        }
        return null;
    }

    /**
     * 按分隔符分割文本，过滤空白部分。
     */
    private List<String> splitBySeparator(String text, String separator) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        while (true) {
            int idx = text.indexOf(separator, start);
            if (idx == -1) {
                String remaining = text.substring(start).trim();
                if (!remaining.isEmpty()) {
                    parts.add(remaining);
                }
                break;
            }
            String part = text.substring(start, idx).trim();
            if (!part.isEmpty()) {
                parts.add(part);
            }
            start = idx + separator.length();
        }
        return parts;
    }

    /**
     * 将缓冲区中的小段合并为一块并加入结果
     */
    private void flushBuffer(List<String> buffer, int bufferLength, String separator, List<String> result) {
        if (buffer.isEmpty()) {
            return;
        }
        String joined = String.join(separator, buffer);
        if (joined.length() >= minChunkLength) {
            result.add(joined);
        }
    }

    // ==================== 小块合并 ====================

    /**
     * 合并太小的分块（< chunkSize * minChunkRatio），向后合并到前一块。
     * <p>
     * 例如 chunkSize=100, minChunkRatio=0.5，块长 35 字 < 50 字 → 合并到前一块。
     * 避免 RAG 场景中出现信息量过少的碎片块。
     */
    private List<String> mergeSmallChunks(List<String> chunks) {
        if (chunks.size() <= 1) {
            return chunks;
        }

        double minChunkThreshold = chunkSize * minChunkRatio;

        List<String> result = new ArrayList<>();
        for (String chunk : chunks) {
            if (result.isEmpty()) {
                result.add(chunk);
                continue;
            }

            // 当前块太小 → 向后合并到前一块
            if (chunk.length() < minChunkThreshold) {
                int lastIdx = result.size() - 1;
                result.set(lastIdx, result.get(lastIdx) + chunk);
            } else {
                result.add(chunk);
            }
        }
        return result;
    }

    // ==================== 块重叠 ====================

    /**
     * 在相邻块之间添加重叠（overlap）。
     * <p>
     * 第一块保持不变，后续每个块在其开头追加前一块末尾的 overlap 字符。
     * 例如：chunks=[A, B, C], overlap=100 → [A, last100(A)+B, last100(B)+C]
     */
    private List<String> applyChunkOverlap(List<String> chunks) {
        if (chunks.size() <= 1 || chunkOverlap <= 0) {
            return chunks;
        }

        List<String> result = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            if (i == 0) {
                result.add(chunks.get(i));
            } else {
                String prevChunk = chunks.get(i - 1);
                String currChunk = chunks.get(i);
                int overlapLen = Math.min(chunkOverlap, prevChunk.length());
                String overlap = prevChunk.substring(prevChunk.length() - overlapLen);
                result.add(overlap + currChunk);
            }
        }
        return result;
    }

    // ==================== Builder ====================

    public static final class Builder {

        private int chunkSize = DEFAULT_CHUNK_SIZE;

        private int chunkOverlap = DEFAULT_CHUNK_OVERLAP;

        private int minChunkLength = DEFAULT_MIN_CHUNK_LENGTH;

        private double minChunkRatio = DEFAULT_MIN_CHUNK_RATIO;

        private Builder() {
        }

        public Builder withChunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
            return this;
        }

        public Builder withChunkOverlap(int chunkOverlap) {
            this.chunkOverlap = chunkOverlap;
            return this;
        }

        public Builder withMinChunkLength(int minChunkLength) {
            this.minChunkLength = minChunkLength;
            return this;
        }

        public Builder withMinChunkRatio(double minChunkRatio) {
            this.minChunkRatio = minChunkRatio;
            return this;
        }

        public MyRecursiveTextSplitter build() {
            return new MyRecursiveTextSplitter(this.chunkSize, this.chunkOverlap, this.minChunkLength, this.minChunkRatio);
        }
    }
}
