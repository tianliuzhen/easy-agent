package com.aaa.easyagent.common.transformer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.ContentFormatter;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.transformer.splitter.TextSplitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * 支持转换id的抽象文本分割器 {@link TextSplitter}
 *
 * @author tlz
 * @version $Id: AbstractTextSplitter.java,v 0.1 2025年01月01日  5:00 PM:34 Exp $
 */
public abstract class AbstractTextSplitter implements DocumentTransformer {
    private static final Logger logger = LoggerFactory.getLogger(TextSplitter.class);

    /**
     * If true the children documents inherit the content-type of the parent they were
     * split from.
     */
    private boolean copyContentFormatter = true;

    /**
     * 文档id自定义生成器
     */
    private final BiFunction<String, Integer, String> generateDocId = this::generateDocSplitTextId;

    @Override
    public List<Document> apply(List<Document> documents) {
        return doSplitDocuments(documents);
    }

    public List<Document> split(List<Document> documents) {
        return this.apply(documents);
    }

    public List<Document> split(Document document) {
        return this.apply(List.of(document));
    }

    public boolean isCopyContentFormatter() {
        return this.copyContentFormatter;
    }

    public void setCopyContentFormatter(boolean copyContentFormatter) {
        this.copyContentFormatter = copyContentFormatter;
    }

    private List<Document> doSplitDocuments(List<Document> documents) {
        List<String> texts = new ArrayList<>();
        List<Map<String, Object>> metadataList = new ArrayList<>();
        List<ContentFormatter> formatters = new ArrayList<>();

        for (Document doc : documents) {
            texts.add(doc.getText());
            metadataList.add(doc.getMetadata());
            formatters.add(doc.getContentFormatter());
        }

        return createDocuments(documents, texts, formatters, metadataList);
    }

    private List<Document> createDocuments(List<Document> originDocuments, List<String> texts,
                                           List<ContentFormatter> formatters,
                                           List<Map<String, Object>> metadataList) {

        // Process the docDetailInfo in a column oriented way and recreate the Document
        List<Document> documents = new ArrayList<>();

        for (int i = 0; i < texts.size(); i++) {
            String text = texts.get(i);
            Document originDocument = originDocuments.get(i);
            Map<String, Object> metadata = metadataList.get(i);
            List<String> chunks = splitText(text);
            if (chunks.size() > 1) {
                logger.info("Splitting up document into " + chunks.size() + " chunks.");
            }
            for (int j = 0; j < chunks.size(); j++) {
                // only primitive values are in here -
                Map<String, Object> metadataCopy = metadata.entrySet()
                        .stream()
                        .filter(e -> e.getKey() != null && e.getValue() != null)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                String docId = generateDocId.apply(originDocument.getId(), j);
                Document newDoc = new Document(docId,chunks.get(j), metadataCopy);

                if (this.copyContentFormatter) {
                    // Transfer the content-formatter of the parent to the chunked
                    // documents it was slit into.
                    newDoc.setContentFormatter(formatters.get(i));
                }

                // TODO copy over other properties.
                documents.add(newDoc);
            }
        }
        return documents;
    }

    protected abstract List<String> splitText(String text);

    protected String generateDocSplitTextId(String id, int index) {
        return id + "_" + index;
    }

}
