import React, {useState, useEffect} from 'react';
import {
    LaptopOutlined,
    UserOutlined,
    SlackOutlined,
    RobotOutlined,
    ToolOutlined,
    ApiOutlined,
    LogoutOutlined,
    BookOutlined,
    ThunderboltOutlined
} from '@ant-design/icons';
import type {MenuProps} from 'antd';
import {Breadcrumb, Layout, Menu, theme, Avatar, Dropdown, message, Space} from 'antd';
import {Link, Routes, Route, useLocation, useNavigate} from 'react-router-dom';
import AgentManager from './page/AgentManager';
import ChatModelConfig from './page/ChatModelConfig';
import User from './page/User';
import ToolManager from './page/ToolManager';
import SQLConfig from './page/agent/tool/SQLConfig';
import HTTPConfig from './page/agent/tool/HTTPConfig';
import MCPConfig from './page/agent/tool/MCPConfig';
import GRPCConfig from './page/agent/tool/GRPCConfig';
import KnowledgeBaseList from './page/agent/knowledge/KnowledgeBaseList';
import MCPMarket from './page/mcp/MCPMarket';
import MyMCP from './page/mcp/MyMCP';
import SkillMarket from './page/skill/SkillMarket';
import MySkill from './page/skill/MySkill';
import logo from '../assets/eaLogo.png';
import AuthGuard from '../components/AuthGuard';
import {authApi} from './api/AuthApi';


const {Header, Content, Sider} = Layout;

// 定义路由配置
const routes = [
    {
        key: 'sub1',
        icon: <RobotOutlined/>,
        label: 'agent 应用',
        children: [
            {
                key: '1',
                label: '我的 agent',
                path: '/page/AgentManager',
                component: <AuthGuard><AgentManager/></AuthGuard>
            },
            {
                key: '2',
                label: 'agent 市场',
                path: '/page/AgentManager',
                component: <AuthGuard><AgentManager/></AuthGuard>
            },
        ]
    },
    {
        key: 'sub5',
        icon: <BookOutlined/>,
        label: '知识库管理',
        children: [
            {
                key: '4',
                label: '我的知识库',
                path: '/page/knowledge/KnowledgeBaseList',
                component: <AuthGuard><KnowledgeBaseList/></AuthGuard>
            },
        ]
    },
    {
        key: 'sub2',
        icon: <ToolOutlined/>,
        label: '工具管理',
        children: [
            {key: '5', label: '默认工具', path: '/toolManager', component: <AuthGuard><ToolManager mode="default" /></AuthGuard>},
            {key: '10', label: '我的工具', path: '/myToolManager', component: <AuthGuard><ToolManager mode="my" /></AuthGuard>},
        ],
    },
    {
        key: 'sub6',
        icon: <ApiOutlined/>,
        label: 'MCP 管理',
        children: [
            {key: '6', label: '我的mcp', path: '/page/mcp/my', component: <AuthGuard><MyMCP/></AuthGuard>},
            {key: '7', label: 'mcp市场', path: '/page/mcp/market', component: <AuthGuard><MCPMarket/></AuthGuard>},
        ]
    },
    {
        key: 'sub7',
        icon: <ThunderboltOutlined/>,
        label: 'Skill 技能',
        children: [
            {key: '8', label: '我的Skill', path: '/page/skill/my', component: <AuthGuard><MySkill/></AuthGuard>},
            {key: '9', label: 'Skill市场', path: '/page/skill/market', component: <AuthGuard><SkillMarket/></AuthGuard>},
        ]
    },
    {
        key: 'sub3',
        icon: <SlackOutlined/>,
        label: '模型平台',
        children: [
            {
                key: '2',
                label: '模型配置',
                path: '/page/chatModelConfig',
                component: <AuthGuard><ChatModelConfig/></AuthGuard>
            },
        ]
    },
    {
        key: 'sub4',
        icon: <UserOutlined/>,
        label: '用户配置',
        children: [
            {key: '3', label: 'User 配置', path: '/page/User', component: <AuthGuard><User/></AuthGuard>},
        ]
    },
];

function Home() {
    return <div>欢迎欢迎</div>
}

