import React, { useState, useRef, useEffect } from 'react';
import { Tag } from 'antd';
import { RobotOutlined, UserOutlined } from '@ant-design/icons';
import { ArrowUp, Square, Sparkles } from 'lucide-react';
import { askAi } from '../../services/api';
import {
  PromptInput,
  PromptInputTextarea,
  PromptInputActions,
  PromptInputAction,
} from '@/components/ui/prompt-input';
import { Button } from '@/components/ui/button';

interface Message {
  role: 'user' | 'assistant';
  content: string;
  source?: string;
}

const sourceColors: Record<string, string> = {
  AI: '#E85D4E', LOCAL_KB: '#C4D94E', CACHE: '#8BB4F7', FALLBACK: '#F2D160', RATE_LIMIT: '#F5B895', SYSTEM: '#C5B5E0', ERROR: '#E85D4E',
};

const quickQuestions = [
  { q: '住宿费多少钱？', color: '#E85D4E' },
  { q: '怎么缴费？', color: '#C4D94E' },
  { q: '逾期了怎么办？', color: '#F2D160' },
  { q: '怎么办理入住？', color: '#8BB4F7' },
  { q: '如何换宿舍？', color: '#C5B5E0' },
];

const AiQa: React.FC = () => {
  const [messages, setMessages] = useState<Message[]>([
    { role: 'assistant', content: '您好！我是宿舍收费管理系统的智能助手。请问有什么可以帮您的？', source: 'SYSTEM' },
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const listRef = useRef<HTMLDivElement>(null);

  const handleSubmit = async () => {
    if (!input.trim() || loading) return;
    const question = input.trim();
    setInput('');
    setMessages((prev) => [...prev, { role: 'user', content: question }]);
    setLoading(true);
    try {
      const res = await askAi(question);
      setMessages((prev) => [...prev, { role: 'assistant', content: res.data.answer, source: res.data.source }]);
    } catch {
      setMessages((prev) => [...prev, { role: 'assistant', content: '抱歉，AI 服务暂时不可用。请稍后重试。', source: 'ERROR' }]);
    }
    setLoading(false);
  };

  useEffect(() => {
    if (listRef.current) {
      listRef.current.scrollTop = listRef.current.scrollHeight;
    }
  }, [messages]);

  return (
    <div style={{ display: 'flex', height: 'calc(100vh - 112px)', gap: 20, padding: 24 }}>
      {/* ====== 主聊天区 ====== */}
      <div style={{
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
        borderRadius: 24,
        border: '1px solid rgba(26,26,26,0.10)',
        background: '#FAF9F5',
        boxShadow: '2px 2px 0 rgba(26,26,26,0.04)',
      }}>
        {/* 消息列表 */}
        <div ref={listRef} style={{ flex: 1, overflow: 'auto', padding: '24px 24px 12px' }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
            {messages.map((msg, idx) => (
              <div
                key={idx}
                style={{
                  display: 'flex',
                  gap: 10,
                  flexDirection: msg.role === 'user' ? 'row-reverse' : 'row',
                }}
              >
                {/* 头像 */}
                <div style={{
                  width: 32, height: 32, borderRadius: '50%', flexShrink: 0,
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  fontSize: 14,
                  background: msg.role === 'user' ? '#E85D4E' : '#EDE8E0',
                  color: msg.role === 'user' ? '#fff' : '#1A1A1A',
                }}>
                  {msg.role === 'user' ? <UserOutlined /> : <RobotOutlined />}
                </div>

                {/* 气泡 */}
                <div style={{ maxWidth: '68%', display: 'flex', flexDirection: 'column', alignItems: msg.role === 'user' ? 'flex-end' : 'flex-start' }}>
                  <div style={{
                    borderRadius: 20,
                    borderTopRightRadius: msg.role === 'user' ? 6 : 20,
                    borderTopLeftRadius: msg.role === 'user' ? 20 : 6,
                    padding: '12px 18px',
                    fontSize: 14,
                    lineHeight: 1.65,
                    whiteSpace: 'pre-wrap',
                    fontFamily: "'Space Grotesk', -apple-system, BlinkMacSystemFont, 'PingFang SC', sans-serif",
                    background: msg.role === 'user' ? '#E85D4E' : '#FFFFFF',
                    color: msg.role === 'user' ? '#fff' : '#1A1A1A',
                    border: msg.role === 'user' ? 'none' : '1px solid rgba(26,26,26,0.08)',
                  }}>
                    {msg.content}
                  </div>
                  {msg.source && msg.source !== 'SYSTEM' && (
                    <Tag
                      color={sourceColors[msg.source] || '#C5B5E0'}
                      style={{
                        marginTop: 6,
                        fontSize: 11,
                        borderRadius: 9999,
                        border: '1px solid rgba(26,26,26,0.10)',
                      }}
                    >
                      {msg.source}
                    </Tag>
                  )}
                </div>
              </div>
            ))}

            {/* loading */}
            {loading && (
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
                      <span
                        key={delay}
                        style={{
                          width: 7, height: 7, borderRadius: '50%',
                          background: '#C5B5E0',
                          animation: 'bounce 0.6s infinite',
                          animationDelay: `${delay}ms`,
                        }}
                      />
                    ))}
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* 底部输入区 */}
        <div style={{
          padding: '12px 20px 20px',
          borderTop: '1px solid rgba(26,26,26,0.06)',
          background: '#FFFFFF',
          borderBottomLeftRadius: 24,
          borderBottomRightRadius: 24,
        }}>
          <PromptInput
            value={input}
            onValueChange={setInput}
            isLoading={loading}
            onSubmit={handleSubmit}
          >
            <PromptInputTextarea
              placeholder="输入你的问题，Enter 发送，Shift+Enter 换行…"
              disabled={loading}
            />
            <PromptInputActions className="justify-end pt-2">
              <PromptInputAction tooltip={loading ? '停止生成' : '发送消息'}>
                <Button
                  variant={input.trim() ? 'default' : 'secondary'}
                  size="icon"
                  className="h-9 w-9 rounded-full"
                  onClick={handleSubmit}
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

      {/* ====== 快捷问题侧边栏 ====== */}
      <div style={{
        width: 220, flexShrink: 0,
        borderRadius: 24,
        border: '1px solid rgba(26,26,26,0.10)',
        background: '#FFFFFF',
        boxShadow: '2px 2px 0 rgba(26,26,26,0.04)',
        padding: '24px 18px',
        display: 'flex',
        flexDirection: 'column',
        gap: 4,
      }}>
        <h3 style={{
          fontFamily: "'Bodoni Moda', 'Noto Serif SC', serif",
          fontSize: 17,
          fontWeight: 700,
          color: '#1A1A1A',
          marginBottom: 10,
          letterSpacing: '-0.01em',
        }}>
          快捷问题
        </h3>
        {quickQuestions.map((item, idx) => (
          <button
            key={idx}
            onClick={() => { setInput(item.q); }}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 8,
              width: '100%',
              textAlign: 'left' as const,
              fontSize: 13,
              fontFamily: "'Space Grotesk', -apple-system, BlinkMacSystemFont, 'PingFang SC', sans-serif",
              color: '#1A1A1A',
              borderRadius: 14,
              padding: '11px 14px',
              cursor: 'pointer',
              transition: 'all 0.15s',
              background: 'transparent',
              border: '1px solid transparent',
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.background = '#EDE8E0';
              e.currentTarget.style.border = '1px solid rgba(26,26,26,0.10)';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.background = 'transparent';
              e.currentTarget.style.border = '1px solid transparent';
            }}
          >
            <span style={{
              width: 8, height: 8, borderRadius: '50%', flexShrink: 0,
              background: item.color,
            }} />
            {item.q}
          </button>
        ))}
      </div>
    </div>
  );
};

export default AiQa;
