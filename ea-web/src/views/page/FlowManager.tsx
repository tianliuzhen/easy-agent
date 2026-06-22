import React, {useEffect, useState} from 'react';
import {App, Button, Card, Drawer, Form, Input, List, Modal, Select, Space, Tag} from 'antd';
import {DeleteOutlined, EditOutlined, MessageOutlined, PlusOutlined, MinusCircleOutlined} from '@ant-design/icons';
import {flowApi, type Flow, type FlowNode} from '../api/FlowApi';
import {eaAgentApi} from '../api/EaAgentApi';

const STRATEGY_OPTIONS = [
    {value: 'WORKFLOW', label: '流水线（WORKFLOW）'},
    {value: 'SUPERVISOR', label: '主管（SUPERVISOR）'},
    {value: 'ROUTER', label: '路由（ROUTER）'},
];

const strategyColor: Record<string, string> = {
    WORKFLOW: 'blue',
    SUPERVISOR: 'purple',
    ROUTER: 'orange',
};

const strategyHint: Record<string, string> = {
    WORKFLOW: '流水线：成员 Agent 按顺序串行执行，上一个的输出作为下一个的输入，适合「生成 → 审校 → 翻译」这类固定流程。',
    SUPERVISOR: '主管：由主管 Agent 把各成员 Agent 当作工具按需调用，可多轮协作，适合专家分工场景。',
    ROUTER: '路由：路由 Agent 判断意图后，将整个对话转交给最合适的某个成员 Agent，适合客服分流场景。',
};

