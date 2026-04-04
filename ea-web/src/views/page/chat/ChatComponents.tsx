import React, { useRef, useEffect } from 'react';
import { Button, Input, Spin, Tooltip } from 'antd';
import {
    SendOutlined,
    RobotOutlined,
    UserOutlined,
    EyeOutlined,
    EyeInvisibleOutlined,
} from '@ant-design/icons';
import type { ThoughtChainProps } from '@ant-design/x';
import { ThoughtChain } from '@ant-design/x';

// ==================== 类型定义 ====================
export interface ThinkingLogEntry {
    type: 'log' | 'think' | 'data' | 'error' | 'tool' | 'finalAnswer';
    content: string;
    timestamp?: number;
}

export interface ThinkingLogState {
    [key: string]: {
        content: ThinkingLogEntry[];
        isVisible: boolean;
    }
}

export interface ChatMessage {
    text: string;
    isUser: boolean;
    type: 'data' | 'log';
    id: string;
    timestamp: number;
}

// ==================== 类型配置 ====================
const typeConfig = {
    think: { title: '思考', icon: '🤔', description: '', color: '#1677ff', status: 'pending' as const },
    data: { title: '回答', icon: '💬', description: '', color: '#52c41a', status: 'success' as const },
    tool: { title: '工具', icon: '🔧', description: '', color: '#fa8c16', status: 'success' as const },
    log: { title: '日志', icon: '📝', description: '运行日志', color: '#8c8c8c', status: 'default' as const },
    error: { title: '错误', icon: '⚠️', description: '', color: '#ff4d4f', status: 'error' as const },
    finalAnswer: { title: '最终答案', icon: '✅', description: '', color: '#52c41a', status: 'success' as const },
};

// ==================== 渲染思考内容 ====================
export const renderThinkingContent = (entries: ThinkingLogEntry[], isRealTime: boolean = false) => {
    if (!entries || entries.length === 0) return null;

    // 合并相同类型的连续 entry
    // 注意：SSE 流式数据可能将一个完整消息分成多个 chunk，需要正确拼接
    const merged: ThinkingLogEntry[] = [];
    for (const entry of entries) {
        const lastEntry = merged[merged.length - 1];
        if (lastEntry && lastEntry.type === entry.type) {
            // 直接使用原始内容拼接，不要用 trim() 或 includes 判断
            // 因为流式数据是连续的，直接追加即可
            lastEntry.content = lastEntry.content + entry.content;
        } else {
            merged.push({ ...entry });
        }
    }

    const items: ThoughtChainProps['items'] = merged.map((entry, index) => {
        const config = typeConfig[entry.type] || typeConfig.log;
        const isLastItem = index === merged.length - 1;

        return {
            key: `${entry.type}-${index}`,
            title: `${config.icon} ${config.title}`,
            description: config.description,
            icon: config.icon,
            status: isRealTime && isLastItem ? 'loading' : config.status,
            collapsible: true,
            content: (
                <div style={{ fontSize: '12px', lineHeight: 1.6, color: '#333', whiteSpace: 'pre-wrap', padding: '8px 0' }}>
                    {entry.content}
                </div>
            ),
        };
    });

    return (
        <div style={{ marginTop: '8px' }}>
            <ThoughtChain defaultExpandedKeys={items.map(item => item.key!)} items={items} line="dashed" />
        </div>
    );
};

// ==================== 思考过程切换按钮 ====================
export const ThinkingLogToggleButton: React.FC<{
    messageId: string;
    isVisible: boolean;
    onToggle: (messageId: string) => void;
}> = ({ messageId, isVisible, onToggle }) => (
    <Tooltip title={isVisible ? "隐藏思考过程" : "显示思考过程"}>
        <Button
            type="text"
            size="small"
            icon={isVisible ? <EyeInvisibleOutlined /> : <EyeOutlined />}
            onClick={(e) => {
                e.stopPropagation();
                onToggle(messageId);
            }}
            style={{
                position: 'absolute',
                top: '-10px',
                right: '-10px',
                background: '#fff',
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
            }}
        />
    </Tooltip>
);

// ==================== 单条消息组件 ====================
export interface ChatMessageItemProps {
    msg: ChatMessage;
    messageThinkingLogs: ThinkingLogState;
    onToggleThinkingLog: (messageId: string) => void;
    renderThinkingContentFn: (entries: ThinkingLogEntry[], isRealTime?: boolean) => React.ReactNode;
    currentAnsweringMsgId: string | null;
}

