import axios from 'axios';
import { message } from 'antd';

const request = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 30000,
});

request.interceptors.request.use(
  (config) => config,
  (error) => Promise.reject(error)
);

request.interceptors.response.use(
  (response) => {
    const res = response.data;
    if (res.code !== 200) {
      message.error(res.message || '请求失败');
      return Promise.reject(new Error(res.message));
    }
    return res;
  },
  (error) => {
    message.error(error.message || '网络错误');
    return Promise.reject(error);
  }
);

export default request;
