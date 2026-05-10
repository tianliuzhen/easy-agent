import React, {useState, useEffect} from 'react';
import {
    Table,
    Button,
    Upload,
    Modal,
    Form,
    Input,
    message,
    Space,
    Popconfirm,
    Card,
    InputNumber,
    Typography,
    Tag
} from 'antd';
import {UploadOutlined, DeleteOutlined, SearchOutlined, ReloadOutlined} from '@ant-design/icons';
import type {UploadFile} from 'antd/es/upload/interface';
import { knowledgeBaseApi } from '../../../api/KnowledgeBaseApi';

const {Title, Text} = Typography;

interface KnowledgeBase {
    id: number;
    kbName: string;
    kbDesc: string;
    fileName: string;
    fileType: string;
    fileSize: number;
    docCount: number;
    docIds: string;
    status: number;
    createTime: string;
    updateTime: string;
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
    const [dataSource, setDataSource] = useState<KnowledgeBase[]>([]);
    const [loading, setLoading] = useState(false);
    const [uploadModalVisible, setUploadModalVisible] = useState(false);
    const [searchModalVisible, setSearchModalVisible] = useState(false);
    const [searchResults, setSearchResults] = useState<any[]>([]);
    const [fileList, setFileList] = useState<UploadFile[]>([]);
    const [uploadMode, setUploadMode] = useState<'file' | 'text'>('file');
    const [pasteImagePreview, setPasteImagePreview] = useState<string | null>(null);
    const [form] = Form.useForm();
    const [searchForm] = Form.useForm();

    useEffect(() => {
        loadData();

        // 添加全局粘贴事件监听
        const handlePaste = (e: ClipboardEvent) => {
            if (!uploadModalVisible || uploadMode !== 'file') {
                return;
            }

            const items = e.clipboardData?.items;
            if (!items) return;

            for (let i = 0; i < items.length; i++) {
                const item = items[i];

                if (item.type.indexOf('image') !== -1) {
                    const file = item.getAsFile();
                    if (file) {
                        const reader = new FileReader();
                        reader.onload = (event) => {
                            setPasteImagePreview(event.target?.result as string);
                        };
                        reader.readAsDataURL(file);

                        const uploadFile: UploadFile = {
                            uid: Date.now().toString(),
                            name: `screenshot-${Date.now()}.png`,
                            status: 'done',
                            originFileObj: file as any,
                            size: file.size,
                            type: file.type,
                        };

                        setFileList([uploadFile]);
                        message.success('截图已粘贴，可以上传了');
                        e.preventDefault();
                    }
                    break;
                }
            }
        };
        document.addEventListener('paste', handlePaste);

        return () => {
            document.removeEventListener('paste', handlePaste);
        };
    }, [uploadModalVisible, uploadMode]);

