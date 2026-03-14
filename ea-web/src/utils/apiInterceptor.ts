/**
 * API 拦截器 - 统一处理 HTTP 错误
 * 特别是处理 401 未认证错误，自动重定向到登录页
 */

import { message } from 'antd';

// 存储原始 fetch 函数
const originalFetch = window.fetch;

// 重定向到登录页
const redirectToLogin = () => {
  // 清除可能的认证信息（包括 cookie 和 localStorage）
  document.cookie = 'AUTH_TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
  localStorage.removeItem('AUTH_TOKEN');
  sessionStorage.removeItem('AUTH_TOKEN');

  // 获取当前路径作为重定向目标
  const currentPath = window.location.pathname + window.location.search;
  const redirectTo = encodeURIComponent(currentPath);

  // 重定向到登录页，并传递重定向目标
  window.location.href = `/login?redirect=${redirectTo}`;
};

// 自定义 fetch 函数
const customFetch = async (input: RequestInfo | URL, init?: RequestInit): Promise<Response> => {
  try {
    // 确保 init 对象包含 credentials
    const fetchInit = init || {};
    if (!fetchInit.credentials) {
      fetchInit.credentials = 'include';
    }

    // 从 localStorage 获取 token 并添加到请求头
    const token = localStorage.getItem('AUTH_TOKEN');
    if (token && !fetchInit.headers) {
      fetchInit.headers = {
        'Authorization': `Bearer ${token}`,
      };
    } else if (token && typeof fetchInit.headers === 'object') {
      (fetchInit.headers as Record<string, string>)['Authorization'] = `Bearer ${token}`;
    }

    // 调试：打印请求信息
    console.log('Making request to:', input);
    console.log('Current cookies before request:', document.cookie);
    console.log('Token from localStorage:', token ? token.substring(0, 10) + '...' : 'none');

    const response = await originalFetch(input, fetchInit);
    
    // 检查响应状态
    if (response.status === 401) {
      // 尝试解析错误信息
      try {
        const errorData = await response.json();
        if (errorData.code === '401') {
          message.error(errorData.message || '登录已过期，请重新登录');
        }
      } catch {
        // 如果无法解析 JSON，使用默认消息
        message.error('登录已过期，请重新登录');
      }

      // 重定向到登录页
      redirectToLogin();

      // 返回一个拒绝的 Promise，防止后续处理
      return Promise.reject(new Error('Unauthorized'));
    }
    
    // 处理其他错误状态
    if (!response.ok && response.status >= 400) {
      try {
        const errorData = await response.json();
        if (errorData.message) {
          message.error(errorData.message);
        }
      } catch {
        // 忽略解析错误
      }
    }
    
    return response;
  } catch (error) {
    console.error('Fetch error:', error);
    
    // 网络错误或其他异常
    if (error instanceof TypeError && error.message.includes('Failed to fetch')) {
      message.error('网络连接失败，请检查网络设置');
    }
    
    throw error;
  }
};

// 安装拦截器
export const installApiInterceptor = () => {
  if (window.fetch !== customFetch) {
    window.fetch = customFetch;
    console.log('API 拦截器已安装');
  }
};

// 卸载拦截器
export const uninstallApiInterceptor = () => {
  if (window.fetch === customFetch) {
    window.fetch = originalFetch;
    console.log('API 拦截器已卸载');
  }
};

// 检查是否已安装
export const isInterceptorInstalled = () => {
  return window.fetch === customFetch;
};
