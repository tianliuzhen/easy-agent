import {useCallback, useEffect, useMemo, useState} from 'react';
import {useLocation} from 'react-router-dom';
import {queryAgent, type AgentDetail} from '../../../api/ChatConversationApi';
import {flowApi, type FlowSummary} from '../../../api/FlowApi';
import {eaAgentApi} from '../../../api/EaAgentApi';
import type {QuickPromptGroup} from '../ChatComponents';

export interface ChatSessionDisplay {
    name: string;
    desc?: string;
    avatar?: string;
    welcome?: string;
}

export interface ChatSession {
    /** 会话归属的 Agent ID（编排模式取首节点，单 Agent 模式取 URL） */
    agentId: number;
    /** 编排模式下的 flowId，单 Agent 模式为 undefined */
    flowId?: number;
    isFlow: boolean;
    agentDetail: AgentDetail | null;
    flowDetail: FlowSummary | null;
    quickPrompts: QuickPromptGroup[];
    display: ChatSessionDisplay;
    modelVersion: string | null;
    modelIcon?: string;
    conversationId: number | null;
    setConversationId: (id: number | null) => void;
    syncSessionIdToUrl: (id: number) => void;
    clearSessionIdFromUrl: () => void;
}

const parseModelVersion = (modelConfig?: string): string | null => {
    if (!modelConfig) return null;
    try {
        const config = JSON.parse(modelConfig);
        return config.modelVersion || config.version || null;
    } catch (error) {
        console.error('解析 modelConfig 失败:', error);
        return null;
    }
};

/**
 * 管理聊天页的会话身份：从 URL 解析 agentId/flowId，加载头部信息（Agent/编排详情、浮选提示词），
 * 并维护当前 conversationId 与 URL 的 sessionId 同步。
 */
export const useChatSession = (): ChatSession => {
    const location = useLocation();
    const [agentDetail, setAgentDetail] = useState<AgentDetail | null>(null);
    const [flowDetail, setFlowDetail] = useState<FlowSummary | null>(null);
    const [quickPrompts, setQuickPrompts] = useState<QuickPromptGroup[]>([]);
    const [conversationId, setConversationId] = useState<number | null>(null);

    // URL 参数解析（search 变化时重算）
    const {urlAgentId, flowId} = useMemo(() => {
        const params = new URLSearchParams(location.search);
        const flowParam = params.get('flowId');
        const agentParam = params.get('agentId');
        return {
            urlAgentId: agentParam ? parseInt(agentParam, 10) : 1,
            flowId: flowParam ? parseInt(flowParam, 10) : undefined,
        };
    }, [location.search]);

    const isFlow = flowId !== undefined;

    // 编排模式：会话归属首节点 Agent（与后端 execFlow 一致）
    const agentId = useMemo(() => {
        if (isFlow) {
            const agentNode = flowDetail?.draft?.nodes.find(node => node.config.agentId != null);
            return agentNode ? Number(agentNode.config.agentId) : 1;
        }
        return urlAgentId;
    }, [isFlow, flowDetail, urlAgentId]);

    // 按模式加载头部信息
    useEffect(() => {
        let cancelled = false;
        const load = async () => {
            if (isFlow) {
                setAgentDetail(null);
                setQuickPrompts([]);
                try {
                    const result = await flowApi.detail(flowId!);
                    if (!cancelled) setFlowDetail(result);
                } catch (error) {
                    console.error('加载编排详情失败:', error);
                }
            } else {
                setFlowDetail(null);
                try {
                    const detail = await queryAgent(urlAgentId);
                    if (!cancelled) setAgentDetail(detail);
                } catch (error) {
                    console.error('加载 Agent 详情失败:', error);
                }
                try {
                    const result = await eaAgentApi.listQuickPrompt(urlAgentId);
                    if (!cancelled && result && Array.isArray(result.data)) {
                        const raw = result.data as Array<{id?: number; label?: string; questions?: string[]}>;
                        setQuickPrompts(
                            raw.map((p) => ({
                                id: p.id,
                                label: p.label || '',
                                questions: Array.isArray(p.questions) ? p.questions : [],
                            }))
                        );
                    }
                } catch (error) {
                    console.error('加载浮选提示词失败:', error);
                }
            }
        };
        load();
        return () => {
            cancelled = true;
        };
    }, [isFlow, flowId, urlAgentId]);

    const syncSessionIdToUrl = useCallback((id: number) => {
        const params = new URLSearchParams(window.location.search);
        params.set('sessionId', id.toString());
        window.history.replaceState({}, '', `${window.location.pathname}?${params.toString()}`);
    }, []);

    const clearSessionIdFromUrl = useCallback(() => {
        const params = new URLSearchParams(window.location.search);
        params.delete('sessionId');
        window.history.replaceState({}, '', `${window.location.pathname}?${params.toString()}`);
    }, []);

    const display: ChatSessionDisplay = isFlow
        ? {
              name: flowDetail?.flowName || '多 Agent 编排',
              desc: flowDetail?.flowDesc,
              avatar: flowDetail?.avatar,
              welcome: flowDetail?.welcomeMessage,
          }
        : {
              name: agentDetail?.agentName || 'EasyAgent',
              desc: agentDetail?.agentDesc,
              avatar: agentDetail?.avatar,
              welcome: agentDetail?.welcomeMessage,
          };

    return {
        agentId,
        flowId,
        isFlow,
        agentDetail,
        flowDetail,
        quickPrompts,
        display,
        modelVersion: isFlow ? null : parseModelVersion(agentDetail?.modelConfig),
        modelIcon: isFlow ? undefined : agentDetail?.modelIcon,
        conversationId,
        setConversationId,
        syncSessionIdToUrl,
        clearSessionIdFromUrl,
    };
};
