const API_BASE_URL = 'http://localhost:8080';

/**
 * 认证相关 API
 *
 * @author Claude Code
 * @version 1.0 AuthApi.ts  2026/03/14
 */
export const authApi = {

  /**
   * 用户登录
   * @param username 用户名
   * @param password 密码
   * @returns 登录结果
   */
  login: async (username: string, password: string) => {
    const response = await fetch(`${API_BASE_URL}/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include', // 携带 Cookie
      body: JSON.stringify({ username, password }),
    });
    return response.json();
  },

  /**
   * 用户注册
   * @param username 用户名
   * @param password 密码
   * @param email 邮箱
   * @param phone 手机号
   * @returns 注册结果
   */
  register: async (username: string, password: string, email?: string, phone?: string) => {
    const response = await fetch(`${API_BASE_URL}/auth/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include', // 携带 Cookie
      body: JSON.stringify({ username, password, email, phone }),
    });
    return response.json();
  },

  /**
   * 获取当前用户信息
   * @returns 当前用户信息
   */
  getCurrentUser: async () => {
    const response = await fetch(`${API_BASE_URL}/auth/currentUser`, {
      method: 'GET',
      credentials: 'include', // 携带 Cookie
    });
    return response.json();
  },

  /**
   * 用户登出
   * @returns 登出结果
   */
  logout: async () => {
    const response = await fetch(`${API_BASE_URL}/auth/logout`, {
      method: 'POST',
      credentials: 'include', // 携带 Cookie
    });
    return response.json();
  },
};
