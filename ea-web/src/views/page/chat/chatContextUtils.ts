import type {ChatMessage, ThinkingLogEntry, ThinkingLogState} from './ChatComponents';

/** 后端 messageContext 中单条记录的原始结构 */
interface RawContextItem {
    type: string;
    value?: string;
    time?: number;
}

/** 后端会话历史中单条消息的原始结构 */
interface RawHistoryMessage {
    id?: number | string;
    question?: string;
    messageContext?: string;
    messageType?: string;
    userMessage?: boolean;
    createdAt?: string;
}

// 后端 messageContext 事件类型 → 前端思考日志类型
const CONTEXT_TYPE_MAP: Record<string, ThinkingLogEntry['type']> = {
    thinking: 'think',
    data: 'data',
    tool: 'tool',
    log: 'log',
    error: 'error',
    finalAnswer: 'finalAnswer',
};

const safeParseContext = (messageContext?: string): RawContextItem[] => {
    if (!messageContext || messageContext.trim() === '') return [];
    try {
        const parsed = JSON.parse(messageContext);
        return Array.isArray(parsed) ? parsed : [];
    } catch (error) {
        console.error('解析 messageContext 失败:', error);
        return [];
    }
};

/** 将后端 messageContext 转为思考日志条目列表 */
export const parseThinkingEntries = (messageContext?: string): ThinkingLogEntry[] => {
    return safeParseContext(messageContext)
        .map((item): ThinkingLogEntry | null => {
            const mapped = CONTEXT_TYPE_MAP[item.type];
            if (!mapped) return null;
            return {type: mapped, content: item.value || '', timestamp: item.time};
        })
        .filter((entry): entry is ThinkingLogEntry => entry !== null);
};

/**
 * 从 messageContext 数组中提取要展示的答案文本，三级回退：
 * 1. finalAnswer 条目
 * 2. 含 <Final Answer> 标签的 data 条目（取标签内文本）
 * 3. 最后一条 data 条目
 */
export const extractAnswerText = (contextArray: RawContextItem[]): string => {
    const finalAnswer = contextArray.find((item) => item.type === 'finalAnswer');
    if (finalAnswer?.value) return finalAnswer.value;

    const taggedData = contextArray.find(
        (item) => item.type === 'data' && item.value?.includes('<Final Answer>')
    );
    if (taggedData?.value) {
        const match = taggedData.value.match(/<Final Answer>([\s\S]*?)<\/Final Answer>/);
        return match ? match[1].trim() : taggedData.value;
    }

    const dataEntries = contextArray.filter((item) => item.type === 'data');
    return dataEntries.length > 0 ? dataEntries[dataEntries.length - 1].value || '' : '';
};

/** 将后端完整会话历史转换为聊天消息列表 + 思考日志状态 */
export const historyToChatMessages = (
    chatHistory: RawHistoryMessage[]
): {messages: ChatMessage[]; thinkingLogs: ThinkingLogState} => {
    const messages: ChatMessage[] = [];
    const thinkingLogs: ThinkingLogState = {};

    chatHistory.forEach((msg) => {
        const hasContext = !!(msg.messageContext && msg.messageContext.trim() !== '');
        const hasQuestion = !!(msg.question && msg.question.trim() !== '');
        const messageId = msg.id?.toString() || crypto.randomUUID();
        const timestamp = msg.createdAt ? new Date(msg.createdAt).getTime() : Date.now();

        if (hasQuestion && hasContext) {
            // 一问一答：拆成用户消息 + AI 消息
            messages.push({text: msg.question!, isUser: true, type: 'data', id: `${messageId}-user`, timestamp});

            const aiMessageId = `${messageId}-ai`;
            const entries = parseThinkingEntries(msg.messageContext);
            const aiText = extractAnswerText(safeParseContext(msg.messageContext));
            messages.push({text: aiText, isUser: false, type: 'data', id: aiMessageId, timestamp});
            if (entries.length > 0) {
                thinkingLogs[aiMessageId] = {content: entries, isVisible: true};
            }
        } else if (hasContext) {
            // 仅有上下文：按 messageType 判定角色
            const isUser = msg.userMessage || msg.messageType?.includes('user') || false;
            const entries = parseThinkingEntries(msg.messageContext);
            const text = extractAnswerText(safeParseContext(msg.messageContext));
            messages.push({text, isUser, type: 'data', id: messageId, timestamp});
            if (!isUser && entries.length > 0) {
                thinkingLogs[messageId] = {content: entries, isVisible: true};
            }
        } else if (hasQuestion) {
            // 仅有提问
            messages.push({text: msg.question!, isUser: true, type: 'data', id: messageId, timestamp});
        }
    });

    return {messages, thinkingLogs};
};
