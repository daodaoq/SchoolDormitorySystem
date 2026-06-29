import { create } from 'zustand';

interface Citation {
  markerId?: number;
  chunkId?: string;
  docTitle: string;
  content?: string;
  score: number;
  docId: number;
  chunkIndex?: number;
  confidence?: 'HIGH' | 'LOW';
  referenced?: boolean;
}

export interface Message {
  role: 'user' | 'assistant';
  content: string;
  source?: string;
  citations?: Citation[];
}

export interface Conversation {
  id: string;
  title: string;
  messages: Message[];
  createdAt: number;
}

const MAX_CONVERSATIONS = 50;

// ---- localStorage helpers ----
function load(userId: string): Conversation[] {
  try {
    const raw = localStorage.getItem(`chat-conversations-${userId}`);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

function save(userId: string, convs: Conversation[]) {
  try {
    localStorage.setItem(`chat-conversations-${userId}`, JSON.stringify(convs));
  } catch { /* quota exceeded */ }
}

function generateId(): string {
  return Date.now().toString(36) + Math.random().toString(36).slice(2, 8);
}

function deriveTitle(messages: Message[]): string {
  const first = messages.find((m) => m.role === 'user');
  if (first) {
    const text = first.content.replace(/\s+/g, ' ').trim();
    return text.length > 24 ? text.slice(0, 24) + '…' : text;
  }
  return '新对话';
}

// ---- Store ----
interface ChatState {
  userId: string;
  conversations: Conversation[];
  activeId: string;

  // derived
  activeConversation: () => Conversation | null;
  messages: () => Message[];

  // actions
  setUserId: (userId: string) => void;
  newConversation: () => void;
  switchConversation: (id: string) => void;
  deleteConversation: (id: string) => void;
  addUserMessage: (content: string) => void;
  addAssistantMessage: (content: string, source?: string, citations?: Citation[]) => void;
}

export const useChatStore = create<ChatState>((set, get) => ({
  userId: 'anonymous',
  conversations: [],
  activeId: '',

  activeConversation: () => {
    const { conversations, activeId } = get();
    return conversations.find((c) => c.id === activeId) || null;
  },

  messages: () => {
    const conv = get().activeConversation();
    return conv?.messages || [];
  },

  setUserId: (userId: string) => {
    const convs = load(userId);
    set({
      userId,
      conversations: convs,
      activeId: convs.length > 0 ? convs[0].id : '',
    });
  },

  newConversation: () => {
    const { userId } = get();
    const conv: Conversation = {
      id: generateId(),
      title: '新对话',
      messages: [
        {
          role: 'assistant',
          content: '您好！我是宿舍收费管理系统的智能助手。请问有什么可以帮您的？',
          source: 'SYSTEM',
        },
      ],
      createdAt: Date.now(),
    };
    set((s) => {
      const trimmed = s.conversations.length >= MAX_CONVERSATIONS
        ? s.conversations.slice(0, MAX_CONVERSATIONS - 1)
        : s.conversations;
      const next = [conv, ...trimmed];
      save(userId, next);
      return { conversations: next, activeId: conv.id };
    });
  },

  switchConversation: (id: string) => set({ activeId: id }),

  deleteConversation: (id: string) => {
    const { userId } = get();
    set((s) => {
      const next = s.conversations.filter((c) => c.id !== id);
      save(userId, next);
      return {
        conversations: next,
        activeId: s.activeId === id ? (next[0]?.id || '') : s.activeId,
      };
    });
  },

  addUserMessage: (content: string) => {
    const { userId } = get();
    set((s) => {
      const next = s.conversations.map((c) => {
        if (c.id !== s.activeId) return c;
        const msg: Message = { role: 'user', content };
        const newMessages = [...c.messages, msg];
        return {
          ...c,
          messages: newMessages,
          title: c.title === '新对话' ? deriveTitle(newMessages) : c.title,
        };
      });
      save(userId, next);
      return { conversations: next };
    });
  },

  addAssistantMessage: (content: string, source?: string, citations?: Citation[]) => {
    const { userId } = get();
    set((s) => {
      const next = s.conversations.map((c) => {
        if (c.id !== s.activeId) return c;
        const msg: Message = { role: 'assistant', content, source, citations };
        return { ...c, messages: [...c.messages, msg] };
      });
      save(userId, next);
      return { conversations: next };
    });
  },
}));
