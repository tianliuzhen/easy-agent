package com.aaa.easyagent.biz.imagesearch;

import co.elastic.clients.transport.rest5_client.low_level.Request;
import co.elastic.clients.transport.rest5_client.low_level.Response;
import co.elastic.clients.transport.rest5_client.low_level.ResponseException;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import com.aaa.easyagent.common.config.exception.AgentException;
import com.aaa.easyagent.common.config.imagesearch.ImageSearchProperties;
import com.aaa.easyagent.core.domain.result.ImageSearchResult;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ES 图片向量读写：复用项目 Rest5Client，使用独立图片索引与多模态向量维度。
 * 首次业务调用才检查或创建索引，构造 Bean 时不访问 ES。
 */
@Slf4j
@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "easy-agent.vectorstore.provider", havingValue = "es", matchIfMissing = true)
public class ImageElasticsearchDao implements ImageVectorDao {

    private static final String FIELD_ID = "id";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_EXT = "file_ext";
    private static final String FIELD_SOURCE_URL = "source_url";
    private static final String FIELD_EMBEDDING = "embedding";
    private static final List<String> METADATA_FIELDS = List.of(FIELD_ID, FIELD_NAME, FIELD_EXT, FIELD_SOURCE_URL);
    /** ES 默认分页窗口与 kNN 候选数上限，避免把全部向量加载到业务内存。 */
    private static final int MAX_RESULT_WINDOW = 10_000;

    private final ObjectProvider<Rest5Client> restClientProvider;
    private final ImageSearchProperties properties;

    private volatile boolean indexReady;

    /** 使用确定的文档标识写入，等待刷新后再返回以保证随后的查询和计数可见。 */
    @Override
    public void insert(String id, String name, String ext, String sourceUrl, float[] vector) {
        validateVector(vector);
        ensureIndex();
        JSONObject document = new JSONObject();
        document.put(FIELD_ID, id);
        document.put(FIELD_NAME, name);
        document.put(FIELD_EXT, ext);
        document.put(FIELD_SOURCE_URL, sourceUrl);
        document.put(FIELD_EMBEDDING, vector);

        Request request = new Request("PUT", indexPath() + "/_doc/" + documentId(id));
        request.addParameter("refresh", "wait_for");
        request.setJsonEntity(document.toJSONString());
        execute(request);
    }

    /** kNN 检索只取元数据，ES 的归一化分数会转换回 Milvus 使用的余弦相似度。 */
    @Override
    public List<ImageSearchResult> search(float[] vector, int topK) {
        validateVector(vector);
        if (topK <= 0 || topK > MAX_RESULT_WINDOW) {
            throw new IllegalArgumentException("图片搜索 topK 必须在 1~10000 之间");
        }
        ensureIndex();
        JSONObject body = new JSONObject();
        body.put("size", topK);
        body.put("_source", METADATA_FIELDS);
        body.put("knn", Map.of("field", FIELD_EMBEDDING, "query_vector", vector,
                "k", topK, "num_candidates", Math.min(MAX_RESULT_WINDOW, Math.max(100, topK * 10))));
        return searchDocuments(body, true);
    }

    /** 按唯一图片标识稳定排序分页，不返回 embedding，避免大向量占用列表带宽。 */
    @Override
    public List<ImageSearchResult> listAll(int offset, int limit) {
        if (offset < 0 || limit <= 0 || (long) offset + limit > MAX_RESULT_WINDOW) {
            throw new IllegalArgumentException("ES 图片列表 offset 必须非负，limit 必须为正，分页范围不能超过 10000 条");
        }
        ensureIndex();
        JSONObject body = new JSONObject();
        body.put("from", offset);
        body.put("size", limit);
        body.put("_source", METADATA_FIELDS);
        body.put("query", Map.of("match_all", Map.of()));
        body.put("sort", List.of(Map.of(FIELD_ID, "asc")));
        return searchDocuments(body, false);
    }

    /** 使用 ES 精确计数接口，避免搜索响应默认总数上限造成统计失真。 */
    @Override
    public long countAll() {
        ensureIndex();
        return execute(new Request("GET", indexPath() + "/_count")).getLongValue("count");
    }

