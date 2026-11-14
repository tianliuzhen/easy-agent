import React, {useState} from 'react';
import {Drawer, Typography} from 'antd';
import {CodeOutlined, RightOutlined} from '@ant-design/icons';
import {formatJson, type ToolTrace} from './traceModel';
import './chatMessage.css';

const {Text} = Typography;

interface ToolCallListProps {
    tools: ToolTrace[];
}

/** 工具调用列表：每次调用一行，点击打开详情抽屉 */
const ToolCallList: React.FC<ToolCallListProps> = ({tools}) => {
    const [activeTool, setActiveTool] = useState<ToolTrace | null>(null);

    if (tools.length === 0) return null;

    return (
        <>
            {tools.map((tool) => (
                <button
                    key={tool.key}
                    type="button"
                    className="ea-chat-tool"
                    onClick={() => setActiveTool(tool)}
                >
                    <CodeOutlined className="ea-chat-tool-icon"/>
                    <span className="ea-chat-tool-name">{tool.name || '工具调用'}</span>
                    <span className="ea-chat-tool-hint">
                        详情 <RightOutlined style={{fontSize: 9}}/>
                    </span>
                </button>
            ))}
            <ToolDetailDrawer tool={activeTool} onClose={() => setActiveTool(null)}/>
        </>
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

export default ToolCallList;
