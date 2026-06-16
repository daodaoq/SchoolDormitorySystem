import request from '../utils/request';

// ===== 学生管理 =====
export const getStudents = (params) => request.get('/students', { params });
export const getStudentById = (id) => request.get(`/students/${id}`);
export const addStudent = (data) => request.post('/students', data);
export const updateStudent = (id, data) => request.put(`/students/${id}`, data);
export const deleteStudent = (id) => request.delete(`/students/${id}`);
export const importStudents = (file) => {
  const formData = new FormData();
  formData.append('file', file);
  return request.post('/students/batch', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
};
export const exportStudents = (params) =>
  request.get('/students/export', { params, responseType: 'blob' });

// ===== 收费项目管理 =====
export const getFeeItems = (params) => request.get('/fee-items', { params });
export const getActiveFeeItems = () => request.get('/fee-items/active');
export const getFeeItemById = (id) => request.get(`/fee-items/${id}`);
export const addFeeItem = (data) => request.post('/fee-items', data);
export const updateFeeItem = (id, data) => request.put(`/fee-items/${id}`, data);
export const deleteFeeItem = (id) => request.delete(`/fee-items/${id}`);

// ===== 账单管理 =====
export const getBills = (params) => request.get('/bills', { params });
export const getBillById = (id) => request.get(`/bills/${id}`);
export const generateBills = (data) => request.post('/bills/generate', data);
export const updateBillStatus = (id, data) => request.put(`/bills/${id}/status`, data);
export const exportBills = (params) =>
  request.get('/bills/export', { params, responseType: 'blob' });

// ===== 在线缴费 =====
export const createPaymentOrder = (billId) =>
  request.post('/payment/create-order', { billId });
export const getPaymentRecords = (params) => request.get('/payment/records', { params });
export const getPaymentDetail = (orderNo) => request.get(`/payment/records/${orderNo}`);

// ===== 统计报表 =====
export const getOverview = () => request.get('/statistics/overview');
export const getCollectionRate = (semester) =>
  request.get('/statistics/collection-rate', { params: { semester } });
export const getArrears = (params) => request.get('/statistics/arrears', { params });
export const getSemesterReport = (semester) =>
  request.get('/statistics/semester-report', { params: { semester } });
export const getMonthlyReport = (year, month) =>
  request.get('/statistics/monthly-report', { params: { year, month } });

// ===== AI问答 =====
export const askAi = (question, userId) =>
  request.post('/ai/ask', { question, userId: userId || 'anonymous' });
export const getAiHistory = (params) => request.get('/ai/history', { params });
