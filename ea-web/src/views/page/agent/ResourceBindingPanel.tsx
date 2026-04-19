import React, {useState, useEffect, useCallback} from 'react';
import type {CollapseProps} from 'antd';
import {Collapse, Button, Empty, Spin, message} from 'antd';
import {PlusCircleFilled, ReloadOutlined, AppstoreOutlined} from '@ant-design/icons';
import AgentKnowledgeBinding from './knowledge/AgentKnowledgeBinding';
import AgentToolBinding from './tool/AgentToolBinding';
import MCPSkillList from './mcp/MCPSkillList';
import SkillList from './skill/SkillList';
import SkillSelector from './skill/SkillSelector';
import AddResourceModal from './common/AddResourceModal';
import {knowledgeBaseApi} from '../../api/KnowledgeBaseApi';
import {eaToolApi} from '../../api/EaToolApi';
import {mcpApi} from '../../api/McpApi';
import {skillApi} from '../../api/SkillApi';

interface ResourceBindingPanelProps {
    agentId?: number;
    className?: string;
}

type ResourceType = 'knowledge' | 'tool' | 'mcp' | 'skill';

const ResourceBindingPanel: React.FC<ResourceBindingPanelProps> = ({agentId, className}) => {
    const [activeKeys, setActiveKeys] = useState<string[]>([]); // 默认全部折叠
    const [isLoading, setIsLoading] = useState(false);
    const [modalVisible, setModalVisible] = useState(false);
    const [skillSelectorVisible, setSkillSelectorVisible] = useState(false);
    const [modalType, setModalType] = useState<ResourceType>('knowledge');
    const [refreshKey, setRefreshKey] = useState(0);

    // 资源数量统计
    const [resourceCounts, setResourceCounts] = useState({
        knowledge: 0,
        tool: 0,
        mcp: 0,
        skill: 0
    });

    // 加载资源数量的函数
    const loadResourceCounts = useCallback(async () => {
        if (!agentId) {
            // 如果没有 agentId，重置为0
            setResourceCounts({
                knowledge: 0,
                tool: 0,
                mcp: 0,
                skill: 0
            });
            return;
        }

        try {
            // 并行获取知识库、工具、MCP和Skill数量
            const [knowledgeResponse, toolResponse, mcpResponse, skillResponse] = await Promise.all([
                knowledgeBaseApi.listByAgentId(agentId.toString()),
                eaToolApi.listBoundToolsByAgentId(agentId.toString()),
                mcpApi.listBoundMcpByAgentId(agentId.toString()),
                skillApi.listBoundSkillsByAgentId(agentId.toString())
            ]);

            setResourceCounts({
                knowledge: knowledgeResponse.success ? (knowledgeResponse.data?.length || 0) : 0,
                tool: toolResponse.success ? (toolResponse.data?.length || 0) : 0,
                mcp: mcpResponse.success ? (mcpResponse.data?.length || 0) : 0,
                skill: skillResponse.success ? (skillResponse.data?.length || 0) : 0
            });
        } catch (error) {
            console.error('加载资源数量失败:', error);
            // 出错时保持当前状态
        }
    }, [agentId]);

    // 当 agentId 变化时，获取资源数量
    useEffect(() => {
        loadResourceCounts();
    }, [loadResourceCounts]);

    // 处理 Collapse 变化
    const handleCollapseChange = (keys: string | string[]) => {
        setActiveKeys(Array.isArray(keys) ? keys : [keys]);
    };


    // 处理知识库数量更新
    const handleKnowledgeCountUpdate = (count: number) => {
        setResourceCounts(prev => ({
            ...prev,
            knowledge: count
        }));
    };

    // 处理工具数量更新
    const handleToolCountUpdate = (count: number) => {
        setResourceCounts(prev => ({
            ...prev,
            tool: count
        }));
    };

    // 处理 MCP 数量更新
    const handleMCPCountUpdate = (count: number) => {
        setResourceCounts(prev => ({
            ...prev,
            mcp: count
        }));
    };

    // 处理 Skill 数量更新
    const handleSkillCountUpdate = (count: number) => {
        setResourceCounts(prev => ({
            ...prev,
            skill: count
        }));
    };

    // 刷新所有资源
    const handleRefreshAll = () => {
        setIsLoading(true);
        setRefreshKey(prev => prev + 1);

        // 重新获取资源数量
        loadResourceCounts();

        // 模拟加载
        setTimeout(() => {
            setIsLoading(false);
            message.success('资源已刷新');
        }, 500);
    };

    // 打开添加资源模态框
    const handleAddResource = (type: ResourceType) => {
        if (type === 'skill') {
            setSkillSelectorVisible(true);
        } else {
            setModalType(type);
            setModalVisible(true);
        }
    };

    // 关闭模态框
    const handleModalClose = () => {
        setModalVisible(false);
    };

    // 处理资源添加成功
    const handleResourceAdded = (type: ResourceType) => {
        setModalVisible(false);
        setRefreshKey(prev => prev + 1);
        message.success(`${getResourceTypeName(type)}添加成功`);

        // 重新获取资源数量
        loadResourceCounts();
    };

    // 获取资源类型名称
    const getResourceTypeName = (type: ResourceType): string => {
        switch (type) {
            case 'knowledge':
                return '知识库';
            case 'tool':
                return '工具';
            case 'mcp':
                return 'MCP';
            default:
                return '资源';
        }
    };

    // Collapse 配置
    const items: CollapseProps['items'] = [
        {
            key: 'knowledge',
            label: (
                <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
                    <div style={{display: 'flex', alignItems: 'center', gap: '8px'}}>
                        <span>知识库</span>
                        <span style={{
                            background: resourceCounts.knowledge > 0 ? '#1890ff' : '#f0f0f0',
                            color: resourceCounts.knowledge > 0 ? '#fff' : '#666',
                            padding: '1px 6px',
                            borderRadius: '4px',
                            fontSize: '11px',
                            fontWeight: 'bold',
                            minWidth: '18px',
                            textAlign: 'center',
                            display: 'inline-block'
                        }}>
                            {resourceCounts.knowledge}
                        </span>
                    </div>
                    <Button
                        type="text"
                        icon={<PlusCircleFilled style={{color: '#1890ff', fontSize: '16px'}}/>}
                        size="small"
                        onClick={(e) => {
                            e.stopPropagation();
                            handleAddResource('knowledge');
                        }}
                        title="添加知识库"
                    />
                </div>
            ),
            children: (
                <AgentKnowledgeBinding
                    agentId={agentId}
                    refreshKey={refreshKey}
                    onRefresh={() => setRefreshKey(prev => prev + 1)}
                    onCountUpdate={handleKnowledgeCountUpdate}
                />
            ),
            extra: <span style={{fontSize: '12px', color: '#666'}}>文档、文件等知识资源</span>
        },
        {
            key: 'tool',
            label: (
                <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
                    <div style={{display: 'flex', alignItems: 'center', gap: '8px'}}>
                        <span>工具</span>
                        <span style={{
                            background: resourceCounts.tool > 0 ? '#52c41a' : '#f0f0f0',
                            color: resourceCounts.tool > 0 ? '#fff' : '#666',
                            padding: '1px 6px',
                            borderRadius: '4px',
                            fontSize: '11px',
                            fontWeight: 'bold',
                            minWidth: '18px',
                            textAlign: 'center',
                            display: 'inline-block'
                        }}>
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
                        <span style={{
                            background: resourceCounts.mcp > 0 ? '#fa8c16' : '#f0f0f0',
                            color: resourceCounts.mcp > 0 ? '#fff' : '#666',
                            padding: '1px 6px',
                            borderRadius: '4px',
                            fontSize: '11px',
                            fontWeight: 'bold',
                            minWidth: '18px',
                            textAlign: 'center',
                            display: 'inline-block'
                        }}>
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
                        <span style={{
                            background: resourceCounts.skill > 0 ? '#722ed1' : '#f0f0f0',
                            color: resourceCounts.skill > 0 ? '#fff' : '#666',
                            padding: '1px 6px',
                            borderRadius: '4px',
                            fontSize: '11px',
                            fontWeight: 'bold',
                            minWidth: '18px',
                            textAlign: 'center',
                            display: 'inline-block'
                        }}>
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

    return (
        <div className={className} style={{
            display: 'flex',
            flexDirection: 'column',
            height: '100%',
            background: '#fff',
            borderRadius: '8px'
        }}>
            {/* 顶部操作栏 */}
            <div style={{
                padding: '16px 16px 8px 16px',
                borderBottom: '1px solid #f0f0f0',
                flexShrink: 0
            }}>
                <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
                    <div>
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
                            管理Agent可用的知识库、工具和MCP技能
                        </p>
                    </div>
                    <Button
                        icon={<ReloadOutlined/>}
                        onClick={handleRefreshAll}
                        loading={isLoading}
                        size="small"
                    >
                        刷新
                    </Button>
                </div>
            </div>

            {/* 资源列表 */}
            <div style={{flex: 1, overflow: 'auto', padding: '16px'}}>
                {isLoading ? (
                    <div style={{display: 'flex', justifyContent: 'center', alignItems: 'center', height: '200px'}}>
                        <Spin tip="加载资源中..."/>
                    </div>
                ) : (
                    <Collapse
                        items={items}
                        activeKey={activeKeys}
                        onChange={handleCollapseChange}
                        bordered={false}
                        size="small"
                        style={{background: 'transparent'}}
                    />
                )}
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
