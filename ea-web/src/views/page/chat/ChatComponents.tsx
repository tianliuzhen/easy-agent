import React, {useRef, useEffect, useState} from 'react';
import {Button, Input, Tooltip} from 'antd';
import {
    SendOutlined,
    RobotOutlined,
    PictureOutlined,
    CloseOutlined,
    BulbOutlined,
    DownOutlined,
    EnterOutlined,
} from '@ant-design/icons';
import ChatMessageList from './message/ChatMessageList';

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
    /** 图片数据（Base64 Data URL），仅用户消息可能有 */
    imageBase64?: string;
}

/** 浮选提示词分组：一个按钮 + 该按钮下的推荐问题 */
export interface QuickPromptGroup {
    id?: number;
    label: string;
    questions: string[];
}

// ==================== 输入区域 ====================
export interface ChatInputAreaProps {
    value: string;
    onChange: (value: string) => void;
    onSend: () => void;
    onKeyPress?: (e: React.KeyboardEvent) => void;
    disabled?: boolean;
    placeholder?: string;
    /** 选中的图片（Base64 Data URL） */
    selectedImage?: string;
    /** 图片变化回调 */
    onImageChange?: (imageBase64: string | undefined) => void;
    /** 浮选提示词分组 */
    quickPrompts?: QuickPromptGroup[];
    /** 点击推荐问题回调（填充到输入框） */
    onQuickQuestion?: (question: string) => void;
}

