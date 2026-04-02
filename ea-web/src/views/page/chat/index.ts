// Chat 组件统一导出
export { default as ThinkComponent } from './ThinkComponent';
export { default as ToolComponent } from './ToolComponent';
export { default as LogComponent } from './LogComponent';
export { default as MessageBubble, MessageBubbleWithXMarkdown } from './MessageBubble';

// 类型导出
export type {
  ChatMessage,
  ThinkingProcess,
  ThinkingStep,
  ToolCall,
  LogEntry,
  ThinkingLogEntry,
  TypeConfig
} from './types';

// 工具函数导出
export {
  parseMessageContext,
  extractFinalAnswer,
  convertEntriesToMessage,
  createNewAIMessage,
  updateMessageThinking,
  updateMessageData,
  addToolToMessage,
  addLogToMessage,
  finalizeMessage,
  errorMessage
} from './messageUtils';

// ChatComponents 导出
export {
  ChatRightPanel,
  ChatMessageItem,
  ChatInputArea,
  ChatEmptyState,
  ThinkingIndicator,
  formatTime,
  typeConfig
} from './ChatComponents';

export type {
  ChatMessageItemProps,
  ChatInputAreaProps,
  ChatEmptyStateProps,
  ChatRightPanelProps,
  ThinkingIndicatorProps
} from './ChatComponents';