    /** 忽略文档不存在的 404，重复删除保持幂等；其他错误继续向上传递。 */
    @Override
    public void deleteById(String id) {
        ensureIndex();
        Request request = new Request("DELETE", indexPath() + "/_doc/" + documentId(id));
        request.addParameter("refresh", "wait_for");
        execute(request, true);
    }

    /** 幂等检查索引，创建失败不缓存成功状态，恢复服务后可重新尝试。 */
    private synchronized void ensureIndex() {
        if (indexReady) {
            return;
        }
        // 1. 校验独立图片索引名称与维度，避免使用 RAG 的 2048 维配置。
        String indexPath = indexPath();
        if (properties.getDimension() <= 0) {
            throw new IllegalArgumentException("图片向量维度必须大于 0");
        }

        // 2. 检查索引是否存在，首次创建时允许其他进程同时完成相同创建。
        Request existsRequest = new Request("HEAD", indexPath);
        if (perform(existsRequest, true).getStatusCode() == 404) {
            createIndex();
        }

        // 3. 校验现有映射，错误时明确失败，不自动删索引或重建已有数据。
        validateMapping();
        indexReady = true;
        log.info("ES 图片索引已就绪: {}, dimension={}", properties.getIndexName(), properties.getDimension());
    }

    /** 建立 keyword 元数据与 cosine dense_vector，图片索引不复用知识库索引。 */
    private void createIndex() {
        Map<String, Object> fields = Map.of(
                FIELD_ID, Map.of("type", "keyword"),
                FIELD_NAME, Map.of("type", "keyword", "index", false),
                FIELD_EXT, Map.of("type", "keyword", "index", false),
                FIELD_SOURCE_URL, Map.of("type", "keyword", "index", false),
                FIELD_EMBEDDING, Map.of("type", "dense_vector", "dims", properties.getDimension(),
                        "index", true, "similarity", "cosine"));
        Request request = new Request("PUT", indexPath());
        request.setJsonEntity(JSON.toJSONString(Map.of("mappings", Map.of("properties", fields))));
        try {
            execute(request);
        } catch (AgentException exception) {
            if (!(exception.getCause() instanceof ResponseException responseException)
                    || responseException.getResponse().getStatusCode() != 400
                    || !responseException.getMessage().contains("resource_already_exists_exception")) {
                throw exception;
            }
        }
    }

    /** 拒绝混用不同模型维度、距离度量或字段类型的现有索引。 */
    private void validateMapping() {
        JSONObject response = execute(new Request("GET", indexPath() + "/_mapping"));
        JSONObject indexMapping = response.getJSONObject(properties.getIndexName());
        JSONObject mappings = indexMapping == null ? null : indexMapping.getJSONObject("mappings");
        JSONObject fields = mappings == null ? null : mappings.getJSONObject("properties");
        JSONObject vectorMapping = fields == null ? null : fields.getJSONObject(FIELD_EMBEDDING);
        JSONObject idMapping = fields == null ? null : fields.getJSONObject(FIELD_ID);
        if (vectorMapping == null || !"dense_vector".equals(vectorMapping.getString("type"))
                || vectorMapping.getIntValue("dims") != properties.getDimension()
                || !"cosine".equals(vectorMapping.getString("similarity"))
                || Boolean.FALSE.equals(vectorMapping.getBoolean("index"))
                || idMapping == null || !"keyword".equals(idMapping.getString("type"))) {
            throw new AgentException("ES 图片索引映射不兼容: " + properties.getIndexName()
                    + "，需要 keyword 类型的 id 和 " + properties.getDimension()
                    + " 维 cosine dense_vector；请使用独立的新图片索引并重新导入");
        }
    }

