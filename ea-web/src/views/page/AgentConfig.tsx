import React, {useState} from 'react';
import {App, Layout, ConfigProvider, Splitter, message, Button} from 'antd';
import {LeftOutlined, EditOutlined} from '@ant-design/icons';
import {useLocation} from 'react-router-dom';
import {AgentConfigProvider, useAgentConfig} from './agent/AgentConfigContext';
import PromptInputPanel from './agent/prompt/PromptInputPanel';
import ResourceBindingPanel from './agent/ResourceBindingPanel';
import ChatDebugPanel from './agent/debug/ChatDebugPanel';
import {eaAgentApi} from '../api/EaAgentApi';
import AgentEditModal, {type ModelConfigField} from './agent/AgentEditModal';


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
    const {agentDetail, refreshResources} = useAgentConfig();

    // 获取 URL 参数中的 agentId
    const urlParams = new URLSearchParams(location.search);
    const agentId = urlParams.get('agentId');
    const agentIdNum = agentId ? parseInt(agentId) : undefined;

    // 获取 agent 信息
    const [sizes, setSizes] = useState<(number | string)[]>(defaultSizes);
    const [promptContent, setPromptContent] = useState('');

    // 编辑相关状态
    const [isEditModalOpen, setIsEditModalOpen] = useState(false);

    // 从 Context 获取 agentName
    const agentName = agentDetail?.agentName || 'Agent 配置';

    // 处理双击重置面板大小
    const handleDoubleClick = () => {
        setSizes(defaultSizes);
    };

    // 处理提示词变化（仅更新本地状态，不自动保存）
    const handlePromptChange = (content: string) => {
        setPromptContent(content);
    };

    // 打开编辑弹窗
    const handleEditClick = () => {
        if (!agentDetail) {
            message.warning('未获取到 Agent 信息');
            return;
        }
        setIsEditModalOpen(true);
    };

    // 提交编辑表单
    const handleEditSubmit = async (values: any, modelConfigFields: ModelConfigField[]) => {
        try {
            // 构建 modelConfig 对象
            const modelConfigObj: any = {};
            modelConfigFields.forEach(field => {
                if (field.fieldName && field.fieldValue) {
                    modelConfigObj[field.fieldName] = field.fieldValue;
                }
            });

            const agentData = {
                ...values,
                id: agentIdNum,
                modelPlatform: values.modelPlatform,
                analysisModel: values.modelPlatform,
                modelConfig: JSON.stringify(modelConfigObj),
            };

            await eaAgentApi.saveAgent(agentData);
            message.success('更新成功');
            setIsEditModalOpen(false);
            refreshResources();
        } catch (error) {
            console.error('保存失败:', error);
            message.error('保存失败');
        }
    };

    return (
        <ConfigProvider getPopupContainer={() => document.body}>
            <App>
                <Layout
                    className="agent-config-layout"
                    style={{
                        height: '100vh',
                        width: '100%',
                        padding: '0px',
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
                            <LeftOutlined style={{margin: '0px', padding: '0px'}}/>
                            {/* 这个地方展示一下 avatar */}
                            <span style={{fontSize: '16px', fontWeight: '500', color: '#333'}}>
                                {agentDetail?.avatar} {agentDetail?.agentName}
                            </span>
                            <Button
                                type="text"
                                size="small"
                                onClick={handleEditClick}
                                icon={<EditOutlined style={{fontSize: '14px'}}/>}
                                title="编辑 Agent"
                                style={{color: '#1890ff', padding: '2px 4px'}}
                            >
                            </Button>
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
                                collapsible
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
                                collapsible
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
                                collapsible={{start: true}}
                            >
                                <ChatDebugPanel
                                    agentId={agentIdNum}
                                />
                            </Splitter.Panel>
                        </Splitter>
                    </div>
                </Layout>
            </App>

            {/* 编辑 Agent 弹窗 - 使用复用组件 */}
            <AgentEditModal
                open={isEditModalOpen}
                editingId={agentIdNum}
                agentDetail={agentDetail}
                onOk={handleEditSubmit}
                onCancel={() => setIsEditModalOpen(false)}
            />
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
            <AgentConfigContent/>
        </AgentConfigProvider>
    );
};

export default AgentConfig;
