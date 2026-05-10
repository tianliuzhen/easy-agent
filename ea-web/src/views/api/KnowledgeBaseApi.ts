const API_BASE_URL = 'http://localhost:8080';

// 定义后端返回的统一格式
interface BaseResult<T> {
    success: boolean;
    data: T;
    message: string;
}

// 知识库查询请求
interface KnowledgeBaseQueryRequest {
    agentId?: string;
    kbName?: string;
    type?: string;
    status?: string;
    pageNum?: number;
    pageSize?: number;
}

// 知识库绑定请求
interface KnowledgeBaseBindRequest {
    agentId: string;
    knowledgeBaseId: number;
    kbName?: string;
    creator?: string;
}

// 知识库解绑请求
interface KnowledgeBaseUnbindRequest {
    agentId: string;
    knowledgeBaseId: number;
}

// 知识库上传请求
interface KnowledgeBaseUploadRequest {
    agentId: string;
    kbName: string;
    kbDesc: string;
    file: File;
}

export const knowledgeBaseApi = {
    /**
     * 上传文档
     */
    upload: async (agentId: string, kbName: string, kbDesc: string, file: File): Promise<BaseResult<any>> => {
        const formData = new FormData();
        // 将元数据作为JSON字符串放入request部分
        const request = {
            agentId,
            kbName,
            kbDesc
        };
        formData.append('request', new Blob([JSON.stringify(request)], { type: 'application/json' }));
        formData.append('file', file);

        const response = await fetch(`${API_BASE_URL}/knowledge/upload`, {
            method: 'POST',
            body: formData,
        });
        return response.json();
    },

    /**
     * 查询知识库列表
     */
    list: async (): Promise<BaseResult<any[]>> => {
        const response = await fetch(`${API_BASE_URL}/knowledge/list`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
        });
        return response.json();
    },

    /**
     * 根据Agent ID查询知识库列表
     */
    listByAgentId: async (agentId: string): Promise<BaseResult<any[]>> => {
        const response = await fetch(`${API_BASE_URL}/knowledge/listByAgentId?agentId=${agentId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
        });
        return response.json();
    },

    /**
     * 根据条件查询知识库列表（使用Request对象）
     */
    listByCondition: async (request: KnowledgeBaseQueryRequest): Promise<BaseResult<any[]>> => {
        const response = await fetch(`${API_BASE_URL}/knowledge/listByCondition`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(request),
        });
        return response.json();
    },

    /**
     * 绑定知识库到Agent
     */
    bind: async (request: KnowledgeBaseBindRequest): Promise<BaseResult<void>> => {
        const response = await fetch(`${API_BASE_URL}/knowledge/bind`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(request),
        });
        return response.json();
    },

    /**
     * 从Agent解绑知识库
     */
    unbind: async (request: KnowledgeBaseUnbindRequest): Promise<BaseResult<void>> => {
        const response = await fetch(`${API_BASE_URL}/knowledge/unbind`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(request),
        });
        return response.json();
    },

    /**
     * 删除知识库
     */
    delete: async (id: number): Promise<BaseResult<string>> => {
        const response = await fetch(`${API_BASE_URL}/knowledge/delete`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({id}),
        });
        return response.json();
    },

    /**
     * 搜索知识
     */
    search: async (query: string, topK: number = 5): Promise<BaseResult<string[]>> => {
        const response = await fetch(`${API_BASE_URL}/knowledge/search`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({query, topK}),
        });
        return response.json();
    },
};