import React from 'react';
import {Result, Button, Spin, message} from 'antd';
import {useNavigate} from 'react-router-dom';
import {App} from 'antd';

// 从 cookie 中获取认证信息的辅助函数
const getCookie = (name: string): string | null => {
    if (typeof document === 'undefined') {
        return null;
    }
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) {
        return parts.pop()?.split(';').shift() || null;
    }
    return null;
};

// 尝试从 storage 中获取 token（作为备用方案）
const getTokenFromStorage = (): string | null => {
    try {
        const localToken = localStorage.getItem('AUTH_TOKEN');
        if (localToken) return localToken;
    } catch (e) {
        console.log('localStorage not accessible');
    }
    try {
        const sessionToken = sessionStorage.getItem('AUTH_TOKEN');
        if (sessionToken) return sessionToken;
    } catch (e) {
        console.log('sessionStorage not accessible');
    }
    return null;
};

// 动态添加样式到全局
if (typeof document !== 'undefined') {
    const styleId = 'easy-agent-message-top-right';
    if (!document.getElementById(styleId)) {
        const style = document.createElement('style');
        style.id = styleId;
        style.textContent = `
      .ant-message {
        top: 24px !important;
        right: 0 !important;
        left: auto !important;
        transform: none !important;
        width: 360px;
        max-width: 480px;
        margin: 0 !important;
      }
      .ant-message-notice {
        position: fixed !important;
        top: 24px !important;
        right: 24px !important;
        left: auto !important;
        transform: none !important;
        margin: 0 !important;
        max-width: 480px;
        width: 360px;
        display: flex !important;
        align-items: flex-start !important;
        padding-top: 4px !important;
      }
      .ant-message-custom-content {
        display: flex !important;
        align-items: flex-start !important;
        gap: 8px !important;
      }
    `;
        document.head.appendChild(style);
    }
}

interface AuthGuardProps {
    children: React.ReactNode;
    requiredAuth?: boolean;
}

/**
 * 认证守卫组件
 * 
 * 三层兜底认证机制：
 * 1. localStorage（主要）- 快速读取，无 HttpOnly 限制
 * 2. sessionStorage（备用）- 会话级存储
 * 3. Cookie（兜底）- 兼容 HttpOnly 禁用场景
 * 
 * @param children 子组件
 * @param requiredAuth 是否需要认证，默认 true
 */
const AuthGuard: React.FC<AuthGuardProps> = ({children, requiredAuth = true}) => {
    const navigate = useNavigate();
    const {message: appMessage} = App.useApp();

    const [isLoading, setIsLoading] = React.useState(true);
    const [isAuthenticated, setIsAuthenticated] = React.useState(false);
    const [countdown, setCountdown] = React.useState(60);

    React.useEffect(() => {
        const checkAuth = () => {
            // 优先从 localStorage 获取 token（主要认证方式）
            let token = localStorage.getItem('AUTH_TOKEN');
            
            // 如果 localStorage 中没有，尝试从 sessionStorage 读取
            if (!token) {
                const sessionToken = sessionStorage.getItem('AUTH_TOKEN');
                if (sessionToken) {
                    token = sessionToken;
                }
            }
            
            // 兜底方案：尝试从 cookie 读取（兼容 HttpOnly 禁用场景）
            if (!token) {
                const cookieToken = getCookie('AUTH_TOKEN');
                if (cookieToken) {
                    token = cookieToken;
                }
            }
            
            // 如果仍然没有，尝试从 URL 参数中获取（临时方案）
            if (!token) {
                const urlParams = new URLSearchParams(window.location.search);
                const urlToken = urlParams.get('token');
                if (urlToken) {
                    token = urlToken;
                    sessionStorage.setItem('AUTH_TOKEN', urlToken);
                }
            }

            // 未认证且需要认证时，显示倒计时提示并跳转
            if (!token && requiredAuth) {
                if (!isAuthenticated) {
                    setIsAuthenticated(false);
                    setIsLoading(false);

                    // 倒计时和跳转逻辑
                    let timeLeft = 5;
                    const timer = setInterval(() => {
                        timeLeft -= 1;
                        setCountdown(timeLeft);
                        if (timeLeft <= 0) {
                            clearInterval(timer);
                            const currentPath = window.location.pathname + window.location.search;
                            const redirectTo = encodeURIComponent(currentPath);
                            window.location.href = `/login?redirect=${redirectTo}`;
                        }
                    }, 1000);

                    // 用户可以点击按钮立即跳转
                    const onLoginNow = () => {
                        clearInterval(timer);
                        const currentPath = window.location.pathname + window.location.search;
                        const redirectTo = encodeURIComponent(currentPath);
                        window.location.href = `/login?redirect=${redirectTo}`;
                    };

                    // 存储函数供按钮使用
                    (window as any).loginNow = onLoginNow;

                    // 显示包含倒计时的提示
                    appMessage.info({
                        key: 'login_redirect',
                        style: {
                            minWidth: '360px',
                            maxWidth: '480px',
                            width: 'auto',
                            whiteSpace: 'normal',
                            zIndex: 9999
                        },
                        content: (
                            <div className="ant-message-custom-content">
                                <div>
                                    <div style={{fontSize: '14px', lineHeight: '20px', marginBottom: 8}}>
                                        您尚未登录，即将跳转到登录页面
                                    </div>
                                    <div style={{fontSize: '14px', color: '#1677ff', marginBottom: 8}}>
                                        系统将在 {countdown} 秒后自动跳转
                                    </div>
                                    <Button
                                        type="primary"
                                        size="small"
                                        onClick={onLoginNow}
                                        style={{ width: '100%' }}
                                    >
                                        立即登录
                                    </Button>
                                </div>
                            </div>
                        ),
                        duration: 0, // 手动控制关闭
                    });
                }
                return false;
            }

            // 已认证，清除提示消息
            appMessage.destroy('login_redirect');
            setIsAuthenticated(true);
            setIsLoading(false);
            return true;
        };

        // 使用 requestIdleCallback 优先级较低地执行检查
        if ('requestIdleCallback' in window) {
            requestIdleCallback(() => checkAuth());
        } else {
            setTimeout(checkAuth, 100);
        }
    }, [requiredAuth, appMessage]);

    // 倒计时计时器 - 更新消息中的倒计时
    React.useEffect(() => {
        // 只有在未认证且需要认证时才更新倒计时
        if (!isAuthenticated && requiredAuth && countdown > 0) {
            const timer = setTimeout(() => {
                setCountdown(prev => prev - 1);
            }, 1000);

            return () => clearTimeout(timer);
        } else {
            // 如果已经认证或者不需要认证，清除消息
            appMessage.destroy('login_redirect');
        }
    }, [isAuthenticated, requiredAuth, countdown, appMessage]);

    // 组件卸载时清理消息
    React.useEffect(() => {
        return () => {
            appMessage.destroy('login_redirect');
        };
    }, [appMessage]);

    // 如果需要认证但未通过，显示无权限页面
    if (requiredAuth && !isAuthenticated) {
        return (
            <div style={{
                display: 'flex',
                justifyContent: 'center',
                alignItems: 'center',
                minHeight: '100vh'
            }}>
                <Result
                    status="warning"
                    title="需要登录"
                    subTitle="请先登录后访问"
                />
            </div>
        );
    }

    // 认证通过或有页面不需要认证时，渲染子组件
    return <>{children}</>;
};

export default AuthGuard;
