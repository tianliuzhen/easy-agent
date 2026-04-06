import React, { useState, useEffect, useCallback, useRef } from 'react';
import { List, Card, Tag, Button, Empty, Spin, Tooltip, message, Popconfirm } from 'antd';
import { ThunderboltOutlined, SettingOutlined, DisconnectOutlined, StarOutlined, ToolOutlined } from '@ant-design/icons';
import { skillApi } from '../../../api/SkillApi';

interface SkillItem {
    id: number;
    skillName: string;
    skillDisplayName: string;
    skillDescription: string;
    skillType: string;
    skillCategory: string;
    skillIcon: string;
    skillVersion: string;
    skillProvider: string;
    skillCapabilities: string[];
    status: 'active' | 'inactive' | 'error';
    lastExecutedAt: string;
    createdAt: string;
    updatedAt: string;
}

interface SkillListProps {
    agentId?: number;
    refreshKey?: number;
    onRefresh?: () => void;
    onCountUpdate?: (count: number) => void;
}

const SkillList: React.FC<SkillListProps> = ({
    agentId,
    refreshKey,
    onRefresh,
    onCountUpdate
}) => {
    const [skills, setSkills] = useState<SkillItem[]>([]);
    const [loading, setLoading] = useState(false);
    const isFirstLoad = useRef(true);
    const prevAgentId = useRef(agentId);

    // 加载技能列表
    const loadSkills = useCallback(async () => {
        if (!agentId) return;

        setLoading(true);
        try {
            // 根据 Agent ID 加载关联的 Skill
            const response = await skillApi.listBoundSkillsByAgentId(agentId.toString());
            if (response.success) {
                const mappedSkills = response.data.map((item: any) => ({
                    id: item.id,
                    skillName: item.skillName || `skill-${item.id}`,
                    skillDisplayName: item.skillDisplayName || item.skillName || `Skill-${item.id}`,
                    skillDescription: item.skillDescription || '无描述',
                    skillType: item.skillType || 'INTERNAL',
                    skillCategory: item.skillCategory || 'general',
                    skillIcon: item.skillIcon || '🔧',
                    skillVersion: item.skillVersion || '1.0.0',
                    skillProvider: item.skillProvider || 'System',
                    skillCapabilities: item.skillCapabilities || [],
                    status: item.status === 'active' ? 'active' : 'inactive',
                    lastExecutedAt: item.lastExecutedAt,
                    createdAt: item.createdAt,
                    updatedAt: item.updatedAt
                }));
                setSkills(mappedSkills);
                // 直接在这里通知父组件更新数量，避免额外的 useEffect
                if (onCountUpdate) {
                    onCountUpdate(mappedSkills.length);
                }
            } else {
                message.error(`加载 Skill 失败: ${response.message}`);
                setSkills([]);
                if (onCountUpdate) {
                    onCountUpdate(0);
                }
            }
        } catch (error) {
            console.error('加载 Skill 失败:', error);
            message.error('加载 Skill 失败');
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
            'error': 'red'
        };
        return statusColors[status] || 'default';
    };

    // 获取状态文本
    const getStatusText = (status: string) => {
        const statusTexts: Record<string, string> = {
            'active': '已启用',
            'inactive': '已停用',
            'error': '错误'
        };
        return statusTexts[status] || status;
    };

    // 获取分类标签颜色
    const getCategoryColor = (category: string) => {
        const categoryColors: Record<string, string> = {
            'general': 'blue',
            'development': 'purple',
            'data': 'cyan',
            'media': 'magenta'
        };
        return categoryColors[category] || 'default';
    };

    // 获取分类文本
    const getCategoryText = (category: string) => {
        const categoryTexts: Record<string, string> = {
            'general': '通用',
            'development': '开发',
            'data': '数据',
            'media': '媒体'
        };
        return categoryTexts[category] || category;
    };

    // 获取类型标签颜色
    const getTypeColor = (type: string) => {
        const typeColors: Record<string, string> = {
            'INTERNAL': 'green',
            'EXTERNAL': 'orange',
            'PLUGIN': 'blue'
        };
        return typeColors[type] || 'default';
    };

    // 获取类型文本
    const getTypeText = (type: string) => {
        const typeTexts: Record<string, string> = {
            'INTERNAL': '内置',
            'EXTERNAL': '外部',
            'PLUGIN': '插件'
        };
        return typeTexts[type] || type;
    };

    // 配置技能
    const handleConfigure = useCallback((item: SkillItem) => {
        message.info(`配置技能: ${item.skillDisplayName}`);
        // 这里可以打开配置模态框
    }, []);

    // 解绑 Skill
    const handleUnbind = useCallback(async (item: SkillItem) => {
        if (!agentId) {
            message.error('无法解绑：缺少 Agent ID');
            return;
        }

        try {
            const request = {
                agentId: agentId.toString(),
                skillConfigId: item.id
            };

            const response = await skillApi.unbindSkill(request);
            if (response.success) {
                message.success(`已解绑 Skill: ${item.skillDisplayName}`);
                if (onRefresh) {
                    onRefresh();
                }
                // 重新加载列表
                await loadSkills();
            } else {
                message.error(`解绑失败: ${response.message}`);
            }
        } catch (error) {
            console.error('解绑 Skill 失败:', error);
            message.error('解绑 Skill 失败');
        }
    }, [agentId, loadSkills, onRefresh]);

    // 测试技能
    const handleTest = useCallback((item: SkillItem) => {
        message.info(`测试技能: ${item.skillDisplayName}`);
        // 这里可以打开测试模态框
    }, []);

    // 渲染能力标签
    const renderCapabilities = (capabilities: string[]) => {
        return (
            <div style={{ marginTop: '8px', display: 'flex', flexWrap: 'wrap', gap: '4px' }}>
                {capabilities.slice(0, 3).map((cap, index) => (
                    <Tag key={index} color="blue" style={{ fontSize: '10px', padding: '0 4px' }}>
                        {cap}
                    </Tag>
                ))}
                {capabilities.length > 3 && (
                    <Tag style={{ fontSize: '10px', padding: '0 4px' }}>
                        +{capabilities.length - 3}
                    </Tag>
                )}
            </div>
        );
    };

    if (loading) {
        return (
            <div style={{ display: 'flex', justifyContent: 'center', padding: '40px 0' }}>
                <Spin tip="加载 Skill 技能中..." />
            </div>
        );
    }

    if (skills.length === 0) {
        return (
            <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description="暂无 Skill 技能"
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
                                    <span style={{ fontSize: '20px', marginRight: '8px' }}>{item.skillIcon}</span>
                                    <h4 style={{ margin: 0, fontSize: '14px', fontWeight: 600 }}>
                                        {item.skillDisplayName}
                                    </h4>
                                    <Tag
                                        color={getStatusColor(item.status)}
                                        style={{ marginLeft: '8px', fontSize: '11px' }}
                                    >
                                        {getStatusText(item.status)}
                                    </Tag>
                                    <Tag
                                        color={getCategoryColor(item.skillCategory)}
                                        style={{ marginLeft: '4px', fontSize: '11px' }}
                                    >
                                        {getCategoryText(item.skillCategory)}
                                    </Tag>
                                    <Tag
                                        color={getTypeColor(item.skillType)}
                                        style={{ marginLeft: '4px', fontSize: '11px' }}
                                    >
                                        {getTypeText(item.skillType)}
                                    </Tag>
                                    <Tag style={{ marginLeft: '4px', fontSize: '11px' }}>
                                        v{item.skillVersion}
                                    </Tag>
                                </div>

                                <p style={{
                                    margin: '0 0 8px 0',
                                    fontSize: '12px',
                                    color: '#666',
                                    lineHeight: '1.4'
                                }}>
                                    {item.skillDescription}
                                </p>

                                {renderCapabilities(item.skillCapabilities)}

                                <div style={{ marginTop: '8px', fontSize: '11px', color: '#999' }}>
                                    提供者: {item.skillProvider} |
                                    更新: {item.updatedAt ? new Date(item.updatedAt).toLocaleString() : '从未使用'}
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
                                    title="确定要解绑此 Skill 吗？"
                                    description="解绑后 Agent 将无法使用此 Skill"
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

export default SkillList;