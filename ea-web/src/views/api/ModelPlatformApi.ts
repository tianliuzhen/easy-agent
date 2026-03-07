const API_BASE_URL = 'http://localhost:8080';

// 模型平台配置 API
export const modelPlatformApi = {
  /**
   * 查询所有模型平台列表
   */
  list: async () => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/modelPlatform/list`, {
      method: 'POST',
    });
    return response.json();
  },

  /**
   * 根据 ID 查询模型平台详情
   */
  getById: async (id: number) => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/modelPlatform/getById`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ id }),
    });
    return response.json();
  },

  /**
   * 保存模型平台配置 (新增或更新)
   */
  save: async (platform: any) => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/modelPlatform/save`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(platform),
    });
    return response.json();
  },

  /**
   * 删除模型平台配置
   */
  delete: async (id: number) => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/modelPlatform/delete`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ id }),
    });
    return response.json();
  },

  /**
   * 更新模型平台启用状态
   */
  updateActiveStatus: async (id: number, isActive: boolean) => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/modelPlatform/updateActiveStatus`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ id, isActive }),
    });
    return response.json();
  },
};
