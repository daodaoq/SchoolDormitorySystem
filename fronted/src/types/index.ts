// ========== 统一类型入口 ==========
// 按领域拆分，所有类型由此文件 re-export，
// 外部统一 `import { ... } from '../types'` 即可。
//
// 注意：React 组件 Props 类型保留在各组件文件中（React 惯例），不在此统一管理。

export type { ApiResult, PageResult } from './api';
export type { MenuItem, LoginResult, RoleItem, UserInfo } from './auth';
export type { Citation, Message, Conversation } from './chat';
export type {
  StudentDormitory,
  FeeItem,
  PaymentBill,
  PaymentRecord,
  DormitoryRecord,
  LogRecord,
} from './entities';
export type { DashboardOverview } from './dashboard';
export type { AiQaResult, AiQaLog } from './ai';
export type { KbDocument, KbChunk, BatchFile } from './kb';
export type { AuthContextType, AuthState, ChatState } from './store';
