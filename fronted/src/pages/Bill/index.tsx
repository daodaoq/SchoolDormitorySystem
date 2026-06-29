import React, { useEffect, useState } from 'react';
import { Table, Button, Space, Modal, Form, Input, Select, message, Tag } from 'antd';
import { UploadOutlined, ThunderboltOutlined } from '@ant-design/icons';
import { AlertTriangle } from 'lucide-react';
import { getBills, generateBills, updateBillStatus, exportBills } from '../../services/api';
import { Alert, AlertIcon, AlertTitle } from '@/components/ui/alert-1';
import type { PaymentBill } from '../../types';

const statusMap: Record<string, { color: string; text: string }> = {
  UNPAID: { color: 'orange', text: '未缴' }, PAID: { color: 'green', text: '已缴' },
  OVERDUE: { color: 'red', text: '逾期' }, CANCELLED: { color: 'default', text: '已取消' },
};

const Bill: React.FC = () => {
  const [data, setData] = useState<PaymentBill[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [filters, setFilters] = useState<Record<string, string | undefined>>({});
  const [generateModal, setGenerateModal] = useState(false);
  const [statusModal, setStatusModal] = useState(false);
  const [currentBill, setCurrentBill] = useState<PaymentBill | null>(null);
  const [form] = Form.useForm();

  useEffect(() => { fetchData(); }, [page, pageSize, filters]);

  const fetchData = async () => {
    setLoading(true);
    try { const res = await getBills({ page, pageSize, ...filters }); setData(res.data.records); setTotal(res.data.total); } catch { message.error('加载账单数据失败'); }
    setLoading(false);
  };

  const handleGenerate = async () => {
    try { const values = await form.validateFields(); const res = await generateBills(values); message.success(res.message); setGenerateModal(false); fetchData(); } catch (e: any) { if (!e?.errorFields) message.error('生成账单失败'); }
  };

  const handleStatusUpdate = async () => {
    try {
      const values = await form.validateFields();
      if (currentBill) { await updateBillStatus(currentBill.id!, values); message.success('状态更新成功'); }
      setStatusModal(false); fetchData();
    } catch (e: any) { if (!e?.errorFields) message.error('更新状态失败'); }
  };

  const handleExport = async () => {
    try {
      const res: any = await exportBills(filters);
      const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a'); a.href = url; a.download = '缴费账单.xlsx'; a.click();
      window.URL.revokeObjectURL(url);
    } catch { message.error('导出失败'); }
  };

  const columns = [
    { title: '账单编号', dataIndex: 'billNo', width: 150 },
    { title: '学生姓名', dataIndex: 'studentName' },
    { title: '学号', dataIndex: 'studentNo' },
    { title: '宿舍号', dataIndex: 'dormitoryNo' },
    { title: '收费项目', dataIndex: 'feeItemName' },
    { title: '学期', dataIndex: 'semester' },
    { title: '应缴', dataIndex: 'amount' },
    { title: '已缴', dataIndex: 'paidAmount' },
    { title: '截止日期', dataIndex: 'dueDate' },
    { title: '状态', dataIndex: 'status', render: (s: string) => { const m = statusMap[s] || { color: 'default', text: s }; return <Tag color={m.color}>{m.text}</Tag>; } },
    { title: '操作', render: (_: any, record: PaymentBill) => (
        <Button type="link" size="small" onClick={() => { setCurrentBill(record); setStatusModal(true); form.setFieldsValue({ status: record.status }); }}>修正状态</Button>
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <div className="search-form">
        <Space wrap>
          <Input placeholder="学号" allowClear style={{ width: 120 }} onChange={(e) => setFilters((f) => ({ ...f, studentNo: e.target.value || undefined }))} />
          <Input placeholder="宿舍号" allowClear style={{ width: 120 }} onChange={(e) => setFilters((f) => ({ ...f, dormitoryNo: e.target.value || undefined }))} />
          <Input placeholder="学期" allowClear style={{ width: 120 }} onChange={(e) => setFilters((f) => ({ ...f, semester: e.target.value || undefined }))} />
          <Select placeholder="状态" allowClear style={{ width: 120 }} onChange={(v) => setFilters((f) => ({ ...f, status: v || undefined }))}>
            <Select.Option value="UNPAID">未缴</Select.Option><Select.Option value="PAID">已缴</Select.Option><Select.Option value="OVERDUE">逾期</Select.Option>
          </Select>
          <Button type="primary" onClick={fetchData}>搜索</Button>
        </Space>
      </div>
      <Space style={{ marginBottom: 16 }}>
        <Button type="primary" icon={<ThunderboltOutlined />} onClick={() => { setGenerateModal(true); form.resetFields(); }}>生成账单</Button>
        <Button icon={<UploadOutlined />} onClick={handleExport}>导出Excel</Button>
      </Space>
      <Table rowKey="id" columns={columns} dataSource={data} loading={loading} pagination={{ current: page, pageSize, total, onChange: (p, ps) => { setPage(p); setPageSize(ps); } }} />
      <Modal title="生成账单" open={generateModal} onOk={handleGenerate} onCancel={() => setGenerateModal(false)} okText="确定" cancelText="取消">
        <Alert variant="warning" appearance="light" size="sm" className="mb-4">
          <AlertIcon><AlertTriangle className="size-3.5" /></AlertIcon>
          <AlertTitle>请输入学期信息，系统将为所有学生生成对应学期的收费账单</AlertTitle>
        </Alert>
        <Form form={form} layout="vertical"><Form.Item name="semester" label="学期" rules={[{ required: true }]}><Input placeholder="如: 2024-1" /></Form.Item></Form>
      </Modal>
      <Modal title="修正账单状态" open={statusModal} onOk={handleStatusUpdate} onCancel={() => setStatusModal(false)} okText="确定" cancelText="取消">
        <Form form={form} layout="vertical">
          <Form.Item name="status" label="状态"><Select><Select.Option value="UNPAID">未缴</Select.Option><Select.Option value="PAID">已缴</Select.Option><Select.Option value="OVERDUE">逾期</Select.Option><Select.Option value="CANCELLED">已取消</Select.Option></Select></Form.Item>
          <Form.Item name="remark" label="备注"><Input.TextArea rows={3} /></Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default Bill;
