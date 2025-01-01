package com.aaa.springai.util;

/**
 * @author liuzhen.tian
 * @version 1.0 RedisSearchExample.java  2024/12/29 17:08
 */

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.*;
import redis.clients.jedis.search.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RedisSearchExample {
    public static final String GOODS_IDX_PREFIX = "idx:goods:";

    private UnifiedJedis client;

    public RedisSearchExample() {
        GenericObjectPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxIdle(10);
        jedisPoolConfig.setMaxWaitMillis(3000);
        jedisPoolConfig.setJmxEnabled(false);
        client = new JedisPooled(jedisPoolConfig, "124.221.225.161", 6389, 1000, null, 0);
    }

    /**
     * 新增索引数据
     */
    private void hset(String keyPrefix, Map<String, String> hash) {
        // 支持中文
        hash.put("_language", "chinese");
        client.hset(keyPrefix, hash);
    }

    /**
     * 查询索引列表
     */
    public Set<String> listIndex() {
        return client.ftList();
    }


    /**
     * 创建索引
     *
     * @param idxName 索引名称
     * @param prefix  要索引的数据前缀
     * @param schema  索引字段配置
     */
    public void createIndex(String idxName, String prefix, Schema schema) {
        IndexDefinition rule = new IndexDefinition(IndexDefinition.Type.HASH)
                .setPrefixes(prefix)
                .setLanguage("chinese");
        client.ftCreate(idxName,
                IndexOptions.defaultOptions().setDefinition(rule),
                schema);
    }

    /**
     * 查询
     *
     * @param idxName 索引名称
     * @param search  查询key
     * @param sort    排序字段
     * @return searchResult
     */
    public SearchResult query(String idxName, String search, String sort) {
        Query q = new Query(search);
        if (StringUtils.isNotBlank(sort)) {
            q.setSortBy(sort, false);
        }
        q.setLanguage("chinese");
        q.limit(0, 10);
        return client.ftSearch(idxName, q);
    }


    public static void main(String[] args) {
        RedisSearchExample example = new RedisSearchExample();
        example.createIndex("idx:goods", "goods", new Schema());


        Map<String, String> hash = new HashMap<>();
        hash.put("id", "1");
        hash.put("goodsName", "你好hello");
        example.hset("idx:goods", hash);
        SearchResult searchResult = example.query("idx:goods", "*", null);
        System.out.println(searchResult.toString());
    }

}

