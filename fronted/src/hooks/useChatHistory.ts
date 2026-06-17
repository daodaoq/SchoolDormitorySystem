import { useState, useCallback, useEffect, useRef } from 'react';

interface Citation {
  docTitle: string;
  content?: string;
  score: number;
  docId: number;
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

function getStorageKey(userId: string): string {
  return `chat-conversations-${userId}`;
}

function loadConversations(storageKey: string): Conversation[] {
  try {
    const raw = localStorage.getItem(storageKey);
    if (raw) return JSON.parse(raw);
  } catch { /* ignore */ }
  return [];
}

function saveConversations(storageKey: string, convs: Conversation[]) {
  try {
    localStorage.setItem(storageKey, JSON.stringify(convs));
  } catch { /* ignore */ }
}

function generateId(): string {
  return Date.now().toString(36) + Math.random().toString(36).slice(2, 8);
}

/**
 * 从消息中自动生成对话标题
 */
function deriveTitle(messages: Message[]): string {
  const firstUser = messages.find((m) => m.role === 'user');
  if (firstUser) {
    const text = firstUser.content.replace(/\s+/g, ' ').trim();
    return text.length > 24 ? text.slice(0, 24) + '…' : text;
  }
  return '新对话';
}

export function useChatHistory(userId: string) {
  const storageKey = getStorageKey(userId);
  const [conversations, setConversations] = useState<Conversation[]>(() => loadConversations(storageKey));
  const [activeId, setActiveId] = useState<string>('');
  const prevUserIdRef = useRef(userId);

  // 切换用户时重新加载该用户的对话
  useEffect(() => {
    if (prevUserIdRef.current !== userId) {
      prevUserIdRef.current = userId;
      const key = getStorageKey(userId);
      const convs = loadConversations(key);
      setConversations(convs);
      setActiveId(convs.length > 0 ? convs[0].id : '');
    }
  }, [userId]);

  // 初始化：如果没有活跃对话，自动选第一个
  useEffect(() => {
    if (conversations.length > 0 && !activeId) {
      setActiveId(conversations[0].id);
    }
  }, []);

  // 持久化（按用户隔离）
  useEffect(() => {
    saveConversations(storageKey, conversations);
  }, [conversations, storageKey]);

  const activeConversation = conversations.find((c) => c.id === activeId) || null;

  /** 获取活跃对话的消息（方便直接使用） */
  const messages = activeConversation?.messages || [];

  /** 新建对话 */
  const newConversation = useCallback(() => {
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
    setConversations((prev) => {
      // 限制最大数量
      const trimmed = prev.length >= MAX_CONVERSATIONS ? prev.slice(0, MAX_CONVERSATIONS - 1) : prev;
      return [conv, ...trimmed];
    });
    setActiveId(conv.id);
  }, []);

  /** 切换对话 */
  const switchConversation = useCallback((id: string) => {
    setActiveId(id);
  }, []);

  /** 删除对话 */
  const deleteConversation = useCallback((id: string) => {
    setConversations((prev) => {
      const next = prev.filter((c) => c.id !== id);
      if (id === activeId) {
        // 如果删除的是当前活跃对话，切换到第一个
        const newActiveId = next.length > 0 ? next[0].id : '';
        setTimeout(() => setActiveId(newActiveId), 0);
      }
      return next;
    });
  }, [activeId]);

  /** 添加用户消息 */
  const addUserMessage = useCallback((content: string) => {
    setConversations((prev) =>
      prev.map((c) => {
        if (c.id !== activeId) return c;
        const msg: Message = { role: 'user', content };
        return {
          ...c,
          messages: [...c.messages, msg],
          // 用第一条用户消息自动设置标题
          title: c.title === '新对话' ? deriveTitle([...c.messages, msg]) : c.title,
        };
      }),
    );
  }, [activeId]);

  /** 添加助手消息（用于流式完成后固化） */
  const addAssistantMessage = useCallback(
    (content: string, source?: string, citations?: Citation[]) => {
      setConversations((prev) =>
        prev.map((c) => {
          if (c.id !== activeId) return c;
          const msg: Message = { role: 'assistant', content, source, citations };
          return { ...c, messages: [...c.messages, msg] };
        }),
      );
    },
    [activeId],
  );

  return {
    conversations,
    activeConversation,
    activeId,
    messages,
    newConversation,
    switchConversation,
    deleteConversation,
    addUserMessage,
    addAssistantMessage,
  };
}
