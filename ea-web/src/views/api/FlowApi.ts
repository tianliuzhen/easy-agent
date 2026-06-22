const API_BASE_URL = 'http://localhost:8080';

export interface FlowNode {
  id?: number;
  flowId?: number;
  agentId: number;
  agentName?: string;
  avatar?: string;
  nodeRole?: string;
  orderIndex?: number;
}

export interface Flow {
  id?: number;
  flowName: string;
  flowKey?: string;
  strategy: string;
  supervisorAgentId?: number;
  avatar?: string;
  flowDesc?: string;
  prompt?: string;
  welcomeMessage?: string;
  createdAt?: string;
  updatedAt?: string;
  nodes?: FlowNode[];
}

// 多 Agent 编排（Flow）管理 API
export const flowApi = {
  // 编排列表（不含成员节点）
  list: async () => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/flow/list`, {
      method: 'GET',
      credentials: 'include',
    });
    return response.json();
  },

  // 编排详情（含有序成员节点）
  detail: async (id: number) => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/flow/detail/${id}`, {
      method: 'GET',
      credentials: 'include',
    });
    return response.json();
  },

  // 保存编排（id 为空新增，否则更新），成员节点全量替换
  save: async (flow: Flow) => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/flow/save`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify(flow),
    });
    return response.json();
  },

  // 删除编排及其成员节点
  remove: async (id: number) => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/flow/delete/${id}`, {
      method: 'DELETE',
      credentials: 'include',
    });
    return response.json();
  },
};
