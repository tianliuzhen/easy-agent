// Chat 组件统一导出

// 消息展示层（重写后的实现）
export {
    ChatMessageList,
    ChatMessageItem,
    MarkdownContent,
    TracePanel,
    ToolCallList,
    buildMessageTrace,
    parseToolTrace,
    formatJson,
} from './message';

export type {
    ChatMessageListProps,
    ChatMessageItemProps,
    MessageTrace,
    ToolTrace,
} from './message';

// 聊天面板与输入区
export {
    ChatRightPanel,
    ChatInputArea,
    ChatEmptyState,
} from './ChatComponents';

export type {
    ChatRightPanelProps,
    ChatInputAreaProps,
    ChatEmptyStateProps,
    ChatMessage,
    ThinkingLogEntry,
    ThinkingLogState,
    QuickPromptGroup,
} from './ChatComponents';

// 会话侧边栏
export {ConversationSidebar} from './ConversationSidebar';

// 历史记录转换
export {
    historyToChatMessages,
    tracesToEntries,
    resolveAnswerText,
} from './chatContextUtils';
