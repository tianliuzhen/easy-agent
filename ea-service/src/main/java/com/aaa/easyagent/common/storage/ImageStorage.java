package com.aaa.easyagent.common.storage;

/**
 * 图片存储抽象。当前实现为本地磁盘（{@link LocalImageStorage}），
 * 未来接入 OSS 只需新增 OssImageStorage 实现并配置 easy-agent.image-search.storage=oss，
 * Service / DAO / Controller / 前端均无需改动。
 *
 * @author liuzhen.tian
 */
public interface ImageStorage {

    /** 保存图片字节 */
    void save(String id, byte[] bytes, String ext);

    /** 读取图片字节 */
    byte[] load(String id);

    /** 删除图片 */
    void delete(String id);

    /**
     * 返回可供前端 &lt;img&gt; 访问的地址。
     * local：相对路径 /imageSearch/file/{id}（前端补 API_BASE_URL）；
     * oss：对象的绝对 URL。
     */
    String publicUrl(String id);
}
