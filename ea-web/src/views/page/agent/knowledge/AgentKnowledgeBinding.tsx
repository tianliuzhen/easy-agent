import React, {useState, useEffect} from 'react';
import {List, Card, Tag, Button, Empty, Spin, Tooltip, message, Popconfirm} from 'antd';
import {FileTextOutlined, EyeOutlined, DownloadOutlined, DisconnectOutlined, PlusOutlined} from '@ant-design/icons';
import {knowledgeBaseApi} from '../../../api/KnowledgeBaseApi';
import AddResourceModal from '../common/AddResourceModal';

interface KnowledgeBaseItem {
    id: number;
    name: string;
    description: string;
    type: string;
    fileCount: number;
    status: 'active' | 'inactive';
    lastUpdated: string;
    isLinked: boolean;
}

interface AgentKnowledgeBindingProps {
    agentId?: number;
    refreshKey?: number;
    onRefresh?: () => void;
    onCountUpdate?: (count: number) => void;
}

const AgentKnowledgeBinding: React.FC<AgentKnowledgeBindingProps> = ({
                                                                         agentId,
                                                                         refreshKey,
                                                                         onRefresh,
                                                                         onCountUpdate
                                                                     }) => {
    const [knowledgeBases, setKnowledgeBases] = useState<KnowledgeBaseItem[]>([]);
    const [loading, setLoading] = useState(false);
    const [modalVisible, setModalVisible] = useState(false);
    const [modalType, setModalType] = useState<'knowledge'>('knowledge');

    // 当 agentId 变化时加载数据
    useEffect(() => {
        if (agentId) {
            loadKnowledgeBases();
        } else {
            // 如果没有 agentId，清空数据
            setKnowledgeBases([]);
            if (onCountUpdate) {
                onCountUpdate(0);
            }
        }
    }, [agentId]); // 监听 agentId 变化

    // 当 refreshKey 变化时重新加载
    useEffect(() => {
        if (refreshKey > 0) {
            loadKnowledgeBases();
        }
    }, [refreshKey]);

    // 当知识库列表变化时，通知父组件更新数量
    useEffect(() => {
        if (onCountUpdate) {
            onCountUpdate(knowledgeBases.length);
        }
    }, [knowledgeBases, onCountUpdate]);

    const loadKnowledgeBases = async () => {
        if (!agentId) return;

        setLoading(true);
        try {
            // 根据Agent ID加载关联的知识库
            const response = await knowledgeBaseApi.listByAgentId(agentId.toString());
            if (response.success) {
                const knowledgeBases = response.data.map((item: any) => ({
                    id: item.id,
                    name: item.kbName || item.fileName,
                    description: item.description || item.kbDesc,
                    type: item.type || '文档',
                    fileCount: item.fileCount || 0,
                    status: item.status === 1 ? 'active' : 'inactive',
                    lastUpdated: item.updateTime || item.createTime,
                    isLinked: true
                }));
                setKnowledgeBases(knowledgeBases);
            } else {
                message.error(`加载知识库失败: ${response.message}`);
                setKnowledgeBases([]);
            }
        } catch (error) {
            console.error('加载知识库失败:', error);
            message.error('加载知识库失败');
            setKnowledgeBases([]);
        } finally {
            setLoading(false);
        }
    };

    // 查看知识库详情
    const handleView = (item: KnowledgeBaseItem) => {
        message.info(`查看知识库: ${item.name}`);
        // 这里可以打开详情模态框或跳转到详情页
    };

    // 解绑知识库
    const handleUnbind = async (item: KnowledgeBaseItem) => {
        if (!agentId) {
            message.error('无法解绑：缺少Agent ID');
            return;
        }

        try {
            const request = {
                agentId: agentId.toString(),
                knowledgeBaseId: item.id
            };

            const response = await knowledgeBaseApi.unbind(request);
            if (response.success) {
                message.success(`已解绑知识库: ${item.name}`);
                if (onRefresh) {
                    onRefresh();
                }
                // 重新加载列表
                loadKnowledgeBases();
            } else {
                message.error(`解绑失败: ${response.message}`);
            }
        } catch (error) {
            console.error('解绑知识库失败:', error);
            message.error('解绑知识库失败');
        }
    };

    // 下载知识库
    const handleDownload = (item: KnowledgeBaseItem) => {
        message.info(`下载知识库: ${item.name}`);
        // 这里可以实现下载逻辑
    };

    // 打开添加资源模态框
    const handleAddResource = () => {
        setModalType('knowledge');
        setModalVisible(true);
    };

    // 关闭模态框
    const handleModalClose = () => {
        setModalVisible(false);
    };

    // 处理资源添加成功
    const handleResourceAdded = (type: 'knowledge') => {
        setModalVisible(false);
        if (onRefresh) {
            onRefresh();
        }
        // 重新加载列表
        loadKnowledgeBases();
        message.success('知识库关联成功');
    };

    // 获取状态标签颜色
    const getStatusColor = (status: string) => {
        return status === 'active' ? 'green' : 'red';
    };

    // 获取类型标签颜色
    const getTypeColor = (type: string) => {
        const typeColors: Record<string, string> = {
            '文档': 'blue',
            '问答': 'purple',
            '技术文档': 'orange',
            '销售资料': 'cyan',
            '默认': 'default'
        };
        return typeColors[type] || 'default';
    };

    // 渲染空状态
    const renderEmptyState = () => {
        return (
            <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description={
                    <div>
                        <div style={{marginBottom: '8px'}}>暂无关联的知识库</div>
                        <Button type="primary" icon={<PlusOutlined/>} onClick={handleAddResource}>
                            关联知识库
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
                <Spin tip="加载知识库中..."/>
            </div>
        );
    }

    if (knowledgeBases.length === 0) {
        return (
            <div>
                <div style={{marginBottom: '16px'}}>
                    <h3 style={{margin: 0}}>关联的知识库</h3>
                    <div style={{fontSize: '12px', color: '#666', marginTop: '4px'}}>
                        关联外部知识库，让Agent具备相关知识能力
                    </div>
                </div>
                {renderEmptyState()}
                <AddResourceModal
                    visible={modalVisible}
                    type={modalType}
                    agentId={agentId}
                    onClose={handleModalClose}
                    onSuccess={handleResourceAdded}
                    allowCreateKnowledge={false}
                />
            </div>
        );
    }

    return (
        <div>
            <div style={{marginBottom: '16px'}}>
                {/*<h3 style={{ margin: 0 }}>关联的知识库</h3>*/}
                <div style={{fontSize: '12px', color: '#666', marginTop: '4px'}}>
                    关联外部知识库，让Agent具备相关知识能力
                </div>
            </div>

            <List
                dataSource={knowledgeBases}
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
                                        <FileTextOutlined style={{marginRight: '8px', color: '#1890ff'}}/>
                                        <h4 style={{margin: 0, fontSize: '14px', fontWeight: 600}}>
                                            {item.name}
                                        </h4>
                                        <Tag
                                            color={getStatusColor(item.status)}
                                            style={{marginLeft: '8px', fontSize: '11px'}}
                                        >
                                            {item.status === 'active' ? '启用' : '停用'}
                                        </Tag>
                                        <Tag
                                            color={getTypeColor(item.type)}
                                            style={{marginLeft: '4px', fontSize: '11px'}}
                                        >
                                            {item.type}
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

                                    <div style={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        fontSize: '11px',
                                        color: '#999'
                                    }}>
                                        <span style={{marginRight: '12px'}}>
                                            文件数: <strong>{item.fileCount}</strong>
                                        </span>
                                        <span>
                                            更新: {item.lastUpdated}
                                        </span>
                                    </div>
                                </div>

                                <div style={{display: 'flex', flexDirection: 'column', gap: '4px', marginLeft: '12px'}}>
                                    <Tooltip title="查看详情">
                                        <Button
                                            type="text"
                                            icon={<EyeOutlined/>}
                                            size="small"
                                            onClick={() => handleView(item)}
                                        />
                                    </Tooltip>
                                    <Tooltip title="下载">
                                        <Button
                                            type="text"
                                            icon={<DownloadOutlined/>}
                                            size="small"
                                            onClick={() => handleDownload(item)}
                                        />
                                    </Tooltip>
                                    <Tooltip title="解绑">
                                        <Popconfirm
                                            title={`确定要解绑知识库 "${item.name}" 吗？`}
                                            onConfirm={() => handleUnbind(item)}
                                            okText="确定"
                                            cancelText="取消"
                                        >
                                            <Button
                                                type="text"
                                                icon={<DisconnectOutlined/>}
                                                size="small"
                                                danger
                                            />
                                        </Popconfirm>
                                    </Tooltip>
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
                allowCreateKnowledge={false}
            />
        </div>
    );
};

export default AgentKnowledgeBinding;
