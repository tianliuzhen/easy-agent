import React, {useEffect, useRef, useState} from 'react';
import {CaretRightOutlined, LoadingOutlined} from '@ant-design/icons';
import MarkdownContent from './MarkdownContent';
import ToolCallList from './ToolCallList';
import type {MessageTrace} from './traceModel';
import {useThinkingVerb} from './thinkingVerbs';
import './chatMessage.css';

interface TracePanelProps {
    trace: MessageTrace;
    /** 该消息是否正在生成 */
    streaming: boolean;
}

/**
 * 执行过程面板：推理、工具调用、运行日志。
 * <p>
 * 展开策略：生成中自动展开，便于观察进度；生成结束自动收起，让回答成为视觉主体。
 * 用户手动操作后以用户选择为准，不再被自动收起覆盖。
 */
const TracePanel: React.FC<TracePanelProps> = ({trace, streaming}) => {
    const [open, setOpen] = useState(streaming);
    // 用户是否手动点过；手动之后不再自动开合
    const toggledByUser = useRef(false);
    const panelRef = useRef<HTMLDivElement>(null);
    // 生成中每 5 秒换一个思考提示词
    const thinkingVerb = useThinkingVerb(streaming);

    useEffect(() => {
        if (toggledByUser.current) return;
        setOpen(streaming);
    }, [streaming]);

    // 生成中让过程面板跟到底部；直接写 scrollTop（非平滑），避免每个 chunk 重启滚动动画
    useEffect(() => {
        if (!open || !streaming) return;
        const panel = panelRef.current;
        if (!panel) return;
        panel.scrollTop = panel.scrollHeight;
    }, [open, streaming, trace.reasoning, trace.tools.length, trace.logs.length]);

    const handleToggle = () => {
        toggledByUser.current = true;
        setOpen((current) => !current);
    };

    return (
        <div className="ea-chat-trace">
            <button type="button" className="ea-chat-trace-head" onClick={handleToggle}>
                {streaming
                    ? <LoadingOutlined spin style={{fontSize: 11}}/>
                    : <CaretRightOutlined className={`ea-chat-trace-caret${open ? ' ea-chat-trace-caret-open' : ''}`}/>}
                {/* key 跟着提示词变化，让每次切换重新播放淡入，读起来是「换了一句」而非文字突变 */}
                <span key={thinkingVerb ?? 'done'} className={streaming ? 'ea-chat-trace-verb' : undefined}>
                    {streaming ? `${thinkingVerb}…` : '已深度思考'}
                </span>
                {/* 概要只在生成结束后展示：推理字数每个 chunk 都在变，
                    持续改写会让这行右对齐文字不停抖动 */}
                {!streaming && <span className="ea-chat-trace-meta">{describeTrace(trace)}</span>}
            </button>

            {open && (
                <div className="ea-chat-trace-panel" ref={panelRef}>
                    {trace.reasoning && (
                        <MarkdownContent
                            content={trace.reasoning}
                            streaming={streaming}
                            className="ea-chat-reasoning"
                        />
                    )}
                    <ToolCallList tools={trace.tools}/>
                    {trace.logs.length > 0 && (
                        <div className="ea-chat-logs">
                            {trace.logs.map((log, index) => (
                                <div className="ea-chat-log ea-chat-plaintext" key={`log-${index}`}>{log}</div>
                            ))}
                        </div>
                    )}
                </div>
            )}
        </div>
    );
};

/** 面板右侧的概要：有几次工具调用、推理多长 */
const describeTrace = (trace: MessageTrace): string => {
    const parts: string[] = [];
    if (trace.tools.length > 0) parts.push(`${trace.tools.length} 次工具调用`);
    if (trace.reasoning) parts.push(`${trace.reasoning.length} 字推理`);
    return parts.join(' · ');
};

export default TracePanel;
