package com.aaa.easyagent.common.util;

import org.slf4j.Logger;
import org.slf4j.helpers.MessageFormatter;

/**
 * 日志工具类，封装 SLF4J 的 error/warn 调用。
 * <p>
 * 解决痛点：原生 {@code log.error("msg: {}", arg, e)} 中异常必须放在参数最后，
 * 且与 {@code {}} 占位参数混在一起，容易遗漏堆栈或顺序写错。
 * 本工具将异常统一放在 logger 之后的<b>第一个参数</b>，消除歧义，且始终打印完整堆栈。
 * </p>
 *
 * <pre>
 * // 各类沿用自己的 @Slf4j log，传进来即可，logger 名称（日志来源）保持正确
 *
 * // 仅打印异常堆栈（消息取 e.getMessage()）
 * LogUtil.error(log, e);
 *
 * // 异常 + 带占位符的消息
 * LogUtil.error(log, e, "MCP 工具调用失败: serverName={}, toolName={}", serverName, toolName);
 *
 * // 无异常，仅格式化错误消息
 * LogUtil.error(log, "HTTP 请求失败，状态码: {}", statusCode);
 * </pre>
 *
 * @author liuzhen.tian
 * @version 1.0 LogUtil.java  2026/6/13
 */
public final class LogUtil {

    private LogUtil() {
    }

    /**
     * 打印异常堆栈，消息使用 {@code e.getMessage()}
     *
     * @param log 调用方的 logger
     * @param e   异常，可为 null
     */
    public static void error(Logger log, Throwable e) {
        if (log.isErrorEnabled()) {
            log.error(e != null ? e.getMessage() : "null", e);
        }
    }

    /**
     * 打印格式化消息 + 完整异常堆栈
     *
     * @param log      调用方的 logger
     * @param e        异常，可为 null
     * @param template 消息模板，使用 {@code {}} 占位符
     * @param args     占位符参数（不要把异常放进来）
     */
    public static void error(Logger log, Throwable e, String template, Object... args) {
        if (log.isErrorEnabled()) {
            log.error(format(template, args), e);
        }
    }

    /**
     * 仅打印格式化的错误消息（无异常堆栈）
     *
     * @param log      调用方的 logger
     * @param template 消息模板，使用 {@code {}} 占位符
     * @param args     占位符参数
     */
    public static void error(Logger log, String template, Object... args) {
        if (log.isErrorEnabled()) {
            log.error(format(template, args));
        }
    }

    /**
     * 打印格式化消息 + 完整异常堆栈（warn 级别）
     *
     * @param log      调用方的 logger
     * @param e        异常，可为 null
     * @param template 消息模板，使用 {@code {}} 占位符
     * @param args     占位符参数（不要把异常放进来）
     */
    public static void warn(Logger log, Throwable e, String template, Object... args) {
        if (log.isWarnEnabled()) {
            log.warn(format(template, args), e);
        }
    }

    /**
     * 仅打印格式化的告警消息（无异常堆栈）
     *
     * @param log      调用方的 logger
     * @param template 消息模板，使用 {@code {}} 占位符
     * @param args     占位符参数
     */
    public static void warn(Logger log, String template, Object... args) {
        if (log.isWarnEnabled()) {
            log.warn(format(template, args));
        }
    }

    /**
     * 使用 SLF4J 的 {@code {}} 占位符语义格式化消息
     */
    private static String format(String template, Object... args) {
        if (template == null) {
            return "null";
        }
        if (args == null || args.length == 0) {
            return template;
        }
        return MessageFormatter.arrayFormat(template, args).getMessage();
    }
}
