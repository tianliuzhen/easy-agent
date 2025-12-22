package com.aaa.easyagent.core.domain.base;

import lombok.Data;

import java.util.function.Supplier;

/**
 * @author liuzhen.tian
 * @version 1.0 BaseResult.java  2025/6/15 12:20
 */
@Data
public class BaseResult<T> {
    private T data;

    private boolean success;

    public static <T> BaseResult<T> buildSuc(T data) {
        BaseResult<T> baseResult = new BaseResult();
        baseResult.setSuccess(true);
        baseResult.setData(data);
        return baseResult;
    }

    public static <T> BaseResult<T> buildSuc(Runnable runnable) {
        BaseResult<T> baseResult = new BaseResult();
        baseResult.setSuccess(true);
        runnable.run();
        return baseResult;
    }

    public static <T> BaseResult<T> buildSuc(Supplier<T> supplier) {
        BaseResult<T> baseResult = new BaseResult();
        baseResult.setSuccess(true);
        baseResult.setData(supplier.get());
        return baseResult;
    }
}
