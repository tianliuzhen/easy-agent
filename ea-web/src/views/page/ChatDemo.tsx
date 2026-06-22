import React, {useState, useEffect, useRef} from 'react';
import {sendMessage} from '../api/ChatApi';
import {
    listConversationsByUserId,
    deleteConversation,
    getFullChatHistory,
    queryAgent,
    createConversation,
    type AgentDetail
} from '../api/ChatConversationApi';
import {useLocation} from 'react-router-dom';
import {
    RobotOutlined,
    UserOutlined,
    MessageOutlined,
    ClockCircleOutlined,
    DeleteOutlined,
    PlusCircleOutlined
} from '@ant-design/icons';
import {Button, Input, Spin, Card, Tooltip, message} from 'antd';

import {
    ChatRightPanel,
    type QuickPromptGroup
} from './chat/ChatComponents';
import {eaAgentApi} from '../api/EaAgentApi';
import {flowApi, type Flow} from '../api/FlowApi';

const {TextArea} = Input;

// 聊天会话数据结构
interface ChatConversation {
    id: number;
    title: string;
    agentId: number;
    userId: string;
    messageCount: number;
    lastMessageTime: string;
    status: string;
    createdAt: string;
    updatedAt: string;
    agentName?: string;
    agentAvatar?: string;
    lastMessagePreview?: string;
    formattedCreatedAt?: string;
}

// 类型定义
interface ChatMessage {
    text: string;
    isUser: boolean;
    type: 'data' | 'log';
    id: string;
    timestamp: number;
}

interface ThinkingLogEntry {
    type: 'log' | 'think' | 'data' | 'error' | 'tool' | 'finalAnswer';
    content: string;
    timestamp?: number;
}

interface ThinkingLogState {
    [key: string]: {
        content: ThinkingLogEntry[];
        isVisible: boolean;
    }
}

const CURRENT_USER_ID = '1';

