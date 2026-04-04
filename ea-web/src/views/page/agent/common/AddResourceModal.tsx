import React, {useState, useEffect} from 'react';
import {Modal, Form, Input, Select, Button, Upload, message, Tabs, Radio, List, Card, Tag} from 'antd';
import {
    UploadOutlined,
    PlusOutlined,
    LinkOutlined,
    FileAddOutlined,
    ApiOutlined,
    DatabaseOutlined,
    CloudServerOutlined
} from '@ant-design/icons';
import {knowledgeBaseApi} from '../../../api/KnowledgeBaseApi';
import {eaToolApi} from '../../../api/EaToolApi';
import {mcpApi} from '../../../api/McpApi';

const {TextArea} = Input;
const {Option} = Select;
const {TabPane} = Tabs;

interface AddResourceModalProps {
    visible: boolean;
    type: 'knowledge' | 'tool' | 'mcp';
    agentId?: number;
    onClose: () => void;
    onSuccess: (type: 'knowledge' | 'tool' | 'mcp') => void;
    // 是否允许创建新知识库（默认true，在Agent配置中设为false）
    allowCreateKnowledge?: boolean;
}

interface KnowledgeBaseItem {
    id: number;
    kbName: string;
    description: string;
    type: string;
    status: string;
    fileCount: number;
    lastUpdated: string;
}

interface ToolItem {
    id: number;
    agentId?: number;
    toolType: string;
    toolInstanceId?: string | null;
    toolInstanceName: string;
    toolInstanceDesc?: string | null;
    inputTemplate?: string | null;
    outTemplate?: string | null;
    isRequired?: boolean | null;
    sortOrder?: number | null;
    isActive: boolean;
    createdAt?: string | null;
    updatedAt?: string | null;
    toolValue?: string | null;
    extraConfig?: string | null;
    // 兼容字段
    name?: string;
    type?: string;
    description?: string;
    status?: string;
    lastUsed?: string;
}

interface McpItem {
    id: number;
    serverName: string;
    description?: string;
    provider?: string;
    version?: string;
    status?: string;
    capabilities?: string[];
    lastUpdated?: string;
}

