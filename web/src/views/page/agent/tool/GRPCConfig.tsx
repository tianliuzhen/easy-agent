import React, { useState, useEffect } from 'react';
import { Card, Form, Input, Button, Space, Divider, Row, Col, Alert, Switch, InputNumber, Tabs, Select, message, Table } from 'antd';
import { eaToolApi } from '../../../api/EaToolApi';
import CommonTemplateConfig from './common/CommonTemplateConfig';

const { TabPane } = Tabs;

interface GRPCConfigProps {
  toolConfigs?: any[];
  agentId?: string | number;
  onRefresh?: () => void;
}

const GRPCConfig: React.FC<GRPCConfigProps> = ({ toolConfigs = [], agentId, onRefresh }) => {
  const [form] = Form.useForm();
  const [activeTab, setActiveTab] = useState('connection');
  const [connectionStatus, setConnectionStatus] = useState<'disconnected' | 'connecting' | 'connected' | 'failed'>('disconnected');
  const [testResult, setTestResult] = useState<{ success: boolean; message: string } | null>(null);
  const [services, setServices] = useState<any[]>([]);
  const [methods, setMethods] = useState<any[]>([]);
  const [executionResult, setExecutionResult] = useState<any>(null);  // 用于存储API原始响应
  const [executingCall, setExecutingCall] = useState<boolean>(false);  // 用于标记gRPC调用状态
  const [loading, setLoading] = useState(false);
  const [inputParams, setInputParams] = useState<any[]>([]);
  const [outputParams, setOutputParams] = useState<any[]>([]);
  const [grpcConfigData, setGrpcConfigData] = useState<any>(null);

  // 表单提交处理 - 保存配置
  const handleSubmit = (values: any) => {
    if (!agentId) {
      console.error('Agent ID is required to save gRPC config');
      return;
    }
    
    // 构建工具配置对象
    const toolConfig = {
      agentId: Number(agentId),
      toolType: 'GRPC',
      toolInstanceName: values.toolInstanceName || 'gRPC工具',
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
          message.success('gRPC配置已保存');
          console.log('gRPC config saved successfully:', result);
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
          message.error('保存gRPC配置失败: ' + errorMessage);
          console.error('Failed to save gRPC config:', result);
        }
      })
      .catch((error) => {
        message.error('保存gRPC配置出错');
        console.error('Error saving gRPC config:', error);
      });
  };

  // 测试连接
  const handleTestConnection = async () => {
    try {
      const values = await form.validateFields(['host', 'port', 'useTLS']);
      setConnectionStatus('connecting');
      setTestResult(null);
      setLoading(true);
      
      // 构建工具配置对象用于调试
      const toolConfig = {
        agentId: Number(agentId),
        toolType: 'GRPC',
        toolInstanceName: 'gRPC工具',
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
              message: 'gRPC服务器连接成功'
            });
            
            // 模拟获取服务列表
            setServices([
              { name: 'UserService', fullName: 'com.example.UserService' },
              { name: 'OrderService', fullName: 'com.example.OrderService' },
              { name: 'ProductService', fullName: 'com.example.ProductService' }
            ]);
            
            console.log('gRPC connection test successful:', result);
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
          console.error('gRPC connection test error:', error);
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

  // 获取服务方法
  const handleServiceChange = (value: string) => {
    // 模拟获取方法列表
    const methodMap: Record<string, any[]> = {
      'UserService': [
        { name: 'GetUser', fullName: 'com.example.UserService.GetUser' },
        { name: 'CreateUser', fullName: 'com.example.UserService.CreateUser' },
        { name: 'UpdateUser', fullName: 'com.example.UserService.UpdateUser' }
      ],
      'OrderService': [
        { name: 'GetOrder', fullName: 'com.example.OrderService.GetOrder' },
        { name: 'CreateOrder', fullName: 'com.example.OrderService.CreateOrder' }
      ],
      'ProductService': [
        { name: 'GetProduct', fullName: 'com.example.ProductService.GetProduct' },
        { name: 'ListProducts', fullName: 'com.example.ProductService.ListProducts' }
      ]
    };
    
    setMethods(methodMap[value] || []);
    form.setFieldsValue({ method: undefined, request: '' });
  };

  // 生成示例请求
  const handleMethodChange = (value: string) => {
    // 模拟生成示例请求
    const requestExamples: Record<string, string> = {
      'GetUser': '{\n  "id": 12345\n}',
      'CreateUser': '{\n  "name": "John Doe",\n  "email": "john@example.com"\n}',
      'UpdateUser': '{\n  "id": 12345,\n  "name": "John Smith",\n  "email": "johnsmith@example.com"\n}',
      'GetOrder': '{\n  "orderId": "ORD-2023-001"\n}',
      'CreateOrder': '{\n  "productId": 1001,\n  "quantity": 2,\n  "customerId": 12345\n}',
      'GetProduct': '{\n  "productId": 1001\n}',
      'ListProducts': '{\n  "pageSize": 10,\n  "pageToken": ""\n}'
    };
    
    form.setFieldsValue({ request: requestExamples[value] || '{}' });
  };

  // 执行gRPC调用
  const handleExecuteCall = async () => {
    try {
      const values = await form.validateFields(['service', 'method', 'request']);
      setExecutingCall(true);
      
      // 构建工具配置对象用于调试
      const toolConfig = {
        agentId: Number(agentId),
        toolType: 'GRPC',
        toolInstanceName: values.toolInstanceName || 'gRPC工具',
        inputTemplate: JSON.stringify(inputParams),
        outTemplate: JSON.stringify(outputParams),
        ...values,
      };
      
      // 调用API执行gRPC调用
      eaToolApi.debug(toolConfig)
        .then((result) => {
          // 记录API响应
          setExecutionResult(result);
          console.log('gRPC call execution completed:', result);
          
          if (!(result.code === 200 || result.success === true)) {
            console.error('gRPC call execution failed:', result.message || result.data);
          }
          setExecutingCall(false);
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
          console.error('gRPC call execution error:', error);
          setExecutingCall(false);
        });
    } catch (error) {
      setExecutingCall(false);
      console.error('Validation failed:', error);
    }
  };

  // 从工具配置中加载gRPC配置数据
  useEffect(() => {
    if (toolConfigs && toolConfigs.length > 0) {
      const grpcConfig = toolConfigs.find(config => config.toolType === 'GRPC');
      if (grpcConfig) {
        setGrpcConfigData(grpcConfig);
        
        // 将配置数据设置到表单中
        form.setFieldsValue({
          toolInstanceName: grpcConfig.toolInstanceName || 'gRPC工具',
          host: grpcConfig.host,
          port: grpcConfig.port,
          useTLS: grpcConfig.useTLS,
          description: grpcConfig.description,
          service: grpcConfig.service,
          method: grpcConfig.method,
          request: grpcConfig.request,
        });
        
        // 设置输入输出参数
        if (grpcConfig.inputTemplate) {
          try {
            setInputParams(JSON.parse(grpcConfig.inputTemplate));
          } catch (e) {
            console.error('Error parsing input template:', e);
          }
        }
        
        if (grpcConfig.outTemplate) {
          try {
            setOutputParams(JSON.parse(grpcConfig.outTemplate));
          } catch (e) {
            console.error('Error parsing output template:', e);
          }
        }
      }
    }
  }, [toolConfigs, form]);

  return (
    <Card title="gRPC工具" size="small">
      <Tabs activeKey={activeTab} onChange={setActiveTab}>
        <TabPane tab="连接配置" key="connection">
          <Form form={form} layout="vertical" onFinish={handleSubmit}>
            <Form.Item
              name="toolInstanceName"
              label="工具实例名称"
              rules={[{ required: true, message: '请输入工具实例名称' }]}
            >
              <Input placeholder="例如: gRPC工具" />
            </Form.Item>
            
            <Row gutter={16}>
              <Col span={12}>
                <Form.Item
                  name="host"
                  label="服务器地址"
                  rules={[{ required: true, message: '请输入gRPC服务器地址' }]}
                >
                  <Input placeholder="例如: 192.168.1.100 或 grpc.example.com" />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item
                  name="port"
                  label="端口号"
                  rules={[{ required: true, message: '请输入gRPC服务器端口号' }]}
                  initialValue={50051}
                >
                  <InputNumber min={1} max={65535} style={{ width: '100%' }} placeholder="例如: 50051" />
                </Form.Item>
              </Col>
            </Row>

            <Row gutter={16}>
              <Col span={12}>
                <Form.Item
                  name="useTLS"
                  label="启用TLS"
                  valuePropName="checked"
                  initialValue={false}
                >
                  <Switch />
                </Form.Item>
              </Col>
            </Row>

            <Form.Item
              name="description"
              label="描述"
            >
              <Input.TextArea rows={3} placeholder="关于此gRPC服务器配置的描述信息" />
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

        <TabPane tab="服务调用" key="execution">
          <Form form={form} layout="vertical">
            <Row gutter={16}>
              <Col span={12}>
                <Form.Item
                  name="service"
                  label="服务"
                  rules={[{ required: true, message: '请选择服务' }]}
                >
                  <Select
                    placeholder="选择服务"
                    options={services.map(s => ({ label: s.name, value: s.name }))}
                    onChange={handleServiceChange}
                  />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item
                  name="method"
                  label="方法"
                  rules={[{ required: true, message: '请选择方法' }]}
                >
                  <Select
                    placeholder="选择方法"
                    options={methods.map(m => ({ label: m.name, value: m.name }))}
                    onChange={handleMethodChange}
                  />
                </Form.Item>
              </Col>
            </Row>

            <Form.Item
              name="request"
              label="请求数据 (JSON格式)"
              rules={[{ required: true, message: '请输入请求数据' }]}
            >
              <Input.TextArea rows={6} placeholder="输入请求数据，JSON格式" />
            </Form.Item>
            
            <Form.Item>
              <Space>
                <Button 
                  type="primary" 
                  onClick={handleExecuteCall}
                  disabled={connectionStatus !== 'connected'}
                  loading={executingCall}
                >
                  执行调用
                </Button>
                <Button htmlType="button" onClick={() => form.resetFields(['request'])}>
                  清空
                </Button>
                {connectionStatus !== 'connected' && (
                  <span style={{ color: 'orange' }}>请先在"连接配置"中建立连接</span>
                )}
              </Space>
            </Form.Item>
          </Form>

          {/* 调试结果展示 */}
          <DebugResult debugResult={executionResult} loading={executingCall} title="gRPC调用结果" />
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

export default GRPCConfig;