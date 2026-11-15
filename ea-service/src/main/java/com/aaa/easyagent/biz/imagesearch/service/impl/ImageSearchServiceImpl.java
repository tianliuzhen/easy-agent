package com.aaa.easyagent.biz.imagesearch.service.impl;

import com.aaa.easyagent.biz.imagesearch.ImageVectorDao;
import com.aaa.easyagent.biz.imagesearch.service.ImageSearchService;
import com.aaa.easyagent.common.embedding.DashScopeMultimodalClient;
import com.aaa.easyagent.common.storage.ImageStorage;
import com.aaa.easyagent.core.domain.result.ImageSearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 图片搜索服务实现：编排多模态向量化、按配置选择的向量库和图片存储。
 *
 * @author liuzhen.tian
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageSearchServiceImpl implements ImageSearchService {

    private static final int PAGE_SIZE = 24;

    private final DashScopeMultimodalClient multimodalClient;
    private final ImageVectorDao imageVectorDao;
    private final ImageStorage imageStorage;

    @Override
    public ImageSearchResult uploadByUrl(String imageUrl, String name) {
        String id = UUID.randomUUID().toString().replace("-", "");
        String displayName = StringUtils.hasText(name) ? name : id;
        String ext = guessExtFromUrl(imageUrl);

        float[] vector = multimodalClient.embedImageByUrl(imageUrl);
        imageVectorDao.insert(id, displayName, ext, imageUrl, vector);

        ImageSearchResult result = new ImageSearchResult();
        result.setId(id);
        result.setName(displayName);
        result.setExt(ext);
        result.setImageUrl(imageUrl);
        result.setSourceUrl(imageUrl);
        log.info("图片入库成功(URL): id={}, name={}, url={}", id, displayName, imageUrl);
        return result;
    }

    private static String guessExtFromUrl(String url) {
        String lower = url.toLowerCase();
        if (lower.contains(".webp")) return "webp";
        if (lower.contains(".png")) return "png";
        if (lower.contains(".gif")) return "gif";
        if (lower.contains(".jpg") || lower.contains(".jpeg")) return "jpg";
        return "jpg";
    }

    @Override
    public ImageSearchResult upload(MultipartFile file, String name) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件为空");
        }
        try {
            String id = UUID.randomUUID().toString().replace("-", "");
            String ext = resolveExt(file);
            String displayName = StringUtils.hasText(name) ? name : file.getOriginalFilename();
            byte[] bytes = file.getBytes();

            imageStorage.save(id, bytes, ext);
            float[] vector = multimodalClient.embedImage(bytes, file.getContentType());
            imageVectorDao.insert(id, displayName, ext, null, vector);

            ImageSearchResult result = new ImageSearchResult();
            result.setId(id);
            result.setName(displayName);
            result.setExt(ext);
            result.setImageUrl(imageStorage.publicUrl(id));
            log.info("图片入库成功: id={}, name={}", id, displayName);
            return result;
        } catch (Exception e) {
            log.error("图片入库失败: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("图片入库失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ImageSearchResult> searchByImage(MultipartFile file, int topK) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("查询图片为空");
        }
        try {
            float[] vector = multimodalClient.embedImage(file.getBytes(), file.getContentType());
            List<ImageSearchResult> results = imageVectorDao.search(vector, topK <= 0 ? 10 : topK);
            results.forEach(r -> r.setImageUrl(
                    StringUtils.hasText(r.getSourceUrl()) ? r.getSourceUrl() : imageStorage.publicUrl(r.getId())));
            return results;
        } catch (Exception e) {
            log.error("图片搜索失败", e);
            throw new RuntimeException("图片搜索失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> list(int offset, int limit) {
        int size = limit > 0 ? Math.min(limit, 200) : PAGE_SIZE;
        long total = imageVectorDao.countAll();
        List<ImageSearchResult> results = imageVectorDao.listAll(offset, size);
        results.forEach(r -> r.setImageUrl(
                StringUtils.hasText(r.getSourceUrl()) ? r.getSourceUrl() : imageStorage.publicUrl(r.getId())));
        Map<String, Object> map = new HashMap<>();
        map.put("data", results);
        map.put("total", total);
        map.put("offset", offset);
        map.put("limit", size);
        return map;
    }

    @Override
    public void delete(String id) {
        imageVectorDao.deleteById(id);
        imageStorage.delete(id);
    }

    @Override
    public byte[] loadImageBytes(String id) {
        return imageStorage.load(id);
    }

    private String resolveExt(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (StringUtils.hasText(fileName) && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        }
        String ct = file.getContentType();
        if (ct != null && ct.contains("/")) {
            return ct.substring(ct.lastIndexOf('/') + 1).toLowerCase();
        }
        return "jpg";
    }
}
