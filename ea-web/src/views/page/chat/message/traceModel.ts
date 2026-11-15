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
 * 执行过程中的一个步骤。
 * <p>
 * 用有序步骤而不是「所有思考 + 所有工具」两个数组：模型的真实行为是
 * 思考 → 调工具 → 看结果 → 继续思考，按类型分流会把这个链路拍平，
 * 变成一堆工具堆在推理文字下面，读者无法判断哪次思考促成了哪次调用。
 */
export type TraceStep =
    | {kind: 'reasoning'; key: string; content: string}
    /**
     * 一组连续的工具调用。
     * 模型一轮常并行发多个 tool_call（例如同时查多张表的结构），
     * 它们之间没有先后依赖，合成一组横向排列，不必每个占一整行。
     */
    | {kind: 'tools'; key: string; tools: ToolTrace[]};

/**
 * 一条助手消息的执行过程。
 * 回答文本与过程严格分开：回答只在正文渲染一次，过程面板只放推理、工具与日志。
 */
export interface MessageTrace {
    /** 按真实时序排列的步骤：推理与工具调用交替出现 */
    steps: TraceStep[];
    /** 工具调用总数，用于折叠条概要 */
    toolCount: number;
    /** 推理总字数，用于折叠条概要 */
    reasoningLength: number;
    /** 运行日志，与时序无关，统一放在面板末尾 */
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
 * 把扁平的 SSE 事件列表整理成有序的执行步骤。
 * <p>
 * 1. 保留事件到达顺序：思考与工具调用交替呈现，还原模型的真实决策链路
 * 2. 连续的思考片段合并为一个步骤（SSE 把一段思考拆成很多 chunk）
 * 3. tool 事件遇到「正在执行工具」标记视为新一次调用，否则并入上一次（入参、结果分片到达）
 * 4. log 与 error 不参与时序，单独收集
 */
export const buildMessageTrace = (entries?: ThinkingLogEntry[]): MessageTrace => {
    const steps: TraceStep[] = [];
    const logs: string[] = [];
    const errors: string[] = [];
    const answerParts: string[] = [];
    let toolCount = 0;
    let reasoningLength = 0;

    (entries || []).forEach((entry) => {
        switch (entry.type) {
            case 'think':
                appendReasoning(steps, entry.content);
                break;
            case 'tool':
                toolCount = appendTool(steps, entry.content, toolCount);
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

    // 去掉只有空白的推理步骤，并统计推理总字数
    const cleaned = steps.filter((step) => step.kind !== 'reasoning' || step.content.trim() !== '');
    cleaned.forEach((step) => {
        if (step.kind === 'reasoning') reasoningLength += step.content.trim().length;
    });

    return {
        steps: cleaned,
        toolCount,
        reasoningLength,
        logs,
        errors,
        streamedAnswer: answerParts.join(''),
        hasTrace: cleaned.length > 0 || logs.length > 0,
    };
};

/** 追加思考片段：能并入上一个推理步骤就并入，否则新起一步 */
const appendReasoning = (steps: TraceStep[], chunk: string) => {
    if (!chunk) {
        return;
    }
    const last = steps[steps.length - 1];
    if (last && last.kind === 'reasoning') {
        last.content += chunk;
        return;
    }
    steps.push({kind: 'reasoning', key: `reasoning-${steps.length}`, content: chunk});
};

/**
 * 追加工具内容。
 * <p>
 * 带开始标记的视为新一次调用，并入当前这组连续调用（没有则新建一组）；
 * 否则是上一次调用的后续分片（入参、执行结果），并入该组最后一个工具。
 *
 * @return 更新后的工具调用总数
 */
const appendTool = (steps: TraceStep[], chunk: string, toolCount: number): number => {
    const isNewCall = TOOL_START_MARKERS.some((marker) => chunk.includes(marker));
    const last = steps[steps.length - 1];
    const lastGroup = last && last.kind === 'tools' ? last : null;

    // 后续分片：补全该组最后一个工具的入参与结果
    if (!isNewCall && lastGroup && lastGroup.tools.length > 0) {
        const target = lastGroup.tools[lastGroup.tools.length - 1];
        lastGroup.tools[lastGroup.tools.length - 1] =
                parseToolTrace(`${target.raw}${chunk}`, target.key);
        return toolCount;
    }

    const key = `tool-${toolCount}`;
    const tool = parseToolTrace(chunk, key);
    // 紧跟在工具组后面的新调用并入同一组；中间插过推理则另起一组
    if (lastGroup) {
        lastGroup.tools.push(tool);
    } else {
        steps.push({kind: 'tools', key: `tools-${steps.length}`, tools: [tool]});
    }
    return toolCount + 1;
};

/** 格式化 JSON 字符串用于展示；非 JSON 原样返回 */
export const formatJson = (value: string): string => {
    try {
        return JSON.stringify(JSON.parse(value), null, 2);
    } catch {
        return value;
    }
};

/**
 * 从工具入参里提取一句可读摘要，用于在列表行上区分同名调用。
 * <p>
 * 同一个工具常被连续调用多次、只是参数不同（例如查 4 张不同表的结构），
 * 只显示工具名会让这几行看起来完全一样。
 *
 * @param inputParams 入参原文，通常是 JSON
 * @return 摘要文本；无法提取时返回空字符串
 */
export const summarizeToolInput = (inputParams: string): string => {
    const trimmed = (inputParams || '').trim();
    if (trimmed === '' || trimmed === '{}') {
        return '';
    }
    try {
        const parsed = JSON.parse(trimmed);
        if (parsed === null || typeof parsed !== 'object') {
            return clip(String(parsed));
        }
        const values = Object.values(parsed as Record<string, unknown>)
            .filter((value) => value !== null && value !== undefined && value !== '')
            .map((value) => (typeof value === 'object' ? JSON.stringify(value) : String(value)));
        // 单个参数直接展示值（表名、日期这类最常见）；多个参数用 · 连接
        return values.length === 0 ? '' : clip(values.join(' · '));
    } catch {
        return clip(trimmed);
    }
};

/** 摘要限长，过长时截断；SQL 这类长文本只保留开头 */
const clip = (text: string, maxLength = 72): string => {
    const oneLine = text.replace(/\s+/g, ' ').trim();
    return oneLine.length > maxLength ? `${oneLine.slice(0, maxLength - 1)}…` : oneLine;
};
