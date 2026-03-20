import React, {useState} from 'react';
import {App, Layout, ConfigProvider, Splitter} from 'antd';
import {LeftOutlined} from '@ant-design/icons';
import {useLocation} from 'react-router-dom';
import {AgentConfigProvider, useAgentConfig} from './agent/AgentConfigContext';
import PromptInputPanel from './agent/prompt/PromptInputPanel';
import ResourceBindingPanel from './agent/ResourceBindingPanel';
import ChatDebugPanel from './agent/debug/ChatDebugPanel';


// 默认面板大小
const defaultSizes = ['33%', '33%', '33%'];

// 全局样式
const GlobalStyles = () => (
    <style>
        {`
      .breadcrumb-container {
        padding: 16px 24px 0 24px;
        background: #fff;
      }

      .splitter-panel {
        overflow: auto;
      }

      .agent-config-container {
        height: calc(100vh - 100px);
      }

      /* 覆盖 ant-layout 的默认 margin */
      .agent-config-layout.ant-layout,
      .agent-config-layout .ant-layout {
        margin: 0 !important;
      }
    `}
    </style>
);

// 内部组件，使用 AgentConfigContext
const AgentConfigContent: React.FC = () => {
    const location = useLocation();
    const { agentDetail } = useAgentConfig();

    // 获取 URL 参数中的 agentId
    const urlParams = new URLSearchParams(location.search);
    const agentId = urlParams.get('agentId');
    const agentIdNum = agentId ? parseInt(agentId) : undefined;

    // 获取 agent 信息
    const [sizes, setSizes] = useState<(number | string)[]>(defaultSizes);
    const [promptContent, setPromptContent] = useState('');

    // 从 Context 获取 agentName
    const agentName = agentDetail?.agentName || 'Agent 配置';
    
    // 处理双击重置面板大小
    const handleDoubleClick = () => {
        setSizes(defaultSizes);
    };
    
    // 处理提示词变化
    const handlePromptChange = (content: string) => {
        setPromptContent(content);
        // 这里可以添加保存逻辑
        console.log('提示词更新:', content);
    };

    return (
        <ConfigProvider getPopupContainer={() => document.body}>
            <App>
                <Layout
                    className="agent-config-layout"
                    style={{
                        height: '100vh',
                        width: '100%',
                        padding: '5px',
                        display: 'flex',
                        flexDirection: 'column',
                        background: '#e1ecf7',
                    }}>
                    <GlobalStyles/>

                    {/* 顶部后退按钮栏 */}
                    <div style={{
                        padding: '6px 6px',
                        background: '#fff',
                        borderBottom: '1px solid #f0f0f0',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        borderRadius: '5px',
                    }}>
                        <div style={{display: 'flex', alignItems: 'center', gap: '10px'}}>
                            <LeftOutlined/>返回
                            <span style={{fontSize: '16px', fontWeight: '500', color: '#333'}}>
                                {agentName || 'Agent 配置'}
                            </span>
                        </div>
                        {agentIdNum && (
                            <span style={{fontSize: '12px', color: '#999'}}>
                                Agent ID: {agentIdNum}
                            </span>
                        )}
                    </div>

                    <div style={{
                        flex: 1,
                        marginTop: '6px',
                        display: 'flex',
                        flexDirection: 'column',
                        maxHeight: 'calc(100vh - 64px)'
                    }}>
                        <Splitter
                            style={{
                                flex: 1,
                                boxShadow: '0 0 10px rgba(0, 0, 0, 0.1)',
                                borderRadius: '8px',
                                overflow: 'hidden'
                            }}
                            onResize={setSizes}
                            onDraggerDoubleClick={handleDoubleClick}
                        >
                            <Splitter.Panel
                                size={sizes[0]}
                                min="20%"
                                max="50%"
                                className="splitter-panel"
                            >
                                <PromptInputPanel
                                    agentId={agentIdNum}
                                    onPromptChange={handlePromptChange}
                                />
                            </Splitter.Panel>

                            <Splitter.Panel
                                size={sizes[1]}
                                min="30%"
                                max="60%"
                                className="splitter-panel"
                            >
                                <ResourceBindingPanel
                                    agentId={agentIdNum}
                                />
                            </Splitter.Panel>

                            <Splitter.Panel
                                size={sizes[2]}
                                min="20%"
                                max="50%"
                                className="splitter-panel"
                            >
                                <ChatDebugPanel
                                    agentId={agentIdNum}
                                />
                            </Splitter.Panel>
                        </Splitter>
                    </div>
                </Layout>
            </App>
        </ConfigProvider>
    );
};

// 主组件，提供 Context
const AgentConfig: React.FC = () => {
    const location = useLocation();

    // 获取 URL 参数中的 agentId
    const urlParams = new URLSearchParams(location.search);
    const agentId = urlParams.get('agentId');
    const agentIdNum = agentId ? parseInt(agentId) : undefined;

    return (
        <AgentConfigProvider initialAgentId={agentIdNum}>
            <AgentConfigContent />
        </AgentConfigProvider>
    );
};

export default AgentConfig;
