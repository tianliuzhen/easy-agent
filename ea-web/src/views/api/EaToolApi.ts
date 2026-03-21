// 定义工具配置相关类型接口
interface EaToolConfig {
  id?: number;
  agentId?: number;
  toolType: string;
  toolInstanceId?: string;
  toolInstanceName?: string;
  inputTemplate?: string;
  outTemplate?: string;
  isRequired?: boolean;
  sortOrder?: number;
  isActive?: boolean;
  createdAt?: Date;
  updatedAt?: Date;
  toolValue?: string;
  extraConfig?: string;
}

interface EaToolConfigReq extends EaToolConfig {}

interface EaToolConfigResult extends EaToolConfig {}

interface BaseResult {
  success: boolean;
  message?: string | null;
  data?: any;
}

// 工具绑定请求
interface ToolBindRequest {
  agentId: string;
  toolConfigId: number;
  toolName?: string;
  creator?: string;
}

// 工具解绑请求
interface ToolUnbindRequest {
  agentId: string;
  toolConfigId: number;
}

const API_BASE_URL = 'http://localhost:8080';

// 定义工具管理 API 请求函数
export const eaToolApi = {

  /**
   * 根据 Agent ID 获取工具配置列表
   * @param agentId Agent 标识
   * @returns 工具配置结果列表
   */
  getToolConfigByAgentId: async (agentId: number | string): Promise<BaseResult> => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/tool/getToolConfigByAgentId/${agentId}`, {
      method: 'GET',
      // headers: {
      //   'Content-Type': 'application/json',
      // },
    });
    return response.json();
  },

  /**
   * 获取所有工具配置列表（不限制 Agent）
   * @returns 工具配置结果列表
   */
  getDefaultTools: async (): Promise<BaseResult> => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/tool/getDefaultTools`, {
      method: 'GET',
    });
    return response.json();
  },

  /**
   * 删除工具配置
   * @param toolConfig 工具配置请求对象
   * @returns 删除结果
   */
  delTool: async (toolConfig: EaToolConfigReq): Promise<BaseResult> => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/tool/delTool`, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(toolConfig),
    });
    return response.json();
  },

  /**
   * 添加工具配置
   * @param toolConfig 工具配置请求对象
   * @returns 添加结果
   */
  addTool: async (toolConfig: EaToolConfigReq): Promise<BaseResult> => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/tool/addTool`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(toolConfig),
    });
    return response.json();
  },

  /**
   * 调试工具配置
   * @param toolConfig 工具配置请求对象
   * @returns 调试结果
   */
  debug: async (toolConfig: EaToolConfigReq): Promise<BaseResult> => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/tool/debug`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(toolConfig),
    });
    return response.json();
  },

  /**
   * 复制工具配置
   * @param toolConfig 工具配置请求对象
   * @returns 复制结果
   */
  copyTool: async (toolConfig: EaToolConfigReq): Promise<BaseResult> => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/tool/copyTool`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(toolConfig),
    });
    return response.json();
  },

  /**
   * 根据Agent ID获取已绑定的工具列表
   * @param agentId Agent标识
   * @returns 已绑定的工具配置列表
   */
  listBoundToolsByAgentId: async (agentId: string): Promise<BaseResult> => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/tool/listBoundToolsByAgentId/${agentId}`, {
      method: 'GET',
    });
    return response.json();
  },

  /**
   * 绑定工具到Agent
   * @param request 工具绑定请求
   * @returns 绑定结果
   */
  bindTool: async (request: ToolBindRequest): Promise<BaseResult> => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/tool/bind`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    });
    return response.json();
  },

  /**
   * 从Agent解绑工具
   * @param request 工具解绑请求
   * @returns 解绑结果
   */
  unbindTool: async (request: ToolUnbindRequest): Promise<BaseResult> => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/tool/unbind`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    });
    return response.json();
  },
};
