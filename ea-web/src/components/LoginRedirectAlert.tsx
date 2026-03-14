import React from 'react';
import { Alert, Button } from 'antd';

interface LoginRedirectAlertProps {
  redirectPath?: string;
  message?: string;
}

const LoginRedirectAlert: React.FC<LoginRedirectAlertProps> = ({
  redirectPath,
  message = "您需要登录才能访问该页面，请先登录后再继续。"
}) => {
  const goToLogin = () => {
    const redirectTo = redirectPath
      ? encodeURIComponent(redirectPath)
      : window.location.pathname + window.location.search;
    window.location.href = `/login?redirect=${redirectTo}`;
  };

  return (
    <Alert
      message="需要登录"
      description={
        <div>
          <p>{message}</p>
          <Button
            type="primary"
            onClick={goToLogin}
            style={{ marginTop: 8 }}
          >
            立即登录
          </Button>
        </div>
      }
      type="info"
      showIcon
      closable
    />
  );
};

export default LoginRedirectAlert;