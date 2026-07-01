// ========== RBAC 菜单 ==========

export interface MenuItem {
  id: number;
  parentId: number;
  menuName: string;
  menuType: 'MENU' | 'PAGE' | 'BUTTON';
  path?: string;
  icon?: string;
  permissionCode?: string;
  sortOrder: number;
  visible: number;
  children?: MenuItem[];
}

// ========== 登录响应 ==========

export interface LoginResult {
  token: string;
  username: string;
  realName: string;
  role: string;
  roleName: string;
  permissions: string[];
  menus: MenuItem[];
  studentInfo?: {
    id: number;
    studentNo: string;
    studentName: string;
    dormitoryNo: string;
    phone: string;
    checkInDate: string;
    paymentStatus: string;
  };
}

// ========== 角色 ==========

export interface RoleItem {
  id: number;
  roleCode: string;
  roleName: string;
  description?: string;
  status: string;
  createTime: string;
}

// ========== 用户 ==========

export interface UserInfo {
  id: number;
  username: string;
  realName: string;
  role: string;
  status: string;
  avatar?: string;
  createTime?: string;
}
