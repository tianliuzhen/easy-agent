import React, { useState, useEffect } from 'react';
import { Modal, Form, Input, Select, Button, Upload, message, Tabs } from 'antd';
import { UploadOutlined, PlusOutlined } from '@ant-design/icons';

const { TextArea } = Input;
const { Option } = Select;
const { TabPane } = Tabs;

interface AddResourceModalProps {
    visible: boolean;
    type: 'knowledge' | 'tool' | 'mcp';
    agentId?: number;
    onClose: () => void;
    onSuccess: (type: 'knowledge' | 'tool' | 'mcp') => void;
}

const AddResourceModal: React.FC<AddResourceModalProps> = ({
    visible,
    type,
    agentId,
    onClose,
    onSuccess
}) => {
    const [form] = Form.useForm();
    const [loading, setLoading] = useState(false);
    const [activeTab, setActiveTab] = useState('basic');

    // 重置表单
    useEffect(() => {
        if (visible) {
            form.resetFields();
            setActiveTab('basic');
        }
    }, [visible, type, form]);

    // 获取模态框标题
    const getModalTitle = () => {
        const typeNames = {
            'knowledge': '知识库',
            'tool': '工具',
            'mcp': 'MCP Skill'
        };
        return `添加${typeNames[type]}`;
    };

    // 获取表单配置
    const getFormConfig = () => {
        const baseFields = [
            {
                name: 'name',
                label: '名称',
                rules: [{ required: true, message: '请输入名称' }],
                component: <Input placeholder={`请输入${getModalTitle()}名称`} />
            },
            {
                name: 'description',
                label: '描述',
                rules: [{ required: true, message: '请输入描述' }],
                component: <TextArea rows={3} placeholder="请输入详细描述" />
            }
        ];

        switch (type) {
            case 'knowledge':
                return [
                    ...baseFields,
                    {
                        name: 'type',
                        label: '类型',
                        rules: [{ required: true, message: '请选择类型' }],
                        component: (
                            <Select placeholder="请选择知识库类型">
                                <Option value="文档">文档</Option>
                                <Option value="问答">问答</Option>
                                <Option value="技术文档">技术文档</Option>
                                <Option value="销售资料">销售资料</Option>
                                <Option value="其他">其他</Option>
                            </Select>
                        )
                    },
                    {
                        name: 'files',
                        label: '上传文件',
                        component: (
                            <Upload
                                multiple
                                beforeUpload={() => false}
                                maxCount={10}
                            >
                                <Button icon={<UploadOutlined />}>选择文件</Button>
                            </Upload>
                        )
                    }
                ];

            case 'tool':
                return [
                    ...baseFields,
                    {
                        name: 'toolType',
                        label: '工具类型',
                        rules: [{ required: true, message: '请选择工具类型' }],
                        component: (
                            <Select placeholder="请选择工具类型">
                                <Option value="SQL">SQL执行器</Option>
                                <Option value="HTTP">HTTP请求</Option>
                                <Option value="MCP">MCP服务器</Option>
                                <Option value="GRPC">gRPC工具</Option>
                                <Option value="其他">其他</Option>
                            </Select>
                        )
                    },
                    {
                        name: 'endpoint',
                        label: '接口地址',
                        component: <Input placeholder="请输入接口地址（可选）" />
                    },
                    {
                        name: 'config',
                        label: '配置参数',
                        component: <TextArea rows={4} placeholder="请输入JSON格式的配置参数（可选）" />
                    }
                ];

            case 'mcp':
                return [
                    ...baseFields,
                    {
                        name: 'provider',
                        label: '提供商',
                        rules: [{ required: true, message: '请输入提供商' }],
                        component: <Input placeholder="例如：Anthropic、GitHub等" />
                    },
                    {
                        name: 'version',
                        label: '版本',
                        component: <Input placeholder="例如：1.0.0" />
                    },
                    {
                        name: 'capabilities',
                        label: '能力',
                        component: (
                            <Select
                                mode="tags"
                                placeholder="输入能力后按回车添加"
                                style={{ width: '100%' }}
                            />
                        )
                    }
                ];

            default:
                return baseFields;
        }
    };

    // 处理提交
    const handleSubmit = async () => {
        try {
            const values = await form.validateFields();
            setLoading(true);

            // 这里可以调用API添加资源
            // await resourceApi.addResource({
            //     ...values,
            //     agentId,
            //     type
            // });

            // 模拟API调用
            await new Promise(resolve => setTimeout(resolve, 1000));

            message.success(`${getModalTitle()}添加成功`);
            onSuccess(type);
            onClose();
        } catch (error) {
            console.error('添加资源失败:', error);
            if (error instanceof Error) {
                message.error(`添加失败: ${error.message}`);
            }
        } finally {
            setLoading(false);
        }
    };

    // 渲染表单字段
    const renderFormFields = () => {
        const formConfig = getFormConfig();
        return formConfig.map((field, index) => (
            <Form.Item
                key={index}
                name={field.name}
                label={field.label}
                rules={field.rules}
            >
                {field.component}
            </Form.Item>
        ));
    };

    // 渲染高级配置
    const renderAdvancedConfig = () => {
        switch (type) {
            case 'knowledge':
                return (
                    <div>
                        <Form.Item
                            name="indexMethod"
                            label="索引方法"
                        >
                            <Select placeholder="请选择索引方法">
                                <Option value="fulltext">全文索引</Option>
                                <Option value="vector">向量索引</Option>
                                <Option value="hybrid">混合索引</Option>
                            </Select>
                        </Form.Item>
                        <Form.Item
                            name="chunkSize"
                            label="分块大小"
                        >
                            <Input placeholder="例如：1000（字符）" />
                        </Form.Item>
                        <Form.Item
                            name="tags"
                            label="标签"
                        >
                            <Select
                                mode="tags"
                                placeholder="输入标签后按回车添加"
                                style={{ width: '100%' }}
                            />
                        </Form.Item>
                    </div>
                );

            case 'tool':
                return (
                    <div>
                        <Form.Item
                            name="timeout"
                            label="超时时间（秒）"
                        >
                            <Input placeholder="例如：30" />
                        </Form.Item>
                        <Form.Item
                            name="retryCount"
                            label="重试次数"
                        >
                            <Input placeholder="例如：3" />
                        </Form.Item>
                        <Form.Item
                            name="authType"
                            label="认证类型"
                        >
                            <Select placeholder="请选择认证类型">
                                <Option value="none">无认证</Option>
                                <Option value="apiKey">API Key</Option>
                                <Option value="bearer">Bearer Token</Option>
                                <Option value="basic">Basic Auth</Option>
                            </Select>
                        </Form.Item>
                    </div>
                );

            case 'mcp':
                return (
                    <div>
                        <Form.Item
                            name="serverUrl"
                            label="服务器地址"
                        >
                            <Input placeholder="例如：http://localhost:8000" />
                        </Form.Item>
                        <Form.Item
                            name="authToken"
                            label="认证令牌"
                        >
                            <Input.Password placeholder="请输入认证令牌" />
                        </Form.Item>
                        <Form.Item
                            name="maxConcurrent"
                            label="最大并发数"
                        >
                            <Input placeholder="例如：5" />
                        </Form.Item>
                    </div>
                );

            default:
                return null;
        }
    };

    return (
        <Modal
            title={getModalTitle()}
            open={visible}
            onCancel={onClose}
            footer={[
                <Button key="cancel" onClick={onClose}>
                    取消
                </Button>,
                <Button
                    key="submit"
                    type="primary"
                    loading={loading}
                    onClick={handleSubmit}
                    icon={<PlusOutlined />}
                >
                    添加
                </Button>
            ]}
            width={600}
            destroyOnClose
        >
            <Tabs
                activeKey={activeTab}
                onChange={setActiveTab}
                size="small"
            >
                <TabPane tab="基本配置" key="basic">
                    <Form
                        form={form}
                        layout="vertical"
                        style={{ marginTop: '16px' }}
                    >
                        {renderFormFields()}
                    </Form>
                </TabPane>
                <TabPane tab="高级配置" key="advanced">
                    <Form
                        form={form}
                        layout="vertical"
                        style={{ marginTop: '16px' }}
                    >
                        {renderAdvancedConfig()}
                    </Form>
                </TabPane>
            </Tabs>

            <div style={{
                marginTop: '16px',
                padding: '12px',
                background: '#f6f8fa',
                borderRadius: '6px',
                fontSize: '12px',
                color: '#586069'
            }}>
                <div style={{ fontWeight: 'bold', marginBottom: '4px' }}>提示：</div>
                <ul style={{ margin: 0, paddingLeft: '16px' }}>
                    <li>添加后需要保存配置才能生效</li>
                    <li>可以在高级配置中设置更多参数</li>
                    <li>添加的资源需要经过测试确保可用</li>
                </ul>
            </div>
        </Modal>
    );
};

export default AddResourceModal;