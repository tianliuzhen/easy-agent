import React, {useState, useEffect} from 'react';
import {Card, Typography, Divider, message, Tooltip, Button, Input, Space, Empty} from 'antd';
import {
    AppstoreOutlined,
    PlusOutlined,
    DeleteOutlined,
    QuestionCircleOutlined,
    SmileOutlined
} from '@ant-design/icons';
import {eaAgentApi} from '../../../api/EaAgentApi';

const {Text} = Typography;
const {TextArea} = Input;

export interface QuickPromptItem {
    id?: number;
    label: string;
    questions: string[];
    sortOrder?: number;
}

/**
 * Agent 个性化组件
 * 包含：浮选提示词配置
 */
const AgentPersonalization: React.FC<{agentId?: number}> = ({agentId}) => {
    const [quickPrompts, setQuickPrompts] = useState<QuickPromptItem[]>([]);
    const [savingPrompts, setSavingPrompts] = useState<boolean>(false);

    // 欢迎语
    const [welcomeMessage, setWelcomeMessage] = useState<string>('');
    const [savingWelcome, setSavingWelcome] = useState<boolean>(false);

    // 加载欢迎语
    useEffect(() => {
        if (agentId) {
            eaAgentApi.queryAgent(agentId).then(result => {
                if (result && result.data) {
                    setWelcomeMessage(result.data.welcomeMessage || '');
                }
            }).catch(err => {
                console.error('加载欢迎语失败:', err);
            });
        }
    }, [agentId]);

    // 保存欢迎语
    const saveWelcomeMessage = async () => {
        if (!agentId) return;
        setSavingWelcome(true);
        try {
            await eaAgentApi.saveAgent({id: agentId, welcomeMessage: welcomeMessage.trim()});
            message.success('欢迎语已保存');
        } catch (error) {
            console.error('保存欢迎语失败:', error);
            message.error('保存失败');
        } finally {
            setSavingWelcome(false);
        }
    };

    // 加载浮选提示词
    useEffect(() => {
        if (agentId) {
            eaAgentApi.listQuickPrompt(agentId).then(result => {
                if (result && Array.isArray(result.data)) {
                    setQuickPrompts(result.data.map((p: any) => ({
                        id: p.id,
                        label: p.label || '',
                        questions: Array.isArray(p.questions) ? p.questions : [],
                        sortOrder: p.sortOrder
                    })));
                }
            }).catch(err => {
                console.error('加载浮选提示词失败:', err);
            });
        }
    }, [agentId]);

    // 浮选提示词：增删改
    const addPromptGroup = () => {
        setQuickPrompts(prev => [...prev, {label: '', questions: ['']}]);
    };
    const removePromptGroup = (index: number) => {
        setQuickPrompts(prev => prev.filter((_, i) => i !== index));
    };
    const updatePromptLabel = (index: number, label: string) => {
        setQuickPrompts(prev => prev.map((p, i) => i === index ? {...p, label} : p));
    };
    const addQuestion = (groupIndex: number) => {
        setQuickPrompts(prev => prev.map((p, i) =>
            i === groupIndex ? {...p, questions: [...p.questions, '']} : p));
    };
    const updateQuestion = (groupIndex: number, qIndex: number, value: string) => {
        setQuickPrompts(prev => prev.map((p, i) =>
            i === groupIndex
                ? {...p, questions: p.questions.map((q, j) => j === qIndex ? value : q)}
                : p));
    };
    const removeQuestion = (groupIndex: number, qIndex: number) => {
        setQuickPrompts(prev => prev.map((p, i) =>
            i === groupIndex
                ? {...p, questions: p.questions.filter((_, j) => j !== qIndex)}
                : p));
    };

    // 保存浮选提示词（全量替换）
    const saveQuickPrompts = async () => {
        if (!agentId) return;
        setSavingPrompts(true);
        try {
            const prompts = quickPrompts
                .map((p, i) => ({
                    id: p.id,
                    label: p.label.trim(),
                    questions: p.questions.map(q => q.trim()).filter(Boolean),
                    sortOrder: i
                }))
                .filter(p => p.label);
            await eaAgentApi.saveQuickPrompt({agentId, prompts});
            message.success('浮选提示词已保存');
        } catch (error) {
            console.error('保存浮选提示词失败:', error);
            message.error('保存失败');
        } finally {
            setSavingPrompts(false);
        }
    };

    return (
        <div style={{display: 'flex', flexDirection: 'column', gap: '16px'}}>
            {/* 欢迎语 */}
            <Card
                size="small"
                style={{borderRadius: '6px'}}
                bodyStyle={{padding: '16px'}}
            >
                <div style={{display: 'flex', alignItems: 'center', justifyContent: 'space-between'}}>
                    <Text style={{
                        fontSize: '14px',
                        fontWeight: 'bold',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '4px'
                    }}>
                        <SmileOutlined style={{fontSize: '14px', color: '#1890ff'}}/>
                        欢迎语
                        <Tooltip title="用户进入对话、尚未发送消息时，展示在聊天区的开场白。">
                            <QuestionCircleOutlined style={{color: '#999', fontSize: '14px', cursor: 'pointer'}}/>
                        </Tooltip>
                    </Text>
                    <Button
                        type="primary"
                        size="small"
                        loading={savingWelcome}
                        onClick={saveWelcomeMessage}
                    >
                        保存
                    </Button>
                </div>

                <Divider style={{margin: '12px 0'}}/>

                <TextArea
                    value={welcomeMessage}
                    onChange={(e) => setWelcomeMessage(e.target.value)}
                    placeholder="如：你好，我是你的售后助手，请问有什么可以帮您？"
                    autoSize={{minRows: 2, maxRows: 5}}
                    maxLength={500}
                    showCount
                />
            </Card>

            {/* 浮选提示词 */}
            <Card
                size="small"
                style={{borderRadius: '6px'}}
                bodyStyle={{padding: '16px'}}
            >
                <div style={{display: 'flex', alignItems: 'center', justifyContent: 'space-between'}}>
                    <Text style={{
                        fontSize: '14px',
                        fontWeight: 'bold',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '4px'
                    }}>
                        <AppstoreOutlined style={{fontSize: '14px', color: '#52c41a'}}/>
                        浮选提示词
                        <Tooltip
                            title="在对话输入框上方展示分类按钮，用户点击按钮可查看推荐问题，点击问题即可快速填充到输入框。">
                            <QuestionCircleOutlined style={{color: '#999', fontSize: '14px', cursor: 'pointer'}}/>
                        </Tooltip>
                    </Text>
                    <Button
                        type="primary"
                        size="small"
                        loading={savingPrompts}
                        onClick={saveQuickPrompts}
                    >
                        保存
                    </Button>
                </div>

                <Divider style={{margin: '12px 0'}}/>

                {quickPrompts.length === 0 && (
                    <Empty
                        image={Empty.PRESENTED_IMAGE_SIMPLE}
                        description="暂无浮选按钮"
                        style={{margin: '12px 0'}}
                    />
                )}

                <Space direction="vertical" size={12} style={{width: '100%'}}>
                    {quickPrompts.map((group, gi) => (
                        <Card
                            key={gi}
                            size="small"
                            style={{borderRadius: '6px', background: '#fafafa'}}
                            bodyStyle={{padding: '12px'}}
                        >
                            <div style={{display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px'}}>
                                <Input
                                    value={group.label}
                                    onChange={(e) => updatePromptLabel(gi, e.target.value)}
                                    placeholder="按钮名称，如 售后/物流/投诉"
                                    size="small"
                                    prefix={<AppstoreOutlined style={{color: '#bbb'}}/>}
                                />
                                <Tooltip title="删除该按钮">
                                    <Button
                                        type="text"
                                        danger
                                        size="small"
                                        icon={<DeleteOutlined/>}
                                        onClick={() => removePromptGroup(gi)}
                                    />
                                </Tooltip>
                            </div>

                            <Space direction="vertical" size={6} style={{width: '100%'}}>
                                {group.questions.map((q, qi) => (
                                    <div key={qi} style={{display: 'flex', alignItems: 'center', gap: '8px'}}>
                                        <Input
                                            value={q}
                                            onChange={(e) => updateQuestion(gi, qi, e.target.value)}
                                            placeholder="推荐问题，如 请输入订单号：eg: 60123123"
                                            size="small"
                                        />
                                        <Button
                                            type="text"
                                            size="small"
                                            icon={<DeleteOutlined/>}
                                            onClick={() => removeQuestion(gi, qi)}
                                        />
                                    </div>
                                ))}
                                <Button
                                    type="dashed"
                                    size="small"
                                    icon={<PlusOutlined/>}
                                    onClick={() => addQuestion(gi)}
                                    style={{width: '100%'}}
                                >
                                    添加推荐问题
                                </Button>
                            </Space>
                        </Card>
                    ))}
                </Space>

                <Button
                    type="dashed"
                    icon={<PlusOutlined/>}
                    onClick={addPromptGroup}
                    style={{width: '100%', marginTop: quickPrompts.length > 0 ? '12px' : 0}}
                >
                    添加浮选按钮
                </Button>
            </Card>
        </div>
    );
};

export default AgentPersonalization;
