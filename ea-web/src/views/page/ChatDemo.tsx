import React, {useState, useEffect, useRef} from 'react';
import {sendMessage} from '..//api/ChatApi';
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
import {Button, Input, Spin, Card, Badge, Tooltip, Divider, Tag} from 'antd';

const {TextArea} = Input;

interface ChatMessage {
    text: string;
    isUser: boolean;
    type: 'data' | 'log';
    id: string;
    timestamp: number;
}

interface ThinkingLogEntry {
    type: 'log' | 'think' | 'data' | 'error';
    content: string;
    timestamp?: number;
}

interface ThinkingLogState {
    [key: string]: {
        content: ThinkingLogEntry[];
        isVisible: boolean;
    }
}

const ChatDemo: React.FC = () => {
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [thinkingLog, setThinkingLog] = useState<string>('');
    const [input, setInput] = useState('');
    const [isThinking, setIsThinking] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const eventSourceRef = useRef<EventSource | null>(null);
    const chatMessagesEndRef = useRef<HTMLDivElement>(null);
    const [uuid, setUuid] = useState('');
    const location = useLocation();
    const currentAnsweringMsgIdRef = useRef<string | null>(null);
    const [messageThinkingLogs, setMessageThinkingLogs] = useState<ThinkingLogState>({});
    const [selectedChatIndex, setSelectedChatIndex] = useState(0);
    const [chatHistory, setChatHistory] = useState<ChatMessage[][]>([[]]);

    useEffect(() => {
        chatMessagesEndRef.current?.scrollIntoView({behavior: 'smooth'});
    }, [messages, thinkingLog, isThinking]);

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

    const formatTime = (timestamp: number) => {
        return new Date(timestamp).toLocaleTimeString('zh-CN', {
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    const handleSendMessage = async () => {
        if (input.trim() === '') return;

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
        const agentId = urlParams.get('agentId') || '1'; // 默认agentId为1

        const eventSource = sendMessage(
            input,
            uuid,
            agentId, // 传递agentId参数
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
                                content: [...currentEntries, { type: 'log' as const, content: log }],
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
                                content: [...currentEntries, { type: 'think' as const, content }],
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
                                content: [...currentEntries, { type: 'data' as const, content }],
                                isVisible: prev[currentAiMessageId]?.isVisible || true
                            }
                        };
                    });
                }
            },
            () => {
                setIsThinking(false);
                currentAnsweringMsgIdRef.current = null;
            },
            (errorMessage: string) => {
                const currentAiMessageId = currentAnsweringMsgIdRef.current;
                if (currentAiMessageId) {
                    setMessageThinkingLogs(prev => {
                        const currentEntries = prev[currentAiMessageId]?.content || [];
                        return {
                            ...prev,
                            [currentAiMessageId]: {
                                content: [...currentEntries, { type: 'error' as const, content: `思考过程中出现错误: ${errorMessage}` }],
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

    const startNewChat = () => {
        setChatHistory(prev => [...prev, []]);
        setSelectedChatIndex(chatHistory.length);
        setMessages([]);
    };

    const deleteChat = (index: number) => {
        if (chatHistory.length <= 1) return;
        const newHistory = chatHistory.filter((_, i) => i !== index);
        setChatHistory(newHistory);
        setSelectedChatIndex(Math.max(0, index - 1));
        setMessages(newHistory[Math.max(0, index - 1)] || []);
    };

    const selectChat = (index: number) => {
        setSelectedChatIndex(index);
        setMessages(chatHistory[index] || []);
    };

    const getChatPreview = (chat: ChatMessage[], fullText = false) => {
        const lastMessage = chat[chat.length - 1];
        if (!lastMessage) return '新对话';

        if (fullText) {
            return lastMessage.text;
        }

        return lastMessage.text.substring(0, 30) + (lastMessage.text.length > 30 ? '...' : '');
    };

    // 渲染思考内容，区分不同类型
    const renderThinkingContent = (entries: ThinkingLogEntry[]) => {
        if (!entries || entries.length === 0) return null;

        // 分离think类型和其他类型的数据
        const thinkEntries = entries.filter(entry => entry.type === 'think');
        const otherEntries = entries.filter(entry => entry.type !== 'think');

        return (
            <>
                {/* 非think类型的内容 */}
                {otherEntries.map((entry, index) => {
                    // 根据条目类型应用样式
                    let lineStyle = {};
                    let lineContent = entry.content;

                    if (entry.type === 'log') {
                        // log类型：浅灰色
                        lineStyle = {color: '#999999', fontStyle: 'italic'};
                    } else if (entry.type === 'data') {
                        // data类型：黑色
                        lineStyle = {color: '#000000'};
                    } else if (entry.type === 'error') {
                        // 错误类型：红色
                        lineStyle = {color: '#ff4d4f', fontWeight: 500};
                    }

                    return (
                        <div key={`other-${index}`} style={lineStyle}>
                            {lineContent}
                        </div>
                    );
                })}

                {/* think类型的内容，放入带边框的独立div中 */}
                {thinkEntries.length > 0 && (
                    <div style={{
                        background: '#ffffff',
                        border: '1px solid #91caff',
                        borderRadius: '8px',
                        padding: '12px',
                        marginTop: otherEntries.length > 0 ? '12px' : '0',
                        boxShadow: '0 2px 4px rgba(0, 0, 0, 0.05)'
                    }}>
                        {thinkEntries.map((entry, index) => (
                            <div 
                                key={`think-${index}`} 
                                style={{
                                    color: '#666666',
                                    fontWeight: 500,
                                    marginBottom: index < thinkEntries.length - 1 ? '8px' : '0'
                                }}
                                className={index < thinkEntries.length - 1 ? "think-line" : ""}
                            >
                                {entry.content}
                            </div>
                        ))}
                    </div>
                )}
            </>
        );
    };

    return (
        <div style={{
            display: 'flex',
            height: '100vh',
            background: '#e1ecf7',
            fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
        }}>
            {/* 左侧聊天历史栏 */}
            <div style={{
                width: '300px',
                background: '#e1ecf7',
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
                                EasyAgent
                            </div>
                            <div style={{
                                fontSize: '12px',
                                color: '#666'
                            }}>
                                智能助手
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

                    {chatHistory.map((chat, index) => (
                        <Card
                            key={index}
                            onClick={() => selectChat(index)}
                            style={{
                                marginBottom: '12px',
                                background: selectedChatIndex === index
                                    ? 'linear-gradient(135deg, #5c74a8 0%, #a9b9d6 100%)'
                                    : '#e1ecf7',
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
                                    e.currentTarget.style.background = '#e1ecf7';
                                    e.currentTarget.style.borderColor = '#d9e6f2';
                                }
                            }}
                            bodyStyle={{
                                padding: '12px',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'space-between'
                            }}
                        >
                            <div style={{display: 'flex', alignItems: 'center', flex: 1}}>
                                <div style={{flex: 1, minWidth: 0}}>
                                    <Tooltip title={getChatPreview(chat, true)}>
                                        <div style={{
                                            fontSize: '14px',
                                            color: selectedChatIndex === index ? 'white' : '#333',
                                            marginBottom: '4px',
                                            overflow: 'hidden',
                                            textOverflow: 'ellipsis',
                                            whiteSpace: 'nowrap',
                                            maxWidth: '100%'
                                        }}>
                                            {getChatPreview(chat)}
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
                                        {chat.length > 0 ? formatTime(chat[chat.length - 1].timestamp) : '--:--'}
                                    </div>
                                </div>
                            </div>

                            {chatHistory.length > 1 && (
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
                    backdropFilter: 'blur(10px)'
                }}>
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
                                                        backgroundColor: msg.isUser ? '#e1ecf7' : '#e1ecf7',
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
                                                                    background: '#e1ecf7',
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
                                                        background: '#e1ecf7',
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
                                                                backgroundColor: '#e1ecf7',
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
                                                                background: '#e1ecf7',
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
                                                    background: '#e1ecf7',
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
                                                <div style={{
                                                    fontSize: '12px',
                                                    lineHeight: 1.6,
                                                    color: '#333',
                                                    whiteSpace: 'pre-wrap',
                                                    maxHeight: '200px',
                                                    overflowY: 'auto'
                                                }}>
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
                        background: '#e1ecf7',
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
                                background: '#e1ecf7',
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
