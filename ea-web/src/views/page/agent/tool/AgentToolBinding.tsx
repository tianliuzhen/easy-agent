import React, {useState, useEffect} from 'react';
import {List, Card, Tag, Button, Empty, Spin, Tooltip, message, Popconfirm} from 'antd';
import {
    ApiOutlined,
    DatabaseOutlined,
    CloudServerOutlined,
    EyeOutlined,
    PlayCircleOutlined,
    DisconnectOutlined,
    PlusOutlined
} from '@ant-design/icons';
import {eaToolApi} from '../../../api/EaToolApi';
import AddResourceModal from '../common/AddResourceModal';

interface ToolItem {
    id: number;
    name: string;
    type: 'SQL' | 'HTTP' | 'MCP' | 'GRPC' | '其他';
    description: string;
    status: 'active' | 'inactive' | 'error';
    lastUsed: string;
    config?: any;
    isLinked: boolean;
    // 后端原始字段（用于兼容）
    toolInstanceName?: string;
    toolInstanceDesc?: string;
    toolType?: string;
}

interface AgentToolBindingProps {
    agentId?: number;
    refreshKey?: number;
    onRefresh?: () => void;
    onCountUpdate?: (count: number) => void;
}

const AgentToolBinding: React.FC<AgentToolBindingProps> = ({
                                                               agentId,
                                                               refreshKey,
                                                               onRefresh,
                                                               onCountUpdate
                                                           }) => {
    const [tools, setTools] = useState<ToolItem[]>([]);
    const [loading, setLoading] = useState(false);
    const [modalVisible, setModalVisible] = useState(false);
    const [modalType, setModalType] = useState<'tool'>('tool');

    // 当 agentId 变化时加载数据
    useEffect(() => {
        if (agentId) {
            loadTools();
        } else {
            // 如果没有 agentId，清空数据
            setTools([]);
            if (onCountUpdate) {
                onCountUpdate(0);
            }
        }
    }, [agentId]); // 监听 agentId 变化

    // 当 refreshKey 变化时重新加载
    useEffect(() => {
        if (refreshKey > 0) {
            loadTools();
        }
    }, [refreshKey]);

    // 当工具列表变化时，通知父组件更新数量
    useEffect(() => {
        if (onCountUpdate) {
            onCountUpdate(tools.length);
        }
    }, [tools, onCountUpdate]);

    const loadTools = async () => {
        if (!agentId) return;

        setLoading(true);
        try {
            // 根据Agent ID加载关联的工具
            const response = await eaToolApi.listBoundToolsByAgentId(agentId.toString());
            if (response.success) {
                const tools = response.data.map((item: any) => ({
                    id: item.id,
                    name: item.toolInstanceName || item.name || `工具-${item.id}`,
                    type: (item.toolType || '其他') as 'SQL' | 'HTTP' | 'MCP' | 'GRPC' | '其他',
                    description: item.toolInstanceDesc || item.description || item.toolValue || '无描述',
                    status: item.isActive ? 'active' : 'inactive',
                    lastUsed: item.updatedAt || item.createdAt || '从未使用',
                    config: item,
                    isLinked: true
                }));
                setTools(tools);
            } else {
                message.error(`加载工具失败: ${response.message}`);
                setTools([]);
            }
        } catch (error) {
            console.error('加载工具失败:', error);
            message.error('加载工具失败');
            setTools([]);
        } finally {
            setLoading(false);
        }
    };

    // 获取工具图标
    const getToolIcon = (type: string) => {
        switch (type) {
            case 'SQL':
                return <DatabaseOutlined style={{color: '#52c41a'}}/>;
            case 'HTTP':
                return <ApiOutlined style={{color: '#1890ff'}}/>;
            case 'MCP':
                return <CloudServerOutlined style={{color: '#fa8c16'}}/>;
            case 'GRPC':
                return <CloudServerOutlined style={{color: '#722ed1'}}/>;
            default:
                return <ApiOutlined style={{color: '#8c8c8c'}}/>;
        }
    };

    // 获取类型标签颜色
    const getTypeColor = (type: string) => {
        const typeColors: Record<string, string> = {
            'SQL': 'green',
            'HTTP': 'blue',
            'MCP': 'orange',
            'GRPC': 'purple',
            '其他': 'default'
        };
        return typeColors[type] || 'default';
    };

    // 获取状态标签颜色
    const getStatusColor = (status: string) => {
        const statusColors: Record<string, string> = {
            'active': 'green',
            'inactive': 'orange',
            'error': 'red'
        };
        return statusColors[status] || 'default';
    };

    // 获取状态文本
    const getStatusText = (status: string) => {
        const statusTexts: Record<string, string> = {
            'active': '运行中',
            'inactive': '已停用',
            'error': '错误'
        };
        return statusTexts[status] || status;
    };

    // 查看工具详情
    const handleView = (item: ToolItem) => {
        message.info(`查看工具: ${item.name}`);
        // 这里可以打开详情模态框或跳转到详情页
    };

    // 测试工具
    const handleTest = (item: ToolItem) => {
        message.info(`测试工具: ${item.name}`);
        // 这里可以打开测试模态框
    };

    // 解绑工具
    const handleUnbind = async (item: ToolItem) => {
        if (!agentId) {
            message.error('无法解绑：缺少Agent ID');
            return;
        }

        try {
            const request = {
                agentId: agentId.toString(),
                toolConfigId: item.id
            };

            const response = await eaToolApi.unbindTool(request);
            if (response.success) {
                message.success(`已解绑工具: ${item.name}`);
                if (onRefresh) {
                    onRefresh();
                }
                // 重新加载列表
                loadTools();
            } else {
                message.error(`解绑失败: ${response.message}`);
            }
        } catch (error) {
            console.error('解绑工具失败:', error);
            message.error('解绑工具失败');
        }
    };

    // 打开添加资源模态框
    const handleAddResource = () => {
        setModalType('tool');
        setModalVisible(true);
    };

    // 关闭模态框
    const handleModalClose = () => {
        setModalVisible(false);
    };

    // 处理资源添加成功
    const handleResourceAdded = (type: 'tool') => {
        setModalVisible(false);
        if (onRefresh) {
            onRefresh();
        }
        // 重新加载列表
        loadTools();
        message.success('工具关联成功');
    };

    // 渲染空状态
    const renderEmptyState = () => {
        return (
            <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description={
                    <div>
                        <div style={{marginBottom: '8px'}}>暂无关联的工具</div>
                        <Button type="primary" icon={<PlusOutlined/>} onClick={handleAddResource}>
                            关联官方工具
                        </Button>
                    </div>
                }
                style={{margin: '40px 0'}}
            />
        );
    };

    if (loading) {
        return (
            <div style={{display: 'flex', justifyContent: 'center', padding: '40px 0'}}>
                <Spin tip="加载工具中..."/>
            </div>
        );
    }

    if (tools.length === 0) {
        return (
            <div>
                <div style={{marginBottom: '16px'}}>
                    <h3 style={{margin: 0}}>关联的工具</h3>
                    <div style={{fontSize: '12px', color: '#666', marginTop: '4px'}}>
                        关联外部工具，让Agent具备相关工具能力
                    </div>
                </div>
                {renderEmptyState()}
                <AddResourceModal
                    visible={modalVisible}
                    type={modalType}
                    agentId={agentId}
                    onClose={handleModalClose}
                    onSuccess={handleResourceAdded}
                />
            </div>
        );
    }

    return (
        <div>
            <div style={{marginBottom: '16px'}}>
                {/*<h3 style={{margin: 0}}>关联的工具</h3>*/}
                <div style={{fontSize: '12px', color: '#666', marginTop: '4px'}}>
                    关联官方工具，让Agent具备相关工具能力
                </div>
            </div>

            <List
                dataSource={tools}
                renderItem={(item) => (
                    <List.Item>
                        <Card
                            size="small"
                            style={{width: '100%'}}
                            bodyStyle={{padding: '12px'}}
                        >
                            <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start'}}>
                                <div style={{flex: 1}}>
                                    <div style={{display: 'flex', alignItems: 'center', marginBottom: '8px'}}>
                                        {getToolIcon(item.type)}
                                        <h4 style={{margin: '0 0 0 8px', fontSize: '14px', fontWeight: 600}}>
                                            {item.name}
                                        </h4>
                                        <Tag
                                            color={getTypeColor(item.type)}
                                            style={{marginLeft: '8px', fontSize: '11px'}}
                                        >
                                            {item.type}
                                        </Tag>
                                        <Tag
                                            color={getStatusColor(item.status)}
                                            style={{marginLeft: '4px', fontSize: '11px'}}
                                        >
                                            {getStatusText(item.status)}
                                        </Tag>
                                    </div>

                                    <p style={{
                                        margin: '0 0 8px 0',
                                        fontSize: '12px',
                                        color: '#666',
                                        lineHeight: '1.4'
                                    }}>
                                        {item.description}
                                    </p>

                                    <div style={{fontSize: '11px', color: '#999'}}>
                                        最后使用: {item.lastUsed}
                                    </div>
                                </div>

                                <div style={{display: 'flex', flexDirection: 'column', gap: '4px', marginLeft: '12px'}}>
                                    <Tooltip title="测试">
                                        <Button
                                            type="text"
                                            icon={<PlayCircleOutlined/>}
                                            size="small"
                                            onClick={() => handleTest(item)}
                                        />
                                    </Tooltip>
                                    <Tooltip title="查看">
                                        <Button
                                            type="text"
                                            icon={<EyeOutlined/>}
                                            size="small"
                                            onClick={() => handleView(item)}
                                        />
                                    </Tooltip>
                                    <Popconfirm
                                        title="确定要解绑此工具吗？"
                                        description="解绑后Agent将无法使用此工具"
                                        onConfirm={() => handleUnbind(item)}
                                        okText="确定"
                                        cancelText="取消"
                                    >
                                        <Tooltip title="解绑">
                                            <Button
                                                type="text"
                                                icon={<DisconnectOutlined/>}
                                                size="small"
                                                danger
                                            />
                                        </Tooltip>
                                    </Popconfirm>
                                </div>
                            </div>
                        </Card>
                    </List.Item>
                )}
                style={{marginTop: '8px'}}
            />

            <AddResourceModal
                visible={modalVisible}
                type={modalType}
                agentId={agentId}
                onClose={handleModalClose}
                onSuccess={handleResourceAdded}
            />
        </div>
    );
};

export default AgentToolBinding;
