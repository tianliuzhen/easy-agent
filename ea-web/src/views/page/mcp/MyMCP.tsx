import React, { useState, useEffect } from 'react';
import {
  Table,
  Button,
  Space,
  Tag,
  Card,
  Input,
  Modal,
  Form,
  Select,
  message,
  Popconfirm,
  Tooltip,
  Descriptions,
  Switch,
  Radio,
  InputNumber,
  Divider,
  List,
  Badge
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  PlayCircleOutlined,
  SyncOutlined,
  EyeOutlined,
  CloudServerOutlined,
  ApiOutlined,
  CodeOutlined,
  ReloadOutlined,
  ThunderboltOutlined,
  DownOutlined,
  RightOutlined
} from '@ant-design/icons';
import { mcpApi } from '../../api/McpApi';

const { Option } = Select;
const { TextArea } = Input;

interface McpServerConfig {
  id?: number;
  serverName: string;
  serverUrl?: string;
  transportType: string;
  command?: string;
  envVars?: string[];
  toolName?: string;
  toolDisplayName?: string;
  toolDescription?: string;
  inputSchema?: string;
  outputSchema?: string;
  connectionTimeout?: number;
  maxRetries?: number;
  status?: string;
  description?: string;
  toolMetadata?: string;
  lastConnectedAt?: string;
  lastError?: string;
  createdAt?: string;
  updatedAt?: string;
}

interface McpToolInfo {
  name: string;
  description?: string;
  inputSchema?: any;
  outputSchema?: any;
}

