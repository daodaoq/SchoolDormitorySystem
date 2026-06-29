import React, { useState, useEffect, useRef } from 'react';
import { Modal, Spin, Tag, Empty } from 'antd';
import { FileTextOutlined, HighlightOutlined } from '@ant-design/icons';
import { getKbDocumentChunks } from '../../services/api';

interface KbChunk {
  id: number;
  documentId: number;
  chunkId?: string;
  chunkIndex: number;
  content: string;
  tokenCount?: number;
}

interface KbDocument {
  id: number;
  title: string;
  fileName: string;
  fileType: string;
  fileSize: number;
  chunkCount: number;
  status: string;
}

interface DocumentViewerProps {
  docId: number;
  docTitle?: string;
  highlightChunkId?: string;
  highlightChunkIndex?: number;
  visible: boolean;
  onClose: () => void;
}

const DocumentViewer: React.FC<DocumentViewerProps> = ({
  docId,
  docTitle,
  highlightChunkId,
  highlightChunkIndex,
  visible,
  onClose,
}) => {
  const [chunks, setChunks] = useState<KbChunk[]>([]);
  const [loading, setLoading] = useState(false);
  const highlightRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (visible && docId > 0) {
      setLoading(true);
      getKbDocumentChunks(docId)
        .then((res: any) => {
          const data = res?.data || res || [];
          setChunks(Array.isArray(data) ? data : []);
        })
        .catch((err) => {
          console.error('加载文档分块失败:', err);
          setChunks([]);
        })
        .finally(() => setLoading(false));
    }
  }, [visible, docId]);

  // 自动滚动到高亮段落
  useEffect(() => {
    if (highlightRef.current && !loading) {
      setTimeout(() => {
        highlightRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }, 300);
    }
  }, [chunks, loading]);

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes}B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)}KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)}MB`;
  };

  const isHighlighted = (chunk: KbChunk): boolean => {
    if (highlightChunkId && chunk.chunkId === highlightChunkId) return true;
    if (highlightChunkIndex !== undefined && chunk.chunkIndex === highlightChunkIndex) return true;
    return false;
  };

  return (
    <Modal
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <FileTextOutlined style={{ color: '#E85D4E' }} />
          <span>{docTitle || '文档查看'}</span>
        </div>
      }
      open={visible}
      onCancel={onClose}
      width={800}
      footer={null}
      destroyOnClose
      styles={{
        body: { maxHeight: '65vh', overflowY: 'auto', padding: '16px 24px' },
      }}
    >
      {loading ? (
        <div style={{ textAlign: 'center', padding: 60 }}>
          <Spin size="large" />
          <div style={{ marginTop: 16, color: 'rgba(26,26,26,0.35)', fontSize: 13 }}>
            加载文档内容中...
          </div>
        </div>
      ) : chunks.length === 0 ? (
        <Empty description="文档已被删除或索引已过期" />
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {/* 统计信息 */}
          <div style={{
            display: 'flex', alignItems: 'center', gap: 12,
            padding: '8px 12px', borderRadius: 10,
            background: '#F5F3ED', fontSize: 12, color: 'rgba(26,26,26,0.45)',
          }}>
            <span>共 {chunks.length} 个段落</span>
            {highlightChunkIndex !== undefined && (
              <Tag color="#E85D4E" style={{ margin: 0 }}>
                <HighlightOutlined /> 引用位置：第 {highlightChunkIndex + 1} 段
              </Tag>
            )}
          </div>

          {/* 段落列表 */}
          {chunks.map((chunk, idx) => {
            const highlighted = isHighlighted(chunk);
            return (
              <div
                key={chunk.id || idx}
                ref={highlighted ? highlightRef : undefined}
                style={{
                  padding: '14px 18px',
                  borderRadius: 12,
                  background: highlighted ? '#FFF8F0' : '#FAF9F5',
                  border: highlighted
                    ? '2px solid #E85D4E'
                    : '1px solid rgba(26,26,26,0.06)',
                  transition: 'all 0.3s',
                  boxShadow: highlighted ? '0 2px 8px rgba(232,93,78,0.15)' : undefined,
                }}
              >
                <div style={{
                  display: 'flex', alignItems: 'center', gap: 8,
                  fontSize: 11, color: highlighted ? '#E85D4E' : 'rgba(26,26,26,0.30)',
                  marginBottom: 8, fontWeight: highlighted ? 600 : 400,
                }}>
                  <span style={{
                    display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                    width: 22, height: 22, borderRadius: 6,
                    background: highlighted ? '#E85D4E' : 'rgba(26,26,26,0.06)',
                    color: highlighted ? '#fff' : 'rgba(26,26,26,0.40)',
                    fontSize: 11, fontWeight: 600,
                  }}>
                    {chunk.chunkIndex + 1}
                  </span>
                  段落 {chunk.chunkIndex + 1}
                  {highlighted && (
                    <Tag color="#E85D4E" style={{ margin: 0, fontSize: 10, lineHeight: '18px' }}>
                      引用位置
                    </Tag>
                  )}
                  {chunk.tokenCount && (
                    <span style={{ marginLeft: 'auto', fontSize: 10 }}>
                      ~{chunk.tokenCount} tokens
                    </span>
                  )}
                </div>
                <p style={{
                  margin: 0, fontSize: 14, lineHeight: 1.85,
                  color: '#1A1A1A', whiteSpace: 'pre-wrap',
                  fontFamily: "'Space Grotesk', -apple-system, BlinkMacSystemFont, 'PingFang SC', sans-serif",
                }}>
                  {chunk.content}
                </p>
              </div>
            );
          })}
        </div>
      )}
    </Modal>
  );
};

export default DocumentViewer;
