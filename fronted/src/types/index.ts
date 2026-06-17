// ========== API 响应 ==========
export interface ApiResult<T = any> {
  code: number;
  message: string;
  data: T;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  page: number;
  pageSize: number;
}

// ========== 实体类型 ==========
export interface StudentDormitory {
  id?: number;
  studentName: string;
  studentNo: string;
  dormitoryNo: string;
  phone?: string;
  checkInDate?: string;
  paymentStatus: string;
  createTime?: string;
}

export interface FeeItem {
  id?: number;
  itemName: string;
  feeType: string;
  unitPrice: number;
  billingCycle: string;
  applicableDormType?: string;
  status: string;
  description?: string;
}

export interface PaymentBill {
  id?: number;
  studentId: number;
  feeItemId: number;
  billNo: string;
  semester: string;
  amount: number;
  paidAmount: number;
  dueDate: string;
  status: string;
  remark?: string;
  studentName?: string;
  studentNo?: string;
  dormitoryNo?: string;
  feeItemName?: string;
  feeType?: string;
  createTime?: string;
}

export interface PaymentRecord {
  id?: number;
  billId: number;
  studentId: number;
  orderNo: string;
  amount: number;
  payMethod: string;
  tradeNo?: string;
  payTime?: string;
  status: string;
  receiptUrl?: string;
  studentName?: string;
  studentNo?: string;
  billNo?: string;
  createTime?: string;
}

export interface DashboardOverview {
  totalStudents: number;
  totalBillsThisSemester: number;
  totalAmount: number;
  paidAmount: number;
  collectionRate: number;
  overdueCount: number;
  overdueAmount: number;
  unpaidCount: number;
  feeTypeDistribution: { feeType: string; count: number; totalAmount: number }[];
}

export interface AiQaResult {
  question: string;
  answer: string;
  source: string;
  confidence: number;
}

export interface AiQaLog {
  id: number;
  userId: string;
  question: string;
  answer: string;
  source: string;
  responseTime: number;
  createTime: string;
}

// ========== 登录 (RBAC) ==========
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

export interface RoleItem {
  id: number;
  roleCode: string;
  roleName: string;
  description?: string;
  status: string;
  createTime: string;
}

export interface UserInfo {
  id: number;
  username: string;
  realName: string;
  role: string;
  status: string;
}
