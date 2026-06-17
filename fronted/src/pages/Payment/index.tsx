import React, { useEffect, useState } from 'react';
import { Table, Button, Space, Input, Tag, message, Card, Tabs } from 'antd';
import { DollarOutlined, SearchOutlined } from '@ant-design/icons';
import { getPaymentRecords, createPaymentOrder, getBills } from '../../services/api';
import type { PaymentBill, PaymentRecord } from '../../types';

const payStatusMap: Record<string, { color: string; text: string }> = {
  WAITING: { color: 'blue', text: '等待支付' }, SUCCESS: { color: 'green', text: '已支付' },
  CLOSED: { color: 'default', text: '已关闭' }, REFUND: { color: 'orange', text: '已退款' },
};
const billStatusMap: Record<string, { color: string; text: string }> = {
  UNPAID: { color: 'orange', text: '未缴' }, PAID: { color: 'green', text: '已缴' },
  OVERDUE: { color: 'red', text: '逾期' }, CANCELLED: { color: 'default', text: '已取消' },
};

const Payment: React.FC = () => {
  const [activeTab, setActiveTab] = useState('bills');

  // ====== 待缴账单 Tab ======
  const [studentNoInput, setStudentNoInput] = useState('');
  const [bills, setBills] = useState<PaymentBill[]>([]);
  const [billsLoading, setBillsLoading] = useState(false);
  const [payingIds, setPayingIds] = useState<Set<number>>(new Set());

  // ====== 支付记录 Tab ======
  const [records, setRecords] = useState<PaymentRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [recordFilter, setRecordFilter] = useState('');

  useEffect(() => {
    if (activeTab === 'records') fetchRecords();
  }, [page, pageSize, activeTab]);

  const fetchRecords = async () => {
    setLoading(true);
    try {
      const res = await getPaymentRecords({ page, pageSize, studentNo: recordFilter || undefined });
      setRecords(res.data.records); setTotal(res.data.total);
    } catch { /* */ }
    setLoading(false);
  };

  /** 按学号查待缴账单 */
  const searchBills = async () => {
    if (!studentNoInput.trim()) { message.warning('请输入学号'); return; }
    setBillsLoading(true);
    try {
      const res = await getBills({ studentNo: studentNoInput.trim(), status: 'UNPAID,OVERDUE', pageSize: 999 });
      setBills(res.data.records || []);
      if (res.data.records?.length === 0) message.info('该学生没有待缴账单');
    } catch { /* */ }
    setBillsLoading(false);
  };

  /** 去支付 */
  const handlePay = async (bill: PaymentBill) => {
    if (!bill.id) return;
    setPayingIds(prev => new Set(prev).add(bill.id!));
    try {
      const res = await createPaymentOrder(bill.id);
      const orderNo = res.data?.orderNo;
      if (!orderNo) { message.error('创建支付订单失败'); return; }
      window.open(`/api/payment/pay-page/${orderNo}`, '_blank');
      searchBills();
    } catch {
      message.error('创建支付订单失败');
    } finally {
      setPayingIds(prev => { const next = new Set(prev); next.delete(bill.id!); return next; });
    }
  };

  const billColumns = [
    { title: '账单编号', dataIndex: 'billNo', width: 160 },
    { title: '学生姓名', dataIndex: 'studentName' },
    { title: '学号', dataIndex: 'studentNo' },
    { title: '收费项目', dataIndex: 'feeItemName' },
    { title: '学期', dataIndex: 'semester' },
    { title: '应缴金额', dataIndex: 'amount', render: (v: number) => `¥${v}` },
    { title: '已缴金额', dataIndex: 'paidAmount', render: (v: number) => `¥${v}` },
    { title: '截止日期', dataIndex: 'dueDate' },
    { title: '状态', dataIndex: 'status', render: (s: string) => {
      const m = billStatusMap[s] || { color: 'default', text: s };
      return <Tag color={m.color}>{m.text}</Tag>;
    }},
    { title: '操作', key: 'action', render: (_: any, record: PaymentBill) => (
        <Button type="primary" size="small" icon={<DollarOutlined />}
          loading={payingIds.has(record.id!)} onClick={() => handlePay(record)}>
          去支付
        </Button>
      ),
    },
  ];

  const recordColumns = [
    { title: '订单号', dataIndex: 'orderNo', width: 180 },
    { title: '学生姓名', dataIndex: 'studentName' },
    { title: '学号', dataIndex: 'studentNo' },
    { title: '账单编号', dataIndex: 'billNo', width: 150 },
    { title: '金额', dataIndex: 'amount', render: (v: number) => `¥${v}` },
    { title: '支付方式', dataIndex: 'payMethod' },
    { title: '交易号', dataIndex: 'tradeNo', width: 150, render: (v: string) => v || '-' },
    { title: '支付时间', dataIndex: 'payTime', render: (v: string) => v || '-' },
    { title: '状态', dataIndex: 'status', render: (s: string) => {
      const m = payStatusMap[s] || { color: 'default', text: s };
      return <Tag color={m.color}>{m.text}</Tag>;
    }},
  ];

  const tabItems = [
    {
      key: 'bills',
      label: '待缴账单',
      children: (
        <Card style={{ borderRadius: 12, border: '1px solid #f0f0f0' }}>
          <Space style={{ marginBottom: 16 }}>
            <Input placeholder="输入学生学号" value={studentNoInput}
              onChange={e => setStudentNoInput(e.target.value)}
              onPressEnter={searchBills} style={{ width: 200 }} />
            <Button type="primary" icon={<SearchOutlined />} onClick={searchBills} loading={billsLoading}>
              查询
            </Button>
          </Space>
          <Table rowKey="id" columns={billColumns} dataSource={bills}
            loading={billsLoading} pagination={false}
            locale={{ emptyText: '输入学号查询待缴账单' }} />
        </Card>
      ),
    },
    {
      key: 'records',
      label: '支付记录',
      children: (
        <>
          <Space style={{ marginBottom: 16 }}>
            <Input placeholder="学号筛选" allowClear style={{ width: 150 }}
              value={recordFilter} onChange={e => setRecordFilter(e.target.value)}
              onPressEnter={fetchRecords} />
            <Button type="primary" onClick={fetchRecords}>搜索</Button>
          </Space>
          <Table rowKey="id" columns={recordColumns} dataSource={records} loading={loading}
            pagination={{ current: page, pageSize, total, onChange: (p, ps) => { setPage(p); setPageSize(ps); } }} />
        </>
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Tabs activeKey={activeTab} onChange={setActiveTab} items={tabItems} />
    </div>
  );
};

export default Payment;
