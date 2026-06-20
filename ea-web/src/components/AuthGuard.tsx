import React from 'react';
import {Button, Modal} from 'antd';
import {ExclamationCircleFilled} from '@ant-design/icons';

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
 */
const AuthGuard: React.FC<AuthGuardProps> = ({children, requiredAuth = true}) => {
    const [isLoading, setIsLoading] = React.useState(true);
    const [showRedirectModal, setShowRedirectModal] = React.useState(false);
    const [countdown, setCountdown] = React.useState(10);
    const timerRef = React.useRef<ReturnType<typeof setInterval> | null>(null);

    const redirectToLogin = React.useCallback(() => {
        if (timerRef.current) clearInterval(timerRef.current);
        const currentPath = window.location.pathname + window.location.search;
        window.location.href = `/login?redirect=${encodeURIComponent(currentPath)}`;
    }, []);

    React.useEffect(() => {
        const checkAuth = async () => {
            let token = localStorage.getItem('AUTH_TOKEN');

            if (!token) {
                const sessionToken = sessionStorage.getItem('AUTH_TOKEN');
                if (sessionToken) token = sessionToken;
            }

            if (!token) {
                const cookieToken = getCookie('AUTH_TOKEN');
                if (cookieToken) token = cookieToken;
            }

            if (!token) {
                const urlParams = new URLSearchParams(window.location.search);
                const urlToken = urlParams.get('token');
                if (urlToken) {
                    token = urlToken;
                    sessionStorage.setItem('AUTH_TOKEN', urlToken);
                }
            }

            if (token) {
                try {
                    const response = await fetch('http://localhost:8080/auth/currentUser', {
                        method: 'GET',
                        credentials: 'include',
                        headers: { 'Authorization': `Bearer ${token}` }
                    });

                    if (response.ok) {
                        const result = await response.json();
                        if (result.code === '0' || result.success === true) {
                            setIsLoading(false);
                            return;
                        } else {
                            localStorage.removeItem('AUTH_TOKEN');
                            sessionStorage.removeItem('AUTH_TOKEN');
                        }
                    } else if (response.status === 401) {
                        localStorage.removeItem('AUTH_TOKEN');
                        sessionStorage.removeItem('AUTH_TOKEN');
                    }
                } catch (error) {
                    console.error('验证 token 时出错:', error);
                }
            }

            // 未认证
            if (requiredAuth) {
                setIsLoading(false);
                setCountdown(10);
                setShowRedirectModal(true);

                timerRef.current = setInterval(() => {
                    setCountdown(prev => {
                        if (prev <= 1) {
                            clearInterval(timerRef.current!);
                            redirectToLogin();
                            return 0;
                        }
                        return prev - 1;
                    });
                }, 1000);
            } else {
                setIsLoading(false);
            }
        };

        if ('requestIdleCallback' in window) {
            requestIdleCallback(() => checkAuth());
        } else {
            setTimeout(checkAuth, 100);
        }
    }, [requiredAuth, redirectToLogin]);

    // 组件卸载时清理
    React.useEffect(() => {
        return () => {
            if (timerRef.current) clearInterval(timerRef.current);
        };
    }, []);

    if (isLoading) return null;

    return (
        <>
            {children}
            <Modal
                open={showRedirectModal}
                closable={false}
                maskClosable={false}
                keyboard={false}
                footer={null}
                mask={false}
                width={360}
                style={{position: 'fixed', top: 24, right: 24, margin: 0, paddingBottom: 0}}
            >
                <div style={{display: 'flex', alignItems: 'flex-start', gap: 12}}>
                    <ExclamationCircleFilled style={{color: '#faad14', fontSize: 22, marginTop: 2}}/>
                    <div style={{flex: 1}}>
                        <div style={{fontSize: 16, fontWeight: 500, marginBottom: 8}}>
                            您尚未登录，即将跳转到登录页面
                        </div>
                        <div style={{fontSize: 14, color: 'rgba(0,0,0,0.45)', marginBottom: 16}}>
                            系统将在 <span style={{color: '#1677ff', fontWeight: 600}}>{countdown}</span> 秒后自动跳转
                        </div>
                        <Button type="primary" block onClick={redirectToLogin}>
                            立即登录
                        </Button>
                    </div>
                </div>
            </Modal>
        </>
    );
};

export default AuthGuard;
