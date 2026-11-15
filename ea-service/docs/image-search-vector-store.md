# 图片搜索向量库切换

知识库和图片搜索统一使用 `easy-agent.vectorstore.provider`，修改后重启应用生效：

```yaml
easy-agent:
  vectorstore:
    provider: es # es 或 milvus
  image-search:
    dimension: 2560
    index-name: ea_image_search      # ES 专属图片索引
    collection-name: ea_image_search # Milvus 专属图片集合
```

该配置已在 `src/main/resources/application.yml` 中定义，默认仍为 `milvus`。ES 连接复用 `config/elasticSearch-config.properties` 的 `spring.elasticsearch.*`；使用桌面 ES 启动脚本后，将 provider 改为 `es` 并重启后端即可。

图片搜索使用百炼多模态 embedding，仍需 `DASHSCOPE_API_KEY`。图片向量维度取 `easy-agent.image-search.dimension`，与文本知识库的 2048 维配置独立；修改模型或维度后需要使用新索引或集合并重新导入。

## 支持范围

原有 `/imageSearch/` 下的上传、URL 导入、以图搜图、分页列表、计数和删除接口保持不变。`ImageSearchServiceImpl` 依赖 `ImageVectorDao`，由条件装配选择 `ImageElasticsearchDao` 或 `ImageMilvusDao`。

- ES 模式不创建 Milvus 图片存储或 Milvus 客户端。
- ES 图片索引在首次业务访问时自动创建，使用 `dense_vector`、cosine 相似度和 keyword 图片标识。
- 已有 ES 索引会先校验字段、维度及距离度量；不兼容时明确报错，不删除已有数据。
- ES 写入和删除等待刷新后返回，保证后续查询与计数可见；重复删除不存在的图片向量可安全重试。
- ES 分数按 `2 × _score - 1` 转回余弦相似度口径，与 Milvus 的 COSINE 结果一致。
- ES 列表按图片标识稳定排序，仅返回元数据；当前 offset 分页沿用 ES 默认 10000 条窗口，超出时明确报错。计数使用 `_count`，不受搜索响应的总数上限影响。
- 本地图片保存位置与 URL 导入的来源链接规则不变。两种向量库的数据独立，切换后不会自动搬迁历史向量，需要重新导入；服装 ONNX 分割功能不受此开关影响。

## 验证

单元和装配测试：

```bash
mvn -Dtest=ImageVectorDaoConfigurationTest,ImageElasticsearchDaoTest,ImageSearchServiceImplTest test
```

真实 ES 集成测试需要显式指定专用测试实例（无认证的本地 HTTP 地址），每个用例仅创建、删除自己的随机临时索引：

```bash
IMAGE_SEARCH_ES_TEST_URI=http://127.0.0.1:19200 \
  mvn -Dtest=ImageElasticsearchDaoIntegrationTest test
```

未指定环境变量时跳过集成测试。测试覆盖真实 2560 维向量检索与分数、稳定分页、重用已有索引、删除幂等，以及维度冲突时保留已有数据。
