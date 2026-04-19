import React, {useState, useEffect, useCallback, useRef} from 'react';
import {
    App,
    Layout,
    Menu,
    Typography,
    Button,
    Space,
    Tag,
    Popconfirm,
    ConfigProvider,
    Input,
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

interface ToolManagerProps {
    mode?: 'default' | 'my';
    title?: string;
    basePath?: string;
}

const ToolManager: React.FC<ToolManagerProps> = ({
                                                     mode = 'default',
                                                     title = mode === 'default' ? '工具列表' : '我的工具列表',
                                                     basePath = mode === 'default' ? '/toolManager' : '/myToolManager'
                                                 }) => {
    console.log('ToolManager rendered, mode:', mode);
    const navigate = useNavigate();
    const location = useLocation();
    const app = App.useApp();

    const agentId = '0';

    const [selectedTool, setSelectedTool] = useState<string>('');
    const [availableTools, setAvailableTools] = useState<{
        key: string;
        label: string;
        icon: React.ReactNode;
        disabled?: boolean
    }[]>([]);
    const [toolConfigs, setToolConfigs] = useState<any[]>([]);
    const [loading, setLoading] = useState(false);

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

    // 获取当前工具类型
    const getCurrentToolType = () => {
        const path = location.pathname;
        if (path.includes('/tool/sql')) return 'SQL';
        if (path.includes('/tool/http')) return 'HTTP';
        if (path.includes('/tool/mcp')) return 'MCP';
        if (path.includes('/tool/grpc')) return 'GRPC';
        return null;
    };

    // 获取当前选中的工具ID
    const getCurrentToolId = () => {
        const urlParams = new URLSearchParams(location.search);
        return urlParams.get('toolId');
    };

    // 加载工具配置
    const loadToolConfigs = useCallback(async () => {
        if (loading) return;

        setLoading(true);
        console.log('Loading tool configs, mode:', mode);

        const apiCall = mode === 'default'
            ? eaToolApi.getDefaultTools()
            : eaToolApi.getToolConfigByUserId();

        try {
            const result = await apiCall;
            if (result && (result.code === 200 || result.success === true)) {
                const configs = result.data || [];
                setToolConfigs(configs);

                // 构建左侧菜单
                const toolsList = configs.map((config: any) => {
                    const isDisabled = config.toolType === 'MCP' || config.toolType === 'GRPC';
                    let toolDisplayName = config.toolInstanceName || getToolTypeName(config.toolType);

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

                    const baseIconProps = {isActive: selectedTool === `${config.toolType}_${config.id}`};

                    switch (config.toolType) {
                        case 'SQL':
                            return {
                                key: `${config.toolType}_${config.id}`,
                                label: displayLabel,
                                icon: <AnimatedDatabaseIcon {...baseIconProps} />,
                                disabled: false
                            };
                        case 'HTTP':
                            return {
                                key: `${config.toolType}_${config.id}`,
                                label: displayLabel,
                                icon: <AnimatedApiIcon {...baseIconProps} />,
                                disabled: false
                            };
                        case 'MCP':
                            return {
                                key: `${config.toolType}_${config.id}`,
                                label: displayLabel,
                                icon: <AnimatedCloudServerIcon {...baseIconProps} />,
                                disabled: true
                            };
                        case 'GRPC':
                            return {
                                key: `${config.toolType}_${config.id}`,
                                label: displayLabel,
                                icon: <AnimatedSyncIcon {...baseIconProps} />,
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

                setAvailableTools(toolsList);
                console.log('Loaded tool configs:', configs.length);
            } else {
                console.error('Failed to load tool configs:', result?.message);
                setToolConfigs([]);
                setAvailableTools([]);
            }
        } catch (error) {
            console.error('Error loading tool configs:', error);
            setToolConfigs([]);
            setAvailableTools([]);
        } finally {
            setLoading(false);
        }
    }, [mode, agentId, basePath, navigate]);

    // 编辑工具
    const handleEditTool = useCallback((config: any) => {
        const toolType = config.toolType.toLowerCase();
        navigate(`${basePath}/tool/${toolType}?agentId=${agentId}&toolId=${config.id}`);
    }, [basePath, agentId, navigate]);

    // 删除工具
    const handleDeleteTool = useCallback((config: any) => {
        eaToolApi.delTool(config)
            .then((result) => {
                if (result && (result.code === 200 || result.success === true)) {
                    console.log('工具删除成功:', config.id);
                    loadToolConfigs();
                    // 如果删除的是当前选中的工具，导航回工具列表页
                    const currentToolId = getCurrentToolId();
                    if (currentToolId === String(config.id)) {
                        navigate(basePath);
                    }
                } else {
                    console.error('工具删除失败:', result?.message);
                    app.message.error(result?.message || '删除失败');
                }
            })
            .catch((error) => {
                console.error('删除工具时出错:', error);
                app.message.error('删除失败');
            });
    }, [loadToolConfigs, basePath, navigate, app.message]);

    // 复制工具
    const showCopyConfirm = useCallback((config: any) => {
        const modal = app.modal;
        let inputValue = config.toolInstanceName ? `${config.toolInstanceName}_copy` : `${config.toolType}工具_copy`;

        const modalInstance = modal.confirm({
            title: '确认复制工具',
            content: (
                <div>
                    <p>此操作将复制该工具配置，是否继续？</p>
                    <div style={{marginTop: '10px'}}>
                        <div style={{marginBottom: '5px', fontSize: '12px', color: '#333'}}>工具实例名称：</div>
                        <Input
                            defaultValue={inputValue}
                            placeholder="请输入新的工具实例名称"
                            onChange={(e) => {
                                inputValue = e.target.value;
                            }}
                            style={{width: '100%'}}
                        />
                    </div>
                </div>
            ),
            okText: '确认',
            cancelText: '取消',
            onOk: () => {
                handleCopyTool(config, inputValue);
            },
        });
    }, [app.modal]);

    const handleCopyTool = useCallback((config: any, newToolInstanceName: string) => {
        const newConfig = {...config};
        delete newConfig.id;
        newConfig.toolInstanceName = newToolInstanceName;

        eaToolApi.copyTool(newConfig)
            .then((result) => {
                if (result && (result.code === 200 || result.success === true)) {
                    console.log('工具复制成功:', result);
                    app.message.success('复制成功');
                    loadToolConfigs();
                } else {
                    console.error('工具复制失败:', result?.message);
                    app.message.error(result?.message || '复制失败');
                }
            })
            .catch((error) => {
                console.error('复制工具时出错:', error);
                app.message.error('复制失败');
            });
    }, [loadToolConfigs, app.message]);

    // 添加新工具
    const handleAddTool = useCallback((toolType: string) => {
        if (toolType === 'MCP' || toolType === 'GRPC') {
            app.message.warning('该功能暂未开放');
            return;
        }
        navigate(`${basePath}/tool/${toolType.toLowerCase()}?agentId=${agentId}&toolId=new`);
    }, [basePath, agentId, navigate, app.message]);

    // 菜单选择
    const handleMenuSelect = useCallback(({key}: { key: string }) => {
        const [toolType, toolId] = key.split('_');
        if (toolType === 'MCP' || toolType === 'GRPC') {
            app.message.warning('该功能暂未开放');
            return;
        }

        setSelectedTool(key);
        navigate(`${basePath}/tool/${toolType.toLowerCase()}?agentId=${agentId}&toolId=${toolId}`);
    }, [basePath, agentId, navigate, app.message]);

    // 渲染配置组件
    const renderToolConfig = useCallback(() => {
        const toolId = getCurrentToolId();
        const toolType = getCurrentToolType();

        if (!toolType) {
            return <div style={{padding: '20px', textAlign: 'center', color: '#999'}}>请从左侧选择一个工具</div>;
        }

        const commonProps = {
            agentId,
            onRefresh: loadToolConfigs
        };

        // 新增模式
        if (toolId === 'new') {
            switch (toolType) {
                case 'SQL':
                    return <SQLConfig toolConfigs={[]} {...commonProps} />;
                case 'HTTP':
                    return <HTTPConfig toolConfigs={[]} {...commonProps} />;
                case 'MCP':
                    return <MCPConfig toolConfigs={[]} {...commonProps} />;
                case 'GRPC':
                    return <GRPCConfig toolConfigs={[]} {...commonProps} />;
                default:
                    return null;
            }
        }

        // 编辑模式
        if (toolId && toolConfigs.length > 0) {
            const specificConfig = toolConfigs.find(config => config.id === parseInt(toolId));
            if (specificConfig) {
                switch (toolType) {
                    case 'SQL':
                        return <SQLConfig toolConfigs={[specificConfig]} {...commonProps} />;
                    case 'HTTP':
                        return <HTTPConfig toolConfigs={[specificConfig]} {...commonProps} />;
                    case 'MCP':
                        return <MCPConfig toolConfigs={[specificConfig]} {...commonProps} />;
                    case 'GRPC':
                        return <GRPCConfig toolConfigs={[specificConfig]} {...commonProps} />;
                    default:
                        return null;
                }
            }
        }

        // 默认显示所有工具配置（当没有选中具体工具时）
        switch (toolType) {
            case 'SQL':
                return <SQLConfig toolConfigs={toolConfigs} {...commonProps} />;
            case 'HTTP':
                return <HTTPConfig toolConfigs={toolConfigs} {...commonProps} />;
            case 'MCP':
                return <MCPConfig toolConfigs={toolConfigs} {...commonProps} />;
            case 'GRPC':
                return <GRPCConfig toolConfigs={toolConfigs} {...commonProps} />;
            default:
                return null;
        }
    }, [location.pathname, location.search, toolConfigs, agentId, loadToolConfigs]);

    // 根据URL同步选中的菜单项
    useEffect(() => {
        const toolId = getCurrentToolId();
        const toolType = getCurrentToolType();

        if (toolId && toolId !== 'new' && toolType) {
            const menuKey = `${toolType}_${toolId}`;
            if (availableTools.some(tool => tool.key === menuKey)) {
                setSelectedTool(menuKey);
            }
        }
    }, [location.pathname, location.search, availableTools]);

    // 初始加载
    useEffect(() => {
        // 切换 mode 时，先导航回列表页，清除 URL 参数
        const currentPath = location.pathname;
        const isInSubPage = currentPath.includes('/tool/');
        
        if (isInSubPage) {
            navigate(basePath, {replace: true});
        }
        
        loadToolConfigs();
    }, [mode]); // 只在 mode 变化时重新加载

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
                                <div style={{padding: '0 0 16px 0', borderBottom: '1px solid #f0f0f0'}}>
                                    <Title level={5} style={{margin: 0}}>
                                        {title}
                                    </Title>
                                    <div style={{fontSize: '12px', color: '#999', marginTop: '4px'}}>
                                        Agent ID: {agentId}
                                    </div>
                                </div>
                                <Menu
                                    mode="inline"
                                    selectedKeys={[selectedTool]}
                                    onSelect={handleMenuSelect}
                                    items={availableTools.map(tool => ({
                                        key: tool.key,
                                        icon: tool.icon,
                                        label: tool.label,
                                        disabled: tool.disabled,
                                    }))}
                                    style={{borderRight: 'none'}}
                                />
                            </Sider>

                            <Content style={{padding: '0 0 0 16px', minHeight: 280}}>
                                <div style={{marginBottom: '16px'}}>
                                    <Space size="middle">
                                        <Button
                                            icon={<AnimatedDatabaseIcon isActive={false}/>}
                                            onClick={() => handleAddTool('SQL')}
                                        >
                                            添加 SQL 执行器
                                        </Button>
                                        <Button
                                            icon={<AnimatedApiIcon isActive={false}/>}
                                            onClick={() => handleAddTool('HTTP')}
                                        >
                                            添加 HTTP 请求
                                        </Button>
                                        <Button
                                            icon={<AnimatedCloudServerIcon isActive={false}/>}
                                            onClick={() => handleAddTool('MCP')}
                                        >
                                            添加 MCP 服务器
                                        </Button>
                                        <Button
                                            icon={<AnimatedSyncIcon isActive={false}/>}
                                            onClick={() => handleAddTool('GRPC')}
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
