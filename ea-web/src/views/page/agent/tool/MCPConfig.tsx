import React, { useState, useEffect } from 'react';
import { Card, Form, Input, Button, Space, Divider, Row, Col, Alert, Switch, InputNumber, Tabs, Select, message } from 'antd';
import { eaToolApi } from '../../../api/EaToolApi';
import CommonTemplateConfig from './common/CommonTemplateConfig';
import DebugResult from './common/DebugResult';

const { TabPane } = Tabs;

interface MCPConfigProps {
  toolConfigs?: any[];
  agentId?: string | number;
  onRefresh?: () => void;
}

const MCPConfig: React.FC<MCPConfigProps> = ({ toolConfigs = [], agentId, onRefresh }) => {
  const [form] = Form.useForm();
  const [activeTab, setActiveTab] = useState('connection');
  const [connectionStatus, setConnectionStatus] = useState<'disconnected' | 'connecting' | 'connected' | 'failed'>('disconnected');
  const [testResult, setTestResult] = useState<{ success: boolean; message: string } | null>(null);
  const [executionResult, setExecutionResult] = useState<any>(null);  // 用于存储API原始响应
  const [executingCommand, setExecutingCommand] = useState<boolean>(false);  // 用于标记命令执行状态
  const [loading, setLoading] = useState(false);
  const [inputParams, setInputParams] = useState<any[]>([]);
  const [outputParams, setOutputParams] = useState<any[]>([]);
  const [mcpConfigData, setMcpConfigData] = useState<any>(null);

  // 表单提交处理 - 保存配置
  const handleSubmit = (values: any) => {
    if (!agentId) {
      console.error('Agent ID is required to save MCP config');
      return;
    }
    
    // 构建工具配置对象
    const toolConfig = {
      agentId: Number(agentId),
      toolType: 'MCP',
      toolInstanceName: values.toolInstanceName || 'MCP服务器',
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
          message.success('MCP配置已保存');
          console.log('MCP config saved successfully:', result);
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
          message.error('保存MCP配置失败: ' + errorMessage);
          console.error('Failed to save MCP config:', result);
        }
      })
      .catch((error) => {
        message.error('保存MCP配置出错');
        console.error('Error saving MCP config:', error);
      });
  };

  // 测试连接
  const handleTestConnection = async () => {
    try {
      const values = await form.validateFields(['host', 'port', 'username', 'password']);
      setConnectionStatus('connecting');
      setTestResult(null);
      setLoading(true);
      
      // 构建工具配置对象用于调试
      const toolConfig = {
        agentId: Number(agentId),
        toolType: 'MCP',
        toolInstanceName: 'MCP服务器',
        inputTemplate: JSON.stringify(inputParams),
        outTemplate: JSON.stringify(outputParams),
        ...values,
      };
      
      // 调用API进行调试测试
      eaToolApi.debug(toolConfig)
        .then((result) => {
          if (result.code === 200) {
            setConnectionStatus('connected');
            setTestResult({
              success: true,
              message: 'MCP服务器连接成功'
            });
            console.log('MCP connection test successful:', result);
          } else {
            setConnectionStatus('failed');
            setTestResult({
              success: false,
              message: result.message || '连接失败'
            });
          }
          setLoading(false);
        })
        .catch((error) => {
          setConnectionStatus('failed');
          setTestResult({
            success: false,
            message: '连接测试出错: ' + error.message
          });
          setLoading(false);
          console.error('MCP connection test error:', error);
        });
    } catch (error) {
      console.error('Validation failed:', error);
      setConnectionStatus('failed');
      setTestResult({
        success: false,
        message: '连接参数验证失败，请检查必填项'
      });
      setLoading(false);
    }
  };

  // 执行命令
  const handleExecuteCommand = async () => {
    try {
      const values = await form.validateFields(['command']);
      setExecutingCommand(true);
      
      // 构建工具配置对象用于调试
      const toolConfig = {
        agentId: Number(agentId),
        toolType: 'MCP',
        toolInstanceName: values.toolInstanceName || 'MCP服务器',
        inputTemplate: JSON.stringify(inputParams),
        outTemplate: JSON.stringify(outputParams),
        ...values,
      };
      
      // 调用API执行命令
      eaToolApi.debug(toolConfig)
        .then((result) => {
          // 记录API响应
          setExecutionResult(result);
          console.log('MCP command execution completed:', result);
          
          if (!(result.code === 200 || result.success === true)) {
            console.error('MCP command execution failed:', result.message || result.data);
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
          console.error('MCP command execution error:', error);
          setExecutingCommand(false);
        });
    } catch (error) {
      setExecutingCommand(false);
      console.error('Validation failed:', error);
    }
  };

  // 从工具配置中加载MCP配置数据
  useEffect(() => {
    if (toolConfigs && toolConfigs.length > 0) {
      const mcpConfig = toolConfigs.find(config => config.toolType === 'MCP');
      if (mcpConfig) {
        setMcpConfigData(mcpConfig);
        
        // 将配置数据设置到表单中
        form.setFieldsValue({
          toolInstanceName: mcpConfig.toolInstanceName || 'MCP服务器',
          host: mcpConfig.host,
          port: mcpConfig.port,
          username: mcpConfig.username,
          password: mcpConfig.password,
          useSSL: mcpConfig.useSSL,
          timeout: mcpConfig.timeout,
          description: mcpConfig.description,
          command: mcpConfig.command,
        });
        
        // 设置输入输出参数
        if (mcpConfig.inputTemplate) {
          try {
            setInputParams(JSON.parse(mcpConfig.inputTemplate));
          } catch (e) {
            console.error('Error parsing input template:', e);
          }
        }
        
        if (mcpConfig.outTemplate) {
          try {
            setOutputParams(JSON.parse(mcpConfig.outTemplate));
          } catch (e) {
            console.error('Error parsing output template:', e);
          }
        }
      }
    }
  }, [toolConfigs, form]);

  return (
    <Card title="MCP服务器配置" size="small">
      <Tabs defaultActiveKey="connection">
        <TabPane tab="连接配置" key="connection">
          <Form form={form} layout="vertical" onFinish={handleSubmit}>
            <Form.Item
              name="toolInstanceName"
              label="工具实例名称"
              rules={[{ required: true, message: '请输入工具实例名称' }]}
            >
              <Input placeholder="例如: MCP服务器" />
            </Form.Item>
            
            <Row gutter={16}>
              <Col span={12}>
                <Form.Item
                  name="host"
                  label="服务器地址"
                  rules={[{ required: true, message: '请输入MCP服务器地址' }]}
                >
                  <Input placeholder="例如: 192.168.1.100 或 mcp.example.com" />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item
                  name="port"
                  label="端口号"
                  rules={[{ required: true, message: '请输入MCP服务器端口号' }]}
                  initialValue={8080}
                >
                  <InputNumber min={1} max={65535} style={{ width: '100%' }} placeholder="例如: 8080" />
                </Form.Item>
              </Col>
            </Row>

            <Row gutter={16}>
              <Col span={12}>
                <Form.Item
                  name="username"
                  label="用户名"
                  rules={[{ required: true, message: '请输入用户名' }]}
                >
                  <Input placeholder="连接MCP服务器的用户名" />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item
                  name="password"
                  label="密码"
                  rules={[{ required: true, message: '请输入密码' }]}
                >
                  <Input.Password placeholder="连接MCP服务器的密码" />
                </Form.Item>
              </Col>
            </Row>

            <Divider orientation="left">连接设置</Divider>

            <Row gutter={16}>
              <Col span={12}>
                <Form.Item
                  name="useSSL"
                  label="安全连接(SSL)"
                  valuePropName="checked"
                  initialValue={false}
                >
                  <Switch />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item
                  name="timeout"
                  label="连接超时(秒)"
                  initialValue={30}
                >
                  <InputNumber min={1} max={300} style={{ width: '100%' }} placeholder="例如: 30" />
                </Form.Item>
              </Col>
            </Row>

            <Form.Item
              name="description"
              label="描述"
            >
              <Input.TextArea rows={3} placeholder="关于此MCP服务器配置的描述信息" />
            </Form.Item>

            <Form.Item>
              <Space>
                <Button 
                  type="primary" 
                  onClick={handleTestConnection}
                  loading={loading && activeTab === 'connection'}
                >
                  {loading && activeTab === 'connection' ? '测试中...' : '测试连接'}
                </Button>
                {connectionStatus === 'connected' && <span style={{ color: 'green' }}>连接成功</span>}
                {connectionStatus === 'failed' && <span style={{ color: 'red' }}>连接失败</span>}
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

        <TabPane tab="命令执行" key="execution">
          <Form form={form} layout="vertical">
            <Form.Item
              name="command"
              label="MCP命令"
              rules={[{ required: true, message: '请输入要执行的MCP命令' }]}
            >
              <Select
                placeholder="选择或输入MCP命令"
                mode="tags"
                tokenSeparators={[',']}
                options={[
                  { label: 'GET_DEVICES', value: 'GET_DEVICES' },
                  { label: 'GET_STATUS', value: 'GET_STATUS' },
                  { label: 'RESTART_DEVICE', value: 'RESTART_DEVICE' },
                  { label: 'UPDATE_CONFIG', value: 'UPDATE_CONFIG' },
                ]}
              >
              </Select>
            </Form.Item>
            
            <Form.Item>
              <Space>
                <Button 
                  type="primary" 
                  onClick={handleExecuteCommand}
                  disabled={connectionStatus !== 'connected'}
                  loading={executingCommand}
                >
                  执行命令
                </Button>
                <Button htmlType="button" onClick={() => form.resetFields(['command'])}>
                  清空
                </Button>
                {connectionStatus !== 'connected' && (
                  <span style={{ color: 'orange' }}>请先在"连接配置"中建立连接</span>
                )}
              </Space>
            </Form.Item>
          </Form>

          {/* 调试结果展示 */}
          <DebugResult debugResult={executionResult} loading={executingCommand} title="MCP命令执行结果" />
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

export default MCPConfig;