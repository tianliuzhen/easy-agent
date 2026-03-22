import React, { useState, useEffect } from 'react';
import { List, Card, Tag, Button, Empty, Spin, Tooltip, message } from 'antd';
import { CodeOutlined, RobotOutlined, DeleteOutlined, SettingOutlined, ThunderboltOutlined } from '@ant-design/icons';

interface MCPSkillItem {
    id: number;
    name: string;
    description: string;
    provider: string;
    version: string;
    status: 'active' | 'inactive' | 'loading';
    capabilities: string[];
    lastUpdated: string;
}

interface MCPSkillListProps {
    agentId?: number;
    refreshKey?: number;
    onRefresh?: () => void;
    onCountUpdate?: (count: number) => void;
}

const MCPSkillList: React.FC<MCPSkillListProps> = ({
    agentId,
    refreshKey,
    onRefresh,
    onCountUpdate
}) => {
    const [skills, setSkills] = useState<MCPSkillItem[]>([]);
    const [loading, setLoading] = useState(false);

    // 模拟MCP技能数据
    const mockSkills: MCPSkillItem[] = [
        {
            id: 1,
            name: '文件系统操作',
            description: '读写本地文件系统，支持多种文件格式',
            provider: 'Anthropic',
            version: '1.2.0',
            status: 'active',
            capabilities: ['read', 'write', 'list', 'delete'],
            lastUpdated: '2024-01-15'
        },
        {
            id: 2,
            name: 'Git操作',
            description: 'Git仓库管理，支持提交、拉取、分支等操作',
            provider: 'GitHub',
            version: '2.1.0',
            status: 'active',
            capabilities: ['clone', 'commit', 'pull', 'push', 'branch'],
            lastUpdated: '2024-01-20'
        },
        {
            id: 3,
            name: '数据库查询',
            description: 'SQL数据库查询和操作',
            provider: 'Community',
            version: '1.0.3',
            status: 'inactive',
            capabilities: ['query', 'insert', 'update', 'delete'],
            lastUpdated: '2024-01-10'
        },
        {
            id: 4,
            name: '网页搜索',
            description: '实时网页搜索和信息提取',
            provider: 'Google',
            version: '3.0.1',
            status: 'loading',
            capabilities: ['search', 'extract', 'summarize'],
            lastUpdated: '2024-02-01'
        }
    ];

    // 当 agentId 变化时加载数据
    useEffect(() => {
        if (agentId !== undefined) {
            loadSkills();
        } else {
            // 如果没有 agentId，清空数据
            setSkills([]);
            if (onCountUpdate) {
                onCountUpdate(0);
            }
        }
    }, [agentId]); // 监听 agentId 变化

    // 当 refreshKey 变化时重新加载
    useEffect(() => {
        if (refreshKey > 0) {
            loadSkills();
        }
    }, [refreshKey]);
    
    // 当技能列表变化时，通知父组件更新数量
    useEffect(() => {
        if (onCountUpdate) {
            onCountUpdate(skills.length);
        }
    }, [skills, onCountUpdate]);

    const loadSkills = async () => {
        setLoading(true);
        try {
            // 这里可以调用API获取MCP技能
            // const response = await mcpApi.getByAgentId(agentId);
            // setSkills(response.data || []);

            // 暂时使用模拟数据
            setTimeout(() => {
                setSkills(mockSkills);
                setLoading(false);
            }, 500);
        } catch (error) {
            console.error('加载MCP技能失败:', error);
            message.error('加载MCP技能失败');
            setSkills([]);
            setLoading(false);
        }
    };

    // 获取状态标签颜色
    const getStatusColor = (status: string) => {
        const statusColors: Record<string, string> = {
            'active': 'green',
            'inactive': 'orange',
            'loading': 'blue'
        };
        return statusColors[status] || 'default';
    };

    // 获取状态文本
    const getStatusText = (status: string) => {
        const statusTexts: Record<string, string> = {
            'active': '已启用',
            'inactive': '已停用',
            'loading': '加载中'
        };
        return statusTexts[status] || status;
    };

    // 获取提供商标签颜色
    const getProviderColor = (provider: string) => {
        const providerColors: Record<string, string> = {
            'Anthropic': 'purple',
            'GitHub': 'black',
            'Google': 'blue',
            'Community': 'green'
        };
        return providerColors[provider] || 'default';
    };

    // 配置技能
    const handleConfigure = (item: MCPSkillItem) => {
        message.info(`配置技能: ${item.name}`);
        // 这里可以打开配置模态框
    };

    // 删除技能
    const handleDelete = (item: MCPSkillItem) => {
        // 这里可以调用API删除技能
        message.success(`已删除技能: ${item.name}`);
        if (onRefresh) {
            onRefresh();
        }
    };

    // 测试技能
    const handleTest = (item: MCPSkillItem) => {
        message.info(`测试技能: ${item.name}`);
        // 这里可以打开测试模态框
    };

    // 渲染能力标签
    const renderCapabilities = (capabilities: string[]) => {
        return (
            <div style={{ marginTop: '8px', display: 'flex', flexWrap: 'wrap', gap: '4px' }}>
                {capabilities.map((cap, index) => (
                    <Tag key={index} color="blue" style={{ fontSize: '10px', padding: '0 4px' }}>
                        {cap}
                    </Tag>
                ))}
            </div>
        );
    };

    if (loading) {
        return (
            <div style={{ display: 'flex', justifyContent: 'center', padding: '40px 0' }}>
                <Spin tip="加载MCP技能中..." />
            </div>
        );
    }

    if (skills.length === 0) {
        return (
            <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description="暂无MCP技能"
                style={{ margin: '40px 0' }}
            />
        );
    }

    return (
        <List
            dataSource={skills}
            renderItem={(item) => (
                <List.Item>
                    <Card
                        size="small"
                        style={{ width: '100%' }}
                        bodyStyle={{ padding: '12px' }}
                    >
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                            <div style={{ flex: 1 }}>
                                <div style={{ display: 'flex', alignItems: 'center', marginBottom: '8px' }}>
                                    <RobotOutlined style={{ marginRight: '8px', color: '#722ed1' }} />
                                    <h4 style={{ margin: 0, fontSize: '14px', fontWeight: 600 }}>
                                        {item.name}
                                    </h4>
                                    <Tag
                                        color={getStatusColor(item.status)}
                                        style={{ marginLeft: '8px', fontSize: '11px' }}
                                    >
                                        {getStatusText(item.status)}
                                    </Tag>
                                    <Tag
                                        color={getProviderColor(item.provider)}
                                        style={{ marginLeft: '4px', fontSize: '11px' }}
                                    >
                                        {item.provider}
                                    </Tag>
                                    <Tag style={{ marginLeft: '4px', fontSize: '11px' }}>
                                        v{item.version}
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

                                {renderCapabilities(item.capabilities)}

                                <div style={{ marginTop: '8px', fontSize: '11px', color: '#999' }}>
                                    更新: {item.lastUpdated}
                                </div>
                            </div>

                            <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', marginLeft: '12px' }}>
                                <Tooltip title="测试">
                                    <Button
                                        type="text"
                                        icon={<ThunderboltOutlined />}
                                        size="small"
                                        onClick={() => handleTest(item)}
                                    />
                                </Tooltip>
                                <Tooltip title="配置">
                                    <Button
                                        type="text"
                                        icon={<SettingOutlined />}
                                        size="small"
                                        onClick={() => handleConfigure(item)}
                                    />
                                </Tooltip>
                                <Tooltip title="删除">
                                    <Button
                                        type="text"
                                        icon={<DeleteOutlined />}
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
            style={{ marginTop: '8px' }}
        />
    );
};

export default MCPSkillList;