export const ChatInputArea: React.FC<ChatInputAreaProps> = ({
                                                                value,
                                                                onChange,
                                                                onSend,
                                                                onKeyPress,
                                                                disabled = false,
                                                                placeholder = "输入您的问题…",
                                                                selectedImage,
                                                                onImageChange,
                                                                quickPrompts,
                                                                onQuickQuestion,
                                                            }) => {
    const {TextArea} = Input;
    const fileInputRef = useRef<HTMLInputElement>(null);
    const [previewUrl, setPreviewUrl] = useState<string | undefined>(selectedImage);
    const [focused, setFocused] = useState(false);
    const [activeGroup, setActiveGroup] = useState<number | null>(null);

    // 同步外部 selectedImage 变化
    useEffect(() => {
        setPreviewUrl(selectedImage);
    }, [selectedImage]);

    // 处理文件选择
    const handleFileSelect = (file: File) => {
        console.log('handleFileSelect 被调用，文件类型:', file.type, '文件大小:', file.size);
        if (!file.type.startsWith('image/')) {
            console.warn('不是图片文件');
            return;
        }
        const reader = new FileReader();
        reader.onload = (e) => {
            const base64 = e.target?.result as string;
            console.log('FileReader 读取完成，base64 长度:', base64.length);
            setPreviewUrl(base64);
            onImageChange?.(base64);
            console.log('onImageChange 已调用');
        };
        reader.onerror = (e) => {
            console.error('FileReader 读取失败:', e);
        };
        reader.readAsDataURL(file);
    };

    // 处理粘贴事件（支持粘贴截图）
    const handlePaste = (e: React.ClipboardEvent<HTMLTextAreaElement>) => {
        const items = e.clipboardData?.items;
        if (!items) return;

        for (let i = 0; i < items.length; i++) {
            if (items[i].type.startsWith('image/')) {
                e.preventDefault();
                const file = items[i].getAsFile();
                if (file) {
                    handleFileSelect(file);
                }
                break;
            }
        }
    };

    // 移除图片
    const handleRemoveImage = () => {
        setPreviewUrl(undefined);
        onImageChange?.(undefined);
    };

    const canSend = !disabled && (value.trim() !== '' || !!previewUrl);

    return (
        <div style={{
            background: 'transparent',
            padding: '12px 24px 16px',
            // 固定在底部：不参与 flex 收缩，消息再长也不会把输入区挤出视口
            flexShrink: 0,
            // 提示词展开很多行时自身限高滚动，不去挤压消息区
            maxHeight: '45vh',
            overflowY: 'auto',
        }}>
            {/* 隐藏的文件输入 */}
            <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
                style={{display: 'none'}}
                onChange={(e) => {
                    const file = e.target.files?.[0];
                    if (file) {
                        handleFileSelect(file);
                    }
                    // 清空 input 值以支持重复选择同一文件
                    if (e.target) e.target.value = '';
                }}
            />

            {/* 浮选提示词 */}
            {quickPrompts && quickPrompts.length > 0 && (
                <div style={{maxWidth: 'var(--ea-chat-max-width)', margin: '0 auto 10px'}}>
                    <style>{`
                        .ea-quick-cat {
                            display: inline-flex;
                            align-items: center;
                            padding: 5px 12px;
                            border-radius: 16px;
                            border: 1px solid #e1e7f5;
                            background: #fff;
                            color: #5b6478;
                            font-size: 13px;
                            font-weight: 500;
                            line-height: 1.5;
                            cursor: pointer;
                            transition: all 0.18s ease;
                        }
                        .ea-quick-cat:hover {
                            border-color: #5c74a8;
                            color: #3f568c;
                            box-shadow: 0 3px 10px rgba(92, 116, 168, 0.16);
                            transform: translateY(-1px);
                        }
                        .ea-quick-cat--active {
                            border-color: transparent;
                            color: #fff;
                            background: linear-gradient(135deg, #5c74a8 0%, #7d8fc0 100%);
                            box-shadow: 0 4px 12px rgba(92, 116, 168, 0.32);
                        }
                        .ea-quick-cat--active:hover {
                            color: #fff;
                            transform: translateY(-1px);
                        }
                        .ea-quick-cat__caret {
                            opacity: 0.55;
                            transition: transform 0.18s ease;
                        }
                        .ea-quick-question {
                            display: inline-flex;
                            align-items: center;
                            gap: 7px;
                            max-width: 100%;
                            padding: 6px 14px;
                            border-radius: 16px;
                            border: 1px solid #d4ddf2;
                            background: #f4f7fd;
                            color: #4b5563;
                            font-size: 12px;
                            line-height: 1.5;
                            cursor: pointer;
                            text-align: left;
                            transition: all 0.18s ease;
                        }
                        .ea-quick-question:hover {
                            border-color: #5c74a8;
                            background: #eaf0fb;
                            color: #3f568c;
                            box-shadow: 0 3px 10px rgba(92, 116, 168, 0.18);
                            transform: translateY(-1px);
                        }
                        .ea-quick-question__text {
                            overflow: hidden;
                            text-overflow: ellipsis;
                            white-space: nowrap;
                        }
                        .ea-quick-question__icon {
                            font-size: 11px;
                            color: #9aa6c2;
                            flex-shrink: 0;
                            transition: color 0.18s ease;
                        }
                        .ea-quick-question:hover .ea-quick-question__icon {
                            color: #5c74a8;
                        }
                    `}</style>
                    {/* 分类按钮行 */}
                    <div style={{display: 'flex', flexWrap: 'wrap', gap: '8px'}}>
                        {quickPrompts.map((group, gi) => (
                            <button
                                key={group.id ?? gi}
                                type="button"
                                className={`ea-quick-cat${activeGroup === gi ? ' ea-quick-cat--active' : ''}`}
                                onClick={() => setActiveGroup(activeGroup === gi ? null : gi)}
                            >
                                {group.label}
                                <DownOutlined
                                    className="ea-quick-cat__caret"
                                    style={{
                                        fontSize: '9px',
                                        marginLeft: '5px',
                                        transform: activeGroup === gi ? 'rotate(180deg)' : 'none',
                                    }}
                                />
                            </button>
                        ))}
                    </div>

                    {/* 选中分类下的推荐问题 */}
                    {activeGroup !== null && quickPrompts[activeGroup] && (
                        <div style={{
                            marginTop: '8px',
                            padding: '12px 14px',
                            background: '#fff',
                            borderRadius: '14px',
                            border: '1px solid #eef0f5',
                            boxShadow: '0 2px 10px rgba(17, 24, 39, 0.05)',
                        }}>
                            <div style={{
                                display: 'flex',
                                alignItems: 'center',
                                gap: '6px',
                                marginBottom: '10px',
                                fontSize: '12px',
                                color: '#8a93a6',
                                fontWeight: 500,
                            }}>
                                <BulbOutlined style={{color: '#5c74a8'}}/>
                                <span>试试这样问 · {quickPrompts[activeGroup].label}</span>
                            </div>

                            {quickPrompts[activeGroup].questions.length === 0 ? (
                                <span style={{fontSize: '12px', color: '#9aa1ab'}}>暂无推荐问题</span>
                            ) : (
                                <div style={{display: 'flex', flexWrap: 'wrap', gap: '8px'}}>
                                    {quickPrompts[activeGroup].questions.map((q, qi) => (
                                        <button
                                            key={qi}
                                            type="button"
                                            className="ea-quick-question"
                                            onClick={() => onQuickQuestion?.(q)}
                                            title={`点击填入：${q}`}
                                        >
                                            <span className="ea-quick-question__text">{q}</span>
                                            <EnterOutlined className="ea-quick-question__icon"/>
                                        </button>
                                    ))}
                                </div>
                            )}
                        </div>
                    )}
                </div>
            )}

            {/* 统一输入容器 */}
            <div style={{
                maxWidth: 'var(--ea-chat-max-width)',
                margin: '0 auto',
                background: '#fff',
                borderRadius: '26px',
                border: `1.5px solid ${focused ? '#5c74a8' : '#e6e9f0'}`,
                boxShadow: focused
                    ? '0 8px 28px rgba(92, 116, 168, 0.18)'
                    : '0 4px 18px rgba(17, 24, 39, 0.06)',
                transition: 'border-color 0.2s ease, box-shadow 0.2s ease',
                padding: '8px 8px 8px 12px',
            }}>
                {/* 图片预览区域 */}
                {previewUrl && (
                    <div style={{
                        margin: '4px 0 10px 4px',
                        display: 'inline-block',
                        position: 'relative',
                    }}>
                        <img
                            src={previewUrl}
                            alt="预览图片"
                            style={{
                                maxHeight: '96px',
                                maxWidth: '180px',
                                borderRadius: '12px',
                                border: '1px solid #eef0f5',
                                objectFit: 'cover',
                                display: 'block',
                            }}
                        />
                        <Button
                            type="text"
                            size="small"
                            icon={<CloseOutlined style={{fontSize: '11px', color: '#fff'}}/>}
                            onClick={handleRemoveImage}
                            style={{
                                position: 'absolute',
                                top: '-8px',
                                right: '-8px',
                                background: 'rgba(0, 0, 0, 0.55)',
                                borderRadius: '50%',
                                width: '22px',
                                height: '22px',
                                minWidth: '22px',
                                padding: 0,
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                boxShadow: '0 2px 6px rgba(0, 0, 0, 0.2)',
                            }}
                        />
                    </div>
                )}

                <div style={{display: 'flex', alignItems: 'flex-end', gap: '6px', width: '100%'}}>
                    {/* 图片上传按钮 */}
                    <Tooltip title="上传图片">
                        <Button
                            type="text"
                            shape="circle"
                            icon={<PictureOutlined style={{fontSize: '19px', color: '#8a93a6'}}/>}
                            onClick={() => fileInputRef.current?.click()}
                            disabled={disabled}
                            style={{
                                width: '40px',
                                height: '40px',
                                flexShrink: 0,
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                            }}
                        />
                    </Tooltip>

                    <TextArea
                        value={value}
                        onChange={(e) => onChange(e.target.value)}
                        onKeyPress={onKeyPress}
                        onPaste={handlePaste}
                        onFocus={() => setFocused(true)}
                        onBlur={() => setFocused(false)}
                        placeholder={placeholder}
                        disabled={disabled}
                        variant="borderless"
                        autoSize={{minRows: 1, maxRows: 6}}
                        style={{
                            flex: 1,
                            fontSize: '15px',
                            lineHeight: 1.6,
                            padding: '8px 4px',
                            resize: 'none',
                            background: 'transparent',
                            boxShadow: 'none',
                        }}
                    />

                    {/* 发送按钮 */}
                    <Tooltip title="发送">
                        <Button
                            type="primary"
                            onClick={onSend}
                            disabled={!canSend}
                            icon={<SendOutlined style={{fontSize: '18px'}}/>}
                            style={{
                                height: '40px',
                                width: '40px',
                                minWidth: '40px',
                                flexShrink: 0,
                                borderRadius: '14px',
                                padding: 0,
                                background: canSend
                                    ? 'linear-gradient(135deg, #5c74a8 0%, #7d8fc0 100%)'
                                    : '#e6e9f0',
                                border: 'none',
                                boxShadow: canSend ? '0 4px 12px rgba(92, 116, 168, 0.35)' : 'none',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                transition: 'background 0.2s ease, box-shadow 0.2s ease',
                            }}
                        />
                    </Tooltip>
                </div>
            </div>

            {/* 提示文字 */}
            <div style={{
                maxWidth: 'var(--ea-chat-max-width)',
                margin: '8px auto 0',
                textAlign: 'center',
                fontSize: '12px',
                color: '#aab1c0',
            }}>
                按 Enter 发送，Shift + Enter 换行
            </div>
        </div>
    );
};

