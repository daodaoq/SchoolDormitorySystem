import React, { useState, useRef, useEffect, useCallback } from 'react';
import { Tag } from 'antd';
import { RobotOutlined, UserOutlined, PlusOutlined, DeleteOutlined, MessageOutlined } from '@ant-design/icons';
import { ArrowUp, Square } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeRaw from 'rehype-raw';
import { askAiStream } from '../../services/api';
import { useChatStore, type Conversation, type Message } from '../../stores/chatStore';
import { useAuthStore } from '../../stores/authStore';
import DocumentViewer from '../../components/DocumentViewer';
import {
  PromptInput,
  PromptInputTextarea,
  PromptInputActions,
  PromptInputAction,
} from '@/components/ui/prompt-input';
import { Button } from '@/components/ui/button';

// ==================== 常量 & 工具 ====================

const sourceColors: Record<string, string> = {
  AI: '#E85D4E', AI_STREAM: '#E85D4E', LOCAL_KB: '#C4D94E', CACHE: '#8BB4F7',
  FALLBACK: '#F2D160', RATE_LIMIT: '#F5B895', SYSTEM: '#C5B5E0', ERROR: '#E85D4E',
  GREETING: '#8BB4F7', OFF_TOPIC: '#F2D160',
};

const sourceLabelMap: Record<string, string> = {
  AI: 'AI模型', AI_STREAM: 'AI流式', LOCAL_KB: '本地知识库', CACHE: '缓存',
  FALLBACK: '降级回复', RATE_LIMIT: '限流', SYSTEM: '系统', ERROR: '错误',
  GREETING: '欢迎', OFF_TOPIC: '超出范围',
};

const quickQuestions = [
  { q: '住宿费多少钱？', color: '#E85D4E' },
  { q: '怎么缴费？', color: '#C4D94E' },
  { q: '逾期了怎么办？', color: '#F2D160' },
  { q: '怎么办理入住？', color: '#8BB4F7' },
  { q: '如何换宿舍？', color: '#C5B5E0' },
];

