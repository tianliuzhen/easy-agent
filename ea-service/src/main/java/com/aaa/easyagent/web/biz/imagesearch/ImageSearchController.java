package com.aaa.easyagent.web.biz.imagesearch;

import com.aaa.easyagent.biz.imagesearch.service.ImageSearchService;
import com.aaa.easyagent.core.domain.base.BaseResult;
import com.aaa.easyagent.core.domain.result.ImageSearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

    @Value("${PRODUCTS_DB_URL:}")
    private String productsDbUrl;
    @Value("${PRODUCTS_DB_USER:}")
    private String productsDbUser;
    @Value("${PRODUCTS_DB_PASSWORD:}")
    private String productsDbPassword;

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

    /** 已入库图片列表（分页） */
    @GetMapping("list")
    public BaseResult<Map<String, Object>> list(@RequestParam(defaultValue = "0") int offset,
                                                 @RequestParam(defaultValue = "24") int limit) {
        try {
            return BaseResult.buildSuc(imageSearchService.list(offset, limit));
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

    /** 从远程 products 表批量导入主图到图像搜索库（异步执行） */
    @PostMapping("import/products")
    public BaseResult<Map<String, Object>> importProducts(
            @RequestParam(defaultValue = "1237") int startId,
            @RequestParam(defaultValue = "20000") int limit) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "started");

        CompletableFuture.runAsync(() -> {
            try {
                doImportProducts(startId, limit);
            } catch (Exception e) {
                log.error("商品图导入异常", e);
            }
        });

        return BaseResult.buildSuc(result);
    }

    private void doImportProducts(int startId, int maxLimit) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");

        // 1. 一次性加载所有商品到内存
        List<ProductRecord> allProducts = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(productsDbUrl, productsDbUser, productsDbPassword)) {
            String sql = "SELECT id, product_code, title, main_image_url FROM products"
                    + " WHERE main_image_url IS NOT NULL AND main_image_url != '' AND is_deleted = 0 AND id > ?"
                    + " ORDER BY id LIMIT ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, startId);
                ps.setInt(2, maxLimit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        allProducts.add(new ProductRecord(
                                rs.getLong("id"),
                                rs.getString("product_code"),
                                rs.getString("title"),
                                rs.getString("main_image_url")));
                    }
                }
            }
        }
        log.info("加载商品完成: {} 条 (startId>{})", allProducts.size(), startId);
        if (allProducts.isEmpty()) {
            log.info("无数据，导入结束");
            return;
        }

        // 2. 按 1000 条分批，每批一个线程
        int batchSize = 1000;
        int threadCount = (allProducts.size() + batchSize - 1) / batchSize;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger totalSuccess = new AtomicInteger(0);
        AtomicInteger totalFail = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int batchIdx = i;
            final List<ProductRecord> batch = allProducts.subList(
                    i * batchSize, Math.min((i + 1) * batchSize, allProducts.size()));

            executor.submit(() -> {
                int ok = 0, fail = 0;
                for (ProductRecord p : batch) {
                    try {
                        String name = (p.title != null && !p.title.isBlank()) ? p.title : p.productCode;
                        imageSearchService.uploadByUrl(p.imageUrl, name);
                        ok++;
                        log.info("[OK] batch={} product_id={} name={}", batchIdx, p.id, name);
                    } catch (Exception e) {
                        fail++;
                        log.error("[FAIL] batch={} product_id={} url={}: {}", batchIdx, p.id, p.imageUrl, e.getMessage());
                    }
                }
                totalSuccess.addAndGet(ok);
                totalFail.addAndGet(fail);
                log.info("batch={} 完成: ok={} fail={}", batchIdx, ok, fail);
            });
        }

        executor.shutdown();
        executor.awaitTermination(12, TimeUnit.HOURS);
        log.info("===== 导入完成 total={} success={} fail={}", allProducts.size(), totalSuccess.get(), totalFail.get());
    }

    private record ProductRecord(long id, String productCode, String title, String imageUrl) {}

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
