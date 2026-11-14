# 服装主体分割

移植自 Psylos 的 `seg_clothes.onnx` 检测流程。全程本地 CPU 推理，不调用大模型、数据库或 Milvus；只定位区域，不执行图搜或保存图片。

## 配置

独立配置为 `src/main/resources/config/image-detect-config.yml`，由 `application.properties` 导入。

- 将 `easy-agent.image-detect.enabled` 改为 `true`，重启后启用。
- 默认模型为 `classpath:model/seg_clothes.onnx`，随 JAR 打包；也可配置 `file:./models/seg_clothes.onnx`。
- 默认输入 512×512、2 个推理线程，返回最多 8 个候选区域，过滤脸、头发和四肢。
- 默认最小面积比例为 `0.004`（0.4%）。调高会过滤鞋、包等小目标。
- 图片上限为 5 MiB，Multipart 上限在同一 YML 内配置。
- 默认关闭时不加载模型。模型加载失败只停用检测功能，并记录错误，不阻断应用启动。

ONNX Runtime 使用原生内存，启用后会增加进程内存占用。依赖随应用打包，无需另行安装推理服务。

## 接口

接口沿用项目 JWT 鉴权，携带 `Authorization: Bearer <token>`，返回 `BaseResult`（`success`、`message`、`data`）。

| 方法 | 路径 | 入参 |
| --- | --- | --- |
| GET | `/imageDetect/status` | 无，`data` 为模型实际可用状态 |
| POST | `/imageDetect/regions` | Multipart 文件字段 `file` |
| POST | `/imageDetect/regions/bytes` | `application/octet-stream` 原始图片字节 |

两个识别接口均支持可选查询参数 `includeBodyParts` 和 `minAreaRatio`；未传时取配置默认值。`minAreaRatio` 必须是 0~1 的有限数值。

```bash
curl http://localhost:8080/imageDetect/regions \
  -H "Authorization: Bearer ${AUTH_TOKEN}" \
  -F 'file=@/path/to/image.jpg'
```

结果包含方向校正后的原图尺寸 `imageWidth/imageHeight`、候选区域 `regions`、推理耗时 `inferMillis` 和总耗时 `totalMillis`（均为毫秒）。每个区域包含类型、中文名、可售标记，以及 0~1 的 `x/y/width/height`。乘以校正后原图尺寸即可得到裁剪像素坐标；`areaRatio` 是区域像素面积占比，不是置信度。

支持 JPEG、PNG、WebP 等 ImageIO 可解码格式，JPEG 会按 EXIF 方向先校正再识别。

## 验证

```bash
mvn -Dtest=ImageDetectServiceTest,ImageDetectConfigTest,ImageDetectControllerTest test
```

测试使用随包模型和真实 JPEG/WebP/EXIF 样图，验证候选类型、小目标过滤、归一化坐标、配置绑定、模型加载失败隔离及接口响应。
