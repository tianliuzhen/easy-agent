package com.aaa.easyagent.biz.imagesearch.service;

import com.aaa.easyagent.core.domain.result.ImageSearchResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 图片搜索（以图搜图）服务。
 *
 * @author liuzhen.tian
 */
public interface ImageSearchService {

    /** 上传图片并入库（向量化 + 存储） */
    ImageSearchResult upload(MultipartFile file, String name);

    /** 以图搜图 */
    List<ImageSearchResult> searchByImage(MultipartFile file, int topK);

    /** 列出已入库图片 */
    List<ImageSearchResult> list();

    /** 删除一条 */
    void delete(String id);

    /** 读取图片字节（供 local 存储的 file 端点用） */
    byte[] loadImageBytes(String id);
}
