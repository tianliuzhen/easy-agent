// Skill 技能配置相关类型接口

interface SkillConfig {
  id?: number;
  skillName: string;
  skillDisplayName?: string;
  skillDescription?: string;
  skillType: string;
  skillCategory?: string;
  skillIcon?: string;
  skillVersion?: string;
  skillProvider?: string;
  skillCapabilities?: string[];
  inputSchema?: string;
  outputSchema?: string;
  skillMetadata?: string;
  skillConfig?: string;
  executionMode?: string;
  timeout?: number;
  maxRetries?: number;
  status?: string;
  lastExecutedAt?: string;
  lastError?: string;
  createdAt?: string;
  updatedAt?: string;
}

interface BaseResult {
  success: boolean;
  message?: string | null;
  data?: any;
}

const API_BASE_URL = 'http://localhost:8080';

// 定义 Skill 管理 API 请求函数
export const skillApi = {

  /**
   * 获取所有 Skill 配置列表
   * @returns Skill 配置列表
   */
  listAllConfigs: async (): Promise<BaseResult> => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/skill/server`, {
      method: 'GET',
    });
    return response.json();
  },

  /**
   * 根据 ID 查询 Skill 配置
   * @param id 配置 ID
   * @returns 配置信息
   */
  getConfigById: async (id: number): Promise<BaseResult> => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/skill/server/${id}`, {
      method: 'GET',
    });
    return response.json();
  },

  /**
   * 新增 Skill 配置
   * @param config 配置对象
   * @returns 配置 ID
   */
  createConfig: async (config: SkillConfig): Promise<BaseResult> => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/skill/server`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(config),
    });
    return response.json();
  },

  /**
   * 更新 Skill 配置
   * @param id 配置 ID
   * @param config 配置对象
   * @returns 是否成功
   */
  updateConfig: async (id: number, config: SkillConfig): Promise<BaseResult> => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/skill/server/${id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(config),
    });
    return response.json();
  },

  /**
   * 删除 Skill 配置
   * @param id 配置 ID
   * @returns 是否成功
   */
  deleteConfig: async (id: number): Promise<BaseResult> => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/skill/server/${id}`, {
      method: 'DELETE',
    });
    return response.json();
  },

  /**
   * 安装 Skill（从市场安装到"我的 Skill"）
   * @param skillConfigId Skill 配置 ID
   * @param customConfig 用户自定义配置（可选）
   * @returns 安装结果
   */
  installSkill: async (skillConfigId: number, customConfig?: string): Promise<BaseResult> => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/skill/install`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        skillConfigId,
        customConfig: customConfig || '{}'
      }),
    });
    return response.json();
  },

  /**
   * 卸载 Skill
   * @param skillConfigId Skill 配置 ID
   * @returns 卸载结果
   */
  uninstallSkill: async (skillConfigId: number): Promise<BaseResult> => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/skill/uninstall/${skillConfigId}`, {
      method: 'POST',
    });
    return response.json();
  },

  /**
   * 检查用户是否已安装指定 Skill
   * @param skillName Skill 名称
   * @returns 是否已安装
   */
  isSkillInstalled: async (skillName: string): Promise<BaseResult> => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/skill/isInstalled/${skillName}`, {
      method: 'GET',
    });
    return response.json();
  },

  /**
   * 根据用户ID获取 Skill 配置列表
   * @returns Skill 配置结果列表
   */
  getSkillConfigByUserId: async (): Promise<BaseResult> => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/skill/getSkillConfigByUserId/`, {
      method: 'GET',
    });
    return response.json();
  },

  /**
   * 获取官方 Skill 配置列表（user_id = 0）
   * @returns 官方 Skill 配置结果列表
   */
  getOfficialSkills: async (): Promise<BaseResult> => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/skill/getOfficialSkills/`, {
      method: 'GET',
    });
    return response.json();
  },

  /**
   * 根据分类获取 Skill 列表
   * @param category 技能分类
   * @returns Skill 配置结果列表
   */
  getSkillsByCategory: async (category: string): Promise<BaseResult> => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/skill/getSkillsByCategory/${category}`, {
      method: 'GET',
    });
    return response.json();
  },

  /**
   * 根据 Agent ID 获取已绑定的 Skill 列表
   * @param agentId Agent 标识
   * @returns 已绑定的 Skill 配置列表
   */
  listBoundSkillsByAgentId: async (agentId: string): Promise<BaseResult> => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/skill/listBoundSkillsByAgentId/${agentId}`, {
      method: 'GET',
    });
    return response.json();
  },

  /**
   * 绑定 Skill 到 Agent
   * @param request Skill 绑定请求
   * @returns 绑定结果
   */
  bindSkill: async (request: { agentId: string; skillConfigId: number; bindingConfig?: string }): Promise<BaseResult> => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/skill/bind`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    });
    return response.json();
  },

  /**
   * 从 Agent 解绑 Skill
   * @param request Skill 解绑请求
   * @returns 解绑结果
   */
  unbindSkill: async (request: { agentId: string; skillConfigId: number }): Promise<BaseResult> => {
    const response = await fetch(`${API_BASE_URL}/eaAgent/skill/unbind`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    });
    return response.json();
  },
};