import React, {useState, useRef, useEffect} from 'react';
import {Button, Input, Spin, Tooltip} from 'antd';
import {
    SendOutlined,
    RobotOutlined,
    UserOutlined,
    EyeOutlined,
    EyeInvisibleOutlined,
    DownOutlined,
    RightOutlined
} from '@ant-design/icons';

// 类型定义
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

export const CURRENT_USER_ID = '1';

// ==================== 折叠组件 ====================
export const CollapsibleGroup: React.FC<{
    type: ThinkingLogEntry['type'];
    title: string;
    color: string;
    background: string;
    border: string;
    contentStyle: React.CSSProperties;
    content: string;
    defaultCollapsed: boolean;
}> = ({
          type,
          title,
          color,
          background,
          border,
          contentStyle,
          content,
          defaultCollapsed
      }) => {
    const [isCollapsed, setIsCollapsed] = useState(defaultCollapsed);

    return (
        <div
            style={{
                background: background,
                border: `1px solid ${border}`,
                borderRadius: '8px',
                padding: '12px',
                marginTop: '12px',
                color: '#000000',
                whiteSpace: 'pre-wrap',
                boxShadow: '0 2px 4px rgba(0, 0, 0, 0.05)'
            }}
        >
            <div
                style={{
                    fontSize: '11px',
                    color: color,
                    fontWeight: 600,
                    marginBottom: isCollapsed ? '0' : '8px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    cursor: 'pointer'
                }}
                onClick={() => setIsCollapsed(!isCollapsed)}
            >
                <div style={{display: 'flex', alignItems: 'center', gap: '4px'}}>
                    {isCollapsed ? <RightOutlined/> : <DownOutlined/>}
                    <span>{title}</span>
                </div>
            </div>
            {!isCollapsed && (
                <div style={{
                    fontSize: '12px',
                    lineHeight: 1.6,
                    whiteSpace: 'pre-wrap',
                    ...contentStyle
                }}>
                    {content}
                </div>
            )}
        </div>
    );
};

// ==================== 类型配置 ====================
const typeConfig = {
    think: {
        title: '🤔 思考',
        color: '#94a3b8',
        background: '#f1f5f9',
        hasContainer: true,
        border: '#e2e8f0',
        contentStyle: {color: '#475569'}
    },
    data: {
        title: '💬 回答',
        color: '#1976d2',
        background: '#ffffff',
        hasContainer: true,
        contentStyle: {color: '#2c3e50'}
    },
    tool: {
        title: '🔧 工具',
        color: '#95a5a6',
        background: '#f5f5f5',
        hasContainer: true,
        contentStyle: {color: '#5d6d7e'}
    },
    log: {
        title: '',
        color: '#b0bec5',
        background: 'transparent',
        hasContainer: false,
        style: {color: '#78909c', fontStyle: 'italic', fontSize: '0.9em'}
    },
    error: {
        title: '',
        color: '#e53935',
        background: 'transparent',
        hasContainer: false,
        style: {
            color: '#c62828',
            fontWeight: 500,
            background: '#ffebee',
            padding: '2px 4px',
            borderRadius: '3px'
        }
    },
    finalAnswer: {
        title: '✅ 最终答案',
        color: '#2e7d32',
        background: '#f1f8e9',
        hasContainer: true,
        contentStyle: {color: '#1b5e20', fontWeight: 500}
    }
};

