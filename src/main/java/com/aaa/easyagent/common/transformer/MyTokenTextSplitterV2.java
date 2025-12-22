package com.aaa.easyagent.common.transformer;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 支持chunkGap的文本分割器 {@link TokenTextSplitter}
 *
 * @author tlz
 * @version $Id: TokenTextSplitter.java,v 0.1 2024年12月30日  4:42 PM:30 Exp $
 * @see TokenTextSplitter
 */
public class MyTokenTextSplitterV2 extends AbstractTextSplitter {

    private final static int DEFAULT_CHUNK_SIZE = 800;

    private final static int MIN_CHUNK_SIZE_CHARS = 350;

    private final static int MIN_CHUNK_LENGTH_TO_EMBED = 5;

    private final static int MAX_NUM_CHUNKS = 10000;

    private final static boolean KEEP_SEPARATOR = true;

    private final EncodingRegistry registry = Encodings.newLazyEncodingRegistry();

    private final Encoding encoding = this.registry.getEncoding(EncodingType.CL100K_BASE);

    // The target size of each text chunk in tokens
    private final int chunkSize;

    // The minimum size of each text chunk in characters
    private final int minChunkSizeChars;

    // Discard chunks shorter than this
    private final int minChunkLengthToEmbed;

    // The maximum number of chunks to generate from a text
    private final int maxNumChunks;

    private final boolean keepSeparator;

    // 设置块重叠可以保持它们之间的语义相关性，增强检索效果。推荐设置 最大块大小的10%-25%。
    private int chunkOverlapSize = 50;

    public MyTokenTextSplitterV2() {
        this(DEFAULT_CHUNK_SIZE, MIN_CHUNK_SIZE_CHARS, MIN_CHUNK_LENGTH_TO_EMBED, MAX_NUM_CHUNKS, KEEP_SEPARATOR);
    }

    public MyTokenTextSplitterV2(boolean keepSeparator) {
        this(DEFAULT_CHUNK_SIZE, MIN_CHUNK_SIZE_CHARS, MIN_CHUNK_LENGTH_TO_EMBED, MAX_NUM_CHUNKS, keepSeparator);
    }

    public MyTokenTextSplitterV2(int chunkSize, int minChunkSizeChars, int minChunkLengthToEmbed, int maxNumChunks,
                                 boolean keepSeparator) {
        this.chunkSize = chunkSize;
        this.minChunkSizeChars = minChunkSizeChars;
        this.minChunkLengthToEmbed = minChunkLengthToEmbed;
        this.maxNumChunks = maxNumChunks;
        this.keepSeparator = keepSeparator;
    }

    public MyTokenTextSplitterV2(int chunkSize, int minChunkSizeChars, int minChunkLengthToEmbed, int maxNumChunks,
                                 boolean keepSeparator, int chunkOverlapSize) {
        this.chunkSize = chunkSize;
        this.minChunkSizeChars = minChunkSizeChars;
        this.minChunkLengthToEmbed = minChunkLengthToEmbed;
        this.maxNumChunks = maxNumChunks;
        this.keepSeparator = keepSeparator;
        this.chunkOverlapSize = chunkOverlapSize;
    }

    public static MyTokenTextSplitterV2.Builder builder() {
        return new MyTokenTextSplitterV2.Builder();
    }

    @Override
    protected List<String> splitText(String text) {
        return doSplit(text, this.chunkSize);
    }

