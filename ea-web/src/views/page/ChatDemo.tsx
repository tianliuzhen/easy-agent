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
    SendOutlined,
    RobotOutlined,
    UserOutlined,
    LoadingOutlined,
    EyeOutlined,
    EyeInvisibleOutlined,
    MessageOutlined,
    ClockCircleOutlined,
    DeleteOutlined,
    PlusCircleOutlined
} from '@ant-design/icons';
import {Button, Input, Spin, Card, Badge, Tooltip, Divider, Tag, message} from 'antd';

const {TextArea} = Input;

interface ChatMessage {
    text: string;
    isUser: boolean;
    type: 'data' | 'log';
    id: string;
    timestamp: number;
}

interface ThinkingLogEntry {
    type: 'log' | 'think' | 'data' | 'error' | 'tool';
    content: string;
    timestamp?: number;
}

interface ThinkingLogState {
    [key: string]: {
        content: ThinkingLogEntry[];
        isVisible: boolean;
    }
}

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

// 当前登录用户 ID（暂时固定为 1，后续集成登录功能后再改为动态获取）
const CURRENT_USER_ID = '1';

const ChatDemo: React.FC = () => {
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [thinkingLog, setThinkingLog] = useState<string>('');
    const [input, setInput] = useState('');
    const [isThinking, setIsThinking] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const eventSourceRef = useRef<EventSource | null>(null);
    const chatMessagesEndRef = useRef<HTMLDivElement>(null);
    const thinkingContentRef = useRef<HTMLDivElement>(null);
    const prevThinkingContentLengthRef = useRef<number>(0);
    const [uuid, setUuid] = useState('');
    const location = useLocation();
    const currentAnsweringMsgIdRef = useRef<string | null>(null);
    const [messageThinkingLogs, setMessageThinkingLogs] = useState<ThinkingLogState>({});
    const [selectedChatIndex, setSelectedChatIndex] = useState(-1);
    const [chatHistory, setChatHistory] = useState<ChatMessage[][]>([[]]);
    // 新增：会话列表状态
    const [conversations, setConversations] = useState<ChatConversation[]>([]);
    const [loading, setLoading] = useState(false);
    // Agent详情状态
    const [agentDetail, setAgentDetail] = useState<AgentDetail | null>(null);
    // 当前会话 ID（每次新开对话时从后端获取）
    const [conversationId, setConversationId] = useState<number | null>(null);

    useEffect(() => {
        chatMessagesEndRef.current?.scrollIntoView({behavior: 'smooth'});
    }, [messages, thinkingLog, isThinking]);

    // 思考内容自动滚动到底部
    useEffect(() => {
        if (thinkingContentRef.current && currentAnsweringMsgIdRef.current) {
            const thinkingContent = messageThinkingLogs[currentAnsweringMsgIdRef.current]?.content || [];
            const currentLength = thinkingContent.length;

            // 只有当思考内容增加时才滚动
            if (currentLength > prevThinkingContentLengthRef.current) {
                // 等待下一帧确保DOM已更新
                setTimeout(() => {
                    thinkingContentRef.current?.scrollTo({
                        top: thinkingContentRef.current.scrollHeight,
                        behavior: 'smooth'
                    });
                }, 0);
            }

            // 更新上一次的长度
            prevThinkingContentLengthRef.current = currentLength;
        } else {
            // 如果没有当前思考内容，重置长度计数
            prevThinkingContentLengthRef.current = 0;
        }
    }, [messageThinkingLogs, currentAnsweringMsgIdRef.current]);

    useEffect(() => {
        return () => {
            if (eventSourceRef.current) {
                eventSourceRef.current.close();
            }
        };
    }, []);

    useEffect(() => {
        const newUuid = crypto.randomUUID();
        setUuid(newUuid);

        // 加载用户的会话列表
        loadConversations();
        // 加载Agent详情
        loadAgentDetail();
    }, []);

    // 监听agentId变化，重新加载Agent详情
    useEffect(() => {
        loadAgentDetail();
    }, [location.search]);

    // 加载会话列表
    const loadConversations = async (): Promise<ChatConversation[]> => {
        try {
            setLoading(true);
            // 从路由参数获取 agentId
            const urlParams = new URLSearchParams(location.search);
            const agentId = urlParams.get('agentId') ? parseInt(urlParams.get('agentId')!) : undefined;

            const conversationList = await listConversationsByUserId(CURRENT_USER_ID, agentId, 'active');
            setConversations(conversationList);

            // 如果有会话，显示会话列表但不自动选中任何会话
            // 等待用户发送第一条消息时再创建新会话，或点击会话时加载历史记录
            if (conversationList.length > 0) {
                // 不设置当前会话ID，等待用户发送消息时创建新会话
                // 也不设置选中索引，让用户手动点击选择
                // setCurrentConversationId(lastConversation.id);
                // setSelectedChatIndex(conversationList.length - 1);
                // 注释掉自动加载聊天记录的逻辑，只在用户点击时加载
                // await loadConversation(lastConversation.id);
            }
            return conversationList;
        } catch (error) {
            console.error('加载会话列表失败:', error);
            message.error('加载会话列表失败');
            return [];
        } finally {
            setLoading(false);
        }
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
            // 不显示错误提示，使用默认值
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

    // 创建新会话
    const createNewConversation = async (): Promise<number> => {
        try {
            const urlParams = new URLSearchParams(location.search);
            const agentId = urlParams.get('agentId') ? parseInt(urlParams.get('agentId')!) : 1;

            const newConversationId = await createConversation({
                agentId,
                userId: CURRENT_USER_ID,
                title: '新对话',
                status: 'active'
            });

            console.log('创建新会话成功，会话ID:', newConversationId);
            return newConversationId;
        } catch (error) {
            console.error('创建会话失败:', error);
            message.error('创建会话失败');
            throw error;
        }
    };

    // 加载指定会话的聊天记录
    const loadConversation = async (conversationId: number) => {
        try {
            const chatHistory = await getFullChatHistory(conversationId);

            // 将后端返回的聊天记录转换为前端格式
            const convertedMessages: ChatMessage[] = chatHistory.map((msg: any) => ({
                text: msg.content || '',
                isUser: msg.messageType?.includes('user') || false,
                type: 'data',
                id: msg.id?.toString() || crypto.randomUUID(),
                timestamp: new Date(msg.createdAt).getTime()
            }));

            setMessages(convertedMessages);

            // 更新选中状态
            const index = conversations.findIndex(c => c.id === conversationId);
            if (index !== -1) {
                setSelectedChatIndex(index);
            }
        } catch (error) {
            console.error('加载聊天记录失败:', error);
            message.error('加载聊天记录失败');
        }
    };

    const formatTime = (timestamp: number | string) => {
        const date = typeof timestamp === 'string' ? new Date(timestamp) : new Date(timestamp);
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        const seconds = String(date.getSeconds()).padStart(2, '0');

        return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
    };

    const handleSendMessage = async () => {
        if (input.trim() === '') return;

        // 检查会话ID是否存在，如果不存在则创建新会话
        let activeConversationId = conversationId;
        if (!activeConversationId) {
            try {
                const urlParams = new URLSearchParams(location.search);
                const agentId = urlParams.get('agentId') ? parseInt(urlParams.get('agentId')!) : 1;

                activeConversationId = await createConversation({
                    agentId,
                    userId: CURRENT_USER_ID,
                    status: 'active'
                });

                setConversationId(activeConversationId);
                updateUrlWithSessionId(activeConversationId);

                console.log('创建新会话成功，会话ID:', activeConversationId);
            } catch (error) {
                console.error('创建会话失败:', error);
                message.error('创建会话失败');
                return;
            }
        }

        setError(null);
        const userMessageId = crypto.randomUUID();
        const newUserMessage = {
            text: input,
            isUser: true,
            type: 'data' as const,
            id: userMessageId,
            timestamp: Date.now()
        };

        setMessages(prev => [...prev, newUserMessage]);
        setInput('');
        setIsThinking(true);

        const aiMessageId = crypto.randomUUID();
        currentAnsweringMsgIdRef.current = aiMessageId;

        setMessageThinkingLogs(prev => ({
            ...prev,
            [aiMessageId]: {
                content: [],
                isVisible: true // 默认显示思考过程
            }
        }));

        setThinkingLog('');

        if (eventSourceRef.current) {
            eventSourceRef.current.close();
        }

        const urlParams = new URLSearchParams(location.search);
        const agentId = urlParams.get('agentId') || '1'; // 默认 agentId 为 1

        const eventSource = sendMessage(
            input,
            activeConversationId.toString(), // 使用会话 ID
            agentId, // 传递 agentId 参数
            (log: string) => {
                // 处理 log 类型数据 - 添加到思考过程
                console.log('Received log:', log); // 调试日志
                const currentAiMessageId = currentAnsweringMsgIdRef.current;
                if (currentAiMessageId) {
                    setMessageThinkingLogs(prev => {
                        const currentEntries = prev[currentAiMessageId]?.content || [];
                        return {
                            ...prev,
                            [currentAiMessageId]: {
                                content: [...currentEntries, {type: 'log' as const, content: log}],
                                isVisible: prev[currentAiMessageId]?.isVisible || true
                            }
                        };
                    });
                }
            },
            (finalAnswer: string) => {
                // 处理 finalAnswer 类型数据 - 作为最终结果显示在消息气泡中
                const currentAiMessageId = currentAnsweringMsgIdRef.current;
                if (currentAiMessageId) {
                    setMessages(prev => {
                        const existingMsgIndex = prev.findIndex(msg => msg.id === currentAiMessageId);

                        if (existingMsgIndex >= 0) {
                            // 如果消息已存在，替换整个内容（遵循接口响应处理规范）
                            return prev.map((msg, index) => {
                                if (index === existingMsgIndex) {
                                    return {
                                        ...msg,
                                        text: finalAnswer
                                    };
                                }
                                return msg;
                            });
                        } else {
                            // 如果是新消息，创建新的AI消息
                            return [...prev, {
                                text: finalAnswer,
                                isUser: false,
                                type: 'data',
                                id: currentAiMessageId,
                                timestamp: Date.now()
                            }];
                        }
                    });
                }
            },
            (think: string) => {
                // 处理 think 类型数据 - 添加到思考过程
                console.log('Received think:', think); // 调试日志
                const currentAiMessageId = currentAnsweringMsgIdRef.current;
                if (currentAiMessageId) {
                    setMessageThinkingLogs(prev => {
                        const currentEntries = prev[currentAiMessageId]?.content || [];
                        // 去除可能的[THINK]前缀
                        const content = think.startsWith('[THINK] ') ? think.substring(8) : think;
                        return {
                            ...prev,
                            [currentAiMessageId]: {
                                content: [...currentEntries, {type: 'think' as const, content}],
                                isVisible: prev[currentAiMessageId]?.isVisible || true
                            }
                        };
                    });
                }
            },
            (data: string) => {
                // 处理 data 类型数据 - 添加到思考过程
                console.log('Received data:', data); // 调试日志
                const currentAiMessageId = currentAnsweringMsgIdRef.current;
                if (currentAiMessageId) {
                    setMessageThinkingLogs(prev => {
                        const currentEntries = prev[currentAiMessageId]?.content || [];
                        // 去除可能的[DATA]前缀
                        const content = data.startsWith('[DATA] ') ? data.substring(7) : data;
                        return {
                            ...prev,
                            [currentAiMessageId]: {
                                content: [...currentEntries, {type: 'data' as const, content}],
                                isVisible: prev[currentAiMessageId]?.isVisible || true
                            }
                        };
                    });
                }
            },
            (tool: string) => {
                // 处理 tool 类型数据 - 添加到思考过程
                console.log('Received tool:', tool); // 调试日志
                const currentAiMessageId = currentAnsweringMsgIdRef.current;
                if (currentAiMessageId) {
                    setMessageThinkingLogs(prev => {
                        const currentEntries = prev[currentAiMessageId]?.content || [];
                        return {
                            ...prev,
                            [currentAiMessageId]: {
                                content: [...currentEntries, {type: 'tool' as const, content: tool}],
                                isVisible: prev[currentAiMessageId]?.isVisible || true
                            }
                        };
                    });
                }
            },
            () => {
                setIsThinking(false);
                currentAnsweringMsgIdRef.current = null;

                // 刷新会话列表以显示最新状态
                loadConversations().catch(err => console.error('刷新会话列表失败:', err));
            },
            (errorMessage: string) => {
                const currentAiMessageId = currentAnsweringMsgIdRef.current;
                if (currentAiMessageId) {
                    setMessageThinkingLogs(prev => {
                        const currentEntries = prev[currentAiMessageId]?.content || [];
                        return {
                            ...prev,
                            [currentAiMessageId]: {
                                content: [...currentEntries, {
                                    type: 'error' as const,
                                    content: `思考过程中出现错误: ${errorMessage}`
                                }],
                                isVisible: prev[currentAiMessageId]?.isVisible || true
                            }
                        };
                    });
                }
                setError(errorMessage);
                setIsThinking(false);
                currentAnsweringMsgIdRef.current = null;
            }
        );

        eventSourceRef.current = eventSource;
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
        // 重置会话 ID，在发送消息时再创建新会话
        setConversationId(null);

        // 更新 URL，移除 sessionId 参数
        const urlParams = new URLSearchParams(location.search);
        urlParams.delete('sessionId');
        const newUrl = `${location.pathname}?${urlParams.toString()}`;
        window.history.replaceState({}, '', newUrl);

        // 清空消息列表和选中状态
        setMessages([]);
        setSelectedChatIndex(-1);
        setMessageThinkingLogs({});

        console.log('开始新对话，会话ID已重置');
    };

    const deleteChat = async (index: number) => {
        if (conversations.length <= 1) return;

        try {
            const conversationToDelete = conversations[index];
            await deleteConversation(conversationToDelete.id);

            // 刷新会话列表
            await loadConversations();

            // 如果删除的是当前会话，切换到第一个会话
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
            loadConversation(conversation.id);
        }
    };

    const getChatPreview = (conversation: ChatConversation | null, fullText = false) => {
        if (!conversation) return '新对话';

        if (fullText) {
            return conversation.title || '新对话';
        }

        const title = conversation.title || '新对话';
        return title.substring(0, 30) + (title.length > 30 ? '...' : '');
    };

    // 渲染思考内容，区分不同类型，按顺序分组显示
    const renderThinkingContent = (entries: ThinkingLogEntry[]) => {
        if (!entries || entries.length === 0) return null;

        // 定义类型配置
        const typeConfig = {
            think: {
                title: '🤔 思考过程',
                color: '#1890ff',
                background: '#ffffff',
                border: '#91caff',
                hasContainer: true
            },
            data: {
                title: '💬 开始回答',
                color: '#52c41a',
                background: '#f6ffed',
                border: '#b7eb8f',
                hasContainer: true
            },
            tool: {
                title: '🔧 工具执行',
                color: '#666666',
                background: '#fafafa',
                border: '#d9d9d9',
                hasContainer: true
            },
            log: {
                title: '',
                color: '#999999',
                background: 'transparent',
                border: 'transparent',
                hasContainer: false,
                style: {color: '#999999', fontStyle: 'italic'}
            },
            error: {
                title: '',
                color: '#ff4d4f',
                background: 'transparent',
                border: 'transparent',
                hasContainer: false,
                style: {color: '#ff4d4f', fontWeight: 500}
            }
        };

        // 分组连续的同类型条目
        const groups: Array<{ type: ThinkingLogEntry['type'], entries: ThinkingLogEntry[] }> = [];
        let currentGroup: { type: ThinkingLogEntry['type'], entries: ThinkingLogEntry[] } | null = null;

        entries.forEach(entry => {
            if (!currentGroup || currentGroup.type !== entry.type) {
                // 开始新组
                if (currentGroup) {
                    groups.push(currentGroup);
                }
                currentGroup = {
                    type: entry.type,
                    entries: [entry]
                };
            } else {
                // 添加到当前组
                currentGroup.entries.push(entry);
            }
        });

        // 添加最后一组
        if (currentGroup) {
            groups.push(currentGroup);
        }

        // 渲染分组
        return (
            <>
                {groups.map((group, groupIndex) => {
                    const config = typeConfig[group.type];
                    const content = group.entries.map(entry => entry.content).join(
                        group.type === 'tool' ? '\n' : ''
                    );

                    if (!config.hasContainer) {
                        // log和error类型：单独显示每个条目
                        return (
                            <React.Fragment key={`group-${groupIndex}`}>
                                {group.entries.map((entry, entryIndex) => (
                                    <div
                                        key={`entry-${groupIndex}-${entryIndex}`}
                                        style={config.style}
                                    >
                                        {entry.content}
                                    </div>
                                ))}
                            </React.Fragment>
                        );
                    }

                    // think、data、tool类型：显示带标题的容器
                    return (
                        <div
                            key={`group-${groupIndex}`}
                            style={{
                                background: config.background,
                                border: `1px solid ${config.border}`,
                                borderRadius: '8px',
                                padding: '12px',
                                marginTop: groupIndex > 0 ? '12px' : '0',
                                color: '#000000',
                                whiteSpace: 'pre-wrap',
                                boxShadow: '0 2px 4px rgba(0, 0, 0, 0.05)'
                            }}
                        >
                            <div style={{
                                fontSize: '11px',
                                color: config.color,
                                fontWeight: 600,
                                marginBottom: '8px',
                                display: 'flex',
                                alignItems: 'center',
                                gap: '4px'
                            }}>
                                <span>{config.title}</span>
                            </div>
                            <div style={{
                                fontSize: '12px',
                                lineHeight: 1.6,
                                whiteSpace: 'pre-wrap'
                            }}>
                                {content}
                            </div>
                        </div>
                    );
                })}
            </>
        );
    };

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
                                {agentDetail?.agentName || 'EasyAgent'}
                            </div>
                            <div style={{
                                fontSize: '12px',
                                color: '#666'
                            }}>
                                {agentDetail?.agentDesc}
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
                            transition: 'all 0.3s ease',
                            boxShadow: '0 4px 12px rgba(92, 116, 168, 0.3)',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center'
                        }}
                        onMouseEnter={(e) => {
                            e.currentTarget.style.transform = 'translateY(-2px)';
                            e.currentTarget.style.boxShadow = '0 6px 16px rgba(92, 116, 168, 0.4)';
                        }}
                        onMouseLeave={(e) => {
                            e.currentTarget.style.transform = 'translateY(0)';
                            e.currentTarget.style.boxShadow = '0 4px 12px rgba(92, 116, 168, 0.3)';
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
                            paddingLeft: '8px',
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            whiteSpace: 'nowrap',
                            cursor: 'default'
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
                                transition: 'all 0.3s ease',
                                color: selectedChatIndex === index ? 'white' : '#333',
                                overflow: 'hidden'
                            }}
                            onMouseEnter={(e) => {
                                if (selectedChatIndex !== index) {
                                    e.currentTarget.style.background = '#d1e4f4';
                                    e.currentTarget.style.borderColor = '#c2d6ee';
                                }
                            }}
                            onMouseLeave={(e) => {
                                if (selectedChatIndex !== index) {
                                    e.currentTarget.style.background = 'var(--ea-theme-background)';
                                    e.currentTarget.style.borderColor = '#d9e6f2';
                                }
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
                                            whiteSpace: 'nowrap',
                                            maxWidth: '100%'
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
                                        {conversation.lastMessageTime
                                            ? formatTime(new Date(conversation.lastMessageTime).getTime())
                                            : conversation.formattedCreatedAt
                                                ? formatTime(new Date(conversation.formattedCreatedAt).getTime())
                                                : formatTime(new Date(conversation.createdAt).getTime())}
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
                                        style={{
                                            marginLeft: '8px',
                                            color: selectedChatIndex === index ? 'white' : '#999'
                                        }}
                                    />
                                </Tooltip>
                            )}
                        </Card>
                    ))}

                    {/* 底部背景图标 */}
                    <div style={{
                        position: 'absolute',
                        bottom: '20px',
                        left: '50%',
                        transform: 'translateX(-50%)',
                        opacity: 0.1,
                        pointerEvents: 'none',
                        zIndex: 1
                    }}>
                        <div style={{
                            width: '80px',
                            height: '80px',
                            borderRadius: '20px',
                            background: 'linear-gradient(135deg, #5c74a8 0%, #a9b9d6 100%)',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            boxShadow: '0 8px 24px rgba(92, 116, 168, 0.3)'
                        }}>
                            <MessageOutlined style={{color: 'white', fontSize: '32px'}}/>
                        </div>
                    </div>
                </div>
            </div>

            {/* 右侧聊天区域 */}
            <div style={{
                flex: 1,
                display: 'flex',
                flexDirection: 'column',
                background: 'linear-gradient(135deg, #5c74a8 0%, #a9b9d6 100%)',
                position: 'relative'
            }}>


                {/* 聊天消息区域 */}
                <div style={{
                    flex: 1,
                    overflowY: 'auto',
                    padding: '32px',
                    background: 'rgba(255, 255, 255, 0.85)',
                    backdropFilter: 'blur(10px)',
                    position: 'relative'
                }}>
                    {/* 模型版本显示 */}
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
                            backdropFilter: 'blur(10px)'
                        }}>
                            🚀 模型版本: {getModelVersion()}
                        </div>
                    )}

                    {messages.length === 0 ? (
                        <div style={{
                            display: 'flex',
                            flexDirection: 'column',
                            alignItems: 'center',
                            justifyContent: 'center',
                            height: '100%',
                            color: '#999'
                        }}>
                            <div style={{
                                width: '80px',
                                height: '80px',
                                borderRadius: '20px',
                                background: 'linear-gradient(135deg, #5c74a8 0%, #a9b9d6 100%)',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                marginBottom: '24px',
                                boxShadow: '0 8px 24px rgba(92, 116, 168, 0.3)'
                            }}>
                                <RobotOutlined style={{color: 'white', fontSize: '32px'}}/>
                            </div>
                            <div style={{fontSize: '18px', marginBottom: '8px', color: '#333'}}>
                                开始新的对话
                            </div>
                            <div style={{fontSize: '14px', color: '#666'}}>
                                在下方输入您的消息，智能助手将为您解答
                            </div>
                        </div>
                    ) : (
                        <>
                            {messages.map((msg, index) => {
                                const msgThinkingLog = messageThinkingLogs[msg.id];
                                const hasThinkingLog = msgThinkingLog && msgThinkingLog.content.length > 0;
                                const isThinkingVisible = msgThinkingLog?.isVisible || false;

                                return (
                                    <React.Fragment key={`msg-${msg.id}`}>
                                        {/* 消息主体 */}
                                        <div
                                            style={{
                                                display: 'flex',
                                                justifyContent: msg.isUser ? 'flex-end' : 'flex-start',
                                                marginBottom: '24px',
                                                animation: 'fadeIn 0.3s ease'
                                            }}
                                        >
                                            {!msg.isUser && (
                                                <div style={{
                                                    width: '40px',
                                                    height: '40px',
                                                    borderRadius: '12px',
                                                    background: 'linear-gradient(135deg, #5c74a8 0%, #a9b9d6 100%)',
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    justifyContent: 'center',
                                                    flexShrink: 0,
                                                    marginRight: '16px',
                                                    marginTop: '4px',
                                                    boxShadow: '0 4px 12px rgba(92, 116, 168, 0.3)'
                                                }}>
                                                    <RobotOutlined style={{color: 'white', fontSize: '18px'}}/>
                                                </div>
                                            )}

                                            <div style={{position: 'relative'}}>
                                                {/* 思考过程按钮 - 只在有思考过程时显示 */}
                                                {!msg.isUser && hasThinkingLog && (
                                                    <div style={{
                                                        display: 'flex',
                                                        justifyContent: 'flex-start',
                                                        marginBottom: '8px'
                                                    }}>
                                                    </div>
                                                )}

                                                {/* 消息气泡 */}
                                                <div
                                                    style={{
                                                        backgroundColor: msg.isUser ? 'var(--ea-theme-background)' : 'var(--ea-theme-background)',
                                                        color: '#000000',
                                                        padding: '16px 20px',
                                                        borderRadius: '18px',
                                                        maxWidth: '70%',
                                                        minWidth: '120px',
                                                        wordBreak: 'break-word',
                                                        lineHeight: 1.6,
                                                        fontSize: '14px',
                                                        position: 'relative',
                                                        boxShadow: msg.isUser
                                                            ? '0 4px 15px rgba(102, 126, 234, 0.3)'
                                                            : '0 4px 15px rgba(0, 0, 0, 0.08)',
                                                        border: `1px solid ${msg.isUser ? 'rgba(255, 255, 255, 0.2)' : '#f0f0f0'}`,
                                                        transition: 'all 0.3s ease'
                                                    }}
                                                    onMouseEnter={(e) => {
                                                        e.currentTarget.style.transform = 'translateY(-2px)';
                                                        e.currentTarget.style.boxShadow = msg.isUser
                                                            ? '0 6px 20px rgba(102, 126, 234, 0.4)'
                                                            : '0 6px 20px rgba(0, 0, 0, 0.12)';
                                                    }}
                                                    onMouseLeave={(e) => {
                                                        e.currentTarget.style.transform = 'translateY(0)';
                                                        e.currentTarget.style.boxShadow = msg.isUser
                                                            ? '0 4px 15px rgba(102, 126, 234, 0.3)'
                                                            : '0 4px 15px rgba(0, 0, 0, 0.08)';
                                                    }}
                                                >
                                                    {/* 常驻的思考过程切换按钮 - 只在有思考过程时显示 */}
                                                    {!msg.isUser && hasThinkingLog && (
                                                        <Tooltip
                                                            title={isThinkingVisible ? "隐藏思考过程" : "显示思考过程"}>
                                                            <Button
                                                                type="text"
                                                                size="small"
                                                                icon={isThinkingVisible ? <EyeInvisibleOutlined/> :
                                                                    <EyeOutlined/>}
                                                                onClick={(e) => {
                                                                    e.stopPropagation();
                                                                    toggleThinkingLog(msg.id);
                                                                }}
                                                                style={{
                                                                    position: 'absolute',
                                                                    top: '-10px',
                                                                    right: '-10px',
                                                                    background: 'var(--ea-theme-background)',
                                                                    border: '1px solid #91caff',
                                                                    borderRadius: '16px',
                                                                    padding: '4px 8px',
                                                                    height: '24px',
                                                                    fontSize: '12px',
                                                                    display: 'flex',
                                                                    alignItems: 'center',
                                                                    gap: '4px',
                                                                    boxShadow: '0 2px 6px rgba(0, 0, 0, 0.1)',
                                                                    color: '#1890ff',
                                                                    fontWeight: 500,
                                                                    zIndex: 10,
                                                                    opacity: 1,
                                                                    visibility: 'visible'
                                                                }}
                                                            >
                                                                {/*{isThinkingVisible ? '隐藏' : '思考'}*/}
                                                            </Button>
                                                        </Tooltip>
                                                    )}
                                                    <div style={{whiteSpace: 'pre-wrap'}}>
                                                        {msg.text.replace(/^\n+|\n+$/g, '')}
                                                    </div>
                                                </div>
                                            </div>

                                            {msg.isUser && (
                                                <div style={{
                                                    width: '40px',
                                                    height: '40px',
                                                    borderRadius: '12px',
                                                    background: 'linear-gradient(135deg, #5c74a8 0%, #a9b9d6 100%)',
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    justifyContent: 'center',
                                                    flexShrink: 0,
                                                    marginLeft: '16px',
                                                    marginTop: '4px',
                                                    boxShadow: '0 4px 12px rgba(92, 116, 168, 0.3)'
                                                }}>
                                                    <UserOutlined style={{color: 'white', fontSize: '18px'}}/>
                                                </div>
                                            )}
                                        </div>

                                        {/* 思考过程内容 - 显示在消息下方 */}
                                        {!msg.isUser && hasThinkingLog && isThinkingVisible && (
                                            <div style={{
                                                marginBottom: '16px',
                                                marginLeft: '56px',
                                                maxWidth: '70%'
                                            }}>
                                                <div
                                                    style={{
                                                        background: 'var(--ea-theme-background)',
                                                        borderRadius: '12px',
                                                        padding: '12px 16px',
                                                        border: '1px solid #91caff',
                                                        boxShadow: '0 2px 8px rgba(0, 0, 0, 0.06)',
                                                        transition: 'all 0.3s ease'
                                                    }}
                                                >
                                                    <div style={{
                                                        display: 'flex',
                                                        alignItems: 'center',
                                                        justifyContent: 'space-between',
                                                        fontSize: '13px',
                                                        color: '#1890ff',
                                                        fontWeight: 500,
                                                        marginBottom: '8px'
                                                    }}>
                                                        <div
                                                            style={{display: 'flex', alignItems: 'center', gap: '8px'}}>
                                                            <span>🤔 AI思考过程</span>
                                                            <span style={{
                                                                fontSize: '11px',
                                                                backgroundColor: 'var(--ea-theme-background)',
                                                                padding: '2px 8px',
                                                                borderRadius: '10px'
                                                            }}>
                                已完成
                                                            </span>
                                                        </div>
                                                        <Tooltip title="隐藏">
                                                            <Button
                                                                type="text"
                                                                size="small"
                                                                icon={<EyeInvisibleOutlined/>}
                                                                onClick={() => toggleThinkingLog(msg.id)}
                                                                style={{
                                                                    fontSize: '12px',
                                                                    padding: '0 4px',
                                                                    height: '20px'
                                                                }}
                                                            />
                                                        </Tooltip>
                                                    </div>
                                                    <div style={{
                                                        fontSize: '12px',
                                                        lineHeight: 1.6,
                                                        color: '#333',
                                                        whiteSpace: 'pre-wrap',
                                                        maxHeight: '300px',
                                                        overflowY: 'auto'
                                                    }}>
                                                        {renderThinkingContent(msgThinkingLog.content)}
                                                    </div>
                                                </div>
                                            </div>
                                        )}
                                    </React.Fragment>
                                );
                            })}

                            {/* 正在思考的指示器 */}
                            {isThinking && currentAnsweringMsgIdRef.current && (
                                <div style={{
                                    marginBottom: '24px',
                                    animation: 'fadeIn 0.3s ease'
                                }}>
                                    <div style={{
                                        display: 'flex',
                                        justifyContent: 'flex-start',
                                        alignItems: 'flex-start'
                                    }}>
                                        <div style={{
                                            width: '40px',
                                            height: '40px',
                                            borderRadius: '12px',
                                            background: 'linear-gradient(135deg, #5c74a8 0%, #a9b9d6 100%)',
                                            display: 'flex',
                                            alignItems: 'center',
                                            justifyContent: 'center',
                                            flexShrink: 0,
                                            marginRight: '16px',
                                            marginTop: '4px',
                                            boxShadow: '0 4px 12px rgba(92, 116, 168, 0.3)'
                                        }}>
                                            <RobotOutlined style={{color: 'white', fontSize: '18px'}}/>
                                        </div>

                                        <div style={{position: 'relative'}}>
                                            {/* 思考过程按钮 - 放在思考中气泡上方 */}
                                            {messageThinkingLogs[currentAnsweringMsgIdRef.current] && (
                                                <div style={{
                                                    display: 'flex',
                                                    justifyContent: 'flex-start',
                                                    marginBottom: '8px'
                                                }}>
                                                    <Tooltip
                                                        title={messageThinkingLogs[currentAnsweringMsgIdRef.current]?.isVisible ? "隐藏实时思考" : "显示实时思考"}>
                                                        <Button
                                                            type="text"
                                                            size="small"
                                                            icon={messageThinkingLogs[currentAnsweringMsgIdRef.current]?.isVisible ?
                                                                <EyeInvisibleOutlined/> : <EyeOutlined/>}
                                                            onClick={() => toggleThinkingLog(currentAnsweringMsgIdRef.current!)}
                                                            style={{
                                                                background: 'var(--ea-theme-background)',
                                                                border: '1px solid #91caff',
                                                                borderRadius: '16px',
                                                                padding: '4px 12px',
                                                                height: '28px',
                                                                fontSize: '12px',
                                                                display: 'flex',
                                                                alignItems: 'center',
                                                                gap: '6px',
                                                                boxShadow: '0 2px 6px rgba(0, 0, 0, 0.08)',
                                                                color: '#1890ff',
                                                                fontWeight: 500
                                                            }}
                                                        >
                                                            {messageThinkingLogs[currentAnsweringMsgIdRef.current]?.isVisible ? '隐藏思考' : '实时思考'}
                                                        </Button>
                                                    </Tooltip>
                                                </div>
                                            )}


                                        </div>
                                    </div>

                                    {/* 实时思考过程内容 - 显示在思考中气泡下方 */}
                                    {messageThinkingLogs[currentAnsweringMsgIdRef.current]?.isVisible && (
                                        <div style={{
                                            marginBottom: '16px',
                                            marginLeft: '56px',
                                            maxWidth: '70%',
                                            marginTop: '8px'
                                        }}>
                                            <div
                                                style={{
                                                    background: 'var(--ea-theme-background)',
                                                    borderRadius: '12px',
                                                    padding: '12px 16px',
                                                    border: '1px solid #ffd591',
                                                    boxShadow: '0 2px 8px rgba(0, 0, 0, 0.06)',
                                                    animation: 'pulse 2s infinite'
                                                }}
                                            >
                                                <div style={{
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    justifyContent: 'space-between',
                                                    fontSize: '13px',
                                                    color: '#fa8c16',
                                                    fontWeight: 500,
                                                    marginBottom: '8px'
                                                }}>
                                                    <div style={{display: 'flex', alignItems: 'center', gap: '8px'}}>
                                                        <span>💭 实时思考中</span>
                                                        <Spin size="small"/>
                                                    </div>
                                                    <Tooltip title="隐藏">
                                                        <Button
                                                            type="text"
                                                            size="small"
                                                            icon={<EyeInvisibleOutlined/>}
                                                            onClick={() => toggleThinkingLog(currentAnsweringMsgIdRef.current!)}
                                                            style={{fontSize: '12px', padding: '0 4px', height: '20px'}}
                                                        />
                                                    </Tooltip>
                                                </div>
                                                <div
                                                    ref={thinkingContentRef}
                                                    style={{
                                                        fontSize: '12px',
                                                        lineHeight: 1.6,
                                                        color: '#333',
                                                        whiteSpace: 'pre-wrap',
                                                        maxHeight: '200px',
                                                        overflowY: 'auto'
                                                    }}
                                                >
                                                    {renderThinkingContent(messageThinkingLogs[currentAnsweringMsgIdRef.current]?.content || [])}
                                                </div>
                                            </div>
                                        </div>
                                    )}
                                </div>
                            )}

                            <div ref={chatMessagesEndRef}/>
                        </>
                    )}
                </div>

                {/* 错误提示 */}
                {error && (
                    <div style={{
                        background: 'var(--ea-theme-background)',
                        color: '#ff4d4f',
                        padding: '12px 32px',
                        fontSize: '14px',
                        borderTop: '1px solid #ffccc7',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '8px'
                    }}>
                        <span style={{fontSize: '16px'}}>⚠️</span>
                        {error}
                    </div>
                )}

                {/* 输入区域 */}
                <div style={{
                    background: 'white',
                    padding: '20px 32px',
                    borderTop: '1px solid #e8e8e8',
                    boxShadow: '0 -2px 8px rgba(0, 0, 0, 0.06)'
                }}>
                    <div style={{
                        display: 'flex',
                        gap: '12px',
                        alignItems: 'stretch',
                        width: '100%'
                    }}>
                        <TextArea
                            value={input}
                            onChange={(e) => setInput(e.target.value)}
                            onKeyPress={handleKeyPress}
                            placeholder="输入您的问题...（按 Enter 发送，Shift + Enter 换行）"
                            style={{
                                flex: 1,
                                borderRadius: '20px',
                                border: '1px solid #d9d9d9',
                                fontSize: '14px',
                                padding: '14px 20px',
                                resize: 'vertical',
                                background: 'var(--ea-theme-background)',
                                transition: 'all 0.3s ease',
                                minHeight: '60px',
                                height: '60px'
                            }}
                            onFocus={(e) => {
                                e.currentTarget.style.borderColor = '#667eea';
                                e.currentTarget.style.boxShadow = '0 0 0 2px rgba(102, 126, 234, 0.1)';
                                e.currentTarget.style.background = 'white';
                            }}
                            onBlur={(e) => {
                                e.currentTarget.style.borderColor = '#d9d9d9';
                                e.currentTarget.style.boxShadow = 'none';
                                e.currentTarget.style.background = '#fafafa';
                            }}
                        />
                        <Button
                            type="primary"
                            onClick={handleSendMessage}
                            disabled={isThinking || input.trim() === ''}
                            style={{
                                height: '60px',
                                width: '60px',
                                borderRadius: '50%',
                                padding: 0,
                                background: 'linear-gradient(135deg, #5c74a8 0%, #a9b9d6 100%)',
                                border: 'none',
                                boxShadow: '0 4px 12px rgba(92, 116, 168, 0.4)',
                                transition: 'all 0.3s ease',
                                alignSelf: 'flex-end'
                            }}
                            onMouseEnter={(e) => {
                                e.currentTarget.style.transform = 'scale(1.05)';
                                e.currentTarget.style.boxShadow = '0 6px 20px rgba(102, 126, 234, 0.5)';
                            }}
                            onMouseLeave={(e) => {
                                e.currentTarget.style.transform = 'scale(1)';
                                e.currentTarget.style.boxShadow = '0 4px 12px rgba(102, 126, 234, 0.4)';
                            }}
                            icon={<SendOutlined style={{fontSize: '18px'}}/>}
                        />
                    </div>

                </div>

                <style>{`
          @keyframes fadeIn {
            from {
              opacity: 0;
              transform: translateY(10px);
            }
            to {
              opacity: 1;
              transform: translateY(0);
            }
          }
          
          @keyframes pulse {
            0% {
              opacity: 1;
            }
            50% {
              opacity: 0.8;
            }
            100% {
              opacity: 1;
            }
          }
          
          ::-webkit-scrollbar {
            width: 6px;
            height: 6px;
          }
          
          ::-webkit-scrollbar-track {
            background: rgba(0, 0, 0, 0.05);
            border-radius: 3px;
          }
          
          ::-webkit-scrollbar-thumb {
            background: rgba(0, 0, 0, 0.2);
            border-radius: 3px;
          }
          
          ::-webkit-scrollbar-thumb:hover {
            background: rgba(0, 0, 0, 0.3);
          }
          
          * {
            scrollbar-width: thin;
            scrollbar-color: rgba(0, 0, 0, 0.2) rgba(0, 0, 0, 0.05);
          }
          
          .think-line {
            position: relative;
            display: block;
            width: 100%;
          }
          .think-line::after {
            content: '';
            position: absolute;
            left: 0;
            right: 0;
            bottom: -2px;
            height: 2px;
            background-image: radial-gradient(circle, #1890ff 1px, transparent 1px);
            background-size: 4px 4px;
            background-repeat: repeat-x;
          }
        `}</style>
            </div>
        </div>
    );
};

export default ChatDemo;
