import React, {useState} from 'react';
import {Drawer, Tooltip, Typography} from 'antd';
import {CodeOutlined} from '@ant-design/icons';
import {formatJson, summarizeToolInput, type ToolTrace} from './traceModel';
import './chatMessage.css';

const {Text} = Typography;

interface ToolCallGroupProps {
    /** 一组连续的工具调用，横向排列 */
    tools: ToolTrace[];
}

/**
 * 一组连续工具调用的紧凑展示。
 * <p>
 * 每次调用只占一个图标：模型一轮常并行发多个 tool_call，
 * 一个调用一整行会把推理文字挤散，读者看到的是一片刷屏。
 * 悬停显示工具名与入参（纯图标无法区分同名调用），点击打开详情抽屉。
 */
const ToolCallGroup: React.FC<ToolCallGroupProps> = ({tools}) => {
    const [activeTool, setActiveTool] = useState<ToolTrace | null>(null);

    if (tools.length === 0) {
        return null;
    }

    return (
        <>
            <div className="ea-chat-tool-group">
                {tools.map((tool) => (
                    <Tooltip key={tool.key} title={tooltipTitle(tool)} placement="top">
                        <button
                            type="button"
                            className="ea-chat-tool-chip"
                            aria-label={`查看工具调用详情：${tool.name || '工具调用'}`}
                            onClick={() => setActiveTool(tool)}
                        >
                            <CodeOutlined/>
                        </button>
                    </Tooltip>
                ))}
            </div>
            <ToolDetailDrawer tool={activeTool} onClose={() => setActiveTool(null)}/>
        </>
    );
};

/** 悬停提示：工具名 + 入参摘要，用于区分同名的多次调用 */
const tooltipTitle = (tool: ToolTrace): React.ReactNode => {
    const summary = summarizeToolInput(tool.inputParams);
    return (
        <div style={{fontSize: 12, lineHeight: 1.6}}>
            <div style={{fontWeight: 600}}>{tool.name || '工具调用'}</div>
            {summary && (
                <div style={{opacity: 0.85, fontFamily: 'ui-monospace, Menlo, monospace'}}>{summary}</div>
            )}
        </div>
    );
};

interface ToolDetailDrawerProps {
    tool: ToolTrace | null;
    onClose: () => void;
}

/** 工具详情：工具名、入参、执行结果；解析不出结构时展示原文 */
const ToolDetailDrawer: React.FC<ToolDetailDrawerProps> = ({tool, onClose}) => {
    const hasParsed = !!tool && (tool.name !== '' || tool.inputParams !== '' || tool.outputParams !== '');

    return (
        <Drawer
            title="工具调用详情"
            placement="right"
            width={560}
            open={!!tool}
            onClose={onClose}
            destroyOnHidden
        >
            {tool && (
                <div style={{display: 'flex', flexDirection: 'column', gap: 16}}>
                    {tool.name && (
                        <div>
                            <div className="ea-chat-trace-section-title">工具名称</div>
                            <Text strong style={{fontSize: 14}}>{tool.name}</Text>
                        </div>
                    )}
                    {tool.inputParams && (
                        <CodeBlock title="输入参数" content={formatJson(tool.inputParams)}/>
                    )}
                    {tool.outputParams && (
                        <CodeBlock title="执行结果" content={formatJson(tool.outputParams)}/>
                    )}
                    {!hasParsed && <CodeBlock title="原始内容" content={tool.raw}/>}
                </div>
            )}
        </Drawer>
    );
};

const CodeBlock: React.FC<{title: string; content: string}> = ({title, content}) => (
    <div>
        <div className="ea-chat-trace-section-title" style={{marginBottom: 6}}>{title}</div>
        <pre className="ea-chat-tool-code">{content}</pre>
    </div>
);

export default ToolCallGroup;
