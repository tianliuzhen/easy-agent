package com.aaa.easyagent.web.biz.imagesearch;

import com.aaa.easyagent.biz.imagesearch.service.ImageSearchService;
import com.aaa.easyagent.core.domain.base.BaseResult;
import com.aaa.easyagent.core.domain.result.ImageSearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 图片搜索（以图搜图）接口。
 *
 * @author liuzhen.tian
 */
@Slf4j
@RestController
@RequestMapping("imageSearch/")
@RequiredArgsConstructor
public class ImageSearchController {

    private final ImageSearchService imageSearchService;

    /** 上传图片入库 */
    @PostMapping("upload")
    public BaseResult<ImageSearchResult> upload(@RequestPart("file") MultipartFile file,
                                                @RequestParam(value = "name", required = false) String name) {
        try {
            return BaseResult.buildSuc(imageSearchService.upload(file, name));
        } catch (Exception e) {
            log.error("上传图片失败", e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /** 以图搜图 */
    @PostMapping("search")
    public BaseResult<List<ImageSearchResult>> search(@RequestPart("file") MultipartFile file,
                                                      @RequestParam(value = "topK", defaultValue = "10") int topK) {
        try {
            return BaseResult.buildSuc(imageSearchService.searchByImage(file, topK));
        } catch (Exception e) {
            log.error("图片搜索失败", e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /** 已入库图片列表 */
    @GetMapping("list")
    public BaseResult<List<ImageSearchResult>> list() {
        try {
            return BaseResult.buildSuc(imageSearchService.list());
        } catch (Exception e) {
            log.error("图片列表查询失败", e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /** 删除 */
    @DeleteMapping("{id}")
    public BaseResult<Void> delete(@PathVariable("id") String id) {
        try {
            imageSearchService.delete(id);
            return BaseResult.buildSuc();
        } catch (Exception e) {
            log.error("删除图片失败: {}", id, e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /** 图片字节（供 &lt;img&gt; 展示，permitAll） */
    @GetMapping("file/{id}")
    public ResponseEntity<byte[]> file(@PathVariable("id") String id) {
        byte[] bytes = imageSearchService.loadImageBytes(id);
        return ResponseEntity.ok()
                .contentType(guessMediaType(bytes))
                .body(bytes);
    }

    private MediaType guessMediaType(byte[] bytes) {
        if (bytes != null && bytes.length >= 4) {
            if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
                return MediaType.IMAGE_PNG;
            }
            if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
                return MediaType.IMAGE_JPEG;
            }
            if (bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') {
                return MediaType.IMAGE_GIF;
            }
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
