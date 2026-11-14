import type {ThinkingLogEntry} from '../ChatComponents';

/** 一次工具调用的解析结果 */
export interface ToolTrace {
    /** 列表 key，按出现顺序生成 */
    key: string;
    /** 工具名；解析不到时为空字符串 */
    name: string;
    /** 入参原文（通常是 JSON 字符串） */
    inputParams: string;
    /** 执行结果原文 */
    outputParams: string;
    /** 原始内容，解析不出结构时兜底展示 */
    raw: string;
}

/**
 * 一条助手消息的执行过程。
 * 回答文本与过程严格分开：回答只在正文渲染一次，过程面板只放推理、工具与日志。
 */
export interface MessageTrace {
    /** 推理文本（think 事件拼接） */
    reasoning: string;
    /** 工具调用列表 */
    tools: ToolTrace[];
    /** 运行日志 */
    logs: string[];
    /** 错误信息 */
    errors: string[];
    /**
     * 流式回答文本（data 事件拼接）。
     * 后端只在 finalAnswer 事件里给出完整答案，在那之前消息本身没有文本，
     * 用它作为流式阶段的回答来源，避免"回答生成中却什么都看不到"。
     */
    streamedAnswer: string;
    /** 是否有可展示的执行过程 */
    hasTrace: boolean;
}

/** 后端在一次工具调用开始时会带上的标记，用于切分连续的 tool 事件 */
const TOOL_START_MARKERS = ['正在执行工具', '工具名称'];

const TOOL_NAME_PATTERN =
    /(?:正在执行工具|工具名称)[:：]\s*([\s\S]+?)(?:\s*(?:\n|$|工具入参|输入参数|执行结果|输出参数))/;
const TOOL_INPUT_PATTERN = /(?:工具入参|输入参数)[:：]\s*({[\s\S]*?})\s*(?:执行结果|输出参数|$)/;
const TOOL_OUTPUT_PATTERN = /(?:执行结果|输出参数)[:：]\s*([\s\S]+)$/;

/** 解析单次工具调用的内容 */
export const parseToolTrace = (content: string, key: string): ToolTrace => {
    const nameMatch = content.match(TOOL_NAME_PATTERN);
    const inputMatch = content.match(TOOL_INPUT_PATTERN);
    const outputMatch = content.match(TOOL_OUTPUT_PATTERN);
    return {
        key,
        name: nameMatch ? nameMatch[1].trim() : '',
        inputParams: inputMatch ? inputMatch[1].trim() : '',
        outputParams: outputMatch ? outputMatch[1].trim() : '',
        raw: content,
    };
};

/**
 * 把扁平的 SSE 事件列表整理成可展示的执行过程。
 * <p>
 * 1. 同类型的连续事件拼成一段（SSE 会把一条消息拆成多个 chunk）
 * 2. tool 事件遇到「正在执行工具」标记时视为新一次调用，不与上一次合并
 * 3. 按类型分流：think → 推理，tool → 工具，log → 日志，error → 错误，data → 流式回答
 */
export const buildMessageTrace = (entries?: ThinkingLogEntry[]): MessageTrace => {
    const reasoningParts: string[] = [];
    const toolContents: string[] = [];
    const logs: string[] = [];
    const errors: string[] = [];
    const answerParts: string[] = [];

    (entries || []).forEach((entry) => {
        switch (entry.type) {
            case 'think':
                reasoningParts.push(entry.content);
                break;
            case 'tool':
                appendToolContent(toolContents, entry.content);
                break;
            case 'log':
                if (entry.content.trim() !== '') logs.push(entry.content);
                break;
            case 'error':
                if (entry.content.trim() !== '') errors.push(entry.content);
                break;
            case 'data':
                answerParts.push(entry.content);
                break;
            default:
                // finalAnswer 已经写入消息正文，这里不再重复收集
                break;
        }
    });

    const tools = toolContents.map((content, index) => parseToolTrace(content, `tool-${index}`));
    const reasoning = reasoningParts.join('').trim();

    return {
        reasoning,
        tools,
        logs,
        errors,
        streamedAnswer: answerParts.join(''),
        hasTrace: reasoning !== '' || tools.length > 0 || logs.length > 0,
    };
};

/** 追加 tool 内容：带开始标记的视为新一次调用，否则并入上一次 */
const appendToolContent = (toolContents: string[], content: string) => {
    const isNewCall = TOOL_START_MARKERS.some((marker) => content.includes(marker));
    if (isNewCall || toolContents.length === 0) {
        toolContents.push(content);
        return;
    }
    toolContents[toolContents.length - 1] += content;
};

/** 格式化 JSON 字符串用于展示；非 JSON 原样返回 */
export const formatJson = (value: string): string => {
    try {
        return JSON.stringify(JSON.parse(value), null, 2);
    } catch {
        return value;
    }
};
