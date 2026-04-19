import { message } from 'antd';

const API_BASE_URL = 'http://localhost:8080'; // 替换为你的后端地址

/**
 * 聊天会话数据结构
 */
export interface ChatConversation {
    id: number;
    title: string;
    agentId: number;
    userId: string;
    messageCount: number;
    lastMessageTime: string;
    status: string;
    createdAt: string;
    updatedAt: string;
    agentName?: string;
    agentAvatar?: string;
    lastMessagePreview?: string;
}

/**
 * 创建新的聊天会话
 * @param req 会话请求对象
 * @returns 创建的会话 ID
 */
export const createConversation = async (req: {
    title?: string;
    agentId: number;
    userId?: string;
    messageCount?: number;
    lastMessageTime?: string;
    status?: string;
}): Promise<number> => {
    try {
        const response = await fetch(`${API_BASE_URL}/chatRecord/conversation/create`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(req),
        });

        const result = await response.json();
        if (result.success) {
            return result.data;
        } else {
            throw new Error(result.message || '创建会话失败');
        }
    } catch (error) {
        console.error('创建会话失败:', error);
        throw error;
    }
};

/**
 * 更新会话信息
 * @param req 会话请求对象
 * @returns 更新结果
 */
export const updateConversation = async (req: {
    id: number;
    title?: string;
    agentId?: number;
    userId?: string;
    messageCount?: number;
    lastMessageTime?: string;
    status?: string;
}): Promise<number> => {
    try {
        const response = await fetch(`${API_BASE_URL}/chatRecord/conversation/update`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(req),
        });

        const result = await response.json();
        if (result.success) {
            return result.data;
        } else {
            throw new Error(result.message || '更新会话失败');
        }
    } catch (error) {
        console.error('更新会话失败:', error);
        throw error;
    }
};

/**
 * 删除会话（软删除）
 * @param conversationId 会话 ID
 * @returns 删除结果
 */
export const deleteConversation = async (conversationId: number): Promise<number> => {
    try {
        const response = await fetch(`${API_BASE_URL}/chatRecord/conversation/delete/${conversationId}`, {
            method: 'POST',
        });

        const result = await response.json();
        if (result.success) {
            return result.data;
        } else {
            throw new Error(result.message || '删除会话失败');
        }
    } catch (error) {
        console.error('删除会话失败:', error);
        throw error;
    }
};

/**
 * 归档会话
 * @param conversationId 会话 ID
 * @returns 归档结果
 */
export const archiveConversation = async (conversationId: number): Promise<number> => {
    try {
        const response = await fetch(`${API_BASE_URL}/chatRecord/conversation/archive/${conversationId}`, {
            method: 'POST',
        });

        const result = await response.json();
        if (result.success) {
            return result.data;
        } else {
            throw new Error(result.message || '归档会话失败');
        }
    } catch (error) {
        console.error('归档会话失败:', error);
        throw error;
    }
};

/**
 * 根据 ID 获取会话信息
 * @param conversationId 会话 ID
 * @returns 会话信息
 */
export const getConversationById = async (conversationId: number): Promise<ChatConversation> => {
    try {
        const response = await fetch(`${API_BASE_URL}/chatRecord/conversation/${conversationId}`);

        const result = await response.json();
        if (result.success) {
            return result.data;
        } else {
            throw new Error(result.message || '获取会话失败');
        }
    } catch (error) {
        console.error('获取会话失败:', error);
        throw error;
    }
};

/**
 * 根据 Agent ID 查询会话列表
 * @param agentId Agent ID
 * @param status 会话状态（可选）
 * @returns 会话列表
 */
export const listConversationsByAgentId = async (
    agentId: number,
    status?: string
): Promise<ChatConversation[]> => {
    try {
        const url = new URL(`${API_BASE_URL}/chatRecord/conversation/listByAgent/${agentId}`);
        if (status) {
            url.searchParams.append('status', status);
        }

        const response = await fetch(url.toString());

        const result = await response.json();
        if (result.success) {
            return result.data;
        } else {
            throw new Error(result.message || '查询会话列表失败');
        }
    } catch (error) {
        console.error('查询会话列表失败:', error);
        throw error;
    }
};

/**
 * 查询用户的会话列表
 * @param userId 用户 ID
 * @param agentId Agent ID（可选）
 * @param status 会话状态（可选）
 * @returns 会话列表
 */
export const listConversationsByUserId = async (
    userId: string,
    agentId?: number,
    status?: string
): Promise<ChatConversation[]> => {
    try {
        const url = new URL(`${API_BASE_URL}/chatRecord/conversation/listByUser/${userId}`);
        if (agentId) {
            url.searchParams.append('agentId', agentId.toString());
        }
        if (status) {
            url.searchParams.append('status', status);
        }

        const response = await fetch(url.toString());

        const result = await response.json();
        if (result.success) {
            return result.data;
        } else {
            throw new Error(result.message || '查询用户会话列表失败');
        }
    } catch (error) {
        console.error('查询用户会话列表失败:', error);
        throw error;
    }
};

/**
 * 开始新的聊天会话
 * @param agentId Agent ID
 * @param userId 用户 ID
 * @param firstQuestion 第一个问题
 * @returns 创建的会话 ID
 */
export const startNewConversation = async (
    agentId: number,
    userId: string,
    firstQuestion: string
): Promise<number> => {
    try {
        const url = new URL(`${API_BASE_URL}/chatRecord/business/startNewConversation`);
        url.searchParams.append('agentId', agentId.toString());
        url.searchParams.append('userId', userId);
        url.searchParams.append('firstQuestion', firstQuestion);

        const response = await fetch(url.toString(), {
            method: 'POST',
        });

        const result = await response.json();
        if (result.success) {
            return result.data;
        } else {
            throw new Error(result.message || '开始新会话失败');
        }
    } catch (error) {
        console.error('开始新会话失败:', error);
        throw error;
    }
};

/**
 * 获取会话的完整聊天记录
 * @param conversationId 会话 ID
 * @returns 完整的聊天记录
 */
export const getFullChatHistory = async (conversationId: number): Promise<any[]> => {
    try {
        const response = await fetch(`${API_BASE_URL}/chatRecord/business/fullChatHistory/${conversationId}`);

        const result = await response.json();
        if (result.success) {
            return result.data;
        } else {
            throw new Error(result.message || '获取完整聊天记录失败');
        }
    } catch (error) {
        console.error('获取完整聊天记录失败:', error);
        throw error;
    }
};

/**
 * Agent详情数据结构
 */
export interface AgentDetail {
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

/**
 * 根据ID查询Agent详情
 * @param agentId Agent ID
 * @returns Agent详情信息
 */
export const queryAgent = async (agentId: number): Promise<AgentDetail> => {
    try {
        const response = await fetch(`${API_BASE_URL}/eaAgent/ai/queryAgent`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ id: agentId }),
        });

        const result = await response.json();
        if (result.success) {
            return result.data;
        } else {
            throw new Error(result.message || '查询Agent详情失败');
        }
    } catch (error) {
        console.error('查询Agent详情失败:', error);
        throw error;
    }
};
