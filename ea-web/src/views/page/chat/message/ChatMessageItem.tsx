import React, {useMemo} from 'react';
import {LoadingOutlined, RobotOutlined, UserOutlined, WarningOutlined} from '@ant-design/icons';
import MarkdownContent from './MarkdownContent';
import TracePanel from './TracePanel';
import {buildMessageTrace} from './traceModel';
import {useThinkingVerb} from './thinkingVerbs';
import type {ChatMessage, ThinkingLogEntry} from '../ChatComponents';
import './chatMessage.css';

export interface ChatMessageItemProps {
    message: ChatMessage;
    /** 该消息的执行过程事件 */
    traceEntries?: ThinkingLogEntry[];
    /** 该消息是否正在生成 */
    streaming: boolean;
    /** Agent 头像：图片 URL 或 emoji，缺省用内置图标 */
    avatar?: string;
}

/**
 * 单条聊天消息。
 * <p>
 * 用户消息为右侧气泡；助手消息不套气泡，由「执行过程面板 + 回答正文」两部分组成，
 * 回答只渲染一次（优先用最终答案，流式阶段用 data 片段）。
 */
const ChatMessageItem: React.FC<ChatMessageItemProps> = ({message, traceEntries, streaming, avatar}) => {
    const trace = useMemo(() => buildMessageTrace(traceEntries), [traceEntries]);

    if (message.isUser) {
        return <UserRow message={message}/>;
    }

    // 最终答案优先；生成中退回 data 流式片段
    const answer = (message.text || trace.streamedAnswer).replace(/^\n+|\n+$/g, '');
    const waiting = streaming && answer === '' && !trace.hasTrace;

    // 既没有回答也没有过程，且已结束：不占位（后端可能只发了日志）
    if (!waiting && answer === '' && !trace.hasTrace && trace.errors.length === 0) {
        return null;
    }

    return (
        <div className="ea-chat-row">
            <AssistantAvatar avatar={avatar}/>
            <div className="ea-chat-body">
                {trace.hasTrace && <TracePanel trace={trace} streaming={streaming}/>}

                {waiting && <PendingHint/>}

                {answer !== '' && (
                    <div className="ea-chat-answer">
                        <MarkdownContent content={answer} streaming={streaming}/>
                    </div>
                )}

                {/* 生成中提示：独立成行，不接在正文末尾，避免末行文字来回换行 */}
                {streaming && answer !== '' && (
                    <div className="ea-chat-streaming-hint">
                        <span className="ea-chat-streaming-dot"/>
                        <span>生成中</span>
                    </div>
                )}

                {trace.errors.map((error, index) => (
                    <div className="ea-chat-error" key={`error-${index}`}>
                        <WarningOutlined style={{marginTop: 3}}/>
                        <span className="ea-chat-plaintext">{error}</span>
                    </div>
                ))}
            </div>
        </div>
    );
};

/**
 * 等待首个事件时的占位提示。
 * 与过程面板用同一套轮换词，否则同一条消息里会出现「有时轮换、有时不换」的不一致。
 */
const PendingHint: React.FC = () => {
    const verb = useThinkingVerb(true);
    return (
        <div className="ea-chat-pending">
            <LoadingOutlined spin/>
            <span key={verb ?? 'pending'} className="ea-chat-trace-verb">{verb}…</span>
        </div>
    );
};

/** 用户消息行：头像在右，气泡靠右 */
const UserRow: React.FC<{message: ChatMessage}> = ({message}) => (
    <div className="ea-chat-row ea-chat-row-user">
        <div className="ea-chat-body ea-chat-body-user">
            {message.imageBase64 && (
                <img
                    className="ea-chat-image"
                    src={message.imageBase64}
                    alt="用户发送的图片"
                    onClick={() => window.open(message.imageBase64, '_blank', 'noopener,noreferrer')}
                />
            )}
            {message.text.trim() !== '' && (
                <div className="ea-chat-bubble-user ea-chat-plaintext">
                    {message.text.replace(/^\n+|\n+$/g, '')}
                </div>
            )}
        </div>
        <div className="ea-chat-avatar ea-chat-avatar-user">
            <UserOutlined/>
        </div>
    </div>
);

/** 助手头像：支持图片 URL 与 emoji 两种配置 */
const AssistantAvatar: React.FC<{avatar?: string}> = ({avatar}) => {
    const isImage = !!avatar && /^(https?:\/\/|data:image\/|\/)/.test(avatar);
    return (
        <div className="ea-chat-avatar">
            {isImage
                ? <img src={avatar} alt="助手头像"/>
                : avatar
                    ? <span style={{fontSize: 16, lineHeight: 1}}>{avatar}</span>
                    : <RobotOutlined/>}
        </div>
    );
};

/**
 * 流式输出期间整个列表每秒重渲染几十次；历史消息的 props 不变，
 * 用 memo 把它们挡在重渲染之外，只让正在生成的那条真正更新。
 */
export default React.memo(ChatMessageItem);
