import React, {useState, useEffect} from 'react';
import {Form, Input, Button, Card, message, Alert} from 'antd';
import {UserOutlined, LockOutlined} from '@ant-design/icons';
import {authApi} from '../api/AuthApi';
import {useNavigate, useLocation} from 'react-router-dom';
import logo from '../../assets/eaLogo.png';

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
            background: 'linear-gradient(160deg, #dde6f2 0%, #d0d6ec 50%, #cdc9e8 100%)',
            position: 'relative',
            overflow: 'hidden',
        }}>
            {/* 柔光团 1 */}
            <div style={{
                position: 'absolute',
                top: '-15%',
                left: '-5%',
                width: '55vw',
                height: '55vw',
                background: 'radial-gradient(circle at center, rgba(70, 110, 175, 0.32) 0%, transparent 65%)',
                filter: 'blur(90px)',
                animation: 'drift1 22s ease-in-out infinite',
                pointerEvents: 'none',
            }}/>
            {/* 柔光团 2 */}
            <div style={{
                position: 'absolute',
                bottom: '-20%',
                right: '-10%',
                width: '50vw',
                height: '50vw',
                background: 'radial-gradient(circle at center, rgba(124, 96, 190, 0.30) 0%, transparent 65%)',
                filter: 'blur(100px)',
                animation: 'drift2 28s ease-in-out infinite',
                pointerEvents: 'none',
            }}/>
            {/* 柔光团 3 - 紫调 */}
            <div style={{
                position: 'absolute',
                top: '30%',
                right: '15%',
                width: '38vw',
                height: '38vw',
                background: 'radial-gradient(circle at center, rgba(140, 110, 205, 0.22) 0%, transparent 65%)',
                filter: 'blur(110px)',
                animation: 'drift1 26s ease-in-out infinite',
                pointerEvents: 'none',
            }}/>
            {/* 浮动气泡 */}
            <div style={{position: 'absolute', inset: 0, overflow: 'hidden', pointerEvents: 'none'}}>
                {[
                    {size: 14, left: '12%', dur: 13, delay: 0, op: 0.5},
                    {size: 26, left: '24%', dur: 18, delay: 2.5, op: 0.35},
                    {size: 9, left: '38%', dur: 11, delay: 1, op: 0.6},
                    {size: 40, left: '50%', dur: 22, delay: 4, op: 0.25},
                    {size: 16, left: '63%', dur: 15, delay: 0.5, op: 0.45},
                    {size: 22, left: '74%', dur: 19, delay: 3, op: 0.35},
                    {size: 11, left: '85%', dur: 12, delay: 1.8, op: 0.55},
                    {size: 32, left: '92%', dur: 24, delay: 5, op: 0.28},
                    {size: 12, left: '6%', dur: 16, delay: 3.5, op: 0.5},
                    {size: 18, left: '45%', dur: 20, delay: 6, op: 0.4},
                ].map((b, i) => (
                    <span key={i} style={{
                        position: 'absolute',
                        bottom: -60,
                        left: b.left,
                        width: b.size,
                        height: b.size,
                        borderRadius: '50%',
                        background: `radial-gradient(circle at 32% 28%, rgba(255,255,255,0.95), rgba(24,144,255,${b.op}) 60%, rgba(92,116,168,${b.op * 0.6}) 100%)`,
                        boxShadow: `0 0 ${b.size}px rgba(24,144,255,0.25)`,
                        animation: `rise ${b.dur}s ease-in infinite, twinkle ${3 + (i % 3)}s ease-in-out infinite`,
                        animationDelay: `${b.delay}s, ${b.delay}s`,
                    }}/>
                ))}
            </div>
            {/* 顶部高光带 */}
            <div style={{
                position: 'absolute',
                inset: 0,
                background: 'radial-gradient(ellipse 120% 50% at 50% -10%, rgba(255, 255, 255, 0.6) 0%, transparent 55%)',
                pointerEvents: 'none',
            }}/>
            <style>{`
        @keyframes drift1 {
          0%, 100% { transform: translate(0, 0) scale(1); }
          50% { transform: translate(6vw, 4vh) scale(1.08); }
        }
        @keyframes drift2 {
          0%, 100% { transform: translate(0, 0) scale(1); }
          50% { transform: translate(-5vw, -4vh) scale(1.1); }
        }
        @keyframes rise {
          0% { transform: translateY(0) translateX(0); }
          50% { transform: translateY(-55vh) translateX(20px); }
          100% { transform: translateY(-110vh) translateX(-15px); }
        }
        @keyframes twinkle {
          0%, 100% { opacity: 0.25; }
          50% { opacity: 1; }
        }
        .login-light-card .ant-form-item { margin-bottom: 22px !important; }
        .login-light-card .login-submit-item { margin-bottom: 12px !important; }
        .login-light-card .ant-input-affix-wrapper {
          background: rgba(255, 255, 255, 0.7) !important;
          border-color: rgba(24, 144, 255, 0.18) !important;
        }
        .login-light-card .ant-input-affix-wrapper:hover { border-color: rgba(24, 144, 255, 0.55) !important; }
        .login-light-card .ant-input-affix-wrapper-focused { box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.15) !important; }
      `}</style>
            <Card
                className="login-light-card"
                styles={{body: {padding: '40px 40px 32px'}}}
                style={{
                    width: 400,
                    background: 'rgba(255, 255, 255, 0.72)',
                    backdropFilter: 'blur(24px) saturate(160%)',
                    WebkitBackdropFilter: 'blur(24px) saturate(160%)',
                    border: '1px solid rgba(255, 255, 255, 0.8)',
                    borderRadius: 20,
                    boxShadow: '0 24px 60px rgba(60, 100, 160, 0.18), inset 0 1px 0 rgba(255, 255, 255, 0.9)',
                    position: 'relative',
                    zIndex: 1,
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
                <div style={{textAlign: 'center', marginBottom: 28}}>
                    <img
                        src={logo}
                        alt="Easy Agent"
                        style={{
                            width: 60,
                            height: 60,
                            margin: '0 auto 16px',
                            display: 'block',
                            objectFit: 'contain',
                            filter: 'drop-shadow(0 8px 18px rgba(24, 144, 255, 0.28))',
                        }}
                    />
                    <div style={{
                        fontSize: 26,
                        fontWeight: 600,
                        color: '#1f2d3d',
                        letterSpacing: '0.5px',
                    }}>
                        Easy Agent
                    </div>
                    <div style={{
                        fontSize: 13,
                        color: 'rgba(60, 90, 130, 0.6)',
                        marginTop: 6,
                        letterSpacing: '1px',
                    }}>
                        智能体平台 · AI Agent Platform
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

                    <Form.Item className="login-submit-item">
                        <Button
                            htmlType="submit"
                            loading={loading}
                            block
                            size="large"
                            style={{
                                height: 44,
                                border: 'none',
                                borderRadius: 12,
                                fontWeight: 600,
                                letterSpacing: '2px',
                                background: 'linear-gradient(135deg, #1890ff 0%, #5c74a8 100%)',
                                color: '#fff',
                                boxShadow: '0 8px 24px rgba(24, 144, 255, 0.35)',
                            }}
                        >
                            登 录
                        </Button>
                    </Form.Item>

                    <div style={{textAlign: 'center'}}>
                        <Button
                            type="link"
                            onClick={() => navigate('/register')}
                            style={{color: 'rgba(60, 90, 130, 0.75)'}}
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
