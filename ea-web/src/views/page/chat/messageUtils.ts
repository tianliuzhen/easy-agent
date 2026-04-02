import type { ChatMessage, ThinkingLogEntry, ToolCall, LogEntry, ThinkingProcess } from './types';

/**
 * 解析 messageContext（兼容旧格式）
 * @param messageContext 后端返回的消息上下文 JSON 字符串
 * @returns 解析后的思考日志条目数组
 */
export const parseMessageContext = (messageContext: string): ThinkingLogEntry[] => {
  try {
    if (!messageContext || messageContext.trim() === '') {
      return [];
    }

    const contextArray = JSON.parse(messageContext);
    if (!Array.isArray(contextArray)) {
      return [];
    }

    const entries: ThinkingLogEntry[] = [];

    contextArray.forEach((item: any) => {
      const type = item.type;
      const value = item.value || '';
      const time = item.time;

      if (type === 'thinking') {
        entries.push({ type: 'think', content: value, timestamp: time });
      } else if (type === 'data') {
        entries.push({ type: 'data', content: value, timestamp: time });
      } else if (type === 'tool') {
        entries.push({ type: 'tool', content: value, timestamp: time });
      } else if (type === 'log') {
        entries.push({ type: 'log', content: value, timestamp: time });
      } else if (type === 'error') {
        entries.push({ type: 'error', content: value, timestamp: time });
      } else if (type === 'finalAnswer') {
        entries.push({ type: 'finalAnswer', content: value, timestamp: time });
      }
    });

    return entries;
  } catch (error) {
    console.error('解析 messageContext 失败:', error);
    return [];
  }
};

/**
 * 从 messageContext 提取最终答案
 * @param messageContext 后端返回的消息上下文 JSON 字符串
 * @returns 最终答案文本
 */
export const extractFinalAnswer = (messageContext: string): string => {
  try {
    if (!messageContext || messageContext.trim() === '') {
      return '';
    }

    const contextArray = JSON.parse(messageContext);
    if (!Array.isArray(contextArray)) {
      return '';
    }

    // 优先查找 finalAnswer 类型
    const finalAnswerEntry = contextArray.find((item: any) => item.type === 'finalAnswer');
    if (finalAnswerEntry?.value) {
      return finalAnswerEntry.value;
    }

    // 查找包含 <Final Answer> 标签的数据
    const finalDataEntry = contextArray.find((item: any) =>
      item.type === 'data' && item.value?.includes('<Final Answer>')
    );
    if (finalDataEntry?.value) {
      const match = finalDataEntry.value.match(/<Final Answer>([\s\S]*?)<\/Final Answer>/);
      return match ? match[1].trim() : finalDataEntry.value;
    }

    // 返回最后一个 data 类型的值
    const dataEntries = contextArray.filter((item: any) => item.type === 'data');
    if (dataEntries.length > 0) {
      return dataEntries[dataEntries.length - 1].value || '';
    }

    return '';
  } catch (error) {
    console.error('提取最终答案失败:', error);
    return '';
  }
};

/**
 * 将旧格式的思考日志条目转换为新的 ChatMessage 格式
 * @param entries 思考日志条目数组
 * @param messageId 消息ID
 * @param timestamp 时间戳
 * @returns 转换后的 ChatMessage 对象
 */
export const convertEntriesToMessage = (
  entries: ThinkingLogEntry[],
  messageId: string,
  timestamp: number
): ChatMessage => {
  // 提取思考内容
  const thinkEntries = entries.filter(e => e.type === 'think');
  const thinkingContent = thinkEntries.map(e => e.content).join('\n\n');

  // 提取工具调用
  const toolEntries = entries.filter(e => e.type === 'tool');
  const tools: ToolCall[] = toolEntries.map((e, index) => ({
    id: `${messageId}-tool-${index}`,
    name: extractToolName(e.content),
    params: extractToolParams(e.content),
    result: e.content,
    status: 'success',
    timestamp: e.timestamp
  }));

  // 提取日志
  const logEntries = entries.filter(e => e.type === 'log');
  const logs: LogEntry[] = logEntries.map((e, index) => ({
    id: `${messageId}-log-${index}`,
    content: e.content,
    timestamp: e.timestamp || Date.now(),
    level: 'info'
  }));

  // 提取错误日志
  const errorEntries = entries.filter(e => e.type === 'error');
  errorEntries.forEach((e, index) => {
    logs.push({
      id: `${messageId}-error-${index}`,
      content: e.content,
      timestamp: e.timestamp || Date.now(),
      level: 'error'
    });
  });

  // 提取最终答案
  const finalAnswerEntry = entries.find(e => e.type === 'finalAnswer');
  const dataEntries = entries.filter(e => e.type === 'data');
  const text = finalAnswerEntry?.content || dataEntries[dataEntries.length - 1]?.content || '';

  return {
    id: messageId,
    text,
    isUser: false,
    type: 'data',
    timestamp,
    thinking: thinkingContent ? {
      content: thinkingContent,
      status: 'done'
    } : undefined,
    tools: tools.length > 0 ? tools : undefined,
    logs: logs.length > 0 ? logs : undefined,
    streamStatus: 'done'
  };
};