// ==================== 渲染思考内容 ====================
export const renderThinkingContent = (entries: ThinkingLogEntry[], isRealTime: boolean = false) => {
    if (!entries || entries.length === 0) return null;

    const groups: Array<{ type: ThinkingLogEntry['type'], entries: ThinkingLogEntry[] }> = [];
    let currentGroup: { type: ThinkingLogEntry['type'], entries: ThinkingLogEntry[] } | null = null;

    entries.forEach(entry => {
        if (!currentGroup || currentGroup.type !== entry.type) {
            if (currentGroup) {
                groups.push(currentGroup);
            }
            currentGroup = {
                type: entry.type,
                entries: [entry]
            };
        } else {
            currentGroup.entries.push(entry);
        }
    });

    if (currentGroup) {
        groups.push(currentGroup);
    }

    return (
        <>
            {groups.map((group, groupIndex) => {
                const config = typeConfig[group.type];
                const content = group.entries.map(entry => entry.content).join(
                    group.type === 'tool' ? '\n' : ''
                );
                const groupKey = `${group.type}-${groupIndex}`;
                const defaultCollapsed = isRealTime ? false : (group.type === 'think' || group.type === 'tool');

                if (!config.hasContainer) {
                    return (
                        <React.Fragment key={groupKey}>
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

                return (
                    <CollapsibleGroup
                        key={groupKey}
                        type={group.type}
                        title={config.title}
                        color={config.color}
                        background={config.background}
                        border={config.border}
                        contentStyle={config.contentStyle || {}}
                        content={content}
                        defaultCollapsed={defaultCollapsed}
                    />
                );
            })}
        </>
    );
};

// ==================== 格式化时间 ====================
export const formatTime = (timestamp: number | string) => {
    const date = typeof timestamp === 'string' ? new Date(timestamp) : new Date(timestamp);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const seconds = String(date.getSeconds()).padStart(2, '0');
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
};

// ==================== 思考过程切换按钮 ====================
export const ThinkingLogToggleButton: React.FC<{
    messageId: string;
    isVisible: boolean;
    onToggle: (messageId: string) => void;
    isThinking?: boolean;
    showText?: boolean;
    position?: 'bubble' | 'panel';
}> = ({messageId, isVisible, onToggle, isThinking, showText = false, position = 'bubble'}) => {
    const isPanel = position === 'panel';

    if (isPanel) {
        return (
            <Tooltip
                title={isVisible ? (isThinking ? "隐藏实时思考" : "隐藏思考过程") : (isThinking ? "显示实时思考" : "显示思考过程")}>
                <Button
                    type="text"
                    size="small"
                    icon={isVisible ? <EyeInvisibleOutlined/> : <EyeOutlined/>}
                    onClick={() => onToggle(messageId)}
                    style={{
                        background: 'var(--ea-theme-background)',
                        border: '1px solid #91caff',
                        borderRadius: '16px',
                        padding: isThinking ? '4px 12px' : '4px 8px',
                        height: isThinking ? '28px' : '24px',
                        fontSize: '12px',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '6px',
                        boxShadow: '0 2px 6px rgba(0, 0, 0, 0.08)',
                        color: '#1890ff',
                        fontWeight: 500
                    }}
                >
                    {isThinking ? (isVisible ? '隐藏思考' : '实时思考') : ''}
                </Button>
            </Tooltip>
        );
    }

    return (
        <Tooltip title={isVisible ? "隐藏思考过程" : "显示思考过程"}>
            <Button
                type="text"
                size="small"
                icon={isVisible ? <EyeInvisibleOutlined/> : <EyeOutlined/>}
                onClick={(e) => {
                    e.stopPropagation();
                    onToggle(messageId);
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
            />
        </Tooltip>
    );
};

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
                                                                    currentAnsweringMsgId
                                                                }) => {
    const msgThinkingLog = messageThinkingLogs[msg.id];
    const hasThinkingLog = msgThinkingLog && msgThinkingLog.content.length > 0;
    const isThinkingVisible = msgThinkingLog?.isVisible || false;

    // 如果是机器人的空消息且正在思考中，不显示（由 ThinkingIndicator 统一显示）
    if (!msg.isUser && msg.text.trim() === '' && currentAnsweringMsgId === msg.id) {
        return null;
    }

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
                            backgroundColor: 'var(--ea-theme-background)',
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
                    >
                        {/* 气泡上的思考过程切换按钮 */}
                        {!msg.isUser && hasThinkingLog && (
                            <ThinkingLogToggleButton
                                messageId={msg.id}
                                isVisible={isThinkingVisible}
                                onToggle={onToggleThinkingLog}
                                position="bubble"
                            />
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

            {/* 思考过程内容 - 只在消息已完成时显示 */}
            {!msg.isUser && hasThinkingLog && isThinkingVisible && currentAnsweringMsgId !== msg.id && (
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
                            <div style={{display: 'flex', alignItems: 'center'}}>
                                {currentAnsweringMsgId === msg.id ? (
                                    <>
                                        <span>💭 </span>
                                        <span style={{
                                            fontSize: '11px',
                                            backgroundColor: 'var(--ea-theme-background)',
                                            padding: '2px 4px',
                                            borderRadius: '10px'
                                        }}>
                                            分析中
                                        </span>
                                    </>
                                ) : (
                                    <>
                                        <span>✅ </span>
                                        <span style={{
                                            fontSize: '11px',
                                            backgroundColor: 'var(--ea-theme-background)',
                                            padding: '2px 4px',
                                            borderRadius: '10px'
                                        }}>
                                            已完成
                                        </span>
                                    </>
                                )}
                            </div>
                            <Tooltip title="隐藏">
                                <Button
                                    type="text"
                                    size="small"
                                    icon={<EyeInvisibleOutlined/>}
                                    onClick={() => onToggleThinkingLog(msg.id)}
                                    style={{fontSize: '12px', padding: '0 4px', height: '20px'}}
                                />
                            </Tooltip>
                        </div>
                        <div style={{
                            fontSize: '12px',
                            lineHeight: 1.6,
                            color: '#333',
                            whiteSpace: 'pre-wrap',
                            maxHeight: '500px',
                            overflowY: 'auto'
                        }}>
                            {renderThinkingContentFn(msgThinkingLog.content, currentAnsweringMsgId === msg.id)}
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
                                                                        thinkingContentRef
                                                                    }) => {
    if (!currentAnsweringMsgId || !messageThinkingLogs[currentAnsweringMsgId]) {
        return null;
    }

    const isVisible = messageThinkingLogs[currentAnsweringMsgId]?.isVisible || false;
    const content = messageThinkingLogs[currentAnsweringMsgId]?.content || [];

    return (
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
                    {/* 思考过程按钮 */}
                    <div style={{
                        display: 'flex',
                        justifyContent: 'flex-start',
                        marginBottom: '8px'
                    }}>
                        <ThinkingLogToggleButton
                            messageId={currentAnsweringMsgId}
                            isVisible={isVisible}
                            onToggle={onToggleThinkingLog}
                            isThinking={true}
                            position="panel"
                        />
                    </div>
                </div>
            </div>

            {/* 实时思考过程内容 */}
            {isVisible && (
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
                                <span>💭 分析中</span>
                                <Spin size="small"/>
                            </div>
                            <Tooltip title="隐藏">
                                <Button
                                    type="text"
                                    size="small"
                                    icon={<EyeInvisibleOutlined/>}
                                    onClick={() => onToggleThinkingLog(currentAnsweringMsgId)}
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
                                maxHeight: '400px',
                                overflowY: 'auto'
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
                                                                placeholder = "输入您的问题...（按 Enter 发送，Shift + Enter 换行）"
                                                            }) => {
    const {TextArea} = Input;

    return (
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
                    value={value}
                    onChange={(e) => onChange(e.target.value)}
                    onKeyPress={onKeyPress}
                    placeholder={placeholder}
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
                    disabled={disabled}
                />
                <Button
                    type="primary"
                    onClick={onSend}
                    disabled={disabled || value.trim() === ''}
                    style={{
                        height: '60px',
                        width: '60px',
                        borderRadius: '50%',
                        padding: 0,
                        background: 'linear-gradient(135deg, #5c74a8 0%, #a9b9d6 100%)',
                        border: 'none',
                        boxShadow: '0 4px 12px rgba(92, 116, 168, 0.4)',
                        transition: 'all 0.3s ease',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center'
                    }}
                    icon={<SendOutlined style={{fontSize: '20px'}}/>}
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
                                                                  subtitle = '在下方输入您的消息，智能助手将为您解答'
                                                              }) => {
    return (
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
                {title}
            </div>
            <div style={{fontSize: '14px', color: '#666'}}>
                {subtitle}
            </div>
        </div>
    );
};

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
    modelVersion?: string | null;
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
                                                                  modelVersion,
                                                                  conversationId,
                                                                  error,
                                                                  emptyTitle = '开始新的对话',
                                                                  emptySubtitle = '在下方输入您的消息，智能助手将为您解答'
                                                              }) => {
    const chatMessagesEndRef = useRef<HTMLDivElement>(null);
    const thinkingContentRef = useRef<HTMLDivElement>(null);
    const prevThinkingContentLengthRef = useRef<number>(0);

    // 自动滚动到底部
    useEffect(() => {
        chatMessagesEndRef.current?.scrollIntoView({behavior: 'smooth'});
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
                        behavior: 'smooth'
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
                {conversationId && (
                    <div style={{
                        position: 'absolute',
                        top: '12px',
                        right: '12px',
                        fontSize: '11px',
                        color: '#999',
                        zIndex: 10
                    }}>
                        会话ID: {conversationId}
                    </div>
                )}

                {messages.length === 0 ? (
                    <ChatEmptyState
                        agentName={agentName}
                        title={emptyTitle}
                        subtitle={emptySubtitle}
                    />
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

                        {/* 实时思考指示器 */}
                        <ThinkingIndicator
                            messageThinkingLogs={messageThinkingLogs}
                            currentAnsweringMsgId={currentAnsweringMsgId}
                            onToggleThinkingLog={onToggleThinkingLog}
                            renderThinkingContentFn={renderThinkingContent}
                            thinkingContentRef={thinkingContentRef}
                        />

                        <div ref={chatMessagesEndRef}/>
                    </>
                )}

                {/* 错误提示 */}
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
                        marginTop: '16px'
                    }}>
                        <span style={{fontSize: '16px'}}>⚠️</span>
                        {error}
                    </div>
                )}
            </div>

            {/* 输入区域 */}
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
