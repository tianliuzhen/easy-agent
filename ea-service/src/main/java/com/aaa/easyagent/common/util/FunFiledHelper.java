package com.aaa.easyagent.common.util;

import tk.mybatis.mapper.weekend.WeekendSqls;
import tk.mybatis.mapper.weekend.reflection.ReflectionOperationException;
import tk.mybatis.mapper.weekend.reflection.Reflections;

import java.beans.Introspector;
import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * @author liuzhen.tian
 * @version 1.0 FunUtil.java  2023/7/30 18:10
 */
public class FunFiledHelper {
    private static final Pattern GET_PATTERN = Pattern.compile("^get[A-Z].*");
    private static final Pattern IS_PATTERN = Pattern.compile("^is[A-Z].*");

    private FunFiledHelper() {
    }

    public interface Fun<T, R> extends Function<T, R>, Serializable {
    }


    /**
     * {@link Reflections} 获取字段名的 让方法直接接收 Fun<T, R> fn 而不是 Fn
     *
     * @param fn
     * @param <T>
     * @param <R>
     * @return
     */
    public static <T, R> String getFieldName(Fun<T, R> fn) {
        try {
            Method method = fn.getClass().getDeclaredMethod("writeReplace");
            method.setAccessible(Boolean.TRUE);
            SerializedLambda serializedLambda = (SerializedLambda) method.invoke(fn);
            String getter = serializedLambda.getImplMethodName();
            if (GET_PATTERN.matcher(getter).matches()) {
                getter = getter.substring(3);
            } else if (IS_PATTERN.matcher(getter).matches()) {
                getter = getter.substring(2);
            }
            return Introspector.decapitalize(getter);
        } catch (ReflectiveOperationException e) {
            throw new ReflectionOperationException(e);
        }
    }
}
