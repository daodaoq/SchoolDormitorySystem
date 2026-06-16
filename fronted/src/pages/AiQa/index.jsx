import React, { useState, useRef, useEffect } from 'react';
import { Card, Input, Button, List, Tag, Space, Typography, message } from 'antd';
import { SendOutlined, RobotOutlined, UserOutlined } from '@ant-design/icons';
import { askAi } from '../../services/api';

const { TextArea } = Input;
const { Text, Paragraph } = Typography;

function AiQa() {
  const [messages, setMessages] = useState([
    { role: 'assistant', content: '您好！我是宿舍收费管理系统的智能助手。您可询问宿舍入住、收费标准、缴费流程、账单查询等相关问题。请问有什么可以帮您的？', source: 'SYSTEM' },
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const listRef = useRef(null);

  const handleSend = async () => {
    if (!input.trim()) return;
    const question = input.trim();
    setInput('');
    setMessages((prev) => [...prev, { role: 'user', content: question }]);
    setLoading(true);

    try {
      const res = await askAi(question, 'user-' + Date.now());
      setMessages((prev) => [...prev, {
        role: 'assistant',
        content: res.data.answer,
        source: res.data.source,
        confidence: res.data.confidence,
      }]);
    } catch (e) {
      setMessages((prev) => [...prev, { role: 'assistant', content: '抱歉，AI服务暂时不可用。请稍后重试或联系管理员。', source: 'ERROR' }]);
    }
    setLoading(false);
  };

  useEffect(() => {
    if (listRef.current) {
      listRef.current.scrollTop = listRef.current.scrollHeight;
    }
  }, [messages]);

  const sourceColors = { AI: 'blue', LOCAL_KB: 'green', CACHE: 'cyan', FALLBACK: 'orange', RATE_LIMIT: 'red', SYSTEM: 'default', ERROR: 'red' };

  const quickQuestions = [
    '住宿费多少钱？',
    '怎么缴费？',
    '逾期了怎么办？',
    '怎么办理入住？',
    '如何换宿舍？',
  ];

  return (
    <div style={{ display: 'flex', height: 'calc(100vh - 200px)', gap: 16 }}>
      <Card style={{ flex: 1, display: 'flex', flexDirection: 'column' }}
        bodyStyle={{ flex: 1, display: 'flex', flexDirection: 'column', padding: 0 }}>
        <div ref={listRef} style={{ flex: 1, overflow: 'auto', padding: 16 }}>
          {messages.map((msg, idx) => (
            <div key={idx} style={{ marginBottom: 16, textAlign: msg.role === 'user' ? 'right' : 'left' }}>
              <Space align="start">
                {msg.role === 'assistant' && <RobotOutlined style={{ fontSize: 18, color: '#1890ff' }} />}
                <div style={{
                  maxWidth: '70%', display: 'inline-block', padding: '10px 16px',
                  borderRadius: 12, background: msg.role === 'user' ? '#1890ff' : '#f0f2f5',
                  color: msg.role === 'user' ? '#fff' : 'rgba(0,0,0,0.85)',
                  textAlign: 'left',
                }}>
                  <Paragraph style={{ margin: 0, whiteSpace: 'pre-wrap' }}>{msg.content}</Paragraph>
                  {msg.source && msg.source !== 'SYSTEM' && (
                    <Tag color={sourceColors[msg.source] || 'default'} style={{ marginTop: 4 }}>
                      {msg.source === 'LOCAL_KB' ? '本地知识库' :
                       msg.source === 'AI' ? 'AI模型' :
                       msg.source === 'CACHE' ? '缓存' :
                       msg.source === 'FALLBACK' ? '降级回复' :
                       msg.source === 'RATE_LIMIT' ? '限流' : msg.source}
                    </Tag>
                  )}
                </div>
                {msg.role === 'user' && <UserOutlined style={{ fontSize: 18, color: '#1890ff' }} />}
              </Space>
            </div>
          ))}
        </div>
        <div style={{ padding: '12px 16px', borderTop: '1px solid #f0f0f0' }}>
          <Space.Compact style={{ width: '100%' }}>
            <TextArea value={input} onChange={(e) => setInput(e.target.value)}
              onPressEnter={(e) => { if (!e.shiftKey) { e.preventDefault(); handleSend(); } }}
              placeholder="输入您的问题，按Enter发送..." rows={2} style={{ flex: 1 }} />
            <Button type="primary" icon={<SendOutlined />} onClick={handleSend} loading={loading}
              style={{ height: 'auto' }}>发送</Button>
          </Space.Compact>
        </div>
      </Card>

      <Card title="快捷问题" style={{ width: 220 }}>
        <Space direction="vertical" style={{ width: '100%' }}>
          {quickQuestions.map((q, idx) => (
            <Button key={idx} type="text" block style={{ textAlign: 'left' }}
              onClick={() => setInput(q)}>{q}</Button>
          ))}
        </Space>
      </Card>
    </div>
  );
}

export default AiQa;
