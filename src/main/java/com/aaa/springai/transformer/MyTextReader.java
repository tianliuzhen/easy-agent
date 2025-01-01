package com.aaa.springai.transformer;

import com.aaa.springai.util.TextUniqueValueCalculator;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.TextReader;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 文本Reader支持计算文档唯一id {@link TextReader}
 *
 * @author tlz
 * @version $Id: TextTextReader.java,v 0.1 2025年01月01日  5:00 PM:34 Exp $
 */
public class MyTextReader implements DocumentReader {

    public static final String CHARSET_METADATA = "charset";

    public static final String SOURCE_METADATA = "source";

    /**
     * Input resource to load the text from.
     */
    private final Resource resource;

    private final Map<String, Object> customMetadata = new HashMap<>();

    /**
     * Character set to be used when loading data from the
     */
    private Charset charset = StandardCharsets.UTF_8;

    public MyTextReader(String resourceUrl) {
        this(new DefaultResourceLoader().getResource(resourceUrl));
    }

    public MyTextReader(Resource resource) {
        Objects.requireNonNull(resource, "The Spring Resource must not be null");
        this.resource = resource;
    }

    public Charset getCharset() {
        return this.charset;
    }

    public void setCharset(Charset charset) {
        Objects.requireNonNull(charset, "The charset must not be null");
        this.charset = charset;
    }

    /**
     * Metadata associated with all documents created by the loader.
     *
     * @return Metadata to be assigned to the output Documents.
     */
    public Map<String, Object> getCustomMetadata() {
        return this.customMetadata;
    }

    @Override
    public List<Document> get() {
        try {

            String document = StreamUtils.copyToString(this.resource.getInputStream(), this.charset);

            // Inject source information as a metadata.
            this.customMetadata.put(CHARSET_METADATA, this.charset.name());
            this.customMetadata.put(SOURCE_METADATA, this.resource.getFilename());
            this.customMetadata.put(SOURCE_METADATA, getResourceIdentifier(this.resource));

            return List.of(new Document(TextUniqueValueCalculator.calculateUniqueShortValue(document), document, this.customMetadata));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected String getResourceIdentifier(Resource resource) {
        // Try to get the filename first
        String filename = resource.getFilename();
        if (filename != null && !filename.isEmpty()) {
            return filename;
        }

        // Try to get the URI
        try {
            URI uri = resource.getURI();
            if (uri != null) {
                return uri.toString();
            }
        } catch (IOException ignored) {
            // If getURI() throws an exception, we'll try the next method
        }

        // Try to get the URL
        try {
            URL url = resource.getURL();
            if (url != null) {
                return url.toString();
            }
        } catch (IOException ignored) {
            // If getURL() throws an exception, we'll fall back to getDescription()
        }

        // If all else fails, use the description
        return resource.getDescription();
    }

}