/**
 * 从工具内容中提取工具名称
 * @param content 工具内容字符串
 * @returns 工具名称
 */
const extractToolName = (content: string): string => {
  // 尝试解析 JSON 格式的工具调用
  try {
    const parsed = JSON.parse(content);
    if (parsed.name || parsed.tool) {
      return parsed.name || parsed.tool;
    }
  } catch {
    // 不是 JSON 格式，继续尝试其他方式
  }

  // 尝试从文本中提取工具名称
  const match = content.match(/工具[：:]\s*(\w+)/);
  if (match) {
    return match[1];
  }

  return '未知工具';
};

/**
 * 从工具内容中提取工具参数
 * @param content 工具内容字符串
 * @returns 工具参数对象
 */
const extractToolParams = (content: string): Record<string, any> => {
  // 尝试解析 JSON 格式的工具调用
  try {
    const parsed = JSON.parse(content);
    if (parsed.params || parsed.parameters || parsed.args) {
      return parsed.params || parsed.parameters || parsed.args;
    }
    // 如果整个对象就是参数
    if (typeof parsed === 'object' && parsed !== null) {
      const { name, tool, result, ...params } = parsed;
      return params;
    }
  } catch {
    // 不是 JSON 格式
  }

  return {};
};

/**
 * 创建新的 AI 消息（用于流式接收）
 * @param messageId 消息ID
 * @returns 新的 ChatMessage 对象
 */
export const createNewAIMessage = (messageId: string): ChatMessage => ({
  id: messageId,
  text: '',
  isUser: false,
  type: 'data',
  timestamp: Date.now(),
  thinking: {
    content: '',
    status: 'streaming'
  },
  tools: [],
  logs: [],
  streamStatus: 'streaming'
});

/**
 * 更新消息的 thinking 内容（流式接收）
 * @param message 当前消息
 * @param content 新的思考内容
 * @returns 更新后的消息
 */
export const updateMessageThinking = (message: ChatMessage, content: string): ChatMessage => {
  // 处理 [THINK] 前缀
  const cleanContent = content.startsWith('[THINK] ') ? content.substring(8) : content;

  return {
    ...message,
    thinking: {
      content: message.thinking ? message.thinking.content + cleanContent : cleanContent,
      status: 'streaming'
    }
  };
};

/**
 * 更新消息的 data 内容（流式接收）
 * @param message 当前消息
 * @param content 新的数据内容
 * @returns 更新后的消息
 */
export const updateMessageData = (message: ChatMessage, content: string): ChatMessage => {
  // 处理 [DATA] 前缀
  const cleanContent = content.startsWith('[DATA] ') ? content.substring(7) : content;

  return {
    ...message,
    text: message.text + cleanContent
  };
};

/**
 * 添加工具调用到消息
 * @param message 当前消息
 * @param toolContent 工具内容
 * @returns 更新后的消息
 */
export const addToolToMessage = (message: ChatMessage, toolContent: string): ChatMessage => {
  const newTool: ToolCall = {
    id: `${message.id}-tool-${(message.tools?.length || 0)}`,
    name: extractToolName(toolContent),
    params: extractToolParams(toolContent),
    result: toolContent,
    status: 'success'
  };

  return {
    ...message,
    tools: [...(message.tools || []), newTool]
  };
};

/**
 * 添加日志到消息
 * @param message 当前消息
 * @param logContent 日志内容
 * @param level 日志级别
 * @returns 更新后的消息
 */
export const addLogToMessage = (
  message: ChatMessage,
  logContent: string,
  level: LogEntry['level'] = 'info'
): ChatMessage => {
  const newLog: LogEntry = {
    id: `${message.id}-log-${(message.logs?.length || 0)}`,
    content: logContent,
    timestamp: Date.now(),
    level
  };

  return {
    ...message,
    logs: [...(message.logs || []), newLog]
  };
};

/**
 * 完成消息流式接收
 * @param message 当前消息
 * @param finalAnswer 最终答案（可选）
 * @returns 更新后的消息
 */
export const finalizeMessage = (message: ChatMessage, finalAnswer?: string): ChatMessage => ({
  ...message,
  text: finalAnswer || message.text,
  thinking: message.thinking ? {
    ...message.thinking,
    status: 'done'
  } : undefined,
  streamStatus: 'done'
});

/**
 * 标记消息出错
 * @param message 当前消息
 * @param errorMessage 错误信息
 * @returns 更新后的消息
 */
export const errorMessage = (message: ChatMessage, errorMessage: string): ChatMessage => ({
  ...message,
  thinking: message.thinking ? {
    ...message.thinking,
    status: 'error'
  } : undefined,
  logs: [
    ...(message.logs || []),
    {
      id: `${message.id}-error`,
      content: errorMessage,
      timestamp: Date.now(),
      level: 'error'
    }
  ],
  streamStatus: 'error'
});
