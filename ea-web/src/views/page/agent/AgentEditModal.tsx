import React, {useEffect, useState} from 'react';
import {Modal, Form, Input, Select, Row, Col} from 'antd';
import {EyeOutlined, EyeInvisibleOutlined} from '@ant-design/icons';

export interface ModelConfigField {
    fieldName: string;
    fieldValue: string;
    fieldLabel: string;
}

export interface AgentEditModalProps {
    open: boolean;
    editingId?: number;
    agentDetail?: any;
    analysisModels?: string[];
    modelVersions?: Map<string, string[]>;
    modelIcons?: Map<string, string>;
    onOk: (values: any, modelConfigFields: ModelConfigField[]) => void;
    onCancel: () => void;
}

const AgentEditModal: React.FC<AgentEditModalProps> = ({
    open,
    editingId,
    agentDetail,
    analysisModels,
    modelVersions,
    modelIcons,
    onOk,
    onCancel
}) => {
    const [form] = Form.useForm();
    const [modelConfigFields, setModelConfigFields] = useState<ModelConfigField[]>([
        {fieldName: 'apiKey', fieldValue: '', fieldLabel: 'API 密钥'},
        {fieldName: 'baseUrl', fieldValue: '', fieldLabel: '基础 URL'},
        {fieldName: 'modelVersion', fieldValue: '', fieldLabel: '模型版本'},
        {fieldName: 'completionsPath', fieldValue: '', fieldLabel: '补全路径'},
    ]);
    const [showApiKey, setShowApiKey] = useState(false);

    // 重置表单
    const resetForm = () => {
        form.resetFields();
        setModelConfigFields([
            {fieldName: 'apiKey', fieldValue: '', fieldLabel: 'API 密钥'},
            {fieldName: 'baseUrl', fieldValue: '', fieldLabel: '基础 URL'},
            {fieldName: 'modelVersion', fieldValue: '', fieldLabel: '模型版本'},
            {fieldName: 'completionsPath', fieldValue: '', fieldLabel: '补全路径'},
        ]);
        setShowApiKey(false);
    };

    // 当弹窗打开时，填充数据
    useEffect(() => {
        if (open && agentDetail) {
            // 解析 modelConfig
            let configObj: any = {};
            if (agentDetail.modelConfig) {
                try {
                    configObj = JSON.parse(agentDetail.modelConfig);
                } catch (e) {
                    console.error('解析 modelConfig 失败:', e);
                }
            }

            // 设置表单初始值
            form.setFieldsValue({
                agentName: agentDetail.agentName,
                modelPlatform: agentDetail.modelPlatform,
                toolRunMode: agentDetail.toolRunMode,
                avatar: agentDetail.avatar,
                agentDesc: agentDetail.agentDesc,
            });

            // 设置模型配置字段值
            const newModelConfigFields = modelConfigFields.map(field => ({
                ...field,
                fieldValue: configObj[field.fieldName] || ''
            }));
            setModelConfigFields(newModelConfigFields);
        }
    }, [open, agentDetail]);

    // 处理模型配置字段变化
    const handleModelConfigFieldChange = (fieldName: string, value: string) => {
        setModelConfigFields(prev => prev.map(field =>
            field.fieldName === fieldName ? {...field, fieldValue: value} : field
        ));
    };

    // 切换 API 密钥显示/隐藏
    const toggleShowApiKey = () => {
        setShowApiKey(!showApiKey);
    };

    // 格式化 API 密钥显示（脱敏）
    const formatApiKey = (key: string) => {
        if (!key) return '';
        const maskLength = Math.max(0, key.length - 4);
        const prefix = key.slice(0, 2);
        const suffix = key.slice(-2);
        return `${prefix}${'*'.repeat(maskLength)}${suffix}`;
    };

    // 处理提交
    const handleSubmit = async () => {
        try {
            const values = await form.validateFields();
            onOk(values, modelConfigFields);
        } catch (error) {
            console.error('表单验证失败:', error);
        }
    };

    return (
        <Modal
            title={editingId ? '编辑 Agent' : '添加 Agent'}
            open={open}
            onOk={handleSubmit}
            onCancel={() => {
                resetForm();
                onCancel();
            }}
            destroyOnClose
            width={1000}
        >
            <Form form={form} layout="vertical">
                <Row gutter={16}>
                    <Col span={12}>
                        <Form.Item
                            name="agentName"
                            label="Agent 名称"
                            rules={[{required: true, message: '请输入 Agent 名称'}]}
                        >
                            <Input placeholder="请输入 Agent 名称"/>
                        </Form.Item>
                    </Col>
                    <Col span={12}>
                        <Form.Item
                            name="modelPlatform"
                            label="模型平台"
                            rules={[{required: true, message: '请选择模型平台'}]}
                        >
                            <Select
                                placeholder="请选择模型平台"
                                showSearch
                                style={{width: '100%'}}
                                allowClear
                            >
                                {analysisModels?.map(model => (
                                    <Select.Option key={model} value={model}>
                                        <span style={{display: 'flex', alignItems: 'center', gap: '8px'}}>
                                            {modelIcons?.get(model) && (
                                                <img
                                                    src={modelIcons.get(model)}
                                                    alt={model}
                                                    style={{
                                                        width: '16px',
                                                        height: '16px',
                                                        objectFit: 'contain'
                                                    }}
                                                    onError={(e) => {
                                                        (e.target as HTMLImageElement).style.display = 'none';
                                                    }}
                                                />
                                            )}
                                            {model}
                                        </span>
                                    </Select.Option>
                                ))}
                            </Select>
                        </Form.Item>
                    </Col>
                </Row>

                <Row gutter={16}>
                    <Col span={12}>
                        <Form.Item
                            name="toolRunMode"
                            label="运行模式"
                            rules={[{required: true, message: '请选择运行模式'}]}
                        >
                            <Select placeholder="请选择运行模式">
                                <Select.Option value="ReAct">ReAct</Select.Option>
                                <Select.Option value="Tool">Tool</Select.Option>
                            </Select>
                        </Form.Item>
                    </Col>
                    <Col span={12}>
                        <Form.Item name="avatar" label="Agent 图标">
                            <Input
                                placeholder="请输入图标 Unicode 或 emoji，例如：🤖"
                                style={{width: '100%'}}
                            />
                        </Form.Item>
                    </Col>
                </Row>

                <Form.Item label="模型配置">
                    <div style={{border: '1px solid #e8e8e8', borderRadius: '4px', padding: '16px'}}>
                        <h4 style={{margin: '0 0 16px 0', fontWeight: 'bold'}}>模型配置参数</h4>

                        <Row gutter={16}>
                            <Col span={12}>
                                <Form.Item label="API 密钥">
                                    <Input
                                        value={showApiKey
                                            ? modelConfigFields.find(f => f.fieldName === 'apiKey')?.fieldValue
                                            : formatApiKey(modelConfigFields.find(f => f.fieldName === 'apiKey')?.fieldValue || '')}
                                        onChange={(e) => handleModelConfigFieldChange('apiKey', e.target.value)}
                                        placeholder="请输入 API 密钥"
                                        type={showApiKey ? 'text' : 'password'}
                                        suffix={
                                            <span onClick={toggleShowApiKey} style={{cursor: 'pointer'}}>
                                                {showApiKey ? <EyeInvisibleOutlined/> : <EyeOutlined/>}
                                            </span>
                                        }
                                    />
                                </Form.Item>
                            </Col>
                            <Col span={12}>
                                <Form.Item label="基础 URL">
                                    <Input
                                        value={modelConfigFields.find(f => f.fieldName === 'baseUrl')?.fieldValue}
                                        onChange={(e) => handleModelConfigFieldChange('baseUrl', e.target.value)}
                                        placeholder="请输入基础 URL"
                                    />
                                </Form.Item>
                            </Col>
                        </Row>

                        <Row gutter={16}>
                            <Col span={12}>
                                <Form.Item
                                    label="模型版本"
                                >
                                    <Select
                                        value={modelConfigFields.find(f => f.fieldName === 'modelVersion')?.fieldValue || ''}
                                        onChange={(value) => {
                                            handleModelConfigFieldChange('modelVersion', value);
                                        }}
                                        placeholder="请选择模型版本"
                                        style={{width: '100%'}}
                                        showSearch
                                        notFoundContent="暂无推荐版本，请输入自定义版本"
                                        options={(modelVersions?.get(form.getFieldValue('modelPlatform')) || []).map((version: string) => ({
                                            label: version,
                                            value: version
                                        }))}
                                    >
                                    </Select>
                                </Form.Item>
                            </Col>
                            <Col span={12}>
                                <Form.Item label="补全路径">
                                    <Input
                                        value={modelConfigFields.find(f => f.fieldName === 'completionsPath')?.fieldValue}
                                        onChange={(e) => handleModelConfigFieldChange('completionsPath', e.target.value)}
                                        placeholder="请输入补全路径"
                                    />
                                </Form.Item>
                            </Col>
                        </Row>
                    </div>
                </Form.Item>

                <Form.Item name="agentDesc" label="备注">
                    <Input.TextArea placeholder="请输入备注信息" rows={4}/>
                </Form.Item>
            </Form>
        </Modal>
    );
};

export default AgentEditModal;
