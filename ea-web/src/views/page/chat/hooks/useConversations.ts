import {useCallback, useEffect, useState} from 'react';
import {message} from 'antd';
import {
    listConversationsByUserId,
    deleteConversation,
    type ChatConversation,
} from '../../../api/ChatConversationApi';

export interface UseConversations {
    conversations: ChatConversation[];
    loading: boolean;
    selectedChatIndex: number;
    setSelectedChatIndex: (index: number) => void;
    reload: () => Promise<ChatConversation[]>;
    /** 删除指定下标会话，返回删除后的最新列表 */
    remove: (index: number) => Promise<ChatConversation[]>;
}

interface UseConversationsParams {
    userId: string;
    agentId: number;
    flowId?: number;
}

/**
 * 管理会话列表：按 flowId（编排）或 agentId（单 Agent）过滤加载，支持刷新与删除。
 * agentId/flowId 变化时自动重载。
 */
export const useConversations = ({userId, agentId, flowId}: UseConversationsParams): UseConversations => {
    const [conversations, setConversations] = useState<ChatConversation[]>([]);
    const [loading, setLoading] = useState(false);
    const [selectedChatIndex, setSelectedChatIndex] = useState(-1);

    const reload = useCallback(async (): Promise<ChatConversation[]> => {
        try {
            setLoading(true);
            // 编排模式按 flowId 过滤，单 Agent 模式按 agentId 过滤
            const list = flowId
                ? await listConversationsByUserId(userId, undefined, flowId, 'active')
                : await listConversationsByUserId(userId, agentId, undefined, 'active');
            setConversations(list);
            return list;
        } catch (error) {
            console.error('加载会话列表失败:', error);
            message.error('加载会话列表失败');
            return [];
        } finally {
            setLoading(false);
        }
    }, [userId, agentId, flowId]);

    useEffect(() => {
        reload();
    }, [reload]);

    const remove = useCallback(
        async (index: number): Promise<ChatConversation[]> => {
            const target = conversations[index];
            if (!target || conversations.length <= 1) return conversations;
            try {
                await deleteConversation(target.id);
                return await reload();
            } catch (error) {
                console.error('删除会话失败:', error);
                message.error('删除会话失败');
                return conversations;
            }
        },
        [conversations, reload]
    );

    return {conversations, loading, selectedChatIndex, setSelectedChatIndex, reload, remove};
};
