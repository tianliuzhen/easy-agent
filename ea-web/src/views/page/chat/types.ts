/**
 * 聊天消息类型定义
 * 使用 XMarkdown + Think 组件模式重构后的新类型
 */

/**
 * 思考步骤
 */
export interface ThinkingStep {
  step: number;
  title: string;
  content: string;
  timestamp: number;
}

/**
 * 思考过程
 */
export interface ThinkingProcess {
  content: string;
  status: 'streaming' | 'done' | 'error';
  steps?: ThinkingStep[];
}

/**
 * 工具调用
 */
export interface ToolCall {
  id: string;
  name: string;
  params: Record<string, any>;
  result?: string;
  status: 'pending' | 'running' | 'success' | 'error';
  duration?: number;
  error?: string;
}

/**
 * 日志条目
 */
export interface LogEntry {
  id: string;
  content: string;
  timestamp: number;
  level: 'info' | 'warn' | 'error';
}

/**
 * 聊天消息
 * 新的统一消息格式，包含思考过程、工具调用和日志
 */
export interface ChatMessage {
  id: string;
  text: string;           // 最终答案 (data)
  isUser: boolean;
  type: 'data' | 'log';
  timestamp: number;

  // 思考过程 (think/thinking)
  thinking?: ThinkingProcess;

  // 工具调用 (tool)
  tools?: ToolCall[];

  // 日志 (log)
  logs?: LogEntry[];

  // 流式状态
  streamStatus?: 'streaming' | 'done' | 'error';
}

/**
 * 思考日志条目（兼容旧格式）
 */
export interface ThinkingLogEntry {
  type: 'log' | 'think' | 'data' | 'error' | 'tool' | 'finalAnswer';
  content: string;
  timestamp?: number;
}

/**
 * 类型配置（用于样式渲染）
 */
export interface TypeConfig {
  title: string;
  color: string;
  background: string;
  hasContainer: boolean;
  border?: string;
  contentStyle?: React.CSSProperties;
  style?: React.CSSProperties;
}

/**
 * 类型配置映射
 */
export const typeConfig: Record<string, TypeConfig> = {
  think: {
    title: '🤔 思考',
    color: '#94a3b8',
    background: '#f1f5f9',
    hasContainer: true,
    border: '#e2e8f0',
    contentStyle: { color: '#475569' }
  },
  data: {
    title: '💬 回答',
    color: '#1976d2',
    background: '#ffffff',
    hasContainer: true,
    contentStyle: { color: '#2c3e50' }
  },
  tool: {
    title: '🔧 工具',
    color: '#95a5a6',
    background: '#f5f5f5',
    hasContainer: true,
    contentStyle: { color: '#5d6d7e' }
  },
  log: {
    title: '',
    color: '#b0bec5',
    background: 'transparent',
    hasContainer: false,
    style: { color: '#78909c', fontStyle: 'italic', fontSize: '0.9em' }
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
    contentStyle: { color: '#1b5e20', fontWeight: 500 }
  }
};
