package com.aaa.easyagent.common.storage;

import com.aaa.easyagent.common.config.imagesearch.ImageSearchProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地磁盘图片存储：文件存为 {uploadDir}/{id}.{ext}。
 *
 * @author liuzhen.tian
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "easy-agent.image-search.storage", havingValue = "local", matchIfMissing = true)
public class LocalImageStorage implements ImageStorage {

    private final ImageSearchProperties properties;

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Paths.get(properties.getUploadDir()));
        log.info("图片本地存储目录: {}", Paths.get(properties.getUploadDir()).toAbsolutePath());
    }

    @Override
    public void save(String id, byte[] bytes, String ext) {
        try {
            Path target = Paths.get(properties.getUploadDir(), id + "." + ext);
            Files.write(target, bytes);
        } catch (IOException e) {
            throw new RuntimeException("保存图片失败: " + id, e);
        }
    }

    @Override
    public byte[] load(String id) {
        try {
            Path path = resolve(id);
            if (path == null) {
                throw new RuntimeException("图片不存在: " + id);
            }
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException("读取图片失败: " + id, e);
        }
    }

    @Override
    public void delete(String id) {
        try {
            Path path = resolve(id);
            if (path != null) {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            log.warn("删除图片失败: {}", id, e);
        }
    }

    @Override
    public String publicUrl(String id) {
        return "/imageSearch/file/" + id;
    }

    /** 按 id 前缀查找 {id}.* 文件 */
    private Path resolve(String id) throws IOException {
        Path dir = Paths.get(properties.getUploadDir());
        if (!Files.exists(dir)) {
            return null;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, id + ".*")) {
            for (Path p : stream) {
                return p;
            }
        }
        return null;
    }
}
