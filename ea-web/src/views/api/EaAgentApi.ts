const API_BASE_URL = 'http://localhost:8080';
// 定义 API 请求函数
export const eaAgentApi = {

  /**
   获取大模型类型列表
   对应后端 ModelTypeEnum.getAll() 方法
   */
  queryChatModelTypeList: async ()=> {
    const response = await fetch(`${API_BASE_URL}/eaAgent/ai/queryChatModelTypeList`, {
      method: 'POST',
      credentials: 'include',
    });
    return response.json();
  },


  // 保存 Agent
  saveAgent: async (agent: any) => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/ai/saveAgent`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify(agent),
    });
    return response.json();
  },

  // 获取 Agent 列表
  listAgent: async (req: any = {}): Promise<any[]> => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/ai/listAgent`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify(req),
    });
    return response.json();
  },


  // 删除 Agent
  delAgent: async (agent: any) => {

    const response = await fetch(`${API_BASE_URL}/eaAgent/ai/delAgent`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify( agent ),
    });
    return response.json();
  },
};