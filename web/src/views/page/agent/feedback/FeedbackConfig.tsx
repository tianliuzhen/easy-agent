import React, { useState } from 'react';
import { Card, Typography, Button, Space, Table, Input, Tag, Modal, Form, Rate } from 'antd';
import { MessageOutlined, SearchOutlined, EyeOutlined, EditOutlined, DeleteOutlined, LikeOutlined, DislikeOutlined } from '@ant-design/icons';

const { Title, Text } = Typography;

const FeedbackConfig: React.FC<{ agentId?: number }> = ({ agentId }) => {
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [editingFeedback, setEditingFeedback] = useState<any>(null);

  // 模拟反馈数据
  const feedbackData = [
    {
      id: 1,
      user: '张三',
      content: '这个回答非常有帮助，解决了我的问题',
      rating: 5,
      category: '正面反馈',
      status: 'processed',
      createdAt: '2024-01-15 14:30',
      response: '感谢您的反馈，很高兴能帮到您！',
    },
    {
      id: 2,
      user: '李四',
      content: '回答不够详细，希望能提供更多具体步骤',
      rating: 2,
      category: '负面反馈',
      status: 'pending',
      createdAt: '2024-01-16 09:15',
      response: '',
    },
    {
      id: 3,
      user: '王五',
      content: '回答准确，但表达方式可以更简洁',
      rating: 4,
      category: '改进建议',
      status: 'processed',
      createdAt: '2024-01-17 16:45',
      response: '感谢建议，我们会优化表达方式',
    },
    {
      id: 4,
      user: '赵六',
      content: '完全解决了我的问题，非常满意',
      rating: 5,
      category: '正面反馈',
      status: 'processed',
      createdAt: '2024-01-18 11:20',
      response: '很高兴能帮到您，如有其他问题请随时咨询',
    },
  ];

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
    },
    {
      title: '用户',
      dataIndex: 'user',
      key: 'user',
    },
    {
      title: '评分',
      dataIndex: 'rating',
      key: 'rating',
      render: (rating: number) => (
        <Rate disabled value={rating} />
      ),
    },
    {
      title: '反馈内容',
      dataIndex: 'content',
      key: 'content',
      render: (text: string) => (
        <div style={{ maxWidth: '250px' }}>
          <Text ellipsis={{ tooltip: text }}>{text}</Text>
        </div>
      ),
    },
    {
      title: '分类',
      dataIndex: 'category',
      key: 'category',
      render: (category: string) => (
        <Tag color={
          category === '正面反馈' ? 'green' : 
          category === '负面反馈' ? 'red' : 'orange'
        }>
          {category}
        </Tag>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={status === 'processed' ? 'blue' : 'orange'}>
          {status === 'processed' ? '已处理' : '待处理'}
        </Tag>
      ),
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
            icon={<EyeOutlined />} 
            size="small"
            onClick={() => {
              setEditingFeedback(record);
              setIsModalVisible(true);
            }}
          >
            查看
          </Button>
          <Button type="link" icon={<EditOutlined />} size="small">回复</Button>
          <Button type="link" icon={<DeleteOutlined />} size="small" danger>删除</Button>
        </Space>
      ),
    },
  ];

  const showModal = () => {
    setIsModalVisible(true);
  };

  const handleOk = () => {
    setIsModalVisible(false);
  };

  const handleCancel = () => {
    setIsModalVisible(false);
  };

  return (
    <div>
      <div style={{ marginBottom: '24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <Title level={4}>反馈管理</Title>
          <Text type="secondary">管理用户对Agent回答的反馈，持续优化服务质量</Text>
        </div>
        <Space>
          <Button type="primary" icon={<MessageOutlined />}>
            导出反馈
          </Button>
        </Space>
      </div>

      <Card>
        <div style={{ marginBottom: '16px', display: 'flex', justifyContent: 'space-between' }}>
          <div style={{ width: '300px' }}>
            <Input placeholder="搜索反馈内容..." prefix={<SearchOutlined />} />
          </div>
          <Space>
            <Button>重置</Button>
            <Button type="primary">搜索</Button>
          </Space>
        </div>

        <Table 
          dataSource={feedbackData} 
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
        title="反馈详情"
        open={isModalVisible}
        onOk={handleOk}
        onCancel={handleCancel}
        width={700}
      >
        {editingFeedback && (
          <div>
            <div style={{ marginBottom: '16px' }}>
              <Text strong>用户: </Text>
              <Text>{editingFeedback.user}</Text>
            </div>
            <div style={{ marginBottom: '16px' }}>
              <Text strong>评分: </Text>
              <Rate disabled value={editingFeedback.rating} />
            </div>
            <div style={{ marginBottom: '16px' }}>
              <Text strong>反馈内容: </Text>
              <div style={{ marginTop: '8px' }}>
                <Text type="secondary">{editingFeedback.content}</Text>
              </div>
            </div>
            <div style={{ marginBottom: '16px' }}>
              <Text strong>分类: </Text>
              <Tag color={
                editingFeedback.category === '正面反馈' ? 'green' : 
                editingFeedback.category === '负面反馈' ? 'red' : 'orange'
              }>
                {editingFeedback.category}
              </Tag>
            </div>
            <div style={{ marginBottom: '16px' }}>
              <Text strong>状态: </Text>
              <Tag color={editingFeedback.status === 'processed' ? 'blue' : 'orange'}>
                {editingFeedback.status === 'processed' ? '已处理' : '待处理'}
              </Tag>
            </div>
            <div style={{ marginBottom: '16px' }}>
              <Text strong>创建时间: </Text>
              <Text type="secondary">{editingFeedback.createdAt}</Text>
            </div>
            {editingFeedback.response && (
              <div>
                <Text strong>回复内容: </Text>
                <div style={{ marginTop: '8px' }}>
                  <Text type="secondary">{editingFeedback.response}</Text>
                </div>
              </div>
            )}
          </div>
        )}
      </Modal>
    </div>
  );
};

export default FeedbackConfig;