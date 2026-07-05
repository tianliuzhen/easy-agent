import {useCallback, useEffect, useRef, useState} from 'react';
import {sendMessage} from '../../../api/ChatApi';
import type {ChatMessage, ThinkingLogEntry, ThinkingLogState} from '../ChatComponents';

export interface SendContext {
    sessionId: string;
    agentId: string;
    flowId?: number | string;
    onDone?: () => void;
}

export interface UseChatStream {
    messages: ChatMessage[];
    thinkingLogs: ThinkingLogState;
    isThinking: boolean;
    error: string | null;
    currentAnsweringMsgId: string | null;
    input: string;
    setInput: (value: string) => void;
    selectedImage?: string;
    setSelectedImage: (value: string | undefined) => void;
    toggleThinkingLog: (messageId: string) => void;
    /** 用历史记录替换当前消息与思考日志 */
    loadHistory: (messages: ChatMessage[], thinkingLogs: ThinkingLogState) => void;
    /** 清空为新对话 */
    reset: () => void;
    /** 发送当前 input（+ 图片）；返回 false 表示无内容未发送 */
    send: (ctx: SendContext) => boolean;
}

/**
 * 管理一次会话的实时消息流：消息列表、思考日志、输入框、图片，以及 SSE 发送与事件聚合。
 * 内部持有上一次连接的关闭函数，发送新消息或卸载时中断旧连接，避免并发流互相污染。
 */
export const useChatStream = (): UseChatStream => {
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [thinkingLogs, setThinkingLogs] = useState<ThinkingLogState>({});
    const [isThinking, setIsThinking] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [currentAnsweringMsgId, setCurrentAnsweringMsgId] = useState<string | null>(null);
    const [input, setInput] = useState('');
    const [selectedImage, setSelectedImage] = useState<string | undefined>(undefined);

    const closeRef = useRef<(() => void) | null>(null);

    // 卸载时中断进行中的连接
    useEffect(() => {
        return () => {
            closeRef.current?.();
            closeRef.current = null;
        };
    }, []);

    const toggleThinkingLog = useCallback((messageId: string) => {
        setThinkingLogs((prev) => {
            const current = prev[messageId];
            if (!current) return prev;
            return {...prev, [messageId]: {...current, isVisible: !current.isVisible}};
        });
    }, []);

    const loadHistory = useCallback((historyMessages: ChatMessage[], historyLogs: ThinkingLogState) => {
        closeRef.current?.();
        closeRef.current = null;
        setIsThinking(false);
        setCurrentAnsweringMsgId(null);
        setError(null);
        setMessages(historyMessages);
        setThinkingLogs(historyLogs);
    }, []);

    const reset = useCallback(() => {
        closeRef.current?.();
        closeRef.current = null;
        setIsThinking(false);
        setCurrentAnsweringMsgId(null);
        setError(null);
        setMessages([]);
        setThinkingLogs({});
        setSelectedImage(undefined);
    }, []);

    const send = useCallback(
        (ctx: SendContext): boolean => {
            const text = input;
            if (text.trim() === '' && !selectedImage) return false;

            // 中断上一次仍在进行的连接
            closeRef.current?.();

            setError(null);
            const image = selectedImage;
            const userMessageId = crypto.randomUUID();
            const aiMessageId = crypto.randomUUID();

            const userMessage: ChatMessage = {
                text,
                isUser: true,
                type: 'data',
                id: userMessageId,
                timestamp: Date.now(),
                imageBase64: image,
            };
            const aiMessage: ChatMessage = {
                text: '',
                isUser: false,
                type: 'data',
                id: aiMessageId,
                timestamp: Date.now(),
            };

            setMessages((prev) => [...prev, userMessage, aiMessage]);
            setInput('');
            setSelectedImage(undefined);
            setIsThinking(true);
            setCurrentAnsweringMsgId(aiMessageId);
            setThinkingLogs((prev) => ({...prev, [aiMessageId]: {content: [], isVisible: true}}));

            const appendEntry = (entry: ThinkingLogEntry) => {
                setThinkingLogs((prev) => {
                    const current = prev[aiMessageId];
                    return {
                        ...prev,
                        [aiMessageId]: {
                            content: [...(current?.content || []), entry],
                            isVisible: current?.isVisible ?? true,
                        },
                    };
                });
            };

            const finish = () => {
                setIsThinking(false);
                setCurrentAnsweringMsgId(null);
                closeRef.current = null;
            };

            closeRef.current = sendMessage({
                message: text,
                sessionId: ctx.sessionId,
                agentId: ctx.agentId,
                flowId: ctx.flowId,
                imageBase64: image,
                handlers: {
                    onLog: (log) => appendEntry({type: 'log', content: log}),
                    onStep: (evt) =>
                        appendEntry({
                            type: 'log',
                            content: `▶ 第 ${evt.index}/${evt.total} 步：${evt.agentName || ''}`,
                        }),
                    onThink: (think) =>
                        appendEntry({
                            type: 'think',
                            content: think.startsWith('[THINK] ') ? think.substring(8) : think,
                        }),
                    onData: (data) =>
                        appendEntry({
                            type: 'data',
                            content: data.startsWith('[DATA] ') ? data.substring(7) : data,
                        }),
                    onTool: (tool) => appendEntry({type: 'tool', content: tool}),
                    onFinalAnswer: (finalAnswer) => {
                        setMessages((prev) =>
                            prev.map((msg) => (msg.id === aiMessageId ? {...msg, text: finalAnswer} : msg))
                        );
                        appendEntry({type: 'finalAnswer', content: finalAnswer});
                    },
                    onDone: () => {
                        finish();
                        ctx.onDone?.();
                    },
                    onError: (errorMessage) => {
                        appendEntry({type: 'error', content: errorMessage});
                        setError(errorMessage);
                        finish();
                    },
                },
            });

            return true;
        },
        [input, selectedImage]
    );

    return {
        messages,
        thinkingLogs,
        isThinking,
        error,
        currentAnsweringMsgId,
        input,
        setInput,
        selectedImage,
        setSelectedImage,
        toggleThinkingLog,
        loadHistory,
        reset,
        send,
    };
};
