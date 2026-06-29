import React, { useEffect, useState } from 'react';
import { Table, Button, Space, Input, Tag, message } from 'antd';
import { getPaymentRecords } from '../../services/api';
import type { PaymentRecord } from '../../types';

const payStatusMap: Record<string, { color: string; text: string }> = {
  WAITING: { color: 'blue', text: '等待支付' }, SUCCESS: { color: 'green', text: '已支付' },
  CLOSED: { color: 'default', text: '已关闭' }, REFUND: { color: 'orange', text: '已退款' },
};
const payMethodMap: Record<string, string> = {
  ALIPAY: '支付宝', WECHAT: '微信支付', CASH: '现金', BANK: '银行转账', SYSTEM: '系统',
};

const Payment: React.FC = () => {
  const [records, setRecords] = useState<PaymentRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [studentNoFilter, setStudentNoFilter] = useState('');

  useEffect(() => { fetchRecords(); }, [page, pageSize]);

  const fetchRecords = async () => {
    setLoading(true);
    try {
      const res = await getPaymentRecords({ page, pageSize, studentNo: studentNoFilter || undefined });
      setRecords(res.data.records); setTotal(res.data.total);
    } catch { message.error('加载支付记录失败'); }
    setLoading(false);
  };

  const columns = [
    { title: '订单号', dataIndex: 'orderNo', width: 180 },
    { title: '学生姓名', dataIndex: 'studentName' },
    { title: '学号', dataIndex: 'studentNo' },
    { title: '账单编号', dataIndex: 'billNo', width: 150 },
    { title: '金额', dataIndex: 'amount', render: (v: number) => `¥${v}` },
    { title: '支付方式', dataIndex: 'payMethod', render: (v: string) => payMethodMap[v] || v || '-' },
    { title: '交易号', dataIndex: 'tradeNo', width: 150, render: (v: string) => v || '-' },
    { title: '支付时间', dataIndex: 'payTime', render: (v: string) => v || '-' },
    { title: '状态', dataIndex: 'status', render: (s: string) => {
      const m = payStatusMap[s] || { color: 'default', text: s };
      return <Tag color={m.color}>{m.text}</Tag>;
    }},
  ];

  return (
    <div style={{ padding: 24 }}>
      <div className="search-form">
        <Space wrap>
          <Input placeholder="学号筛选" allowClear style={{ width: 150 }}
            value={studentNoFilter} onChange={e => setStudentNoFilter(e.target.value)}
            onPressEnter={fetchRecords} />
          <Button type="primary" onClick={fetchRecords}>搜索</Button>
        </Space>
      </div>
      <Table rowKey="id" columns={columns} dataSource={records} loading={loading}
        pagination={{ current: page, pageSize, total, onChange: (p, ps) => { setPage(p); setPageSize(ps); } }} />
    </div>
  );
};

export default Payment;
