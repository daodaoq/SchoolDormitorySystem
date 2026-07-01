import React, { useState, useEffect, useCallback } from 'react';
import { Button, Table, Modal, Upload, Tag, message, Space, Input, Typography, Card } from 'antd';
import { PlusOutlined, DeleteOutlined, SearchOutlined, ReloadOutlined, InboxOutlined, FileTextOutlined, CheckCircleOutlined, CloseCircleOutlined, LoadingOutlined } from '@ant-design/icons';
import { getKbDocuments, uploadKbDocument, deleteKbDocument, getKbDocumentChunks, reprocessKbDocument, searchKnowledgeBase } from '../../services/api';
import type { KbDocument, BatchFile } from '../../types';

const { Text, Paragraph } = Typography;
const { Dragger } = Upload;

const statusColors: Record<string, string> = {
  PENDING: 'default', PROCESSING: 'processing', COMPLETED: 'success', FAILED: 'error',
};
const statusLabels: Record<string, string> = {
  PENDING: '待处理', PROCESSING: '处理中', COMPLETED: '已完成', FAILED: '失败',
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
  const [deleteTarget, setDeleteTarget] = useState<number | null>(null);
  const [deleting, setDeleting] = useState(false);

  const [batchFiles, setBatchFiles] = useState<BatchFile[]>([]);
  const [batchUploading, setBatchUploading] = useState(false);
  const fileRef = React.useRef<File[]>([]);

  const fetchDocs = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getKbDocuments({ page: 1, pageSize: 50 });
      setDocs(res.data?.records || []);
    } catch (err) { console.error(err); message.error('获取文档列表失败'); }
    setLoading(false);
  }, []);

  useEffect(() => { fetchDocs(); }, [fetchDocs]);

  // ====== 单文件上传（保留兼容） ======
  const handleUpload = async (file: File) => {
    try {
      const res = await uploadKbDocument(file);
      if (res.code === 200) {
        message.success('文档上传成功，正在后台处理...');
        setUploadOpen(false);
        fetchDocs();
      }
    } catch (err) { console.error(err); message.error('上传失败'); }
    return false;
  };

  // ====== 批量上传 ======
  const handleBatchSelect = (_file: File, fileList: File[]) => {
    fileRef.current = fileList;
    setBatchFiles(
      fileList.map((f, i) => ({
        uid: `${Date.now()}-${i}-${f.name}`,
        name: f.name,
        size: f.size,
        status: 'pending' as const,
      })),
    );
    return false; // 阻止自动上传
  };

  const handleRemoveFile = (uid: string) => {
    const idx = batchFiles.findIndex((f) => f.uid === uid);
    if (idx >= 0) {
      fileRef.current.splice(idx, 1);
      setBatchFiles((prev) => prev.filter((f) => f.uid !== uid));
    }
  };

  const startBatchUpload = async () => {
    // 检查是否正在上传（防止重复点击）
    if (fileRef.current.length === 0 || batchUploading) return;

    // 设置 batchUploading = true 禁用按钮
    setBatchUploading(true);

    let successCount = 0;
    let failCount = 0;

    for (let i = 0; i < fileRef.current.length; i++) {
      const file = fileRef.current[i];
      const uid = batchFiles[i]?.uid;
      if (!uid) continue;

      // 更新状态为「上传中」
      setBatchFiles((prev) =>
        prev.map((f) => (f.uid === uid ? { ...f, status: 'uploading' as const } : f)),
      );

      try {
        await uploadKbDocument(file);
        setBatchFiles((prev) =>
          prev.map((f) => (f.uid === uid ? { ...f, status: 'success' as const } : f)),
        );
        successCount++;
      } catch (err: any) {
        setBatchFiles((prev) =>
          prev.map((f) => (f.uid === uid ? { ...f, status: 'error' as const, errorMsg: err?.message || '上传失败' } : f)),
        );
        failCount++;
      }
    }

    setBatchUploading(false);
    if (failCount === 0) {
      message.success(`全部 ${successCount} 个文档上传成功！`);
    } else {
      message.warning(`上传完成：${successCount} 成功，${failCount} 失败`);
    }
    fetchDocs();

    // 3 秒后清空列表
    setTimeout(() => {
      setBatchFiles([]);
      fileRef.current = [];
    }, 3000);
  };

  const handleDelete = (id: number) => {
    setDeleteTarget(id);
  };

  const confirmDelete = async () => {
    if (deleteTarget === null) return;
    setDeleting(true);
    try {
      await deleteKbDocument(deleteTarget);
      message.success('已删除');
      setDeleteTarget(null);
      fetchDocs();
    } catch (err: any) {
      message.error(err?.message || '删除失败，请重试');
      console.error('Delete error:', err);
    } finally {
      setDeleting(false);
    }
  };

  const handleViewChunks = async (id: number) => {
    try {
      const res = await getKbDocumentChunks(id);
      setChunks(res.data || []);
      setChunksOpen(true);
    } catch (err) { console.error(err); message.error('获取分块失败'); }
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
    } catch (err) { console.error(err); message.error('搜索失败'); }
    setSearching(false);
  };

  const columns = [
    { title: '#', dataIndex: 'rowIndex', width: 50, render: (_: any, __: any, index: number) => index + 1 },
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
      render: (s: string) => <Tag color={statusColors[s] || 'default'}>{statusLabels[s] || s}</Tag>,
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
        onCancel={() => { setUploadOpen(false); setBatchFiles([]); fileRef.current = []; }}
        footer={null}
        width={560}
        destroyOnClose
      >
        <div style={{ padding: '8px 0' }}>
          {/* 批量上传区域 */}
          <Dragger
            accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.md"
            multiple
            showUploadList={false}
            beforeUpload={handleBatchSelect}
            disabled={batchUploading}
          >
            <p className="ant-upload-drag-icon"><InboxOutlined /></p>
            <p style={{ fontSize: 15, fontWeight: 600 }}>点击或拖拽文件上传（支持多选）</p>
            <p style={{ color: 'rgba(0,0,0,0.45)', fontSize: 13 }}>
              PDF、Word、Excel、PPT、TXT、Markdown — 可一次选择多个文件
            </p>
          </Dragger>

          {/* 待上传文件列表 */}
          {batchFiles.length > 0 && (
            <div style={{ marginTop: 16 }}>
              <div style={{
                display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                marginBottom: 10, padding: '0 4px',
              }}>
                <span style={{ fontSize: 13, fontWeight: 600, color: '#1A1A1A' }}>
                  已选择 {batchFiles.length} 个文件
                </span>
                <Space>
                  <Button
                    size="small"
                    onClick={() => { setBatchFiles([]); fileRef.current = []; }}
                    disabled={batchUploading}
                  >
                    清空
                  </Button>
                  <Button
                    type="primary"
                    size="small"
                    icon={<PlusOutlined />}
                    onClick={startBatchUpload}
                    loading={batchUploading}
                    disabled={batchFiles.filter((f) => f.status === 'pending').length === 0}
                  >
                    {batchUploading ? '上传中...' : '开始上传'}
                  </Button>
                </Space>
              </div>
              <div style={{ maxHeight: 260, overflowY: 'auto', borderRadius: 12, border: '1px solid rgba(26,26,26,0.06)' }}>
                {batchFiles.map((f) => (
                  <div
                    key={f.uid}
                    style={{
                      display: 'flex', alignItems: 'center', gap: 10,
                      padding: '8px 12px', fontSize: 13,
                      borderBottom: '1px solid rgba(26,26,26,0.04)',
                      background: f.status === 'uploading' ? '#FFF8F0'
                        : f.status === 'error' ? '#FFF2F0'
                        : '#fff',
                    }}
                  >
                    {/* 状态图标 */}
                    {f.status === 'pending' && (
                      <FileTextOutlined style={{ color: 'rgba(26,26,26,0.25)', fontSize: 14 }} />
                    )}
                    {f.status === 'uploading' && (
                      <LoadingOutlined style={{ color: '#E85D4E', fontSize: 14 }} />
                    )}
                    {f.status === 'success' && (
                      <CheckCircleOutlined style={{ color: '#52c41a', fontSize: 14 }} />
                    )}
                    {f.status === 'error' && (
                      <CloseCircleOutlined style={{ color: '#ff4d4f', fontSize: 14 }} />
                    )}

                    {/* 文件名与大小 */}
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{
                        color: '#1A1A1A', fontWeight: 500,
                        overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
                      }}>
                        {f.name}
                      </div>
                      <div style={{ fontSize: 11, color: 'rgba(26,26,26,0.35)' }}>
                        {(f.size / 1024).toFixed(1)} KB
                        {f.status === 'error' && f.errorMsg && (
                          <span style={{ color: '#ff4d4f', marginLeft: 8 }}>{f.errorMsg}</span>
                        )}
                      </div>
                    </div>

                    {/* 删除按钮 */}
                    {f.status !== 'uploading' && (
                      <DeleteOutlined
                        onClick={() => handleRemoveFile(f.uid)}
                        style={{
                          fontSize: 12, color: 'rgba(26,26,26,0.25)',
                          cursor: 'pointer', padding: 4,
                        }}
                        onMouseEnter={(e) => { e.currentTarget.style.color = '#E85D4E'; }}
                        onMouseLeave={(e) => { e.currentTarget.style.color = 'rgba(26,26,26,0.25)'; }}
                      />
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </Modal>

      {/* 删除确认 Modal */}
      <Modal
        title="确认删除"
        open={deleteTarget !== null}
        onOk={confirmDelete}
        onCancel={() => setDeleteTarget(null)}
        confirmLoading={deleting}
        okText="确认删除"
        cancelText="取消"
        okButtonProps={{ danger: true }}
      >
        <p>删除文档将同时删除所有分块和向量数据，不可恢复。确定要删除吗？</p>
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
