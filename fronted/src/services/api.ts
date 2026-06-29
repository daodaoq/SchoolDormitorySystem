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

export const createBill = (data: Record<string, any>) =>
  request.post<any, ApiResult>('/bills', data);

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

export const getStudentOverview = (studentId: number) =>
  request.get<any, ApiResult>(`/statistics/student/${studentId}`);

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

// ===== AI问答 =====
export const askAi = (question: string, userId?: string) =>
  request.post<any, ApiResult>('/ai/ask', { question, userId: userId || 'anonymous' });

/**
 * SSE 流式 AI 问答
 * 使用 fetch + ReadableStream 消费 SSE 事件流
 *
 * @param question 用户问题
 * @param callbacks 回调函数集
 * @param callbacks.onChunk 收到内容块时的回调
 * @param callbacks.onDone 流完成时的回调，携带最终状态信息
 * @param callbacks.onError 流出错时的回调
 * @returns AbortController 用于取消请求
 */
export const askAiStream = (
  question: string,
  callbacks: {
    onChunk: (text: string) => void;
    onDone: (info: { status: string; source?: string; totalLength?: number; citations?: Array<{
      markerId?: number; chunkId?: string; docTitle: string; content?: string;
      score: number; docId: number; chunkIndex?: number;
      confidence?: 'HIGH' | 'LOW'; referenced?: boolean;
    }> }) => void;
    onError: (error: string) => void;
  },
  userId?: string,
): AbortController => {
  const abortController = new AbortController();

  const token = (() => {
    try {
      const auth = localStorage.getItem('auth');
      if (auth) {
        const parsed = JSON.parse(auth);
        // Zustand v5 persist 格式为 { state: {...}, version: 0 }，v4 为扁平结构
        return parsed?.state?.token || parsed?.token || '';
      }
    } catch { /* ignore */ }
    return '';
  })();

  fetch('/api/ai/ask/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify({ question, userId: userId || 'anonymous' }),
    signal: abortController.signal,
  })
    .then(async (response) => {
      if (!response.ok) {
        if (response.status === 401) {
          localStorage.removeItem('auth');
          window.location.assign('/login');
          return;
        }
        callbacks.onError(`HTTP ${response.status}: ${response.statusText}`);
        return;
      }

      const reader = response.body?.getReader();
      if (!reader) {
        callbacks.onError('浏览器不支持流式读取');
        return;
      }

      const decoder = new TextDecoder();
      let buffer = '';

      try {
        while (true) {
          const { done, value } = await reader.read();
          if (done) break;

          buffer += decoder.decode(value, { stream: true });

          // SSE 协议：事件以双换行 \n\n 分隔
          // 找到最后一个 \n\n，之前的是完整事件，之后的是不完整的事件留在 buffer
          const lastDoubleNewline = buffer.lastIndexOf('\n\n');
          if (lastDoubleNewline === -1) continue; // 还没有完整事件

          const complete = buffer.substring(0, lastDoubleNewline);
          buffer = buffer.substring(lastDoubleNewline + 2); // 跳过 \n\n

          // 处理所有完整事件（可能有多个事件被 \n\n 连接）
          const events = complete.split('\n\n');
          for (const eventBlock of events) {
            if (!eventBlock.trim()) continue;
            parseSSEEvent(eventBlock, callbacks);
          }
        }

        // 流结束后处理残留 buffer（最后一个事件可能没有 \n\n 结尾）
        if (buffer.trim()) {
          parseSSEEvent(buffer, callbacks);
        }
      } catch (err: any) {
        if (err.name === 'AbortError') return;
        callbacks.onError(err.message || '流式读取失败');
      }
    })
    .catch((err) => {
      if (err.name === 'AbortError') return;
      callbacks.onError(err.message || '网络请求失败');
    });

  return abortController;
};

/** 解析单个 SSE 事件块 */
function parseSSEEvent(
  eventBlock: string,
  callbacks: {
    onChunk: (text: string) => void;
    onDone: (info: any) => void;
    onError: (error: string) => void;
  },
) {
  const lines = eventBlock.split('\n');
  let eventType = '';
  const dataLines: string[] = [];

  for (const line of lines) {
    if (line.startsWith('event:')) {
      eventType = line.slice(6).trim();
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trim());
    } else if (line.startsWith(':')) {
      // heartbeat comment，忽略
    }
    // 空行忽略
  }

  if (dataLines.length === 0) return;

  const data = dataLines.join('\n');

  switch (eventType) {
    case 'content':
      callbacks.onChunk(data);
      break;
    case 'done':
      try {
        callbacks.onDone(JSON.parse(data));
      } catch {
        callbacks.onDone({ status: 'completed' });
      }
      break;
    case 'error':
      callbacks.onError(data);
      break;
    default:
      // 未知事件类型，忽略
      break;
  }
}

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

// ===== 人员管理 =====
export const getStudentPersonnel = (params: Record<string, any>) =>
  request.get<any, ApiResult<PageResult<any>>>('/students/personnel', { params });

export const linkStudentToUser = (studentId: number, userId: number) =>
  request.put<any, ApiResult>(`/students/${studentId}/link-user`, { userId });

export const unlinkStudentUser = (studentId: number) =>
  request.put<any, ApiResult>(`/students/${studentId}/unlink-user`);
