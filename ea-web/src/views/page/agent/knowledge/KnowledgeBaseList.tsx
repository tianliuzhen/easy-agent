import React, {useState, useEffect} from 'react';
import {List, Card, Tag, Button, Empty, Spin, Tooltip, message} from 'antd';
import {FileTextOutlined, DeleteOutlined, EyeOutlined, DownloadOutlined} from '@ant-design/icons';

interface KnowledgeBaseItem {
    id: number;
    name: string;
    description: string;
    type: string;
    fileCount: number;
    status: 'active' | 'inactive';
    lastUpdated: string;
}

interface KnowledgeBaseListProps {
    agentId?: number;
    refreshKey?: number;
    onRefresh?: () => void;
}

const KnowledgeBaseList: React.FC<KnowledgeBaseListProps> = ({
                                                                 agentId,
                                                                 refreshKey,
                                                                 onRefresh
                                                             }) => {
    const [knowledgeBases, setKnowledgeBases] = useState<KnowledgeBaseItem[]>([]);
    const [loading, setLoading] = useState(false);

    // 模拟知识库数据
    const mockKnowledgeBases: KnowledgeBaseItem[] = [
        {
            id: 1,
            name: '产品文档',
            description: '包含所有产品功能和API文档',
            type: '文档',
            fileCount: 24,
            status: 'active',
            lastUpdated: '2024-01-15'
        },
        {
            id: 2,
            name: '客户FAQ',
            description: '常见问题解答和解决方案',
            type: '问答',
            fileCount: 156,
            status: 'active',
            lastUpdated: '2024-01-20'
        },
    ];

    // 加载知识库
    useEffect(() => {
        loadKnowledgeBases();
    }, [agentId, refreshKey]);

    const loadKnowledgeBases = async () => {
        setLoading(true);
        try {
            // 这里可以调用API获取知识库
            // const response = await knowledgeBaseApi.getByAgentId(agentId);
            // setKnowledgeBases(response.data || []);

            // 暂时使用模拟数据
            setTimeout(() => {
                setKnowledgeBases(mockKnowledgeBases);
                setLoading(false);
            }, 500);
        } catch (error) {
            console.error('加载知识库失败:', error);
            message.error('加载知识库失败');
            setKnowledgeBases([]);
            setLoading(false);
        }
    };

    // 查看知识库详情
    const handleView = (item: KnowledgeBaseItem) => {
        message.info(`查看知识库: ${item.name}`);
        // 这里可以打开详情模态框或跳转到详情页
    };

    // 删除知识库
    const handleDelete = (item: KnowledgeBaseItem) => {
        // 这里可以调用API删除知识库
        message.success(`已删除知识库: ${item.name}`);
        if (onRefresh) {
            onRefresh();
        }
    };

    // 下载知识库
    const handleDownload = (item: KnowledgeBaseItem) => {
        message.info(`下载知识库: ${item.name}`);
        // 这里可以实现下载逻辑
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

    if (loading) {
        return (
            <div style={{display: 'flex', justifyContent: 'center', padding: '40px 0'}}>
                <Spin tip="加载知识库中..."/>
            </div>
        );
    }

    if (knowledgeBases.length === 0) {
        return (
            <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description="暂无知识库"
                style={{margin: '40px 0'}}
            />
        );
    }

    return (
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

                                <div style={{display: 'flex', alignItems: 'center', fontSize: '11px', color: '#999'}}>
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

export default KnowledgeBaseList;
