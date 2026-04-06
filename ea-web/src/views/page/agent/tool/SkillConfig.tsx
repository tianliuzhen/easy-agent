import React, { useState, useEffect } from 'react';
import { Card, Form, Input, Button, Space, Divider, Row, Col, Alert, Switch, InputNumber, Tabs, Select, message } from 'antd';
import { eaToolApi } from '../../../api/EaToolApi';
import CommonTemplateConfig from './common/CommonTemplateConfig';
import DebugResult from './common/DebugResult';

const { TabPane } = Tabs;
const { Option } = Select;
const { TextArea } = Input;

interface SkillConfigProps {
  toolConfigs?: any[];
  agentId?: string | number;
  onRefresh?: () => void;
}

const SkillConfig: React.FC<SkillConfigProps> = ({ toolConfigs = [], agentId, onRefresh }) => {
  const [form] = Form.useForm();
  const [activeTab, setActiveTab] = useState('basic');
  const [testResult, setTestResult] = useState<{ success: boolean; message: string } | null>(null);
  const [executionResult, setExecutionResult] = useState<any>(null);
  const [executingCommand, setExecutingCommand] = useState<boolean>(false);
  const [loading, setLoading] = useState(false);
  const [inputParams, setInputParams] = useState<any[]>([]);
  const [outputParams, setOutputParams] = useState<any[]>([]);
  const [skillConfigData, setSkillConfigData] = useState<any>(null);

  // 表单提交处理 - 保存配置
  const handleSubmit = (values: any) => {
    if (!agentId) {
      console.error('Agent ID is required to save Skill config');
      return;
    }

    // 构建工具配置对象
    const toolConfig = {
      agentId: Number(agentId),
      toolType: 'SKILL',
      toolInstanceName: values.skillDisplayName || values.skillName || 'Skill技能',
      inputTemplate: JSON.stringify(inputParams),
      outTemplate: JSON.stringify(outputParams),
      isRequired: false,
      isActive: true,
      ...values,
    };

    // 调用API保存配置
    eaToolApi.addTool(toolConfig)
      .then((result) => {
        if (result.code === 200 || result.success === true) {
          message.success('Skill 配置已保存');
          console.log('Skill config saved successfully:', result);
          // 检查API返回结果中是否包含新创建工具的ID
          if (result.data && result.data.id) {
            console.log('新创建的工具ID:', result.data.id);
          }
          // 调用父组件传递的刷新函数来更新工具列表
          if (onRefresh) {
            onRefresh();
          }
        } else {
          const errorMessage = result.message || result.msg || 'Unknown error';
          message.error('保存 Skill 配置失败: ' + errorMessage);
          console.error('Failed to save Skill config:', result);
        }
      })
      .catch((error) => {
        message.error('保存 Skill 配置出错');
        console.error('Error saving Skill config:', error);
      });
  };

  // 测试技能
  const handleTestSkill = async () => {
    try {
      const values = await form.validateFields(['skillName', 'skillConfig']);
      setLoading(true);
      setTestResult(null);

      // 构建工具配置对象用于调试
      const toolConfig = {
        agentId: Number(agentId),
        toolType: 'SKILL',
        toolInstanceName: values.skillDisplayName || values.skillName || 'Skill技能',
        inputTemplate: JSON.stringify(inputParams),
        outTemplate: JSON.stringify(outputParams),
        ...values,
      };

      // 调用API进行调试测试
      eaToolApi.debug(toolConfig)
        .then((result) => {
          if (result.code === 200) {
            setTestResult({
              success: true,
              message: 'Skill 测试成功'
            });
            console.log('Skill test successful:', result);
          } else {
            setTestResult({
              success: false,
              message: result.message || '测试失败'
            });
          }
          setLoading(false);
        })
        .catch((error) => {
          setTestResult({
            success: false,
            message: '测试出错: ' + error.message
          });
          setLoading(false);
          console.error('Skill test error:', error);
        });
    } catch (error) {
      console.error('Validation failed:', error);
      setTestResult({
        success: false,
        message: '参数验证失败，请检查必填项'
      });
      setLoading(false);
    }
  };

  // 执行技能
  const handleExecuteSkill = async () => {
    try {
      const values = await form.validateFields();
      setExecutingCommand(true);

      // 构建工具配置对象用于调试
      const toolConfig = {
        agentId: Number(agentId),
        toolType: 'SKILL',
        toolInstanceName: values.skillDisplayName || values.skillName || 'Skill技能',
        inputTemplate: JSON.stringify(inputParams),
        outTemplate: JSON.stringify(outputParams),
        ...values,
      };

      // 调用API执行技能
      eaToolApi.debug(toolConfig)
        .then((result) => {
          // 记录API响应
          setExecutionResult(result);
          console.log('Skill execution completed:', result);

          if (!(result.code === 200 || result.success === true)) {
            console.error('Skill execution failed:', result.message || result.data);
          }
          setExecutingCommand(false);
        })
        .catch((error) => {
          // 处理网络错误或其他异常
          const fullResult = {
            success: false,
            data: error.message || '请求失败',
            code: 500,
            message: error.message || '请求失败',
          };
          setExecutionResult(fullResult);
          console.error('Skill execution error:', error);
          setExecutingCommand(false);
        });
    } catch (error) {
      setExecutingCommand(false);
      console.error('Validation failed:', error);
    }
  };

  // 从工具配置中加载 Skill 配置数据
  useEffect(() => {
    if (toolConfigs && toolConfigs.length > 0) {
      const skillConfig = toolConfigs.find(config => config.toolType === 'SKILL');
      if (skillConfig) {
        setSkillConfigData(skillConfig);

        // 将配置数据设置到表单中
        form.setFieldsValue({
          skillName: skillConfig.skillName,
          skillDisplayName: skillConfig.skillDisplayName,
          skillDescription: skillConfig.skillDescription,
          skillType: skillConfig.skillType || 'INTERNAL',
          skillCategory: skillConfig.skillCategory || 'general',
          skillIcon: skillConfig.skillIcon,
          skillVersion: skillConfig.skillVersion || '1.0.0',
          skillProvider: skillConfig.skillProvider || 'System',
          executionMode: skillConfig.executionMode || 'sync',
          timeout: skillConfig.timeout || 30,
          maxRetries: skillConfig.maxRetries || 3,
          skillConfig: skillConfig.skillConfig,
        });

        // 设置输入输出参数
        if (skillConfig.inputTemplate) {
          try {
            setInputParams(JSON.parse(skillConfig.inputTemplate));
          } catch (e) {
            console.error('Error parsing input template:', e);
          }
        }

        if (skillConfig.outTemplate) {
          try {
            setOutputParams(JSON.parse(skillConfig.outTemplate));
          } catch (e) {
            console.error('Error parsing output template:', e);
          }
        }
      }
    }
  }, [toolConfigs, form]);

  return (
    <Card title="Skill 技能配置" size="small">
      <Tabs defaultActiveKey="basic">
        <TabPane tab="基础配置" key="basic">
          <Form form={form} layout="vertical" onFinish={handleSubmit}>
            <Form.Item
              name="skillName"
              label="技能名称"
              rules={[{ required: true, message: '请输入技能名称' }]}
            >
              <Input placeholder="例如: code_review" />
            </Form.Item>

            <Form.Item
              name="skillDisplayName"
              label="技能显示名称"
              rules={[{ required: true, message: '请输入技能显示名称' }]}
            >
              <Input placeholder="例如: 代码审查" />
            </Form.Item>

            <Form.Item
              name="skillDescription"
              label="技能描述"
            >
              <TextArea rows={3} placeholder="描述这个技能的功能..." />
            </Form.Item>

            <Row gutter={16}>
              <Col span={12}>
                <Form.Item
                  name="skillType"
                  label="技能类型"
                  initialValue="INTERNAL"
                >
                  <Select>
                    <Option value="INTERNAL">内置技能</Option>
                    <Option value="EXTERNAL">外部技能</Option>
                    <Option value="PLUGIN">插件技能</Option>
                  </Select>
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item
                  name="skillCategory"
                  label="技能分类"
                  initialValue="general"
                >
                  <Select>
                    <Option value="general">通用</Option>
                    <Option value="development">开发</Option>
                    <Option value="data">数据</Option>
                    <Option value="media">媒体</Option>
                  </Select>
                </Form.Item>
              </Col>
            </Row>

            <Row gutter={16}>
              <Col span={12}>
                <Form.Item
                  name="skillIcon"
                  label="技能图标"
                >
                  <Input placeholder="例如: 🔧 或图标URL" />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item
                  name="skillVersion"
                  label="版本号"
                  initialValue="1.0.0"
                >
                  <Input placeholder="例如: 1.0.0" />
                </Form.Item>
              </Col>
            </Row>

            <Form.Item
              name="skillProvider"
              label="技能提供者"
              initialValue="System"
            >
              <Input placeholder="例如: System" />
            </Form.Item>
          </Form>
        </TabPane>

        <TabPane tab="执行配置" key="execution">
          <Form form={form} layout="vertical">
            <Row gutter={16}>
              <Col span={12}>
                <Form.Item
                  name="executionMode"
                  label="执行模式"
                  initialValue="sync"
                >
                  <Select>
                    <Option value="sync">同步</Option>
                    <Option value="async">异步</Option>
                  </Select>
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item
                  name="timeout"
                  label="超时时间(秒)"
                  initialValue={30}
                >
                  <InputNumber min={1} max={300} style={{ width: '100%' }} />
                </Form.Item>
              </Col>
            </Row>

            <Row gutter={16}>
              <Col span={12}>
                <Form.Item
                  name="maxRetries"
                  label="最大重试次数"
                  initialValue={3}
                >
                  <InputNumber min={0} max={10} style={{ width: '100%' }} />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item
                  name="isActive"
                  label="启用状态"
                  valuePropName="checked"
                  initialValue={true}
                >
                  <Switch />
                </Form.Item>
              </Col>
            </Row>

            <Form.Item
              name="skillConfig"
              label="技能配置(JSON)"
            >
              <TextArea
                rows={6}
                placeholder={`{
  "envVars": ["KEY=value"],
  "customParam": "value"
}`}
              />
            </Form.Item>

            <Form.Item>
              <Space>
                <Button
                  type="primary"
                  onClick={handleTestSkill}
                  loading={loading && activeTab === 'execution'}
                >
                  {loading && activeTab === 'execution' ? '测试中...' : '测试技能'}
                </Button>
              </Space>
            </Form.Item>

            {testResult && (
              <Alert
                message={testResult.success ? "成功" : "错误"}
                description={testResult.message}
                type={testResult.success ? "success" : "error"}
                showIcon
                closable
                onClose={() => setTestResult(null)}
              />
            )}
          </Form>
        </TabPane>

        <TabPane tab="通用模板配置" key="template">
          <CommonTemplateConfig
            inputParams={inputParams}
            outputParams={outputParams}
            onInputParamsChange={setInputParams}
            onOutputParamsChange={setOutputParams}
          />
        </TabPane>
      </Tabs>

      <Divider dashed />

      <Form.Item>
        <Space>
          <Button type="primary" onClick={() => form.submit()}>
            保存配置
          </Button>
          <Button htmlType="button" onClick={() => form.resetFields()}>
            重置
          </Button>
        </Space>
      </Form.Item>
    </Card>
  );
};

export default SkillConfig;