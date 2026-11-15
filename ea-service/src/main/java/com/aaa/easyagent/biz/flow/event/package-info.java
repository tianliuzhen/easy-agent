/**
 * Flow 运行事件出口：把调度与节点产生的过程推送给前端。
 * <p>
 * {@link com.aaa.easyagent.biz.flow.event.FlowEventType} 维护事件名与前端消息类型的映射；
 * {@link com.aaa.easyagent.biz.flow.event.FlowEventPublisher} 负责写 SSE，并在终态事件后关闭出口；
 * {@link com.aaa.easyagent.biz.flow.event.FlowNodeSseEmitter} 让单 Agent 的 think/data/tool
 * 事件带上 nodeId 后汇入同一条 Flow 流。
 * <p>
 * 依赖 runtime（取 runId 与 SseEmitter），不依赖 node / plugin / engine。
 */
package com.aaa.easyagent.biz.flow.event;
