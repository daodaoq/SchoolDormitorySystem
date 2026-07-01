import type { LoginResult, MenuItem, Citation, Message, Conversation } from './index';

// ========== Auth Context ==========
export interface AuthContextType {
  user: LoginResult | null;
  token: string | null;
  permissions: string[];
  menus: MenuItem[];
  login: (data: LoginResult) => void;
  logout: () => void;
  isAuthenticated: boolean;
  initializing: boolean;
  hasPermission: (code: string) => boolean;
}

// ========== Auth Store ==========
export interface AuthState {
  user: LoginResult | null;
  token: string | null;
  permissions: string[];
  menus: MenuItem[];
  initializing: boolean;
  login: (data: LoginResult) => void;
  logout: () => void;
  setInitialized: () => void;
  hasPermission: (code: string) => boolean;
}

// ========== Chat Store ==========
export interface ChatState {
  userId: string;
  conversations: Conversation[];
  activeId: string;
  activeConversation: () => Conversation | null;
  messages: () => Message[];
  setUserId: (userId: string) => void;
  newConversation: () => void;
  switchConversation: (id: string) => void;
  deleteConversation: (id: string) => void;
  addUserMessage: (content: string) => void;
  addAssistantMessage: (content: string, source?: string, citations?: Citation[]) => void;
}
