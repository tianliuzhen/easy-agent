package com.aaa.easyagent.biz.agent.context;

/** SseHelper 识别的节点级事件出口。 */
public interface ScopedEventEmitter {
    void sendScoped(String eventType, String content);
}
