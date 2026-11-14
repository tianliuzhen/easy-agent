import React from 'react';
import ChatMessageItem from './ChatMessageItem';
import type {ChatMessage, ThinkingLogState} from '../ChatComponents';
import './chatMessage.css';

export interface ChatMessageListProps {
    messages: ChatMessage[];
    /** 每条助手消息对应的执行过程事件 */
    thinkingLogs: ThinkingLogState;
    /** 正在生成的消息 id；为 null 表示当前没有进行中的回答 */
    currentAnsweringMsgId: string | null;
    /** Agent 头像 */
    avatar?: string;
}

/**
 * 消息列表：居中定宽的单列，统一消息之间的垂直节奏。
 * 滚动与跟随由外层滚动容器负责，这里只做展示。
 */
const ChatMessageList: React.FC<ChatMessageListProps> = ({
    messages,
    thinkingLogs,
    currentAnsweringMsgId,
    avatar,
}) => (
    <div className="ea-chat-list">
        {messages.map((message) => (
            <ChatMessageItem
                key={message.id}
                message={message}
                traceEntries={thinkingLogs[message.id]?.content}
                streaming={currentAnsweringMsgId === message.id}
                avatar={avatar}
            />
        ))}
    </div>
);

export default ChatMessageList;
