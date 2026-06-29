import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { LoginResult, MenuItem } from '../types';

interface AuthState {
  user: LoginResult | null;
  token: string | null;
  permissions: string[];
  menus: MenuItem[];
  initializing: boolean;

  // actions
  login: (data: LoginResult) => void;
  logout: () => void;
  setInitialized: () => void;
  hasPermission: (code: string) => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      token: null,
      permissions: [],
      menus: [],
      initializing: true,

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
      name: 'auth', // localStorage key
      partialize: (state) => ({
        // Only persist these fields
        user: state.user,
        token: state.token,
        permissions: state.permissions,
        menus: state.menus,
      }),
      onRehydrateStorage: () => (state) => {
        // After hydration from localStorage, mark as initialized
        if (state) {
          state.setInitialized();
        }
      },
    },
  ),
);

// Convenience: read-only derived value
export const useIsAuthenticated = () => useAuthStore((s) => !!s.token);
