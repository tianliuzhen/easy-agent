package com.aaa.easyagent.image;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 图搜图效果验证 spike（独立，不依赖 Spring 上下文、不碰 Milvus）。
 *
 * 调用阿里云百炼 DashScope 原生多模态 embedding 接口（qwen3-vl-embedding）。
 * 注意：百炼多模态 embedding 不支持 OpenAI 兼容接口，必须用原生 multimodal-embedding 端点。
 * 把若干商品图 + 一张查询图向量化，按余弦相似度排序，看相似商品是否排在前面。
 *
 * 运行：
 *   1. 在项目根 .env 配置 DASHSCOPE_API_KEY（复用现有百炼 key）
 *   2. 填入 QUERY_IMAGE 和 CANDIDATE_IMAGES（本地路径或 http URL）
 *   3. mvn test -Dtest=Qwen3VlImageSearchSpikeTest
 */
public class Qwen3VlImageSearchSpikeTest {

    // 复用百炼 key，从项目根 .env 读取，避免密钥写进代码
    private static final String API_KEY = loadEnv("DASHSCOPE_API_KEY");

    private static final String ENDPOINT = "https://dashscope.aliyuncs.com/api/v1/services/embeddings/multimodal-embedding/multimodal-embedding";
    private static final String MODEL = "qwen3-vl-embedding";
    // 支持维度：2560(默认)、2048、1536、1024、768、512、256
    private static final int DIMENSIONS = 1024;

    // 查询图（本地路径或 http URL 都行；本地会自动转 base64）
    private static final String QUERY_IMAGE = "/Users/tlz/Desktop/shoes-1.png";

    // 商品图库
    private static final String[] CANDIDATE_IMAGES = {
            "/Users/tlz/Desktop/shoes-1.png",
            "/Users/tlz/Desktop/shoes-11.png",
            "/Users/tlz/Desktop/shoes-2.png",
            "/Users/tlz/Desktop/shoes-3.png",
    };

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    public void imageToImageSearch() throws Exception {
        System.out.println("查询图向量化: " + QUERY_IMAGE);
        float[] queryVec = embedImage(QUERY_IMAGE);
        System.out.println("向量维度: " + queryVec.length);

        List<Result> results = new ArrayList<>();
        for (String url : CANDIDATE_IMAGES) {
            float[] vec = embedImage(url);
            double sim = cosine(queryVec, vec);
            results.add(new Result(url, sim));
        }

        results.sort((a, b) -> Double.compare(b.score, a.score));

        System.out.println("\n===== 相似度排序（越大越相似）=====");
        for (int i = 0; i < results.size(); i++) {
            Result r = results.get(i);
            System.out.printf("#%d  %.4f  %s%n", i + 1, r.score, r.url);
        }
    }

    /** 图片向量化：本地路径自动转 base64 data URI，http(s) 直接当 URL 传 */
    private float[] embedImage(String imageUrlOrPath) throws Exception {
        String imageValue;
        if (imageUrlOrPath.startsWith("http://") || imageUrlOrPath.startsWith("https://")) {
            imageValue = imageUrlOrPath;
        } else {
            File file = new File(imageUrlOrPath);
            byte[] bytes = Files.readAllBytes(file.toPath());
            String base64 = Base64.getEncoder().encodeToString(bytes);
            String mime = file.getName().toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
            imageValue = "data:" + mime + ";base64," + base64;
        }
        JSONObject input = new JSONObject();
        input.put("image", imageValue);
        return embed(input);
    }

    /** 文本向量化：input = {"text": q}，用于将来"文字搜图"验证 */
    private float[] embedText(String text) throws Exception {
        JSONObject input = new JSONObject();
        input.put("text", text);
        return embed(input);
    }

    private float[] embed(JSONObject contentItem) throws Exception {
        // 百炼原生多模态格式：{"model":..., "input":{"contents":[{...}]}, "parameters":{"dimension":N}}
        JSONArray contents = new JSONArray();
        contents.add(contentItem);
        JSONObject input = new JSONObject();
        input.put("contents", contents);
        JSONObject parameters = new JSONObject();
        parameters.put("dimension", DIMENSIONS);

        JSONObject body = new JSONObject();
        body.put("model", MODEL);
        body.put("input", input);
        body.put("parameters", parameters);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toJSONString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("百炼请求失败 " + response.statusCode() + ": " + response.body());
        }

        // 响应：{"output":{"embeddings":[{"index":0,"embedding":[...],"type":"image"}]}, "usage":..., "request_id":...}
        JSONObject json = JSON.parseObject(response.body());
        JSONArray embeddings = json.getJSONObject("output").getJSONArray("embeddings");
        JSONArray embedding = embeddings.getJSONObject(0).getJSONArray("embedding");
        float[] vec = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            vec[i] = embedding.getFloatValue(i);
        }
        return vec;
    }

    /**
     * 读取密钥：优先系统环境变量，其次向上查找项目根的 .env 文件（key=value）。
     */
    private static String loadEnv(String key) {
        String fromSystem = System.getenv(key);
        if (fromSystem != null && !fromSystem.isBlank()) {
            return fromSystem;
        }
        File dir = new File("").getAbsoluteFile();
        for (int i = 0; i < 5 && dir != null; i++, dir = dir.getParentFile()) {
            File env = new File(dir, ".env");
            if (env.exists()) {
                try {
                    for (String line : Files.readAllLines(env.toPath())) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                            continue;
                        }
                        int idx = line.indexOf('=');
                        if (line.substring(0, idx).trim().equals(key)) {
                            return line.substring(idx + 1).trim();
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException("读取 .env 失败: " + env, e);
                }
            }
        }
        throw new IllegalStateException("未找到密钥 " + key + "，请在项目根 .env 或环境变量中配置");
    }

    private static double cosine(float[] a, float[] b) {
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb) + 1e-10);
    }

    private record Result(String url, double score) {
    }
}
