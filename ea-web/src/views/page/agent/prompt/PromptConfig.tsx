import React, { useState } from 'react';
import { Card, Typography, Button, Space, Table, Input, Tag, Modal, Form, InputNumber } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, SearchOutlined, CopyOutlined } from '@ant-design/icons';

const { Title, Text } = Typography;

const PromptConfig: React.FC<{ agentId?: number }> = ({ agentId }) => {
  const [form] = Form.useForm();
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [editingPrompt, setEditingPrompt] = useState<any>(null);

  // 模拟提示词数据
  const promptData = [
    {
      id: 1,
      name: '客户咨询回复',
      content: '请根据以下信息回答客户问题：产品特性、价格、使用方法等。语气要专业、友好。',
      category: '客户服务',
      status: 'active',
      createdAt: '2024-01-15',
      useCount: 125,
    },
    {
      id: 2,
      name: '技术问题解答',
      content: '针对技术问题，提供详细的解决方案和操作步骤，引用相关文档。',
      category: '技术支持',
      status: 'active',
      createdAt: '2024-01-20',
      useCount: 89,
    },
    {
      id: 3,
      name: '销售引导',
      content: '根据客户需求推荐合适的产品，并介绍产品优势和优惠政策。',
      category: '销售',
      status: 'inactive',
      createdAt: '2024-02-01',
      useCount: 42,
    },
  ];

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
    },
    {
      title: '提示词名称',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '分类',
      dataIndex: 'category',
      key: 'category',
    },
    {
      title: '内容',
      dataIndex: 'content',
      key: 'content',
      render: (text: string) => (
        <div style={{ maxWidth: '300px' }}>
          <Text ellipsis={{ tooltip: text }}>{text}</Text>
        </div>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={status === 'active' ? 'green' : 'red'}>
          {status === 'active' ? '启用' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '使用次数',
      dataIndex: 'useCount',
      key: 'useCount',
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space size="middle">
          <Button 
            type="link" 
            icon={<EditOutlined />} 
            size="small" 
            onClick={() => {
              setEditingPrompt(record);
              form.setFieldsValue(record);
              setIsModalVisible(true);
            }}
          >
            编辑
          </Button>
          <Button type="link" icon={<CopyOutlined />} size="small">复制</Button>
          <Button type="link" icon={<DeleteOutlined />} size="small" danger>删除</Button>
        </Space>
      ),
    },
  ];

  const showModal = () => {
    setEditingPrompt(null);
    form.resetFields();
    setIsModalVisible(true);
  };

  const handleOk = () => {
    form.validateFields().then(values => {
      console.log('保存提示词:', values);
      setIsModalVisible(false);
      // 这里可以添加实际的保存逻辑
    });
  };

  const handleCancel = () => {
    setIsModalVisible(false);
  };

  return (
    <div>
      <div style={{ marginBottom: '24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <Title level={4}>提示词管理</Title>
          <Text type="secondary">管理Agent的提示词模板，优化回答质量</Text>
        </div>
        <Space>
          <Button type="primary" icon={<PlusOutlined />} onClick={showModal}>
            新建提示词
          </Button>
        </Space>
      </div>

      <Card>
        <div style={{ marginBottom: '16px', display: 'flex', justifyContent: 'space-between' }}>
          <div style={{ width: '300px' }}>
            <Input placeholder="搜索提示词..." prefix={<SearchOutlined />} />
          </div>
        </div>

        <Table 
          dataSource={promptData} 
          columns={columns} 
          rowKey="id"
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条记录`,
          }}
        />
      </Card>

      <Modal
        title={editingPrompt ? "编辑提示词" : "新建提示词"}
        open={isModalVisible}
        onOk={handleOk}
        onCancel={handleCancel}
        width={600}
      >
        <Form
          form={form}
          layout="vertical"
          initialValues={{ status: 'active', useCount: 0 }}
        >
          <Form.Item
            name="name"
            label="提示词名称"
            rules={[{ required: true, message: '请输入提示词名称' }]}
          >
            <Input placeholder="请输入提示词名称" />
          </Form.Item>

          <Form.Item
            name="category"
            label="分类"
            rules={[{ required: true, message: '请选择分类' }]}
          >
            <Input placeholder="请输入分类" />
          </Form.Item>

          <Form.Item
            name="content"
            label="提示词内容"
            rules={[{ required: true, message: '请输入提示词内容' }]}
          >
            <Input.TextArea rows={4} placeholder="请输入提示词内容" />
          </Form.Item>

          <Form.Item
            name="status"
            label="状态"
            rules={[{ required: true, message: '请选择状态' }]}
          >
            <Input placeholder="active 或 inactive" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default PromptConfig;
