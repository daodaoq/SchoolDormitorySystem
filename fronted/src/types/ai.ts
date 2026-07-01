/** AI 问答结果（非流式） */
export interface AiQaResult {
  question: string;
  answer: string;
  source: string;
  confidence: number;
}

/** AI 问答历史记录 */
export interface AiQaLog {
  id: number;
  userId: string;
  question: string;
  answer: string;
  source: string;
  responseTime: number;
  createTime: string;
}