export const ChatMessageItem: React.FC<ChatMessageItemProps> = ({
    msg,
    messageThinkingLogs,
    onToggleThinkingLog,
    renderThinkingContentFn,
    currentAnsweringMsgId,
}) => {
    const msgThinkingLog = messageThinkingLogs[msg.id];
    const hasThinkingLog = msgThinkingLog && msgThinkingLog.content.length > 0;
    const isThinkingVisible = msgThinkingLog?.isVisible || false;
    const isThinkingMessage = !msg.isUser &&
        (msg.text.trim() === '' || msg.text === '思考中...') &&
        currentAnsweringMsgId === msg.id;

    const avatarStyle: React.CSSProperties = {
        width: '40px',
        height: '40px',
        borderRadius: '12px',
        background: 'linear-gradient(135deg, #5c74a8 0%, #a9b9d6 100%)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        flexShrink: 0,
        boxShadow: '0 4px 12px rgba(92, 116, 168, 0.3)',
    };

    return (
        <React.Fragment key={`msg-${msg.id}`}>
            <div style={{
                display: 'flex',
                justifyContent: msg.isUser ? 'flex-end' : 'flex-start',
                marginBottom: '24px',
                animation: 'fadeIn 0.3s ease',
            }}>
                {!msg.isUser && (
                    <div style={{ ...avatarStyle, marginRight: '16px', marginTop: '4px' }}>
                        <RobotOutlined style={{ color: 'white', fontSize: '18px' }} />
                    </div>
                )}

                <div style={{ position: 'relative' }}>
                    <div style={{
                        backgroundColor: '#fff',
                        color: '#000',
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
                    }}>
                        {!msg.isUser && hasThinkingLog && (
                            <ThinkingLogToggleButton
                                messageId={msg.id}
                                isVisible={isThinkingVisible}
                                onToggle={onToggleThinkingLog}
                            />
                        )}
                        <div style={{ whiteSpace: 'pre-wrap' }}>
                            {isThinkingMessage ? (
                                <>
                                    <Spin size="small" />
                                    {msg.text}
                                </>
                            ) : (
                                msg.text.replace(/^\n+|\n+$/g, '')
                            )}
                        </div>
                    </div>
                </div>

                {msg.isUser && (
                    <div style={{ ...avatarStyle, marginLeft: '16px', marginTop: '4px' }}>
                        <UserOutlined style={{ color: 'white', fontSize: '18px' }} />
                    </div>
                )}
            </div>

            {/* 已完成的思考过程 */}
            {!msg.isUser && hasThinkingLog && isThinkingVisible && currentAnsweringMsgId !== msg.id && (
                <div style={{ marginBottom: '16px', marginLeft: '56px', maxWidth: '70%' }}>
                    <div style={{
                        background: '#fff',
                        borderRadius: '12px',
                        padding: '12px 16px',
                        boxShadow: '0 2px 8px rgba(0, 0, 0, 0.06)',
                    }}>
                        <div style={{
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between',
                            fontSize: '13px',
                            color: '#1890ff',
                            fontWeight: 500,
                            marginBottom: '8px',
                        }}>
                            <div style={{ display: 'flex', alignItems: 'center' }}>
                                <span>✅ </span>
                                <span style={{ fontSize: '11px', backgroundColor: '#fff', padding: '2px 4px', borderRadius: '10px' }}>
                                    已完成
                                </span>
                            </div>
                            <Tooltip title="隐藏">
                                <Button
                                    type="text"
                                    size="small"
                                    icon={<EyeInvisibleOutlined />}
                                    onClick={() => onToggleThinkingLog(msg.id)}
                                    style={{ fontSize: '12px', padding: '0 4px', height: '20px' }}
                                />
                            </Tooltip>
                        </div>
                        <div style={{
                            fontSize: '12px',
                            lineHeight: 1.6,
                            color: '#333',
                            whiteSpace: 'pre-wrap',
                            maxHeight: '500px',
                            overflowY: 'auto',
                        }}>
                            {renderThinkingContentFn(msgThinkingLog.content, false)}
                        </div>
                    </div>
                </div>
            )}
        </React.Fragment>
    );
};

// ==================== 实时思考指示器 ====================
export interface ThinkingIndicatorProps {
    messageThinkingLogs: ThinkingLogState;
    currentAnsweringMsgId: string | null;
    onToggleThinkingLog: (messageId: string) => void;
    renderThinkingContentFn: (entries: ThinkingLogEntry[], isRealTime?: boolean) => React.ReactNode;
    thinkingContentRef: React.RefObject<HTMLDivElement>;
}

