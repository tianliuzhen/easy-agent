import React, { useState, useEffect } from 'react';
import { 
  Table, Button, Space, Modal, Form, Input, InputNumber, Switch, 
  message, Popconfirm, Tag, Tooltip, Drawer 
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, CopyOutlined } from '@ant-design/icons';
import { modelPlatformApi } from '../../api/ModelPlatformApi';

interface ModelPlatform {
  id?: number;
  modelPlatform: string;
  modelDesc: string;
  icon: string;
  officialWebsite: string;
  baseUrl: string;
  modelVersions: string | string[];
  isActive: boolean;
  sortOrder: number;
}

const ModelPlatformConfig: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<ModelPlatform[]>([]);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingRecord, setEditingRecord] = useState<ModelPlatform | null>(null);
  const [form] = Form.useForm();

  // 加载模型平台列表
  const loadData = async () => {
    setLoading(true);
    try {
      const result = await modelPlatformApi.list();
      if (result && (result.code === 200 || result.success === true)) {
        const platformList = Object.values(result.data || {}).map((item: any) => ({
          ...item,
          modelVersions: item.modelVersions || [],
        }));
        setData(platformList);
      } else {
        message.error('加载模型平台列表失败');
        setData([]);
      }
    } catch (error) {
      console.error('加载模型平台列表失败:', error);
      message.error('加载模型平台列表失败');
      setData([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  // 打开新增/编辑弹窗
  const showModal = (record?: ModelPlatform) => {
    setEditingRecord(record || null);
    if (record) {
      // 编辑模式，解析 modelVersions
      let versions: string[] = [];
      if (typeof record.modelVersions === 'string') {
        try {
          versions = JSON.parse(record.modelVersions);
        } catch (e) {
          versions = record.modelVersions.split(',').map((v: string) => v.trim());
        }
      } else if (Array.isArray(record.modelVersions)) {
        versions = record.modelVersions;
      }
      
      form.setFieldsValue({
        ...record,
        modelVersionArray: versions,
      });
    } else {
      // 新增模式
      form.resetFields();
      form.setFieldsValue({
        isActive: true,
        sortOrder: 0,
        modelVersionArray: [],
      });
    }
    setModalVisible(true);
  };

  // 保存数据
  const handleSave = async () => {
    try {
      const values = await form.validateFields();
      
      // 构建保存的数据
      const saveData: any = {
        ...values,
      };
      
      // 如果有 ID，则是更新
      if (editingRecord?.id) {
        saveData.id = editingRecord.id;
      }
      
      // 将数组转换为字符串存储
      if (values.modelVersionArray && Array.isArray(values.modelVersionArray)) {
        saveData.modelVersionArray = values.modelVersionArray;
      }
      
      const result = await modelPlatformApi.save(saveData);
      if (result && (result.code === 200 || result.success === true)) {
        message.success(editingRecord ? '更新成功' : '新增成功');
        setModalVisible(false);
        loadData();
      } else {
        message.error(result?.message || '保存失败');
      }
    } catch (error: any) {
      if (error.message) {
        message.error(error.message);
      }
    }
  };

  // 删除数据
  const handleDelete = async (id: number) => {
    try {
      const result = await modelPlatformApi.delete(id);
      if (result && (result.code === 200 || result.success === true)) {
        message.success('删除成功');
        loadData();
      } else {
        message.error(result?.message || '删除失败');
      }
    } catch (error) {
      message.error('删除失败');
    }
  };

  // 切换启用状态
  const handleToggleActive = async (record: ModelPlatform) => {
    try {
      const result = await modelPlatformApi.updateActiveStatus(
        record.id as number,
        !record.isActive
      );
      if (result && (result.code === 200 || result.success === true)) {
        message.success('更新成功');
        loadData();
      } else {
        message.error(result?.message || '更新失败');
      }
    } catch (error) {
      message.error('更新失败');
    }
  };

  // 复制数据
  const handleCopy = (record: ModelPlatform) => {
    const newData = { ...record };
    delete newData.id;
    newData.modelPlatform = `${newData.modelPlatform}_copy`;
    newData.modelDesc = `${newData.modelDesc} (副本)`;
    showModal(newData);
  };

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 60,
    },
    {
      title: '平台标识',
      dataIndex: 'modelPlatform',
      key: 'modelPlatform',
      width: 150,
    },
    {
      title: '描述',
      dataIndex: 'modelDesc',
      key: 'modelDesc',
      width: 150,
    },
    {
      title: '图标',
      dataIndex: 'icon',
      key: 'icon',
      width: 100,
      render: (icon: string) => (
        <img src={icon} alt="icon" style={{ width: 32, height: 32 }} />
      ),
    },
    {
      title: '官网链接',
      dataIndex: 'officialWebsite',
      key: 'officialWebsite',
      width: 200,
      ellipsis: true,
    },
    {
      title: '基础 URL',
      dataIndex: 'baseUrl',
      key: 'baseUrl',
      width: 200,
      ellipsis: true,
    },
    {
      title: '模型版本',
      dataIndex: 'modelVersions',
      key: 'modelVersions',
      width: 200,
      render: (versions: string | string[]) => {
        let versionArray: string[] = [];
        if (typeof versions === 'string') {
          try {
            versionArray = JSON.parse(versions);
          } catch (e) {
            versionArray = versions.split(',');
          }
        } else if (Array.isArray(versions)) {
          versionArray = versions;
        }
        return (
          <Space wrap>
            {versionArray.slice(0, 3).map((v, idx) => (
              <Tag key={idx}>{v}</Tag>
            ))}
            {versionArray.length > 3 && (
              <Tooltip title={versionArray.join(', ')}>
                <Tag>+{versionArray.length - 3}</Tag>
              </Tooltip>
            )}
          </Space>
        );
      },
    },
    {
      title: '状态',
      dataIndex: 'isActive',
      key: 'isActive',
      width: 80,
      render: (isActive: boolean) => (
        <Tag color={isActive ? 'green' : 'red'}>
          {isActive ? '启用' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '排序',
      dataIndex: 'sortOrder',
      key: 'sortOrder',
      width: 70,
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      render: (_: any, record: ModelPlatform) => (
        <Space size="small">
          <Tooltip title="编辑">
            <Button
              type="link"
              size="small"
              icon={<EditOutlined />}
              onClick={() => showModal(record)}
            />
          </Tooltip>
          <Tooltip title="复制">
            <Button
              type="link"
              size="small"
              icon={<CopyOutlined />}
              onClick={() => handleCopy(record)}
            />
          </Tooltip>
          <Popconfirm
            title="确认删除"
            description="确定要删除该模型平台吗？"
            onConfirm={() => record.id && handleDelete(record.id)}
            okText="确认"
            cancelText="取消"
          >
            <Tooltip title="删除">
              <Button
                type="link"
                size="small"
                danger
                icon={<DeleteOutlined />}
              />
            </Tooltip>
          </Popconfirm>
          <Tooltip title={record.isActive ? '禁用' : '启用'}>
            <Switch
              size="small"
              checked={record.isActive}
              onChange={() => handleToggleActive(record)}
            />
          </Tooltip>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: '24px' }}>
      <div style={{ marginBottom: 16 }}>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => showModal()}
        >
          新增模型平台
        </Button>
      </div>
      
      <Table
        loading={loading}
        columns={columns}
        dataSource={data}
        rowKey="id"
        pagination={{ pageSize: 10 }}
      />

      <Modal
        title={editingRecord ? '编辑模型平台' : '新增模型平台'}
        open={modalVisible}
        onOk={handleSave}
        onCancel={() => setModalVisible(false)}
        width={700}
        destroyOnClose
      >
        <Form
          form={form}
          layout="vertical"
          initialValues={{
            isActive: true,
            sortOrder: 0,
          }}
        >
          <Form.Item
            name="modelPlatform"
            label="平台标识"
            rules={[{ required: true, message: '请输入平台标识' }]}
          >
            <Input placeholder="如：deepseek, siliconflow, openai, ollama" disabled={!!editingRecord} />
          </Form.Item>
          
          <Form.Item
            name="modelDesc"
            label="描述"
            rules={[{ required: true, message: '请输入描述' }]}
          >
            <Input placeholder="如：硅基流动" />
          </Form.Item>
          
          <Form.Item
            name="icon"
            label="图标 URL"
            rules={[{ required: true, message: '请输入图标 URL' }]}
          >
            <Input placeholder="https://..." />
          </Form.Item>
          
          <Form.Item
            name="officialWebsite"
            label="官网链接"
            rules={[{ required: true, message: '请输入官网链接' }]}
          >
            <Input placeholder="https://..." />
          </Form.Item>
          
          <Form.Item
            name="baseUrl"
            label="基础 API URL"
            rules={[{ required: true, message: '请输入基础 API URL' }]}
          >
            <Input placeholder="https://api.xxx.com/v1" />
          </Form.Item>
          
          <Form.Item
            name="modelVersionArray"
            label="模型版本"
            tooltip="每行输入一个模型版本号"
          >
            <Input.TextArea 
              rows={4} 
              placeholder="每行一个模型版本号，例如：&#10;gpt-3.5-turbo&#10;gpt-4"
            />
          </Form.Item>
          
          <Form.Item
            name="sortOrder"
            label="排序顺序"
            initialValue={0}
          >
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          
          <Form.Item
            name="isActive"
            label="是否启用"
            valuePropName="checked"
            initialValue={true}
          >
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default ModelPlatformConfig;