const AddResourceModal: React.FC<AddResourceModalProps> = ({
                                                               visible,
                                                               type,
                                                               agentId,
                                                               onClose,
                                                               onSuccess,
                                                               allowCreateKnowledge = true
                                                           }) => {
    const [form] = Form.useForm();
    const [loading, setLoading] = useState(false);
    const [activeTab, setActiveTab] = useState('basic');
    const [knowledgeBaseMode, setKnowledgeBaseMode] = useState<'create' | 'link'>('link');
    const [availableKnowledgeBases, setAvailableKnowledgeBases] = useState<KnowledgeBaseItem[]>([]);
    const [availableTools, setAvailableTools] = useState<ToolItem[]>([]);
    const [myTools, setMyTools] = useState<ToolItem[]>([]);
    const [officialMcps, setOfficialMcps] = useState<McpItem[]>([]);
    const [myMcps, setMyMcps] = useState<McpItem[]>([]);
    const [selectedKnowledgeBase, setSelectedKnowledgeBase] = useState<number | null>(null);
    const [selectedTool, setSelectedTool] = useState<number | null>(null);
    const [selectedMcp, setSelectedMcp] = useState<number | null>(null);
    const [searchLoading, setSearchLoading] = useState(false);
    const [searchText, setSearchText] = useState('');
    const [resourceMode, setResourceMode] = useState<'official' | 'my'>('official');

    // 重置表单
    useEffect(() => {
        if (visible) {
            form.resetFields();
            setActiveTab('basic');
            // 总是设置为 link 模式（关联现有资源）
            setKnowledgeBaseMode('link');
            setSelectedKnowledgeBase(null);
            setSelectedTool(null);
            setSearchText('');
            setResourceMode('official');

            // 根据类型加载可用的资源
            if (type === 'knowledge') {
                loadAvailableKnowledgeBases();
            } else if (type === 'tool') {
                // 根据 resourceMode 加载对应的工具列表
                if (resourceMode === 'official') {
                    loadOfficialTools();
                } else {
                    loadMyTools();
                }
            } else if (type === 'mcp') {
                // 根据 resourceMode 加载对应的MCP列表
                if (resourceMode === 'official') {
                    loadOfficialMcps();
                } else {
                    loadMyMcps();
                }
            }
        }
    }, [visible, type, form]);

    // 加载可用的知识库
    const loadAvailableKnowledgeBases = async () => {
        if (type !== 'knowledge') return;

        setSearchLoading(true);
        try {
            const response = await knowledgeBaseApi.listByCondition({});
            if (response.success) {
                // 转换数据格式
                const knowledgeBases = response.data.map((item: any) => ({
                    id: item.id,
                    kbName: item.kbName || item.fileName,
                    description: item.description || item.kbDesc,
                    type: item.type || '文档',
                    status: item.status === 1 ? 'active' : 'inactive',
                    fileCount: item.fileCount || 0,
                    lastUpdated: item.updateTime || item.createTime
                }));
                setAvailableKnowledgeBases(knowledgeBases);
            } else {
                message.error(`加载知识库失败: ${response.message}`);
            }
        } catch (error) {
            console.error('加载知识库失败:', error);
            message.error('加载知识库失败');
        } finally {
            setSearchLoading(false);
        }
    };

    // 加载官方可用工具
    const loadOfficialTools = async () => {
        if (type !== 'tool') return;

        setSearchLoading(true);
        try {
            const response = await eaToolApi.getDefaultTools();
            if (response.success && response.data) {
                setAvailableTools(response.data);
            } else {
                message.error(`加载工具失败：${response.message || '未知错误'}`);
                setAvailableTools([]);
            }
        } catch (error) {
            console.error('加载工具失败:', error);
            message.error('加载工具失败');
            setAvailableTools([]);
        } finally {
            setSearchLoading(false);
        }
    };

    // 加载我的工具
    const loadMyTools = async () => {
        if (type !== 'tool') return;

        setSearchLoading(true);
        try {
            const response = await eaToolApi.getToolConfigByUserId();
            if (response.success && response.data) {
                setMyTools(response.data);
            } else {
                message.error(`加载我的工具失败：${response.message || '未知错误'}`);
                setMyTools([]);
            }
        } catch (error) {
            console.error('加载我的工具失败:', error);
            message.error('加载我的工具失败');
            setMyTools([]);
        } finally {
            setSearchLoading(false);
        }
    };

    // 处理我的工具模式切换
    const handleResourceModeChange = (mode: 'official' | 'my') => {
        setResourceMode(mode);
        if (type === 'tool') {
            if (mode === 'my') {
                loadMyTools();
            } else {
                loadOfficialTools();
            }
        } else if (type === 'mcp') {
            if (mode === 'my') {
                loadMyMcps();
            } else {
                loadOfficialMcps();
            }
        }
    };

    // 加载官方MCP
    const loadOfficialMcps = async () => {
        if (type !== 'mcp') return;

        setSearchLoading(true);
        try {
            const response = await mcpApi.getOfficialMcpConfigs();
            if (response.success && response.data) {
                setOfficialMcps(response.data);
            } else {
                message.error(`加载官方MCP失败：${response.message || '未知错误'}`);
                setOfficialMcps([]);
            }
        } catch (error) {
            console.error('加载官方MCP失败:', error);
            message.error('加载官方MCP失败');
            setOfficialMcps([]);
        } finally {
            setSearchLoading(false);
        }
    };

    // 加载我的MCP
    const loadMyMcps = async () => {
        if (type !== 'mcp') return;

        setSearchLoading(true);
        try {
            const response = await mcpApi.getMcpConfigByUserId();
            if (response.success && response.data) {
                setMyMcps(response.data);
            } else {
                message.error(`加载我的MCP失败：${response.message || '未知错误'}`);
                setMyMcps([]);
            }
        } catch (error) {
            console.error('加载我的MCP失败:', error);
            message.error('加载我的MCP失败');
            setMyMcps([]);
        } finally {
            setSearchLoading(false);
        }
    };

    // 获取模态框标题
    const getModalTitle = () => {
        const typeNames = {
            'knowledge': '知识库',
            'tool': '工具',
            'mcp': 'MCP',
            'skill': 'SKILL'
        };

        if (type === 'knowledge' && knowledgeBaseMode === 'link') {
            return `关联${typeNames[type]}`;
        } else if (type === 'tool') {
            if (knowledgeBaseMode === 'link') {
                return resourceMode === 'official' ? '关联官方工具' : '我的工具';
            } else if (knowledgeBaseMode === 'create') {
                return `创建新工具`;
            }
        }

        return `添加${typeNames[type]}`;
    };

    // 获取表单配置
    const getFormConfig = () => {
        const baseFields = [
            {
                name: 'name',
                label: '名称',
                rules: [{required: true, message: '请输入名称'}],
                component: <Input placeholder={`请输入${getModalTitle()}名称`}/>
            },
            {
                name: 'description',
                label: '描述',
                rules: [{required: true, message: '请输入描述'}],
                component: <TextArea rows={3} placeholder="请输入详细描述"/>
            }
        ];

        switch (type) {
            case 'knowledge':
                return [
                    ...baseFields,
                    {
                        name: 'type',
                        label: '类型',
                        rules: [{required: true, message: '请选择类型'}],
                        component: (
                            <Select placeholder="请选择知识库类型">
                                <Option value="文档">文档</Option>
                                <Option value="问答">问答</Option>
                                <Option value="技术文档">技术文档</Option>
                                <Option value="销售资料">销售资料</Option>
                                <Option value="其他">其他</Option>
                            </Select>
                        )
                    },
                    {
                        name: 'files',
                        label: '上传文件',
                        component: (
                            <Upload
                                multiple
                                beforeUpload={() => false}
                                maxCount={10}
                            >
                                <Button icon={<UploadOutlined/>}>选择文件</Button>
                            </Upload>
                        )
                    }
                ];

            case 'tool':
                return [
                    ...baseFields,
                    {
                        name: 'toolType',
                        label: '工具类型',
                        rules: [{required: true, message: '请选择工具类型'}],
                        component: (
                            <Select placeholder="请选择工具类型">
                                <Option value="SQL">SQL执行器</Option>
                                <Option value="HTTP">HTTP请求</Option>
                                <Option value="MCP">MCP服务器</Option>
                                <Option value="GRPC">gRPC工具</Option>
                                <Option value="其他">其他</Option>
                            </Select>
                        )
                    },
                    {
                        name: 'endpoint',
                        label: '接口地址',
                        component: <Input placeholder="请输入接口地址（可选）"/>
                    },
                    {
                        name: 'config',
                        label: '配置参数',
                        component: <TextArea rows={4} placeholder="请输入JSON格式的配置参数（可选）"/>
                    }
                ];

            case 'mcp':
                return [
                    ...baseFields,
                    {
                        name: 'provider',
                        label: '提供商',
                        rules: [{required: true, message: '请输入提供商'}],
                        component: <Input placeholder="例如：Anthropic、GitHub等"/>
                    },
                    {
                        name: 'version',
                        label: '版本',
                        component: <Input placeholder="例如：1.0.0"/>
                    },
                    {
                        name: 'capabilities',
                        label: '能力',
                        component: (
                            <Select
                                mode="tags"
                                placeholder="输入能力后按回车添加"
                                style={{width: '100%'}}
                            />
                        )
                    }
                ];

            default:
                return baseFields;
        }
    };

    // 处理提交
    const handleSubmit = async () => {
        try {
            setLoading(true);
    
            if (type === 'knowledge' || type === 'tool' || type === 'mcp') {
                // 关联现有资源
                if (type === 'knowledge') {
                    if (!selectedKnowledgeBase) {
                        message.error('请选择一个知识库');
                        return;
                    }
    
                    const selectedKb = availableKnowledgeBases.find(kb => kb.id === selectedKnowledgeBase);
                    if (!selectedKb) {
                        message.error('选择的知识库不存在');
                        return;
                    }
    
                    // 调用绑定API
                    const bindRequest = {
                        agentId: agentId?.toString() || '',
                        knowledgeBaseId: selectedKnowledgeBase,
                        kbName: selectedKb.kbName,
                        creator: 'system' // 这里应该从用户上下文获取
                    };
    
                    const response = await knowledgeBaseApi.bind(bindRequest);
                    if (response.success) {
                        message.success(`知识库 "${selectedKb.kbName}" 关联成功`);
                        onSuccess(type);
                        onClose();
                    } else {
                        message.error(`关联失败: ${response.message}`);
                    }
                } else if (type === 'tool') {
                    if (!selectedTool) {
                        message.error('请选择一个工具');
                        return;
                    }
    
                    // 根据当前模式选择数据源
                    const toolSource = resourceMode === 'official' ? availableTools : myTools;
                    const selectedToolItem = toolSource.find(tool => tool.id === selectedTool);
    
                    if (!selectedToolItem) {
                        message.error('选择的工具不存在');
                        return;
                    }
    
                    // 调用绑定 API
                    const bindRequest = {
                        agentId: agentId?.toString() || '',
                        toolConfigId: selectedTool,
                        toolName: selectedToolItem.toolInstanceName || selectedToolItem.name,
                        creator: 'system' // 这里应该从用户上下文获取
                    };
    
                    const response = await eaToolApi.bindTool(bindRequest);
                    if (response.success) {
                        message.success(`工具 "${selectedToolItem.toolInstanceName || selectedToolItem.name}" 关联成功`);
                        onSuccess(type);
                        onClose();
                    } else {
                        message.error(`关联失败：${response.message}`);
                    }
                } else if (type === 'mcp') {
                    if (!selectedMcp) {
                        message.error('请选择一个MCP');
                        return;
                    }
    
                    const selectedMcpItem = (resourceMode === 'official' ? officialMcps : myMcps).find(mcp => mcp.id === selectedMcp);
                    if (!selectedMcpItem) {
                        message.error('选择的MCP不存在');
                        return;
                    }
    
                    // 调用绑定 API
                    const bindRequest = {
                        agentId: agentId?.toString() || '',
                        mcpConfigId: selectedMcp,
                        mcpName: selectedMcpItem.serverName,
                        creator: 'system'
                    };
    
                    const response = await mcpApi.bindMcp(bindRequest);
                    if (response.success) {
                        message.success(`MCP "${selectedMcpItem.serverName}" 关联成功`);
                        onSuccess(type);
                        onClose();
                    } else {
                        message.error(`关联失败：${response.message}`);
                    }
                }
            } else {
                // 创建新资源（MCP）
                const values = await form.validateFields();
    
                // 这里可以调用API添加资源
                // await resourceApi.addResource({
                //     ...values,
                //     agentId,
                //     type
                // });
    
                // 模拟API调用
                await new Promise(resolve => setTimeout(resolve, 1000));
    
                message.success(`${getModalTitle()}添加成功`);
                onSuccess(type);
                onClose();
            }
        } catch (error) {
            console.error('添加资源失败:', error);
            if (error instanceof Error) {
                message.error(`添加失败: ${error.message}`);
            }
        } finally {
            setLoading(false);
        }
    };

    // 渲染表单字段
    const renderFormFields = () => {
        const formConfig = getFormConfig();
        return formConfig.map((field, index) => (
            <Form.Item
                key={index}
                name={field.name}
                label={field.label}
                rules={field.rules}
            >
                {field.component}
            </Form.Item>
        ));
    };

    // 渲染资源模式选择
    const renderResourceModeSelector = () => {
        if ((type !== 'knowledge' && type !== 'tool' && type !== 'mcp') || !allowCreateKnowledge) return null;

        if (type === 'knowledge') {
            const resourceName = '知识库';
            return (
                <div style={{marginBottom: '16px'}}>
                    <Radio.Group
                        value={knowledgeBaseMode}
                        onChange={(e) => setKnowledgeBaseMode(e.target.value)}
                        style={{width: '100%'}}
                    >
                        <Radio.Button value="link" style={{width: '50%', textAlign: 'center'}}>
                            <LinkOutlined/> 关联现有{resourceName}
                        </Radio.Button>
                        <Radio.Button
                            value="create"
                            style={{width: '50%', textAlign: 'center'}}
                        >
                            <FileAddOutlined/> 创建新{resourceName}
                        </Radio.Button>
                    </Radio.Group>
                </div>
            );
        } else if (type === 'tool') {
            // 工具类型使用新的 mode 选择
            return (
                <div style={{marginBottom: '16px'}}>
                    <Radio.Group
                        value={resourceMode}
                        onChange={(e) => handleResourceModeChange(e.target.value)}
                        style={{width: '100%'}}
                    >
                        <Radio.Button value="official" style={{width: '50%', textAlign: 'center'}}>
                            <CloudServerOutlined/> 关联官方工具
                        </Radio.Button>
                        <Radio.Button value="my" style={{width: '50%', textAlign: 'center'}}>
                            <DatabaseOutlined/> 我的工具
                        </Radio.Button>
                    </Radio.Group>
                </div>
            );
        } else if (type === 'mcp') {
            // MCP类型使用 mode 选择
            return (
                <div style={{marginBottom: '16px'}}>
                    <Radio.Group
                        value={resourceMode}
                        onChange={(e) => handleResourceModeChange(e.target.value)}
                        style={{width: '100%'}}
                    >
                        <Radio.Button value="official" style={{width: '50%', textAlign: 'center'}}>
                            <CloudServerOutlined/> 官方MCP
                        </Radio.Button>
                        <Radio.Button value="my" style={{width: '50%', textAlign: 'center'}}>
                            <DatabaseOutlined/> 我的MCP
                        </Radio.Button>
                    </Radio.Group>
                </div>
            );
        }

        return null;
    };

    // 获取工具图标
    const getToolIcon = (toolType: string) => {
        switch (toolType) {
            case 'SQL':
                return <DatabaseOutlined style={{color: '#52c41a', marginRight: '8px'}}/>;
            case 'HTTP':
                return <ApiOutlined style={{color: '#1890ff', marginRight: '8px'}}/>;
            case 'MCP':
                return <CloudServerOutlined style={{color: '#fa8c16', marginRight: '8px'}}/>;
            case 'GRPC':
                return <CloudServerOutlined style={{color: '#722ed1', marginRight: '8px'}}/>;
            default:
                return <ApiOutlined style={{color: '#8c8c8c', marginRight: '8px'}}/>;
        }
    };

    // 获取类型标签颜色
    const getToolTypeColor = (toolType: string) => {
        const typeColors: Record<string, string> = {
            'SQL': 'green',
            'HTTP': 'blue',
            'MCP': 'orange',
            'GRPC': 'purple',
            '其他': 'default'
        };
        return typeColors[toolType] || 'default';
    };

    // 获取状态标签颜色
    const getStatusColor = (status: string) => {
        return status === 'active' ? 'green' : 'red';
    };

    // 渲染资源列表（知识库、工具或MCP）
    const renderResourceList = () => {
        if (type !== 'knowledge' && type !== 'tool' && type !== 'mcp') return null;

        if (searchLoading) {
            return (
                <div style={{textAlign: 'center', padding: '40px 0'}}>
                    <div>加载{type === 'knowledge' ? '知识库' : (type === 'tool' ? '工具' : 'MCP')}中...</div>
                </div>
            );
        }

        if (type === 'knowledge' && availableKnowledgeBases.length === 0) {
            return (
                <div style={{textAlign: 'center', padding: '40px 0', color: '#999'}}>
                    暂无可用知识库
                </div>
            );
        }

        if (type === 'tool' && resourceMode === 'official' && availableTools.length === 0) {
            return (
                <div style={{textAlign: 'center', padding: '40px 0', color: '#999'}}>
                    暂无可用工具
                </div>
            );
        }

        if (type === 'tool' && resourceMode === 'my' && myTools.length === 0) {
            return (
                <div style={{textAlign: 'center', padding: '40px 0', color: '#999'}}>
                    暂无我的工具
                </div>
            );
        }

        if (type === 'mcp' && resourceMode === 'official' && officialMcps.length === 0) {
            return (
                <div style={{textAlign: 'center', padding: '40px 0', color: '#999'}}>
                    暂无官方MCP
                </div>
            );
        }

        if (type === 'mcp' && resourceMode === 'my' && myMcps.length === 0) {
            return (
                <div style={{textAlign: 'center', padding: '40px 0', color: '#999'}}>
                    暂无我的MCP
                </div>
            );
        }

        const renderKnowledgeBaseItem = (item: KnowledgeBaseItem) => (
            <List.Item style={{padding: '0', border: 'none', marginBottom: '5px', marginTop: '5px'}}>
                <Card
                    size="small"
                    style={{
                        width: '100%',
                        cursor: 'pointer',
                        borderColor: selectedKnowledgeBase === item.id ? '#1890ff' : '#f0f0f0',
                        backgroundColor: selectedKnowledgeBase === item.id ? '#e6f7ff' : '#fff'
                    }}
                    onClick={() => setSelectedKnowledgeBase(item.id)}
                >
                    <div style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'flex-start'
                    }}>
                        <div style={{flex: 1, display: 'flex', flexDirection: 'column'}}>
                            <div style={{display: 'flex', alignItems: 'center', marginBottom: '8px'}}>
                                <h4 style={{margin: 0, fontSize: '14px', fontWeight: 600}}>
                                    {item.kbName}
                                </h4>
                                <Tag
                                    color={item.status === 'active' ? 'green' : 'red'}
                                    style={{marginLeft: '8px', fontSize: '11px'}}
                                >
                                    {item.status === 'active' ? '启用' : '停用'}
                                </Tag>
                                <Tag
                                    color="blue"
                                    style={{marginLeft: '4px', fontSize: '11px'}}
                                >
                                    {item.type}
                                </Tag>
                                <Tag
                                    color="blue"
                                    style={{marginLeft: '4px', fontSize: '11px'}}
                                >
                                    文件数：<strong>{item.fileCount}</strong>
                                </Tag>
                                <span style={{marginLeft: 'auto', fontSize: '11px', color: '#999'}}>
                                    更新：{item.lastUpdated}
                                </span>
                            </div>

                            <p style={{
                                margin: '0 0 8px 0',
                                fontSize: '12px',
                                color: '#666',
                                lineHeight: '1.4'
                            }}>
                                {item.description}
                            </p>
                        </div>
                    </div>
                </Card>
            </List.Item>
        );

        const renderToolItem = (item: ToolItem) => {
            // 统一使用 API 返回的原始字段
            const toolName = item.toolInstanceName || item.name || `工具-${item.id}`;
            const toolType = item.toolType || item.type || '其他';
            const toolDesc = item.toolInstanceDesc || item.description || item.toolValue || '无描述';
            const toolStatus = item.isActive ? 'active' : 'inactive';
            const lastUsed = item.updatedAt || item.createdAt || '从未使用';

            return (
                <List.Item style={{padding: '0', border: 'none', marginBottom: '5px', marginTop: '5px'}}>
                    <Card
                        size="small"
                        style={{
                            width: '100%',
                            cursor: 'pointer',
                            borderColor: selectedTool === item.id ? '#1890ff' : '#f0f0f0',
                            backgroundColor: selectedTool === item.id ? '#e6f7ff' : '#fff'
                        }}
                        onClick={() => setSelectedTool(item.id)}
                    >
                        <div style={{
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'flex-start'
                        }}>
                            <div style={{flex: 1, display: 'flex', flexDirection: 'column'}}>
                                <div style={{display: 'flex', alignItems: 'center', marginBottom: '8px'}}>
                                    {getToolIcon(toolType)}
                                    <h4 style={{margin: 0, fontSize: '14px', fontWeight: 600}}>
                                        {toolName}
                                    </h4>
                                    <Tag
                                        color={toolStatus === 'active' ? 'green' : 'red'}
                                        style={{marginLeft: '8px', fontSize: '11px'}}
                                    >
                                        {toolStatus === 'active' ? '启用' : '停用'}
                                    </Tag>
                                    <Tag
                                        color={getToolTypeColor(toolType)}
                                        style={{marginLeft: '4px', fontSize: '11px'}}
                                    >
                                        {toolType}
                                    </Tag>
                                    <span style={{marginLeft: 'auto', fontSize: '11px', color: '#999'}}>
                                        最后使用：{lastUsed}
                                    </span>
                                </div>

                                <p style={{
                                    margin: '0 0 8px 0',
                                    fontSize: '12px',
                                    color: '#666',
                                    lineHeight: '1.4'
                                }}>
                                    {toolDesc}
                                </p>
                            </div>
                        </div>
                    </Card>
                </List.Item>
            );
        };

        const renderMcpItem = (item: McpItem) => (
            <List.Item style={{padding: '0', border: 'none', marginBottom: '5px', marginTop: '5px'}}>
                <Card
                    size="small"
                    style={{
                        width: '100%',
                        cursor: 'pointer',
                        borderColor: selectedMcp === item.id ? '#1890ff' : '#f0f0f0',
                        backgroundColor: selectedMcp === item.id ? '#e6f7ff' : '#fff'
                    }}
                    onClick={() => setSelectedMcp(item.id)}
                >
                    <div style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'flex-start'
                    }}>
                        <div style={{flex: 1, display: 'flex', flexDirection: 'column'}}>
                            <div style={{display: 'flex', alignItems: 'center', marginBottom: '8px'}}>
                                <CloudServerOutlined style={{color: '#fa8c16', marginRight: '8px'}} />
                                <h4 style={{margin: 0, fontSize: '14px', fontWeight: 600}}>
                                    {item.serverName}
                                </h4>
                                <Tag
                                    color={item.status === 'active' ? 'green' : 'red'}
                                    style={{marginLeft: '8px', fontSize: '11px'}}
                                >
                                    {item.status === 'active' ? '启用' : '停用'}
                                </Tag>
                                {item.provider && (
                                    <Tag
                                        color="purple"
                                        style={{marginLeft: '4px', fontSize: '11px'}}
                                    >
                                        {item.provider}
                                    </Tag>
                                )}
                                {item.version && (
                                    <Tag
                                        style={{marginLeft: '4px', fontSize: '11px'}}
                                    >
                                        v{item.version}
                                    </Tag>
                                )}
                                <span style={{marginLeft: 'auto', fontSize: '11px', color: '#999'}}>
                                    更新：{item.lastUpdated}
                                </span>
                            </div>

                            <p style={{
                                margin: '0 0 8px 0',
                                fontSize: '12px',
                                color: '#666',
                                lineHeight: '1.4'
                            }}>
                                {item.description}
                            </p>
                        </div>
                    </div>
                </Card>
            </List.Item>
        );

        const dataSource = type === 'knowledge' ? availableKnowledgeBases : 
                          (type === 'tool' ? (resourceMode === 'official' ? availableTools : myTools) : (resourceMode === 'official' ? officialMcps : myMcps));
        const searchPlaceholder = type === 'knowledge' ? '搜索知识库名称或描述' : 
                                 (type === 'tool' ? '搜索工具名称或描述' : '搜索MCP名称或描述');
        const filterFunc = type === 'knowledge'
            ? (item: KnowledgeBaseItem) =>
                !searchText ||
                item.kbName.toLowerCase().includes(searchText.toLowerCase()) ||
                (item.description && item.description.toLowerCase().includes(searchText.toLowerCase()))
            : type === 'tool'
            ? (item: ToolItem) =>
                !searchText ||
                (item.toolInstanceName || item.name || '').toLowerCase().includes(searchText.toLowerCase()) ||
                (item.toolInstanceDesc || item.description || '').toLowerCase().includes(searchText.toLowerCase())
            : (item: McpItem) =>
                !searchText ||
                item.serverName.toLowerCase().includes(searchText.toLowerCase()) ||
                (item.description && item.description.toLowerCase().includes(searchText.toLowerCase()));

        return (
            <div>
                <Input.Search
                    placeholder={searchPlaceholder}
                    allowClear
                    value={searchText}
                    onChange={(e) => setSearchText(e.target.value)}
                    style={{margin: '0px'}}
                />
                <List
                    dataSource={dataSource.filter(filterFunc as any)}
                    renderItem={(item: any) =>
                        type === 'knowledge' ? renderKnowledgeBaseItem(item) : 
                        (type === 'tool' ? renderToolItem(item) : renderMcpItem(item))
                    }
                    style={{margin: '0px', maxHeight: '500px', overflowY: 'auto'}}
                />
            </div>
        );
    };

    // 渲染高级配置
    const renderAdvancedConfig = () => {
        switch (type) {
            case 'knowledge':
                return (
                    <div>
                        <Form.Item
                            name="indexMethod"
                            label="索引方法"
                        >
                            <Select placeholder="请选择索引方法">
                                <Option value="fulltext">全文索引</Option>
                                <Option value="vector">向量索引</Option>
                                <Option value="hybrid">混合索引</Option>
                            </Select>
                        </Form.Item>
                        <Form.Item
                            name="chunkSize"
                            label="分块大小"
                        >
                            <Input placeholder="例如：1000（字符）"/>
                        </Form.Item>
                        <Form.Item
                            name="tags"
                            label="标签"
                        >
                            <Select
                                mode="tags"
                                placeholder="输入标签后按回车添加"
                                style={{width: '100%'}}
                            />
                        </Form.Item>
                    </div>
                );

            case 'tool':
                return (
                    <div>
                        <Form.Item
                            name="timeout"
                            label="超时时间（秒）"
                        >
                            <Input placeholder="例如：30"/>
                        </Form.Item>
                        <Form.Item
                            name="retryCount"
                            label="重试次数"
                        >
                            <Input placeholder="例如：3"/>
                        </Form.Item>
                        <Form.Item
                            name="authType"
                            label="认证类型"
                        >
                            <Select placeholder="请选择认证类型">
                                <Option value="none">无认证</Option>
                                <Option value="apiKey">API Key</Option>
                                <Option value="bearer">Bearer Token</Option>
                                <Option value="basic">Basic Auth</Option>
                            </Select>
                        </Form.Item>
                    </div>
                );

            case 'mcp':
                return (
                    <div>
                        <Form.Item
                            name="serverUrl"
                            label="服务器地址"
                        >
                            <Input placeholder="例如：http://localhost:8000"/>
                        </Form.Item>
                        <Form.Item
                            name="authToken"
                            label="认证令牌"
                        >
                            <Input.Password placeholder="请输入认证令牌"/>
                        </Form.Item>
                        <Form.Item
                            name="maxConcurrent"
                            label="最大并发数"
                        >
                            <Input placeholder="例如：5"/>
                        </Form.Item>
                    </div>
                );

            default:
                return null;
        }
    };

    return (
        <Modal
            title={getModalTitle()}
            open={visible}
            onCancel={onClose}
            footer={[
                <Button key="cancel" onClick={onClose}>
                    取消
                </Button>,
                <Button
                    key="submit"
                    type="primary"
                    loading={loading}
                    onClick={handleSubmit}
                    icon={<PlusOutlined/>}
                >
                    添加
                </Button>
            ]}
            width={600}
            destroyOnClose
        >
            {renderResourceModeSelector()}

            {(type === 'knowledge' || type === 'tool' || type === 'mcp') && (!allowCreateKnowledge || knowledgeBaseMode === 'link') ? (
                <div style={{marginTop: '16px'}}>
                    <div style={{marginBottom: '8px', fontWeight: 500, color: '#333'}}>
                        {type === 'knowledge' ? '选择要关联的知识库：' :
                            (type === 'tool' ? 
                                (resourceMode === 'official' ? '选择要关联的官方工具：' : '选择我的工具：') :
                                '选择要关联的MCP：')}
                    </div>
                    {renderResourceList()}
                </div>
            ) : (
                <Tabs
                    activeKey={activeTab}
                    onChange={setActiveTab}
                    size="small"
                >
                    <TabPane tab="基本配置" key="basic">
                        <Form
                            form={form}
                            layout="vertical"
                            style={{marginTop: '16px'}}
                        >
                            {renderFormFields()}
                        </Form>
                    </TabPane>
                    <TabPane tab="高级配置" key="advanced">
                        <Form
                            form={form}
                            layout="vertical"
                            style={{marginTop: '16px'}}
                        >
                            {renderAdvancedConfig()}
                        </Form>
                    </TabPane>
                </Tabs>
            )}

            <div style={{
                marginTop: '16px',
                padding: '12px',
                background: '#f6f8fa',
                borderRadius: '6px',
                fontSize: '12px',
                color: '#586069'
            }}>
                <div style={{fontWeight: 'bold', marginBottom: '4px'}}>提示：</div>
                <ul style={{margin: 0, paddingLeft: '16px'}}>
                    <li>添加后需要保存配置才能生效</li>
                    <li>可以在高级配置中设置更多参数</li>
                    <li>添加的资源需要经过测试确保可用</li>
                </ul>
            </div>
        </Modal>
    );
};

export default AddResourceModal;