    /**
     * 重写 org.springframework.ai.transformer.splitter.TokenTextSplitter#doSplit(java.lang.String, int)
     * 支持 overlapSize
     * <p>
     * 不使用块间隙覆盖
     * token = 1000
     * chunkSize = 400
     * overlapSize=0
     * ==================》400,400-800,800-1000
     * <p>
     * 使用块间隙覆盖
     * token = 1000
     * chunkSize = 400
     * overlapSize =100
     * ==================》400,300-700,600-1000
     *
     * @param text
     * @param chunkSize
     * @return
     */
    protected List<String> doSplit(String text, int chunkSize) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<Integer> tokens = getEncodedTokens(text);
        List<String> chunks = new ArrayList<>();
        int num_chunks = 0;
        int start_index = 0;
        while (!tokens.isEmpty() && num_chunks < this.maxNumChunks) {
            List<Integer> chunk = tokens.subList(0, Math.min(chunkSize, tokens.size()));

            String chunkText = decodeTokens(chunk);

            // Skip the chunk if it is empty or whitespace
            if (chunkText.trim().isEmpty()) {
                tokens = tokens.subList(chunk.size(), tokens.size());
                continue;
            }

            // Find the last period or punctuation mark in the chunk
            int lastPunctuation = Math.max(chunkText.lastIndexOf('.'), Math.max(chunkText.lastIndexOf('?'),
                    Math.max(chunkText.lastIndexOf('!'), chunkText.lastIndexOf('\n'))));

            // 根据最后出现的标志性标点进行分割
            if (lastPunctuation != -1 && lastPunctuation > this.minChunkSizeChars) {
                // Truncate the chunk text at the punctuation mark
                chunkText = chunkText.substring(0, lastPunctuation + 1);
            }

            String chunkTextToAppend = (this.keepSeparator) ? chunkText.trim()
                    : chunkText.replace(System.lineSeparator(), " ").trim();
            if (chunkTextToAppend.length() > this.minChunkLengthToEmbed) {
                chunks.add(chunkTextToAppend);
            }

            // 支持分割间隙,每次 chunks.add 后，往前移动token
            if (chunkText.length() > chunkSize) {
                chunkText = chuckGapSkipLastPunctuation(chunkText, chunkOverlapSize);
            }

            // Remove the tokens corresponding to the chunk text from the remaining tokens
            tokens = tokens.subList(getEncodedTokens(chunkText).size(), tokens.size());

            num_chunks++;
        }

        // Handle the remaining tokens
        if (!tokens.isEmpty()) {
            String remaining_text = decodeTokens(tokens).replace(System.lineSeparator(), " ").trim();
            if (remaining_text.length() > this.minChunkLengthToEmbed) {
                chunks.add(remaining_text);
            }
        }

