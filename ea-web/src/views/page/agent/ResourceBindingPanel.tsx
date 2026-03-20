import React, { useState, useEffect } from 'react';
import type { CollapseProps } from 'antd';
import { Collapse, Button, Empty, Spin, message } from 'antd';
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import KnowledgeBaseList from './knowledge/KnowledgeBaseList';
import ToolList from './tool/ToolList';
import MCPSkillList from './mcp/MCPSkillList';
import AddResourceModal from './common/AddResourceModal';

interface ResourceBindingPanelProps {
    agentId?: number;
    className?: string;
}

type ResourceType = 'knowledge' | 'tool' | 'mcp';

const ResourceBindingPanel: React.FC<ResourceBindingPanelProps> = ({ agentId, className }) => {
    const [activeKeys, setActiveKeys] = useState<string[]>(['knowledge', 'tool', 'mcp']);
    const [isLoading, setIsLoading] = useState(false);
    const [modalVisible, setModalVisible] = useState(false);
    const [modalType, setModalType] = useState<ResourceType>('knowledge');
    const [refreshKey, setRefreshKey] = useState(0);

    // 处理Collapse变化
    const handleCollapseChange = (keys: string | string[]) => {
        setActiveKeys(Array.isArray(keys) ? keys : [keys]);
    };

    // 刷新所有资源
    const handleRefreshAll = () => {
        setIsLoading(true);
        setRefreshKey(prev => prev + 1);

        // 模拟加载
        setTimeout(() => {
            setIsLoading(false);
            message.success('资源已刷新');
        }, 500);
    };

    // 打开添加资源模态框
    const handleAddResource = (type: ResourceType) => {
        setModalType(type);
        setModalVisible(true);
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
    };

    // 获取资源类型名称
    const getResourceTypeName = (type: ResourceType): string => {
        switch (type) {
            case 'knowledge': return '知识库';
            case 'tool': return '工具';
            case 'mcp': return 'MCP Skill';
            default: return '资源';
        }
    };

    // Collapse配置
    const items: CollapseProps['items'] = [
        {
            key: 'knowledge',
            label: (
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span>知识库</span>
                    <Button
                        type="text"
                        icon={<PlusOutlined />}
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
                <KnowledgeBaseList
                    agentId={agentId}
                    refreshKey={refreshKey}
                    onRefresh={() => setRefreshKey(prev => prev + 1)}
                />
            ),
            extra: <span style={{ fontSize: '12px', color: '#666' }}>文档、文件等知识资源</span>
        },
        {
            key: 'tool',
            label: (
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span>工具</span>
                    <Button
                        type="text"
                        icon={<PlusOutlined />}
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
                <ToolList
                    agentId={agentId}
                    refreshKey={refreshKey}
                    onRefresh={() => setRefreshKey(prev => prev + 1)}
                />
            ),
            extra: <span style={{ fontSize: '12px', color: '#666' }}>API、SQL、HTTP等工具</span>
        },
        {
            key: 'mcp',
            label: (
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span>MCP Skills</span>
                    <Button
                        type="text"
                        icon={<PlusOutlined />}
                        size="small"
                        onClick={(e) => {
                            e.stopPropagation();
                            handleAddResource('mcp');
                        }}
                        title="添加MCP Skill"
                    />
                </div>
            ),
            children: (
                <MCPSkillList
                    agentId={agentId}
                    refreshKey={refreshKey}
                    onRefresh={() => setRefreshKey(prev => prev + 1)}
                />
            ),
            extra: <span style={{ fontSize: '12px', color: '#666' }}>模型上下文协议技能</span>
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
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div>
                        <h3 style={{ margin: 0, fontSize: '16px', fontWeight: 600 }}>资源绑定</h3>
                        <p style={{ margin: '4px 0 0 0', fontSize: '12px', color: '#666' }}>
                            管理Agent可用的知识库、工具和MCP技能
                        </p>
                    </div>
                    <Button
                        icon={<ReloadOutlined />}
                        onClick={handleRefreshAll}
                        loading={isLoading}
                        size="small"
                    >
                        刷新
                    </Button>
                </div>
            </div>

            {/* 资源列表 */}
            <div style={{ flex: 1, overflow: 'auto', padding: '16px' }}>
                {isLoading ? (
                    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '200px' }}>
                        <Spin tip="加载资源中..." />
                    </div>
                ) : (
                    <Collapse
                        items={items}
                        activeKey={activeKeys}
                        onChange={handleCollapseChange}
                        bordered={false}
                        size="small"
                        style={{ background: 'transparent' }}
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
        </div>
    );
};

export default ResourceBindingPanel;