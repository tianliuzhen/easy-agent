package com.aaa.easyagent.common.context;

/**
 * @author liuzhen.tian
 * @version 1.0 UserContextHolder.java  2026/3/9 22:20
 */
public class UserContextHolder {

    /**
     * 获取当前用户ID
     * todo 从ThreadLocal中获取当前用户ID 暂时没集成
     *
     * @return
     */
    public static String getUserId() {
        return "1";
    }
}
