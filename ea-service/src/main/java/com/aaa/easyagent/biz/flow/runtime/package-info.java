/**
 * 单次 Flow 运行的上下文、状态与审计。
 * <p>
 * {@link com.aaa.easyagent.biz.flow.runtime.FlowRunContext} 承载 runId、会话、取消标志；
 * {@link com.aaa.easyagent.biz.flow.runtime.FlowState} 存放节点端口产出；
 * {@link com.aaa.easyagent.biz.flow.runtime.FlowRunRecorder} 把运行与节点的开始、结束写审计表。
 * <p>
 * 本包是 flow 的最底层，不依赖 flow 下的其他任何包。
 */
package com.aaa.easyagent.biz.flow.runtime;