export const ThinkingIndicator: React.FC<ThinkingIndicatorProps> = ({
    messageThinkingLogs,
    currentAnsweringMsgId,
    onToggleThinkingLog,
    renderThinkingContentFn,
    thinkingContentRef,
}) => {
    if (!currentAnsweringMsgId || !messageThinkingLogs[currentAnsweringMsgId]) {
        return null;
    }

    const isVisible = messageThinkingLogs[currentAnsweringMsgId]?.isVisible || false;
    const content = messageThinkingLogs[currentAnsweringMsgId]?.content || [];

    return (
        <div style={{ marginBottom: '24px', animation: 'fadeIn 0.3s ease' }}>
            {isVisible && (
                <div style={{ marginBottom: '16px', marginLeft: '56px', maxWidth: '70%', marginTop: '8px' }}>
                    <div style={{
                        background: '#fff',
                        borderRadius: '12px',
                        padding: '12px 16px',
                        boxShadow: '0 2px 8px rgba(0, 0, 0, 0.06)',
                        animation: 'pulse 2s infinite',
                    }}>
                        <div style={{
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between',
                            fontSize: '13px',
                            color: '#fa8c16',
                            fontWeight: 500,
                            marginBottom: '8px',
                        }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                <span>💭 分析中</span>
                                <Spin size="small" />
                            </div>
                            <Tooltip title="隐藏">
                                <Button
                                    type="text"
                                    size="small"
                                    icon={<EyeInvisibleOutlined />}
                                    onClick={() => onToggleThinkingLog(currentAnsweringMsgId!)}
                                    style={{ fontSize: '12px', padding: '0 4px', height: '20px' }}
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
                                maxHeight: '400px',
                                overflowY: 'auto',
                            }}
                        >
                            {renderThinkingContentFn(content, true)}
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

// ==================== 输入区域 ====================
export interface ChatInputAreaProps {
    value: string;
    onChange: (value: string) => void;
    onSend: () => void;
    onKeyPress?: (e: React.KeyboardEvent) => void;
    disabled?: boolean;
    placeholder?: string;
}

export const ChatInputArea: React.FC<ChatInputAreaProps> = ({
    value,
    onChange,
    onSend,
    onKeyPress,
    disabled = false,
    placeholder = "输入您的问题...（按 Enter 发送，Shift + Enter 换行）",
}) => {
    const { TextArea } = Input;

    return (
        <div style={{
            background: 'white',
            padding: '20px 32px',
            borderTop: '1px solid #e8e8e8',
            boxShadow: '0 -2px 8px rgba(0, 0, 0, 0.06)',
        }}>
            <div style={{ display: 'flex', gap: '12px', alignItems: 'stretch', width: '100%' }}>
                <TextArea
                    value={value}
                    onChange={(e) => onChange(e.target.value)}
                    onKeyPress={onKeyPress}
                    placeholder={placeholder}
                    disabled={disabled}
                    style={{
                        flex: 1,
                        borderRadius: '20px',
                        border: '1px solid #d9d9d9',
                        fontSize: '14px',
                        padding: '14px 20px',
                        resize: 'vertical',
                        background: 'var(--ea-theme-background)',
                        minHeight: '60px',
                        height: '60px',
                    }}
                />
                <Button
                    type="primary"
                    onClick={onSend}
                    disabled={disabled || value.trim() === ''}
                    icon={<SendOutlined style={{ fontSize: '20px' }} />}
                    style={{
                        height: '60px',
                        width: '60px',
                        borderRadius: '50%',
                        padding: 0,
                        background: 'linear-gradient(135deg, #5c74a8 0%, #a9b9d6 100%)',
                        border: 'none',
                        boxShadow: '0 4px 12px rgba(92, 116, 168, 0.4)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                    }}
                />
            </div>
        </div>
    );
};

// ==================== 空状态 ====================
export interface ChatEmptyStateProps {
    agentName?: string;
    title?: string;
    subtitle?: string;
}

export const ChatEmptyState: React.FC<ChatEmptyStateProps> = ({
    agentName = 'EasyAgent',
    title = '开始新的对话',
    subtitle = '在下方输入您的消息，智能助手将为您解答',
}) => (
    <div style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        height: '100%',
        color: '#999',
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
            boxShadow: '0 8px 24px rgba(92, 116, 168, 0.3)',
        }}>
            <RobotOutlined style={{ color: 'white', fontSize: '32px' }} />
        </div>
        <div style={{ fontSize: '18px', marginBottom: '8px', color: '#333' }}>{title}</div>
        <div style={{ fontSize: '14px', color: '#666' }}>{subtitle}</div>
    </div>
);

// ==================== 完整的右侧聊天面板 ====================
export interface ChatRightPanelProps {
    messages: ChatMessage[];
    messageThinkingLogs: ThinkingLogState;
    isThinking: boolean;
    currentAnsweringMsgId: string | null;
    input: string;
    onInputChange: (value: string) => void;
    onSend: () => void;
    onKeyPress?: (e: React.KeyboardEvent) => void;
    onToggleThinkingLog: (messageId: string) => void;
    agentName?: string;
    conversationId?: number | null;
    error?: string | null;
    emptyTitle?: string;
    emptySubtitle?: string;
}

export const ChatRightPanel: React.FC<ChatRightPanelProps> = ({
    messages,
    messageThinkingLogs,
    isThinking,
    currentAnsweringMsgId,
    input,
    onInputChange,
    onSend,
    onKeyPress,
    onToggleThinkingLog,
    agentName = 'EasyAgent',
    conversationId,
    error,
    emptyTitle = '开始新的对话',
    emptySubtitle = '在下方输入您的消息，智能助手将为您解答',
}) => {
    const chatMessagesEndRef = useRef<HTMLDivElement>(null);
    const thinkingContentRef = useRef<HTMLDivElement>(null);
    const prevThinkingContentLengthRef = useRef<number>(0);

    // 自动滚动到底部
    useEffect(() => {
        chatMessagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages, isThinking]);

    // 思考内容自动滚动到底部
    useEffect(() => {
        if (thinkingContentRef.current && currentAnsweringMsgId) {
            const thinkingContent = messageThinkingLogs[currentAnsweringMsgId]?.content || [];
            const currentLength = thinkingContent.length;

            if (currentLength > prevThinkingContentLengthRef.current) {
                setTimeout(() => {
                    thinkingContentRef.current?.scrollTo({
                        top: thinkingContentRef.current.scrollHeight,
                        behavior: 'smooth',
                    });
                }, 0);
            }
            prevThinkingContentLengthRef.current = currentLength;
        } else {
            prevThinkingContentLengthRef.current = 0;
        }
    }, [messageThinkingLogs, currentAnsweringMsgId]);

    return (
        <div style={{
            flex: 1,
            display: 'flex',
            flexDirection: 'column',
            background: '#f5f5f5',
            position: 'relative',
        }}>
            <div style={{
                flex: 1,
                overflowY: 'auto',
                padding: '32px',
                background: '#f5f5f5',
                position: 'relative',
            }}>
                {conversationId && (
                    <div style={{
                        position: 'absolute',
                        top: '12px',
                        right: '12px',
                        fontSize: '11px',
                        color: '#999',
                        zIndex: 10,
                    }}>
                        会话ID: {conversationId}
                    </div>
                )}

                {messages.length === 0 ? (
                    <ChatEmptyState agentName={agentName} title={emptyTitle} subtitle={emptySubtitle} />
                ) : (
                    <>
                        {messages.map((msg) => (
                            <ChatMessageItem
                                key={msg.id}
                                msg={msg}
                                messageThinkingLogs={messageThinkingLogs}
                                onToggleThinkingLog={onToggleThinkingLog}
                                renderThinkingContentFn={renderThinkingContent}
                                currentAnsweringMsgId={currentAnsweringMsgId}
                            />
                        ))}
                        <ThinkingIndicator
                            messageThinkingLogs={messageThinkingLogs}
                            currentAnsweringMsgId={currentAnsweringMsgId}
                            onToggleThinkingLog={onToggleThinkingLog}
                            renderThinkingContentFn={renderThinkingContent}
                            thinkingContentRef={thinkingContentRef}
                        />
                        <div ref={chatMessagesEndRef} />
                    </>
                )}

                {error && (
                    <div style={{
                        background: 'var(--ea-theme-background)',
                        color: '#ff4d4f',
                        padding: '12px 16px',
                        fontSize: '14px',
                        borderRadius: '8px',
                        border: '1px solid #ffccc7',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '8px',
                        marginTop: '16px',
                    }}>
                        <span style={{ fontSize: '16px' }}>⚠️</span>
                        {error}
                    </div>
                )}
            </div>

            <ChatInputArea
                value={input}
                onChange={onInputChange}
                onSend={onSend}
                onKeyPress={onKeyPress}
                disabled={isThinking}
            />
        </div>
    );
};
