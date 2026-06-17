import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import BasicLayout from './layouts/BasicLayout';
import StudentLayout from './layouts/StudentLayout';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Student from './pages/Student';
import FeeItem from './pages/FeeItem';
import Bill from './pages/Bill';
import Payment from './pages/Payment';
import Statistics from './pages/Statistics';
import AiQa from './pages/AiQa';
import KnowledgeBase from './pages/KnowledgeBase';
import Users from './pages/Users';
import Roles from './pages/Roles';
import Dormitory from './pages/Dormitory';
import Menus from './pages/Menus';
import Personnel from './pages/Personnel';
import MyDormitory from './pages/MyDormitory';
import MyBills from './pages/MyBills';
import Profile from './pages/Profile';

const RequireAuth: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, initializing } = useAuth();

  // 初始化阶段显示 loading，不跳转，避免闪跳 bug
  if (initializing) {
    return (
      <div style={{
        minHeight: '100vh', display: 'flex', alignItems: 'center',
        justifyContent: 'center', background: '#F5F5F0',
      }}>
        <div style={{ textAlign: 'center' }}>
          <div style={{
            width: 32, height: 32, borderRadius: '50%',
            border: '3px solid #EDE8E0', borderTopColor: '#E85D4E',
            animation: 'spin 0.8s linear infinite', margin: '0 auto 12px',
          }} />
          <div style={{ color: 'rgba(26,26,26,0.35)', fontSize: 13 }}>加载中...</div>
        </div>
        <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
      </div>
    );
  }

  if (!isAuthenticated) return <Navigate to="/login" replace />;
  return <>{children}</>;
};

/** 根据角色选择布局 */
const LayoutSwitcher: React.FC = () => {
  const { user } = useAuth();
  const isStudent = user?.role === 'STUDENT';
  return isStudent ? <StudentLayout /> : <BasicLayout />;
};

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/" element={<RequireAuth><LayoutSwitcher /></RequireAuth>}>
        {/* 学生端路由 */}
        <Route index element={<RoleHome />} />
        <Route path="my-dormitory" element={<MyDormitory />} />
        <Route path="my-bills" element={<MyBills />} />
        <Route path="profile" element={<Profile />} />
        {/* 通用路由 */}
        <Route path="dashboard" element={<Dashboard />} />
        <Route path="students" element={<Student />} />
        <Route path="fee-items" element={<FeeItem />} />
        <Route path="bills" element={<Bill />} />
        <Route path="payments" element={<Payment />} />
        <Route path="statistics" element={<Statistics />} />
        <Route path="ai-qa" element={<AiQa />} />
        <Route path="knowledge-base" element={<KnowledgeBase />} />
        <Route path="dormitories" element={<Dormitory />} />
        <Route path="users" element={<Users />} />
        <Route path="roles" element={<Roles />} />
        <Route path="menus" element={<Menus />} />
        <Route path="personnel" element={<Personnel />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

/** 登录后根据角色跳转 */
const RoleHome: React.FC = () => {
  const { user } = useAuth();
  if (user?.role === 'STUDENT') return <Navigate to="/my-dormitory" replace />;
  return <Navigate to="/dashboard" replace />;
};

function App() {
  return (
    <AuthProvider>
      <AppRoutes />
    </AuthProvider>
  );
}

export default App;
