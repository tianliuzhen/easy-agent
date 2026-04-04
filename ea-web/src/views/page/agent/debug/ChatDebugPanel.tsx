import React, {useState, useEffect, useRef} from 'react';
import {sendMessage} from '../../../api/ChatApi';
import {createConversation} from '../../../api/ChatConversationApi';
import {useLocation} from 'react-router-dom';
import {useAgentConfig} from '../AgentConfigContext';
import {
    SendOutlined,
    RobotOutlined,
    DeleteOutlined,
    PlusCircleOutlined
} from '@ant-design/icons';
import {Button, message} from 'antd';

import {
    ChatRightPanel
} from '../../chat/ChatComponents';

// 内联类型定义
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

interface ChatDebugPanelProps {
    agentId?: number;
    className?: string;
}

const ChatDebugPanel: React.FC<ChatDebugPanelProps> = ({agentId: propAgentId, className}) => {
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [input, setInput] = useState('');
    const [isThinking, setIsThinking] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [currentAnsweringMsgId, setCurrentAnsweringMsgId] = useState<string | null>(null);
    const [messageThinkingLogs, setMessageThinkingLogs] = useState<ThinkingLogState>({});
    const {agentDetail} = useAgentConfig();
    const [conversationId, setConversationId] = useState<number | null>(null);
    const location = useLocation();

    // 从props或URL参数获取agentId
    const getAgentId = (): number => {
        if (propAgentId) return propAgentId;
        const urlParams = new URLSearchParams(location.search);
        return urlParams.get('agentId') ? parseInt(urlParams.get('agentId')!) : 1;
    };

    useEffect(() => {
        return () => {
            // 注意：现在 sendMessage 不返回 AbortController，无法在组件卸载时手动中断连接
            // 依赖 fetchEventSource 自己管理连接生命周期
        };
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

    // 开始新对话
    const startNewChat = async () => {
        try {
            setMessages([]);
            setMessageThinkingLogs({});
            setCurrentAnsweringMsgId(null);

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

        setMessages(prev => [...prev, userMessage]);
        setInput('');
        setIsThinking(true);
        setError(null);

        try {
            let currentConversationId = conversationId;
            if (!currentConversationId) {
                currentConversationId = await createNewConversation();
                setConversationId(currentConversationId);
                updateUrlWithSessionId(currentConversationId);
            }

            const agentId = getAgentId();
            const messageId = crypto.randomUUID();
            setCurrentAnsweringMsgId(messageId);

            setMessageThinkingLogs(prev => ({
                ...prev,
                [messageId]: {
                    content: [],
                    isVisible: true
                }
            }));

            const aiMessage: ChatMessage = {
                text: '',
                isUser: false,
                type: 'data',
                id: messageId,
                timestamp: Date.now()
            };

            setMessages(prev => [...prev, aiMessage]);

            // 使用正确的函数参数形式调用 sendMessage
            sendMessage(
                input,
                currentConversationId!.toString(),
                agentId.toString(),
                (logText: string) => {
                    // onLog
                    const currentMessageId = messageId;
                    setMessageThinkingLogs(prev => {
                        const currentLogs = prev[currentMessageId]?.content || [];
                        return {
                            ...prev,
                            [currentMessageId]: {
                                ...prev[currentMessageId],
                                content: [...currentLogs, {
                                    type: 'log',
                                    content: logText,
                                    timestamp: Date.now()
                                }]
                            }
                        };
                    });
                },
                (finalAnswer: string) => {
                    // onFinalAnswer
                    const currentMessageId = messageId;
                    setMessages(prev => prev.map(msg =>
                        msg.id === currentMessageId
                            ? {...msg, text: finalAnswer}
                            : msg
                    ));

                    setMessageThinkingLogs(prev => {
                        const currentLogs = prev[currentMessageId]?.content || [];
                        return {
                            ...prev,
                            [currentMessageId]: {
                                ...prev[currentMessageId],
                                content: [...currentLogs, {
                                    type: 'finalAnswer',
                                    content: finalAnswer,
                                    timestamp: Date.now()
                                }]
                            }
                        };
                    });
                },
                (thinkingText: string) => {
                    // onThink
                    const currentMessageId = messageId;
                    const content = thinkingText.startsWith('[THINK] ') ? thinkingText.substring(8) : thinkingText;
                    setMessageThinkingLogs(prev => {
                        const currentLogs = prev[currentMessageId]?.content || [];
                        return {
                            ...prev,
                            [currentMessageId]: {
                                ...prev[currentMessageId],
                                content: [...currentLogs, {
                                    type: 'think',
                                    content,
                                    timestamp: Date.now()
                                }]
                            }
                        };
                    });
                },
                (data: string) => {
                    // onData
                    const currentMessageId = messageId;
                    const content = data.startsWith('[DATA] ') ? data.substring(7) : data;
                    setMessageThinkingLogs(prev => {
                        const currentLogs = prev[currentMessageId]?.content || [];
                        return {
                            ...prev,
                            [currentMessageId]: {
                                ...prev[currentMessageId],
                                content: [...currentLogs, {
                                    type: 'data',
                                    content,
                                    timestamp: Date.now()
                                }]
                            }
                        };
                    });
                },
                (toolInfo: string) => {
                    // onTool
                    const currentMessageId = messageId;
                    setMessageThinkingLogs(prev => {
                        const currentLogs = prev[currentMessageId]?.content || [];
                        return {
                            ...prev,
                            [currentMessageId]: {
                                ...prev[currentMessageId],
                                content: [...currentLogs, {
                                    type: 'tool',
                                    content: toolInfo,
                                    timestamp: Date.now()
                                }]
                            }
                        };
                    });
                },
                () => {
                    // onDone
                    setIsThinking(false);
                    setCurrentAnsweringMsgId(null);
                },
                (errorText: string) => {
                    // onError
                    const currentMessageId = messageId;
                    setMessageThinkingLogs(prev => {
                        const currentLogs = prev[currentMessageId]?.content || [];
                        return {
                            ...prev,
                            [currentMessageId]: {
                                ...prev[currentMessageId],
                                content: [...currentLogs, {
                                    type: 'error',
                                    content: `${errorText}`,
                                    timestamp: Date.now()
                                }]
                            }
                        };
                    });
                    setError(errorText);
                    setIsThinking(false);
                    setCurrentAnsweringMsgId(null);
                }
            );

        } catch (error: any) {
            console.error('发送消息失败:', error);
            setError(error.message || '发送消息失败');
            setIsThinking(false);
            setCurrentAnsweringMsgId(null);
            setMessages(prev => prev.filter(msg => msg.text !== ''));
        }
    };

    // 处理输入框按键
    const handleKeyPress = (e: React.KeyboardEvent) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSendMessage();
        }
    };

    // 切换思考日志的可见性
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

    // 清空对话
    const handleClearChat = () => {
        setMessages([]);
        setMessageThinkingLogs({});
        setConversationId(null);
        setCurrentAnsweringMsgId(null);
        message.success('对话已清空');
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
                padding: '6px 6px 6px',
                borderBottom: '1px solid #d9e6f2',
                background: '#fff',
                flexShrink: 0
            }}>
                <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                }}>
                    <div style={{display: 'flex', alignItems: 'center', gap: '12px'}}>
                        <div style={{
                            width: '30px',
                            height: '30px',
                            borderRadius: '8px',
                            background: 'linear-gradient(135deg, #5c74a8 0%, #a9b9d6 100%)',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            boxShadow: '0 4px 12px rgba(92, 116, 168, 0.3)'
                        }}>
                            <RobotOutlined style={{color: 'white', fontSize: '20px'}}/>
                        </div>
                        <div>
                            {getModelVersion() && (
                                <div style={{
                                    fontSize: '11px',
                                    color: '#666',
                                    fontWeight: 500,
                                    background: 'rgba(255, 255, 255, 0.9)',
                                    border: '1px solid #d9e6f2',
                                    borderRadius: '16px',
                                    padding: '4px 10px',
                                    display: 'inline-block',
                                    boxShadow: '0 2px 8px rgba(0, 0, 0, 0.06)'
                                }}>
                                    🚀 模型版本：{getModelVersion()}
                                </div>
                            )}
                        </div>
                    </div>

                    <div style={{display: 'flex', gap: '12px'}}>
                        <Button
                            type="primary"
                            onClick={startNewChat}
                            icon={<PlusCircleOutlined/>}
                            style={{
                                height: '30px',
                                width: '78px',
                                background: 'linear-gradient(135deg, #5c74a8 0%, #a9b9d6 100%)',
                                border: 'none',
                                borderRadius: '10px',
                                fontWeight: 500
                            }}
                        >
                            新对话
                        </Button>
                        {/*<Button*/}
                        {/*    danger*/}
                        {/*    onClick={handleClearChat}*/}
                        {/*    icon={<DeleteOutlined/>}*/}
                        {/*    style={{height: '30px', borderRadius: '10px'}}*/}
                        {/*>*/}
                        {/*    清空*/}
                        {/*</Button>*/}
                    </div>
                </div>
            </div>

            {/* 右侧聊天面板 */}
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
                agentName={agentDetail?.agentName || 'EasyAgent'}
                modelVersion={getModelVersion()}
                conversationId={conversationId}
                error={error}
                emptyTitle="开始调试对话"
                emptySubtitle="输入消息与AI进行对话调试"
            />
        </div>
    );
};

export default ChatDebugPanel;