const AppLayout = () => {
    const [collapsed, setCollapsed] = useState(false);
    const [currentUser, setCurrentUser] = useState<{
        id: number,
        username: string,
        email?: string,
        phone?: string
    } | null>(null);
    const {
        token: {borderRadiusLG},
    } = theme.useToken();
    const location = useLocation();
    const navigate = useNavigate();

    // 获取当前用户信息
    useEffect(() => {
        const fetchUserInfo = async () => {
            try {
                const response = await authApi.getCurrentUser();
                if (response.code === '0' || response.success === true) {
                    setCurrentUser(response.data);
                }
            } catch (error) {
                console.error('获取用户信息失败:', error);
            }
        };
        fetchUserInfo();
    }, []);

    // 处理登出
    const handleLogout = async () => {
        console.log('=== 开始登出流程 ===');
        try {
            console.log('1. 调用 authApi.logout()');
            const response = await authApi.logout();
            console.log('2. 登出接口响应:', response);

            if (response.code === '0' || response.success === true) {
                console.log('3. 登出成功，清除本地 token');
                // 清除本地存储的 token
                localStorage.removeItem('AUTH_TOKEN');
                sessionStorage.removeItem('AUTH_TOKEN');
                message.success('登出成功');
                console.log('4. 跳转到登录页');
                navigate('/login');
            } else {
                console.error('登出失败，响应码:', response.code, response.success);
                message.error('登出失败');
            }
        } catch (error) {
            console.error('登出异常:', error);
            message.error('登出失败，请稍后重试');
        }
    };

    // 用户菜单下拉选项
    const userMenuItems: MenuProps['items'] = [
        {
            key: 'profile',
            icon: <UserOutlined/>,
            label: '个人设置',
            onClick: () => navigate('/page/User'),
        },
        {
            type: 'divider' as const,
        },
        {
            key: 'logout',
            icon: <LogoutOutlined/>,
            label: '退出登录',
            onClick: handleLogout,
        },
    ];

    // 获取当前路由对应的面包屑路径
    const getBreadcrumbItems = () => {
        const breadcrumbs: { title: string }[] = [];

        // 查找当前路由
        for (const group of routes) {
            for (const item of group.children) {
                if (item.path === location.pathname) {
                    breadcrumbs.push(
                        {title: group.label},
                        {title: item.label}
                    );
                    return breadcrumbs;
                }
            }
        }

        // 处理工具管理的子路由（如 /toolManager/tool/sql）
        if (location.pathname.startsWith('/toolManager/tool/')) {
            breadcrumbs.push(
                {title: '工具管理'},
                {title: '默认工具'},
                {title: '工具配置'}
            );
            return breadcrumbs;
        }

        if (location.pathname.startsWith('/myToolManager/tool/')) {
            breadcrumbs.push(
                {title: '工具管理'},
                {title: '我的工具'},
                {title: '工具配置'}
            );
            return breadcrumbs;
        }

        // 如果是首页，只显示 Home
        if (location.pathname === '/') {
            return [{title: 'Home'}];
        }

        return breadcrumbs;
    };

    const getMenuItems = (): MenuProps['items'] => {
        return routes.map(route => ({
            key: route.key,
            icon: route.icon,
            label: route.label,
            children: route.children.map(child => ({
                key: child.key,
                label: <Link to={child.path}>{child.label}</Link>
            }))
        }));
    };

    const items1: MenuProps['items'] = [
        {key: '1', label: <Link to="/home/appBack">首页</Link>},
        {key: '2', label: '空间1'},
        {key: '3', label: '空间2'}
    ];

    return (
        <Layout style={{minHeight: '100vh'}}>
            <Header style={{
                display: 'flex',
                alignItems: 'center',
                background: 'var(--ea-theme-background)',
                borderBottom: 'none',
                justifyContent: 'space-between',
                padding: '0 24px',
                height: '64px'
            }}>
                <div style={{display: 'flex', alignItems: 'center', gap: '16px'}}>
                    <img
                        src={logo}
                        alt="Easy Agent Logo"
                        style={{height: '50px', width: '50px', background: 'transparent'}}
                    />
                    <Menu
                        theme="light"
                        mode="horizontal"
                        items={items1}
                        style={{flex: 1, minWidth: 0, background: 'var(--ea-theme-background)', borderBottom: 'none'}}
                    />
                </div>

                {/* 右上角用户信息 */}
                <Dropdown menu={{items: userMenuItems}} placement="bottomRight" trigger={['click']}>
                    <Space style={{cursor: 'pointer', padding: '0px', display: 'flex', alignItems: 'center'}}>
                        <span style={{fontSize: '14px', marginLeft: '0px'}}>
                            {currentUser?.username || '用户'}
                        </span>
                        <Avatar style={{backgroundColor: '#1890ff'}} icon={<UserOutlined/>}/>
                    </Space>
                </Dropdown>
            </Header>
            <Layout>
                <Sider width={200} style={{background: 'var(--ea-theme-background)'}} collapsible collapsed={collapsed}
                       onCollapse={(value) => setCollapsed(value)}
                       trigger={<span style={{fontSize: '16px'}}>{collapsed ? '>' : '<'}</span>}>
                    <Menu
                        theme="light"
                        mode="inline"
                        defaultSelectedKeys={[location.pathname === '/' ? '1' : '']}
                        style={{height: '100%', borderRight: 0, background: 'var(--ea-theme-background)'}}
                        items={getMenuItems()}
                    />
                </Sider>
                <Layout style={{padding: '0 10px 10px'}}>
                    <Breadcrumb
                        items={getBreadcrumbItems()}
                        style={{margin: '10px 0'}}
                    />
                    <Content
                        style={{
                            padding: 10,
                            margin: 0,
                            minHeight: 280,
                            background: '#fff',
                            borderRadius: borderRadiusLG,
                        }}
                    >
                        <Routes>
                            <Route path="/" element={<Home/>}/>
                            {routes.flatMap(route =>
                                route.children.map(child => (
                                    <Route key={child.key} path={child.path} element={child.component}/>
                                ))
                            )}
                            {/* 工具管理的子路由 */}
                            <Route path="/toolManager/tool/:type" element={<AuthGuard><ToolManager mode="default" /></AuthGuard>} />
                            <Route path="/myToolManager/tool/:type" element={<AuthGuard><ToolManager mode="my" /></AuthGuard>} />
                        </Routes>
                    </Content>
                </Layout>
            </Layout>
        </Layout>
    );
};

export default AppLayout;
