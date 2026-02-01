import React, { useState, useEffect } from 'react';
import { Card, Form, Input, Button, Space, Divider, Select, Row, Col, Alert, Table, Tabs, message } from 'antd';
import { eaToolApi } from '../../../api/EaToolApi';
import CommonTemplateConfig from './common/CommonTemplateConfig';
import DebugResult from './common/DebugResult';

const { TabPane } = Tabs;

interface SQLConfigProps {
  toolConfigs?: any[];
  agentId?: string | number;
  onRefresh?: () => void;
}

// 定义SQL连接配置的类型
interface SQLConnectionConfig {
  dialect: string;
  host: string;
  port: string;
  database: string;
  username: string;
  password: string;
  maxRows?: number;
  timeout?: number;
}

// 定义模板参数的类型
interface TemplateParam {
  name: string;
  type: string;
  description: string;
  required: boolean;
  defaultValue?: string;
}

import { useLocation } from 'react-router-dom';

const SQLConfig: React.FC<SQLConfigProps> = ({ toolConfigs = [], agentId, onRefresh }) => {
  const location = useLocation();
  
  // 从URL参数中获取工具ID
  const urlParams = new URLSearchParams(location.search);
  const toolIdFromUrl = urlParams.get('toolId');
  const [form] = Form.useForm();
  const [connectionStatus, setConnectionStatus] = useState<'disconnected' | 'connecting' | 'connected' | 'failed'>('disconnected');
  const [executionResult, setExecutionResult] = useState<any>(null);  // 用于存储API原始响应
  const [executingSql, setExecutingSql] = useState<boolean>(false);  // 用于标记SQL执行状态
  const [loading, setLoading] = useState(false);
  const [inputParams, setInputParams] = useState<any[]>([]);
  const [outputParams, setOutputParams] = useState<any[]>([]);
  const [sqlConfigData, setSqlConfigData] = useState<any>(null);
  
  // 表单初始值状态，用于动态设置表单初始值
  const [formInitialValues, setFormInitialValues] = useState({
    dialect: 'mysql',
    host: 'localhost',
    port: '3306',
    database: 'test',
    username: 'root',
    password: 'password',
    maxRows: 1000,
    timeout: 30,
    sql: 'SELECT * FROM users WHERE id = ${id};',
  });
  
  // 监听表单值变化，确保保存时获取最新值
  const [formValues, setFormValues] = useState<any>({});
  
  // 监听表单值变化
  const onFormValuesChange = (changedValues: any, allValues: any) => {
    setFormValues(prev => ({
      ...prev,
      ...allValues,
    }));
  };

  // 表单提交处理 - 保存配置
  const handleSubmit = async (values: any) => {
    if (!agentId) {
      console.error('Agent ID is required to save SQL config');
      return;
    }
    
    // 使用实时表单值，确保获取到最新的字段值
    const currentFormValues = {
      ...formInitialValues,
      ...formValues,
    };
    
    // 将连接配置信息和SQL语句存储到toolValue中
    const connectionConfig: SQLConnectionConfig = {
      dialect: currentFormValues.dialect,
      host: currentFormValues.host,
      port: currentFormValues.port,
      database: currentFormValues.database,
      username: currentFormValues.username,
      password: currentFormValues.password,
      maxRows: currentFormValues.maxRows,
      timeout: currentFormValues.timeout,
      sql: currentFormValues.sql, // 将SQL语句也存储在toolValue中
    };
    
    // 构建工具配置对象
    const toolConfig = {
      agentId: Number(agentId),
      toolType: 'SQL',
      toolInstanceName: currentFormValues.toolInstanceName || 'SQL执行器',
      toolInstanceDesc: currentFormValues.toolInstanceDesc || '',
      inputTemplate: JSON.stringify(inputParams),
      outTemplate: JSON.stringify(outputParams),
      isRequired: false,
      isActive: true,
      toolValue: JSON.stringify(connectionConfig), // 将连接配置和SQL语句存储在toolValue字段中
    };
    
    // 如果当前工具配置有ID，说明是更新操作，将ID添加到工具配置中
    if (sqlConfigData && sqlConfigData.id) {
      toolConfig.id = sqlConfigData.id;
    }
    
    // 调用API保存配置
    eaToolApi.addTool(toolConfig)
      .then((result) => {
        if (result.success === true || result.code === 200) {
          console.log('SQL config saved successfully:', result);
          // 检查API返回结果中是否包含新创建工具的ID
          if (result.data && result.data.id) {
            console.log('新创建的工具ID:', result.data.id);
          }
          message.success('SQL配置保存成功');
          // 调用父组件传递的刷新函数来更新工具列表
          if (onRefresh) {
            onRefresh();
          }
        } else {
          console.error('Failed to save SQL config:', result);
          const errorMessage = result.message || result.data || result.msg || 'Unknown error';
          message.error('SQL配置保存失败: ' + errorMessage);
        }
      })
      .catch((error) => {
        console.error('Error saving SQL config:', error);
        message.error('保存SQL配置时发生错误: ' + error.message);
      });
  };

  // 测试连接
  const handleTestConnection = async () => {
    try {
      const values = await form.validateFields(['dialect', 'host', 'port', 'database', 'username', 'password']);
      setConnectionStatus('connecting');
      
      // 将连接配置信息存储到toolValue中用于调试
      const connectionConfig: SQLConnectionConfig = {
        dialect: values.dialect,
        host: values.host,
        port: values.port,
        database: values.database,
        username: values.username,
        password: values.password,
        maxRows: values.maxRows || 1000,
        timeout: values.timeout || 30,
      };
      
      // 构建工具配置对象用于调试
      const toolConfig = {
        agentId: Number(agentId),
        toolType: 'SQL',
        toolInstanceName: formValues.toolInstanceName || 'SQL执行器',
        toolInstanceDesc: formValues.toolInstanceDesc || '',
        inputTemplate: JSON.stringify(inputParams),
        outTemplate: JSON.stringify(outputParams),
        toolValue: JSON.stringify(connectionConfig), // 将连接配置存储在toolValue字段中
      };
      
      // 调用API进行调试测试
      eaToolApi.debug(toolConfig)
        .then((result) => {
          if (result.success === true || result.code === 200) {
            setConnectionStatus('connected');
            message.success('连接测试成功');
            console.log('Connection test successful:', result);
          } else {
            setConnectionStatus('failed');
            const errorMsg = result.message || result.data || '连接测试失败';
            message.error('连接测试失败: ' + errorMsg);
            console.error('Connection test failed:', errorMsg);
          }
        })
        .catch((error) => {
          setConnectionStatus('failed');
          message.error('连接测试失败: ' + error.message);
          console.error('Connection test error:', error);
        });
    } catch (error) {
      console.error('Validation failed:', error);
      message.error('请填写完整的连接信息');
    }
  };

  // 执行SQL
  const handleExecuteSQL = async () => {
    try {
      const values = await form.validateFields(['sql']);
      setExecutingSql(true);
      
      // 使用实时表单值，确保获取到最新的字段值
      const currentFormValues = {
        ...formInitialValues,
        ...formValues,
      };
      
      // 从表单获取当前连接配置
      const connectionConfig: SQLConnectionConfig = {
        dialect: currentFormValues.dialect,
        host: currentFormValues.host,
        port: currentFormValues.port,
        database: currentFormValues.database,
        username: currentFormValues.username,
        password: currentFormValues.password,
        maxRows: currentFormValues.maxRows || 1000,
        timeout: currentFormValues.timeout || 30,
      };
      
      // 将SQL语句添加到连接配置中
      const configWithSql = {
        ...connectionConfig,
        sql: values.sql, // 将SQL语句也包含在toolValue中
      };
      
      // 构建执行SQL的工具配置对象
      const toolConfig = {
        agentId: Number(agentId),
        toolType: 'SQL',
        toolInstanceName: formValues.toolInstanceName || 'SQL执行器',
        toolInstanceDesc: formValues.toolInstanceDesc || '',
        inputTemplate: JSON.stringify(inputParams),
        outTemplate: JSON.stringify(outputParams),
        toolValue: JSON.stringify(configWithSql), // 包含连接配置和SQL语句
      };
      
      // 记录请求开始时间
      const startTime = Date.now();
      
      // 调用API进行调试
      eaToolApi.debug(toolConfig)
        .then((result) => {
          // 计算请求耗时
          const endTime = Date.now();
          const duration = endTime - startTime;
          
          // 将原始API响应和计算的时间添加到结果中
          const fullResult = {
            ...result,
            time: duration
          };
          
          setExecutionResult(fullResult);
          
          if (result.success === true || result.code === 200) {
            message.success('SQL执行成功');
            console.log('SQL执行成功:', result);
          } else {
            const errorMsg = result.message || result.data || 'SQL执行失败';
            message.error(errorMsg);
            console.error('SQL执行失败:', errorMsg);
          }
          setExecutingSql(false);
        })
        .catch((error) => {
          setExecutingSql(false);
          console.error('SQL执行错误:', error);
          
          // 处理网络错误等异常情况
          const endTime = Date.now();
          const duration = endTime - startTime;
          
          const fullResult = {
            success: false,
            data: error.message || '网络错误或服务器无响应',
            code: 500,
            message: error.message || '网络错误或服务器无响应',
            time: duration,
          };
          
          setExecutionResult(fullResult);
          message.error('SQL执行出错: ' + (error.message || '未知错误'));
        });
    } catch (error) {
      setExecutingSql(false);
      console.error('Validation failed:', error);
      message.error('请填写SQL语句');
    }
  };

  // 生成表格列定义
  const generateColumns = (columns: string[]) => {
    return columns.map(col => ({
      title: col,
      dataIndex: col,
      key: col,
    }));
  };
  
  // 从工具配置中加载SQL配置数据
  useEffect(() => {
    if (toolConfigs && toolConfigs.length > 0) {
      let sqlConfig;
      
      // 如果URL中有工具ID，优先查找匹配ID的工具配置
      if (toolIdFromUrl && toolIdFromUrl !== 'new') {
        sqlConfig = toolConfigs.find(config => config.toolType === 'SQL' && config.id === parseInt(toolIdFromUrl));
      } else {
        // 否则查找第一个SQL类型配置
        sqlConfig = toolConfigs.find(config => config.toolType === 'SQL');
      }
      
      if (sqlConfig) {
        setSqlConfigData(sqlConfig);
        
        // 从toolValue中解析连接配置
        let connectionConfig: SQLConnectionConfig | null = null;
        if (sqlConfig.toolValue) {
          try {
            connectionConfig = JSON.parse(sqlConfig.toolValue);
          } catch (e) {
            console.error('Error parsing connection config from toolValue:', e);
          }
        }
        
        // 构建新的初始值
        const newInitialValues = {
          toolInstanceName: sqlConfig.toolInstanceName || 'SQL执行器',
          toolInstanceDesc: sqlConfig.toolInstanceDesc || '',
          dialect: 'mysql',
          host: 'localhost',
          port: '3306',
          database: 'test',
          username: 'root',
          password: 'password',
          maxRows: 1000,
          timeout: 30,
          sql: 'SELECT * FROM users WHERE id = ${id};',
        };
        
        // 如果有连接配置，更新初始值
        if (connectionConfig) {
          Object.assign(newInitialValues, {
            dialect: connectionConfig.dialect,
            host: connectionConfig.host,
            port: connectionConfig.port,
            database: connectionConfig.database,
            username: connectionConfig.username,
            password: connectionConfig.password,
            maxRows: connectionConfig.maxRows,
            timeout: connectionConfig.timeout,
          });
        }
        
        // 优先从toolValue中读取SQL语句，如果toolValue中没有则从sqlConfig中读取
        if (connectionConfig && connectionConfig.sql) {
          newInitialValues.sql = connectionConfig.sql;
        } else if (sqlConfig.sql) {
          newInitialValues.sql = sqlConfig.sql;
        }
        
        // 更新表单初始值状态
        setFormInitialValues(newInitialValues);
        
        // 将配置数据设置到表单中
        form.setFieldsValue(newInitialValues);
        
        // 设置输入输出参数
        if (sqlConfig.inputTemplate) {
          try {
            setInputParams(JSON.parse(sqlConfig.inputTemplate));
          } catch (e) {
            console.error('Error parsing input template:', e);
            // 设置默认输入参数
            setInputParams([
              { name: 'id', type: 'int', description: '用户ID', required: true, defaultValue: '1' }
            ]);
          }
        } else {
          // 如果没有输入模板，设置默认输入参数
          setInputParams([
            { name: 'id', type: 'int', description: '用户ID', required: true, defaultValue: '1' }
          ]);
        }
        
        if (sqlConfig.outTemplate) {
          try {
            setOutputParams(JSON.parse(sqlConfig.outTemplate));
          } catch (e) {
            console.error('Error parsing output template:', e);
            // 设置默认输出参数
            setOutputParams([
              { name: 'result', type: 'object', description: '查询结果', required: true }
            ]);
          }
        } else {
          // 如果没有输出模板，设置默认输出参数
          setOutputParams([
            { name: 'result', type: 'object', description: '查询结果', required: true }
          ]);
        }
      } else {
        // 如果没有找到SQL配置，设置默认值
        const defaultInitialValues = {
          toolInstanceName: 'SQL执行器',
          toolInstanceDesc: '',
          dialect: 'mysql',
          host: 'localhost',
          port: '3306',
          database: 'test',
          username: 'root',
          password: 'password',
          maxRows: 1000,
          timeout: 30,
          sql: 'SELECT * FROM users WHERE id = ${id};',
        };
        
        setFormInitialValues(defaultInitialValues);
        form.setFieldsValue(defaultInitialValues);
        
        // 设置默认输入输出参数
        setInputParams([
          { name: 'id', type: 'int', description: '用户ID', required: true, defaultValue: '1' }
        ]);
        setOutputParams([
          { name: 'result', type: 'object', description: '查询结果', required: true }
        ]);
      }
    } else {
      // 如果没有工具配置，设置默认值
      const defaultInitialValues = {
        toolInstanceName: 'SQL执行器',
        toolInstanceDesc: '',
        dialect: 'mysql',
        host: 'localhost',
        port: '3306',
        database: 'test',
        username: 'root',
        password: 'password',
        maxRows: 1000,
        timeout: 30,
        sql: 'SELECT * FROM users WHERE id = ${id};',
      };
      
      setFormInitialValues(defaultInitialValues);
      form.setFieldsValue(defaultInitialValues);
      
      // 设置默认输入输出参数
      setInputParams([
        { name: 'id', type: 'int', description: '用户ID', required: true, defaultValue: '1' }
      ]);
      setOutputParams([
        { name: 'result', type: 'object', description: '查询结果', required: true }
      ]);
    }
  }, [toolConfigs, form, toolIdFromUrl]);

  // 通用模板配置变更处理
  const handleInputParamsChange = (params: any[]) => {
    setInputParams(params);
  };

  const handleOutputParamsChange = (params: any[]) => {
    setOutputParams(params);
  };

  return (
    <Card title="SQL执行器" size="small">
      <Tabs defaultActiveKey="connection">
        <TabPane tab="连接配置与SQL执行" key="connection">
          <Form form={form} layout="vertical" onFinish={handleSubmit} initialValues={formInitialValues} onValuesChange={onFormValuesChange}>
            <Form.Item
              name="toolInstanceName"
              label="工具实例名称"
              rules={[{ required: true, message: '请输入工具实例名称' }]}
            >
              <Input placeholder="例如: SQL执行器" />
            </Form.Item>
            <Form.Item
              name="toolInstanceDesc"
              label="工具实例描述"
              rules={[{ required: true, message: '请输入工具实例描述' }]}
            >
              <Input.TextArea placeholder="请输入工具实例描述" />
            </Form.Item>
            
            <Row gutter={16}>
              <Col span={12}>
                <Form.Item
                  name="dialect"
                  label="数据库类型"
                  rules={[{ required: true, message: '请选择数据库类型' }]}
                  initialValue="mysql"
                >
                  <Select placeholder="选择数据库类型">
                    <Select.Option value="mysql">MySQL</Select.Option>
                    <Select.Option value="postgresql">PostgreSQL</Select.Option>
                    <Select.Option value="oracle">Oracle</Select.Option>
                    <Select.Option value="sqlserver">SQL Server</Select.Option>
                  </Select>
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item
                  name="host"
                  label="主机地址"
                  rules={[{ required: true, message: '请输入主机地址' }]}
                  initialValue="localhost"
                >
                  <Input placeholder="例如: localhost" />
                </Form.Item>
              </Col>
            </Row>

            <Row gutter={16}>
              <Col span={12}>
                <Form.Item
                  name="port"
                  label="端口"
                  rules={[{ required: true, message: '请输入端口号' }]}
                  initialValue="3306"
                >
                  <Input placeholder="例如: 3306" />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item
                  name="database"
                  label="数据库名"
                  rules={[{ required: true, message: '请输入数据库名' }]}
                  initialValue="test"
                >
                  <Input placeholder="例如: myapp_db" />
                </Form.Item>
              </Col>
            </Row>

            <Row gutter={16}>
              <Col span={12}>
                <Form.Item
                  name="username"
                  label="用户名"
                  rules={[{ required: true, message: '请输入用户名' }]}
                  initialValue="root"
                >
                  <Input placeholder="数据库用户名" />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item
                  name="password"
                  label="密码"
                  rules={[{ required: true, message: '请输入密码' }]}
                  initialValue="password"
                >
                  <Input.Password placeholder="数据库密码" />
                </Form.Item>
              </Col>
            </Row>

            <Form.Item>
              <Space>
                <Button 
                  type="primary" 
                  onClick={handleTestConnection}
                  loading={connectionStatus === 'connecting'}
                >
                  {connectionStatus === 'connecting' ? '连接中...' : '测试连接'}
                </Button>
                <span>
                  {connectionStatus === 'connected' && <span style={{ color: 'green' }}>连接成功</span>}
                  {connectionStatus === 'failed' && <span style={{ color: 'red' }}>连接失败</span>}
                </span>
              </Space>
            </Form.Item>

            <Divider dashed />

            <Row gutter={16}>
              <Col span={12}>
                <Form.Item
                  name="maxRows"
                  label="最大返回行数"
                  rules={[{ required: true, message: '请输入最大返回行数' }]}
                  initialValue={1000}
                >
                  <Input type="number" placeholder="例如: 1000" />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item
                  name="timeout"
                  label="查询超时(秒)"
                  rules={[{ required: true, message: '请输入查询超时时间' }]}
                  initialValue={30}
                >
                  <Input type="number" placeholder="例如: 30" />
                </Form.Item>
              </Col>
            </Row>

            <Divider dashed />

            <Form.Item
              name="sql"
              label="SQL语句"
              rules={[{ required: true, message: '请输入SQL语句' }]}
              initialValue="SELECT * FROM users WHERE id = ${id};"
            >
              <Input.TextArea rows={6} placeholder="输入SQL语句，例如: SELECT * FROM users;" />
            </Form.Item>
            
            <Form.Item>
              <Space>
                <Button 
                  type="primary" 
                  onClick={handleExecuteSQL}
                  loading={executingSql}
                >
                  执行SQL
                </Button>
                <Button htmlType="button" onClick={() => form.resetFields(['sql'])}>
                  清空
                </Button>
              </Space>
            </Form.Item>
          </Form>

          {/* 调试结果展示 */}
          <DebugResult debugResult={executionResult} loading={executingSql} title="SQL执行结果" />
        </TabPane>
        <TabPane tab="通用模板配置" key="template">
          <CommonTemplateConfig 
            inputParams={inputParams}
            outputParams={outputParams}
            onInputParamsChange={handleInputParamsChange}
            onOutputParamsChange={handleOutputParamsChange}
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

export default SQLConfig;