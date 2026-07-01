/** AI 引用文献条目 */
export interface Citation {
  markerId?: number;       // 引用标记编号，对应回答中的 [N]
  chunkId?: string;        // Milvus chunk_id，用于段落定位
  docTitle: string;
  content?: string;
  score: number;
  docId: number;
  chunkIndex?: number;     // 段落序号（从0开始）
  confidence?: 'HIGH' | 'LOW';  // 溯源置信度
  referenced?: boolean;    // LLM 是否实际引用了此来源
}

/** 聊天消息 */
export interface Message {
  role: 'user' | 'assistant';
  content: string;
  source?: string;
  citations?: Citation[];
}

/** 对话 */
export interface Conversation {
  id: string;
  title: string;
  messages: Message[];
  createdAt: number;
}
