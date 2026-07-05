const API_BASE_URL = 'http://localhost:8080';

// 后端统一返回格式
interface BaseResult<T> {
    success: boolean;
    data: T;
    message: string;
}

// 图片记录 / 搜索结果
export interface ImageItem {
    id: string;
    name: string;
    ext: string;
    score?: number;
    imageUrl: string;
}

/**
 * 把后端返回的 imageUrl 解析成可访问地址：
 * - 相对路径（local 存储，以 / 开头）→ 前缀 API_BASE_URL
 * - 绝对 URL（未来 OSS）→ 原样返回
 */
export const resolveImageUrl = (imageUrl?: string): string => {
    if (!imageUrl) return '';
    if (imageUrl.startsWith('http://') || imageUrl.startsWith('https://')) return imageUrl;
    return `${API_BASE_URL}${imageUrl}`;
};

export const imageSearchApi = {
    /** 上传图片入库 */
    upload: async (file: File, name?: string): Promise<BaseResult<ImageItem>> => {
        const formData = new FormData();
        formData.append('file', file);
        const query = name ? `?name=${encodeURIComponent(name)}` : '';
        const response = await fetch(`${API_BASE_URL}/imageSearch/upload${query}`, {
            method: 'POST',
            body: formData,
        });
        return response.json();
    },

    /** 以图搜图 */
    search: async (file: File, topK: number = 10): Promise<BaseResult<ImageItem[]>> => {
        const formData = new FormData();
        formData.append('file', file);
        const response = await fetch(`${API_BASE_URL}/imageSearch/search?topK=${topK}`, {
            method: 'POST',
            body: formData,
        });
        return response.json();
    },

    /** 已入库图片列表 */
    list: async (): Promise<BaseResult<ImageItem[]>> => {
        const response = await fetch(`${API_BASE_URL}/imageSearch/list`, {
            method: 'GET',
        });
        return response.json();
    },

    /** 删除 */
    delete: async (id: string): Promise<BaseResult<void>> => {
        const response = await fetch(`${API_BASE_URL}/imageSearch/${id}`, {
            method: 'DELETE',
        });
        return response.json();
    },
};
