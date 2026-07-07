package com.aaa.easyagent.biz.imagesearch.service;

import com.aaa.easyagent.core.domain.result.ImageSearchResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 图片搜索（以图搜图）服务。
 *
 * @author liuzhen.tian
 */
public interface ImageSearchService {

    /** 上传图片并入库（本地文件 base64 编码） */
    ImageSearchResult upload(MultipartFile file, String name);

    /** 通过 URL 上传（百炼直接拉取，不存本地文件） */
    ImageSearchResult uploadByUrl(String imageUrl, String name);

    /** 以图搜图 */
    List<ImageSearchResult> searchByImage(MultipartFile file, int topK);

    /** 分页列出已入库图片 */
    Map<String, Object> list(int offset, int limit);

    /** 删除一条 */
    void delete(String id);

    /** 读取图片字节（供 local 存储的 file 端点用） */
    byte[] loadImageBytes(String id);
}