// ==================== 空状态 ====================
export interface ChatEmptyStateProps {
    agentName?: string;
    title?: string;
    subtitle?: string;
    welcomeMessage?: string;
    avatar?: string;
}

export const ChatEmptyState: React.FC<ChatEmptyStateProps> = ({
                                                                  agentName = 'EasyAgent',
                                                                  title = '开始新的对话',
                                                                  subtitle = '在下方输入您的消息，智能助手将为您解答',
                                                                  welcomeMessage,
                                                                  avatar,
                                                              }) => {
    const isImageAvatar = !!avatar && /^(https?:\/\/|data:image\/|\/)/.test(avatar);
    const isEmojiAvatar = !!avatar && !isImageAvatar;
    return (
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
            background: isImageAvatar ? 'transparent' : 'linear-gradient(135deg, #5c74a8 0%, #a9b9d6 100%)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            marginBottom: '24px',
            boxShadow: '0 8px 24px rgba(92, 116, 168, 0.3)',
            overflow: 'hidden',
        }}>
            {isImageAvatar
                ? <img src={avatar} alt={agentName} style={{width: '100%', height: '100%', objectFit: 'cover'}}/>
                : isEmojiAvatar
                    ? <span style={{fontSize: '40px', lineHeight: 1}}>{avatar}</span>
                    : <RobotOutlined style={{color: 'white', fontSize: '32px'}}/>}
        </div>
        <div style={{fontSize: '18px', marginBottom: '8px', color: '#333'}}>{title}</div>
        <div style={{
            fontSize: '14px',
            color: '#666',
            maxWidth: '520px',
            lineHeight: 1.6,
            whiteSpace: 'pre-wrap',
            textAlign: 'center',
        }}>
            {welcomeMessage || subtitle}
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
    /**
     * @deprecated 执行过程的展开状态已由消息自身维护（生成中展开、结束收起、手动可切换），
     * 不再需要外部传入。保留该属性只为兼容现有调用方。
     */
    onToggleThinkingLog?: (messageId: string) => void;
    agentName?: string;
    /** 模型版本，展示在消息区顶部 */
    modelVersion?: string | null;
    conversationId?: number | null;
    error?: string | null;
    emptyTitle?: string;
    emptySubtitle?: string;
    /** 欢迎语：对话开始时展示给用户的开场白 */
    welcomeMessage?: string;
    /** Agent 头像 */
    avatar?: string;
    /** 选中的图片（Base64 Data URL） */
    selectedImage?: string;
    /** 图片变化回调 */
    onImageChange?: (imageBase64: string | undefined) => void;
    /** 浮选提示词分组 */
    quickPrompts?: QuickPromptGroup[];
    /** 点击推荐问题回调（填充到输入框） */
    onQuickQuestion?: (question: string) => void;
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
                                                                  agentName = 'EasyAgent',
                                                                  modelVersion,
                                                                  conversationId,
                                                                  error,
                                                                  emptyTitle = '开始新的对话',
                                                                  emptySubtitle = '在下方输入您的消息，智能助手将为您解答',
                                                                  welcomeMessage,
                                                                  avatar,
                                                                  selectedImage,
                                                                  onImageChange,
                                                                  quickPrompts,
                                                                  onQuickQuestion,
                                                              }) => {
    const scrollRef = useRef<HTMLDivElement>(null);
    // 用户手动上翻后暂停自动跟随，滚回底部附近再恢复
    const followBottomRef = useRef(true);
    // 自动滚动写入 scrollTop 会再次触发 onScroll，这里标记忽略那一次回调
    const autoScrollingRef = useRef(false);
    const scrollFrameRef = useRef<number | null>(null);

    const handleScroll: React.UIEventHandler<HTMLDivElement> = () => {
        if (autoScrollingRef.current) {
            autoScrollingRef.current = false;
            return;
        }
        const container = scrollRef.current;
        if (!container) return;
        followBottomRef.current =
            container.scrollHeight - container.scrollTop - container.clientHeight <= 60;
    };

    /**
     * 跟随到底部。
     * <p>
     * 流式输出每秒有几十次内容更新，这里用两条措施避免画面抖动：
     * 1. 直接写 scrollTop 而不是 scrollIntoView({behavior:'smooth'})，
     *    平滑滚动会在每个 chunk 到达时重启动画，前后动画互相打断就是肉眼看到的抖动；
     * 2. 用 requestAnimationFrame 合并同一帧内的多次更新，一帧只滚一次。
     */
    useEffect(() => {
        if (!followBottomRef.current) return;
        if (scrollFrameRef.current !== null) return;
        scrollFrameRef.current = requestAnimationFrame(() => {
            scrollFrameRef.current = null;
            const container = scrollRef.current;
            if (!container) return;
            autoScrollingRef.current = true;
            container.scrollTop = container.scrollHeight;
        });
    }, [messages, messageThinkingLogs, isThinking]);

    // 卸载时取消未执行的滚动帧
    useEffect(() => () => {
        if (scrollFrameRef.current !== null) cancelAnimationFrame(scrollFrameRef.current);
    }, []);

    return (
        <div style={{
            flex: 1,
            display: 'flex',
            flexDirection: 'column',
            background: '#f5f5f5',
            position: 'relative',
            minWidth: 0,
            // 关键：flex 子项默认 min-height:auto，不会收缩到小于内容高度。
            // 不写死这条，长回答会把本容器撑出父级高度，导致整页出现滚动条、
            // 输入框被顶到视口外（消息区则完全不滚动）。
            minHeight: 0,
            // 自身高度由父级 flex 决定，内部靠消息区滚动消化超出部分
            overflow: 'hidden',
        }}>
            {/* 会话元信息：模型版本与会话 ID */}
            {(modelVersion || conversationId) && (
                <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    gap: '12px',
                    padding: '10px 24px 0',
                    fontSize: '11.5px',
                    color: '#9aa1ab',
                    flexShrink: 0,
                }}>
                    <span>{modelVersion ? `模型版本：${modelVersion}` : ''}</span>
                    <span>{conversationId ? `会话ID: ${conversationId}` : ''}</span>
                </div>
            )}

            <div
                ref={scrollRef}
                onScroll={handleScroll}
                style={{
                    flex: 1,
                    overflowY: 'auto',
                    padding: '20px 24px 8px',
                    minHeight: 0,
                }}
            >
                {messages.length === 0 ? (
                    <ChatEmptyState agentName={agentName} title={emptyTitle} subtitle={emptySubtitle} welcomeMessage={welcomeMessage} avatar={avatar}/>
                ) : (
                    <ChatMessageList
                        messages={messages}
                        thinkingLogs={messageThinkingLogs}
                        currentAnsweringMsgId={currentAnsweringMsgId}
                        avatar={avatar}
                    />
                )}

                {error && (
                    <div style={{
                        maxWidth: 'var(--ea-chat-max-width)',
                        margin: '16px auto 0',
                        padding: '10px 14px',
                        background: '#fff2f0',
                        color: '#cf1322',
                        fontSize: '13px',
                        borderRadius: '8px',
                        border: '1px solid #ffccc7',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '8px',
                    }}>
                        <span>⚠️</span>
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
                selectedImage={selectedImage}
                onImageChange={onImageChange}
                quickPrompts={quickPrompts}
                onQuickQuestion={onQuickQuestion}
            />
        </div>
    );
};
