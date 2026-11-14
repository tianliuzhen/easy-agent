package com.aaa.easyagent.web.biz.imagedetect;

import com.aaa.easyagent.biz.imagedetect.domain.ImageDetectResult;
import com.aaa.easyagent.biz.imagedetect.service.ImageDetectService;
import com.aaa.easyagent.common.config.imagedetect.ImageDetectProperties;
import com.aaa.easyagent.core.domain.base.BaseResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 服装主体分割接口，返回可用于裁图后图搜的归一化坐标；沿用项目 JWT 鉴权。
 */
@Slf4j
@RestController
@RequestMapping("/imageDetect")
@RequiredArgsConstructor
public class ImageDetectController {

    private final ImageDetectService imageDetectService;
    private final ImageDetectProperties properties;

    /** 返回模型实际可用状态，配置关闭或加载失败时为 false。 */
    @GetMapping("/status")
    public BaseResult<Boolean> status() {
        return BaseResult.buildSuc(imageDetectService.isEnabled());
    }

    /** 上传图片并识别区域，可选参数覆盖本次调用的人体部位与最小面积过滤配置。 */
    @PostMapping(value = "/regions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResult<ImageDetectResult> detect(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "includeBodyParts", required = false) Boolean includeBodyParts,
            @RequestParam(name = "minAreaRatio", required = false) Float minAreaRatio) throws IOException {
        if (file == null || file.isEmpty()) {
            return BaseResult.buildFail("请上传图片");
        }
        if (file.getSize() > properties.getMaxImageBytes()) {
            return BaseResult.buildFail("图片超过 " + properties.getMaxImageBytes() + " 字节限制");
        }
        return detectBytes(file.getBytes(), includeBodyParts, minAreaRatio);
    }

    /** 接收原始图片字节，避免调用方为已有字节流额外构造 Multipart 表单。 */
    @PostMapping(value = "/regions/bytes", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public BaseResult<ImageDetectResult> detectBytes(
            @RequestBody byte[] imageBytes,
            @RequestParam(name = "includeBodyParts", required = false) Boolean includeBodyParts,
            @RequestParam(name = "minAreaRatio", required = false) Float minAreaRatio) {
        try {
            return BaseResult.buildSuc(imageDetectService.detect(imageBytes, includeBodyParts, minAreaRatio));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            log.warn("[ImageDetect] 识别失败: {}", exception.getMessage());
            return BaseResult.buildFail(exception.getMessage());
        }
    }
}
