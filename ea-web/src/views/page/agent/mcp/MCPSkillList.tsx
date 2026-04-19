import React, { useState, useEffect, useCallback, useRef } from 'react';
import { List, Card, Tag, Button, Empty, Spin, Tooltip, message, Popconfirm } from 'antd';
import { CodeOutlined, RobotOutlined, DeleteOutlined, SettingOutlined, ThunderboltOutlined, DisconnectOutlined } from '@ant-design/icons';
import { mcpApi } from '../../../api/McpApi';

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
    const isFirstLoad = useRef(true);
    const prevAgentId = useRef(agentId);

    // 加载技能列表
    const loadSkills = useCallback(async () => {
        if (!agentId) return;

        setLoading(true);
        try {
            // 根据Agent ID加载关联的MCP
            const response = await mcpApi.listBoundMcpByAgentId(agentId.toString());
            if (response.success) {
                const mappedSkills = response.data.map((item: any) => ({
                    id: item.id,
                    name: item.serverName || item.name || `MCP-${item.id}`,
                    description: item.description || item.toolDescription || '无描述',
                    provider: item.provider || 'Unknown',
                    version: item.version || '1.0.0',
                    status: item.status === 'active' ? 'active' : 'inactive',
                    capabilities: item.capabilities || [],
                    lastUpdated: item.updatedAt || item.createdAt || '从未使用'
                }));
                setSkills(mappedSkills);
                // 直接在这里通知父组件更新数量，避免额外的useEffect
                if (onCountUpdate) {
                    onCountUpdate(mappedSkills.length);
                }
            } else {
                message.error(`加载MCP失败: ${response.message}`);
                setSkills([]);
                if (onCountUpdate) {
                    onCountUpdate(0);
                }
            }
        } catch (error) {
            console.error('加载MCP失败:', error);
            message.error('加载MCP失败');
            setSkills([]);
            if (onCountUpdate) {
                onCountUpdate(0);
            }
        } finally {
            setLoading(false);
        }
    }, [agentId, onCountUpdate]);

    // 当 agentId 变化时加载数据
    useEffect(() => {
        // 只在 agentId 真正变化时加载，或者首次加载
        if (agentId !== prevAgentId.current || isFirstLoad.current) {
            isFirstLoad.current = false;
            prevAgentId.current = agentId;

            if (agentId !== undefined && agentId !== null) {
                loadSkills();
            } else {
                // 如果没有 agentId，清空数据
                setSkills([]);
                if (onCountUpdate) {
                    onCountUpdate(0);
                }
            }
        }
    }, [agentId, loadSkills, onCountUpdate]);

    // 当 refreshKey 变化时重新加载
    useEffect(() => {
        if (refreshKey !== undefined && refreshKey !== null && refreshKey > 0 && agentId) {
            loadSkills();
        }
    }, [refreshKey, loadSkills, agentId]);

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
    const handleConfigure = useCallback((item: MCPSkillItem) => {
        message.info(`配置技能: ${item.name}`);
        // 这里可以打开配置模态框
    }, []);

    // 解绑MCP
    const handleUnbind = useCallback(async (item: MCPSkillItem) => {
        if (!agentId) {
            message.error('无法解绑：缺少Agent ID');
            return;
        }

        try {
            const request = {
                agentId: agentId.toString(),
                mcpConfigId: item.id
            };

            const response = await mcpApi.unbindMcp(request);
            if (response.success) {
                message.success(`已解绑MCP: ${item.name}`);
                if (onRefresh) {
                    onRefresh();
                }
                // 重新加载列表
                await loadSkills();
            } else {
                message.error(`解绑失败: ${response.message}`);
            }
        } catch (error) {
            console.error('解绑MCP失败:', error);
            message.error('解绑MCP失败');
        }
    }, [agentId, loadSkills, onRefresh]);

    // 测试技能
    const handleTest = useCallback((item: MCPSkillItem) => {
        message.info(`测试技能: ${item.name}`);
        // 这里可以打开测试模态框
    }, []);

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
                                <Popconfirm
                                    title="确定要解绑此MCP吗？"
                                    description="解绑后Agent将无法使用此MCP"
                                    onConfirm={() => handleUnbind(item)}
                                    okText="确定"
                                    cancelText="取消"
                                >
                                    <Tooltip title="解绑">
                                        <Button
                                            type="text"
                                            icon={<DisconnectOutlined />}
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
            style={{ marginTop: '8px' }}
        />
    );
};

export default MCPSkillList;