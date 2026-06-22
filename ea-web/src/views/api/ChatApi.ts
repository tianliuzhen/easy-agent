import {fetchEventSource} from '@microsoft/fetch-event-source';

const API_BASE_URL = 'http://localhost:8080'; // 替换为你的后端地址

// 用于存储当前的 AbortController，以便需要时手动关闭连接
let currentAbortController: AbortController | null = null;

/**
 * SSE 事件载荷（后端 SseHelper 下发的 JSON）。
 * 内容事件固定含 type/content；转场事件（step/handoff/agent）可附带 agentName/index/total 等。
 */
export interface SseEvent {
    type: string;
    content: string;
    agentName?: string;
    index?: number;
    total?: number;

    [key: string]: any;
}

/**
 * SSE 事件处理器集合。新增事件类型时在此扩展，避免位置参数膨胀。
 */
export interface SseHandlers {
    onLog?: (content: string) => void;
    onFinalAnswer?: (content: string) => void;
    onThink?: (content: string) => void;
    onData?: (content: string) => void;
    onTool?: (content: string) => void;
    /** 流水线转场（WORKFLOW）：进入某个子 Agent 节点 */
    onStep?: (evt: SseEvent) => void;
    /** 流式响应结束（后端关闭连接时触发） */
    onDone?: () => void;
    onError?: (error: string) => void;
}

/**
 * 发送消息参数。agentId 与 flowId 二选一：
 * - 传 agentId 走单 Agent 路径；
 * - 传 flowId 走多 Agent 编排路径。
 */
export interface SendMessageParams {
    message: string;
    sessionId: string;
    agentId?: string;
    flowId?: number | string;
    imageBase64?: string;
    handlers: SseHandlers;
}

/**
 * 发送消息到 SSE 服务端，并接收流式响应。
 *
 * @param params 消息内容、会话 ID、agentId/flowId、图片及事件处理器
 * @returns 返回一个函数，用于手动关闭连接
 */
export const sendMessage = (params: SendMessageParams): (() => void) => {
    const {message, sessionId, agentId, flowId, imageBase64, handlers} = params;
    const url = `${API_BASE_URL}/eaAgent/ai/chat`;
    console.log('创建 SSE 连接:', url);
    console.log('请求参数:', {sessionId, msg: message, agentId, flowId, hasImage: !!imageBase64});

    // 创建请求体
    const requestBody: any = {
        sessionId: sessionId || "110",
        msg: message || "你好",
    };
    // 编排路径优先；否则走单 Agent
    if (flowId !== undefined && flowId !== null && `${flowId}` !== '') {
        requestBody.flowId = typeof flowId === 'string' ? parseInt(flowId, 10) : flowId;
    } else {
        requestBody.agentId = agentId || "1";
    }

    // 如果有图片，添加到请求体
    if (imageBase64) {
        requestBody.imageBase64 = imageBase64;
        console.log('发送图片数据，长度:', imageBase64.length);
    }

    console.log('完整请求体:', JSON.stringify({...requestBody, imageBase64: imageBase64 ? '[omitted]' : undefined}));

    // 创建 AbortController 用于手动关闭连接
    const abortController = new AbortController();
    currentAbortController = abortController;

    // 标记是否已结束，避免重复回调
    let isClosed = false;

    // 使用 fetchEventSource 发起 POST 请求获取 SSE 连接
    fetchEventSource(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'text/event-stream',
        },
        body: JSON.stringify(requestBody),
        signal: abortController.signal,
        openWhenHidden: true, // todo 这个参数很重要，20260233排查了很久， 浏览器对后台连接有限制 - 现代浏览器会优化资源，自动限制后台页面的网络请求
        async onopen(response) {
            console.log('SSE 连接已建立，状态码:', response.status);
            console.log('响应 Content-Type:', response.headers.get('content-type'));

            if (response.ok && response.headers.get('content-type')?.includes('text/event-stream')) {
                console.log('SSE 连接成功，准备接收流式数据');
                return; // 连接成功
            }

            // 如果连接失败，抛出错误
            const errorText = await response.text().catch(() => '');
            throw new Error(`连接失败: ${response.status} ${response.statusText} - ${errorText}`);
        },

        onmessage(event) {
            try {
                // 忽略空数据或保持连接的心跳包
                if (!event.data || event.data.trim() === '' || event.data === ':keepalive') {
                    console.log('收到心跳包或空数据，忽略');
                    return;
                }

                // 解析 JSON 数据
                const jsonData: SseEvent = JSON.parse(event.data);

                // 根据数据类型分发到对应处理器
                switch (jsonData.type) {
                    case 'log':
                        handlers.onLog?.(jsonData.content);
                        break;
                    case 'finalAnswer':
                        handlers.onFinalAnswer?.(jsonData.content);
                        break;
                    case 'think':
                        handlers.onThink?.(jsonData.content);
                        break;
                    case 'data':
                        handlers.onData?.(jsonData.content);
                        break;
                    case 'tool':
                        handlers.onTool?.(jsonData.content);
                        break;
                    case 'step':
                        handlers.onStep?.(jsonData);
                        break;
                    case 'error':
                        isClosed = true;
                        handlers.onError?.(jsonData.content);
                        break;
                    default:
                        console.warn('未知的数据类型:', jsonData.type, jsonData);
                }
            } catch (error) {
                console.error('SSE 数据解析失败:', error);
                console.error('接收到的原始数据:', event.data);
                handlers.onError?.(`数据解析失败: ${error instanceof Error ? error.message : '未知错误'}`);
            }
        },

        onerror(err) {
            console.error('SSE 连接错误:', err);

            // 如果是手动中止，不触发错误回调
            if (err.name === 'AbortError') {
                console.log('连接已被手动关闭');
                return;
            }

            // 如果是连接关闭导致的错误，不重复触发错误回调
            if (!isClosed && err.name !== 'AbortError') {
                handlers.onError?.(`连接错误: ${err.message || '未知错误'}`);
            }

            // 重要：不要抛出错误，否则会导致连接彻底中断且无法重连
            return undefined; // 不自动重连，避免重复请求
        },

        onclose() {
            console.log('SSE 连接已关闭');
            if (!isClosed) {
                isClosed = true;
                handlers.onDone?.();
            }
            currentAbortController = null;
        }
    }).catch((error) => {
        // 捕获 fetchEventSource 抛出的异常
        if (error.name !== 'AbortError' && !isClosed) {
            console.error('fetchEventSource 异常:', error);
            handlers.onError?.(`请求异常: ${error.message || '未知错误'}`);
            isClosed = true;
            handlers.onDone?.();
        }
        currentAbortController = null;
    });

    // 返回一个函数，用于手动关闭连接
    return () => {
        console.log('手动关闭 SSE 连接');
        if (abortController && !abortController.signal.aborted) {
            abortController.abort();
            isClosed = true;
        }
        currentAbortController = null;
    };
};

/**
 * 关闭当前的 SSE 连接
 */
export const closeCurrentConnection = (): void => {
    if (currentAbortController && !currentAbortController.signal.aborted) {
        console.log('关闭当前 SSE 连接');
        currentAbortController.abort();
        currentAbortController = null;
    }
};
