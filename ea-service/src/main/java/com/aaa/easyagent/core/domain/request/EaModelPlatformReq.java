package com.aaa.easyagent.core.domain.request;

import com.aaa.easyagent.core.domain.DO.EaModelPlatformDO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 模型平台配置请求对象
 *
 * @author liuzhen.tian
 * @version 1.0 EaModelPlatformReq.java  2026/3/7
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EaModelPlatformReq extends EaModelPlatformDO {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 模型版本数组 (JSON 字符串格式，例如 ["v1","v2"])
     */
    private String modelVersionArray;
    
    /**
     * 验证请求参数是否有效
     * 
     * @return 是否有效
     */
    public boolean isValid() {
        return this.getModelPlatform() != null && !this.getModelPlatform().isEmpty();
    }
    
    /**
     * 获取模型版本数组 (将 JSON 字符串转换为数组)
     *
     * @return 模型版本数组
     */
    public String[] getModelVersionsAsArray() {
        if (modelVersionArray == null || modelVersionArray.isEmpty()) {
            return new String[0];
        }
        
        try {
            // 尝试解析 JSON 数组
            return objectMapper.readValue(modelVersionArray, String[].class);
        } catch (JsonProcessingException e) {
            // 如果不是 JSON 格式，尝试按逗号分隔处理
            if (!modelVersionArray.startsWith("[")) {
                String[] versions = modelVersionArray.split(",");
                for (int i = 0; i < versions.length; i++) {
                    versions[i] = versions[i].trim();
                }
                return versions;
            }
            // 解析失败返回空数组
            return new String[0];
        }
    }
}
