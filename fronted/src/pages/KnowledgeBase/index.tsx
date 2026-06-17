import React, { useState, useEffect, useCallback } from 'react';
import { Button, Table, Modal, Upload, Tag, message, Space, Tooltip, Input, Typography, Card, Descriptions } from 'antd';
import { PlusOutlined, DeleteOutlined, SearchOutlined, ReloadOutlined, InboxOutlined, FileTextOutlined } from '@ant-design/icons';
import { getKbDocuments, uploadKbDocument, deleteKbDocument, getKbDocumentChunks, reprocessKbDocument, searchKnowledgeBase } from '../../services/api';

const { Text, Paragraph } = Typography;
const { Dragger } = Upload;

interface KbDocument {
  id: number; title: string; description: string; fileName: string;
  fileType: string; fileSize: number; chunkCount: number;
  status: string; errorMsg?: string; createTime: string;
}

const statusColors: Record<string, string> = {
  PENDING: 'default', PROCESSING: 'processing', COMPLETED: 'success', FAILED: 'error',
};

const KnowledgeBase: React.FC = () => {
  const [docs, setDocs] = useState<KbDocument[]>([]);
  const [loading, setLoading] = useState(false);
  const [uploadOpen, setUploadOpen] = useState(false);
  const [chunksOpen, setChunksOpen] = useState(false);
  const [chunks, setChunks] = useState<any[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<any[]>([]);
  const [searching, setSearching] = useState(false);

  const fetchDocs = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getKbDocuments({ page: 1, pageSize: 50 });
      setDocs(res.data?.records || []);
    } catch { message.error('获取文档列表失败'); }
    setLoading(false);
  }, []);

  useEffect(() => { fetchDocs(); }, [fetchDocs]);

  const handleUpload = async (file: File) => {
    try {
      const res = await uploadKbDocument(file);
      if (res.code === 200) {
        message.success('文档上传成功，正在后台处理...');
        setUploadOpen(false);
        fetchDocs();
      }
    } catch { message.error('上传失败'); }
    return false; // Prevent default upload
  };

  const handleDelete = async (id: number) => {
    Modal.confirm({
      title: '确认删除', content: '删除文档将同时删除所有分块和向量数据，不可恢复。',
      onOk: async () => {
        await deleteKbDocument(id);
        message.success('已删除');
        fetchDocs();
      },
    });
  };

  const handleViewChunks = async (id: number) => {
    try {
      const res = await getKbDocumentChunks(id);
      setChunks(res.data || []);
      setChunksOpen(true);
    } catch { message.error('获取分块失败'); }
  };

  const handleReprocess = async (id: number) => {
    await reprocessKbDocument(id);
    message.success('已触发重新处理');
    fetchDocs();
  };

  const handleSearch = async () => {
    if (!searchQuery.trim()) return;
    setSearching(true);
    try {
      const res = await searchKnowledgeBase(searchQuery, 5);
      setSearchResults(res.data || []);
    } catch { message.error('搜索失败'); }
    setSearching(false);
  };

  const columns = [
    { title: 'ID', dataIndex: 'id', width: 60 },
    { title: '标题', dataIndex: 'title', ellipsis: true },
    {
      title: '类型', dataIndex: 'fileType', width: 70,
      render: (t: string) => <Tag>{t}</Tag>,
    },
    {
      title: '大小', dataIndex: 'fileSize', width: 80,
      render: (s: number) => s ? `${(s / 1024).toFixed(0)} KB` : '-',
    },
    {
      title: '分块', dataIndex: 'chunkCount', width: 60,
    },
    {
      title: '状态', dataIndex: 'status', width: 90,
      render: (s: string) => <Tag color={statusColors[s] || 'default'}>{s}</Tag>,
    },
    {
      title: '上传时间', dataIndex: 'createTime', width: 150,
      render: (t: string) => t ? new Date(t).toLocaleString() : '-',
    },
    {
      title: '操作', key: 'actions', width: 180,
      render: (_: any, r: KbDocument) => (
        <Space size="small">
          <Button size="small" icon={<FileTextOutlined />} onClick={() => handleViewChunks(r.id)}>分块</Button>
          {r.status === 'FAILED' && (
            <Button size="small" icon={<ReloadOutlined />} onClick={() => handleReprocess(r.id)}>重试</Button>
          )}
          <Button size="small" danger icon={<DeleteOutlined />} onClick={() => handleDelete(r.id)} />
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
        <h2 style={{ margin: 0, fontFamily: "'Bodoni Moda', 'Noto Serif SC', serif", fontSize: 22, fontWeight: 700 }}>
          知识库管理
        </h2>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={fetchDocs}>刷新</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setUploadOpen(true)}>上传文档</Button>
        </Space>
      </div>

      {/* 搜索测试 */}
      <Card size="small" style={{ marginBottom: 20, background: '#FAF9F5' }}>
        <Space.Compact style={{ width: '100%' }}>
          <Input
            placeholder="输入问题测试知识库搜索..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            onPressEnter={handleSearch}
          />
          <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch} loading={searching}>搜索</Button>
        </Space.Compact>
        {searchResults.length > 0 && (
          <div style={{ marginTop: 12, display: 'flex', flexDirection: 'column', gap: 8 }}>
            {searchResults.map((r, i) => (
              <div key={i} style={{
                padding: '10px 14px', background: '#fff', borderRadius: 10,
                border: '1px solid rgba(26,26,26,0.08)', fontSize: 13,
              }}>
                <div style={{ display: 'flex', gap: 12, marginBottom: 4 }}>
                  <Tag color="blue">相关度: {r.score}</Tag>
                  <Text type="secondary">docId: {r.docId}</Text>
                </div>
                <Paragraph ellipsis={{ rows: 3 }} style={{ margin: 0 }}>{r.content}</Paragraph>
              </div>
            ))}
          </div>
        )}
      </Card>

      {/* 文档列表 */}
      <Table
        rowKey="id"
        dataSource={docs}
        columns={columns}
        loading={loading}
        size="middle"
        expandable={{
          expandedRowRender: (r) => r.errorMsg ? <Text type="danger">{r.errorMsg}</Text> : null,
          rowExpandable: (r) => !!r.errorMsg,
        }}
      />

      {/* 上传弹窗 */}
      <Modal
        title="上传知识库文档"
        open={uploadOpen}
        onCancel={() => setUploadOpen(false)}
        footer={null}
        width={520}
      >
        <div style={{ padding: '20px 0' }}>
          <Dragger
            accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.md"
            showUploadList={false}
            beforeUpload={(file) => { handleUpload(file); return false; }}
          >
            <p className="ant-upload-drag-icon"><InboxOutlined /></p>
            <p style={{ fontSize: 15, fontWeight: 600 }}>点击或拖拽文件上传</p>
            <p style={{ color: 'rgba(0,0,0,0.45)', fontSize: 13 }}>
              支持 PDF、Word、Excel、PPT、TXT、Markdown
            </p>
          </Dragger>
        </div>
      </Modal>

      {/* 分块查看 */}
      <Modal
        title="文档分块列表"
        open={chunksOpen}
        onCancel={() => setChunksOpen(false)}
        footer={null}
        width={700}
      >
        <div style={{ maxHeight: 400, overflow: 'auto' }}>
          {chunks.map((c, i) => (
            <div key={i} style={{
              marginBottom: 10, padding: '10px 14px', background: '#FAF9F5',
              borderRadius: 10, border: '1px solid rgba(26,26,26,0.06)', fontSize: 13,
            }}>
              <div style={{ display: 'flex', gap: 12, marginBottom: 4 }}>
                <Tag>#{c.chunkIndex}</Tag>
                <Text type="secondary">Tokens: {c.tokenCount || 'N/A'}</Text>
              </div>
              <Paragraph ellipsis={{ rows: 4 }} style={{ margin: 0, whiteSpace: 'pre-wrap' }}>{c.content}</Paragraph>
            </div>
          ))}
          {chunks.length === 0 && <Text type="secondary">暂无分块</Text>}
        </div>
      </Modal>
    </div>
  );
};

export default KnowledgeBase;
