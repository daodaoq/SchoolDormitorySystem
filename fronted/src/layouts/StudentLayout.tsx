import React from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { Layout, Space, Avatar, Dropdown, Typography } from 'antd';
import { LogoutOutlined, UserOutlined, IdcardOutlined } from '@ant-design/icons';
import { Home, FileText, CreditCard, Bot, User } from 'lucide-react';
import { useAuthStore } from '../stores/authStore';
import { ExpandableTabs } from '@/components/ui/expandable-tabs';


// 在JavaScript， TypeScript里面嵌入html
// React 分为 类组件（官方不推荐） 和 函数式组件
// React 有 19 个版本，18 版以前是类组件，18 版开始变成函数式组件

const { Header, Content, Footer } = Layout;
const { Text } = Typography;

const tabs = [
  { title: '我的宿舍', icon: Home },
  { title: '我的账单', icon: FileText },
  { title: '在线缴费', icon: CreditCard },
  { title: 'AI助手', icon: Bot },
  { title: '个人信息', icon: User },
];

const pathToIndex: Record<string, number> = {
  '/my-dormitory': 0,
  '/my-bills': 1,
  '/payment': 2,
  '/ai-qa': 3,
  '/profile': 4,
};

const indexToPath: Record<number, string> = {
  0: '/my-dormitory',
  1: '/my-bills',
  2: '/payment',
  3: '/ai-qa',
  4: '/profile',
};

const StudentLayout: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);

  const activeIndex = pathToIndex[location.pathname] ?? null;

  const handleTabChange = (index: number | null) => {
    if (index !== null && indexToPath[index]) {
      navigate(indexToPath[index]);
    }
  };

  return (
    <Layout
      style={{
        minHeight: '100vh',
        background: '#F5F5F0',
        position: 'relative',
      }}
    >
      {/* === 装饰性浮动药丸（低调） === */}
      <div style={{
        position: 'absolute',
        top: '5%',
        right: '5%',
        width: 110,
        height: 40,
        borderRadius: 9999,
        border: '1px solid rgba(26,26,26,0.10)',
        background: 'var(--peach)',
        opacity: 0.22,
        transform: 'rotate(-10deg)',
        pointerEvents: 'none',
        zIndex: 0,
      }} />
      <div style={{
        position: 'absolute',
        bottom: '12%',
        left: '4%',
        width: 120,
        height: 42,
        borderRadius: 9999,
        border: '1px solid rgba(26,26,26,0.10)',
        background: 'var(--mint)',
        opacity: 0.22,
        transform: 'rotate(6deg)',
        pointerEvents: 'none',
        zIndex: 0,
      }} />

      {/* === 纸纹肌理 === */}
      <div className="grain-overlay" />

      {/* === Capsule Header === */}
      <Header
        style={{
          background: '#FFFFFF',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '0 40px',
          height: 72,
          borderBottom: '1px solid rgba(26,26,26,0.08)',
          boxShadow: '0 2px 0 rgba(26,26,26,0.04)',
          position: 'sticky',
          top: 0,
          zIndex: 100,
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 32 }}>
          {/* Logo */}
          <h2
            style={{
              margin: 0,
              fontFamily: "'Bodoni Moda', 'Noto Serif SC', serif",
              fontSize: 24,
              fontWeight: 800,
              color: '#1A1A1A',
              letterSpacing: '-0.02em',
            }}
          >
            智慧宿舍
          </h2>

          {/* 药丸风格 ExpandableTabs */}
          <ExpandableTabs
            tabs={tabs}
            selectedIndex={activeIndex}
            activeColor="text-[#E85D4E]"
            onChange={handleTabChange}
            className="border-[#1E1E1E] bg-[#F5F5F0]"
          />
        </div>

        {/* 用户信息 — 药丸胶囊 */}
        <Dropdown
          menu={{
            items: [
              {
                key: 'profile',
                icon: <IdcardOutlined />,
                label: '个人信息',
                onClick: () => navigate('/profile'),
              },
              { type: 'divider' as const },
              {
                key: 'logout',
                icon: <LogoutOutlined />,
                label: '退出登录',
                onClick: () => {
                  logout();
                  navigate('/login');
                },
              },
            ],
          }}
        >
          <Space style={{ cursor: 'pointer' }}>
            <div style={{
              width: 34,
              height: 34,
              borderRadius: '50%',
              border: '1px solid rgba(26,26,26,0.12)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}>
              <Avatar
                icon={<UserOutlined />}
                size={24}
                style={{
                  background: 'transparent',
                  color: '#1A1A1A',
                }}
              />
            </div>
            <span style={{ fontSize: 14, color: '#1A1A1A', fontWeight: 600, fontFamily: "'Space Grotesk', sans-serif" }}>
              {user?.realName || user?.username}
            </span>
          </Space>
        </Dropdown>
      </Header>

      {/* === 内容区 === */}
      <Content style={{ padding: '32px 40px', maxWidth: 1200, margin: '0 auto', width: '100%', position: 'relative', zIndex: 1 }}>
        <div
          style={{
            background: '#FFFFFF',
            borderRadius: 32,
            border: '1px solid rgba(26,26,26,0.10)',
            boxShadow: '4px 4px 0 rgba(26,26,26,0.05)',
            minHeight: 'calc(100vh - 260px)',
            overflow: 'hidden',
          }}
        >
          <Outlet />
        </div>
      </Content>

      {/* === Footer === */}
      <Footer style={{
        textAlign: 'center',
        background: 'transparent',
        color: 'rgba(26,26,26,0.30)',
        fontFamily: "'Space Grotesk', sans-serif",
        fontSize: 12,
        letterSpacing: '0.08em',
        padding: '24px',
        position: 'relative',
        zIndex: 1,
      }}>
        智慧宿舍管理系统 © 2024 — 让校园生活更美好
      </Footer>
    </Layout>
  );
};

export default StudentLayout;
