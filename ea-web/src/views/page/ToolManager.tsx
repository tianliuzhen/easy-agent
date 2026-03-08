import React, {useState, useEffect} from 'react';
import {
    App,
    Layout,
    Menu,
    Typography,
    Breadcrumb,
    Button,
    Space,
    Tag,
    Popconfirm,
    ConfigProvider,
    Input,
    Modal,
    Tooltip
} from 'antd';
import {
    DatabaseOutlined,
    ApiOutlined,
    CloudServerOutlined,
    SyncOutlined,
    EditOutlined,
    DeleteOutlined,
    CopyOutlined,
} from '@ant-design/icons';
import {useNavigate, useLocation} from 'react-router-dom';
import {eaToolApi} from '../api/EaToolApi';
import SQLConfig from './agent/tool/SQLConfig';
import HTTPConfig from './agent/tool/HTTPConfig';
import MCPConfig from './agent/tool/MCPConfig';
import GRPCConfig from './agent/tool/GRPCConfig';

const {Sider, Content} = Layout;
const {Title} = Typography;

// 自定义带动画效果的图标组件
const AnimatedDatabaseIcon = ({isActive}: { isActive: boolean }) => (
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

const AnimatedApiIcon = ({isActive}: { isActive: boolean }) => (
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

const AnimatedCloudServerIcon = ({isActive}: { isActive: boolean }) => (
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

const AnimatedSyncIcon = ({isActive}: { isActive: boolean }) => (
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

// 添加全局 CSS 动画关键帧
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
    `}
    </style>
);

const ToolManager: React.FC = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const app = App.useApp();

    // 默认 agentId 为 0
    const agentId = '0';

    const [selectedTool, setSelectedTool] = useState<string>('');
    const [availableTools, setAvailableTools] = useState<{
        key: string;
        label: string;
        icon: React.ReactNode;
        disabled?: boolean
    }[]>([]);
    const [toolConfigs, setToolConfigs] = useState<any[]>([]);

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
        switch (toolType) {
            case 'SQL':
                return 'SQL 执行器';
            case 'HTTP':
                return 'HTTP 请求';
            case 'MCP':
                return 'MCP 服务器';
            case 'GRPC':
                return 'gRPC 工具';
            default:
                return toolType;
        }
    };

    // 根据 URL 参数获取当前选中的工具键值
    const getCurrentToolKey = () => {
        const urlParams = new URLSearchParams(location.search);
        const toolIdFromUrl = urlParams.get('toolId');

        if (toolIdFromUrl && toolIdFromUrl !== 'new') {
            const toolType = getCurrentTool();
            return `${toolType}_${toolIdFromUrl}`;
        }

        return '';
    };

    // 获取工具配置（用于设置 toolConfigs 和 availableTools 状态）
    const loadToolConfigs = () => {
        eaToolApi.getToolConfigByAgentId(parseInt(agentId))
            .then((result) => {
                if (result && (result.code === 200 || result.success === true)) {
                    const toolConfigs = result.data || [];
                    setToolConfigs(toolConfigs);

                    // 同时更新左侧工具列表
                    const toolsWithDetails = toolConfigs.map((config: any, index: number) => {
                        // 禁用 MCP 和 GRPC 工具
                        const isDisabled = config.toolType === 'MCP' || config.toolType === 'GRPC';

                        // 生成工具显示名称
                        let toolDisplayName = config.toolInstanceName;
                        if (!toolDisplayName) {
                            toolDisplayName = getToolTypeName(config.toolType);
                        }
                        const displayLabel = (
                            <div style={{
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'space-between',
                                width: '100%'
                            }}>
                                <Tooltip title={toolDisplayName} color="black" placement="right">
                                    <div style={{
                                        flex: 1,
                                        overflow: 'hidden',
                                        textOverflow: 'ellipsis',
                                        whiteSpace: 'nowrap',
                                        marginRight: '8px'
                                    }}>
                                        {toolDisplayName}
                                        <Tag color="blue" style={{marginLeft: '8px'}}>{config.id}</Tag>
                                    </div>
                                </Tooltip>
                                <div style={{display: 'flex', gap: '4px', alignItems: 'center', flexShrink: 0}}>
                                    <Button
                                        type="text"
                                        size="small"
                                        icon={<EditOutlined style={{color: '#1890ff'}}/>}
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            handleEditTool(config);
                                        }}
                                        style={{padding: '0 4px'}}
                                    />
                                    <Button
                                        type="text"
                                        size="small"
                                        icon={<CopyOutlined/>}
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            showCopyConfirm(config);
                                        }}
                                        style={{padding: '0 4px'}}
                                    />
                                    <Popconfirm
                                        title="确认删除工具？"
                                        description="此操作将永久删除该工具配置，是否继续？"
                                        onConfirm={(e) => {
                                            e?.stopPropagation();
                                            handleDeleteTool(config);
                                        }}
                                        okText="确认"
                                        cancelText="取消"
                                    >
                                        <Button
                                            type="text"
                                            size="small"
                                            danger
                                            icon={<DeleteOutlined/>}
                                            onClick={(e) => e.stopPropagation()}
                                            style={{padding: '0 4px'}}
                                        />
                                    </Popconfirm>
                                </div>
                            </div>
                        );

                        switch (config.toolType) {
                            case 'SQL':
                                return {
                                    key: `${config.toolType}_${config.id}`,
                                    label: displayLabel,
                                    icon: <AnimatedDatabaseIcon isActive={selectedTool.startsWith(config.toolType)}/>,
                                    disabled: false
                                };
                            case 'HTTP':
                                return {
                                    key: `${config.toolType}_${config.id}`,
                                    label: displayLabel,
                                    icon: <AnimatedApiIcon isActive={selectedTool.startsWith(config.toolType)}/>,
                                    disabled: false
                                };
                            case 'MCP':
                                return {
                                    key: `${config.toolType}_${config.id}`,
                                    label: displayLabel,
                                    icon: <AnimatedCloudServerIcon
                                        isActive={selectedTool.startsWith(config.toolType)}/>,
                                    disabled: true
                                };
                            case 'GRPC':
                                return {
                                    key: `${config.toolType}_${config.id}`,
                                    label: displayLabel,
                                    icon: <AnimatedSyncIcon isActive={selectedTool.startsWith(config.toolType)}/>,
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
                    console.log('Loaded tool configs:', result.data);

                    // 如果有可用工具，尝试根据 URL 参数设置选中的工具，否则选择第一个可用工具
                    const currentToolKey = getCurrentToolKey();
                    if (currentToolKey && toolsWithDetails.some(tool => tool.key === currentToolKey)) {
                        setSelectedTool(currentToolKey);
                    } else if (toolsWithDetails.length > 0) {
                        setSelectedTool(toolsWithDetails[0].key);
                    } else {
                        setSelectedTool('');
                    }
                } else {
                    console.error('Failed to load tool configs:', result?.message || 'Request failed');
                    setToolConfigs([]);
                    setAvailableTools([]);
                    setSelectedTool('');
                }
            })
            .catch((error) => {
                console.error('Error loading tool configs:', error);
                setToolConfigs([]);
                setAvailableTools([]);
                setSelectedTool('');
            });
    };

    // 加载工具配置
    useEffect(() => {
        loadToolConfigs();

        // 根据 URL 参数设置当前选中的工具
        const currentToolKey = getCurrentToolKey();
        if (currentToolKey) {
            setSelectedTool(currentToolKey);
        }
    }, [location.search]);

    // 编辑工具处理函数
    const handleEditTool = (config: any) => {
        switch (config.toolType) {
            case 'SQL':
                navigate(`/toolManager/tool/sql${agentId ? `?agentId=${agentId}&toolId=${config.id}` : ''}`);
                break;
            case 'HTTP':
                navigate(`/toolManager/tool/http${agentId ? `?agentId=${agentId}&toolId=${config.id}` : ''}`);
                break;
            case 'MCP':
                navigate(`/toolManager/tool/mcp${agentId ? `?agentId=${agentId}&toolId=${config.id}` : ''}`);
                break;
            case 'GRPC':
                navigate(`/toolManager/tool/grpc${agentId ? `?agentId=${agentId}&toolId=${config.id}` : ''}`);
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
                    <div style={{marginTop: '10px'}}>
                        <div style={{marginBottom: '5px', fontSize: '12px', color: '#333'}}>工具实例名称：</div>
                        <Input
                            defaultValue={config.toolInstanceName ? `${config.toolInstanceName}_copy` : `${config.toolType}工具_copy`}
                            placeholder="请输入新的工具实例名称"
                            id="toolInstanceNameInput"
                            style={{width: '100%'}}
                        />
                    </div>
                </div>
            ),
            okText: '确认',
            cancelText: '取消',
            onOk: () => {
                const inputElement = document.getElementById('toolInstanceNameInput') as HTMLInputElement;
                const newToolInstanceName = inputElement ? inputElement.value : (config.toolInstanceName ? `${config.toolInstanceName}_copy` : `${config.toolType}工具_copy`);
                handleCopyTool(config, newToolInstanceName);
            },
        });
    };

    // 复制工具处理函数
    const handleCopyTool = (config: any, newToolInstanceName?: string) => {
        const newConfig = {...config};
        delete newConfig.id;

        if (newToolInstanceName) {
            newConfig.toolInstanceName = newToolInstanceName;
        } else if (!newConfig.toolInstanceName) {
            newConfig.toolInstanceName = config.toolInstanceName ? `${config.toolInstanceName}_copy` : `${config.toolType}工具_copy`;
        }

        eaToolApi.copyTool(newConfig)
            .then((result) => {
                if (result && (result.code === 200 || result.success === true)) {
                    console.log('工具复制成功:', result);
                    loadToolConfigs();

                    if (result.data && result.data.id) {
                        const newToolId = result.data.id;

                        switch (config.toolType) {
                            case 'SQL':
                                navigate(`/toolManager/tool/sql${agentId ? `?agentId=${agentId}&toolId=${newToolId}` : ''}`);
                                break;
                            case 'HTTP':
                                navigate(`/toolManager/tool/http${agentId ? `?agentId=${agentId}&toolId=${newToolId}` : ''}`);
                                break;
                            case 'MCP':
                                navigate(`/toolManager/tool/mcp${agentId ? `?agentId=${agentId}&toolId=${newToolId}` : ''}`);
                                break;
                            case 'GRPC':
                                navigate(`/toolManager/tool/grpc${agentId ? `?agentId=${agentId}&toolId=${newToolId}` : ''}`);
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

    // 顶部按钮点击处理 - 用于添加新工具
    const handleTopButtonClick = (key: string) => {
        if (key === 'MCP' || key === 'GRPC') {
            return;
        }

        switch (key) {
            case 'SQL':
                navigate(`/toolManager/tool/sql${agentId ? `?agentId=${agentId}&toolId=new` : ''}`);
                break;
            case 'HTTP':
                navigate(`/toolManager/tool/http${agentId ? `?agentId=${agentId}&toolId=new` : ''}`);
                break;
            case 'MCP':
                navigate(`/toolManager/tool/mcp${agentId ? `?agentId=${agentId}&toolId=new` : ''}`);
                break;
            case 'GRPC':
                navigate(`/toolManager/tool/grpc${agentId ? `?agentId=${agentId}&toolId=new` : ''}`);
                break;
            default:
                break;
        }
    };

    // 菜单选择处理
    const handleMenuSelect = ({key}: { key: string }) => {
        const toolType = key.split('_')[0];
        if (toolType === 'MCP' || toolType === 'GRPC') {
            return;
        }
        setSelectedTool(key);
        const toolId = key.split('_')[1];

        switch (toolType) {
            case 'SQL':
                navigate(`/toolManager/tool/sql${agentId ? `?agentId=${agentId}&toolId=${toolId}` : ''}`);
                break;
            case 'HTTP':
                navigate(`/toolManager/tool/http${agentId ? `?agentId=${agentId}&toolId=${toolId}` : ''}`);
                break;
            case 'MCP':
                navigate(`/toolManager/tool/mcp${agentId ? `?agentId=${agentId}&toolId=${toolId}` : ''}`);
                break;
            case 'GRPC':
                navigate(`/toolManager/tool/grpc${agentId ? `?agentId=${agentId}&toolId=${toolId}` : ''}`);
                break;
            default:
                break;
        }

        loadToolConfigs();
    };

    // 渲染不同工具的配置项
    const renderToolConfig = () => {
        const urlParams = new URLSearchParams(location.search);
        const toolIdFromUrl = urlParams.get('toolId');

        if (toolIdFromUrl === 'new') {
            if (location.pathname.includes('/tool/sql')) {
                return <SQLConfig toolConfigs={[]} agentId={agentId} onRefresh={loadToolConfigs}/>;
            }
            if (location.pathname.includes('/tool/http')) {
                return <HTTPConfig toolConfigs={[]} agentId={agentId} onRefresh={loadToolConfigs}/>;
            }
            if (location.pathname.includes('/tool/mcp')) {
                return <MCPConfig toolConfigs={[]} agentId={agentId} onRefresh={loadToolConfigs}/>;
            }
            if (location.pathname.includes('/tool/grpc')) {
                return <GRPCConfig toolConfigs={[]} agentId={agentId} onRefresh={loadToolConfigs}/>;
            }
        }

        if (toolIdFromUrl && toolIdFromUrl !== 'new' && toolConfigs.length > 0) {
            const specificToolConfig = toolConfigs.find(config => config.id === parseInt(toolIdFromUrl));
            if (specificToolConfig) {
                const specificToolConfigs = [specificToolConfig];

                if (location.pathname.includes('/tool/sql')) {
                    return <SQLConfig toolConfigs={specificToolConfigs} agentId={agentId} onRefresh={loadToolConfigs}/>;
                }
                if (location.pathname.includes('/tool/http')) {
                    return <HTTPConfig toolConfigs={specificToolConfigs} agentId={agentId}
                                       onRefresh={loadToolConfigs}/>;
                }
                if (location.pathname.includes('/tool/mcp')) {
                    return <MCPConfig toolConfigs={specificToolConfigs} agentId={agentId} onRefresh={loadToolConfigs}/>;
                }
                if (location.pathname.includes('/tool/grpc')) {
                    return <GRPCConfig toolConfigs={specificToolConfigs} agentId={agentId}
                                       onRefresh={loadToolConfigs}/>;
                }
            }
        }

        if (location.pathname.includes('/tool/sql')) {
            return <SQLConfig toolConfigs={toolConfigs} agentId={agentId} onRefresh={loadToolConfigs}/>;
        }
        if (location.pathname.includes('/tool/http')) {
            return <HTTPConfig toolConfigs={toolConfigs} agentId={agentId} onRefresh={loadToolConfigs}/>;
        }
        if (location.pathname.includes('/tool/mcp')) {
            return <MCPConfig toolConfigs={toolConfigs} agentId={agentId} onRefresh={loadToolConfigs}/>;
        }
        if (location.pathname.includes('/tool/grpc')) {
            return <GRPCConfig toolConfigs={toolConfigs} agentId={agentId} onRefresh={loadToolConfigs}/>;
        }

        return <SQLConfig toolConfigs={toolConfigs} agentId={agentId} onRefresh={loadToolConfigs}/>;
    };

    const tools = availableTools;

    return (
        <ConfigProvider getPopupContainer={() => document.body}>
            <App>
                <Layout style={{minHeight: '100vh', padding: '0'}}>
                    <GlobalStyles/>
                    <div style={{background: '#fff', borderRadius: '0', padding: '10px'}}>
                        <Layout style={{background: '#fff', borderRadius: '0', padding: '0'}}>
                            <Sider width={300} theme="light" style={{
                                background: '#fff',
                                borderRight: '1px solid #f0f0f0',
                                borderRadius: '0'
                            }}>
                                <div style={{padding: '0', borderBottom: '1px solid #f0f0f0'}}>
                                    <Title level={5} style={{margin: 0}}>
                                        工具列表
                                    </Title>
                                    <div style={{fontSize: '12px', color: '#999', marginTop: '4px'}}>
                                        ID: {agentId}
                                    </div>
                                </div>
                                <Menu
                                    mode="inline"
                                    selectedKeys={[selectedTool]}
                                    onSelect={handleMenuSelect}
                                    items={tools.map(tool => ({
                                        key: tool.key,
                                        icon: tool.icon,
                                        label: tool.label,
                                        disabled: tool.disabled,
                                    }))}
                                />
                            </Sider>

                            <Content style={{padding: '0', minHeight: 280}}>
                                <div style={{marginBottom: '0'}}>
                                    <Space size="middle">
                                        <Button
                                            type="default"
                                            icon={<AnimatedDatabaseIcon isActive={false}/>}
                                            onClick={() => handleTopButtonClick('SQL')}
                                        >
                                            添加 SQL 执行器
                                        </Button>
                                        <Button
                                            type="default"
                                            icon={<AnimatedApiIcon isActive={false}/>}
                                            onClick={() => handleTopButtonClick('HTTP')}
                                        >
                                            添加 HTTP 请求
                                        </Button>
                                        <Button
                                            type="default"
                                            icon={<AnimatedCloudServerIcon isActive={false}/>}
                                            onClick={() => handleTopButtonClick('MCP')}
                                            disabled
                                        >
                                            添加 MCP 服务器
                                        </Button>
                                        <Button
                                            type="default"
                                            icon={<AnimatedSyncIcon isActive={false}/>}
                                            onClick={() => handleTopButtonClick('GRPC')}
                                            disabled
                                        >
                                            添加 gRPC 工具
                                        </Button>
                                    </Space>
                                </div>
                                {renderToolConfig()}
                            </Content>
                        </Layout>
                    </div>
                </Layout>
            </App>
        </ConfigProvider>
    );
};

export default ToolManager;
