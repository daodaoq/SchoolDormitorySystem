/** 仪表盘概览数据 */
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
