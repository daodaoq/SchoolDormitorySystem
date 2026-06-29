import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuthStore } from './stores/authStore';
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

import MyDormitory from './pages/MyDormitory';
import MyBills from './pages/MyBills';
import Profile from './pages/Profile';

const RequireAuth: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const isAuthenticated = useAuthStore((s) => !!s.token);
  const initializing = useAuthStore((s) => s.initializing);

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

/** 角色守卫 — 限制学生端路由只能学生访问，后台路由只能管理员/教师访问 */
const RequireRole: React.FC<{ roles: string[]; children: React.ReactNode }> = ({ roles, children }) => {
  const user = useAuthStore((s) => s.user);
  if (!user || !roles.includes(user.role)) {
    // 无权限 → 跳回对应首页
    const home = user?.role === 'STUDENT' ? '/my-dormitory' : '/dashboard';
    return <Navigate to={home} replace />;
  }
  return <>{children}</>;
};

/** 根据角色选择布局 */
const LayoutSwitcher: React.FC = () => {
  const user = useAuthStore((s) => s.user);
  const isStudent = user?.role === 'STUDENT';
  return isStudent ? <StudentLayout /> : <BasicLayout />;
};

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/" element={<RequireAuth><LayoutSwitcher /></RequireAuth>}>
        <Route index element={<RoleHome />} />

        {/* 学生端专属路由 */}
        <Route path="my-dormitory" element={<RequireRole roles={['STUDENT']}><MyDormitory /></RequireRole>} />

        {/* 我的账单 — 学生专属 */}
        <Route path="my-bills" element={<RequireRole roles={['STUDENT']}><MyBills /></RequireRole>} />

        {/* 后台管理专属路由 */}
        <Route path="dashboard" element={<RequireRole roles={['ADMIN', 'TEACHER']}><Dashboard /></RequireRole>} />
        <Route path="students" element={<RequireRole roles={['ADMIN', 'TEACHER']}><Student /></RequireRole>} />
        <Route path="fee-items" element={<RequireRole roles={['ADMIN', 'TEACHER']}><FeeItem /></RequireRole>} />
        <Route path="bills" element={<RequireRole roles={['ADMIN', 'TEACHER']}><Bill /></RequireRole>} />
        <Route path="payment" element={<RequireRole roles={['ADMIN', 'TEACHER']}><Payment /></RequireRole>} />
        <Route path="statistics" element={<RequireRole roles={['ADMIN', 'TEACHER']}><Statistics /></RequireRole>} />
        <Route path="dormitories" element={<RequireRole roles={['ADMIN', 'TEACHER']}><Dormitory /></RequireRole>} />
        <Route path="users" element={<RequireRole roles={['ADMIN', 'TEACHER']}><Users /></RequireRole>} />
        <Route path="roles" element={<RequireRole roles={['ADMIN', 'TEACHER']}><Roles /></RequireRole>} />

        {/* 所有角色可访问 */}
        <Route path="profile" element={<Profile />} />
        <Route path="ai-qa" element={<AiQa />} />
        <Route path="knowledge-base" element={<KnowledgeBase />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

/** 登录后根据角色跳转 */
const RoleHome: React.FC = () => {
  const user = useAuthStore((s) => s.user);
  if (user?.role === 'STUDENT') return <Navigate to="/my-dormitory" replace />;
  return <Navigate to="/dashboard" replace />;
};

function App() {
  return <AppRoutes />;
}

export default App;
