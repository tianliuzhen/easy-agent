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
  InputNumber,
  Divider,
  Badge,
  Row,
  Col
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  PlayCircleOutlined,
  SyncOutlined,
  EyeOutlined,
  ThunderboltOutlined,
  ReloadOutlined,
  StarOutlined,
  ToolOutlined
} from '@ant-design/icons';
import { skillApi } from '../../api/SkillApi';

const { Option } = Select;
const { TextArea } = Input;

interface SkillConfig {
  id?: number;
  skillName: string;
  skillDisplayName: string;
  skillDescription?: string;
  skillType: string;
  skillCategory: string;
  skillIcon?: string;
  skillVersion?: string;
  skillProvider?: string;
  skillCapabilities?: string[];
  inputSchema?: string;
  outputSchema?: string;
  skillConfig?: string;
  executionMode?: string;
  timeout?: number;
  maxRetries?: number;
  status?: string;
  lastExecutedAt?: string;
  lastError?: string;
  createdAt?: string;
  updatedAt?: string;
}

const MySkill: React.FC = () => {
  const [data, setData] = useState<SkillConfig[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchText, setSearchText] = useState('');
  const [modalVisible, setModalVisible] = useState(false);
  const [detailModalVisible, setDetailModalVisible] = useState(false);
  const [testModalVisible, setTestModalVisible] = useState(false);
  const [editingRecord, setEditingRecord] = useState<SkillConfig | null>(null);
  const [viewingRecord, setViewingRecord] = useState<SkillConfig | null>(null);
  const [form] = Form.useForm();
  const [testLoading, setTestLoading] = useState(false);
  const [testResult, setTestResult] = useState<any>(null);

  // 加载数据
  const loadData = async () => {
    setLoading(true);
    try {
      const response = await skillApi.getSkillConfigByUserId();
      if (response.success) {
        setData(response.data || []);
      } else {
        message.error(response.message || '加载失败');
      }
    } catch (error) {
      console.error('加载 Skill 配置失败:', error);
      message.error('加载 Skill 配置失败');
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
      skillType: 'INTERNAL',
      skillCategory: 'general',
      skillVersion: '1.0.0',
      skillProvider: 'System',
      executionMode: 'sync',
      timeout: 30,
      maxRetries: 3,
      status: 'active'
    });
    setModalVisible(true);
  };

  // 打开编辑模态框
  const handleEdit = (record: SkillConfig) => {
    setEditingRecord(record);
    form.setFieldsValue({
      ...record,
      skillCapabilities: record.skillCapabilities?.join(',')
    });
    setModalVisible(true);
  };

  // 查看详情
  const handleView = (record: SkillConfig) => {
    setViewingRecord(record);
    setDetailModalVisible(true);
  };

  // 卸载 Skill
  const handleUninstall = async (skillConfigId: number) => {
    try {
      const response = await skillApi.uninstallSkill(skillConfigId);
      if (response.success) {
        message.success('卸载成功');
        loadData();
      } else {
        message.error(response.message || '卸载失败');
      }
    } catch (error) {
      console.error('卸载失败:', error);
      message.error('卸载失败');
    }
  };

  // 测试技能
  const handleTest = async (record: SkillConfig) => {
    setViewingRecord(record);
    setTestResult(null);
    setTestModalVisible(true);
  };

  // 执行测试
  const handleExecuteTest = async () => {
    try {
      setTestLoading(true);
      // 模拟测试结果
      setTimeout(() => {
        setTestResult({
          success: true,
          skill: viewingRecord?.skillName,
          result: '技能执行成功',
          timestamp: new Date().toISOString()
        });
        setTestLoading(false);
      }, 1000);
    } catch (error) {
      console.error('测试失败:', error);
      message.error('测试失败');
      setTestLoading(false);
    }
  };

  // 提交表单
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      // 处理能力列表
      if (values.skillCapabilities) {
        values.skillCapabilities = values.skillCapabilities.split(',').map((s: string) => s.trim());
      }

      let response;
      if (editingRecord?.id) {
        response = await skillApi.updateConfig(editingRecord.id, values);
      } else {
        response = await skillApi.createConfig(values);
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
      error: { color: 'error', text: '错误' },
      deprecated: { color: 'warning', text: '已弃用' }
    };
    const { color, text } = statusMap[status || 'inactive'] || statusMap.inactive;
    return <Badge status={color as any} text={text} />;
  };

  // 获取类型标签
  const getTypeTag = (type?: string) => {
    const typeMap: Record<string, { color: string; text: string }> = {
      INTERNAL: { color: 'green', text: '内置' },
      EXTERNAL: { color: 'orange', text: '外部' },
      PLUGIN: { color: 'blue', text: '插件' }
    };
    const { color, text } = typeMap[type || 'INTERNAL'] || typeMap.INTERNAL;
    return <Tag color={color}>{text}</Tag>;
  };

  // 获取分类标签
  const getCategoryTag = (category?: string) => {
    const categoryMap: Record<string, { color: string; text: string }> = {
      general: { color: 'blue', text: '通用' },
      development: { color: 'purple', text: '开发' },
      data: { color: 'cyan', text: '数据' },
      media: { color: 'magenta', text: '媒体' }
    };
    const { color, text } = categoryMap[category || 'general'] || categoryMap.general;
    return <Tag color={color}>{text}</Tag>;
  };

  // 表格列定义
  const columns = [
    {
      title: '技能',
      dataIndex: 'skillDisplayName',
      key: 'skillDisplayName',
      render: (text: string, record: SkillConfig) => (
        <Space>
          <span style={{ fontSize: '20px' }}>{record.skillIcon || '🔧'}</span>
          <div>
            <div style={{ fontWeight: 500 }}>{text}</div>
            <div style={{ fontSize: '12px', color: '#999' }}>{record.skillName}</div>
          </div>
        </Space>
      )
    },
    {
      title: '类型',
      dataIndex: 'skillType',
      key: 'skillType',
      width: 100,
      render: (type: string) => getTypeTag(type)
    },
    {
      title: '分类',
      dataIndex: 'skillCategory',
      key: 'skillCategory',
      width: 100,
      render: (category: string) => getCategoryTag(category)
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => getStatusTag(status)
    },
    {
      title: '版本',
      dataIndex: 'skillVersion',
      key: 'skillVersion',
      width: 100,
      render: (version: string) => <Tag size="small">v{version || '1.0.0'}</Tag>
    },
    {
      title: '最后执行',
      dataIndex: 'lastExecutedAt',
      key: 'lastExecutedAt',
      width: 180,
      render: (text: string) => text || '从未执行'
    },
    {
      title: '操作',
      key: 'action',
      width: 250,
      render: (_: any, record: SkillConfig) => (
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
          <Tooltip title="测试">
            <Button
              type="text"
              icon={<PlayCircleOutlined />}
              onClick={() => handleTest(record)}
            />
          </Tooltip>
          <Popconfirm
            title="确定要卸载吗？"
            description="卸载后可以从市场重新安装"
            onConfirm={() => record.id && handleUninstall(record.id)}
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
    item.skillName.toLowerCase().includes(searchText.toLowerCase()) ||
    item.skillDisplayName.toLowerCase().includes(searchText.toLowerCase()) ||
    (item.skillDescription && item.skillDescription.toLowerCase().includes(searchText.toLowerCase()))
  );

  return (
    <div>
      <Card
        title={
          <Space>
            <ThunderboltOutlined style={{ color: '#faad14' }} />
            <span>我的 Skill</span>
          </Space>
        }
        extra={
          <Space>
            <Input.Search
              placeholder="搜索技能名称"
              allowClear
              onChange={(e) => setSearchText(e.target.value)}
              style={{ width: 250 }}
            />
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={handleAdd}
            >
              新增 Skill
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
        title={editingRecord ? '编辑 Skill' : '新增 Skill'}
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
            skillType: 'INTERNAL',
            skillCategory: 'general',
            skillVersion: '1.0.0',
            skillProvider: 'System',
            executionMode: 'sync',
            timeout: 30,
            maxRetries: 3,
            status: 'active'
          }}
        >
          <Form.Item
            name="skillName"
            label="技能名称"
            rules={[{ required: true, message: '请输入技能名称' }]}
          >
            <Input placeholder="例如：code_review" />
          </Form.Item>

          <Form.Item
            name="skillDisplayName"
            label="显示名称"
            rules={[{ required: true, message: '请输入显示名称' }]}
          >
            <Input placeholder="例如：代码审查" />
          </Form.Item>

          <Form.Item
            name="skillDescription"
            label="描述"
          >
            <TextArea rows={2} placeholder="技能的功能描述" />
          </Form.Item>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="skillType"
                label="技能类型"
                rules={[{ required: true }]}
              >
                <Select>
                  <Option value="INTERNAL">内置</Option>
                  <Option value="EXTERNAL">外部</Option>
                  <Option value="PLUGIN">插件</Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="skillCategory"
                label="分类"
                rules={[{ required: true }]}
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
                label="图标"
              >
                <Input placeholder="例如：🔧 或图标URL" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="skillVersion"
                label="版本"
              >
                <Input placeholder="例如：1.0.0" />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            name="skillProvider"
            label="提供者"
          >
            <Input placeholder="例如：System" />
          </Form.Item>

          <Form.Item
            name="skillCapabilities"
            label="能力列表"
            extra="多个能力用逗号分隔"
          >
            <Input placeholder="例如：code-analysis,quality-check,suggestion" />
          </Form.Item>

          <Divider orientation="left">执行配置</Divider>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="executionMode"
                label="执行模式"
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
              >
                <InputNumber min={0} max={10} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="status"
                label="状态"
                valuePropName="checked"
                getValueFromEvent={(checked) => checked ? 'active' : 'inactive'}
                getValueProps={(value) => ({ checked: value === 'active' })}
              >
                <Switch checkedChildren="启用" unCheckedChildren="停用" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      {/* 详情模态框 */}
      <Modal
        title="Skill 详情"
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
            <Descriptions.Item label="技能名称" span={2}>
              <Space>
                <span style={{ fontSize: '24px' }}>{viewingRecord.skillIcon || '🔧'}</span>
                <span>{viewingRecord.skillDisplayName}</span>
              </Space>
            </Descriptions.Item>
            <Descriptions.Item label="标识">
              <code>{viewingRecord.skillName}</code>
            </Descriptions.Item>
            <Descriptions.Item label="版本">
              {viewingRecord.skillVersion || '1.0.0'}
            </Descriptions.Item>
            <Descriptions.Item label="类型">
              {getTypeTag(viewingRecord.skillType)}
            </Descriptions.Item>
            <Descriptions.Item label="分类">
              {getCategoryTag(viewingRecord.skillCategory)}
            </Descriptions.Item>
            <Descriptions.Item label="状态">
              {getStatusTag(viewingRecord.status)}
            </Descriptions.Item>
            <Descriptions.Item label="提供者">
              {viewingRecord.skillProvider || 'System'}
            </Descriptions.Item>
            <Descriptions.Item label="执行模式">
              {viewingRecord.executionMode === 'sync' ? '同步' : '异步'}
            </Descriptions.Item>
            <Descriptions.Item label="超时时间">
              {viewingRecord.timeout || 30} 秒
            </Descriptions.Item>
            <Descriptions.Item label="最大重试">
              {viewingRecord.maxRetries || 3} 次
            </Descriptions.Item>
            <Descriptions.Item label="描述" span={2}>
              {viewingRecord.skillDescription || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="能力" span={2}>
              <Space wrap>
                {viewingRecord.skillCapabilities?.map(cap => (
                  <Tag key={cap} color="blue">{cap}</Tag>
                ))}
              </Space>
            </Descriptions.Item>
            <Descriptions.Item label="最后执行">
              {viewingRecord.lastExecutedAt || '从未执行'}
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

      {/* 测试模态框 */}
      <Modal
        title="测试 Skill"
        open={testModalVisible}
        onCancel={() => setTestModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setTestModalVisible(false)}>
            关闭
          </Button>,
          <Button
            key="test"
            type="primary"
            icon={<ThunderboltOutlined />}
            loading={testLoading}
            onClick={handleExecuteTest}
          >
            执行测试
          </Button>
        ]}
        width={600}
      >
        <p>技能：{viewingRecord?.skillDisplayName}</p>
        {testResult && (
          <div style={{ background: '#f6f8fa', padding: 16, borderRadius: 8 }}>
            <pre style={{ margin: 0 }}>
              {JSON.stringify(testResult, null, 2)}
            </pre>
          </div>
        )}
      </Modal>
    </div>
  );
};

export default MySkill;