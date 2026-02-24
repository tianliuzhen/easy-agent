import React, { useState, useEffect } from 'react';
import { App, Layout, Menu, Typography, Breadcrumb, Button, Space, Tag, Popconfirm, ConfigProvider, Input, Modal, Tooltip, Tabs } from 'antd';
import { 
  DatabaseOutlined, 
  ApiOutlined, 
  CloudServerOutlined, 
  SyncOutlined,
  EditOutlined,
  DeleteOutlined,
  CopyOutlined,
  BookOutlined,
  MessageOutlined,
  CommentOutlined
} from '@ant-design/icons';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { eaToolApi } from '../api/EaToolApi';
import { eaAgentApi } from '../api/EaAgentApi';
import SQLConfig from './agent/tool/SQLConfig';
import HTTPConfig from './agent/tool/HTTPConfig';
import MCPConfig from './agent/tool/MCPConfig';
import GRPCConfig from './agent/tool/GRPCConfig';
import KnowledgeBaseConfig from './agent/knowledge/KnowledgeBaseConfig';
import PromptConfig from './agent/prompt/PromptConfig';
import FeedbackConfig from './agent/feedback/FeedbackConfig';

const { Sider, Content } = Layout;
const { Title } = Typography;

// 自定义带动画效果的图标组件
const AnimatedDatabaseIcon = ({ isActive }: { isActive: boolean }) => (
  <DatabaseOutlined 
    style={{ 
      color: '#4CAF50',
      fontSize: isActive ? '16px' : '14px',
      transition: 'all 0.3s ease',
      animation: !isActive ? 'swing 2.8s ease-in-out infinite' : 'none',
      transform: 'rotate(0deg)',
      transformOrigin: 'center center'
    }} 
  />
);

const AnimatedApiIcon = ({ isActive }: { isActive: boolean }) => (
  <ApiOutlined 
    style={{ 
      color: '#2196F3',
      fontSize: isActive ? '16px' : '14px',
      transition: 'all 0.3s ease',
      animation: !isActive ? 'swing 3s ease-in-out infinite' : 'none',
      transform: 'rotate(0deg)',
      transformOrigin: 'center center'
    }} 
  />
);

const AnimatedCloudServerIcon = ({ isActive }: { isActive: boolean }) => (
  <CloudServerOutlined 
    style={{ 
      color: '#FF9800',
      fontSize: isActive ? '16px' : '14px',
      transition: 'all 0.3s ease',
      animation: !isActive ? 'swing 2s ease-in-out infinite' : 'none',
      transform: 'rotate(0deg)',
      transformOrigin: 'center center'
    }} 
  />
);

const AnimatedSyncIcon = ({ isActive }: { isActive: boolean }) => (
  <SyncOutlined 
    style={{ 
      color: '#9C27B0',
      fontSize: isActive ? '16px' : '14px',
      transition: 'all 0.3s ease',
      animation: !isActive ? 'spin 2.5s linear infinite' : 'none',
      transform: 'rotate(0deg)',
    }} 
  />
);

// 添加全局CSS动画关键帧
const GlobalStyles = () => (
  <style>
    {`
      @keyframes swing {
        0% {
          transform: rotate(-60deg);
        }
        50% {
          transform: rotate(60deg);
        }
        100% {
          transform: rotate(-60deg);
        }
      }
      
      @keyframes spin {
        from {
          transform: rotate(0deg);
        }
        to {
          transform: rotate(360deg);
        }
      }
      
      .breadcrumb-container {
        padding: 16px 24px 0 24px;
        background: #fff;
      }
    `}
  </style>
);

