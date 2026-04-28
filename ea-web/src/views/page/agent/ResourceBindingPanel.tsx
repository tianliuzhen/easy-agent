import React, {useState, useEffect, useCallback} from 'react';
import type {CollapseProps} from 'antd';
import {Collapse, Button, Empty, message, Tabs} from 'antd';
import {PlusCircleFilled, AppstoreOutlined, ToolOutlined, DatabaseOutlined, HistoryOutlined} from '@ant-design/icons';
import AgentKnowledgeBinding from './knowledge/AgentKnowledgeBinding';
import AgentToolBinding from './tool/AgentToolBinding';
import MCPSkillList from './mcp/MCPSkillList';
import SkillList from './skill/SkillList';
import SkillSelector from './skill/SkillSelector';
import AddResourceModal from './common/AddResourceModal';
import {eaToolApi} from '../../api/EaToolApi';
import {mcpApi} from '../../api/McpApi';
import {skillApi} from '../../api/SkillApi';

interface ResourceBindingPanelProps {
    agentId?: number;
    className?: string;
}

const ResourceBindingPanel: React.FC<ResourceBindingPanelProps> = ({agentId, className}) => {
    const [activeKeys, setActiveKeys] = useState<string[]>([]);
    const [activeTab, setActiveTab] = useState('capability');
    const [refreshKey, setRefreshKey] = useState(0);

    // Modal 状态
    const [modalVisible, setModalVisible] = useState(false);
    const [modalType, setModalType] = useState<'tool' | 'mcp'>('tool');
    const [skillSelectorVisible, setSkillSelectorVisible] = useState(false);

    // 能力 tab：资源数量徽标
    const [resourceCounts, setResourceCounts] = useState({
        tool: 0,
        mcp: 0,
        skill: 0
    });

    // 加载能力资源数量
    const loadResourceCounts = useCallback(async () => {
        if (!agentId) {
            setResourceCounts({tool: 0, mcp: 0, skill: 0});
            return;
        }

        try {
            const [toolResponse, mcpResponse, skillResponse] = await Promise.all([
                eaToolApi.listBoundToolsByAgentId(agentId.toString()),
                mcpApi.listBoundMcpByAgentId(agentId.toString()),
                skillApi.listBoundSkillsByAgentId(agentId.toString())
            ]);

            setResourceCounts({
                tool: toolResponse.success ? (toolResponse.data?.length || 0) : 0,
                mcp: mcpResponse.success ? (mcpResponse.data?.length || 0) : 0,
                skill: skillResponse.success ? (skillResponse.data?.length || 0) : 0
            });
        } catch (error) {
            console.error('加载资源数量失败:', error);
        }
    }, [agentId]);

    useEffect(() => {
        loadResourceCounts();
    }, [loadResourceCounts]);

    // Collapse 切换
    const handleCollapseChange = (keys: string | string[]) => {
        setActiveKeys(Array.isArray(keys) ? keys : [keys]);
    };

    // 数量更新回调
    const handleToolCountUpdate = (count: number) => {
        setResourceCounts(prev => ({...prev, tool: count}));
    };

    const handleMCPCountUpdate = (count: number) => {
        setResourceCounts(prev => ({...prev, mcp: count}));
    };

    const handleSkillCountUpdate = (count: number) => {
        setResourceCounts(prev => ({...prev, skill: count}));
    };

    // 打开添加资源
    const handleAddResource = (type: 'tool' | 'mcp' | 'skill') => {
        if (type === 'skill') {
            setSkillSelectorVisible(true);
        } else {
            setModalType(type);
            setModalVisible(true);
        }
    };

    const handleModalClose = () => {
        setModalVisible(false);
    };

    const handleResourceAdded = () => {
        setModalVisible(false);
        setRefreshKey(prev => prev + 1);
        message.success('资源添加成功');
        loadResourceCounts();
    };

    // 徽标样式
    const badgeStyle = (count: number, color: string): React.CSSProperties => ({
        background: count > 0 ? color : '#f0f0f0',
        color: count > 0 ? '#fff' : '#666',
        padding: '1px 6px',
        borderRadius: '4px',
        fontSize: '11px',
        fontWeight: 'bold',
        minWidth: '18px',
        textAlign: 'center',
        display: 'inline-block'
    });

    // 能力 tab：Collapse 配置
    const collapseItems: CollapseProps['items'] = [
        {
            key: 'tool',
            label: (
                <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
                    <div style={{display: 'flex', alignItems: 'center', gap: '8px'}}>
                        <span>工具</span>
                        <span style={badgeStyle(resourceCounts.tool, '#52c41a')}>
                            {resourceCounts.tool}
                        </span>
                    </div>
                    <Button
                        type="text"
                        icon={<PlusCircleFilled style={{color: '#1890ff', fontSize: '16px'}}/>}
                        size="small"
                        onClick={(e) => {
                            e.stopPropagation();
                            handleAddResource('tool');
                        }}
                        title="添加工具"
                    />
                </div>
            ),
            children: (
                <AgentToolBinding
                    agentId={agentId}
                    refreshKey={refreshKey}
                    onRefresh={() => setRefreshKey(prev => prev + 1)}
                    onCountUpdate={handleToolCountUpdate}
                />
            ),
            extra: <span style={{fontSize: '12px', color: '#666'}}>RPC/HTTP/SQL 等工具</span>
        },
        {
            key: 'mcp',
            label: (
                <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
                    <div style={{display: 'flex', alignItems: 'center', gap: '8px'}}>
                        <span>MCP</span>
                        <span style={badgeStyle(resourceCounts.mcp, '#fa8c16')}>
                            {resourceCounts.mcp}
                        </span>
                    </div>
                    <Button
                        type="text"
                        icon={<PlusCircleFilled style={{color: '#1890ff', fontSize: '16px'}}/>}
                        size="small"
                        onClick={(e) => {
                            e.stopPropagation();
                            handleAddResource('mcp');
                        }}
                        title="添加 MCP"
                    />
                </div>
            ),
            children: (
                <MCPSkillList
                    agentId={agentId}
                    refreshKey={refreshKey}
                    onRefresh={() => setRefreshKey(prev => prev + 1)}
                    onCountUpdate={handleMCPCountUpdate}
                />
            ),
            extra: <span style={{fontSize: '12px', color: '#666'}}>模型上下文协议</span>
        },
        {
            key: 'skill',
            label: (
                <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
                    <div style={{display: 'flex', alignItems: 'center', gap: '8px'}}>
                        <span>Skills</span>
                        <span style={badgeStyle(resourceCounts.skill, '#722ed1')}>
                            {resourceCounts.skill}
                        </span>
                    </div>
                    <Button
                        type="text"
                        icon={<PlusCircleFilled style={{color: '#1890ff', fontSize: '16px'}}/>}
                        size="small"
                        onClick={(e) => {
                            e.stopPropagation();
                            handleAddResource('skill');
                        }}
                        title="添加 Skill"
                    />
                </div>
            ),
            children: (
                <SkillList
                    agentId={agentId}
                    refreshKey={refreshKey}
                    onRefresh={() => setRefreshKey(prev => prev + 1)}
                    onCountUpdate={handleSkillCountUpdate}
                />
            ),
            extra: <span style={{fontSize: '12px', color: '#666'}}>技能配置</span>
        }
    ];

    // 顶部 Tabs
    const tabItems = [
        {
            key: 'capability',
            label: (
                <span>
                    <ToolOutlined style={{marginRight: 6}}/>
                    能力
                </span>
            ),
            children: (
                <div style={{padding: '16px 0'}}>
                    <Collapse
                        items={collapseItems}
                        activeKey={activeKeys}
                        onChange={handleCollapseChange}
                        bordered={false}
                        size="small"
                        style={{background: 'transparent'}}
                    />
                </div>
            )
        },
        {
            key: 'knowledge',
            label: (
                <span>
                    <DatabaseOutlined style={{marginRight: 6}}/>
                    知识
                </span>
            ),
            children: (
                <div style={{padding: '16px 0'}}>
                    <AgentKnowledgeBinding agentId={agentId}/>
                </div>
            )
        },
        {
            key: 'memory',
            label: (
                <span>
                    <HistoryOutlined style={{marginRight: 6}}/>
                    记忆
                </span>
            ),
            children: (
                <div style={{
                    display: 'flex',
                    justifyContent: 'center',
                    alignItems: 'center',
                    padding: '80px 0'
                }}>
                    <Empty description="记忆功能开发中..."/>
                </div>
            )
        }
    ];

    return (
        <div className={className} style={{
            display: 'flex',
            flexDirection: 'column',
            height: '100%',
            background: '#fff',
            borderRadius: '8px'
        }}>
            {/* 顶部标题 */}
            <div style={{
                padding: '16px 16px 0 16px',
                flexShrink: 0
            }}>
                <h3 style={{
                    margin: 0,
                    fontSize: '16px',
                    fontWeight: 600,
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px'
                }}>
                    <AppstoreOutlined style={{color: '#1890ff', fontSize: '18px'}}/>
                    资源绑定
                </h3>
                <p style={{margin: '4px 0 0 0', fontSize: '12px', color: '#666'}}>
                    管理 Agent 的能力、知识和记忆
                </p>
            </div>

            {/* Tabs 区域 */}
            <div style={{flex: 1, overflow: 'auto', padding: '0 16px'}}>
                <Tabs
                    activeKey={activeTab}
                    onChange={setActiveTab}
                    items={tabItems}
                    size="small"
                    style={{height: '100%'}}
                />
            </div>

            {/* 添加资源模态框 */}
            <AddResourceModal
                visible={modalVisible}
                type={modalType}
                agentId={agentId}
                onClose={handleModalClose}
                onSuccess={handleResourceAdded}
            />

            {/* Skill 选择器 */}
            <SkillSelector
                visible={skillSelectorVisible}
                agentId={agentId}
                onCancel={() => setSkillSelectorVisible(false)}
                onSuccess={() => {
                    setSkillSelectorVisible(false);
                    setRefreshKey(prev => prev + 1);
                    message.success('Skill 添加成功');
                    loadResourceCounts();
                }}
            />
        </div>
    );
};

export default ResourceBindingPanel;
