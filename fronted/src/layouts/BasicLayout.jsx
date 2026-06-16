import React from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { Layout, Menu } from 'antd';
import {
  DashboardOutlined,
  UserOutlined,
  DollarOutlined,
  FileTextOutlined,
  PayCircleOutlined,
  BarChartOutlined,
  RobotOutlined,
} from '@ant-design/icons';

const { Header, Sider, Content } = Layout;

const menuItems = [
  { key: '/dashboard', icon: <DashboardOutlined />, label: '首页仪表盘' },
  { key: '/students', icon: <UserOutlined />, label: '学生宿舍管理' },
  { key: '/fee-items', icon: <DollarOutlined />, label: '收费项目管理' },
  { key: '/bills', icon: <FileTextOutlined />, label: '缴费账单管理' },
  { key: '/payments', icon: <PayCircleOutlined />, label: '在线缴费' },
  { key: '/statistics', icon: <BarChartOutlined />, label: '统计报表' },
  { key: '/ai-qa', icon: <RobotOutlined />, label: 'AI智能问答' },
];

function BasicLayout() {
  const navigate = useNavigate();
  const location = useLocation();

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider width={220} theme="dark">
        <div style={{ height: 64, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <h2 style={{ color: '#fff', margin: 0, fontSize: 16 }}>宿舍收费管理系统</h2>
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>
      <Layout>
        <Header style={{ background: '#fff', padding: '0 24px', boxShadow: '0 1px 4px rgba(0,0,0,0.1)' }}>
          <h3 style={{ margin: 0, lineHeight: '64px' }}>
            {menuItems.find((item) => item.key === location.pathname)?.label || '学生宿舍收费管理系统'}
          </h3>
        </Header>
        <Content style={{ margin: 16, padding: 24, background: '#fff', borderRadius: 8, minHeight: 280 }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}

export default BasicLayout;