const ChatDemo: React.FC = () => {
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [input, setInput] = useState('');
    const [isThinking, setIsThinking] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const location = useLocation();
    const [currentAnsweringMsgId, setCurrentAnsweringMsgId] = useState<string | null>(null);
    const [messageThinkingLogs, setMessageThinkingLogs] = useState<ThinkingLogState>({});
    const [selectedChatIndex, setSelectedChatIndex] = useState(-1);
    // 新增：会话列表状态
    const [conversations, setConversations] = useState<ChatConversation[]>([]);
    const [loading, setLoading] = useState(false);
    // Agent详情状态
    const [agentDetail, setAgentDetail] = useState<AgentDetail | null>(null);
    // 编排详情状态（URL 带 flowId 时填充）
    const [flowDetail, setFlowDetail] = useState<Flow | null>(null);
    // 当前会话 ID（每次新开对话时从后端获取）
    const [conversationId, setConversationId] = useState<number | null>(null);
    // 新增：选中的图片（Base64 Data URL）
    const [selectedImage, setSelectedImage] = useState<string | undefined>(undefined);
    // 浮选提示词
    const [quickPrompts, setQuickPrompts] = useState<QuickPromptGroup[]>([]);

    // 读取 URL 中的 flowId（编排模式）
    const getFlowId = (): number | undefined => {
        const urlParams = new URLSearchParams(location.search);
        return urlParams.get('flowId') ? parseInt(urlParams.get('flowId')!) : undefined;
    };

    // 编排模式下，会话归属到首节点 Agent（与后端 execFlow 一致）
    const getFlowFirstAgentId = (flow: Flow | null): number | undefined => {
        return flow?.nodes && flow.nodes.length > 0 ? flow.nodes[0].agentId : undefined;
    };

    // 当前会话归属的 Agent ID：编排模式取首节点，单 Agent 模式取 URL
    const currentAgentId = (): number => {
        if (getFlowId()) {
            return getFlowFirstAgentId(flowDetail) ?? 1;
        }
        const urlParams = new URLSearchParams(location.search);
        return urlParams.get('agentId') ? parseInt(urlParams.get('agentId')!) : 1;
    };

    useEffect(() => {
        init();
    }, [location.search]);

    // 统一初始化：按 flowId / agentId 分别加载头部信息与会话列表
    const init = async (): Promise<void> => {
        const flowId = getFlowId();
        if (flowId) {
            // 编排模式：加载编排详情，历史按 flowId 过滤
            try {
                const result = await flowApi.detail(flowId);
                const flow: Flow = result?.data || null;
                setFlowDetail(flow);
                setAgentDetail(null);
                setQuickPrompts([]);
                await loadConversations(undefined, flowId);
            } catch (error) {
                console.error('加载编排详情失败:', error);
            }
        } else {
            // 单 Agent 模式
            setFlowDetail(null);
            await loadAgentDetail();
            await loadQuickPrompts();
            const urlParams = new URLSearchParams(location.search);
            const agentId = urlParams.get('agentId') ? parseInt(urlParams.get('agentId')!) : undefined;
            await loadConversations(agentId);
        }
    };

    // 加载浮选提示词
    const loadQuickPrompts = async (): Promise<void> => {
        try {
            const urlParams = new URLSearchParams(location.search);
            const agentId = urlParams.get('agentId') ? parseInt(urlParams.get('agentId')!) : 1;
            const result = await eaAgentApi.listQuickPrompt(agentId);
            if (result && Array.isArray(result.data)) {
                setQuickPrompts(result.data.map((p: any) => ({
                    id: p.id,
                    label: p.label || '',
                    questions: Array.isArray(p.questions) ? p.questions : []
                })));
            }
        } catch (error) {
            console.error('加载浮选提示词失败:', error);
        }
    };

    // 加载会话列表（编排模式按 flowId 过滤，单 Agent 模式按 agentId 过滤）
    const loadConversations = async (agentId?: number, flowId?: number): Promise<ChatConversation[]> => {
        try {
            setLoading(true);
            const conversationList = await listConversationsByUserId(CURRENT_USER_ID, agentId, flowId, 'active');
            setConversations(conversationList);
            return conversationList;
        } catch (error) {
            console.error('加载会话列表失败:', error);
            message.error('加载会话列表失败');
            return [];
        } finally {
            setLoading(false);
        }
    };

    // 按当前模式刷新会话列表
    const reloadConversations = (): Promise<ChatConversation[]> => {
        const flowId = getFlowId();
        return flowId ? loadConversations(undefined, flowId) : loadConversations(currentAgentId());
    };

    // 加载Agent详情
    const loadAgentDetail = async (): Promise<void> => {
        try {
            const urlParams = new URLSearchParams(location.search);
            const agentId = urlParams.get('agentId') ? parseInt(urlParams.get('agentId')!) : 1;

            const detail = await queryAgent(agentId);
            setAgentDetail(detail);
        } catch (error) {
            console.error('加载Agent详情失败:', error);
        }
    };

    // 获取模型版本号
    const getModelVersion = (): string | null => {
        if (!agentDetail?.modelConfig) {
            return null;
        }
        try {
            const config = JSON.parse(agentDetail.modelConfig);
            return config.modelVersion || config.version || null;
        } catch (error) {
            console.error('解析modelConfig失败:', error);
            return null;
        }
    };

    // 更新 URL 中的会话 ID
    const updateUrlWithSessionId = (sessionId: number) => {
        const urlParams = new URLSearchParams(location.search);
        urlParams.set('sessionId', sessionId.toString());
        const newUrl = `${location.pathname}?${urlParams.toString()}`;
        window.history.replaceState({}, '', newUrl);
    };

    // 解析 messageContext
    const parseMessageContext = (messageContext: string): ThinkingLogEntry[] => {
        try {
            if (!messageContext || messageContext.trim() === '') {
                return [];
            }

            const contextArray = JSON.parse(messageContext);
            if (!Array.isArray(contextArray)) {
                return [];
            }

            const entries: ThinkingLogEntry[] = [];

            contextArray.forEach((item: any) => {
                const type = item.type;
                const value = item.value || '';
                const time = item.time;

                if (type === 'thinking') {
                    entries.push({type: 'think', content: value, timestamp: time});
                } else if (type === 'data') {
                    entries.push({type: 'data', content: value, timestamp: time});
                } else if (type === 'tool') {
                    entries.push({type: 'tool', content: value, timestamp: time});
                } else if (type === 'log') {
                    entries.push({type: 'log', content: value, timestamp: time});
                } else if (type === 'error') {
                    entries.push({type: 'error', content: value, timestamp: time});
                } else if (type === 'finalAnswer') {
                    entries.push({type: 'finalAnswer', content: value, timestamp: time});
                }
            });

            return entries;
        } catch (error) {
            console.error('解析 messageContext 失败:', error);
            return [];
        }
    };

    // 加载指定会话的聊天记录
    const loadConversation = async (convId: number) => {
        try {
            console.log('加载会话记录，conversationId:', convId);
            const chatHistory = await getFullChatHistory(convId);

            const convertedMessages: ChatMessage[] = [];
            const newMessageThinkingLogs: ThinkingLogState = {};

            chatHistory.forEach((msg: any) => {
                const hasMessageContext = msg.messageContext && msg.messageContext.trim() !== '';
                const hasQuestion = msg.question && msg.question.trim() !== '';
                const messageId = msg.id?.toString() || crypto.randomUUID();

                if (hasQuestion && hasMessageContext) {
                    // 用户消息
                    convertedMessages.push({
                        text: msg.question,
                        isUser: true,
                        type: 'data',
                        id: `${messageId}-user`,
                        timestamp: new Date(msg.createdAt).getTime()
                    });

                    // AI消息
                    const aiMessageId = `${messageId}-ai`;
                    const thinkingEntries = parseMessageContext(msg.messageContext);
                    const contextArray = JSON.parse(msg.messageContext);

                    let aiText = '';
                    const finalAnswerEntry = contextArray.find((item: any) => item.type === 'finalAnswer');
                    if (finalAnswerEntry?.value) {
                        aiText = finalAnswerEntry.value;
                    } else {
                        const finalDataEntry = contextArray.find((item: any) =>
                            item.type === 'data' && item.value?.includes('<Final Answer>')
                        );
                        if (finalDataEntry?.value) {
                            const match = finalDataEntry.value.match(/<Final Answer>([\s\S]*?)<\/Final Answer>/);
                            aiText = match ? match[1].trim() : finalDataEntry.value;
                        } else {
                            const dataEntries = contextArray.filter((item: any) => item.type === 'data');
                            if (dataEntries.length > 0) {
                                aiText = dataEntries[dataEntries.length - 1].value || '';
                            }
                        }
                    }

                    convertedMessages.push({
                        text: aiText,
                        isUser: false,
                        type: 'data',
                        id: aiMessageId,
                        timestamp: new Date(msg.createdAt).getTime()
                    });

                    if (thinkingEntries.length > 0) {
                        newMessageThinkingLogs[aiMessageId] = {
                            content: thinkingEntries,
                            isVisible: true
                        };
                    }
                } else if (hasMessageContext) {
                    const isUser = msg.userMessage || msg.messageType?.includes('user') || false;
                    const thinkingEntries = parseMessageContext(msg.messageContext);
                    const contextArray = JSON.parse(msg.messageContext);

                    let text = '';
                    const finalAnswerEntry = contextArray.find((item: any) => item.type === 'finalAnswer');
                    if (finalAnswerEntry?.value) {
                        text = finalAnswerEntry.value;
                    } else {
                        const finalDataEntry = contextArray.find((item: any) =>
                            item.type === 'data' && item.value?.includes('<Final Answer>')
                        );
                        if (finalDataEntry?.value) {
                            const match = finalDataEntry.value.match(/<Final Answer>([\s\S]*?)<\/Final Answer>/);
                            text = match ? match[1].trim() : finalDataEntry.value;
                        } else {
                            const dataEntries = contextArray.filter((item: any) => item.type === 'data');
                            if (dataEntries.length > 0) {
                                text = dataEntries[dataEntries.length - 1].value || '';
                            }
                        }
                    }

                    convertedMessages.push({
                        text,
                        isUser,
                        type: 'data',
                        id: messageId,
                        timestamp: new Date(msg.createdAt).getTime()
                    });

                    if (!isUser && thinkingEntries.length > 0) {
                        newMessageThinkingLogs[messageId] = {
                            content: thinkingEntries,
                            isVisible: true
                        };
                    }
                } else if (hasQuestion) {
                    convertedMessages.push({
                        text: msg.question,
                        isUser: true,
                        type: 'data',
                        id: messageId,
                        timestamp: new Date(msg.createdAt).getTime()
                    });
                }
            });

            setMessages(convertedMessages);
            setMessageThinkingLogs(newMessageThinkingLogs);

            const index = conversations.findIndex(c => c.id === convId);
            if (index !== -1) {
                setSelectedChatIndex(index);
            }
        } catch (error) {
            console.error('加载聊天记录失败:', error);
            message.error('加载聊天记录失败');
        }
    };

    const handleSendMessage = async () => {
        if (input.trim() === '' && !selectedImage) return;

        let activeConversationId = conversationId;
        if (!activeConversationId) {
            try {
                activeConversationId = await createConversation({
                    agentId: currentAgentId(),
                    flowId: getFlowId(),
                    userId: CURRENT_USER_ID,
                    status: 'active'
                });

                setConversationId(activeConversationId);
                updateUrlWithSessionId(activeConversationId);
            } catch (error) {
                console.error('创建会话失败:', error);
                message.error('创建会话失败');
                return;
            }
        }

        setError(null);
        const userMessageId = crypto.randomUUID();
        const newUserMessage: ChatMessage = {
            text: input,
            isUser: true,
            type: 'data' as const,
            id: userMessageId,
            timestamp: Date.now(),
            imageBase64: selectedImage,
        };

        setMessages(prev => [...prev, newUserMessage]);
        setInput('');
        setIsThinking(true);
        // 发送后清空选中的图片
        const currentImage = selectedImage;
        setSelectedImage(undefined);

        const aiMessageId = crypto.randomUUID();
        setCurrentAnsweringMsgId(aiMessageId);

        // 立即创建空的AI消息，显示"思考中..."，避免闪烁
        const newAiMessage = {
            text: '',
            isUser: false,
            type: 'data' as const,
            id: aiMessageId,
            timestamp: Date.now()
        };
        setMessages(prev => [...prev, newAiMessage]);

        setMessageThinkingLogs(prev => ({
            ...prev,
            [aiMessageId]: {
                content: [],
                isVisible: true
            }
        }));

        // 注意：现在 sendMessage 不返回 AbortController，无法手动中断之前的连接
        // 需要确保不会同时发送多个消息，或者依赖后端处理并发请求

        const urlParams = new URLSearchParams(location.search);
        const agentId = urlParams.get('agentId') || '1';
        const flowId = urlParams.get('flowId') || undefined;

        // 统一往当前 AI 消息的思考日志追加一条
        const appendEntry = (entry: ThinkingLogEntry) => {
            setMessageThinkingLogs(prev => {
                const currentEntries = prev[aiMessageId]?.content || [];
                return {
                    ...prev,
                    [aiMessageId]: {
                        content: [...currentEntries, entry],
                        isVisible: prev[aiMessageId]?.isVisible || true
                    }
                };
            });
        };

        sendMessage({
            message: input,
            sessionId: activeConversationId.toString(),
            agentId,
            flowId,
            imageBase64: currentImage,
            handlers: {
                onLog: (log) => appendEntry({type: 'log', content: log}),
                onStep: (evt) => appendEntry({
                    type: 'log',
                    content: `▶ 第 ${evt.index}/${evt.total} 步：${evt.agentName || ''}`
                }),
                onFinalAnswer: (finalAnswer) => {
                    setMessages(prev => {
                        const existingMsgIndex = prev.findIndex(msg => msg.id === aiMessageId);
                        if (existingMsgIndex >= 0) {
                            return prev.map((msg, index) =>
                                index === existingMsgIndex ? {...msg, text: finalAnswer} : msg
                            );
                        }
                        return [...prev, {
                            text: finalAnswer,
                            isUser: false,
                            type: 'data',
                            id: aiMessageId,
                            timestamp: Date.now()
                        }];
                    });
                    appendEntry({type: 'finalAnswer', content: finalAnswer});
                },
                onThink: (think) => appendEntry({
                    type: 'think',
                    content: think.startsWith('[THINK] ') ? think.substring(8) : think
                }),
                onData: (data) => appendEntry({
                    type: 'data',
                    content: data.startsWith('[DATA] ') ? data.substring(7) : data
                }),
                onTool: (tool) => appendEntry({type: 'tool', content: tool}),
                onDone: () => {
                    setIsThinking(false);
                    setCurrentAnsweringMsgId(null);
                    reloadConversations().catch(err => console.error('刷新会话列表失败:', err));
                },
                onError: (errorMessage) => {
                    appendEntry({type: 'error', content: `${errorMessage}`});
                    setError(errorMessage);
                    setIsThinking(false);
                    setCurrentAnsweringMsgId(null);
                }
            }
        });
    };

    const handleKeyPress = (e: React.KeyboardEvent) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSendMessage();
        }
    };

    const toggleThinkingLog = (messageId: string) => {
        setMessageThinkingLogs(prev => {
            const currentState = prev[messageId];
            if (!currentState) return prev;
            return {
                ...prev,
                [messageId]: {
                    ...currentState,
                    isVisible: !currentState.isVisible
                }
            };
        });
    };

    const startNewChat = async () => {
        setConversationId(null);
        setSelectedImage(undefined);
        const urlParams = new URLSearchParams(location.search);
        urlParams.delete('sessionId');
        const newUrl = `${location.pathname}?${urlParams.toString()}`;
        window.history.replaceState({}, '', newUrl);
        setMessages([]);
        setSelectedChatIndex(-1);
        setMessageThinkingLogs({});
    };

    const deleteChat = async (index: number) => {
        if (conversations.length <= 1) return;

        try {
            const conversationToDelete = conversations[index];
            await deleteConversation(conversationToDelete.id);
            await reloadConversations();

            if (index === selectedChatIndex) {
                if (conversations.length > 1) {
                    const newConversations = conversations.filter((_, i) => i !== index);
                    if (newConversations.length > 0) {
                        await loadConversation(newConversations[0].id);
                    }
                }
            }
        } catch (error) {
            console.error('删除会话失败:', error);
            message.error('删除会话失败');
        }
    };

    const selectChat = (index: number) => {
        const conversation = conversations[index];
        if (conversation) {
            setSelectedChatIndex(index);
            loadConversation(conversation.id).catch(err => {
                console.error('加载会话失败:', err);
            });
        }
    };

    const getChatPreview = (conversation: ChatConversation | null, fullText = false) => {
        if (!conversation) return '新对话';
        if (fullText) return conversation.title || '新对话';
        const title = conversation.title || '新对话';
        return title.substring(0, 30) + (title.length > 30 ? '...' : '');
    };

    // 头部展示：编排模式取编排信息，单 Agent 模式取 Agent 信息
    const isFlow = !!getFlowId();
    const displayName = isFlow ? (flowDetail?.flowName || '多 Agent 编排') : (agentDetail?.agentName || 'EasyAgent');
    const displayDesc = isFlow ? flowDetail?.flowDesc : agentDetail?.agentDesc;
    const displayAvatar = isFlow ? flowDetail?.avatar : agentDetail?.avatar;
    const displayWelcome = isFlow ? flowDetail?.welcomeMessage : agentDetail?.welcomeMessage;

    return (
        <div style={{
            display: 'flex',
            height: '100vh',
            background: 'var(--ea-theme-background)',
            fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
        }}>
            {/* 左侧聊天历史栏 */}
            <div style={{
                width: '300px',
                background: 'var(--ea-theme-background)',
                borderRight: '1px solid #d9e6f2',
                display: 'flex',
                flexDirection: 'column',
                padding: '24px 0',
                minWidth: 0
            }}>
                <div style={{
                    padding: '0 16px 20px',
                    borderBottom: '1px solid #d9e6f2'
                }}>
                    <div style={{
                        display: 'flex',
                        alignItems: 'center',
                        marginBottom: '20px'
                    }}>
                        <div style={{
                            width: '40px',
                            height: '40px',
                            borderRadius: '12px',
                            background: 'linear-gradient(135deg, #5c74a8 0%, #a9b9d6 100%)',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            marginRight: '12px',
                            boxShadow: '0 4px 12px rgba(92, 116, 168, 0.3)'
                        }}>
                            <RobotOutlined style={{color: 'white', fontSize: '20px'}}/>
                        </div>
                        <div>
                            <div style={{
                                fontSize: '16px',
                                fontWeight: 600,
                                color: '#333',
                                marginBottom: '4px'
                            }}>
                                {displayName}
                            </div>
                            <div style={{
                                fontSize: '12px',
                                color: '#666'
                            }}>
                                {displayDesc}
                            </div>
                        </div>
                    </div>

                    <Button
                        type="primary"
                        onClick={startNewChat}
                        style={{
                            width: '100%',
                            height: '44px',
                            background: 'linear-gradient(135deg, #5c74a8 0%, #a9b9d6 100%)',
                            border: 'none',
                            borderRadius: '12px',
                            color: 'white',
                            fontWeight: 500,
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center'
                        }}
                    >
                        <PlusCircleOutlined style={{marginRight: '-2px'}}/>
                        开始新对话
                    </Button>
                </div>

                <div style={{
                    flex: 1,
                    overflowY: 'auto',
                    padding: '20px 12px',
                    minWidth: 0,
                    position: 'relative'
                }}>
                    <Tooltip title="聊天历史">
                        <div style={{
                            fontSize: '12px',
                            color: '#999',
                            fontWeight: 500,
                            marginBottom: '12px',
                            paddingLeft: '8px'
                        }}>
                            聊天历史
                        </div>
                    </Tooltip>

                    {loading ? (
                        <div style={{
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            padding: '20px',
                            color: '#999'
                        }}>
                            <Spin size="small"/> 加载会话中...
                        </div>
                    ) : conversations.length === 0 ? (
                        <div style={{
                            textAlign: 'center',
                            padding: '40px 20px',
                            color: '#999'
                        }}>
                            <MessageOutlined style={{fontSize: '48px', marginBottom: '16px', opacity: 0.3}}/>
                            <div style={{fontSize: '14px'}}>暂无会话记录</div>
                        </div>
                    ) : conversations.map((conversation, index) => (
                        <Card
                            key={conversation.id}
                            onClick={() => selectChat(index)}
                            style={{
                                marginBottom: '12px',
                                background: selectedChatIndex === index
                                    ? 'linear-gradient(135deg, #5c74a8 0%, #a9b9d6 100%)'
                                    : 'var(--ea-theme-background)',
                                border: selectedChatIndex === index
                                    ? '1px solid #5c74a8'
                                    : '1px solid #d9e6f2',
                                borderRadius: '12px',
                                cursor: 'pointer',
                                color: selectedChatIndex === index ? 'white' : '#333'
                            }}
                            styles={{
                                body: {
                                    padding: '12px',
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'space-between'
                                }
                            }}
                        >
                            <div style={{display: 'flex', alignItems: 'center', flex: 1}}>
                                <div style={{flex: 1, minWidth: 0}}>
                                    <Tooltip title={getChatPreview(conversation, true)}>
                                        <div style={{
                                            fontSize: '14px',
                                            color: selectedChatIndex === index ? 'white' : '#333',
                                            marginBottom: '4px',
                                            overflow: 'hidden',
                                            textOverflow: 'ellipsis',
                                            whiteSpace: 'nowrap'
                                        }}>
                                            {getChatPreview(conversation)}
                                        </div>
                                    </Tooltip>
                                    <div style={{
                                        fontSize: '11px',
                                        color: selectedChatIndex === index ? 'rgba(255, 255, 255, 0.8)' : '#999',
                                        display: 'flex',
                                        alignItems: 'center',
                                        gap: '4px'
                                    }}>
                                        <ClockCircleOutlined/>
                                        {conversation.lastMessageTime || conversation.createdAt}
                                    </div>
                                </div>
                            </div>

                            {conversations.length > 1 && (
                                <Tooltip title="删除对话">
                                    <Button
                                        type="text"
                                        icon={<DeleteOutlined style={{
                                            color: selectedChatIndex === index ? 'rgba(255, 255, 255, 0.7)' : '#999'
                                        }}/>}
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            deleteChat(index);
                                        }}
                                    />
                                </Tooltip>
                            )}
                        </Card>
                    ))}
                </div>
            </div>

            {/* 右侧聊天面板 - 使用共享组件 */}
            <div style={{
                flex: 1,
                display: 'flex',
                flexDirection: 'column',
                background: 'linear-gradient(135deg, #5c74a8 0%, #a9b9d6 100%)',
                position: 'relative'
            }}>
                {/* 顶部模型版本展示 */}
                {getModelVersion() && (
                    <div style={{
                        position: 'absolute',
                        top: '12px',
                        left: '12px',
                        background: 'rgba(255, 255, 255, 0.9)',
                        border: '1px solid #d9e6f2',
                        borderRadius: '16px',
                        padding: '6px 12px',
                        fontSize: '11px',
                        color: '#666',
                        fontWeight: 500,
                        zIndex: 10,
                        boxShadow: '0 2px 8px rgba(0, 0, 0, 0.06)',
                        backdropFilter: 'blur(10px)',
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '6px'
                    }}>
                        {agentDetail?.modelIcon && (
                            <img 
                                src={agentDetail.modelIcon} 
                                alt="model icon" 
                                style={{
                                    width: '16px',
                                    height: '16px',
                                    objectFit: 'contain'
                                }}
                            />
                        )}
                        模型版本：{getModelVersion()}
                    </div>
                )}

                <ChatRightPanel
                    messages={messages}
                    messageThinkingLogs={messageThinkingLogs}
                    isThinking={isThinking}
                    currentAnsweringMsgId={currentAnsweringMsgId}
                    input={input}
                    onInputChange={setInput}
                    onSend={handleSendMessage}
                    onKeyPress={handleKeyPress}
                    onToggleThinkingLog={toggleThinkingLog}
                    agentName={displayName}
                    modelVersion={null}
                    conversationId={conversationId}
                    error={error}
                    welcomeMessage={displayWelcome}
                    avatar={displayAvatar}
                    selectedImage={selectedImage}
                    onImageChange={setSelectedImage}
                    quickPrompts={quickPrompts}
                    onQuickQuestion={setInput}
                />
            </div>
        </div>
    );
};

export default ChatDemo;
