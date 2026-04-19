import React, { createContext, useContext, useState, useCallback, useEffect } from 'react';
import {eaAgentApi} from '../../api/EaAgentApi';

interface AgentDetail {
    id: number;
    agentName: string;
    agentKey: string;
    modelPlatform: string;
    analysisModel: string;
    toolModel: string;
    toolRunMode: string;
    modelConfig: string;
    avatar: string;
    agentDesc: string;
    createdAt: string;
    updatedAt: string;
    modelIcon?: string;
}

interface AgentConfigContextType {
    agentId: number | null;
    setAgentId: (id: number | null) => void;
    refreshResources: () => void;
    refreshKey: number;
    promptContent: string;
    setPromptContent: (content: string) => void;
    agentDetail: AgentDetail | null;
    setAgentDetail: (detail: AgentDetail | null) => void;
    loadingAgentDetail: boolean;
    loadAgentDetail: () => Promise<void>;
}

const AgentConfigContext = createContext<AgentConfigContextType | undefined>(undefined);

interface AgentConfigProviderProps {
    children: React.ReactNode;
    initialAgentId?: number;
}

export const AgentConfigProvider: React.FC<AgentConfigProviderProps> = ({
                                                                            children,
                                                                            initialAgentId
                                                                        }) => {
    const [agentId, setAgentId] = useState<number | null>(initialAgentId || null);
    const [refreshKey, setRefreshKey] = useState(0);
    const [promptContent, setPromptContent] = useState('');
    const [agentDetail, setAgentDetail] = useState<AgentDetail | null>(null);
    const [loadingAgentDetail, setLoadingAgentDetail] = useState(false);

    // 当 agentId 变化时重新加载 Agent 详情
    useEffect(() => {
        if (agentId) {
            setLoadingAgentDetail(true);
            eaAgentApi.queryAgent(agentId)
                .then(result => {
                    if (result && result.data) {
                        setAgentDetail(result.data);
                    }
                })
                .catch(error => {
                    console.error('加载 Agent 详情失败:', error);
                    setAgentDetail(null);
                })
                .finally(() => {
                    setLoadingAgentDetail(false);
                });
        } else {
            setAgentDetail(null);
            setLoadingAgentDetail(false);
        }
    }, [agentId]);

    // 刷新所有资源
    const refreshResources = useCallback(() => {
        setRefreshKey(prev => prev + 1);
        console.log('刷新所有资源，当前key:', refreshKey + 1);
    }, [refreshKey]);

    const value: AgentConfigContextType = {
        agentId,
        setAgentId,
        refreshResources,
        refreshKey,
        promptContent,
        setPromptContent,
        agentDetail,
        setAgentDetail,
        loadingAgentDetail,
        loadAgentDetail: () => {} // 空函数，保持接口兼容
    };

    return (
        <AgentConfigContext.Provider value={value}>
            {children}
        </AgentConfigContext.Provider>
    );
};

// 自定义 hook 使用 Context
export const useAgentConfig = (): AgentConfigContextType => {
    const context = useContext(AgentConfigContext);
    if (context === undefined) {
        throw new Error('useAgentConfig 必须在 AgentConfigProvider 内使用');
    }
    return context;
};

// 工具函数：从URL获取agentId
export const getAgentIdFromUrl = (): number | null => {
    if (typeof window === 'undefined') return null;

    const urlParams = new URLSearchParams(window.location.search);
    const agentId = urlParams.get('agentId');
    return agentId ? parseInt(agentId) : null;
};

// 工具函数：更新URL中的agentId
export const updateAgentIdInUrl = (agentId: number | null) => {
    if (typeof window === 'undefined') return;

    const urlParams = new URLSearchParams(window.location.search);
    if (agentId) {
        urlParams.set('agentId', agentId.toString());
    } else {
        urlParams.delete('agentId');
    }

    const newUrl = `${window.location.pathname}?${urlParams.toString()}`;
    window.history.replaceState({}, '', newUrl);
};

export default AgentConfigContext;
