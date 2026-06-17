import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, Form, Input, Button, Typography, message } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { useAuth } from '../../contexts/AuthContext';
import { loginApi } from '../../services/api';

const { Title, Text } = Typography;

const Login: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (isAuthenticated) navigate('/', { replace: true });
  }, [isAuthenticated, navigate]);

  const onFinish = async (values: { username: string; password: string }) => {
    setLoading(true);
    try {
      const res = await loginApi(values.username, values.password);
      if (res.code === 200 && res.data?.token) {
        login(res.data);
        message.success('登录成功');
      } else {
        message.error(res.message || '登录失败');
      }
    } catch (err: any) {
      message.error(err?.message || '登录请求失败，请检查后端是否已启动');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        display: 'flex',
        height: '100vh',
        alignItems: 'center',
        justifyContent: 'center',
        background: '#F5F5F0',
        position: 'relative',
        overflow: 'hidden',
      }}
    >
      {/* === 装饰性浮动药丸 === */}
      <div className="login-deco" style={{ top: '8%', left: '10%', width: 150, height: 50, background: 'var(--lime)', transform: 'rotate(-12deg)' }} />
      <div className="login-deco" style={{ top: '15%', right: '12%', width: 110, height: 40, background: 'var(--lavender)', transform: 'rotate(15deg)' }} />
      <div className="login-deco" style={{ bottom: '18%', left: '15%', width: 130, height: 42, background: 'var(--sky)', transform: 'rotate(7deg)' }} />
      <div className="login-deco" style={{ bottom: '12%', right: '8%', width: 120, height: 44, background: 'var(--peach)', transform: 'rotate(-6deg)' }} />
      <div className="login-deco" style={{ top: '45%', left: '4%', width: 90, height: 36, background: 'var(--yellow)', transform: 'rotate(-18deg)' }} />

      {/* === 纸纹肌理 === */}
      <div className="grain-overlay" />

      {/* === 登录卡片 === */}
      <Card
        style={{
          width: 420,
          borderRadius: 32,
          border: '1px solid rgba(26,26,26,0.12)',
          boxShadow: '6px 6px 0 rgba(26,26,26,0.05)',
          background: '#FFFFFF',
          position: 'relative',
          zIndex: 1,
        }}
        bodyStyle={{ padding: '56px 48px' }}
      >
        {/* 标题区 */}
        <div style={{ textAlign: 'center', marginBottom: 40 }}>
          <Title
            level={2}
            style={{
              marginBottom: 6,
              color: '#1A1A1A',
              fontFamily: "'Bodoni Moda', 'Noto Serif SC', serif",
              fontWeight: 800,
              fontSize: 28,
              letterSpacing: '-0.02em',
            }}
          >
            宿舍收费管理系统
          </Title>
          <Text style={{ color: 'rgba(26,26,26,0.45)', fontFamily: "'Space Grotesk', sans-serif", fontSize: 13, letterSpacing: '0.08em' }}>
            学校宿舍管理平台 · 登录
          </Text>
        </div>

        {/* 登录表单 */}
        <Form name="login" onFinish={onFinish} size="large">
          <Form.Item name="username" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input
              prefix={<UserOutlined style={{ color: 'rgba(26,26,26,0.35)' }} />}
              placeholder="用户名"
            />
          </Form.Item>
          <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password
              prefix={<LockOutlined style={{ color: 'rgba(26,26,26,0.35)' }} />}
              placeholder="密码"
            />
          </Form.Item>
          <Form.Item style={{ marginBottom: 20 }}>
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              block
              style={{
                height: 48,
                fontSize: 16,
                fontWeight: 600,
                letterSpacing: '0.06em',
              }}
            >
              登 录
            </Button>
          </Form.Item>
        </Form>

        <Text style={{
          display: 'block',
          textAlign: 'center',
          fontSize: 12,
          color: 'rgba(26,26,26,0.30)',
          fontFamily: "'Space Grotesk', sans-serif",
          letterSpacing: '0.05em',
        }}>
          默认账号: admin / admin123
        </Text>
      </Card>
    </div>
  );
};

export default Login;
