import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import type { LoginResult, MenuItem } from '../types';

interface AuthState {
  user: LoginResult | null;
  token: string | null;
  permissions: string[];
  menus: MenuItem[];
  initializing: boolean;

  login: (data: LoginResult) => void; // 登录
  logout: () => void; // 退出
  setInitialized: () => void; // 标记初始化完成
  hasPermission: (code: string) => boolean; // 检查权限
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      // state: 数据
      user: null,
      token: null,
      permissions: [],
      menus: [],
      initializing: true,

      // actions: 操作函数
      login: (data: LoginResult) =>
        set({
          user: data,
          token: data.token,
          permissions: data.permissions || [],
          menus: data.menus || [],
        }),

      logout: () =>
        set({
          user: null,
          token: null,
          permissions: [],
          menus: [],
        }),

      setInitialized: () => set({ initializing: false }),

      hasPermission: (code: string) => get().permissions.includes(code),
    }),
    {
      // 使用 sessionStorage：关闭浏览器标签后自动清除，重新打开需重新登录
      // 页面刷新或 HMR 热更新时保持登录态
      name: 'auth',
      storage: createJSONStorage(() => sessionStorage),
      partialize: (state) => ({
        // 只保存这些字段到 localStorage
        user: state.user,
        token: state.token,
        permissions: state.permissions,
        menus: state.menus,
      }),
      onRehydrateStorage: () => (state) => {
        // sessionStorage 恢复数据后执行
        if (state) {
          // 标记初始化完成
          state.setInitialized();
        }
      },
    },
  ),
);

// 这是一个派生状态，用来快速判断是否已登录
export const useIsAuthenticated = () => useAuthStore((s) => !!s.token);