const AgentConfig: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const app = App.useApp();
  
  // 获取URL参数中的agentId
  const urlParams = new URLSearchParams(location.search);
  const agentId = urlParams.get('agentId');
  
  // 获取agent信息，用于显示在左侧菜单顶部
  const [agentInfo, setAgentInfo] = useState<any>(null);

  // 添加activeTab状态用于控制标签页
  const [activeTab, setActiveTab] = useState<string>('tools');

  // 解析当前选中的工具
  const getCurrentTool = () => {
    const path = location.pathname;
    if (path.includes('/tool/sql')) return 'SQL';
    if (path.includes('/tool/http')) return 'HTTP';
    if (path.includes('/tool/mcp')) return 'MCP';
    if (path.includes('/tool/grpc')) return 'GRPC';
    return 'SQL'; // 默认选中SQL
  };

  // 将工具类型转换为中文名称
  const getToolTypeName = (toolType: string) => {
    switch(toolType) {
      case 'SQL':
        return 'SQL执行器';
      case 'HTTP':
        return 'HTTP请求';
      case 'MCP':
        return 'MCP服务器';
      case 'GRPC':
        return 'gRPC 工具';
      default:
        return toolType;
    }
  };

  // 根据URL参数获取当前选中的工具键值
  const getCurrentToolKey = () => {
    const urlParams = new URLSearchParams(location.search);
    const toolIdFromUrl = urlParams.get('toolId');
    
    if (toolIdFromUrl && toolIdFromUrl !== 'new') {
      const toolType = getCurrentTool();
      return `${toolType}_${toolIdFromUrl}`;
    }
    
    return '';
  };

  const [selectedTool, setSelectedTool] = useState<string>('');
  const [availableTools, setAvailableTools] = useState<{ key: string; label: string; icon: React.ReactNode; disabled?: boolean }[]>([]);
  const [toolConfigs, setToolConfigs] = useState<any[]>([]);
  
  // 获取agent的工具配置（用于设置toolConfigs和availableTools状态）
  const loadToolConfigs = () => {
    if (agentId) {
      eaToolApi.getToolConfigByAgentId(parseInt(agentId))
        .then((result) => {
          if (result && (result.code === 200 || result.success === true)) {
            const toolConfigs = result.data || [];
            setToolConfigs(toolConfigs);
            
            // 同时更新左侧工具列表
            // 使用完整工具配置，不只基于toolType去重，保留每个具体的工具配置
            const toolsWithDetails = toolConfigs.map((config: any, index: number) => {
              // 禁用MCP和GRPC工具
              const isDisabled = config.toolType === 'MCP' || config.toolType === 'GRPC';
              
              // 生成工具显示名称，优先使用toolInstanceName，如果没有则使用对应的中文名称，再加上ID以便区分
              let toolDisplayName = config.toolInstanceName;
              if (!toolDisplayName) {
                toolDisplayName = getToolTypeName(config.toolType);
              }
              const displayLabel = (
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', width: '100%' }}>
                  <Tooltip title={toolDisplayName} color="black" placement="right">
                    <div style={{ flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', marginRight: '8px' }}>
                      {toolDisplayName}
                      <Tag color="blue" style={{ marginLeft: '8px' }}>{config.id}</Tag>
                    </div>
                  </Tooltip>
                  <div style={{ display: 'flex', gap: '4px', alignItems: 'center', flexShrink: 0 }}>
                    <Button
                      type="text"
                      size="small"
                      icon={<EditOutlined style={{ color: '#1890ff' }} />}
                      onClick={(e) => {
                        e.stopPropagation(); // 阻止事件冒泡到菜单项
                        handleEditTool(config);
                      }}
                      style={{ padding: '0 4px' }}
                    />
                    <Button
                      type="text"
                      size="small"
                      icon={<CopyOutlined />}
                      onClick={(e) => {
                        e.stopPropagation(); // 阻止事件冒泡到菜单项
                        showCopyConfirm(config);
                      }}
                      style={{ padding: '0 4px' }}
                    />
                    <Popconfirm
                      title="确认删除工具?"
                      description="此操作将永久删除该工具配置，是否继续?"
                      onConfirm={(e) => {
                        e?.stopPropagation(); // 阻止事件冒泡到菜单项
                        handleDeleteTool(config);
                      }}
                      okText="确认"
                      cancelText="取消"
                    >
                      <Button
                        type="text"
                        size="small"
                        danger
                        icon={<DeleteOutlined />}
                        onClick={(e) => e.stopPropagation()} // 阻止事件冒泡到菜单项
                        style={{ padding: '0 4px' }}
                      />
                    </Popconfirm>
                  </div>
                </div>
              );
              
              switch (config.toolType) {
                case 'SQL':
                  // 使用id作为唯一标识，而不是只用toolType
                  return { 
                    key: `${config.toolType}_${config.id}`, 
                    label: displayLabel, 
                    icon: <AnimatedDatabaseIcon isActive={selectedTool.startsWith(config.toolType)} />, 
                    disabled: false 
                  };
                case 'HTTP':
                  return { 
                    key: `${config.toolType}_${config.id}`, 
                    label: displayLabel, 
                    icon: <AnimatedApiIcon isActive={selectedTool.startsWith(config.toolType)} />, 
                    disabled: false 
                  };
                case 'MCP':
                  return { 
                    key: `${config.toolType}_${config.id}`, 
                    label: displayLabel, 
                    icon: <AnimatedCloudServerIcon isActive={selectedTool.startsWith(config.toolType)} />, 
                    disabled: true 
                  };
                case 'GRPC':
                  return { 
                    key: `${config.toolType}_${config.id}`, 
                    label: displayLabel, 
                    icon: <AnimatedSyncIcon isActive={selectedTool.startsWith(config.toolType)} />, 
                    disabled: true 
                  };
                default:
                  return { 
                    key: `${config.toolType}_${config.id}`, 
                    label: displayLabel, 
                    icon: null, 
                    disabled: isDisabled 
                  };
              }
            });
            setAvailableTools(toolsWithDetails);
            console.log('Loaded tool configs for agent:', agentId, result.data);
            
            // 如果有可用工具，尝试根据URL参数设置选中的工具，否则选择第一个可用工具
            const currentToolKey = getCurrentToolKey();
            if (currentToolKey && toolsWithDetails.some(tool => tool.key === currentToolKey)) {
              setSelectedTool(currentToolKey);
            } else if (toolsWithDetails.length > 0) {
              setSelectedTool(toolsWithDetails[0].key);
            } else {
              // 如果没有可用工具，清空选中状态
              setSelectedTool('');
            }
          } else {
            console.error('Failed to load tool configs:', result?.message || 'Request failed');
            setToolConfigs([]);
            // 接口失败时清空工具列表
            setAvailableTools([]);
            setSelectedTool('');
          }
        })
        .catch((error) => {
          console.error('Error loading tool configs:', error);
          setToolConfigs([]);
          // 接口错误时清空工具列表
          setAvailableTools([]);
          setSelectedTool('');
        });
    }
  };
  
  // 处理标签页切换和URL参数
  useEffect(() => {
    // 根据URL参数设置初始标签页
    const urlParams = new URLSearchParams(location.search);
    const tabFromUrl = urlParams.get('tab');
    
    if (tabFromUrl && ['knowledge', 'prompt', 'tools', 'feedback'].includes(tabFromUrl)) {
      setActiveTab(tabFromUrl);
    } else {
      // 默认显示工具标签页
      setActiveTab('tools');
    }
  }, [location.search]);

  // 合并两个useEffect，统一管理工具配置加载
  useEffect(() => {
    loadToolConfigs();
    
    // 根据URL参数设置当前选中的工具
    const currentToolKey = getCurrentToolKey();
    if (currentToolKey) {
      setSelectedTool(currentToolKey);
    }
  }, [agentId, location.search]); // 移除了selectedTool依赖，避免循环调用

  // 获取agent的工具配置
  useEffect(() => {
    if (agentId) {
      eaAgentApi.listAgent({}).then(result => {
        if (result && result.data) {
          const agent = result.data.find((a: any) => a.id === parseInt(agentId));
          if (agent) {
            setAgentInfo(agent);
          }
        }
      }).catch(error => {
        console.error('获取Agent信息失败:', error);
      });
    }
  }, [agentId]);

  // 编辑工具处理函数
  const handleEditTool = (config: any) => {
    // 根据工具类型导航到对应的编辑页面
    switch (config.toolType) {
      case 'SQL':
        navigate(`/pageTool/AgentConfig/tool/sql${agentId ? `?agentId=${agentId}&toolId=${config.id}` : ''}`);
        break;
      case 'HTTP':
        navigate(`/pageTool/AgentConfig/tool/http${agentId ? `?agentId=${agentId}&toolId=${config.id}` : ''}`);
        break;
      case 'MCP':
        navigate(`/pageTool/AgentConfig/tool/mcp${agentId ? `?agentId=${agentId}&toolId=${config.id}` : ''}`);
        break;
      case 'GRPC':
        navigate(`/pageTool/AgentConfig/tool/grpc${agentId ? `?agentId=${agentId}&toolId=${config.id}` : ''}`);
        break;
      default:
        break;
    }
  };

  // 删除工具处理函数
  const handleDeleteTool = (config: any) => {
    eaToolApi.delTool(config)
      .then((result) => {
        if (result && (result.code === 200 || result.success === true)) {
          console.log('工具删除成功:', config.id);
          // 重新加载工具配置列表
          loadToolConfigs();
        } else {
          console.error('工具删除失败:', result?.message || 'Request failed');
        }
      })
      .catch((error) => {
        console.error('删除工具时出错:', error);
      });
  };

  // 显示复制确认对话框
  const showCopyConfirm = (config: any) => {
    const modal = app.modal;
    
    const modalInstance = modal.confirm({
      title: '确认复制工具',
      content: (
        <div>
          <p>此操作将复制该工具配置，是否继续？</p>
          <div style={{ marginTop: '10px' }}>
            <div style={{ marginBottom: '5px', fontSize: '12px', color: '#333' }}>工具实例名称：</div>
            <Input
              defaultValue={config.toolInstanceName ? `${config.toolInstanceName}_copy` : `${config.toolType}工具_copy`}
              placeholder="请输入新的工具实例名称"
              id="toolInstanceNameInput"
              style={{ width: '100%' }}
            />
          </div>
        </div>
      ),
      okText: '确认',
      cancelText: '取消',
      onOk: () => {
        // 获取输入框中的值
        const inputElement = document.getElementById('toolInstanceNameInput') as HTMLInputElement;
        const newToolInstanceName = inputElement ? inputElement.value : (config.toolInstanceName ? `${config.toolInstanceName}_copy` : `${config.toolType}工具_copy`);
        
        // 调用复制工具函数，传递修改后的工具名称
        handleCopyTool(config, newToolInstanceName);
      },
    });
  };

  // 复制工具处理函数
  const handleCopyTool = (config: any, newToolInstanceName?: string) => {
    // 创建一个新的工具配置，基于当前配置但修改一些字段
    const newConfig = { ...config };
    
    // 删除ID，因为复制的工具应该是新的
    delete newConfig.id;
    
    // 使用用户提供的工具实例名称，如果没有提供则使用默认名称
    if (newToolInstanceName) {
      newConfig.toolInstanceName = newToolInstanceName;
    } else if (!newConfig.toolInstanceName) {
      newConfig.toolInstanceName = config.toolInstanceName ? `${config.toolInstanceName}_copy` : `${config.toolType}工具_copy`;
    }
    
    // 调用API复制工具
    eaToolApi.copyTool(newConfig)
      .then((result) => {
        if (result && (result.code === 200 || result.success === true)) {
          console.log('工具复制成功:', result);
          // 重新加载工具配置列表
          loadToolConfigs();
          
          // 获取新创建的工具ID并导航到编辑页面
          if (result.data && result.data.id) {
            const newToolId = result.data.id;
            
            // 根据工具类型导航到对应的编辑页面
            switch(config.toolType) {
              case 'SQL':
                navigate(`/pageTool/AgentConfig/tool/sql${agentId ? `?agentId=${agentId}&toolId=${newToolId}` : ''}`);
                break;
              case 'HTTP':
                navigate(`/pageTool/AgentConfig/tool/http${agentId ? `?agentId=${agentId}&toolId=${newToolId}` : ''}`);
                break;
              case 'MCP':
                navigate(`/pageTool/AgentConfig/tool/mcp${agentId ? `?agentId=${agentId}&toolId=${newToolId}` : ''}`);
                break;
              case 'GRPC':
                navigate(`/pageTool/AgentConfig/tool/grpc${agentId ? `?agentId=${agentId}&toolId=${newToolId}` : ''}`);
                break;
              default:
                break;
            }
          }
        } else {
          console.error('工具复制失败:', result?.message || 'Request failed');
          app.message.error(result?.message || '工具复制失败');
        }
      })
      .catch((error) => {
        console.error('复制工具时出错:', error);
        app.message.error('复制工具时发生错误');
      });
  };

  // 使用从API获取的工具列表
  const tools = availableTools;

  // 生成面包屑导航使用的简单标签（不包含按钮）
  const getSimpleToolLabel = (config: any) => {
    // 生成工具显示名称，优先使用toolInstanceName，如果没有则使用对应的中文名称，再加上ID以便区分
    let toolDisplayName = config.toolInstanceName;
    if (!toolDisplayName) {
      toolDisplayName = getToolTypeName(config.toolType);
    }
    
    return (
      <>
        {toolDisplayName}
        <Tag color="blue" style={{ marginLeft: '8px' }}>{config.id}</Tag>
      </>
    );
  };

  // 获取当前路径用于面包屑导航
  const getPathName = () => {
    // 根据当前活动的标签页返回相应的名称
    switch (activeTab) {
      case 'knowledge':
        return '知识库';
      case 'prompt':
        return '提示词';
      case 'tools':
        // 如果有选中的工具，使用工具实例名称（不包含按钮）
        if (selectedTool && availableTools.length > 0) {
          const selectedToolItem = availableTools.find(tool => tool.key === selectedTool);
          if (selectedToolItem) {
            // 查找对应的工具配置以生成简单标签
            const config = toolConfigs.find((c: any) => `${c.toolType}_${c.id}` === selectedTool);
            if (config) {
              return getSimpleToolLabel(config);
            }
            // 如果找不到配置，返回工具类型和ID
            const [toolType, toolId] = selectedTool.split('_');
            let toolTypeName = toolType;
            toolTypeName = getToolTypeName(toolType);
            return (
              <>
                {toolTypeName}
                <Tag color="blue" style={{ marginLeft: '8px' }}>{toolId}</Tag>
              </>
            );
          }
        }
        
        // 如果没有选中的工具，但URL中有工具ID，尝试构建带ID的标签格式
        const urlParams = new URLSearchParams(location.search);
        const toolIdFromUrl = urlParams.get('toolId');
        
        if (toolIdFromUrl && toolIdFromUrl !== 'new') {
          const currentToolType = getCurrentTool();
          let toolTypeName = currentToolType;
          
          toolTypeName = getToolTypeName(currentToolType);
          
          return (
            <>
              {toolTypeName}
              <Tag color="blue" style={{ marginLeft: '8px' }}>{toolIdFromUrl}</Tag>
            </>
          );
        }
        
        const path = location.pathname;
        if (path.includes('/tool/sql')) return getToolTypeName('SQL');
        if (path.includes('/tool/http')) return getToolTypeName('HTTP');
        if (path.includes('/tool/mcp')) return getToolTypeName('MCP');
        if (path.includes('/tool/grpc')) return getToolTypeName('GRPC');
        return getToolTypeName('SQL');
      case 'feedback':
        return '反馈';
      default:
        return 'Agent配置详情';
    }
  };

  // 菜单选择处理 - 用于选择现有工具
  const handleMenuSelect = ({ key }: { key: string }) => {
    const toolType = key.split('_')[0];
    if (toolType === 'MCP' || toolType === 'GRPC') {
      // 禁用MCP和GRPC工具的菜单选择
      return;
    }
    setSelectedTool(key);
    const toolId = key.split('_')[1]; // 从key中提取工具ID
    
    // 根据选择的工具导航到对应页面
    switch (toolType) {
      case 'SQL':
        navigate(`/pageTool/AgentConfig/tool/sql${agentId ? `?agentId=${agentId}&toolId=${toolId}` : ''}`);
        break;
      case 'HTTP':
        navigate(`/pageTool/AgentConfig/tool/http${agentId ? `?agentId=${agentId}&toolId=${toolId}` : ''}`);
        break;
      case 'MCP':
        navigate(`/pageTool/AgentConfig/tool/mcp${agentId ? `?agentId=${agentId}&toolId=${toolId}` : ''}`);
        break;
      case 'GRPC':
        navigate(`/pageTool/AgentConfig/tool/grpc${agentId ? `?agentId=${agentId}&toolId=${toolId}` : ''}`);
        break;
      default:
        break;
    }
    
    // 确保工具配置被刷新
    loadToolConfigs();
  };

  // 顶部按钮点击处理 - 用于添加新工具
  const handleTopButtonClick = (key: string) => {
    if (key === 'MCP' || key === 'GRPC') {
      // 禁用MCP和GRPC工具的按钮点击
      return;
    }
    
    // 根据选择的工具类型导航到对应页面以添加新工具
    switch (key) {
      case 'SQL':
        navigate(`/pageTool/AgentConfig/tool/sql${agentId ? `?agentId=${agentId}&toolId=new` : ''}`);
        break;
      case 'HTTP':
        navigate(`/pageTool/AgentConfig/tool/http${agentId ? `?agentId=${agentId}&toolId=new` : ''}`);
        break;
      case 'MCP':
        navigate(`/pageTool/AgentConfig/tool/mcp${agentId ? `?agentId=${agentId}&toolId=new` : ''}`);
        break;
      case 'GRPC':
        navigate(`/pageTool/AgentConfig/tool/grpc${agentId ? `?agentId=${agentId}&toolId=new` : ''}`);
        break;
      default:
        break;
    }
  };

  // 渲染不同工具的配置项
  const renderToolConfig = () => {
    // 根据当前路径决定显示哪个工具配置
    // 如果当前路径是MCP或GRPC，但这些工具被禁用，则默认显示SQL配置
    if (location.pathname.includes('/tool/mcp') || location.pathname.includes('/tool/grpc')) {
      // 如果MCP或GRPC被禁用，阻止显示它们的配置
      const currentTool = getCurrentTool();
      if (currentTool === 'MCP' || currentTool === 'GRPC') {
        return <SQLConfig toolConfigs={toolConfigs} agentId={agentId} onRefresh={loadToolConfigs} />;
      }
    }
    
    // 从URL参数中获取工具ID
    const urlParams = new URLSearchParams(location.search);
    const toolIdFromUrl = urlParams.get('toolId');
    
    // 如果URL中的toolId是"new"，表示要添加新工具，显示空配置
    if (toolIdFromUrl === 'new') {
      if (location.pathname.includes('/tool/sql')) {
        return <SQLConfig toolConfigs={[]} agentId={agentId} onRefresh={loadToolConfigs} />;
      }
      if (location.pathname.includes('/tool/http')) {
        return <HTTPConfig toolConfigs={[]} agentId={agentId} onRefresh={loadToolConfigs} />;
      }
      if (location.pathname.includes('/tool/mcp')) {
        return <MCPConfig toolConfigs={[]} agentId={agentId} onRefresh={loadToolConfigs} />;
      }
      if (location.pathname.includes('/tool/grpc')) {
        return <GRPCConfig toolConfigs={[]} agentId={agentId} onRefresh={loadToolConfigs} />;
      }
    }
    
    // 如果URL中有具体的工具ID，优先使用URL中的工具ID来过滤配置
    if (toolIdFromUrl && toolIdFromUrl !== 'new' && toolConfigs.length > 0) {
      const specificToolConfig = toolConfigs.find(config => config.id === parseInt(toolIdFromUrl));
      if (specificToolConfig) {
        // 创建只包含当前工具配置的数组
        const specificToolConfigs = [specificToolConfig];
        
        if (location.pathname.includes('/tool/sql')) {
          return <SQLConfig toolConfigs={specificToolConfigs} agentId={agentId} onRefresh={loadToolConfigs} />;
        }
        if (location.pathname.includes('/tool/http')) {
          return <HTTPConfig toolConfigs={specificToolConfigs} agentId={agentId} onRefresh={loadToolConfigs} />;
        }
        if (location.pathname.includes('/tool/mcp')) {
          return <MCPConfig toolConfigs={specificToolConfigs} agentId={agentId} onRefresh={loadToolConfigs} />;
        }
        if (location.pathname.includes('/tool/grpc')) {
          return <GRPCConfig toolConfigs={specificToolConfigs} agentId={agentId} onRefresh={loadToolConfigs} />;
        }
      } else {
        // 如果URL中的工具ID在工具配置列表中找不到，但有选中的工具，则使用选中的工具
        if (selectedTool && selectedTool !== '' && toolConfigs.length > 0) {
          const selectedToolType = selectedTool.split('_')[0];
          const selectedToolId = selectedTool.split('_')[1];
          
          // 查找匹配的工具配置
          const specificToolConfig = toolConfigs.find(config => 
            config.toolType === selectedToolType && config.id === parseInt(selectedToolId)
          );
          
          if (specificToolConfig) {
            // 创建只包含当前工具配置的数组
            const specificToolConfigs = [specificToolConfig];
            
            switch (selectedToolType) {
              case 'SQL':
                return <SQLConfig toolConfigs={specificToolConfigs} agentId={agentId} onRefresh={loadToolConfigs} />;
              case 'HTTP':
                return <HTTPConfig toolConfigs={specificToolConfigs} agentId={agentId} onRefresh={loadToolConfigs} />;
              case 'MCP':
                return <MCPConfig toolConfigs={specificToolConfigs} agentId={agentId} onRefresh={loadToolConfigs} />;
              case 'GRPC':
                return <GRPCConfig toolConfigs={specificToolConfigs} agentId={agentId} onRefresh={loadToolConfigs} />;
              default:
                return <SQLConfig toolConfigs={specificToolConfigs} agentId={agentId} onRefresh={loadToolConfigs} />;
            }
          }
        }
        
        // 如果URL中的工具ID找不到，且选中的工具也找不到配置，则根据路径显示默认配置
        if (location.pathname.includes('/tool/sql')) {
          return <SQLConfig toolConfigs={toolConfigs} agentId={agentId} onRefresh={loadToolConfigs} />;
        }
        if (location.pathname.includes('/tool/http')) {
          return <HTTPConfig toolConfigs={toolConfigs} agentId={agentId} onRefresh={loadToolConfigs} />;
        }
        if (location.pathname.includes('/tool/mcp')) {
          return <MCPConfig toolConfigs={toolConfigs} agentId={agentId} onRefresh={loadToolConfigs} />;
        }
        if (location.pathname.includes('/tool/grpc')) {
          return <GRPCConfig toolConfigs={toolConfigs} agentId={agentId} onRefresh={loadToolConfigs} />;
        }
      }
    } else {
      // 如果URL中没有工具ID，但有选中的工具，则根据选中的工具过滤配置
      if (selectedTool && selectedTool !== '' && toolConfigs.length > 0) {
        const selectedToolType = selectedTool.split('_')[0];
        const selectedToolId = selectedTool.split('_')[1];
        
        // 查找匹配的工具配置
        const specificToolConfig = toolConfigs.find(config => 
          config.toolType === selectedToolType && config.id === parseInt(selectedToolId)
        );
        
        if (specificToolConfig) {
          // 创建只包含当前工具配置的数组
          const specificToolConfigs = [specificToolConfig];
          
          switch (selectedToolType) {
            case 'SQL':
              return <SQLConfig toolConfigs={specificToolConfigs} agentId={agentId} onRefresh={loadToolConfigs} />;
            case 'HTTP':
              return <HTTPConfig toolConfigs={specificToolConfigs} agentId={agentId} onRefresh={loadToolConfigs} />;
            case 'MCP':
              return <MCPConfig toolConfigs={specificToolConfigs} agentId={agentId} onRefresh={loadToolConfigs} />;
            case 'GRPC':
              return <GRPCConfig toolConfigs={specificToolConfigs} agentId={agentId} onRefresh={loadToolConfigs} />;
            default:
              return <SQLConfig toolConfigs={specificToolConfigs} agentId={agentId} onRefresh={loadToolConfigs} />;
          }
        }
      }
      
      if (location.pathname.includes('/tool/sql')) {
        return <SQLConfig toolConfigs={toolConfigs} agentId={agentId} onRefresh={loadToolConfigs} />;
      }
      if (location.pathname.includes('/tool/http')) {
        return <HTTPConfig toolConfigs={toolConfigs} agentId={agentId} onRefresh={loadToolConfigs} />;
      }
      if (location.pathname.includes('/tool/mcp')) {
        return <MCPConfig toolConfigs={toolConfigs} agentId={agentId} onRefresh={loadToolConfigs} />;
      }
      if (location.pathname.includes('/tool/grpc')) {
        return <GRPCConfig toolConfigs={toolConfigs} agentId={agentId} onRefresh={loadToolConfigs} />;
      }
    }
    
    // 如果没有可用工具且没有特定路径，则默认显示SQL配置
    if (availableTools.length === 0) {
      return <SQLConfig toolConfigs={toolConfigs} agentId={agentId} onRefresh={loadToolConfigs} />;
    }
    
    // 根据选中的工具类型显示对应配置组件
    const selectedToolType = selectedTool.split('_')[0];
    switch (selectedToolType) {
      case 'SQL':
        return <SQLConfig toolConfigs={toolConfigs} agentId={agentId} onRefresh={loadToolConfigs} />;
      case 'HTTP':
        return <HTTPConfig toolConfigs={toolConfigs} agentId={agentId} onRefresh={loadToolConfigs} />;
      case 'MCP':
        return <MCPConfig toolConfigs={toolConfigs} agentId={agentId} onRefresh={loadToolConfigs} />;
      case 'GRPC':
        return <GRPCConfig toolConfigs={toolConfigs} agentId={agentId} onRefresh={loadToolConfigs} />;
      default:
        return <SQLConfig toolConfigs={toolConfigs} agentId={agentId} onRefresh={loadToolConfigs} />;
    }
  };

  // 知识库标签页内容
  const knowledgeBaseContent = (
    <div style={{ padding: '24px' }}>
      <KnowledgeBaseConfig agentId={agentId ? parseInt(agentId) : undefined} />
    </div>
  );

  // 提示词标签页内容
  const promptContent = (
    <div style={{ padding: '24px' }}>
      <PromptConfig agentId={agentId ? parseInt(agentId) : undefined} />
    </div>
  );

  // 工具标签页内容
  const toolsContent = (
    <Layout style={{ background: '#fff', borderRadius: '0 0 8px 8px' }}>
      <Sider width={300} theme="light" style={{ 
        background: '#fff', 
        borderRight: '1px solid #f0f0f0',
        borderRadius: '0 0 0 8px'
      }}>
        <div style={{ padding: '16px', borderBottom: '1px solid #f0f0f0' }}>
          <Title level={5} style={{ margin: 0 }}>
            {agentInfo ? `Agent: ${agentInfo.agentName}` : '工具列表'}
          </Title>
          {agentId && (
            <div style={{ fontSize: '12px', color: '#999', marginTop: '4px' }}>
              ID: {agentId}
            </div>
          )}
        </div>
        <Menu
          mode="inline"
          selectedKeys={[selectedTool]}
          onSelect={handleMenuSelect}
          items={tools.map(tool => ({
            key: tool.key,
            icon: tool.icon,
            label: tool.label,
            disabled: tool.disabled, // 添加禁用状态
          }))}
        />
      </Sider>
      
      <Content style={{ padding: '24px', minHeight: 280 }}>
        {/* 顶部工具按钮行 - 用于添加新工具 */}
        <div style={{ marginBottom: '24px' }}>
          <Space size="middle">
            <Button
              type="default"
              icon={<AnimatedDatabaseIcon isActive={false} />}
              onClick={() => handleTopButtonClick('SQL')}
            >
              添加SQL执行器
            </Button>
            <Button
              type="default"
              icon={<AnimatedApiIcon isActive={false} />}
              onClick={() => handleTopButtonClick('HTTP')}
            >
              添加HTTP请求
            </Button>
            <Button
              type="default"
              icon={<AnimatedCloudServerIcon isActive={false} />}
              onClick={() => handleTopButtonClick('MCP')}
              disabled
            >
              添加MCP服务器
            </Button>
            <Button
              type="default"
              icon={<AnimatedSyncIcon isActive={false} />}
              onClick={() => handleTopButtonClick('GRPC')}
              disabled
            >
              添加gRPC工具
            </Button>
          </Space>
        </div>
        {renderToolConfig()}
      </Content>
    </Layout>
  );

  // 反馈标签页内容
  const feedbackContent = (
    <div style={{ padding: '24px' }}>
      <FeedbackConfig agentId={agentId ? parseInt(agentId) : undefined} />
    </div>
  );

  // 处理标签页切换
  const handleTabChange = (key: string) => {
    setActiveTab(key);
    
    // 更新URL参数
    const urlParams = new URLSearchParams(location.search);
    if (key) {
      urlParams.set('tab', key);
    } else {
      urlParams.delete('tab');
    }
    
    // 保持agentId参数
    if (agentId) {
      urlParams.set('agentId', agentId);
    }
    
    navigate(`/pageTool/AgentConfig?${urlParams.toString()}`);
  };

  // 标签页配置
  const tabItems = [
    {
      key: 'knowledge',
      label: (
        <span>
          <BookOutlined />
          知识库
        </span>
      ),
      children: knowledgeBaseContent,
    },
    {
      key: 'prompt',
      label: (
        <span>
          <MessageOutlined />
          提示词
        </span>
      ),
      children: promptContent,
    },
    {
      key: 'tools',
      label: (
        <span>
          <DatabaseOutlined />
          工具
        </span>
      ),
      children: toolsContent,
    },
    {
      key: 'feedback',
      label: (
        <span>
          <CommentOutlined />
          反馈
        </span>
      ),
      children: feedbackContent,
    },
  ];

  return (
    <ConfigProvider getPopupContainer={() => document.body}>
      <App>
        <Layout style={{ minHeight: '100vh', padding: '24px' }}>
          <GlobalStyles />
          
          <div className="breadcrumb-container">
            <Breadcrumb>
              <Breadcrumb.Item href="/">首页</Breadcrumb.Item>
              <Breadcrumb.Item href="/home/AgentConfig">Agent配置</Breadcrumb.Item>
              <Breadcrumb.Item>Agent配置详情</Breadcrumb.Item>
            </Breadcrumb>
          </div>
          
          <div style={{ background: '#fff', borderRadius: '8px', marginTop: '16px', paddingLeft: '10px' }}>
            <Tabs 
              activeKey={activeTab}
              onChange={handleTabChange}
              items={tabItems}
              destroyInactiveTabPane={true}
            />
          </div>
        </Layout>
      </App>
    </ConfigProvider>
  );
};

export default AgentConfig;