// MCP Server 配置相关类型接口

interface McpServerConfig {
  id?: number;
  serverName: string;
  serverUrl?: string;
  transportType: string;
  command?: string;
  envVars?: string[];
  toolName?: string;
  toolDisplayName?: string;
  toolDescription?: string;
  inputSchema?: string;
  outputSchema?: string;
  connectionTimeout?: number;
  maxRetries?: number;
  status?: string;
  description?: string;
  toolMetadata?: string;
  lastConnectedAt?: string;
  lastError?: string;
  createdAt?: string;
  updatedAt?: string;
}

interface McpToolInfo {
  name: string;
  description?: string;
  inputSchema?: any;
}

interface BaseResult {
  success: boolean;
  message?: string | null;
  data?: any;
}

const API_BASE_URL = 'http://localhost:8080';

// 定义 MCP 管理 API 请求函数
export const mcpApi = {

  /**
   * 获取所有 MCP Server 配置列表
   * @returns MCP Server 配置列表
   */
  listAllConfigs: async (): Promise<BaseResult> => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/mcp/server`, {
      method: 'GET',
    });
    return response.json();
  },

  /**
   * 根据 ID 查询 MCP Server 配置
   * @param id 配置 ID
   * @returns 配置信息
   */
  getConfigById: async (id: number): Promise<BaseResult> => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/mcp/server/${id}`, {
      method: 'GET',
    });
    return response.json();
  },

  /**
   * 根据服务器名称查询配置
   * @param serverName 服务器名称
   * @returns 配置列表
   */
  listConfigsByServerName: async (serverName: string): Promise<BaseResult> => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/mcp/server/name/${serverName}`, {
      method: 'GET',
    });
    return response.json();
  },

  /**
   * 新增 MCP Server 配置
   * @param config 配置对象
   * @returns 配置 ID
   */
  createConfig: async (config: McpServerConfig): Promise<BaseResult> => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/mcp/server`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(config),
    });
    return response.json();
  },

  /**
   * 更新 MCP Server 配置
   * @param id 配置 ID
   * @param config 配置对象
   * @returns 是否成功
   */
  updateConfig: async (id: number, config: McpServerConfig): Promise<BaseResult> => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/mcp/server/${id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(config),
    });
    return response.json();
  },

  /**
   * 删除 MCP Server 配置
   * @param id 配置 ID
   * @returns 是否成功
   */
  deleteConfig: async (id: number): Promise<BaseResult> => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/mcp/server/${id}`, {
      method: 'DELETE',
    });
    return response.json();
  },

  /**
   * 从 MCP Server 获取工具列表
   * @param id 配置 ID
   * @returns 可用工具列表
   */
  fetchToolsFromServer: async (id: number): Promise<BaseResult> => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/mcp/server/${id}/tools/fetch`, {
      method: 'POST',
    });
    return response.json();
  },

  /**
   * 直接获取 MCP Server 工具列表（不保存配置）
   * @param config 配置对象
   * @returns 可用工具列表
   */
  fetchToolsFromServerDirect: async (config: McpServerConfig): Promise<BaseResult> => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/mcp/server/tools/fetch`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(config),
    });
    return response.json();
  },

  /**
   * 获取指定 Server 的工具列表
   * @param id 配置 ID
   * @returns 工具列表
   */
  listTools: async (id: number): Promise<BaseResult> => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/mcp/server/${id}/tools`, {
      method: 'GET',
    });
    return response.json();
  },

  /**
   * 同步 MCP Server 的工具列表到本地配置
   * @param id 配置 ID
   * @returns 同步的工具数量
   */
  syncTools: async (id: number): Promise<BaseResult> => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/mcp/server/${id}/sync`, {
      method: 'POST',
    });
    return response.json();
  },

  /**
   * 根据用户ID获取MCP配置列表
   * @returns MCP配置结果列表
   */
  getMcpConfigByUserId: async (): Promise<BaseResult> => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/mcp/getMcpConfigByUserId/`, {
      method: 'GET',
    });
    return response.json();
  },

  /**
   * 获取官方MCP配置列表（user_id = 0）
   * @returns 官方MCP配置结果列表
   */
  getOfficialMcpConfigs: async (): Promise<BaseResult> => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/mcp/getOfficialMcpConfigs/`, {
      method: 'GET',
    });
    return response.json();
  },

  /**
   * 根据Agent ID获取已绑定的MCP列表
   * @param agentId Agent标识
   * @returns 已绑定的MCP配置列表
   */
  listBoundMcpByAgentId: async (agentId: string): Promise<BaseResult> => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/mcp/listBoundMcpByAgentId/${agentId}`, {
      method: 'GET',
    });
    return response.json();
  },

  /**
   * 绑定MCP到Agent
   * @param request MCP绑定请求
   * @returns 绑定结果
   */
  bindMcp: async (request: { agentId: string; mcpConfigId: number; mcpName?: string; creator?: string; bindingConfig?: string }): Promise<BaseResult> => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/mcp/bind`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    });
    return response.json();
  },

  /**
   * 从Agent解绑MCP
   * @param request MCP解绑请求
   * @returns 解绑结果
   */
  unbindMcp: async (request: { agentId: string; mcpConfigId: number }): Promise<BaseResult> => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/mcp/unbind`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    });
    return response.json();
  },
};