function normalizeMarkdown(text: string): string {
  return text
    .split('\n')
    .map((line) => {
      let fixed = line;
      fixed = fixed.replace(/^(#{1,6})([^\s#])/, '$1 $2');
      fixed = fixed.replace(/^([-*])([^\s-*])/, '$1 $2');
      fixed = fixed.replace(/^(\d+\.)([^\s])/, '$1 $2');
      fixed = fixed.replace(/^(>)([^\s>])/, '$1 $2');
      return fixed;
    })
    .join('\n');
}

/**
 * 将 AI 回答中的 [N] 引用标记转为可点击的 HTML sup 标签
 * 注意：[N] 在 markdown 中可能被误解为链接引用，需预处理
 */
function injectCitationMarkers(markdown: string): string {
  // 保护代码块内的 [N]：先提取代码块，替换后还原
  const codeBlocks: string[] = [];
  let protected_ = markdown.replace(/```[\s\S]*?```/g, (match) => {
    codeBlocks.push(match);
    return `%%CODEBLOCK_${codeBlocks.length - 1}%%`;
  });
  protected_ = protected_.replace(/`[^`]*`/g, (match) => {
    codeBlocks.push(match);
    return `%%CODEBLOCK_${codeBlocks.length - 1}%%`;
  });

  // 将 [1] [1,3] [1,2,3] 转为 HTML sup 标签
  protected_ = protected_.replace(
    /\[(\d+(?:,\d+)*)\]/g,
    '<sup class="citation-ref" data-refs="$1">[$1]</sup>'
  );

  // 还原代码块
  protected_ = protected_.replace(/%%CODEBLOCK_(\d+)%%/g, (_m, idx) => {
    return codeBlocks[parseInt(idx)] || '';
  });

  return protected_;
}

function formatTime(ts: number): string {
  const now = Date.now();
  const diff = now - ts;
  if (diff < 60_000) return '刚刚';
  if (diff < 3600_000) return `${Math.floor(diff / 60_000)}分钟前`;
  if (diff < 86_400_000) return `${Math.floor(diff / 3600_000)}小时前`;
  return `${Math.floor(diff / 86_400_000)}天前`;
}

// ==================== Markdown 组件定制 ====================

const markdownComponents: Record<string, React.FC<any>> = {
  a: ({ href, children, ...props }: any) => (
    <a href={href} target="_blank" rel="noopener noreferrer" style={{ color: '#E85D4E', fontWeight: 500 }} {...props}>
      {children}
    </a>
  ),
  code: ({ className, children, ...props }: any) => {
    const isInline = !className;
    if (isInline) {
      return (
        <code style={{
          background: 'rgba(232,93,78,0.08)', color: '#E85D4E',
          padding: '1px 6px', borderRadius: 4, fontSize: '0.9em',
          fontFamily: "'JetBrains Mono', 'Fira Code', monospace",
        }} {...props}>{children}</code>
      );
    }
    return (
      <pre style={{
        background: '#F5F3ED', borderRadius: 10, padding: '12px 16px',
        overflow: 'auto', fontSize: 13, lineHeight: 1.6,
        border: '1px solid rgba(26,26,26,0.06)',
      }}>
        <code className={className} {...props}>{children}</code>
      </pre>
    );
  },
  blockquote: ({ children, ...props }: any) => (
    <blockquote style={{
      borderLeft: '3px solid #E85D4E', paddingLeft: 14, margin: '8px 0',
      color: 'rgba(26,26,26,0.6)', fontStyle: 'italic',
    }} {...props}>{children}</blockquote>
  ),
  table: ({ children, ...props }: any) => (
    <div style={{ overflow: 'auto', margin: '8px 0' }}>
      <table style={{ borderCollapse: 'collapse', width: '100%', fontSize: 13 }} {...props}>{children}</table>
    </div>
  ),
  th: ({ children, ...props }: any) => (
    <th style={{
      border: '1px solid rgba(26,26,26,0.1)', padding: '6px 12px',
      background: '#F5F3ED', fontWeight: 600, textAlign: 'left',
    }} {...props}>{children}</th>
  ),
  td: ({ children, ...props }: any) => (
    <td style={{
      border: '1px solid rgba(26,26,26,0.08)', padding: '6px 12px',
    }} {...props}>{children}</td>
  ),
  hr: (props: any) => (
    <hr style={{ border: 'none', borderTop: '1px solid rgba(26,26,26,0.08)', margin: '12px 0' }} {...props} />
  ),
};

/** 创建带溯源点击功能的 markdownComponents */
function createMarkdownComponents(
  citations: any[],
  onCitationClick: (docId: number, docTitle: string, chunkId: string, chunkIndex: number) => void,
) {
  return {
    ...markdownComponents,
    sup: ({ children, node, ...props }: any) => {
      // 检查是否是引用标记
      const className = props.className || '';
      const dataRefs = props['data-refs'] || '';
      if (className.includes('citation-ref') && dataRefs) {
        const markerIds = dataRefs.split(',').map(Number);
        // 找到第一个匹配的 citation
        const firstMarkerId = markerIds[0];
        const citation = citations?.find((c: any) => c.markerId === firstMarkerId);
        const docId = citation?.docId;
        const docTitle = citation?.docTitle || '';
        const chunkId = citation?.chunkId || '';
        const chunkIndex = citation?.chunkIndex ?? 0;

        return (
          <sup
            onClick={(e) => {
              e.stopPropagation();
              if (docId) {
                onCitationClick(docId, docTitle, chunkId, chunkIndex);
              }
            }}
            style={{
              cursor: docId ? 'pointer' : 'default',
              color: '#E85D4E',
              fontWeight: 700,
              fontSize: '0.75em',
              textDecoration: 'none',
              padding: '0 1px',
              transition: 'all 0.15s',
            }}
            title={docId ? `查看来源: ${docTitle}` : undefined}
            onMouseEnter={(e) => {
              if (docId) {
                e.currentTarget.style.background = 'rgba(232,93,78,0.12)';
                e.currentTarget.style.borderRadius = '3px';
              }
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.background = 'transparent';
            }}
          >
            {children}
          </sup>
        );
      }
      return <sup {...props}>{children}</sup>;
    },
  };
}

// ==================== 组件主体 ====================

const AiQa: React.FC = () => {
  const user = useAuthStore((s) => s.user);
  const userId = user?.username || 'anonymous';

  // chat store
  const conversations = useChatStore((s) => s.conversations);
  const activeId = useChatStore((s) => s.activeId);
  const setUserId = useChatStore((s) => s.setUserId);
  const newConversation = useChatStore((s) => s.newConversation);
  const switchConversation = useChatStore((s) => s.switchConversation);
  const deleteConversation = useChatStore((s) => s.deleteConversation);
  const addUserMessage = useChatStore((s) => s.addUserMessage);
  const addAssistantMessage = useChatStore((s) => s.addAssistantMessage);
  // messages is a function in the store, compute it
  const messages = useChatStore((s) => {
    const conv = s.conversations.find((c) => c.id === s.activeId);
    return conv?.messages || [];
  });

  // sync userId on mount/change
  useEffect(() => {
    setUserId(userId);
  }, [userId, setUserId]);

  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [streamingText, setStreamingText] = useState('');
  const [hoveredConv, setHoveredConv] = useState<string | null>(null);
  const listRef = useRef<HTMLDivElement>(null);
  const abortRef = useRef<AbortController | null>(null);
  const streamBufferRef = useRef('');
  const doneHandledRef = useRef(false);

  // 文档查看器状态
  const [docViewerVisible, setDocViewerVisible] = useState(false);
  const [viewingDocId, setViewingDocId] = useState<number>(0);
  const [viewingDocTitle, setViewingDocTitle] = useState<string>('');
  const [viewingChunkId, setViewingChunkId] = useState<string>('');
  const [viewingChunkIndex, setViewingChunkIndex] = useState<number>(0);

  const handleViewDocument = useCallback((docId: number, docTitle: string, chunkId?: string, chunkIndex?: number) => {
    setViewingDocId(docId);
    setViewingDocTitle(docTitle);
    setViewingChunkId(chunkId || '');
    setViewingChunkIndex(chunkIndex ?? 0);
    setDocViewerVisible(true);
  }, []);

  // 如果没有任何对话，自动创建一个
  useEffect(() => {
    if (conversations.length === 0) {
      newConversation();
    }
  }, [conversations.length, newConversation]);

  // 自动滚动到底部
  useEffect(() => {
    if (listRef.current) {
      listRef.current.scrollTop = listRef.current.scrollHeight;
    }
  }, [messages, streamingText]);

  // 发送消息
  const handleSubmit = useCallback(() => {
    if (!input.trim() || loading || !activeId) return;
    const question = input.trim();
    setInput('');
    addUserMessage(question);
    setLoading(true);
    setStreamingText('');
    streamBufferRef.current = '';
    doneHandledRef.current = false;

    const abortController = askAiStream(
      question,
      {
        onChunk: (text) => {
          streamBufferRef.current += text;
          setStreamingText(streamBufferRef.current);
        },
        onDone: (info) => {
          if (doneHandledRef.current) return;
          doneHandledRef.current = true;
          addAssistantMessage(
            streamBufferRef.current,
            info.source || 'AI_STREAM',
            info.citations,
          );
          setStreamingText('');
          setLoading(false);
          abortRef.current = null;
        },
        onError: (error) => {
          if (doneHandledRef.current) return;
          doneHandledRef.current = true;
          const partial = streamBufferRef.current;
          addAssistantMessage(
            partial || `抱歉，AI 服务出现了问题：${error}`,
            partial ? 'AI_STREAM' : 'ERROR',
          );
          setStreamingText('');
          setLoading(false);
          abortRef.current = null;
        },
      },
    );

    abortRef.current = abortController;
  }, [input, loading, activeId, addUserMessage, addAssistantMessage]);

  // 停止生成
  const handleStop = useCallback(() => {
    if (abortRef.current) {
      abortRef.current.abort();
      abortRef.current = null;
    }
    doneHandledRef.current = true;
    const partial = streamBufferRef.current;
    if (partial) {
      addAssistantMessage(partial + '\n\n> ⚠️ *已停止生成*', 'AI_STREAM');
    }
    setStreamingText('');
    setLoading(false);
  }, [addAssistantMessage]);

  return (
    <div style={{ display: 'flex', height: 'calc(100vh - 112px)', gap: 20, padding: 24 }}>
      {/* ====== 主聊天区 ====== */}
      <div style={{
        flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden',
        borderRadius: 24, border: '1px solid rgba(26,26,26,0.10)',
        background: '#FAF9F5', boxShadow: '2px 2px 0 rgba(26,26,26,0.04)',
      }}>
        {/* 顶部标题栏 */}
        <div style={{
          padding: '18px 24px',
          borderBottom: '1px solid rgba(26,26,26,0.06)',
          background: '#FFFFFF',
          borderTopLeftRadius: 24,
          borderTopRightRadius: 24,
          display: 'flex',
          alignItems: 'center',
          gap: 10,
        }}>
          <span style={{ fontSize: 18 }}>💬</span>
          <span style={{
            fontSize: 14, fontWeight: 600, color: '#1A1A1A',
            fontFamily: "'Space Grotesk', -apple-system, BlinkMacSystemFont, 'PingFang SC', sans-serif",
          }}>
            {conversations.find((c) => c.id === activeId)?.title || 'AI 智能问答'}
          </span>
        </div>

        {/* 消息列表 */}
        <div ref={listRef} style={{ flex: 1, overflow: 'auto', padding: '24px 24px 12px' }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
            {messages.map((msg, idx) => (
              <MessageBubble key={idx} msg={msg} onCitationClick={handleViewDocument} />
            ))}

            {loading && streamingText && <StreamingBubble content={streamingText} />}
            {loading && !streamingText && <LoadingBubble />}

            {/* 空状态 */}
            {!loading && messages.length === 0 && (
              <div style={{
                textAlign: 'center', padding: '60px 20px',
                color: 'rgba(26,26,26,0.25)', fontSize: 14,
              }}>
                <div style={{ fontSize: 48, marginBottom: 16 }}>💬</div>
                选择一个对话或新建对话开始聊天
              </div>
            )}
          </div>
        </div>

        {/* 底部输入区 */}
        <div style={{
          padding: '12px 20px 20px', borderTop: '1px solid rgba(26,26,26,0.06)',
          background: '#FFFFFF', borderBottomLeftRadius: 24, borderBottomRightRadius: 24,
        }}>
          <PromptInput value={input} onValueChange={setInput} isLoading={loading} onSubmit={handleSubmit}>
            <PromptInputTextarea
              placeholder={activeId ? '输入你的问题，Enter 发送，Shift+Enter 换行…' : '请先创建或选择一个对话'}
              disabled={loading || !activeId}
            />
            <PromptInputActions className="justify-end pt-2">
              <PromptInputAction tooltip={loading ? '停止生成' : '发送消息'}>
                <Button
                  variant={input.trim() || loading ? 'default' : 'secondary'}
                  size="icon"
                  className="h-9 w-9 rounded-full"
                  disabled={!activeId && !loading}
                  onClick={loading ? handleStop : handleSubmit}
                >
                  {loading ? (
                    <Square className="size-4 fill-current" />
                  ) : (
                    <ArrowUp className="size-4" />
                  )}
                </Button>
              </PromptInputAction>
            </PromptInputActions>
          </PromptInput>
        </div>
      </div>

      {/* ====== 右侧边栏：对话列表 + 快捷问题 ====== */}
      <div style={{
        width: 240, flexShrink: 0, display: 'flex', flexDirection: 'column', gap: 16,
      }}>
        {/* 对话列表卡片 */}
        <div style={{
          flex: 1, borderRadius: 24, border: '1px solid rgba(26,26,26,0.10)',
          background: '#FFFFFF', boxShadow: '2px 2px 0 rgba(26,26,26,0.04)',
          padding: '18px 14px', display: 'flex', flexDirection: 'column',
          overflow: 'hidden',
        }}>
          {/* 新建对话按钮 */}
          <button
            onClick={newConversation}
            disabled={loading}
            style={{
              display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6,
              width: '100%', padding: '10px 0', marginBottom: 14,
              borderRadius: 14, border: '1px dashed rgba(26,26,26,0.15)',
              background: 'transparent', cursor: loading ? 'not-allowed' : 'pointer',
              fontSize: 13, fontWeight: 500, color: '#1A1A1A',
              fontFamily: "'Space Grotesk', -apple-system, BlinkMacSystemFont, 'PingFang SC', sans-serif",
              transition: 'all 0.15s', opacity: loading ? 0.4 : 1,
            }}
            onMouseEnter={(e) => {
              if (!loading) {
                e.currentTarget.style.background = '#EDE8E0';
                e.currentTarget.style.border = '1px solid rgba(26,26,26,0.20)';
              }
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.background = 'transparent';
              e.currentTarget.style.border = '1px dashed rgba(26,26,26,0.15)';
            }}
          >
            <PlusOutlined style={{ fontSize: 12 }} />
            新建对话
          </button>

          {/* 对话列表 */}
          <div style={{
            flex: 1, overflowY: 'auto',
            display: 'flex', flexDirection: 'column', gap: 2,
          }}>
            <div style={{
              fontSize: 10, fontWeight: 600, color: 'rgba(26,26,26,0.25)',
              letterSpacing: '0.08em', textTransform: 'uppercase',
              padding: '0 6px', marginBottom: 4,
            }}>
              最近对话
            </div>
            {conversations.map((conv) => (
              <div
                key={conv.id}
                onClick={() => { if (!loading) switchConversation(conv.id); }}
                onMouseEnter={() => setHoveredConv(conv.id)}
                onMouseLeave={() => setHoveredConv(null)}
                style={{
                  display: 'flex', alignItems: 'center', gap: 8,
                  padding: '9px 10px', borderRadius: 12,
                  cursor: loading ? 'not-allowed' : 'pointer',
                  background: conv.id === activeId ? '#EDE8E0' : 'transparent',
                  border: conv.id === activeId
                    ? '1px solid rgba(26,26,26,0.10)'
                    : '1px solid transparent',
                  transition: 'all 0.12s',
                  opacity: loading ? 0.5 : 1,
                }}
              >
                <MessageOutlined style={{
                  fontSize: 12,
                  color: conv.id === activeId ? '#E85D4E' : 'rgba(26,26,26,0.25)',
                  flexShrink: 0,
                }} />
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{
                    fontSize: 12, fontWeight: conv.id === activeId ? 600 : 400,
                    color: '#1A1A1A',
                    fontFamily: "'Space Grotesk', -apple-system, BlinkMacSystemFont, 'PingFang SC', sans-serif",
                    overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
                    lineHeight: 1.4,
                  }}>
                    {conv.title}
                  </div>
                  <div style={{
                    fontSize: 10, color: 'rgba(26,26,26,0.25)',
                    marginTop: 1,
                  }}>
                    {formatTime(conv.createdAt)}
                  </div>
                </div>
                {hoveredConv === conv.id && conversations.length > 1 && (
                  <DeleteOutlined
                    onClick={(e) => {
                      e.stopPropagation();
                      deleteConversation(conv.id);
                    }}
                    style={{
                      fontSize: 12, color: 'rgba(26,26,26,0.30)',
                      flexShrink: 0, padding: 4, borderRadius: 6,
                      transition: 'all 0.12s',
                    }}
                    onMouseEnter={(e) => { e.currentTarget.style.color = '#E85D4E'; }}
                    onMouseLeave={(e) => { e.currentTarget.style.color = 'rgba(26,26,26,0.30)'; }}
                  />
                )}
              </div>
            ))}
          </div>
        </div>

        {/* 快捷问题卡片 */}
        <div style={{
          borderRadius: 24, border: '1px solid rgba(26,26,26,0.10)',
          background: '#FFFFFF', boxShadow: '2px 2px 0 rgba(26,26,26,0.04)',
          padding: '18px 14px', display: 'flex', flexDirection: 'column', gap: 2,
        }}>
          <div style={{
            fontSize: 10, fontWeight: 600, color: 'rgba(26,26,26,0.25)',
            letterSpacing: '0.08em', textTransform: 'uppercase',
            padding: '0 6px', marginBottom: 6,
          }}>
            快捷问题
          </div>
          {quickQuestions.map((item, idx) => (
            <button
              key={idx}
              onClick={() => { if (!loading && activeId) setInput(item.q); }}
              style={{
                display: 'flex', alignItems: 'center', gap: 8, width: '100%',
                textAlign: 'left' as const, fontSize: 12,
                fontFamily: "'Space Grotesk', -apple-system, BlinkMacSystemFont, 'PingFang SC', sans-serif",
                color: '#1A1A1A', borderRadius: 10, padding: '8px 10px',
                cursor: loading || !activeId ? 'not-allowed' : 'pointer',
                transition: 'all 0.15s', background: 'transparent',
                border: '1px solid transparent',
                opacity: loading || !activeId ? 0.4 : 1,
              }}
              onMouseEnter={(e) => {
                if (!loading && activeId) {
                  e.currentTarget.style.background = '#EDE8E0';
                  e.currentTarget.style.border = '1px solid rgba(26,26,26,0.10)';
                }
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.background = 'transparent';
                e.currentTarget.style.border = '1px solid transparent';
              }}
            >
              <span style={{ width: 7, height: 7, borderRadius: '50%', flexShrink: 0, background: item.color }} />
              {item.q}
            </button>
          ))}
        </div>
      </div>

      {/* 文档查看器 */}
      <DocumentViewer
        docId={viewingDocId}
        docTitle={viewingDocTitle}
        highlightChunkId={viewingChunkId}
        highlightChunkIndex={viewingChunkIndex}
        visible={docViewerVisible}
        onClose={() => setDocViewerVisible(false)}
      />
    </div>
  );
};

// ==================== 子组件 ====================

const MessageBubble: React.FC<{
  msg: Message;
  onCitationClick: (docId: number, docTitle: string, chunkId: string, chunkIndex: number) => void;
}> = ({ msg, onCitationClick }) => {
  const isUser = msg.role === 'user';
  const citations = msg.citations || [];
  // 筛选：优先显示被引用的（HIGH），如果没有被引用的则显示全部
  const referencedCitations = citations.filter((c) => c.referenced || c.confidence === 'HIGH');
  const displayCitations = referencedCitations.length > 0 ? referencedCitations : citations;
  const dynamicComponents = createMarkdownComponents(citations, onCitationClick);
  // 预处理文本：将 [N] 转为可点击的 HTML sup 标签
  const processedContent = isUser ? msg.content : injectCitationMarkers(normalizeMarkdown(msg.content));

  return (
    <div style={{ display: 'flex', gap: 10, flexDirection: isUser ? 'row-reverse' : 'row' }}>
      <div style={{
        width: 32, height: 32, borderRadius: '50%', flexShrink: 0,
        display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 14,
        background: isUser ? '#E85D4E' : '#EDE8E0', color: isUser ? '#fff' : '#1A1A1A',
      }}>
        {isUser ? <UserOutlined /> : <RobotOutlined />}
      </div>

      <div style={{ maxWidth: '72%', display: 'flex', flexDirection: 'column', alignItems: isUser ? 'flex-end' : 'flex-start' }}>
        <div style={{
          borderRadius: 20,
          borderTopRightRadius: isUser ? 6 : 20,
          borderTopLeftRadius: isUser ? 20 : 6,
          padding: '14px 20px', fontSize: 14, lineHeight: 1.75,
          fontFamily: "'Space Grotesk', -apple-system, BlinkMacSystemFont, 'PingFang SC', sans-serif",
          background: isUser ? '#E85D4E' : '#FFFFFF',
          color: isUser ? '#fff' : '#1A1A1A',
          border: isUser ? 'none' : '1px solid rgba(26,26,26,0.08)',
          overflow: 'hidden',
        }}>
          {isUser ? (
            <span style={{ whiteSpace: 'pre-wrap' }}>{msg.content}</span>
          ) : (
            <div className="markdown-body" style={{ fontSize: 'inherit' }}>
              <ReactMarkdown
                remarkPlugins={[remarkGfm]}
                rehypePlugins={[rehypeRaw]}
                components={dynamicComponents}
              >
                {processedContent}
              </ReactMarkdown>
            </div>
          )}
        </div>

        {msg.source && msg.source !== 'SYSTEM' && (
          <Tag color={sourceColors[msg.source] || '#C5B5E0'} style={{
            marginTop: 6, fontSize: 11, borderRadius: 9999, border: '1px solid rgba(26,26,26,0.10)',
          }}>
            {sourceLabelMap[msg.source] || msg.source}
          </Tag>
        )}

        {displayCitations.length > 0 && (
          <div style={{
            marginTop: 8, padding: '10px 14px', borderRadius: 14,
            background: '#F8F6F0', border: '1px solid rgba(26,26,26,0.06)', maxWidth: 400,
          }}>
            <div style={{
              fontSize: 11, fontWeight: 600, color: 'rgba(26,26,26,0.35)',
              marginBottom: 6, letterSpacing: '0.04em', textTransform: 'uppercase',
            }}>
              📚 参考来源
              {referencedCitations.length > 0 && (
                <span style={{ fontWeight: 400, textTransform: 'none', marginLeft: 4 }}>
                  （{referencedCitations.length}/{citations.length} 被引用）
                </span>
              )}
            </div>
            {displayCitations.map((c, i) => (
              <div
                key={i}
                onClick={() => {
                  if (c.docId) {
                    onCitationClick(c.docId, c.docTitle, c.chunkId || '', c.chunkIndex ?? 0);
                  }
                }}
                style={{
                  fontSize: 12, color: 'rgba(26,26,26,0.55)', lineHeight: 1.5,
                  padding: '5px 0', cursor: c.docId ? 'pointer' : 'default',
                  borderBottom: i < displayCitations.length - 1 ? '1px solid rgba(26,26,26,0.04)' : 'none',
                  transition: 'all 0.15s',
                  borderRadius: 6,
                  paddingLeft: 4,
                  paddingRight: 4,
                }}
                onMouseEnter={(e) => {
                  if (c.docId) {
                    e.currentTarget.style.background = 'rgba(232,93,78,0.06)';
                  }
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.background = 'transparent';
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap' }}>
                  <span style={{ fontWeight: 600, color: '#E85D4E' }}>
                    [{c.markerId || i + 1}]
                  </span>
                  <span style={{ fontWeight: 500 }}>《{c.docTitle}》</span>
                  {c.chunkIndex !== undefined && c.chunkIndex >= 0 && (
                    <span style={{
                      fontSize: 10, color: 'rgba(26,26,26,0.30)',
                      background: 'rgba(26,26,26,0.03)', padding: '0 6px', borderRadius: 9999,
                    }}>
                      第{c.chunkIndex + 1}段
                    </span>
                  )}
                  <span style={{ fontSize: 10, color: 'rgba(26,26,26,0.30)' }}>
                    相关度 {c.score.toFixed(2)}
                  </span>
                  {c.confidence === 'HIGH' && (
                    <Tag color="green" style={{ margin: 0, fontSize: 10, lineHeight: '16px', padding: '0 4px' }}>
                      高置信
                    </Tag>
                  )}
                  {c.confidence === 'LOW' && c.referenced === false && (
                    <Tag color="default" style={{ margin: 0, fontSize: 10, lineHeight: '16px', padding: '0 4px' }}>
                      未引用
                    </Tag>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

const StreamingBubble: React.FC<{ content: string }> = ({ content }) => (
  <div style={{ display: 'flex', gap: 10 }}>
    <div style={{
      width: 32, height: 32, borderRadius: '50%', flexShrink: 0,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      fontSize: 14, background: '#EDE8E0', color: '#1A1A1A',
    }}>
      <RobotOutlined />
    </div>
    <div style={{ maxWidth: '72%' }}>
      <div style={{
        borderRadius: 20, borderTopLeftRadius: 6, padding: '14px 20px',
        fontSize: 14, lineHeight: 1.75,
        fontFamily: "'Space Grotesk', -apple-system, BlinkMacSystemFont, 'PingFang SC', sans-serif",
        background: '#FFFFFF', color: '#1A1A1A',
        border: '1px solid rgba(26,26,26,0.08)', overflow: 'hidden',
      }}>
        <div className="markdown-body" style={{ fontSize: 'inherit' }}>
          <ReactMarkdown
            remarkPlugins={[remarkGfm]}
            rehypePlugins={[rehypeRaw]}
            components={markdownComponents}
          >
            {normalizeMarkdown(content) || '*思考中…*'}
          </ReactMarkdown>
        </div>
        <span style={{
          display: 'inline-block', width: 2, height: 16,
          background: '#E85D4E', marginLeft: 2,
          verticalAlign: 'text-bottom', animation: 'blink 0.8s infinite',
        }} />
      </div>
    </div>
  </div>
);

const LoadingBubble: React.FC = () => (
  <div style={{ display: 'flex', gap: 10 }}>
    <div style={{
      width: 32, height: 32, borderRadius: '50%', flexShrink: 0,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      fontSize: 14, background: '#EDE8E0', color: '#1A1A1A',
    }}>
      <RobotOutlined />
    </div>
    <div style={{
      borderRadius: 20, borderTopLeftRadius: 6,
      padding: '14px 18px', background: '#FFFFFF',
      border: '1px solid rgba(26,26,26,0.08)',
    }}>
      <div style={{ display: 'flex', gap: 6 }}>
        {[0, 150, 300].map((delay) => (
          <span key={delay} style={{
            width: 7, height: 7, borderRadius: '50%', background: '#C5B5E0',
            animation: 'bounce 0.6s infinite', animationDelay: `${delay}ms`,
          }} />
        ))}
      </div>
    </div>
  </div>
);

export default AiQa;
