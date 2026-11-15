package com.aaa.easyagent.biz.imagesearch;

import com.aaa.easyagent.common.config.imagesearch.ImageSearchProperties;
import com.aaa.easyagent.core.domain.result.ImageSearchResult;
import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.grpc.DataType;
import io.milvus.grpc.QueryResults;
import io.milvus.grpc.SearchResults;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.GetCollectionStatisticsParam;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.QueryParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.QueryResultsWrapper;
import io.milvus.response.SearchResultsWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 图片向量的 Milvus 原生读写（独立集合，不走 Spring AI VectorStore）。
 *
 * @author liuzhen.tian
 */
@Slf4j
@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "easy-agent.vectorstore.provider", havingValue = "milvus")
public class ImageMilvusDao implements ImageVectorDao {

    private static final String F_ID = "id";
    private static final String F_NAME = "name";
    private static final String F_EXT = "file_ext";
    private static final String F_EMBEDDING = "embedding";
    private static final String F_SOURCE_URL = "source_url";

    private final ObjectProvider<MilvusServiceClient> milvusClientProvider;
    private final ImageSearchProperties properties;

    private volatile boolean collectionReady = false;

    private MilvusServiceClient client() {
        MilvusServiceClient client = milvusClientProvider.getIfAvailable();
        if (client == null) {
            throw new IllegalStateException("未找到 MilvusServiceClient，请确认 easy-agent.vectorstore.provider=milvus");
        }
        return client;
    }

    /** 幂等确保集合存在并已加载 */
    public synchronized void ensureCollection() {
        if (collectionReady) {
            return;
        }
        MilvusServiceClient client = client();
        String coll = properties.getCollectionName();

        R<Boolean> has = client.hasCollection(HasCollectionParam.newBuilder().withCollectionName(coll).build());
        check(has, "hasCollection");
        if (Boolean.FALSE.equals(has.getData())) {
            FieldType idField = FieldType.newBuilder().withName(F_ID).withDataType(DataType.VarChar)
                    .withPrimaryKey(true).withMaxLength(64).build();
            FieldType nameField = FieldType.newBuilder().withName(F_NAME).withDataType(DataType.VarChar)
                    .withMaxLength(512).build();
            FieldType extField = FieldType.newBuilder().withName(F_EXT).withDataType(DataType.VarChar)
                    .withMaxLength(16).build();
            FieldType sourceUrlField = FieldType.newBuilder().withName(F_SOURCE_URL).withDataType(DataType.VarChar)
                    .withMaxLength(1024).build();
            FieldType vecField = FieldType.newBuilder().withName(F_EMBEDDING).withDataType(DataType.FloatVector)
                    .withDimension(properties.getDimension()).build();

            check(client.createCollection(CreateCollectionParam.newBuilder()
                    .withCollectionName(coll)
                    .addFieldType(idField).addFieldType(nameField).addFieldType(extField).addFieldType(sourceUrlField).addFieldType(vecField)
                    .build()), "createCollection");

            check(client.createIndex(CreateIndexParam.newBuilder()
                    .withCollectionName(coll)
                    .withFieldName(F_EMBEDDING)
                    .withIndexType(IndexType.valueOf(properties.getIndexType()))
                    .withMetricType(MetricType.valueOf(properties.getMetricType()))
                    .withExtraParam("{\"nlist\":1024}")
                    .build()), "createIndex");
            log.info("已创建图片搜索集合: {} (dim={})", coll, properties.getDimension());
        }

        check(client.loadCollection(LoadCollectionParam.newBuilder().withCollectionName(coll).build()), "loadCollection");
        collectionReady = true;
    }

    public void insert(String id, String name, String ext, String sourceUrl, float[] vector) {
        ensureCollection();
        List<Float> vec = new ArrayList<>(vector.length);
        for (float v : vector) {
            vec.add(v);
        }
        List<InsertParam.Field> fields = new ArrayList<>();
        fields.add(new InsertParam.Field(F_ID, Collections.singletonList(id)));
        fields.add(new InsertParam.Field(F_NAME, Collections.singletonList(name)));
        fields.add(new InsertParam.Field(F_EXT, Collections.singletonList(ext)));
        fields.add(new InsertParam.Field(F_SOURCE_URL, Collections.singletonList(sourceUrl != null ? sourceUrl : "")));
        fields.add(new InsertParam.Field(F_EMBEDDING, Collections.singletonList(vec)));

        check(client().insert(InsertParam.newBuilder()
                .withCollectionName(properties.getCollectionName())
                .withFields(fields)
                .build()), "insert");
    }

