import React, {useState, useEffect, useRef} from 'react';
import {sendMessage} from '../../../api/ChatApi';
import {
    createConversation
} from '../../../api/ChatConversationApi';
import {useLocation} from 'react-router-dom';
import {useAgentConfig} from '../AgentConfigContext';
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

// 当前登录用户 ID（暂时固定为 1，后续集成登录功能后再改为动态获取）
const CURRENT_USER_ID = '1';

interface ChatDebugPanelProps {
    agentId?: number;
    className?: string;
}

const ChatDebugPanel: React.FC<ChatDebugPanelProps> = ({ agentId: propAgentId, className }) => {
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
    // 从 Context 获取 Agent 详情
    const { agentDetail } = useAgentConfig();
    // 当前会话 ID（每次新开对话时从后端获取）
    const [conversationId, setConversationId] = useState<number | null>(null);
    const [loading, setLoading] = useState(false);

    // 从props或URL参数获取agentId
    const getAgentId = (): number => {
        if (propAgentId) return propAgentId;
        const urlParams = new URLSearchParams(location.search);
        return urlParams.get('agentId') ? parseInt(urlParams.get('agentId')!) : 1;
    };

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
    }, []);

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
            const agentId = getAgentId();

            const newConversationId = await createConversation({
                agentId,
                userId: CURRENT_USER_ID,
                title: '调试对话',
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

    // 解析 messageContext 中的思考过程
    const parseMessageContext = (messageContext: string, messageId: string): ThinkingLogEntry[] => {
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
                    entries.push({
                        type: 'think',
                        content: value,
                        timestamp: time
                    });
                } else if (type === 'data') {
                    entries.push({
                        type: 'data',
                        content: value,
                        timestamp: time
                    });
                } else if (type === 'tool') {
                    entries.push({
                        type: 'tool',
                        content: value,
                        timestamp: time
                    });
                } else if (type === 'log') {
                    entries.push({
                        type: 'log',
                        content: value,
                        timestamp: time
                    });
                } else if (type === 'error') {
                    entries.push({
                        type: 'error',
                        content: value,
                        timestamp: time
                    });
                } else if (type === 'finalAnswer') {
                    entries.push({
                        type: 'finalAnswer',
                        content: value,
                        timestamp: time
                    });
                }
            });

            return entries;
        } catch (error) {
            console.error('解析 messageContext 失败:', error);
            return [];
        }
    };

    // 开始新对话
    const startNewChat = async () => {
        try {
            // 清空当前消息
            setMessages([]);
            setMessageThinkingLogs({});
            setThinkingLog('');
            currentAnsweringMsgIdRef.current = null;

            // 创建新会话
            const newConversationId = await createNewConversation();
            setConversationId(newConversationId);
            updateUrlWithSessionId(newConversationId);

            message.success('已开始新对话');
        } catch (error) {
            console.error('开始新对话失败:', error);
        }
    };

    // 发送消息
    const handleSendMessage = async () => {
        if (!input.trim()) {
            message.warning('请输入消息内容');
            return;
        }

        if (isThinking) {
            message.warning('正在思考中，请稍候...');
            return;
        }

        const userMessage: ChatMessage = {
            text: input,
            isUser: true,
            type: 'data',
            id: crypto.randomUUID(),
            timestamp: Date.now()
        };

        // 添加到消息列表
        setMessages(prev => [...prev, userMessage]);
        setInput('');
        setIsThinking(true);
        setError(null);

        try {
            // 如果没有会话ID，先创建新会话
            let currentConversationId = conversationId;
            if (!currentConversationId) {
                currentConversationId = await createNewConversation();
                setConversationId(currentConversationId);
                updateUrlWithSessionId(currentConversationId);
            }

            const agentId = getAgentId();
            const messageId = crypto.randomUUID();
            currentAnsweringMsgIdRef.current = messageId;

            // 初始化该消息的思考日志
            setMessageThinkingLogs(prev => ({
                ...prev,
                [messageId]: {
                    content: [],
                    isVisible: true
                }
            }));

            // 创建AI消息占位符
            const aiMessage: ChatMessage = {
                text: '',
                isUser: false,
                type: 'data',
                id: messageId,
                timestamp: Date.now()
            };

            setMessages(prev => [...prev, aiMessage]);

            // 发送消息并处理流式响应
            await sendMessage({
                agentId,
                conversationId: currentConversationId!,
                message: input,
                messageId,
                onThinking: (thinkingText: string) => {
                    setMessageThinkingLogs(prev => {
                        const currentLogs = prev[messageId]?.content || [];
                        return {
                            ...prev,
                            [messageId]: {
                                ...prev[messageId],
                                content: [...currentLogs, {
                                    type: 'think',
                                    content: thinkingText,
                                    timestamp: Date.now()
                                }]
                            }
                        };
                    });
                },
                onToolCall: (toolInfo: string) => {
                    setMessageThinkingLogs(prev => {
                        const currentLogs = prev[messageId]?.content || [];
                        return {
                            ...prev,
                            [messageId]: {
                                ...prev[messageId],
                                content: [...currentLogs, {
                                    type: 'tool',
                                    content: toolInfo,
                                    timestamp: Date.now()
                                }]
                            }
                        };
                    });
                },
                onData: (data: string) => {
                    setMessageThinkingLogs(prev => {
                        const currentLogs = prev[messageId]?.content || [];
                        return {
                            ...prev,
                            [messageId]: {
                                ...prev[messageId],
                                content: [...currentLogs, {
                                    type: 'data',
                                    content: data,
                                    timestamp: Date.now()
                                }]
                            }
                        };
                    });
                },
                onLog: (logText: string) => {
                    setMessageThinkingLogs(prev => {
                        const currentLogs = prev[messageId]?.content || [];
                        return {
                            ...prev,
                            [messageId]: {
                                ...prev[messageId],
                                content: [...currentLogs, {
                                    type: 'log',
                                    content: logText,
                                    timestamp: Date.now()
                                }]
                            }
                        };
                    });
                },
                onError: (errorText: string) => {
                    setMessageThinkingLogs(prev => {
                        const currentLogs = prev[messageId]?.content || [];
                        return {
                            ...prev,
                            [messageId]: {
                                ...prev[messageId],
                                content: [...currentLogs, {
                                    type: 'error',
                                    content: errorText,
                                    timestamp: Date.now()
                                }]
                            }
                        };
                    });
                    setError(errorText);
                },
                onFinalAnswer: (finalAnswer: string) => {
                    // 更新AI消息的文本内容
                    setMessages(prev => prev.map(msg =>
                        msg.id === messageId
                            ? {...msg, text: finalAnswer}
                            : msg
                    ));

                    setMessageThinkingLogs(prev => {
                        const currentLogs = prev[messageId]?.content || [];
                        return {
                            ...prev,
                            [messageId]: {
                                ...prev[messageId],
                                content: [...currentLogs, {
                                    type: 'finalAnswer',
                                    content: finalAnswer,
                                    timestamp: Date.now()
                                }]
                            }
                        };
                    });
                },
                onComplete: () => {
                    setIsThinking(false);
                    currentAnsweringMsgIdRef.current = null;
                }
            });

        } catch (error: any) {
            console.error('发送消息失败:', error);
            setError(error.message || '发送消息失败');
            setIsThinking(false);
            currentAnsweringMsgIdRef.current = null;

            // 移除AI消息占位符
            setMessages(prev => prev.filter(msg => msg.text !== ''));
        }
    };

    // 处理输入框按键
    const handleKeyDown = (e: React.KeyboardEvent) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSendMessage();
        }
    };

    // 切换思考日志的可见性
    const toggleThinkingLogVisibility = (messageId: string) => {
        setMessageThinkingLogs(prev => ({
            ...prev,
            [messageId]: {
                ...prev[messageId],
                isVisible: !prev[messageId]?.isVisible
            }
        }));
    };

    // 清空对话
    const handleClearChat = () => {
        setMessages([]);
        setMessageThinkingLogs({});
        setThinkingLog('');
        setConversationId(null);
        currentAnsweringMsgIdRef.current = null;
        message.success('对话已清空');
    };

    // 格式化时间
    const formatTime = (timestamp: number) => {
        return new Date(timestamp).toLocaleTimeString('zh-CN', {
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        });
    };

    // 渲染思考日志
    const renderThinkingLog = (messageId: string) => {
        const thinkingLog = messageThinkingLogs[messageId];
        if (!thinkingLog || !thinkingLog.content.length) return null;

        const isVisible = thinkingLog.isVisible;

        return (
            <div style={{
                marginTop: '8px',
                border: '1px solid #e8e8e8',
                borderRadius: '8px',
                overflow: 'hidden'
            }}>
                <div
                    style={{
                        padding: '8px 12px',
                        background: '#fafafa',
                        borderBottom: isVisible ? '1px solid #e8e8e8' : 'none',
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        cursor: 'pointer'
                    }}
                    onClick={() => toggleThinkingLogVisibility(messageId)}
                >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <ClockCircleOutlined style={{ color: '#999' }} />
                        <span style={{ fontSize: '12px', color: '#666' }}>
                            思考过程 ({thinkingLog.content.length} 条)
                        </span>
                    </div>
                    {isVisible ? <EyeInvisibleOutlined /> : <EyeOutlined />}
                </div>

                {isVisible && (
                    <div
                        ref={thinkingContentRef}
                        style={{
                            maxHeight: '200px',
                            overflowY: 'auto',
                            padding: '12px',
                            background: '#fff',
                            fontSize: '12px',
                            lineHeight: '1.5'
                        }}
                    >
                        {thinkingLog.content.map((entry, index) => (
                            <div
                                key={index}
                                style={{
                                    marginBottom: '8px',
                                    padding: '8px',
                                    borderRadius: '4px',
                                    background: getLogEntryBackground(entry.type),
                                    borderLeft: `3px solid ${getLogEntryColor(entry.type)}`
                                }}
                            >
                                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
                                    <Tag color={getLogEntryTagColor(entry.type)} style={{ margin: 0 }}>
                                        {getLogEntryTypeText(entry.type)}
                                    </Tag>
                                    {entry.timestamp && (
                                        <span style={{ fontSize: '11px', color: '#999' }}>
                                            {formatTime(entry.timestamp)}
                                        </span>
                                    )}
                                </div>
                                <div style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
                                    {entry.content}
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        );
    };

    // 获取日志条目背景色
    const getLogEntryBackground = (type: ThinkingLogEntry['type']) => {
        switch (type) {
            case 'think': return '#f0f7ff';
            case 'tool': return '#f6ffed';
            case 'data': return '#fff7e6';
            case 'error': return '#fff1f0';
            case 'log': return '#fafafa';
            case 'finalAnswer': return '#f6ffed';
            default: return '#fafafa';
        }
    };

    // 获取日志条目颜色
    const getLogEntryColor = (type: ThinkingLogEntry['type']) => {
        switch (type) {
            case 'think': return '#1890ff';
            case 'tool': return '#52c41a';
            case 'data': return '#fa8c16';
            case 'error': return '#ff4d4f';
            case 'log': return '#8c8c8c';
            case 'finalAnswer': return '#52c41a';
            default: return '#8c8c8c';
        }
    };

    // 获取日志条目标签颜色
    const getLogEntryTagColor = (type: ThinkingLogEntry['type']) => {
        switch (type) {
            case 'think': return 'blue';
            case 'tool': return 'green';
            case 'data': return 'orange';
            case 'error': return 'red';
            case 'log': return 'default';
            case 'finalAnswer': return 'success';
            default: return 'default';
        }
    };

    // 获取日志条目类型文本
    const getLogEntryTypeText = (type: ThinkingLogEntry['type']) => {
        switch (type) {
            case 'think': return '思考';
            case 'tool': return '工具调用';
            case 'data': return '数据';
            case 'error': return '错误';
            case 'log': return '日志';
            case 'finalAnswer': return '最终回答';
            default: return '未知';
        }
    };

    return (
        <div className={className} style={{
            display: 'flex',
            flexDirection: 'column',
            height: '100%',
            background: 'var(--ea-theme-background)',
            fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
        }}>
            {/* 顶部标题栏 */}
            <div style={{
                padding: '16px',
                borderBottom: '1px solid #d9e6f2',
                background: '#fff',
                flexShrink: 0
            }}>
                <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    marginBottom: '12px'
                }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                        <div style={{
                            width: '32px',
                            height: '32px',
                            borderRadius: '8px',
                            background: 'linear-gradient(135deg, #5c74a8 0%, #a9b9d6 100%)',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            boxShadow: '0 2px 8px rgba(92, 116, 168, 0.2)'
                        }}>
                            <RobotOutlined style={{color: 'white', fontSize: '16px'}}/>
                        </div>
                        <div>
                            <div style={{
                                fontSize: '14px',
                                fontWeight: 600,
                                color: '#333',
                                marginBottom: '2px'
                            }}>
                                {agentDetail?.agentName || 'EasyAgent'} 调试
                            </div>
                            <div style={{
                                fontSize: '11px',
                                color: '#666'
                            }}>
                                {agentDetail?.agentDesc || '对话调试面板'}
                                {getModelVersion() && ` · 模型: ${getModelVersion()}`}
                            </div>
                        </div>
                    </div>

                    <div style={{ display: 'flex', gap: '8px' }}>
                        <Button
                            size="small"
                            onClick={startNewChat}
                            icon={<PlusCircleOutlined />}
                        >
                            新对话
                        </Button>
                        <Button
                            size="small"
                            danger
                            onClick={handleClearChat}
                            icon={<DeleteOutlined />}
                        >
                            清空
                        </Button>
                    </div>
                </div>

                {conversationId && (
                    <div style={{
                        fontSize: '11px',
                        color: '#999',
                        marginTop: '4px'
                    }}>
                        会话ID: {conversationId}
                    </div>
                )}
            </div>

            {/* 消息列表区域 */}
            <div style={{
                flex: 1,
                overflowY: 'auto',
                padding: '16px',
                background: '#f8fafc'
            }}>
                {messages.length === 0 ? (
                    <div style={{
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center',
                        justifyContent: 'center',
                        height: '100%',
                        color: '#999',
                        textAlign: 'center'
                    }}>
                        <MessageOutlined style={{ fontSize: '48px', marginBottom: '16px', opacity: 0.3 }} />
                        <div style={{ fontSize: '14px', marginBottom: '8px' }}>开始调试对话</div>
                        <div style={{ fontSize: '12px', color: '#666' }}>输入消息与AI进行对话调试</div>
                    </div>
                ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                        {messages.map((message) => (
                            <div
                                key={message.id}
                                style={{
                                    display: 'flex',
                                    flexDirection: 'column',
                                    alignItems: message.isUser ? 'flex-end' : 'flex-start'
                                }}
                            >
                                <div style={{
                                    display: 'flex',
                                    alignItems: 'flex-start',
                                    gap: '8px',
                                    maxWidth: '85%'
                                }}>
                                    {!message.isUser && (
                                        <div style={{
                                            width: '24px',
                                            height: '24px',
                                            borderRadius: '6px',
                                            background: 'linear-gradient(135deg, #5c74a8 0%, #a9b9d6 100%)',
                                            display: 'flex',
                                            alignItems: 'center',
                                            justifyContent: 'center',
                                            flexShrink: 0,
                                            marginTop: '2px'
                                        }}>
                                            <RobotOutlined style={{color: 'white', fontSize: '12px'}}/>
                                        </div>
                                    )}

                                    <div style={{
                                        background: message.isUser ? '#5c74a8' : '#fff',
                                        color: message.isUser ? 'white' : '#333',
                                        padding: '12px 16px',
                                        borderRadius: '12px',
                                        borderTopLeftRadius: message.isUser ? '12px' : '4px',
                                        borderTopRightRadius: message.isUser ? '4px' : '12px',
                                        boxShadow: '0 2px 8px rgba(0, 0, 0, 0.1)',
                                        wordBreak: 'break-word',
                                        whiteSpace: 'pre-wrap'
                                    }}>
                                        {message.text || (message.isUser ? null : '思考中...')}

                                        <div style={{
                                            fontSize: '10px',
                                            opacity: 0.7,
                                            marginTop: '4px',
                                            textAlign: 'right'
                                        }}>
                                            {formatTime(message.timestamp)}
                                        </div>
                                    </div>

                                    {message.isUser && (
                                        <div style={{
                                            width: '24px',
                                            height: '24px',
                                            borderRadius: '6px',
                                            background: '#1890ff',
                                            display: 'flex',
                                            alignItems: 'center',
                                            justifyContent: 'center',
                                            flexShrink: 0,
                                            marginTop: '2px'
                                        }}>
                                            <UserOutlined style={{color: 'white', fontSize: '12px'}}/>
                                        </div>
                                    )}
                                </div>

                                {/* 思考日志 */}
                                {!message.isUser && renderThinkingLog(message.id)}
                            </div>
                        ))}

                        {isThinking && !currentAnsweringMsgIdRef.current && (
                            <div style={{
                                display: 'flex',
                                alignItems: 'center',
                                gap: '8px',
                                padding: '12px 16px',
                                background: '#fff',
                                borderRadius: '12px',
                                boxShadow: '0 2px 8px rgba(0, 0, 0, 0.1)',
                                maxWidth: '85%'
                            }}>
                                <Spin indicator={<LoadingOutlined style={{ fontSize: 16 }} spin />} />
                                <span style={{ color: '#666' }}>思考中...</span>
                            </div>
                        )}

                        <div ref={chatMessagesEndRef} />
                    </div>
                )}

                {error && (
                    <div style={{
                        padding: '12px',
                        marginTop: '16px',
                        background: '#fff1f0',
                        border: '1px solid #ffccc7',
                        borderRadius: '8px',
                        color: '#ff4d4f'
                    }}>
                        <div style={{ fontWeight: 'bold', marginBottom: '4px' }}>错误</div>
                        <div>{error}</div>
                    </div>
                )}
            </div>

            {/* 输入区域 */}
            <div style={{
                padding: '16px',
                borderTop: '1px solid #d9e6f2',
                background: '#fff',
                flexShrink: 0
            }}>
                <div style={{ display: 'flex', gap: '8px' }}>
                    <TextArea
                        value={input}
                        onChange={(e) => setInput(e.target.value)}
                        onKeyDown={handleKeyDown}
                        placeholder="输入消息进行调试..."
                        autoSize={{ minRows: 1, maxRows: 4 }}
                        style={{ flex: 1 }}
                        disabled={isThinking}
                    />
                    <Button
                        type="primary"
                        icon={<SendOutlined />}
                        onClick={handleSendMessage}
                        loading={isThinking}
                        style={{ height: 'auto' }}
                    >
                        发送
                    </Button>
                </div>
                <div style={{
                    fontSize: '11px',
                    color: '#999',
                    marginTop: '8px',
                    textAlign: 'center'
                }}>
                    Enter 发送，Shift + Enter 换行
                </div>
            </div>
        </div>
    );
};

export default ChatDebugPanel;