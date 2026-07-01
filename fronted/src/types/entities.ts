// ========== 学生 ==========

export interface StudentDormitory {
  id?: number;
  studentName: string;
  studentNo: string;
  dormitoryNo: string;
  phone?: string;
  checkInDate?: string;
  paymentStatus: string;
  photo?: string;
  userId?: number;
  createTime?: string;
}

// ========== 收费项目 ==========

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

// ========== 账单 ==========

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

// ========== 支付记录 ==========

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

// ========== 宿舍 ==========

export interface DormitoryRecord {
  id?: number;
  dormitoryNo: string;
  building?: string;
  floor?: string;
  roomType?: string;
  capacity?: number;
  status: string;
}

// ========== 操作日志 ==========

export interface LogRecord {
  id: number;
  userId?: number;
  username?: string;
  realName?: string;
  module: string;
  action: string;
  description?: string;
  method?: string;
  requestParams?: string;
  duration?: number;
  status: string;
  errorMsg?: string;
  ipAddress?: string;
  createTime: string;
}
