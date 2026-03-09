package com.aaa.easyagent.biz.agent.wrapper;

import com.aaa.easyagent.biz.agent.service.ChatRecordSaver;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author liuzhen.tian
 * @version 1.0 SceneWrapper.java  2026/3/14 0:44
 */
public class SceneWrapper {

    /**
     * 这是一个错误的demo
     *
     * @param consumer
     * @param <T>
     * @return
     */
    public static <T> Consumer<? super T> wrapperErrorDemo(Consumer<? super T> consumer) {

        // 主线程的消息上下文
        ThreadLocal<List<ChatRecordSaver.ChatContext>> parent = ChatRecordSaver.messageContext;

        return new Consumer<T>() {
            @Override
            public void accept(T t) {
                // 复制主线程的消息上下文
                ThreadLocal<List<ChatRecordSaver.ChatContext>> child = ChatRecordSaver.messageContext;
                /**
                 * 这里有坑
                 * 此时的parent.get() =》return get(Thread.currentThread());
                 * 还是当前的线程的消息上下文，而不是parent的
                 */
                ChatRecordSaver.messageContext.set(parent.get());
                consumer.accept(t);
            }
        };
    }

    public static <T> Consumer<? super T> wrapper(Consumer<? super T> consumer) {

        // 主线程的消息上下文
        ThreadLocal<List<ChatRecordSaver.ChatContext>> parent = ChatRecordSaver.messageContext;
        List<ChatRecordSaver.ChatContext> chatContexts = parent.get();

        return new Consumer<T>() {
            @Override
            public void accept(T t) {
                // 复制主线程的消息上下文
                ThreadLocal<List<ChatRecordSaver.ChatContext>> child = ChatRecordSaver.messageContext;
                ChatRecordSaver.messageContext.set(chatContexts);
                consumer.accept(t);
            }
        };
    }
}
