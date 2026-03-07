package com.aaa.easyagent.core.domain.result;

import com.aaa.easyagent.core.domain.DO.EaModelPlatformDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 模型平台配置结果对象
 *
 * @author liuzhen.tian
 * @version 1.0 EaModelPlatformResult.java  2026/3/7
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EaModelPlatformResult extends EaModelPlatformDO {
    
    /**
     * 获取模型版本数组 (解析 JSON 字符串)
     * 
     * @return 模型版本数组
     */
    public String[] getModelVersionArray() {
        String modelVersionsStr = this.getModelVersions();
        if (modelVersionsStr == null || modelVersionsStr.isEmpty()) {
            return new String[0];
        }
        try {
            // 简单的 JSON 数组解析，去除括号和引号
            String versions = modelVersionsStr.trim();
            if (versions.startsWith("[") && versions.endsWith("]")) {
                versions = versions.substring(1, versions.length() - 1);
                if (versions.isEmpty()) {
                    return new String[0];
                }
                // 分割并清理引号和空格
                String[] result = versions.split(",");
                for (int i = 0; i < result.length; i++) {
                    result[i] = result[i].trim().replace("\"", "");
                }
                return result;
            }
        } catch (Exception e) {
            // 解析失败返回空数组
        }
        return new String[0];
    }
}
