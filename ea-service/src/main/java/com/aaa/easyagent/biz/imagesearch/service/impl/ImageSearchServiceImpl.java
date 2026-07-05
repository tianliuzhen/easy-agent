package com.aaa.easyagent.biz.imagesearch.service.impl;

import com.aaa.easyagent.biz.imagesearch.ImageMilvusDao;
import com.aaa.easyagent.biz.imagesearch.service.ImageSearchService;
import com.aaa.easyagent.common.embedding.DashScopeMultimodalClient;
import com.aaa.easyagent.common.storage.ImageStorage;
import com.aaa.easyagent.core.domain.result.ImageSearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * 图片搜索服务实现：编排 多模态向量化 + Milvus + 存储。
 *
 * @author liuzhen.tian
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageSearchServiceImpl implements ImageSearchService {

    private static final int LIST_LIMIT = 500;

    private final DashScopeMultimodalClient multimodalClient;
    private final ImageMilvusDao imageMilvusDao;
    private final ImageStorage imageStorage;

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
            imageMilvusDao.insert(id, displayName, ext, vector);

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
            List<ImageSearchResult> results = imageMilvusDao.search(vector, topK <= 0 ? 10 : topK);
            results.forEach(r -> r.setImageUrl(imageStorage.publicUrl(r.getId())));
            return results;
        } catch (Exception e) {
            log.error("图片搜索失败", e);
            throw new RuntimeException("图片搜索失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ImageSearchResult> list() {
        List<ImageSearchResult> results = imageMilvusDao.listAll(LIST_LIMIT);
        results.forEach(r -> r.setImageUrl(imageStorage.publicUrl(r.getId())));
        return results;
    }

    @Override
    public void delete(String id) {
        imageMilvusDao.deleteById(id);
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