    const loadData = async () => {
        setLoading(true);
        try {
            let result;
            if (agentId) {
                result = await knowledgeBaseApi.listByAgentId(agentId.toString());
            } else {
                result = await knowledgeBaseApi.list();
            }
            if (result.success) {
                setDataSource(result.data || []);
            } else {
                message.error(result.message || '加载失败');
            }
        } catch (error) {
            message.error('加载失败');
            console.error('加载数据失败:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleUpload = async (values: any) => {
        let file: File;

        if (uploadMode === 'text') {
            if (!values.textContent || values.textContent.trim() === '') {
                message.error('请输入文本内容');
                return;
            }
            const blob = new Blob([values.textContent], {type: 'text/plain'});
            file = new File([blob], values.fileName || 'text-content.txt', {type: 'text/plain'});
        } else {
            if (fileList.length === 0) {
                message.error('请选择文件');
                return;
            }
            file = fileList[0].originFileObj as File;
        }

        setLoading(true);
        try {
            const result = await knowledgeBaseApi.upload(
                agentId?.toString() || '',
                values.kbName,
                values.kbDesc,
                file,
                values.catalog
            );

            if (result.success) {
                message.success('上传成功');
                setUploadModalVisible(false);
                form.resetFields();
                setFileList([]);
                setUploadMode('file');
                loadData();
            } else {
                message.error(result.message || '上传失败');
            }
        } catch (error) {
            message.error('上传失败');
            console.error('上传文档失败:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleDelete = async (id: number) => {
        setLoading(true);
        try {
            const result = await knowledgeBaseApi.delete(id);
            if (result.success) {
                message.success('删除成功');
                loadData();
            } else {
                message.error(result.message || '删除失败');
            }
        } catch (error) {
            message.error('删除失败');
            console.error('删除知识库失败:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleSearch = async (values: any) => {
        setLoading(true);
        try {
            const result = await knowledgeBaseApi.search(values.query, values.topK || 5, values.catalog, values.threshold);
            if (result.success) {
                setSearchResults(result.data || []);
                if (result.data && result.data.length === 0) {
                    message.info('未找到相关知识');
                }
            } else {
                message.error(result.message || '搜索失败');
            }
        } catch (error) {
            message.error('搜索失败');
            console.error('搜索知识失败:', error);
        } finally {
            setLoading(false);
        }
    };

    const columns = [
        {
            title: '知识库名称',
            dataIndex: 'kbName',
            key: 'kbName',
            width: 150,
        },
        {
            title: '描述',
            dataIndex: 'kbDesc',
            key: 'kbDesc',
            ellipsis: true,
        },
        {
            title: '文件名',
            dataIndex: 'fileName',
            key: 'fileName',
            width: 200,
        },
        {
            title: '类型',
            dataIndex: 'fileType',
            key: 'fileType',
            width: 80,
        },
        {
            title: '文件大小',
            dataIndex: 'fileSize',
            key: 'fileSize',
            width: 120,
            render: (size: number) => `${(size / 1024).toFixed(2)} KB`,
        },
        {
            title: '文档片段数',
            dataIndex: 'docCount',
            key: 'docCount',
            width: 120,
        },
        {
            title: '创建时间',
            dataIndex: 'createTime',
            key: 'createTime',
            width: 180,
            render: (time: string) => new Date(time).toLocaleString('zh-CN'),
        },
        {
            title: '操作',
            key: 'action',
            width: 100,
            fixed: 'right' as const,
            render: (_: any, record: KnowledgeBase) => (
                <Popconfirm
                    title="确定要删除吗？"
                    description="删除后将同时删除向量库中的文档数据"
                    onConfirm={() => handleDelete(record.id)}
                    okText="确定"
                    cancelText="取消"
                >
                    <Button type="link" danger icon={<DeleteOutlined/>}>
                        删除
                    </Button>
                </Popconfirm>
            ),
        },
    ];

    return (
        <div>
            <div style={{marginBottom: '24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
                <div>
                    <Title level={4}>知识库管理</Title>
                    <Text type="secondary">管理 Agent 的知识库，支持文本、PDF 文档和图片（OCR 识别）上传</Text>
                </div>
                <Space>
                    <Button
                        type="primary"
                        icon={<UploadOutlined/>}
                        onClick={() => setUploadModalVisible(true)}
                    >
                        上传文档
                    </Button>
                    <Button
                        icon={<SearchOutlined/>}
                        onClick={() => setSearchModalVisible(true)}
                    >
                        知识搜索
                    </Button>
                    <Button icon={<ReloadOutlined/>} onClick={loadData}>
                        刷新
                    </Button>
                </Space>
            </div>

            <Card>
                <Table
                    columns={columns}
                    dataSource={dataSource}
                    rowKey="id"
                    loading={loading}
                    scroll={{x: 1200}}
                    pagination={{
                        pageSize: 10,
                        showTotal: (total) => `共 ${total} 条`,
                        showSizeChanger: true,
                    }}
                />
            </Card>

            <Modal
                title="上传文档"
                open={uploadModalVisible}
                onCancel={() => {
                    setUploadModalVisible(false);
                    form.resetFields();
                    setFileList([]);
                    setUploadMode('file');
                }}
                onOk={() => form.submit()}
                confirmLoading={loading}
                width={600}
            >
                <Form form={form} onFinish={handleUpload} layout="vertical">
                    <Form.Item
                        label="知识库名称"
                        name="kbName"
                        rules={[{required: true, message: '请输入知识库名称'}]}
                    >
                        <Input placeholder="请输入知识库名称" maxLength={100}/>
                    </Form.Item>

                    <Form.Item
                        label="描述"
                        name="kbDesc"
                        rules={[{required: true, message: '请输入描述'}]}
                    >
                        <Input.TextArea placeholder="请输入描述" rows={3} maxLength={500}/>
                    </Form.Item>

                    <Form.Item
                        label="分类（Catalog）"
                        name="catalog"
                    >
                        <Input placeholder="请输入分类，用于搜索时过滤（可选）" maxLength={100}/>
                    </Form.Item>

                    <Form.Item label="上传方式" required>
                        <Space direction="vertical" style={{width: '100%'}}>
                            <Space>
                                <Button
                                    type={uploadMode === 'file' ? 'primary' : 'default'}
                                    onClick={() => setUploadMode('file')}
                                >
                                    文件上传
                                </Button>
                                <Button
                                    type={uploadMode === 'text' ? 'primary' : 'default'}
                                    onClick={() => setUploadMode('text')}
                                >
                                    文本输入
                                </Button>
                            </Space>

                            {uploadMode === 'file' ? (
                                <>
                                    <Upload.Dragger
                                        fileList={fileList}
                                        onChange={({fileList}) => {
                                            setFileList(fileList);
                                            setPasteImagePreview(null);
                                        }}
                                        beforeUpload={() => false}
                                        accept=".txt,.pdf,.jpg,.jpeg,.png,.bmp,.tiff,.gif"
                                        maxCount={1}
                                        style={{background: '#fafafa'}}
                                    >
                                        <p className="ant-upload-drag-icon">
                                            <UploadOutlined style={{fontSize: '48px', color: '#1890ff'}}/>
                                        </p>
                                        <p className="ant-upload-text" style={{fontSize: '16px', marginBottom: '8px'}}>
                                            点击或拖拽文件到此区域上传
                                        </p>
                                        <p className="ant-upload-hint" style={{color: '#999', fontSize: '14px'}}>
                                            📋 支持：截图粘贴（Ctrl+V）/ 拖拽 / 点击上传
                                        </p>
                                        <p className="ant-upload-hint"
                                           style={{color: '#666', fontSize: '13px', marginTop: '8px'}}>
                                            支持格式：txt、pdf、jpg、png 等
                                        </p>
                                    </Upload.Dragger>

                                    {fileList.length > 0 && (
                                        <div style={{
                                            marginTop: 16,
                                            padding: '12px',
                                            background: '#f0f7ff',
                                            borderRadius: '4px'
                                        }}>
                                            <div style={{color: '#1890ff', marginBottom: 8, fontWeight: 500}}>
                                                ✅ 已选择：{fileList[0].name} ({(fileList[0].size! / 1024).toFixed(2)} KB)
                                            </div>

                                            {pasteImagePreview && (
                                                <div style={{marginTop: 12}}>
                                                    <div style={{color: '#999', fontSize: '12px', marginBottom: 8}}>图片预览：</div>
                                                    <img
                                                        src={pasteImagePreview}
                                                        alt="预览"
                                                        style={{
                                                            maxWidth: '100%',
                                                            maxHeight: '200px',
                                                            border: '1px solid #d9d9d9',
                                                            borderRadius: '4px'
                                                        }}
                                                    />
                                                </div>
                                            )}
                                        </div>
                                    )}
                                </>
                            ) : (
                                <>
                                    <Form.Item
                                        name="fileName"
                                        label="文件名"
                                        rules={[{required: true, message: '请输入文件名'}]}
                                        style={{marginBottom: 8}}
                                    >
                                        <Input placeholder="例如：my-document.txt" suffix=".txt"/>
                                    </Form.Item>
                                    <Form.Item
                                        name="textContent"
                                        label="文本内容"
                                        rules={[{required: true, message: '请输入文本内容'}]}
                                        style={{marginBottom: 0}}
                                    >
                                        <Input.TextArea
                                            placeholder="请输入或粘贴文本内容..."
                                            rows={10}
                                            showCount
                                            maxLength={50000}
                                        />
                                    </Form.Item>
                                </>
                            )}
                        </Space>
                    </Form.Item>
                </Form>
            </Modal>

            <Modal
                title="知识搜索"
                open={searchModalVisible}
                onCancel={() => {
                    setSearchModalVisible(false);
                    searchForm.resetFields();
                    setSearchResults([]);
                }}
                footer={null}
                width={800}
            >
                <Form form={searchForm} onFinish={handleSearch} style={{marginBottom: 16}}>
                    <Form.Item
                        label="问题"
                        name="query"
                        rules={[{required: true, message: '请输入搜索内容'}]}
                    >
                        <Input placeholder="请输入搜索内容"/>
                    </Form.Item>
                    <Form.Item>
                        <Space>
                            <Form.Item
                                label="topK"
                                name="topK"
                                initialValue={5}
                                style={{marginBottom: 0}}
                            >
                                <InputNumber placeholder="返回结果数" min={1} max={20} style={{width: 120}}/>
                            </Form.Item>
                            <Form.Item
                                label="catalog"
                                name="catalog"
                                style={{marginBottom: 0}}
                            >
                                <Input placeholder="分类筛选" style={{width: 150}}/>
                            </Form.Item>
                            <Form.Item
                                label="threshold"
                                name="threshold"
                                style={{marginBottom: 0}}
                            >
                                <InputNumber placeholder="相似度阈值" min={0} max={1} step={0.1} style={{width: 120}}/>
                            </Form.Item>
                            <Button type="primary" htmlType="submit" loading={loading} icon={<SearchOutlined/>}>
                                搜索
                            </Button>
                        </Space>
                    </Form.Item>
                </Form>

                {searchResults.length > 0 && (
                    <div style={{maxHeight: 500, overflow: 'auto'}}>
                        {searchResults.map((result, index) => (
                            <Card key={index} size="small" style={{marginBottom: 12}}>
                                <div style={{marginBottom: 8}}>
                                    <Space>
                                        <Tag color="blue">阈值: {(result.score || 0).toFixed(2)}</Tag>
                                        <Tag color="geekblue">{result.kbName}</Tag>
                                        <Tag color="cyan">{result.fileName}</Tag>
                                        {result.catalog && <Tag color="purple">{result.catalog}</Tag>}
                                    </Space>
                                </div>
                                <pre style={{margin: 0, whiteSpace: 'pre-wrap', wordWrap: 'break-word'}}>
                  {result.text}
                </pre>
                            </Card>
                        ))}
                    </div>
                )}
            </Modal>
        </div>
    );
};

export default KnowledgeBaseList;
