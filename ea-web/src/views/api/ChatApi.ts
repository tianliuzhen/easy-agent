import {fetchEventSource} from '@microsoft/fetch-event-source';

const API_BASE_URL = 'http://localhost:8080'; // 替换为你的后端地址

// 用于存储当前的 AbortController，以便需要时手动关闭连接
let currentAbortController: AbortController | null = null;

/**
 * 发送消息到 SSE 服务端，并接收流式响应
 * @param message 用户输入的消息
 * @param sessionId 会话 ID
 * @param agentId 代理 ID
 * @param onLog 每次接收到日志数据时的回调
 * @param onFinalAnswer 每次接收到最终答案数据时的回调
 * @param onThink 每次接收到思考过程数据时的回调
 * @param onData 每次接收到数据消息时的回调
 * @param onTool 每次接收到工具执行数据时的回调
 * @param onDone 流式响应结束时的回调（后端关闭连接时触发）
 * @param onError 发生错误时的回调（如解析失败）
 * @returns 返回一个函数，用于手动关闭连接
 */
export const sendMessage = (
    message: string,
    sessionId: string,
    agentId: string,
    onLog: (log: string) => void,
    onFinalAnswer: (data: string) => void,
    onThink: (think: string) => void,
    onData: (data: string) => void,
    onTool: (tool: string) => void,
    onDone: () => void,
    onError: (error: string) => void
): (() => void) => {
    const url = `${API_BASE_URL}/eaAgent/ai/chat`;
    console.log('创建 SSE 连接:', url);
    console.log('请求参数:', {sessionId, msg: message, agentId});

    // 创建请求体
    const requestBody = {
        sessionId: sessionId || "110",
        msg: message || "你好",
        agentId: agentId || "1"
    };

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
        // 关键修复：禁用自动重连，避免重复请求
        // 或者设置重连策略
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

                console.log('SSE 收到数据:', event.data);

                // 解析 JSON 数据
                const jsonData = JSON.parse(event.data);

                // 根据数据类型调用相应的回调
                if (jsonData.type === 'log') {
                    onLog(jsonData.content);
                } else if (jsonData.type === 'finalAnswer') {
                    onFinalAnswer(jsonData.content);
                } else if (jsonData.type === 'think') {
                    onThink(jsonData.content);
                } else if (jsonData.type === 'data') {
                    onData(jsonData.content);
                } else if (jsonData.type === 'tool') {
                    onTool(jsonData.content);
                } else {
                    // 处理未知类型的数据
                    console.warn('未知的数据类型:', jsonData.type, jsonData);
                }
            } catch (error) {
                console.error('SSE 数据解析失败:', error);
                console.error('接收到的原始数据:', event.data);
                onError(`数据解析失败: ${error instanceof Error ? error.message : '未知错误'}`);
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
                onError(`连接错误: ${err.message || '未知错误'}`);
            }

            // 重要：不要抛出错误，否则会导致连接彻底中断且无法重连
            // 如果需要重连，可以在这里返回自定义的重连延迟
            // 返回一个数字表示重连延迟（毫秒），返回 undefined 表示不重连
            return undefined; // 不自动重连，避免重复请求
        },

        onclose() {
            console.log('SSE 连接已关闭');
            if (!isClosed) {
                isClosed = true;
                onDone();
            }
            currentAbortController = null;
        }
    }).catch((error) => {
        // 捕获 fetchEventSource 抛出的异常
        if (error.name !== 'AbortError' && !isClosed) {
            console.error('fetchEventSource 异常:', error);
            onError(`请求异常: ${error.message || '未知错误'}`);
            isClosed = true;
            onDone();
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
