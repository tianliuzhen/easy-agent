package com.aaa.easyagent.common.config.exception;

/**
 * @author liuzhen.tian
 * @version 1.0 AgentToolException.java  2025/5/25 22:25
 */
public class AgentToolException extends AgentException{
    public AgentToolException() {
    }

    public AgentToolException(String message) {
        super(message);
    }

    public AgentToolException(String message, Throwable cause) {
        super(message, cause);
    }

    public AgentToolException(Throwable cause) {
        super(cause);
    }

    public AgentToolException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
