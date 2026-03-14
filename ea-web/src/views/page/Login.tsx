import React, {useState, useEffect} from 'react';
import {Form, Input, Button, Card, message, Alert} from 'antd';
import {UserOutlined, LockOutlined} from '@ant-design/icons';
import {authApi} from '../api/AuthApi';
import {useNavigate, useLocation} from 'react-router-dom';

/**
 * 登录页面
 *
 * @author Claude Code
 * @version 1.0 Login.tsx  2026/03/14
 */
const Login: React.FC = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const [loading, setLoading] = useState(false);
    const [showRedirectAlert, setShowRedirectAlert] = useState(false);

    // 检查是否是重定向到登录页面
    useEffect(() => {
        const params = new URLSearchParams(location.search);
        const redirectParam = params.get('redirect');

        if (redirectParam) {
            setShowRedirectAlert(true);
            // 清理URL参数，避免刷新页面时再次显示提示
            const newUrl = new URL(window.location.href);
            newUrl.searchParams.delete('redirect');
            window.history.replaceState({}, '', newUrl.toString());
        }
    }, [location.search]);

    // 从查询参数获取重定向目标
    const getRedirectPath = () => {
        const params = new URLSearchParams(location.search);
        const redirectParam = params.get('redirect');
        console.log('原始重定向参数:', redirectParam);
        console.log('location.search:', location.search);

        if (redirectParam) {
            try {
                const decodedPath = decodeURIComponent(redirectParam);
                console.log('解码后路径:', decodedPath);
                // 安全检查：确保是内部路径
                if (decodedPath.startsWith('/')) {
                    return decodedPath;
                } else {
                    console.warn('重定向路径不是内部路径:', decodedPath);
                }
            } catch (error) {
                console.error('解析重定向路径失败:', error);
            }
        }
        console.log('使用默认重定向路径: /');
        return '/'; // 默认跳转到首页
    };

    const onFinish = async (values: any) => {
        setLoading(true);
        try {
            // 登录前先清除旧的 token
            localStorage.removeItem('AUTH_TOKEN');
            sessionStorage.removeItem('AUTH_TOKEN');

            const response = await authApi.login(values.username, values.password);
            console.log('登录响应:', response);
            console.log('重定向路径:', getRedirectPath());

            // 兼容两种响应格式：code 或 success
            const isSuccess = response.code === '0' || response.success === true;

            if (isSuccess) {
                // 登录成功后将 token 存储到 localStorage（主要方式）
                if (response.data && response.data.token) {
                    localStorage.setItem('AUTH_TOKEN', response.data.token);
                    console.log('✓ Token saved to localStorage:', response.data.token.substring(0, 10) + '...');
                }

                // Cookie 由后端自动设置（HttpOnly），作为兜底方案
                setTimeout(() => {
                    console.log('登录后检查:');
                    console.log('  - localStorage token:', localStorage.getItem('AUTH_TOKEN')?.substring(0, 10) + '...');
                    console.log('  - document.cookie:', document.cookie ? document.cookie.substring(0, 50) + '...' : '无值');
                }, 100);

                message.success('登录成功');
                // 登录成功后跳转到原始页面或首页
                const redirectPath = getRedirectPath();
                console.log('准备跳转到:', redirectPath);
                navigate(redirectPath);
            } else {
                message.error(response.message || '登录失败');
            }
        } catch (error) {
            message.error('登录失败，请稍后重试');
            console.error('Login error:', error);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div style={{
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            minHeight: '100vh',
            background: `
        radial-gradient(ellipse at 30% 20%, rgba(90, 130, 180, 0.5) 0%, transparent 50%),
        radial-gradient(ellipse at 70% 80%, rgba(100, 140, 190, 0.45) 0%, transparent 50%),
        radial-gradient(ellipse at 50% 50%, rgba(80, 120, 170, 0.35) 0%, transparent 60%),
        linear-gradient(180deg, #b8d4e8 0%, #9ec5db 50%, #a8cce0 100%)
      `,
            backgroundSize: '100% 100%',
            position: 'relative',
            overflow: 'hidden',
        }}>
            {/* 网格背景 */}
            <div style={{
                position: 'absolute',
                top: 0,
                left: 0,
                right: 0,
                bottom: 0,
                backgroundImage: `
          linear-gradient(rgba(60, 100, 150, 0.05) 1px, transparent 1px),
          linear-gradient(90deg, rgba(60, 100, 150, 0.05) 1px, transparent 1px)
        `,
                backgroundSize: '60px 60px',
                pointerEvents: 'none',
            }}/>
            {/* 底部光晕 */}
            <div style={{
                position: 'absolute',
                top: 0,
                left: 0,
                right: 0,
                height: '30%',
                background: 'linear-gradient(180deg, rgba(100, 150, 200, 0.08) 0%, transparent 100%)',
                pointerEvents: 'none',
            }}/>
            {/* 流动线条 1 */}
            <div style={{
                position: 'absolute',
                top: '10%',
                left: '-10%',
                width: '60%',
                height: 2,
                background: 'linear-gradient(90deg, transparent 0%, rgba(255, 255, 255, 0.6) 50%, transparent 100%)',
                animation: 'scanLine 8s linear infinite',
                boxShadow: '0 0 20px rgba(255, 255, 255, 0.4)',
            }}/>
            {/* 流动线条 2 */}
            <div style={{
                position: 'absolute',
                bottom: '20%',
                right: '-10%',
                width: '50%',
                height: 1,
                background: 'linear-gradient(90deg, transparent 0%, rgba(255, 255, 255, 0.5) 50%, transparent 100%)',
                animation: 'scanLine 12s linear infinite reverse',
                boxShadow: '0 0 15px rgba(255, 255, 255, 0.3)',
            }}/>
            {/* 流动线条 3 */}
            <div style={{
                position: 'absolute',
                top: '50%',
                left: '-5%',
                width: '40%',
                height: 1,
                background: 'linear-gradient(90deg, transparent 0%, rgba(255, 255, 255, 0.4) 50%, transparent 100%)',
                animation: 'scanLine 15s linear infinite',
                boxShadow: '0 0 10px rgba(255, 255, 255, 0.25)',
            }}/>
            {/* 极光光晕 */}
            <div style={{
                position: 'absolute',
                top: '5%',
                left: '10%',
                width: 500,
                height: 400,
                borderRadius: '60% 40% 50% 50% / 50% 60% 40% 50%',
                background: 'radial-gradient(ellipse at center, rgba(80, 130, 190, 0.35) 0%, rgba(100, 150, 210, 0.2) 40%, transparent 70%)',
                filter: 'blur(30px)',
                animation: 'aurora1 12s ease-in-out infinite, breathe 6s ease-in-out infinite',
            }}/>
            <div style={{
                position: 'absolute',
                bottom: '10%',
                right: '5%',
                width: 450,
                height: 350,
                borderRadius: '40% 60% 45% 55% / 55% 45% 60% 40%',
                background: 'radial-gradient(ellipse at center, rgba(70, 120, 180, 0.3) 0%, rgba(90, 140, 200, 0.15) 40%, transparent 70%)',
                filter: 'blur(35px)',
                animation: 'aurora2 15s ease-in-out infinite, breathe 8s ease-in-out infinite reverse',
            }}/>
            <div style={{
                position: 'absolute',
                top: '40%',
                left: '50%',
                width: 300,
                height: 250,
                borderRadius: '50% 50% 45% 55% / 45% 55% 50% 50%',
                background: 'radial-gradient(ellipse at center, rgba(90, 140, 195, 0.25) 0%, rgba(110, 160, 220, 0.12) 40%, transparent 70%)',
                filter: 'blur(25px)',
                animation: 'aurora3 10s ease-in-out infinite, breathe 5s ease-in-out infinite',
            }}/>
            {/* 颗粒质感 */}
            <div style={{
                position: 'absolute',
                top: 0,
                left: 0,
                right: 0,
                bottom: 0,
                backgroundImage: `url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%' height='100%' filter='url(%23noise)'/%3E%3C/svg%3E")`,
                opacity: 0.05,
                pointerEvents: 'none',
                animation: 'grainMove 0.5s steps(10) infinite',
            }}/>
            {/* 星光效果 */}
            <div style={{
                position: 'absolute',
                top: 0,
                left: 0,
                right: 0,
                bottom: 0,
                backgroundImage: `
          radial-gradient(2px 2px at 15% 25%, rgba(255, 255, 255, 0.9), transparent),
          radial-gradient(3px 3px at 35% 60%, rgba(255, 255, 255, 0.7), transparent),
          radial-gradient(2px 2px at 55% 15%, rgba(255, 255, 255, 0.8), transparent),
          radial-gradient(3px 3px at 75% 45%, rgba(255, 255, 255, 0.6), transparent),
          radial-gradient(2px 2px at 25% 80%, rgba(255, 255, 255, 0.7), transparent),
          radial-gradient(3px 3px at 85% 10%, rgba(255, 255, 255, 0.8), transparent),
          radial-gradient(2px 2px at 45% 75%, rgba(255, 255, 255, 0.6), transparent),
          radial-gradient(2px 2px at 10% 50%, rgba(255, 255, 255, 0.7), transparent),
          radial-gradient(3px 3px at 65% 85%, rgba(255, 255, 255, 0.8), transparent),
          radial-gradient(2px 2px at 90% 70%, rgba(255, 255, 255, 0.5), transparent),
          radial-gradient(2px 2px at 30% 35%, rgba(255, 255, 255, 0.7), transparent),
          radial-gradient(3px 3px at 50% 40%, rgba(255, 255, 255, 0.6), transparent),
          radial-gradient(2px 2px at 80% 90%, rgba(255, 255, 255, 0.6), transparent),
          radial-gradient(2px 2px at 20% 55%, rgba(255, 255, 255, 0.8), transparent)
        `,
                animation: 'twinkle 4s ease-in-out infinite',
                pointerEvents: 'none',
            }}/>
            {/* 边框光效 - 左上角 */}
            <div style={{
                position: 'absolute',
                top: 30,
                left: 30,
                width: 100,
                height: 100,
                borderLeft: '2px solid rgba(80, 130, 180, 0.4)',
                borderTop: '2px solid rgba(80, 130, 180, 0.4)',
                pointerEvents: 'none',
            }}/>
            {/* 边框光效 - 右下角 */}
            <div style={{
                position: 'absolute',
                bottom: 30,
                right: 30,
                width: 100,
                height: 100,
                borderRight: '2px solid rgba(80, 130, 180, 0.4)',
                borderBottom: '2px solid rgba(80, 130, 180, 0.4)',
                pointerEvents: 'none',
            }}/>
            <style>{`
        @keyframes aurora1 {
          0%, 100% { transform: translate(0, 0) rotate(0deg); border-radius: 60% 40% 50% 50% / 50% 60% 40% 50%; }
          25% { transform: translate(30px, -20px) rotate(5deg); border-radius: 50% 50% 45% 55% / 55% 45% 50% 50%; }
          50% { transform: translate(-20px, 20px) rotate(-3deg); border-radius: 45% 55% 50% 50% / 50% 50% 45% 55%; }
          75% { transform: translate(15px, 10px) rotate(2deg); border-radius: 55% 45% 50% 50% / 45% 55% 50% 50%; }
        }
        @keyframes aurora2 {
          0%, 100% { transform: translate(0, 0) rotate(0deg); border-radius: 40% 60% 45% 55% / 55% 45% 60% 40%; }
          33% { transform: translate(-25px, 15px) rotate(-4deg); border-radius: 50% 50% 50% 50% / 45% 55% 45% 55%; }
          66% { transform: translate(20px, -15px) rotate(3deg); border-radius: 45% 55% 40% 60% / 55% 45% 50% 50%; }
        }
        @keyframes aurora3 {
          0%, 100% { transform: translate(0, 0) scale(1); }
          50% { transform: translate(10px, -10px) scale(1.1); }
        }
        @keyframes breathe {
          0%, 100% { opacity: 0.7; }
          50% { opacity: 1; }
        }
        @keyframes scanLine {
          0% { transform: translateX(-100%); }
          100% { transform: translateX(200%); }
        }
        @keyframes grainMove {
          0%, 100% { transform: translate(0, 0); }
          10% { transform: translate(-1px, -1px); }
          20% { transform: translate(1px, 1px); }
          30% { transform: translate(-1px, 1px); }
          40% { transform: translate(1px, -1px); }
          50% { transform: translate(-1px, 0px); }
          60% { transform: translate(1px, 0px); }
          70% { transform: translate(0px, -1px); }
          80% { transform: translate(0px, 1px); }
          90% { transform: translate(-1px, -1px); }
        }
        @keyframes twinkle {
          0%, 100% { opacity: 0.2; }
          50% { opacity: 0.8; }
        }
      `}</style>
            <Card
                style={{
                    width: 420,
                    background: 'rgba(255, 255, 255, 0.9)',
                    backdropFilter: 'blur(20px)',
                    border: '1px solid rgba(100, 150, 200, 0.3)',
                    boxShadow: '0 8px 32px rgba(60, 100, 150, 0.2)',
                }}
            >
                {showRedirectAlert && (
                    <Alert
                        message="请登录"
                        description="您需要登录才能访问该页面"
                        type="info"
                        showIcon
                        closable
                        onClose={() => setShowRedirectAlert(false)}
                        style={{marginBottom: 16}}
                    />
                )}
                <div style={{textAlign: 'center', marginBottom: 24}}>
                    <div style={{
                        fontSize: 28,
                        fontWeight: 700,
                        color: '#2c3e50',
                        marginBottom: 8,
                        letterSpacing: '2px',
                    }}>
                        Easy Agent
                    </div>
                </div>
                <Form
                    name="login"
                    onFinish={onFinish}
                    autoComplete="off"
                    layout="vertical"
                >
                    <Form.Item
                        name="username"
                        rules={[{required: true, message: '请输入用户名'}]}
                    >
                        <Input
                            prefix={<UserOutlined/>}
                            placeholder="用户名"
                            size="large"
                        />
                    </Form.Item>

                    <Form.Item
                        name="password"
                        rules={[{required: true, message: '请输入密码'}]}
                    >
                        <Input.Password
                            prefix={<LockOutlined/>}
                            placeholder="密码"
                            size="large"
                        />
                    </Form.Item>

                    <Form.Item>
                        <Button
                            htmlType="submit"
                            loading={loading}
                            block
                            size="large"
                            style={{
                                background: '#4c76ad',
                                borderColor: '#8c8c8c',
                                color: '#fff',
                            }}
                        >
                            登录
                        </Button>
                    </Form.Item>

                    <div style={{textAlign: 'center'}}>
                        <Button
                            type="link"
                            onClick={() => navigate('/register')}
                        >
                            还没有账号？立即注册
                        </Button>
                    </div>
                </Form>
            </Card>
        </div>
    );
};

export default Login;
