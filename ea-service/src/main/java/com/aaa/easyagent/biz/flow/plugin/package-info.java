/**
 * 内置节点实现：每个类对应前端节点库里的一种节点。
 * <p>
 * 基础节点 START / END，控制节点 FORK / JOIN / ROUTER / SUPERVISOR，
 * 业务节点 AGENT，数据节点 TEMPLATE。
 * {@link com.aaa.easyagent.biz.flow.plugin.NodeMetadataSupport} 是构造节点元数据与端口的内部工具，
 * {@link com.aaa.easyagent.biz.flow.plugin.FlowTemplateRenderer} 负责 {{ 变量 }} 渲染，二者只服务于本包。
 * <p>
 * 依赖 node（实现 SPI）与 event（节点级 SSE 出口），不依赖 engine，因此新增节点不用改调度代码。
 */
package com.aaa.easyagent.biz.flow.plugin;
