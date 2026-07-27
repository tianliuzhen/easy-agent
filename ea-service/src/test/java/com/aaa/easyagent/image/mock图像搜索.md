## 目前代码中已经具备了图像搜索的能力

## 现在需要下个脚本导入
下面是开发环境账号
#    url: jdbc:mysql://rm-bp1v66rws9oe8p4bqdo.mysql.rds.aliyuncs.com:3306/psylos1_dev?useSSL=false&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true
#    # 数据库用户名
#    username: root
#    # 数据库密码
#    password: Root123456@

有一个 products表 里面的 
main_image_url 格式是：https://cdn.shopify.com/s/files/1/0608/9798/4733/files/image_imageId_f34b5768-45d6-4e8a-86da-b9686f37abbd.jpg?v=1782445177
有些是webp格式,

```SQL
CREATE TABLE `products` (
`id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
`product_code` varchar(100) NOT NULL COMMENT '商品唯一编码(UUID)',
`supplier_id` varchar(64) DEFAULT NULL COMMENT '供应商业务ID(UUID)',
`supplier_name` varchar(255) NOT NULL DEFAULT '' COMMENT '供应商名称冗余',
`picker_name` varchar(64) DEFAULT NULL COMMENT '选品人姓名',
`title` varchar(255) NOT NULL COMMENT '商品标题',
`description` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '商品描述',
`main_image_url` varchar(1000) DEFAULT NULL COMMENT '主图URL',
`image_list` json DEFAULT NULL COMMENT '图片列表(JSON数组)',
`videos` json DEFAULT NULL COMMENT '视频列表(JSON数组)',
`category_code` varchar(100) DEFAULT NULL COMMENT '后台类目编码',
`portal_category_code` varchar(100) DEFAULT NULL COMMENT '前台类目编码',
`external_product_code` varchar(100) DEFAULT NULL COMMENT '外部商品编码',
`url_handle` varchar(512) DEFAULT NULL COMMENT '商品跳转链接',
`site` varchar(255) DEFAULT NULL COMMENT '站点集合，逗号分隔，如US,BE,AU',
`brand_code` varchar(100) DEFAULT NULL COMMENT '品牌编码',
`brand_name` varchar(255) DEFAULT NULL COMMENT '品牌名称',
`product_type` tinyint NOT NULL DEFAULT '1' COMMENT '商品类型:1实物,2虚拟',
`status` tinyint NOT NULL DEFAULT '0' COMMENT '上架状态:0下架,1上架,2隐藏上架',
`audit_status` tinyint NOT NULL DEFAULT '0' COMMENT '审核状态:0草稿,1待审,2通过,3驳回',
`sold_count` bigint unsigned NOT NULL DEFAULT '0' COMMENT 'SPU累计销量',
`tags` varchar(500) DEFAULT NULL COMMENT '标签，逗号分隔',
`seo_title` varchar(255) DEFAULT NULL COMMENT 'SEO标题',
`seo_desc` mediumtext COMMENT 'SEO描述',
`materials_info` mediumtext COMMENT '材质/成分信息',
`size_chart_required` tinyint NOT NULL DEFAULT '0' COMMENT '是否需要填写尺码表:0否,1是',
`shipping_return_policy` text COMMENT '运输与退换政策文案',
`tenant_id` varchar(100) DEFAULT NULL COMMENT '租户标识',
`creator` varchar(64) DEFAULT NULL COMMENT '创建人',
`create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
`updater` varchar(64) DEFAULT NULL COMMENT '更新人',
`update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
`is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除:0否,1是',
PRIMARY KEY (`id`),
UNIQUE KEY `uk_products_product_code` (`product_code`),
UNIQUE KEY `uk_products_url_handle` (`url_handle`),
KEY `idx_products_tenant_status_deleted` (`tenant_id`,`status`,`is_deleted`),
KEY `idx_products_tenant_audit_deleted` (`tenant_id`,`audit_status`,`is_deleted`),
KEY `idx_products_category_code` (`category_code`),
KEY `idx_products_portal_category_code` (`portal_category_code`),
KEY `idx_products_brand_code` (`brand_code`),
KEY `idx_products_supplier_id` (`supplier_id`),
KEY `idx_products_sold_count` (`sold_count`),
KEY `idx_products_picker_name` (`picker_name`)
) ENGINE=InnoDB AUTO_INCREMENT=26369 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品主表';
```

现在需要在 /Users/tlz/Documents/workspace/my/easy-agent/ea-service/src/test/java/com/aaa/easyagent/image
目录下面，jdbc的方式来链接查询主图，并且看下当前接口直接上传到