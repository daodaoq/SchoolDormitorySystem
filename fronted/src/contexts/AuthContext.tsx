import React, { createContext, useContext, useState, useEffect } from 'react';
import type { LoginResult, MenuItem } from '../types';

interface AuthContextType {
  user: LoginResult | null;
  token: string | null;
  permissions: string[];
  menus: MenuItem[];
  login: (data: LoginResult) => void;
  logout: () => void;
  isAuthenticated: boolean;
  initializing: boolean;  // 新增：标记初始化阶段
  hasPermission: (code: string) => boolean;
}

const AuthContext = createContext<AuthContextType>({
  user: null, token: null, permissions: [], menus: [],
  login: () => {}, logout: () => {}, isAuthenticated: false,
  initializing: true,
  hasPermission: () => false,
});

export const useAuth = () => useContext(AuthContext);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<LoginResult | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [permissions, setPermissions] = useState<string[]>([]);
  const [menus, setMenus] = useState<MenuItem[]>([]);
  const [initializing, setInitializing] = useState(true);

  // 初始化：同步读取 localStorage（不依赖 useEffect 的异步时序）
  useEffect(() => {
    try {
      const saved = localStorage.getItem('auth');
      if (saved) {
        const parsed = JSON.parse(saved) as LoginResult;
        setUser(parsed);
        setToken(parsed.token);
        setPermissions(parsed.permissions || []);
        setMenus(parsed.menus || []);
      }
    } catch {
      localStorage.removeItem('auth');
    }
    setInitializing(false);
  }, []);

  const login = (data: LoginResult) => {
    setUser(data);
    setToken(data.token);
    setPermissions(data.permissions || []);
    setMenus(data.menus || []);
    localStorage.setItem('auth', JSON.stringify(data));
  };

  const logout = () => {
    setUser(null); setToken(null); setPermissions([]); setMenus([]);
    localStorage.removeItem('auth');
  };

  const hasPermission = (code: string) => permissions.includes(code);

  return (
    <AuthContext.Provider value={{
      user, token, permissions, menus, login, logout,
      isAuthenticated: !!token, initializing, hasPermission,
    }}>
      {children}
    </AuthContext.Provider>
  );
}
