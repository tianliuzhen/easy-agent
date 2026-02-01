import React from 'react';
import { Card, Typography, Button, Space, Table, Input, Tag } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, SearchOutlined } from '@ant-design/icons';

const { Title, Text } = Typography;

const KnowledgeBaseConfig: React.FC<{ agentId?: number }> = ({ agentId }) => {
  // 模拟知识库数据
  const knowledgeData = [
    {
      id: 1,
      name: '产品知识库',
      description: '关于产品的常见问题和解答',
      status: 'active',
      createdAt: '2024-01-15',
      size: '1.2 MB',
    },
    {
      id: 2,
      name: '技术文档库',
      description: '技术规格和API文档',
      status: 'active',
      createdAt: '2024-01-20',
      size: '3.4 MB',
    },
    {
      id: 3,
      name: '客户案例库',
      description: '客户成功案例和解决方案',
      status: 'inactive',
      createdAt: '2024-02-01',
      size: '2.1 MB',
    },
  ];

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
    },
    {
      title: '知识库名称',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
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
      title: '大小',
      dataIndex: 'size',
      key: 'size',
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
    },
    {
      title: '操作',
      key: 'action',
      render: () => (
        <Space size="middle">
          <Button type="link" icon={<EditOutlined />} size="small">编辑</Button>
          <Button type="link" icon={<DeleteOutlined />} size="small" danger>删除</Button>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: '24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <Title level={4}>知识库管理</Title>
          <Text type="secondary">管理Agent的知识库，提升回答准确性</Text>
        </div>
        <Space>
          <Button type="primary" icon={<PlusOutlined />}>
            新建知识库
          </Button>
        </Space>
      </div>

      <Card>
        <div style={{ marginBottom: '16px', display: 'flex', justifyContent: 'space-between' }}>
          <div style={{ width: '300px' }}>
            <Input placeholder="搜索知识库..." prefix={<SearchOutlined />} />
          </div>
        </div>

        <Table 
          dataSource={knowledgeData} 
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
    </div>
  );
};

export default KnowledgeBaseConfig;