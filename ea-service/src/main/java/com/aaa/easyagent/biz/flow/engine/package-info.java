/**
 * 运行期调度：按编译产物推进执行，并收口超时、取消与失败。
 * <p>
 * {@link com.aaa.easyagent.biz.flow.engine.FlowEngine} 是对外唯一入口；
 * {@link com.aaa.easyagent.biz.flow.engine.DefaultFlowEngine} 用控制边三态传播做动态调度；
 * {@link com.aaa.easyagent.biz.flow.engine.FlowExecutorConfig} 提供全局共享的节点线程池；
 * {@link com.aaa.easyagent.biz.flow.engine.FlowOrchestrationProperties} 是并发与超时的配置项。
 * <p>
 * 本包位于 flow 的最上层，依赖 compile + node + event + runtime，不被它们反向依赖。
 */
package com.aaa.easyagent.biz.flow.engine;