const MyMCP: React.FC = () => {
  const [data, setData] = useState<McpServerConfig[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchText, setSearchText] = useState('');
  const [modalVisible, setModalVisible] = useState(false);
  const [detailModalVisible, setDetailModalVisible] = useState(false);
  const [testModalVisible, setTestModalVisible] = useState(false);
  const [editingRecord, setEditingRecord] = useState<McpServerConfig | null>(null);
  const [viewingRecord, setViewingRecord] = useState<McpServerConfig | null>(null);
  const [form] = Form.useForm();
  const [testLoading, setTestLoading] = useState(false);
  const [testResult, setTestResult] = useState<McpToolInfo[] | null>(null);
  const [syncLoading, setSyncLoading] = useState<number | null>(null);
  const [testConnectionLoading, setTestConnectionLoading] = useState<number | null>(null);
  const [expandedSchemas, setExpandedSchemas] = useState<Record<string, { input: boolean; output: boolean }>>({});

  // 默认展开所有 Schema
  useEffect(() => {
    if (testResult) {
      const defaultExpanded: Record<string, { input: boolean; output: boolean }> = {};
      testResult.forEach(item => {
        defaultExpanded[item.name] = { input: true, output: true };
      });
      setExpandedSchemas(defaultExpanded);
    }
  }, [testResult]);

  // 加载数据
  const loadData = async () => {
    setLoading(true);
    try {
      const response = await mcpApi.listAllConfigs();
      if (response.success) {
        setData(response.data || []);
      } else {
        message.error(response.message || '加载失败');
      }
    } catch (error) {
      console.error('加载 MCP 配置失败:', error);
      message.error('加载 MCP 配置失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  // 打开新增模态框
  const handleAdd = () => {
    setEditingRecord(null);
    form.resetFields();
    form.setFieldsValue({
      transportType: 'STREAMABLE',
      connectionTimeout: 30,
      maxRetries: 3,
      status: 'active'
    });
    setModalVisible(true);
  };

  // 打开编辑模态框
  const handleEdit = (record: McpServerConfig) => {
    setEditingRecord(record);
    form.setFieldsValue({
      ...record,
      envVars: record.envVars ? record.envVars.join('\n') : ''
    });
    setModalVisible(true);
  };

  // 查看详情
  const handleView = (record: McpServerConfig) => {
    setViewingRecord(record);
    setDetailModalVisible(true);
  };

  // 删除
  const handleDelete = async (id: number) => {
    try {
      const response = await mcpApi.deleteConfig(id);
      if (response.success) {
        message.success('删除成功');
        loadData();
      } else {
        message.error(response.message || '删除失败');
      }
    } catch (error) {
      console.error('删除失败:', error);
      message.error('删除失败');
    }
  };

  // 获取工具列表
  const handleFetchTools = async (record: McpServerConfig) => {
    if (!record.id) return;
    setTestConnectionLoading(record.id);
    try {
      const response = await mcpApi.fetchToolsFromServer(record.id);
      if (response.success) {
        message.success('获取工具列表成功');
        setTestResult(response.data || []);
        setTestModalVisible(true);
      } else {
        message.error(response.message || '获取工具列表失败');
      }
    } catch (error) {
      console.error('获取工具列表失败:', error);
      message.error('获取工具列表失败');
    } finally {
      setTestConnectionLoading(null);
    }
  };

  // 同步工具
  const handleSync = async (record: McpServerConfig) => {
    if (!record.id) return;
    setSyncLoading(record.id);
    try {
      const response = await mcpApi.syncTools(record.id);
      if (response.success) {
        message.success(`同步成功，共 ${response.data} 个工具`);
        loadData();
      } else {
        message.error(response.message || '同步失败');
      }
    } catch (error) {
      console.error('同步失败:', error);
      message.error('同步失败');
    } finally {
      setSyncLoading(null);
    }
  };

  // 提交表单
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      // 处理环境变量
      if (values.envVars) {
        values.envVars = values.envVars.split('\n').filter((line: string) => line.trim());
      }

      let response;
      if (editingRecord?.id) {
        response = await mcpApi.updateConfig(editingRecord.id, values);
      } else {
        response = await mcpApi.createConfig(values);
      }

      if (response.success) {
        message.success(editingRecord ? '更新成功' : '创建成功');
        setModalVisible(false);
        loadData();
      } else {
        message.error(response.message || (editingRecord ? '更新失败' : '创建失败'));
      }
    } catch (error) {
      console.error('提交失败:', error);
      message.error('提交失败');
    }
  };

  // 获取状态标签
  const getStatusTag = (status?: string) => {
    const statusMap: Record<string, { color: string; text: string }> = {
      active: { color: 'success', text: '正常' },
      inactive: { color: 'default', text: '停用' },
      error: { color: 'error', text: '错误' }
    };
    const { color, text } = statusMap[status || 'inactive'] || statusMap.inactive;
    return <Badge status={color as any} text={text} />;
  };

  // 获取传输类型标签
  const getTransportTypeTag = (type?: string) => {
    const typeMap: Record<string, { color: string; text: string }> = {
      STDIO: { color: 'blue', text: 'STDIO' },
      SSE: { color: 'orange', text: 'SSE' },
      STREAMABLE: { color: 'purple', text: 'STREAMABLE' }
    };
    const { color, text } = typeMap[type || 'STREAMABLE'] || typeMap.STREAMABLE;
    return <Tag color={color}>{text}</Tag>;
  };

  // 表格列定义
  const columns = [
    {
      title: '服务器名称',
      dataIndex: 'serverName',
      key: 'serverName',
      render: (text: string, record: McpServerConfig) => (
        <Space>
          <CloudServerOutlined style={{ color: '#722ed1' }} />
          <span style={{ fontWeight: 500 }}>{text}</span>
        </Space>
      )
    },
    {
      title: '传输类型',
      dataIndex: 'transportType',
      key: 'transportType',
      width: 120,
      render: (type: string) => getTransportTypeTag(type)
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => getStatusTag(status)
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true
    },
    {
      title: '最后连接',
      dataIndex: 'lastConnectedAt',
      key: 'lastConnectedAt',
      width: 180,
      render: (text: string) => text || '从未连接'
    },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      width: 180
    },
    {
      title: '操作',
      key: 'action',
      width: 280,
      render: (_: any, record: McpServerConfig) => (
        <Space size="small">
          <Tooltip title="查看">
            <Button
              type="text"
              icon={<EyeOutlined />}
              onClick={() => handleView(record)}
            />
          </Tooltip>
          <Tooltip title="编辑">
            <Button
              type="text"
              icon={<EditOutlined />}
              onClick={() => handleEdit(record)}
            />
          </Tooltip>
          <Tooltip title="获取工具列表">
            <Button
              type="text"
              icon={<PlayCircleOutlined />}
              loading={testConnectionLoading === record.id}
              onClick={() => handleFetchTools(record)}
            />
          </Tooltip>
          <Tooltip title="同步工具">
            <Button
              type="text"
              icon={<SyncOutlined spin={syncLoading === record.id} />}
              loading={syncLoading === record.id}
              onClick={() => handleSync(record)}
            />
          </Tooltip>
          <Popconfirm
            title="确定要删除吗？"
            description="删除后无法恢复"
            onConfirm={() => record.id && handleDelete(record.id)}
            okText="确定"
            cancelText="取消"
          >
            <Button type="text" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      )
    }
  ];

  // 过滤数据
  const filteredData = data.filter(item =>
    item.serverName.toLowerCase().includes(searchText.toLowerCase()) ||
    (item.description && item.description.toLowerCase().includes(searchText.toLowerCase()))
  );

  return (
    <div>
      <Card
        title={
          <Space>
            <ApiOutlined />
            <span>我的 MCP</span>
          </Space>
        }
        extra={
          <Space>
            <Input.Search
              placeholder="搜索名称或描述"
              allowClear
              onChange={(e) => setSearchText(e.target.value)}
              style={{ width: 250 }}
            />
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={handleAdd}
            >
              新增 MCP
            </Button>
            <Button
              icon={<ReloadOutlined />}
              onClick={loadData}
            >
              刷新
            </Button>
          </Space>
        }
      >
        <Table
          columns={columns}
          dataSource={filteredData}
          rowKey="id"
          loading={loading}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条`
          }}
        />
      </Card>

      {/* 新增/编辑模态框 */}
      <Modal
        title={editingRecord ? '编辑 MCP Server' : '新增 MCP Server'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        width={700}
        destroyOnClose
      >
        <Form
          form={form}
          layout="vertical"
          initialValues={{
            transportType: 'STREAMABLE',
            connectionTimeout: 30,
            maxRetries: 3,
            status: 'active'
          }}
        >
          <Form.Item
            name="serverName"
            label="服务器名称"
            rules={[{ required: true, message: '请输入服务器名称' }]}
          >
            <Input placeholder="例如：文件系统 MCP" />
          </Form.Item>

          <Form.Item
            name="transportType"
            label="传输类型"
            rules={[{ required: true, message: '请选择传输类型' }]}
          >
            <Radio.Group>
              <Radio.Button value="STDIO">STDIO (本地进程)</Radio.Button>
              <Radio.Button value="SSE">SSE (传统)</Radio.Button>
              <Radio.Button value="STREAMABLE">STREAMABLE (推荐)</Radio.Button>
            </Radio.Group>
          </Form.Item>

          <Form.Item
            noStyle
            shouldUpdate={(prevValues, currentValues) =>
              prevValues.transportType !== currentValues.transportType
            }
          >
            {({ getFieldValue }) => {
              const transportType = getFieldValue('transportType');
              if (transportType === 'STDIO') {
                return (
                  <Form.Item
                    name="command"
                    label="启动命令"
                    rules={[{ required: true, message: '请输入启动命令' }]}
                  >
                    <TextArea
                      rows={2}
                      placeholder="例如：npx -y @modelcontextprotocol/server-filesystem /path/to/files"
                    />
                  </Form.Item>
                );
              }
              return (
                <Form.Item
                  name="serverUrl"
                  label="服务器地址"
                  rules={[{ required: true, message: '请输入服务器地址' }]}
                >
                  <Input placeholder="例如：http://localhost:3000/sse" />
                </Form.Item>
              );
            }}
          </Form.Item>

          <Form.Item
            name="envVars"
            label="环境变量"
            extra="每行一个，格式：KEY=value"
          >
            <TextArea
              rows={3}
              placeholder={`API_KEY=your_api_key
DEBUG=true`}
            />
          </Form.Item>

          <Divider orientation="left">工具配置</Divider>

          <Form.Item
            name="toolName"
            label="工具名称"
          >
            <Input placeholder="MCP Server 中的原始工具名称" />
          </Form.Item>

          <Form.Item
            name="toolDisplayName"
            label="显示名称"
          >
            <Input placeholder="用于展示的工具名称" />
          </Form.Item>

          <Form.Item
            name="toolDescription"
            label="工具描述"
          >
            <TextArea rows={2} placeholder="工具的功能描述" />
          </Form.Item>

          <Form.Item
            name="description"
            label="描述"
          >
            <TextArea rows={2} placeholder="MCP Server 的描述信息" />
          </Form.Item>

          <Divider orientation="left">高级配置</Divider>

          <Space size="large">
            <Form.Item
              name="connectionTimeout"
              label="连接超时(秒)"
            >
              <InputNumber min={1} max={300} defaultValue={30} />
            </Form.Item>

            <Form.Item
              name="maxRetries"
              label="最大重试次数"
            >
              <InputNumber min={0} max={10} defaultValue={3} />
            </Form.Item>

            <Form.Item
              name="status"
              label="状态"
              valuePropName="checked"
              getValueFromEvent={(checked) => checked ? 'active' : 'inactive'}
              getValueProps={(value) => ({ checked: value === 'active' })}
            >
              <Switch checkedChildren="启用" unCheckedChildren="停用" />
            </Form.Item>
          </Space>
        </Form>
      </Modal>

      {/* 详情模态框 */}
      <Modal
        title="MCP Server 详情"
        open={detailModalVisible}
        onCancel={() => setDetailModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setDetailModalVisible(false)}>
            关闭
          </Button>
        ]}
        width={700}
      >
        {viewingRecord && (
          <Descriptions bordered column={2}>
            <Descriptions.Item label="服务器名称" span={2}>
              {viewingRecord.serverName}
            </Descriptions.Item>
            <Descriptions.Item label="传输类型">
              {getTransportTypeTag(viewingRecord.transportType)}
            </Descriptions.Item>
            <Descriptions.Item label="状态">
              {getStatusTag(viewingRecord.status)}
            </Descriptions.Item>
            <Descriptions.Item label="服务器地址" span={2}>
              {viewingRecord.serverUrl || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="启动命令" span={2}>
              <code style={{ wordBreak: 'break-all' }}>
                {viewingRecord.command || '-'}
              </code>
            </Descriptions.Item>
            <Descriptions.Item label="工具名称">
              {viewingRecord.toolName || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="显示名称">
              {viewingRecord.toolDisplayName || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="描述" span={2}>
              {viewingRecord.description || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="工具描述" span={2}>
              {viewingRecord.toolDescription || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="连接超时">
              {viewingRecord.connectionTimeout || 30} 秒
            </Descriptions.Item>
            <Descriptions.Item label="最大重试">
              {viewingRecord.maxRetries || 3} 次
            </Descriptions.Item>
            <Descriptions.Item label="最后连接">
              {viewingRecord.lastConnectedAt || '从未连接'}
            </Descriptions.Item>
            <Descriptions.Item label="最后错误">
              {viewingRecord.lastError || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="创建时间">
              {viewingRecord.createdAt}
            </Descriptions.Item>
            <Descriptions.Item label="更新时间">
              {viewingRecord.updatedAt}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Modal>

      {/* 测试结果模态框 */}
      <Modal
        title={
          <Space>
            <ThunderboltOutlined style={{ color: '#52c41a' }} />
            <span>连接测试成功</span>
          </Space>
        }
        open={testModalVisible}
        onCancel={() => setTestModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setTestModalVisible(false)}>
            关闭
          </Button>
        ]}
        width={600}
      >
        <p>可用工具列表：</p>
        <List
          dataSource={testResult || []}
          renderItem={(item: McpToolInfo) => {
            const key = item.name;
            const expanded = expandedSchemas[key] || { input: false, output: false };
            return (
              <List.Item>
                <Card size="small" style={{ width: '100%' }}>
                  <Space direction="vertical" style={{ width: '100%' }}>
                    <Space>
                      <CodeOutlined />
                      <strong>{item.name}</strong>
                    </Space>
                    <span style={{ color: '#666' }}>{item.description}</span>
                    {item.inputSchema && (
                      <div style={{ marginTop: 8 }}>
                        <Button
                          type="link"
                          size="small"
                          onClick={() => setExpandedSchemas(prev => ({
                            ...prev,
                            [key]: { ...expanded, input: !expanded.input }
                          }))}
                          style={{ padding: 0, height: 'auto' }}
                        >
                          {expanded.input ? <DownOutlined /> : <RightOutlined />}
                          <span style={{ marginLeft: 4 }}>输入参数 Schema</span>
                        </Button>
                        {expanded.input && (
                          <pre style={{
                            background: '#f6f8fa',
                            padding: 12,
                            borderRadius: 6,
                            fontSize: 12,
                            maxHeight: 300,
                            overflow: 'auto',
                            margin: '8px 0 0 0',
                            border: '1px solid #e1e4e8'
                          }}>
                            <code style={{ fontFamily: 'Monaco, Consolas, monospace' }}>
                              {typeof item.inputSchema === 'string'
                                ? JSON.stringify(JSON.parse(item.inputSchema), null, 2)
                                : JSON.stringify(item.inputSchema, null, 2)}
                            </code>
                          </pre>
                        )}
                      </div>
                    )}
                    {item.outputSchema && (
                      <div style={{ marginTop: 8 }}>
                        <Button
                          type="link"
                          size="small"
                          onClick={() => setExpandedSchemas(prev => ({
                            ...prev,
                            [key]: { ...expanded, output: !expanded.output }
                          }))}
                          style={{ padding: 0, height: 'auto' }}
                        >
                          {expanded.output ? <DownOutlined /> : <RightOutlined />}
                          <span style={{ marginLeft: 4 }}>输出参数 Schema</span>
                        </Button>
                        {expanded.output && (
                          <pre style={{
                            background: '#f6f8fa',
                            padding: 12,
                            borderRadius: 6,
                            fontSize: 12,
                            maxHeight: 300,
                            overflow: 'auto',
                            margin: '8px 0 0 0',
                            border: '1px solid #e1e4e8'
                          }}>
                            <code style={{ fontFamily: 'Monaco, Consolas, monospace' }}>
                              {typeof item.outputSchema === 'string'
                                ? JSON.stringify(JSON.parse(item.outputSchema), null, 2)
                                : JSON.stringify(item.outputSchema, null, 2)}
                            </code>
                          </pre>
                        )}
                      </div>
                    )}
                  </Space>
                </Card>
              </List.Item>
            );
          }}
        />
      </Modal>
    </div>
  );
};

export default MyMCP;
