const API_BASE_URL = 'http://localhost:8080';

// 定义后端返回的统一格式
interface BaseResult<T> {
    success: boolean;
    data: T;
    message: string;
}

export const knowledgeBaseApi = {
    /**
     * 上传文档
     */
    upload: async (kbName: string, kbDesc: string, file: File): Promise<BaseResult<any>> => {
        const formData = new FormData();
        formData.append('kbName', kbName);
        formData.append('kbDesc', kbDesc);
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
