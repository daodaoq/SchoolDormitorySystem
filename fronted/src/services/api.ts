import request from '../utils/request';
import type { ApiResult, PageResult } from '../types';

// ===== 认证 =====
export const loginApi = (username: string, password: string) =>
  request.post<any, ApiResult>('/auth/login', { username, password });

export const getCurrentUser = () =>
  request.get<any, ApiResult>('/auth/me');

// ===== 学生管理 =====
export const getStudents = (params: Record<string, any>) =>
  request.get<any, ApiResult<PageResult<any>>>('/students', { params });

export const getStudentById = (id: number) =>
  request.get<any, ApiResult>(`/students/${id}`);

export const addStudent = (data: any) =>
  request.post<any, ApiResult>('/students', data);

export const updateStudent = (id: number, data: any) =>
  request.put<any, ApiResult>(`/students/${id}`, data);

export const deleteStudent = (id: number) =>
  request.delete<any, ApiResult>(`/students/${id}`);

export const importStudents = (file: File) => {
  const fd = new FormData();
  fd.append('file', file);
  return request.post<any, ApiResult>('/students/batch', fd, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
};

export const exportStudents = (params: Record<string, any>) =>
  request.get('/students/export', { params, responseType: 'blob' });

// ===== 收费项目 =====
export const getFeeItems = (params: Record<string, any>) =>
  request.get<any, ApiResult<PageResult<any>>>('/fee-items', { params });

export const getActiveFeeItems = () =>
  request.get<any, ApiResult>('/fee-items/active');

export const addFeeItem = (data: any) =>
  request.post<any, ApiResult>('/fee-items', data);

export const updateFeeItem = (id: number, data: any) =>
  request.put<any, ApiResult>(`/fee-items/${id}`, data);

export const deleteFeeItem = (id: number) =>
  request.delete<any, ApiResult>(`/fee-items/${id}`);

// ===== 账单 =====
export const getBills = (params: Record<string, any>) =>
  request.get<any, ApiResult<PageResult<any>>>('/bills', { params });

export const generateBills = (data: Record<string, any>) =>
  request.post<any, ApiResult>('/bills/generate', data);

export const updateBillStatus = (id: number, data: Record<string, any>) =>
  request.put<any, ApiResult>(`/bills/${id}/status`, data);

export const exportBills = (params: Record<string, any>) =>
  request.get('/bills/export', { params, responseType: 'blob' });

// ===== 支付 =====
export const createPaymentOrder = (billId: number) =>
  request.post<any, ApiResult>('/payment/create-order', { billId });

export const getPaymentRecords = (params: Record<string, any>) =>
  request.get<any, ApiResult<PageResult<any>>>('/payment/records', { params });

// ===== 统计 =====
export const getOverview = () =>
  request.get<any, ApiResult>('/statistics/overview');

export const getCollectionRate = (semester?: string) =>
  request.get<any, ApiResult>('/statistics/collection-rate', { params: { semester } });

export const getArrears = (params: Record<string, any>) =>
  request.get<any, ApiResult>('/statistics/arrears', { params });

// ===== 用户管理 =====
export const getUsers = (params: Record<string, any>) =>
  request.get<any, ApiResult<PageResult<any>>>('/users', { params });

export const addUser = (data: Record<string, any>) =>
  request.post<any, ApiResult>('/users', data);

export const updateUser = (id: number, data: Record<string, any>) =>
  request.put<any, ApiResult>(`/users/${id}`, data);

export const deleteUser = (id: number) =>
  request.delete<any, ApiResult>(`/users/${id}`);

export const resetUserPassword = (id: number) =>
  request.put<any, ApiResult>(`/users/${id}/reset-password`);

// ===== 宿舍管理 =====
export const getDormitories = (params: Record<string, any>) =>
  request.get<any, ApiResult<PageResult<any>>>('/dormitories', { params });

export const getActiveDormitories = () =>
  request.get<any, ApiResult>('/dormitories/active');

export const addDormitory = (data: any) =>
  request.post<any, ApiResult>('/dormitories', data);

export const updateDormitory = (id: number, data: any) =>
  request.put<any, ApiResult>(`/dormitories/${id}`, data);

export const deleteDormitory = (id: number) =>
  request.delete<any, ApiResult>(`/dormitories/${id}`);

// ===== 菜单管理 =====
export const getMenuTree = () =>
  request.get<any, ApiResult>('/menus/tree');

export const addMenu = (data: any) =>
  request.post<any, ApiResult>('/menus', data);

export const updateMenu = (id: number, data: any) =>
  request.put<any, ApiResult>(`/menus/${id}`, data);

export const deleteMenu = (id: number) =>
  request.delete<any, ApiResult>(`/menus/${id}`);

// ===== AI问答 =====
export const askAi = (question: string, userId?: string) =>
  request.post<any, ApiResult>('/ai/ask', { question, userId: userId || 'anonymous' });

export const getAiHistory = (params: Record<string, any>) =>
  request.get<any, ApiResult<PageResult<any>>>('/ai/history', { params });

export const getAiHealth = () =>
  request.get<any, ApiResult>('/ai/health');

// ===== 知识库 =====
export const getKbDocuments = (params: Record<string, any>) =>
  request.get<any, ApiResult<PageResult<any>>>('/kb/documents', { params });

export const uploadKbDocument = (file: File, title?: string, description?: string) => {
  const fd = new FormData();
  fd.append('file', file);
  if (title) fd.append('title', title);
  if (description) fd.append('description', description);
  return request.post<any, ApiResult>('/kb/documents/upload', fd, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
};

export const deleteKbDocument = (id: number) =>
  request.delete<any, ApiResult>(`/kb/documents/${id}`);

export const getKbDocumentChunks = (id: number) =>
  request.get<any, ApiResult>(`/kb/documents/${id}/chunks`);

export const reprocessKbDocument = (id: number) =>
  request.post<any, ApiResult>(`/kb/documents/${id}/reprocess`);

export const searchKnowledgeBase = (query: string, topK = 5) =>
  request.post<any, ApiResult>('/kb/search', { query, topK });
