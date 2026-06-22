import React, {useState, useEffect, useRef} from 'react';
import {sendMessage} from '../../../api/ChatApi';
import {createConversation} from '../../../api/ChatConversationApi';
import {useLocation} from 'react-router-dom';
import {useAgentConfig} from '../AgentConfigContext';
import {
    SendOutlined,
    DeleteOutlined,
    PlusCircleOutlined
} from '@ant-design/icons';
import {Button, message} from 'antd';

import {
    ChatRightPanel,
    type QuickPromptGroup
} from '../../chat/ChatComponents';
import {eaAgentApi} from '../../../api/EaAgentApi';

// 内联类型定义
interface ChatMessage {
    text: string;
    isUser: boolean;
    type: 'data' | 'log';
    id: string;
    timestamp: number;
    /** 图片数据（Base64 Data URL），仅用户消息可能有 */
    imageBase64?: string;
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
    const [selectedImage, setSelectedImage] = useState<string | undefined>(undefined);
    const [quickPrompts, setQuickPrompts] = useState<QuickPromptGroup[]>([]);
    const location = useLocation();

    // 从props或URL参数获取agentId
    const getAgentId = (): number => {
        if (propAgentId) return propAgentId;
        const urlParams = new URLSearchParams(location.search);
        return urlParams.get('agentId') ? parseInt(urlParams.get('agentId')!) : 1;
    };

    // 加载浮选提示词
    useEffect(() => {
        const agentId = getAgentId();
        if (!agentId) return;
        eaAgentApi.listQuickPrompt(agentId).then(result => {
            if (result && Array.isArray(result.data)) {
                setQuickPrompts(result.data.map((p: any) => ({
                    id: p.id,
                    label: p.label || '',
                    questions: Array.isArray(p.questions) ? p.questions : []
                })));
            }
        }).catch(err => console.error('加载浮选提示词失败:', err));
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [propAgentId, location.search]);

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
        if (!input.trim() && !selectedImage) {
            message.warning('请输入消息内容或选择图片');
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
            timestamp: Date.now(),
            imageBase64: selectedImage,
        };

        setMessages(prev => [...prev, userMessage]);
        setInput('');
        // 发送后清空选中的图片
        const currentImage = selectedImage;
        setSelectedImage(undefined);
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

            // 统一往当前 AI 消息的思考日志追加一条
            const appendEntry = (entry: ThinkingLogEntry) => {
                setMessageThinkingLogs(prev => {
                    const currentLogs = prev[messageId]?.content || [];
                    return {
                        ...prev,
                        [messageId]: {
                            ...prev[messageId],
                            content: [...currentLogs, {...entry, timestamp: Date.now()}]
                        }
                    };
                });
            };

            sendMessage({
                message: input,
                sessionId: currentConversationId!.toString(),
                agentId: agentId.toString(),
                imageBase64: currentImage,
                handlers: {
                    onLog: (logText) => appendEntry({type: 'log', content: logText}),
                    onStep: (evt) => appendEntry({
                        type: 'log',
                        content: `▶ 第 ${evt.index}/${evt.total} 步：${evt.agentName || ''}`
                    }),
                    onFinalAnswer: (finalAnswer) => {
                        setMessages(prev => prev.map(msg =>
                            msg.id === messageId ? {...msg, text: finalAnswer} : msg
                        ));
                        appendEntry({type: 'finalAnswer', content: finalAnswer});
                    },
                    onThink: (thinkingText) => appendEntry({
                        type: 'think',
                        content: thinkingText.startsWith('[THINK] ') ? thinkingText.substring(8) : thinkingText
                    }),
                    onData: (data) => appendEntry({
                        type: 'data',
                        content: data.startsWith('[DATA] ') ? data.substring(7) : data
                    }),
                    onTool: (toolInfo) => appendEntry({type: 'tool', content: toolInfo}),
                    onDone: () => {
                        setIsThinking(false);
                        setCurrentAnsweringMsgId(null);
                    },
                    onError: (errorText) => {
                        appendEntry({type: 'error', content: `${errorText}`});
                        setError(errorText);
                        setIsThinking(false);
                        setCurrentAnsweringMsgId(null);
                    }
                }
            });

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
                padding: '12px 20px',
                borderBottom: '1px solid #eef0f5',
                background: '#fff',
                flexShrink: 0
            }}>
                <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                }}>
                    <div style={{display: 'flex', alignItems: 'center', gap: '12px'}}>
                        {getModelVersion() && (
                            <div style={{
                                fontSize: '13px',
                                color: '#5c74a8',
                                fontWeight: 500,
                                background: '#f0f3fa',
                                borderRadius: '12px',
                                padding: '5px 12px',
                                display: 'inline-flex',
                                alignItems: 'center',
                                gap: '6px',
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
                                {getModelVersion()}
                            </div>
                        )}
                    </div>

                    <Button
                        type="primary"
                        onClick={startNewChat}
                        icon={<PlusCircleOutlined/>}
                        style={{
                            height: '36px',
                            padding: '0 16px',
                            background: 'linear-gradient(135deg, #5c74a8 0%, #7d8fc0 100%)',
                            border: 'none',
                            borderRadius: '12px',
                            fontWeight: 500,
                            boxShadow: '0 4px 12px rgba(92, 116, 168, 0.3)',
                        }}
                    >
                        新对话
                    </Button>
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
                welcomeMessage={agentDetail?.welcomeMessage}
                avatar={agentDetail?.avatar}
                selectedImage={selectedImage}
                onImageChange={setSelectedImage}
                quickPrompts={quickPrompts}
                onQuickQuestion={setInput}
            />
        </div>
    );
};

export default ChatDebugPanel;
