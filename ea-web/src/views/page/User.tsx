import React, {useState, useEffect} from 'react';
import {Table, Card, Tag, Space, Button, message, Modal, Form, Input, Switch} from 'antd';
import {UserOutlined, ReloadOutlined, EditOutlined} from '@ant-design/icons';
import {authApi} from '../api/AuthApi';

interface UserData {
    id: number;
    username: string;
    email?: string;
    phone?: string;
    status: number;
    createdAt: string;
    updatedAt: string;
}

interface CurrentUser {
    id: number;
    username: string;
    email?: string;
    phone?: string;
    roles?: string[];
    permissions?: string[];
}

/**
 * 用户列表页面
 *
 * @author Claude Code
 * @version 1.0 User.tsx  2026/04/19
 */
const UserList: React.FC = () => {
    const [userList, setUserList] = useState<UserData[]>([]);
    const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
    const [loading, setLoading] = useState(false);
    const [editModalVisible, setEditModalVisible] = useState(false);
    const [editingUser, setEditingUser] = useState<UserData | null>(null);
    const [form] = Form.useForm();

    // 检查是否为管理员
    const isAdmin = currentUser?.roles?.includes('ADMIN') || false;

    // 加载用户列表
    const loadUserList = async () => {
        setLoading(true);
        try {
            const response = await authApi.getUserList();
            if (response.code === '0' || response.success === true) {
                setUserList(response.data || []);
            } else {
                message.error(response.message || '获取用户列表失败');
            }
        } catch (error) {
            console.error('获取用户列表失败:', error);
            message.error('获取用户列表失败');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadUserList();
        loadCurrentUser();
    }, []);

    // 加载当前用户信息
    const loadCurrentUser = async () => {
        try {
            const response = await authApi.getCurrentUser();
            if (response.code === '0' || response.success === true) {
                setCurrentUser(response.data);
            }
        } catch (error) {
            console.error('获取当前用户信息失败:', error);
        }
    };

    // 打开编辑模态框
    const handleEdit = (record: UserData) => {
        setEditingUser(record);
        form.setFieldsValue({
            email: record.email,
            phone: record.phone,
            status: record.status === 1,
        });
        setEditModalVisible(true);
    };

    // 提交编辑
    const handleUpdate = async () => {
        try {
            const values = await form.validateFields();

            if (!editingUser) return;

            const userData: any = {
                id: editingUser.id,
                email: values.email,
                phone: values.phone,
                status: values.status ? 1 : 0,
            };

            // 如果输入了新密码，则添加到更新数据中
            if (values.password && values.password.trim()) {
                userData.password = values.password;
            }

            const response = await authApi.updateUser(userData);

            if (response.code === '0' || response.success === true) {
                message.success('更新成功');
                setEditModalVisible(false);
                form.resetFields();
                loadUserList(); // 刷新列表
            } else {
                message.error(response.message || '更新失败');
            }
        } catch (error) {
            console.error('更新用户失败:', error);
            message.error('更新失败');
        }
    };

    // 表格列定义
    const columns = [
        {
            title: 'ID',
            dataIndex: 'id',
            key: 'id',
            width: 80,
        },
        {
            title: '用户名',
            dataIndex: 'username',
            key: 'username',
            width: 150,
        },
        {
            title: '邮箱',
            dataIndex: 'email',
            key: 'email',
            width: 200,
            render: (email: string) => email || '-',
        },
        {
            title: '手机号',
            dataIndex: 'phone',
            key: 'phone',
            width: 150,
            render: (phone: string) => phone || '-',
        },
        {
            title: '状态',
            dataIndex: 'status',
            key: 'status',
            width: 100,
            render: (status: number) => (
                <Tag color={status === 1 ? 'green' : 'red'}>
                    {status === 1 ? '正常' : '禁用'}
                </Tag>
            ),
        },
        {
            title: '创建时间',
            dataIndex: 'createdAt',
            key: 'createdAt',
            width: 180,
            render: (date: string) => date ? new Date(date).toLocaleString('zh-CN') : '-',
        },
        {
            title: '更新时间',
            dataIndex: 'updatedAt',
            key: 'updatedAt',
            width: 180,
            render: (date: string) => date ? new Date(date).toLocaleString('zh-CN') : '-',
        },
        ...(isAdmin ? [{
            title: '操作',
            key: 'action',
            width: 100,
            fixed: 'right' as const,
            render: (_: any, record: UserData) => (
                <Button
                    type="link"
                    icon={<EditOutlined/>}
                    onClick={() => handleEdit(record)}
                >
                    编辑
                </Button>
            ),
        }] : []),
    ];

    return (
        <div style={{height: '100%', display: 'flex', flexDirection: 'column'}}>
            <Card
                bordered={false}
                title={
                    <Space>
                        <UserOutlined/>
                        <span>用户列表</span>
                        {isAdmin && <Tag color="blue">管理员</Tag>}
                    </Space>
                }
                extra={
                    <Button
                        icon={<ReloadOutlined/>}
                        onClick={loadUserList}
                        loading={loading}
                    >
                        刷新
                    </Button>
                }
                style={{flex: 1, display: 'flex', flexDirection: 'column', margin: 0}}
                bodyStyle={{flex: 1, padding: 0, overflow: 'auto'}}
            >
                <Table
                    columns={columns}
                    dataSource={userList}
                    rowKey="id"
                    loading={loading}
                    pagination={{
                        pageSize: 10,
                        showSizeChanger: true,
                        showTotal: (total) => `共 ${total} 条`,
                    }}
                    scroll={{x: 1200}}
                />
            </Card>

            {/* 编辑用户模态框 */}
            <Modal
                title="编辑用户"
                open={editModalVisible}
                onOk={handleUpdate}
                onCancel={() => {
                    setEditModalVisible(false);
                    form.resetFields();
                }}
                width={500}
            >
                <Form
                    form={form}
                    layout="vertical"
                >
                    <Form.Item
                        label="用户名"
                    >
                        <Input value={editingUser?.username} disabled/>
                    </Form.Item>
                    <Form.Item
                        name="email"
                        label="邮箱"
                        rules={[
                            {type: 'email', message: '请输入有效的邮箱地址'},
                        ]}
                    >
                        <Input placeholder="请输入邮箱"/>
                    </Form.Item>
                    <Form.Item
                        name="phone"
                        label="手机号"
                        rules={[
                            {pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号'},
                        ]}
                    >
                        <Input placeholder="请输入手机号"/>
                    </Form.Item>
                    <Form.Item
                        name="password"
                        label="新密码"
                        extra="留空则不修改密码"
                    >
                        <Input.Password placeholder="请输入新密码（可选）"/>
                    </Form.Item>
                    <Form.Item
                        name="status"
                        label="状态"
                        valuePropName="checked"
                    >
                        <Switch checkedChildren="正常" unCheckedChildren="禁用"/>
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    );
};

export default UserList;
