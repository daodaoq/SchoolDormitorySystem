/** 知识库文档 */
export interface KbDocument {
  id: number;
  title: string;
  description?: string;
  fileName: string;
  fileType: string;
  fileSize: number;
  chunkCount: number;
  status: string;
  errorMsg?: string;
  createTime?: string;
}

/** 文档分块 */
export interface KbChunk {
  id: number;
  documentId: number;
  chunkId?: string;
  chunkIndex: number;
  content: string;
  tokenCount?: number;
}

// 批量上传状态
export interface BatchFile {
  uid: string;
  name: string;
  size: number;
  status: 'pending' | 'uploading' | 'success' | 'error';
  errorMsg?: string;
}