        return chunks;
    }

    private List<Integer> getEncodedTokens(String text) {
        Assert.notNull(text, "Text must not be null");
        return this.encoding.encode(text).boxed();
    }


    private String decodeTokens(List<Integer> tokens) {
        Assert.notNull(tokens, "Tokens must not be null");
        var tokensIntArray = new IntArrayList(tokens.size());
        tokens.forEach(tokensIntArray::add);
        return this.encoding.decode(tokensIntArray);
    }

    public static final class Builder {

        private int chunkSize;

        private int minChunkSizeChars;

        private int minChunkLengthToEmbed;

        private int maxNumChunks;

        private boolean keepSeparator;

        private Builder() {
        }

        public MyTokenTextSplitterV2.Builder withChunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
            return this;
        }

        public MyTokenTextSplitterV2.Builder withMinChunkSizeChars(int minChunkSizeChars) {
            this.minChunkSizeChars = minChunkSizeChars;
            return this;
        }

        public MyTokenTextSplitterV2.Builder withMinChunkLengthToEmbed(int minChunkLengthToEmbed) {
            this.minChunkLengthToEmbed = minChunkLengthToEmbed;
            return this;
        }

        public MyTokenTextSplitterV2.Builder withMaxNumChunks(int maxNumChunks) {
            this.maxNumChunks = maxNumChunks;
            return this;
        }

        public MyTokenTextSplitterV2.Builder withKeepSeparator(boolean keepSeparator) {
            this.keepSeparator = keepSeparator;
            return this;
        }

        public MyTokenTextSplitterV2 build() {
            return new MyTokenTextSplitterV2(this.chunkSize, this.minChunkSizeChars, this.minChunkLengthToEmbed,
                    this.maxNumChunks, this.keepSeparator);
        }

    }


    /**
     * 覆盖间隙并且根据最后标点符合拆分给每个块追加，上下块之前的间隙。
     * <p>
     * 如给出文本如下：627，当gap=100后，也就是527 长度，但是直接分割会造成上下午语义割裂。不利于rag检索，
     * 为了支持更好适配，在100长度内，根据标点或者换行符，定位最小的语义下标进行截取。
     * <p>
     * 是的，Java中的类加载（Class Loading）工作主要由ClassLoader及其子类负责。类加载是将类的字节码文件读入并转化为Class对象的过程。这个过程通常包含以下几个步骤：加载（Loading
     * ）：通过类的全限定名（包括包名）来查找该类的字节码文件，并将其读入内存。ClassLoader负责这个过程。链接（Linking）：链接过程又分为三个阶段：验证（Verification
     * ）：确保所加载的类文件的字节码是正确的，没有安全问题。准备（Preparation）：为类变量分配内存，并设置默认值。解析（Resolution
     * ）：将类中常量池中的符号引用转换为直接引用。初始化（Initialization）：执行类的初始化方法（<clinit>）。这个过程会执行类变量的初始化和静态代码块。在Java中，有以下几种常用的ClassLoader
     * 子类：Bootstrap ClassLoader：加载JRE的核心类库（如java.lang、java.util等）。Extension
     * ClassLoader：加载JRE的扩展库（如jre/lib/ext目录下的类）。Application
     * ClassLoader：加载用户类路径（classpath）下的类。开发者也可以定义自己的ClassLoader
     * 子类，以实现特殊的加载需求。通过创建自定义类加载器，开发者可以控制类的加载流程，例如实现热部署、隔离加载等功能类的加载可分为隐式加载和显示加载
     * <p>
     * 是的，Java中的类加载（Class Loading）工作主要由ClassLoader及其子类负责。类加载是将类的字节码文件读入并转化为Class对象的过程。这个过程通常包含以下几个步骤：加载（Loading
     * ）：通过类的全限定名（包括包名）来查找该类的字节码文件，并将其读入内存。ClassLoader负责这个过程。链接（Linking）：链接过程又分为三个阶段：验证（Verification
     * ）：确保所加载的类文件的字节码是正确的，没有安全问题。准备（Preparation）：为类变量分配内存，并设置默认值。解析（Resolution
     * ）：将类中常量池中的符号引用转换为直接引用。初始化（Initialization）：执行类的初始化方法（<clinit>）。这个过程会执行类变量的初始化和静态代码块。在Java中，有以下几种常用的ClassLoader
     * 子类：Bootstrap ClassLoader：加载JRE的核心类库（如java.lang、java.util等）。Extension
     * ClassLoader：加载JRE的扩展库（如jre/lib/ext目录下的类）。Application
     * ClassLoader：加载用户类路径（classpath）下的类。开发者也可以定义自己的ClassLoader子类，以实现特殊的加载需求。
     * <p>
     *
     * @param chunkText
     * @param chuckGap
     * @return
     */
    public static String chuckGapSkipLastPunctuation(String chunkText, int chuckGap) {
        // 截取块间隙
        int beginIndex = chunkText.length() - chuckGap;
        String tempChunkText = chunkText.substring(beginIndex);

        // 计算当前快间隙区间内的最早出现的字符串
        List<Integer> lastIndexOf = List.of(tempChunkText.lastIndexOf('.'),
                tempChunkText.lastIndexOf('?'),
                tempChunkText.lastIndexOf('。'),
                tempChunkText.lastIndexOf('，'),
                tempChunkText.lastIndexOf(','),
                tempChunkText.lastIndexOf('!'),
                tempChunkText.lastIndexOf('\n')
        );

        Optional<Integer> min = lastIndexOf.stream().filter(e -> e > 0).min(Integer::compareTo);
        if (min.isPresent()) {
            // 返回块间隙内的完整句子
            return chunkText.substring(0, beginIndex + min.get() + 1);
        }
        return chunkText;
    }

}