    /** 执行搜索并转换为现有接口的结果结构，列表场景保留空分数。 */
    private List<ImageSearchResult> searchDocuments(JSONObject body, boolean includeScore) {
        Request request = new Request("POST", indexPath() + "/_search");
        request.setJsonEntity(body.toJSONString());
        JSONArray hits = execute(request).getJSONObject("hits").getJSONArray("hits");
        List<ImageSearchResult> results = new ArrayList<>(hits.size());
        for (int i = 0; i < hits.size(); i++) {
            JSONObject hit = hits.getJSONObject(i);
            JSONObject source = hit.getJSONObject("_source");
            ImageSearchResult result = new ImageSearchResult();
            result.setId(source.getString(FIELD_ID));
            result.setName(source.getString(FIELD_NAME));
            result.setExt(source.getString(FIELD_EXT));
            result.setSourceUrl(source.getString(FIELD_SOURCE_URL));
            if (includeScore) {
                result.setScore(Math.max(-1f, Math.min(1f, 2 * hit.getFloatValue("_score") - 1)));
            }
            results.add(result);
        }
        return results;
    }

    /** 在发送请求前拒绝长度错误、非有限数值或 cosine 不支持的全零向量。 */
    private void validateVector(float[] vector) {
        if (vector == null || vector.length != properties.getDimension()) {
            throw new IllegalArgumentException("图片向量维度必须为 " + properties.getDimension());
        }
        double squaredNorm = 0;
        for (float coordinate : vector) {
            if (!Float.isFinite(coordinate)) {
                throw new IllegalArgumentException("图片向量不能包含 NaN 或无穷值");
            }
            squaredNorm += (double) coordinate * coordinate;
        }
        if (squaredNorm == 0) {
            throw new IllegalArgumentException("cosine 图片向量不能为全零向量");
        }
    }

    /** 限定为单个具体索引，禁止路径、通配符或批量索引表达式。 */
    private String indexPath() {
        String indexName = properties.getIndexName();
        if (!StringUtils.hasText(indexName) || !indexName.matches("[a-z0-9][a-z0-9._-]*")) {
            throw new IllegalArgumentException("图片 ES 索引名必须以小写字母或数字开头，且仅包含小写字母、数字、点、下划线和短横线");
        }
        return "/" + indexName;
    }

    /** 图片标识沿用 UUID 格式，避免用户路径片段进入 ES 请求地址。 */
    private String documentId(String id) {
        if (!StringUtils.hasText(id) || !id.matches("[a-zA-Z0-9_-]+")) {
            throw new IllegalArgumentException("图片标识只能包含字母、数字、下划线和短横线");
        }
        return id;
    }

    /** 请求失败保留 SDK 根因，不能把存储失败伪装成空结果。 */
    private Response perform(Request request, boolean allowNotFound) {
        try {
            Response response = restClientProvider.getObject().performRequest(request);
            // Rest5Client 会直接返回非成功状态，不能依赖 SDK 自动抛错。
            if (response.getStatusCode() >= 300 && !(allowNotFound && response.getStatusCode() == 404)) {
                throw new ResponseException(response);
            }
            return response;
        } catch (ResponseException exception) {
            if (allowNotFound && exception.getResponse().getStatusCode() == 404) {
                return exception.getResponse();
            }
            throw new AgentException("图片 ES 请求失败: " + request.getMethod() + " " + request.getEndpoint(), exception);
        } catch (IOException exception) {
            throw new AgentException("图片 ES 请求失败: " + request.getMethod() + " " + request.getEndpoint(), exception);
        }
    }

    /** 读取并关闭 HTTP 响应体，JSON 解析失败同样按真实失败向上传递。 */
    private JSONObject execute(Request request) {
        return execute(request, false);
    }

    /** 仅索引存在性检查和文档删除允许 404，其他状态统一按失败处理。 */
    private JSONObject execute(Request request, boolean allowNotFound) {
        Response response = perform(request, allowNotFound);
        if (response.getEntity() == null) {
            return new JSONObject();
        }
        try (InputStream responseStream = response.getEntity().getContent()) {
            return JSON.parseObject(new String(responseStream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException | JSONException exception) {
            throw new AgentException("读取图片 ES 响应失败: " + request.getEndpoint(), exception);
        }
    }
}
