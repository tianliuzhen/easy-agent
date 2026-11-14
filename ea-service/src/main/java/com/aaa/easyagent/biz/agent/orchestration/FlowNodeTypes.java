package com.aaa.easyagent.biz.agent.orchestration;

/**
 * 内置 Flow 节点类型 code。
 * <p>
 * 与各 {@link FlowNodePlugin} 的 metadata().type 以及前端节点库一一对应，调度与编译期
 * 需要按类型做特殊判断时引用这里的常量，不再散落字符串字面量。
 */
public final class FlowNodeTypes {

    /** 开始节点：声明 Flow 输入，唯一的无控制入边起点 */
    public static final String START = "START";
    /** 结束节点：声明 Flow 最终输出 */
    public static final String END = "END";
    /** Agent 节点：调用已配置的单个 Agent，也可作为 SUPERVISOR 的 Worker */
    public static final String AGENT = "AGENT";
    /** 主管节点：把 Worker Agent 作为工具动态调度 */
    public static final String SUPERVISOR = "SUPERVISOR";
    /** 路由节点：用模型选择一个控制出口 */
    public static final String ROUTER = "ROUTER";
    /** 并行节点：同时激活所有控制分支 */
    public static final String FORK = "FORK";
    /** 聚合节点：等待已激活分支并聚合数据 */
    public static final String JOIN = "JOIN";
    /** 模板节点：把多个输入渲染为文本 */
    public static final String TEMPLATE = "TEMPLATE";

    private FlowNodeTypes() {
    }
}
