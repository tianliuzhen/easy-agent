/**
 * 编译期：把前端画出来的 FlowDefinition 校验成一张可执行的图。
 * <p>
 * {@link com.aaa.easyagent.biz.flow.compile.FlowCompiler} 做图级校验（START/END 数量、边端口匹配、
 * 控制流无环、从 START 可达）并按边类型建索引；
 * {@link com.aaa.easyagent.biz.flow.compile.CompiledFlowPlan} 是编译产物，运行期只读；
 * {@link com.aaa.easyagent.biz.flow.compile.PortTypeSystem} 判定数据端口类型是否兼容。
 * <p>
 * 依赖 node（取插件做节点级校验），不依赖 engine。校验不通过抛
 * {@link com.aaa.easyagent.biz.flow.compile.FlowValidationException}，带完整问题清单。
 */
package com.aaa.easyagent.biz.flow.compile;
