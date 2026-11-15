/**
 * 节点扩展点（SPI）：新增一种节点类型只需在 plugin 包实现本包的接口。
 * <p>
 * {@link com.aaa.easyagent.biz.flow.node.FlowNodePlugin} 定义「元数据 + 端口补全 + 校验 + 执行」四件事；
 * {@link com.aaa.easyagent.biz.flow.node.FlowNodePluginRegistry} 按 type@version 收集全部实现；
 * {@link com.aaa.easyagent.biz.flow.node.FlowNodeTypes} 是内置节点类型 code 的唯一出处；
 * {@link com.aaa.easyagent.biz.flow.node.NodeExecutionContext} 与
 * {@link com.aaa.easyagent.biz.flow.node.NodeExecutionResult} 是节点与引擎之间的入参、出参契约。
 * <p>
 * 依赖 event + runtime，不依赖 compile / engine / plugin，保证扩展方不用知道调度实现。
 */
package com.aaa.easyagent.biz.flow.node;