    public List<ImageSearchResult> search(float[] vector, int topK) {
        ensureCollection();
        List<Float> vec = new ArrayList<>(vector.length);
        for (float v : vector) {
            vec.add(v);
        }

        R<SearchResults> resp = client().search(SearchParam.newBuilder()
                .withCollectionName(properties.getCollectionName())
                .withConsistencyLevel(ConsistencyLevelEnum.STRONG)
                .withMetricType(MetricType.valueOf(properties.getMetricType()))
                .withVectorFieldName(F_EMBEDDING)
                .withTopK(topK)
                .withFloatVectors(Collections.singletonList(vec))
                .withOutFields(List.of(F_NAME, F_EXT, F_SOURCE_URL))
                .withParams("{\"nprobe\":10}")
                .build());
        check(resp, "search");

        SearchResultsWrapper wrapper = new SearchResultsWrapper(resp.getData().getResults());
        List<SearchResultsWrapper.IDScore> scores = wrapper.getIDScore(0);
        List<ImageSearchResult> list = new ArrayList<>();
        for (SearchResultsWrapper.IDScore s : scores) {
            ImageSearchResult r = new ImageSearchResult();
            r.setId(s.getStrID());
            r.setScore(s.getScore());
            r.setName(asString(s.get(F_NAME)));
            r.setExt(asString(s.get(F_EXT)));
            r.setSourceUrl(asString(s.get(F_SOURCE_URL)));
            list.add(r);
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    public List<ImageSearchResult> listAll(int offset, int limit) {
        ensureCollection();
        R<QueryResults> resp = client().query(QueryParam.newBuilder()
                .withCollectionName(properties.getCollectionName())
                .withConsistencyLevel(ConsistencyLevelEnum.STRONG)
                .withExpr(F_ID + " != \"\"")
                .withOutFields(List.of(F_ID, F_NAME, F_EXT, F_SOURCE_URL))
                .withOffset((long) offset)
                .withLimit((long) limit)
                .build());
        check(resp, "query");

        QueryResultsWrapper wrapper = new QueryResultsWrapper(resp.getData());
        List<?> ids = wrapper.getFieldWrapper(F_ID).getFieldData();
        List<?> names = wrapper.getFieldWrapper(F_NAME).getFieldData();
        List<?> exts = wrapper.getFieldWrapper(F_EXT).getFieldData();
        List<?> sourceUrls = wrapper.getFieldWrapper(F_SOURCE_URL).getFieldData();

        List<ImageSearchResult> list = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            ImageSearchResult r = new ImageSearchResult();
            r.setId(asString(ids.get(i)));
            r.setName(i < names.size() ? asString(names.get(i)) : null);
            r.setExt(i < exts.size() ? asString(exts.get(i)) : null);
            r.setSourceUrl(i < sourceUrls.size() ? asString(sourceUrls.get(i)) : null);
            list.add(r);
        }
        return list;
    }

    public long countAll() {
        ensureCollection();
        R<io.milvus.grpc.GetCollectionStatisticsResponse> resp = client().getCollectionStatistics(
                GetCollectionStatisticsParam.newBuilder()
                        .withCollectionName(properties.getCollectionName())
                        .build());
        check(resp, "getCollectionStatistics");
        for (io.milvus.grpc.KeyValuePair kv : resp.getData().getStatsList()) {
            if ("row_count".equals(kv.getKey())) {
                return Long.parseLong(kv.getValue());
            }
        }
        return 0;
    }

    public void deleteById(String id) {
        ensureCollection();
        check(client().delete(DeleteParam.newBuilder()
                .withCollectionName(properties.getCollectionName())
                .withExpr(F_ID + " in [\"" + id + "\"]")
                .build()), "delete");
    }

    private static String asString(Object o) {
        return o == null ? null : o.toString();
    }

    private static void check(R<?> r, String op) {
        if (r.getStatus() != R.Status.Success.getCode()) {
            throw new RuntimeException("Milvus " + op + " 失败: " + r.getMessage());
        }
    }
}