const FlowManagerPage: React.FC = () => {
    const {message} = App.useApp();
    const [data, setData] = useState<Flow[]>([]);
    const [loading, setLoading] = useState(false);
    const [agents, setAgents] = useState<any[]>([]);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editingId, setEditingId] = useState<number | null>(null);
    const [form] = Form.useForm();
    const [strategy, setStrategy] = useState<string>('WORKFLOW');
    const [nodes, setNodes] = useState<FlowNode[]>([]);
    const [deleteTarget, setDeleteTarget] = useState<Flow | null>(null);

    const loadData = async () => {
        setLoading(true);
        try {
            const result = await flowApi.list();
            setData(result?.data || []);
        } catch (e) {
            message.error('加载编排列表失败');
        } finally {
            setLoading(false);
        }
    };

    const loadAgents = async () => {
        try {
            const result = await eaAgentApi.listAgent({});
            setAgents(result?.data || []);
        } catch (e) {
            console.error('加载 Agent 列表失败', e);
        }
    };

    useEffect(() => {
        loadData();
        loadAgents();
    }, []);

    const agentName = (agentId?: number) => agents.find(a => a.id === agentId)?.agentName || agentId;

    const showAddModal = () => {
        form.resetFields();
        form.setFieldsValue({strategy: 'WORKFLOW', avatar: '🧩'});
        setStrategy('WORKFLOW');
        setNodes([]);
        setEditingId(null);
        setIsModalOpen(true);
    };

    const handleEdit = async (record: Flow) => {
        try {
            const result = await flowApi.detail(record.id!);
            const flow: Flow = result?.data;
            if (!flow) {
                message.error('获取编排详情失败');
                return;
            }
            form.setFieldsValue({
                flowName: flow.flowName,
                flowKey: flow.flowKey,
                strategy: flow.strategy,
                supervisorAgentId: flow.supervisorAgentId,
                avatar: flow.avatar || '🧩',
                welcomeMessage: flow.welcomeMessage,
                prompt: flow.prompt,
                flowDesc: flow.flowDesc,
            });
            setStrategy(flow.strategy);
            setNodes(flow.nodes || []);
            setEditingId(flow.id!);
            setIsModalOpen(true);
        } catch (e) {
            message.error('获取编排详情失败');
        }
    };

    const addNode = () => {
        setNodes(prev => [...prev, {agentId: undefined as any, nodeRole: '', orderIndex: prev.length}]);
    };

    const removeNode = (index: number) => {
        setNodes(prev => prev.filter((_, i) => i !== index));
    };

    const updateNode = (index: number, patch: Partial<FlowNode>) => {
        setNodes(prev => prev.map((n, i) => (i === index ? {...n, ...patch} : n)));
    };

    const handleSubmit = async () => {
        try {
            const values = await form.validateFields();
            const validNodes = nodes.filter(n => n.agentId != null);
            if (validNodes.length === 0) {
                message.warning('请至少添加一个成员 Agent');
                return;
            }
            const payload: Flow = {
                ...values,
                id: editingId || undefined,
                nodes: validNodes.map((n, i) => ({
                    agentId: n.agentId,
                    nodeRole: n.nodeRole,
                    orderIndex: i,
                })),
            };
            const result = await flowApi.save(payload);
            if (result?.success === false) {
                message.error(result?.message || '保存失败');
                return;
            }
            message.success(editingId ? '更新成功' : '添加成功');
            setIsModalOpen(false);
            loadData();
        } catch (e) {
            // validateFields 抛出会落到这里，无需额外提示
        }
    };

    const handleConfirmDelete = async () => {
        if (!deleteTarget) return;
        try {
            await flowApi.remove(deleteTarget.id!);
            message.success('删除成功');
            setDeleteTarget(null);
            loadData();
        } catch (e) {
            message.error('删除失败');
        }
    };

    return (
        <div style={{padding: 0, margin: 0}}>
            <div style={{marginBottom: 16, display: 'flex', justifyContent: 'flex-end'}}>
                <Button type="primary" icon={<PlusOutlined/>} onClick={showAddModal}>
                    添加编排
                </Button>
            </div>

            <List
                grid={{gutter: 16, xs: 1, sm: 2, md: 3, lg: 3, xl: 4, xxl: 5}}
                loading={loading}
                dataSource={data}
                renderItem={(item) => (
                    <List.Item>
                        <Card
                            size="small"
                            title={
                                <span style={{display: 'flex', alignItems: 'center', gap: 8}}>
                                    <span style={{fontSize: 18}}>{item.avatar || '🧩'}</span>
                                    <a
                                        href="#"
                                        onClick={(e) => {
                                            e.preventDefault();
                                            window.open(`/agentChat/chatDemo/?flowId=${item.id}`, '_blank');
                                        }}
                                    >
                                        {item.flowName}
                                    </a>
                                </span>
                            }
                            extra={
                                <Space>
                                    <Button type="text" size="small" title="编辑"
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                handleEdit(item);
                                            }}>
                                        <EditOutlined/>
                                    </Button>
                                    <Button type="text" size="small" danger title="删除"
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                setDeleteTarget(item);
                                            }}>
                                        <DeleteOutlined/>
                                    </Button>
                                </Space>
                            }
                            style={{cursor: 'pointer'}}
                            onClick={() => window.open(`/agentChat/chatDemo/?flowId=${item.id}`, '_blank')}
                        >
                            <div style={{marginBottom: 8}}>
                                <Tag color={strategyColor[item.strategy] || 'default'}>{item.strategy}</Tag>
                            </div>
                            {item.flowDesc && (
                                <div style={{fontSize: 12, color: '#888', marginBottom: 8}}>{item.flowDesc}</div>
                            )}
                            <div style={{fontSize: 12, color: '#aaa', display: 'flex', alignItems: 'center', gap: 4}}>
                                <MessageOutlined/> 点击发起对话
                            </div>
                        </Card>
                    </List.Item>
                )}
            />

            <Drawer
                title={editingId ? '编辑编排' : '添加编排'}
                open={isModalOpen}
                onClose={() => setIsModalOpen(false)}
                destroyOnClose
                width={760}
                footer={
                    <div style={{textAlign: 'right'}}>
                        <Space>
                            <Button onClick={() => setIsModalOpen(false)}>取消</Button>
                            <Button type="primary" onClick={handleSubmit}>确定</Button>
                        </Space>
                    </div>
                }
            >
                <Form form={form} layout="vertical">
                    <Form.Item name="flowName" label="编排名称" rules={[{required: true, message: '请输入编排名称'}]}>
                        <Input placeholder="请输入编排名称"/>
                    </Form.Item>

                    <Form.Item
                        name="strategy"
                        label="编排策略"
                        rules={[{required: true, message: '请选择编排策略'}]}
                        extra={<span style={{color: '#999', fontSize: 12}}>{strategyHint[strategy]}</span>}
                    >
                        <Select options={STRATEGY_OPTIONS} onChange={(v) => setStrategy(v)}/>
                    </Form.Item>

                    {(strategy === 'SUPERVISOR' || strategy === 'ROUTER') && (
                        <Form.Item name="supervisorAgentId" label={strategy === 'ROUTER' ? '路由 Agent' : '主管 Agent'}>
                            <Select
                                placeholder="请选择主管/路由 Agent"
                                showSearch
                                allowClear
                                optionFilterProp="label"
                                options={agents.map(a => ({value: a.id, label: a.agentName}))}
                            />
                        </Form.Item>
                    )}

                    <Form.Item label="成员 Agent">
                        <div style={{border: '1px solid #f0f0f0', borderRadius: 4, padding: 12}}>
                            {nodes.length === 0 && (
                                <div style={{color: '#999', marginBottom: 8}}>暂无成员，点击下方添加</div>
                            )}
                            {nodes.map((node, index) => (
                                <Space key={index} align="start" style={{display: 'flex', marginBottom: 8}}>
                                    {strategy === 'WORKFLOW' && (
                                        <Tag color="blue" style={{marginTop: 4}}>{index + 1}</Tag>
                                    )}
                                    <Select
                                        placeholder="选择 Agent"
                                        style={{width: 200}}
                                        showSearch
                                        optionFilterProp="label"
                                        value={node.agentId}
                                        onChange={(v) => updateNode(index, {agentId: v})}
                                        options={agents.map(a => ({value: a.id, label: a.agentName}))}
                                    />
                                    <Input
                                        placeholder="节点角色描述（选填）"
                                        style={{width: 280}}
                                        value={node.nodeRole}
                                        onChange={(e) => updateNode(index, {nodeRole: e.target.value})}
                                    />
                                    <Button type="text" danger icon={<MinusCircleOutlined/>}
                                            onClick={() => removeNode(index)}/>
                                </Space>
                            ))}
                            <Button type="dashed" block icon={<PlusOutlined/>} onClick={addNode}>
                                添加成员
                            </Button>
                        </div>
                    </Form.Item>

                    {(strategy === 'SUPERVISOR' || strategy === 'ROUTER') && (
                        <Form.Item name="prompt" label={strategy === 'ROUTER' ? '路由指令' : '主管指令'}>
                            <Input.TextArea rows={3} placeholder="编排级提示词（主管/路由指令）"/>
                        </Form.Item>
                    )}

                    <Form.Item name="avatar" label="头像（emoji）">
                        <Input placeholder="例如：🧩"/>
                    </Form.Item>

                    <Form.Item name="welcomeMessage" label="欢迎语">
                        <Input.TextArea rows={2} placeholder="对话开始时展示的欢迎语"/>
                    </Form.Item>

                    <Form.Item name="flowDesc" label="备注">
                        <Input.TextArea rows={2} placeholder="编排备注"/>
                    </Form.Item>
                </Form>
            </Drawer>

            <Modal
                title="确认删除编排"
                open={!!deleteTarget}
                onOk={handleConfirmDelete}
                onCancel={() => setDeleteTarget(null)}
                okText="确定删除"
                cancelText="取消"
                okButtonProps={{danger: true}}
                destroyOnClose
            >
                <p>删除后不可恢复，确认删除编排 <strong>{deleteTarget?.flowName}</strong> 吗？</p>
            </Modal>
        </div>
    );
};

export default () => (
    <App>
        <FlowManagerPage/>
    </App>
);
