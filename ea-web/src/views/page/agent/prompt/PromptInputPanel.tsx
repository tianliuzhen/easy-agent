import React, {useState, useEffect} from 'react';
import {Card, Typography, Button, Space, Select, Input, message, Divider} from 'antd';
import {SaveOutlined, ReloadOutlined, CopyOutlined, DeleteOutlined, FileTextOutlined} from '@ant-design/icons';

const {Title, Text} = Typography;
const {TextArea} = Input;
const {Option} = Select;

interface PromptTemplate {
    id: number;
    name: string;
    content: string;
    category: string;
    description?: string;
    tag?: string;
}

interface PromptInputPanelProps {
    agentId?: number;
    className?: string;
    onPromptChange?: (content: string) => void;
}

const PromptInputPanel: React.FC<PromptInputPanelProps> = ({
                                                               agentId,
                                                               className,
                                                               onPromptChange
                                                           }) => {
    const [promptContent, setPromptContent] = useState<string>('');
    const [selectedTemplate, setSelectedTemplate] = useState<string>();
    const [templates, setTemplates] = useState<PromptTemplate[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [isSaving, setIsSaving] = useState(false);

    // 模拟提示词模板数据
    const defaultTemplates: PromptTemplate[] = [
        {
            id: 0,
            name: '客服助手',
            content: '你是一位客服助手，正在处理用户的{{ inquiry_type }}。\n                \n                {% if inquiry_type == "投诉" %}\n                请先表达歉意，然后询问具体问题细节。\n                语气要温和诚恳，避免使用推卸责任的表达。\n                {% elif inquiry_type == "咨询" %}\n                请提供准确、详细的信息，如果涉及多个方案可以对比说明。\n                {% elif inquiry_type == "售后" %}\n                请先确认订单信息，再根据用户问题提供相应的售后流程。\n                {% else %}\n                请礼貌询问用户的具体需求，并表示愿意提供帮助。\n                {% endif %}\n                \n                用户问题：{{ user_message }}',
            category: '客户服务',
            description: '用于动态回复客户咨询的标准模板',
            tag: "jinjia"
        },
        {
            id: 1,
            name: '客户咨询回复',
            content: '请根据以下信息回答客户问题：产品特性、价格、使用方法等。语气要专业、友好。',
            category: '客户服务',
            description: '用于回复客户咨询的标准模板',
            tag: ""
        },
        {
            id: 2,
            name: '技术问题解答',
            content: '针对技术问题，提供详细的解决方案和操作步骤，引用相关文档。确保回答准确、清晰。',
            category: '技术支持',
            description: '技术问题解答模板',
            tag: ""
        },
        {
            id: 3,
            name: '销售引导',
            content: '根据客户需求推荐合适的产品，并介绍产品优势和优惠政策。突出产品价值。',
            category: '销售',
            description: '销售引导和产品推荐模板',
            tag: ""
        },
        {
            id: 4,
            name: '内容创作',
            content: '请创作一篇关于以下主题的内容：要求结构清晰、语言生动、有吸引力。',
            category: '创作',
            description: '内容创作和写作模板',
            tag: ""
        },
        {
            id: 5,
            name: '代码助手',
            content: '请帮助编写代码：要求代码规范、有注释、考虑边界情况。',
            category: '编程',
            description: '编程和代码助手模板',
            tag: ""
        },
        {
            id: 6,
            name: '数据分析',
            content: '请分析以下数据：提取关键信息、发现趋势、提供洞察和建议。',
            category: '分析',
            description: '数据分析和报告模板',
            tag: ""
        }
    ];

    // 初始化加载模板
    useEffect(() => {
        loadTemplates();
        // 尝试加载已保存的提示词
        loadSavedPrompt();
    }, [agentId]);

    // 加载模板
    const loadTemplates = async () => {
        setIsLoading(true);
        try {
            // 这里可以调用API获取模板
            // const response = await promptApi.getTemplates(agentId);
            // setTemplates(response.data || defaultTemplates);

            // 暂时使用默认模板
            setTemplates(defaultTemplates);
        } catch (error) {
            console.error('加载模板失败:', error);
            message.error('加载模板失败');
            setTemplates(defaultTemplates);
        } finally {
            setIsLoading(false);
        }
    };

    // 加载已保存的提示词
    const loadSavedPrompt = async () => {
        if (!agentId) return;

        try {
            // 这里可以调用API获取已保存的提示词
            // const response = await promptApi.getAgentPrompt(agentId);
            // if (response.data) {
            //     setPromptContent(response.data.content);
            // }

            // 暂时模拟加载
            const savedPrompt = localStorage.getItem(`agent_prompt_${agentId}`);
            if (savedPrompt) {
                setPromptContent(savedPrompt);
                if (onPromptChange) {
                    onPromptChange(savedPrompt);
                }
            }
        } catch (error) {
            console.error('加载提示词失败:', error);
        }
    };

    // 处理模板选择
    const handleTemplateChange = (value: string) => {
        setSelectedTemplate(value);

        if (value) {
            const template = templates.find(t => t.id.toString() === value);
            if (template) {
                setPromptContent(template.content);
                if (onPromptChange) {
                    onPromptChange(template.content);
                }
                message.success(`已应用模板: ${template.name}`);
            }
        }
    };

    // 处理提示词内容变化
    const handleContentChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
        const newContent = e.target.value;
        setPromptContent(newContent);
        if (onPromptChange) {
            onPromptChange(newContent);
        }
    };

    // 保存提示词
    const handleSave = async () => {
        if (!promptContent.trim()) {
            message.warning('请输入提示词内容');
            return;
        }

        setIsSaving(true);
        try {
            // 这里可以调用API保存提示词
            // await promptApi.savePrompt({
            //     agentId,
            //     content: promptContent
            // });

            // 暂时模拟保存到localStorage
            if (agentId) {
                localStorage.setItem(`agent_prompt_${agentId}`, promptContent);
            }

            message.success('提示词保存成功');
        } catch (error) {
            console.error('保存提示词失败:', error);
            message.error('保存提示词失败');
        } finally {
            setIsSaving(false);
        }
    };

    // 清空提示词
    const handleClear = () => {
        setPromptContent('');
        setSelectedTemplate('');
        if (onPromptChange) {
            onPromptChange('');
        }
        message.info('已清空提示词');
    };

    // 复制提示词
    const handleCopy = () => {
        if (!promptContent.trim()) {
            message.warning('没有内容可复制');
            return;
        }

        navigator.clipboard.writeText(promptContent)
            .then(() => {
                message.success('提示词已复制到剪贴板');
            })
            .catch(err => {
                console.error('复制失败:', err);
                message.error('复制失败');
            });
    };

    // 重置为默认
    const handleReset = () => {
        setPromptContent('');
        setSelectedTemplate('');
        if (onPromptChange) {
            onPromptChange('');
        }
        message.info('已重置提示词');
    };

    // 获取字符统计
    const getCharacterStats = () => {
        const chars = promptContent.length;
        const words = promptContent.trim().split(/\s+/).filter(word => word.length > 0).length;
        const lines = promptContent.split('\n').length;

        return {chars, words, lines};
    };

    const stats = getCharacterStats();

    return (
        <div className={className} style={{
            display: 'flex',
            flexDirection: 'column',
            height: '100%',
            padding: '16px',
            background: '#fff',
            borderRadius: '8px',
            overflow: 'auto'
        }}>
            {/* 标题和操作按钮 */}
            <div style={{marginBottom: '16px', flexShrink: 0}}>
                <div style={{
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '8px',
                    marginBottom: '8px'
                }}>
                    <div style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center'
                    }}>
                        <Title level={5} style={{margin: 0, display: 'flex', alignItems: 'center', gap: '8px'}}>
                            <FileTextOutlined style={{color: '#1890ff', fontSize: '16px'}}/>
                            提示词配置
                        </Title>
                        <Space>
                            <Select
                                value={selectedTemplate}
                                onChange={handleTemplateChange}
                                placeholder="💡选择提示词模板"
                                style={{width: 200}}
                                loading={isLoading}
                                size="small"
                            >
                                {templates.map(template => (
                                    <Option key={template.id} value={template.id.toString()}>
                                        {template.name}
                                        {template.tag && (
                                            <span style={{
                                                background: 'linear-gradient(45deg, #ff6b6b, #f06595, #cc5de8, #845ef7, #5c7cfa, #339af0, #22b8cf, #20c997, #51cf66, #94d82d, #ffd43b, #ff922b)',
                                                WebkitBackgroundClip: 'text',
                                                WebkitTextFillColor: 'transparent',
                                                fontWeight: 'bold',
                                                margin: '0 4px',
                                                fontSize: '15px'
                                            }}>
                                                {template.tag}
                                            </span>
                                        )}
                                        ({template.category})
                                    </Option>

                                ))}
                            </Select>
                        </Space>
                    </div>
                </div>
                <Text type="secondary" style={{fontSize: '12px'}}>
                    配置 Agent 的提示词，指导 AI 的行为和回答风格
                </Text>
            </div>

            {/* 文本输入区域 */}
            <div style={{display: 'flex', flexDirection: 'column', marginBottom: '16px'}}>
                <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '12px',
                    marginBottom: '8px',
                    flexShrink: 0
                }}>
                    <Text strong style={{fontSize: '13px'}}>提示词内容:</Text>
                    {selectedTemplate && (
                        <div style={{
                            background: '#f6ffed',
                            padding: '0px 6px',
                            borderRadius: '4px',
                            border: '1px solid #b7eb8f',
                            fontSize: '12px',
                            color: '#389e0d'
                        }}>
                            {templates.find(t => t.id.toString() === selectedTemplate)?.description}
                        </div>
                    )}
                    <div style={{flex: 1}}/>
                    <Space size="small">
                        <Button
                            type="text"
                            icon={<CopyOutlined/>}
                            onClick={handleCopy}
                            size="small"
                            title="复制"
                        />
                        <Button
                            type="text"
                            icon={<DeleteOutlined/>}
                            onClick={handleClear}
                            size="small"
                            danger
                            title="清空"
                        />
                    </Space>
                </div>

                <TextArea
                    value={promptContent}
                    onChange={handleContentChange}
                    placeholder="请输入提示词内容，指导 AI 如何回答问题..."
                    rows={10}
                    style={{
                        fontFamily: 'Microsoft YaHei, "微软雅黑", sans-serif',
                        fontSize: '14px',
                        lineHeight: '1.5',
                        resize: 'vertical',
                        width: '100%',
                        minHeight: '343px'
                    }}
                />
            </div>

            {/* 统计信息和操作按钮 */}
            <div style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                paddingTop: '12px',
                borderTop: '1px solid #f0f0f0',
                flexShrink: 0
            }}>
                <div style={{fontSize: '12px', color: '#666'}}>
                    <span style={{marginRight: '12px'}}>字符: {stats.chars}</span>
                    <span style={{marginRight: '12px'}}>单词: {stats.words}</span>
                    <span>行数: {stats.lines}</span>
                </div>

                <Space>
                    <Button
                        onClick={handleClear}
                        size="small"
                    >
                        清空
                    </Button>
                    <Button
                        type="primary"
                        onClick={handleSave}
                        loading={isSaving}
                        size="small"
                    >
                        保存
                    </Button>
                </Space>
            </div>

            {/* 使用提示 */}
            <div style={{
                marginTop: '12px',
                padding: '8px 12px',
                background: '#f6f8fa',
                borderRadius: '6px',
                fontSize: '11px',
                color: '#586069',
                flexShrink: 0
            }}>
                <div style={{fontWeight: 'bold', marginBottom: '4px'}}>使用提示:</div>
                <ul style={{margin: 0, paddingLeft: '16px'}}>
                    <li>选择模板快速开始，然后根据需求自定义修改</li>
                    <li>清晰的指令能获得更好的回答质量</li>
                    <li>可以指定回答格式、语气、长度等要求</li>
                </ul>
            </div>
        </div>
    );
};

export default PromptInputPanel;
