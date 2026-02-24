import React, {useState} from 'react';
import {LaptopOutlined, UserOutlined, SlackOutlined} from '@ant-design/icons';
import type {MenuProps} from 'antd';
import {Breadcrumb, Layout, Menu, theme} from 'antd';
import {Link, Routes, Route, useLocation} from 'react-router-dom';
import AgentManager from './page/AgentManager';
import ChatModelConfig from './page/ChatModelConfig';
import User from './page/User';
import logo from '../assets/eaLogo.png';


const {Header, Content, Sider} = Layout;

// 定义路由配置
const routes = [
    {
        key: 'sub1',
        icon: <UserOutlined/>,
        label: 'agent管理',
        children: [
            {key: '1', label: 'agent配置', path: '/page/AgentManager', component: <AgentManager/>},
            {key: '2', label: '大模型配置', path: '/page/ToolManager', component: <ChatModelConfig/>},
            {key: '3', label: 'User配置', path: '/page/User', component: <User/>},
        ]
    },
    {
        key: 'sub2',
        icon: <LaptopOutlined/>,
        label: '工具管理',
        children: [
            {key: '5', label: 'option5', path: '/tool/option5', component: <Option5/>},
            {key: '6', label: 'option6', path: '/tool/option6', component: <Option6/>},
        ]
    }
];

// 示例组件
function Option5() {
    return <div>Option 5 Content</div>
}

function Option6() {
    return <div>Option 6 Content</div>
}

function Home() {
    return <div>欢迎欢迎</div>
}

const AppLayout = () => {
    const [collapsed, setCollapsed] = useState(false);
    const {
        token: {colorBgContainer, borderRadiusLG},
    } = theme.useToken();
    const location = useLocation();

    // 获取当前路由对应的面包屑路径
    const getBreadcrumbItems = () => {
        const breadcrumbs: { title: string }[] = [];

        // 添加首页
        // breadcrumbs.push({ title: <Link to="/">Home</Link> });

        // 查找当前路由
        for (const group of routes) {
            for (const item of group.children) {
                if (item.path === location.pathname) {
                    breadcrumbs.push(
                        {title: group.label},
                        {title: item.label}
                    );
                    break;
                }
            }
        }

        // 如果是首页，只显示Home
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
        {key: '2', label: 'nav 2'},
        {key: '3', label: 'nav 3'}
    ];

    return (
        <Layout style={{minHeight: '100vh'}}>
            <Header style={{display: 'flex', alignItems: 'center', background: '#e1ecf7', borderBottom: 'none'}}>
                {/*<div style={{ fontSize: '20px', color: '#08c' }} >easy-agent</div>*/}
                <img
                    src={logo}
                    alt="Easy Agent Logo"
                    style={{height: '50px', width: '50px', background: 'transparent'}}
                />
                <Menu
                    theme="light"
                    mode="horizontal"
                    // defaultSelectedKeys={['1']} // 默认上方导航栏不选
                    items={items1}
                    style={{flex: 1, minWidth: 0, background: '#e1ecf7', borderBottom: 'none'}}
                />
            </Header>
            <Layout>
                <Sider width={200} style={{background: '#e1ecf7'}} collapsible collapsed={collapsed}
                       onCollapse={(value) => setCollapsed(value)}
                       trigger={<span style={{fontSize: '16px'}}>{collapsed ? '>' : '<'}</span>}>
                    <Menu
                        theme="light"
                        mode="inline"
                        defaultSelectedKeys={[location.pathname === '/' ? '1' : '']}
                        style={{height: '100%', borderRight: 0, background: '#e1ecf7'}}
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
                            background: '#e1ecf7',
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
                        </Routes>
                    </Content>
                </Layout>
            </Layout>
        </Layout>
    );
};

export default AppLayout;
