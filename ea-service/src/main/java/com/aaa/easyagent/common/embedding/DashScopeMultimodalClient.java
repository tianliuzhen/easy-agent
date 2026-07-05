package com.aaa.easyagent.common.embedding;

import com.aaa.easyagent.common.config.imagesearch.ImageSearchProperties;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

/**
 * 阿里云百炼 DashScope 原生多模态 embedding 客户端（qwen3-vl-embedding）。
 * 注意：百炼多模态 embedding 不支持 OpenAI 兼容接口，必须走原生 multimodal-embedding 端点。
 *
 * @author liuzhen.tian
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DashScopeMultimodalClient {

    private final ImageSearchProperties properties;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    /** 图片向量化 */
    public float[] embedImage(byte[] bytes, String contentType) {
        String mime = (contentType == null || contentType.isBlank()) ? "image/jpeg" : contentType;
        String dataUri = "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
        JSONObject content = new JSONObject();
        content.put("image", dataUri);
        return embed(content);
    }

    /** 文本向量化（预留：文字搜图） */
    public float[] embedText(String text) {
        JSONObject content = new JSONObject();
        content.put("text", text);
        return embed(content);
    }

    private float[] embed(JSONObject contentItem) {
        JSONArray contents = new JSONArray();
        contents.add(contentItem);
        JSONObject input = new JSONObject();
        input.put("contents", contents);
        JSONObject parameters = new JSONObject();
        parameters.put("dimension", properties.getDimension());

        JSONObject body = new JSONObject();
        body.put("model", properties.getModel());
        body.put("input", input);
        body.put("parameters", parameters);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getEndpoint()))
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toJSONString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("百炼多模态 embedding 请求失败 " + response.statusCode() + ": " + response.body());
            }

            // 响应：{"output":{"embeddings":[{"index":0,"embedding":[...],"type":"image"}]}, ...}
            JSONObject json = JSON.parseObject(response.body());
            JSONArray embeddings = json.getJSONObject("output").getJSONArray("embeddings");
            JSONArray embedding = embeddings.getJSONObject(0).getJSONArray("embedding");
            float[] vec = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                vec[i] = embedding.getFloatValue(i);
            }
            return vec;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("调用百炼多模态 embedding 异常", e);
        }
    }
}
