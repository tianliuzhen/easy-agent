package com.aaa.easyagent.biz.agent.context;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * SLF4J 的 MessageFormatter
 * SSE 推送辅助类
 * 提供日志、思考过程和数据的 SSE 推送功能
 * <p>
 * 使用示例：
 * SseHelper.sendLog(emitter, "用户 {} 正在执行操作", userId);                           // {} 等价于 {0}
 * SseHelper.sendThink(emitter, "正在分析第 {} 步", stepNumber);                         // {} 等价于 {0}
 * SseHelper.sendData(emitter, "用户 {0} 执行 {1} 操作", userId, operation);           // {0} {1} 按顺序对应参数
 * SseHelper.sendData(emitter, "用户 {1} 执行 {0} 操作", operation, userId);           // {0} {1} 可调整参数顺序
 *
 * @author liuzhen.tian
 * @version 1.0 SseHelper.java  2026/1/25 11:46
 */
@Slf4j
public class SseHelper {

    // 为每个 SseEmitter 维护一个锁，用于同步发送
    private static final ConcurrentHashMap<SseEmitter, ReentrantLock> emitterLocks = new ConcurrentHashMap<>();

    /**
     * 获取或创建 SseEmitter 对应的锁
     */
    private static ReentrantLock getLock(SseEmitter sseEmitter) {
        return emitterLocks.computeIfAbsent(sseEmitter, k -> new ReentrantLock());
    }

    /**
     * 发送日志消息
     *
     * @param sseEmitter SSE连接对象，可为null
     * @param message    消息模板及参数（支持{}占位符，{}等价于{0}，多个参数可用{0}{1}等）
     */
    public static void sendLog(SseEmitter sseEmitter, Object... message) {
        doSend(sseEmitter, message, "log");
    }

    /**
     * 发送思考过程消息
     *
     * @param sseEmitter SSE连接对象，可为null
     * @param message    消息模板及参数（支持{}占位符，{}等价于{0}，多个参数可用{0}{1}等）
     */
    public static void sendThink(SseEmitter sseEmitter, Object... message) {
        if (message != null && message.length > 0) {
            doSend(sseEmitter, message, "think");
        }
    }

    /**
     * 发送数据消息
     *
     * @param sseEmitter SSE连接对象，可为null
     * @param message    消息模板及参数（支持{}占位符，{}等价于{0}，多个参数可用{0}{1}等）
     */
    public static void sendData(SseEmitter sseEmitter, Object... message) {
        doSend(sseEmitter, message, "data");
    }

    public static void sendTool(SseEmitter sseEmitter, Object... message) {
        doSend(sseEmitter, message, "tool");
    }

    public static void sendFinalAnswer(SseEmitter sseEmitter, Object... message) {
        doSend(sseEmitter, message, "finalAnswer");
    }

    /**
     * 发送错误消息
     *
     * @param sseEmitter SSE连接对象，可为null
     * @param message    消息模板及参数
     */
    public static void sendError(SseEmitter sseEmitter, Object... message) {
        // 在消息前添加[ERROR]前缀用于前端识别
        doSend(sseEmitter, message, "log");

    }


    /**
     * 通用发送方法
     *
     * @param sseEmitter SSE 连接对象
     * @param message    消息模板及参数
     * @param eventName  事件名称
     */
    private static void doSend(SseEmitter sseEmitter, Object[] message, String eventName) {
        if (message == null || message.length == 0) {
            return;
        }
        if (message.length == 1 && (message[0] == null || StringUtils.isBlank(message[0].toString()))) {
            return;
        }

        String formattedMessage = parseMessage(message);

        // 发送日志
        log.info(formattedMessage);

        // 如果需要通过 SSE 发送
        if (sseEmitter != null) {
            ReentrantLock lock = getLock(sseEmitter);
            lock.lock();
            try {
                // 统一发送 JSON 格式数据，包含事件类型和消息内容
                String jsonData = "{\"type\":\"" + eventName + "\",\"content\":\"" + escapeJsonString(formattedMessage) + "\"}";
                sseEmitter.send(SseEmitter.event()
                        .data(jsonData));
            } catch (IOException e) {
                log.error("SSE 发送失败，type={}", eventName, e);
                // 可选：通知监听器连接已断开
                try {
                    sseEmitter.complete();
                } catch (Exception ex) {
                    log.error("SSE 连接关闭失败", ex);
                }
            } catch (IllegalStateException e) {
                // 处理 ResponseBodyEmitter has already completed 错误
                log.warn("SSE 连接已关闭，无法发送消息，type={}", eventName);
                // 清理锁
                emitterLocks.remove(sseEmitter);
            } finally {
                lock.unlock();
            }
        }
    }

    private static String parseMessage(Object[] message) {
        // 第一个元素是模板
        String template = message[0].toString();

        // 获取参数（从第二个元素开始）
        Object[] args = new Object[0];
        if (message.length > 1) {
            args = new Object[message.length - 1];
            System.arraycopy(message, 1, args, 0, args.length);
        }

        // 格式化消息
        String formattedMessage = formatMessage(template, args);
        return formattedMessage;
    }


    /**
     * 格式化消息（支持{}占位符，{}等价于{0}，多个参数可用{0}{1}等）
     *
     * @param template 消息模板
     * @param args     参数
     * @return 格式化后的消息
     */
    public static String formatMessage(String template, Object... args) {
        if (template == null) {
            return args == null || args.length == 0 ? "" : String.join(", ", String.valueOf(args));
        }

        if (args == null || args.length == 0) {
            return template;
        }

        try {
            // 使用 SLF4J 的 MessageFormatter 处理 {} 占位符
            return MessageFormatter.arrayFormat(template, args).getMessage();
        } catch (Exception e) {
            // 降级处理
            return fallbackFormat(template, args);
        }
    }

    /**
     * 降级格式化方法
     *
     * @param template 消息模板
     * @param args     参数
     * @return 格式化后的消息
     */
    private static String fallbackFormat(String template, Object... args) {
        if (args == null || args.length == 0) {
            return template;
        }

        String result = template;
        for (Object arg : args) {
            result = result.replaceFirst("\\{\\}", String.valueOf(arg != null ? arg : "null"));
        }
        return result;
    }

    /**
     * 转义JSON字符串中的特殊字符
     *
     * @param str 待转义的字符串
     * @return 转义后的字符串
     */
    private static String escapeJsonString(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
