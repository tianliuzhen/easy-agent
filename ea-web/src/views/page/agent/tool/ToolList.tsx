import React, {useState, useEffect} from 'react';
import {List, Card, Tag, Button, Empty, Spin, Tooltip, message} from 'antd';
import {
    ApiOutlined,
    DatabaseOutlined,
    CloudServerOutlined,
    DeleteOutlined,
    EditOutlined,
    PlayCircleOutlined
} from '@ant-design/icons';

interface ToolItem {
    id: number;
    name: string;
    type: 'SQL' | 'HTTP' | 'MCP' | 'GRPC' | '其他';
    description: string;
    status: 'active' | 'inactive' | 'error';
    lastUsed: string;
    config?: any;
}

interface ToolListProps {
    agentId?: number;
    refreshKey?: number;
    onRefresh?: () => void;
}

const ToolList: React.FC<ToolListProps> = ({
                                               agentId,
                                               refreshKey,
                                               onRefresh
                                           }) => {
    const [tools, setTools] = useState<ToolItem[]>([]);
    const [loading, setLoading] = useState(false);

    // 模拟工具数据
    const mockTools: ToolItem[] = [
        {
            id: 1,
            name: '用户数据库查询',
            type: 'SQL',
            description: '查询用户信息和订单数据',
            status: 'active',
            lastUsed: '2024-01-15 14:30'
        },
        {
            id: 2,
            name: '天气API',
            type: 'HTTP',
            description: '获取实时天气信息',
            status: 'active',
            lastUsed: '2024-01-20 09:15'
        }
    ];

    // 加载工具
    useEffect(() => {
        loadTools();
    }, [agentId, refreshKey]);

    const loadTools = async () => {
        setLoading(true);
        try {
            // 这里可以调用API获取工具
            // const response = await toolApi.getByAgentId(agentId);
            // setTools(response.data || []);

            // 暂时使用模拟数据
            setTimeout(() => {
                setTools(mockTools);
                setLoading(false);
            }, 500);
        } catch (error) {
            console.error('加载工具失败:', error);
            message.error('加载工具失败');
            setTools([]);
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

    // 编辑工具
    const handleEdit = (item: ToolItem) => {
        message.info(`编辑工具: ${item.name}`);
        // 这里可以打开编辑模态框或跳转到编辑页
    };

    // 删除工具
    const handleDelete = (item: ToolItem) => {
        // 这里可以调用API删除工具
        message.success(`已删除工具: ${item.name}`);
        if (onRefresh) {
            onRefresh();
        }
    };

    // 测试工具
    const handleTest = (item: ToolItem) => {
        message.info(`测试工具: ${item.name}`);
        // 这里可以打开测试模态框
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
            <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description="暂无工具"
                style={{margin: '40px 0'}}
            />
        );
    }

    return (
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
                                <Tooltip title="编辑">
                                    <Button
                                        type="text"
                                        icon={<EditOutlined/>}
                                        size="small"
                                        onClick={() => handleEdit(item)}
                                    />
                                </Tooltip>
                                <Tooltip title="删除">
                                    <Button
                                        type="text"
                                        icon={<DeleteOutlined/>}
                                        size="small"
                                        danger
                                        onClick={() => handleDelete(item)}
                                    />
                                </Tooltip>
                            </div>
                        </div>
                    </Card>
                </List.Item>
            )}
            style={{marginTop: '8px'}}
        />
    );
};

export default ToolList;
