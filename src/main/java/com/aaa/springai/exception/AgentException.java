package com.aaa.springai.exception;

/**
 * @author liuzhen.tian
 * @version 1.0 AgentException.java  2025/5/25 22:23
 */
public class AgentException extends RuntimeException {
    public AgentException() {
    }

    public AgentException(String message) {
        super(message);
    }

    public AgentException(String message, Throwable cause) {
        super(message, cause);
    }

    public AgentException(Throwable cause) {
        super(cause);
    }

    public AgentException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
