import axios from 'axios';
import { message } from 'antd';
import { getToken } from './token';

const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
});

request.interceptors.request.use((config) => {
  const token = getToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

request.interceptors.response.use(
  (response) => {
    // blob 响应（导出等）直接返回，不做 JSON 解析
    if (response.config.responseType === 'blob') {
      return response.data;
    }
    const res = response.data;
    if (res.code !== 200) {
      if (res.code === 401) {
        localStorage.removeItem('auth');
        // 用 assign 而非直接替换，保留 history 历史
        window.location.assign('/login');
        return Promise.reject(new Error('未登录'));
      }
      message.error(res.message || '请求失败');
      return Promise.reject(new Error(res.message));
    }
    return res;
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('auth');
      window.location.assign('/login');
      return Promise.reject(new Error('未登录'));
    }
    // 网络错误不弹提示，避免频繁跳转时刷屏
    if (error.code === 'ERR_CANCELED') {
      return Promise.reject(error);
    }
    message.error(error.message || '网络错误');
    return Promise.reject(error);
  }
);

export default request;
