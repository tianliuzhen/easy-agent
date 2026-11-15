import type {ChatMessage, ThinkingLogEntry, ThinkingLogState} from './ChatComponents';

/** 后端返回的单条执行轨迹 */
interface RawTrace {
    seq?: number;
    /** THINKING / DATA / TOOL / FINAL_ANSWER / ERROR */
    traceType?: string;
    content?: string;
    toolName?: string;
    toolInput?: string;
    toolOutput?: string;
    agentName?: string;
    flowNodeId?: string;
    durationMs?: number;
}

/** 后端会话历史中单条消息的原始结构 */
interface RawHistoryMessage {
    id?: number | string;
    question?: string;
    answer?: string;
    messageType?: string;
    userMessage?: boolean;
    createdAt?: string;
    /** 执行轨迹，按 seq 升序 */
    traces?: RawTrace[];
}

// 后端轨迹类型 → 前端思考日志类型
const TRACE_TYPE_MAP: Record<string, ThinkingLogEntry['type']> = {
    THINKING: 'think',
    DATA: 'data',
    TOOL: 'tool',
    FINAL_ANSWER: 'finalAnswer',
    ERROR: 'error',
};

/**
 * 把工具轨迹还原成前端展示用的文本。
 * <p>
 * 后端已按字段存了工具名、入参、结果，这里拼回 `traceModel.parseToolTrace`
 * 认识的格式，让实时与回放共用同一套解析，避免两套渲染逻辑。
 */
const formatToolTrace = (trace: RawTrace): string => {
    const parts = [`正在执行工具：${trace.toolName || '未知工具'}`];
    if (trace.toolInput) parts.push(`工具入参：${trace.toolInput}`);
    if (trace.toolOutput) parts.push(`执行结果：${trace.toolOutput}`);
    return parts.join('\n');
};

/** 将后端轨迹转为前端思考日志条目 */
export const tracesToEntries = (traces?: RawTrace[]): ThinkingLogEntry[] =>
    (traces || [])
        .map((trace): ThinkingLogEntry | null => {
            const mapped = TRACE_TYPE_MAP[trace.traceType || ''];
            if (!mapped) return null;
            const content = mapped === 'tool' ? formatToolTrace(trace) : trace.content || '';
            return {type: mapped, content};
        })
        .filter((entry): entry is ThinkingLogEntry => entry !== null);

/**
 * 取要展示的答案文本。
 * <p>
 * 优先用后端回填的 answer（它本身就是「FINAL_ANSWER 优先，否则拼接 DATA」的口径），
 * 为空时再从轨迹里兜底，保证刷新后看到的答案与生成时一致。
 */
export const resolveAnswerText = (message: RawHistoryMessage): string => {
    if (message.answer && message.answer.trim() !== '') {
        return message.answer;
    }
    const traces = message.traces || [];
    const finalAnswer = [...traces].reverse().find((trace) => trace.traceType === 'FINAL_ANSWER');
    if (finalAnswer?.content) {
        return finalAnswer.content;
    }
    return traces
        .filter((trace) => trace.traceType === 'DATA')
        .map((trace) => trace.content || '')
        .join('');
};

/**
 * 将后端完整会话历史转换为聊天消息列表 + 执行轨迹。
 * <p>
 * 后端一条消息就是一轮问答（question + answer + traces），
 * 这里拆成前端的「用户气泡 + 助手气泡」两条消息。
 */
export const historyToChatMessages = (
    chatHistory: RawHistoryMessage[]
): {messages: ChatMessage[]; thinkingLogs: ThinkingLogState} => {
    const messages: ChatMessage[] = [];
    const thinkingLogs: ThinkingLogState = {};

    chatHistory.forEach((raw) => {
        const messageId = raw.id?.toString() || crypto.randomUUID();
        const timestamp = raw.createdAt ? new Date(raw.createdAt).getTime() : Date.now();
        const hasQuestion = !!raw.question && raw.question.trim() !== '';
        const entries = tracesToEntries(raw.traces);
        const answer = resolveAnswerText(raw);

        if (hasQuestion) {
            messages.push({
                text: raw.question!,
                isUser: true,
                type: 'data',
                id: `${messageId}-user`,
                timestamp,
            });
        }

        // 有答案或有轨迹才产生助手气泡：只落了提问、模型还没回答的记录不占位
        if (answer.trim() !== '' || entries.length > 0) {
            const aiMessageId = `${messageId}-ai`;
            messages.push({
                text: answer,
                isUser: false,
                type: 'data',
                id: aiMessageId,
                timestamp,
            });
            if (entries.length > 0) {
                thinkingLogs[aiMessageId] = {content: entries, isVisible: true};
            }
        }
    });

    return {messages, thinkingLogs};
};
