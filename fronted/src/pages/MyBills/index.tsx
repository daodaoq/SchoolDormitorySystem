import React, { useEffect, useState } from 'react';
import { Card, Table, Tag, Button, Spin, Empty, Statistic, Row, Col, message } from 'antd';
import { DollarOutlined, ClockCircleOutlined, CheckCircleOutlined } from '@ant-design/icons';
import { getBills, createPaymentOrder } from '../../services/api';
import { useAuth } from '../../contexts/AuthContext';
import type { PaymentBill } from '../../types';

const statusMap: Record<string, { color: string; text: string }> = {
  UNPAID: { color: 'orange', text: '待缴费' },
  PAID: { color: 'green', text: '已缴费' },
  OVERDUE: { color: 'red', text: '已逾期' },
};

const MyBills: React.FC = () => {
  const { user } = useAuth();
  const [bills, setBills] = useState<PaymentBill[]>([]);
  const [loading, setLoading] = useState(true);
  const [payingIds, setPayingIds] = useState<Set<number>>(new Set());

  useEffect(() => { loadBills(); }, []);

  const loadBills = async () => {
    try {
      const res = await getBills({ studentNo: user?.username, pageSize: 999 });
      setBills(res.data.records || []);
    } catch { /* */ }
    setLoading(false);
  };

  const handlePay = async (bill: PaymentBill) => {
    if (!bill.id) return;
    setPayingIds(prev => new Set(prev).add(bill.id!));
    try {
      const res = await createPaymentOrder(bill.id);
      const orderNo = res.data?.orderNo;
      if (!orderNo) { message.error('创建支付订单失败'); return; }
      window.open(`/api/payment/pay-page/${orderNo}`, '_blank');
      loadBills();
    } catch {
      message.error('创建支付订单失败');
    } finally {
      setPayingIds(prev => { const next = new Set(prev); next.delete(bill.id!); return next; });
    }
  };

  const paid = bills.filter((b) => b.status === 'PAID');
  const unpaid = bills.filter((b) => b.status === 'UNPAID' || b.status === 'OVERDUE');
  const totalUnpaid = unpaid.reduce((s, b) => s + b.amount - b.paidAmount, 0);

  if (loading) return <Spin size="large" style={{ display: 'block', margin: '120px auto' }} />;

  return (
    <div style={{ padding: 24 }}>
      <h2 style={{
        fontFamily: "'Bodoni Moda', 'Noto Serif SC', serif",
        fontSize: 22, fontWeight: 700, marginBottom: 24,
        color: '#1A1A1A', letterSpacing: '-0.01em',
      }}>我的账单</h2>

      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={8}>
          <Card style={{ borderRadius: 24, textAlign: 'center', border: '1px solid rgba(26,26,26,0.10)', boxShadow: '3px 3px 0 rgba(26,26,26,0.05)' }}>
            <Statistic title="待缴金额" value={totalUnpaid} prefix="¥" precision={2}
              valueStyle={{ color: '#E85D4E', fontWeight: 700, fontFamily: "'Bodoni Moda', serif" }} />
          </Card>
        </Col>
        <Col xs={8}>
          <Card style={{ borderRadius: 24, textAlign: 'center', border: '1px solid rgba(26,26,26,0.10)', boxShadow: '3px 3px 0 rgba(26,26,26,0.05)' }}>
            <Statistic title="已缴账单" value={paid.length} suffix="笔" prefix={<CheckCircleOutlined />}
              valueStyle={{ color: '#C4D94E', fontWeight: 700, fontFamily: "'Bodoni Moda', serif" }} />
          </Card>
        </Col>
        <Col xs={8}>
          <Card style={{ borderRadius: 24, textAlign: 'center', border: '1px solid rgba(26,26,26,0.10)', boxShadow: '3px 3px 0 rgba(26,26,26,0.05)' }}>
            <Statistic title="待处理" value={unpaid.length} suffix="笔" prefix={<ClockCircleOutlined />}
              valueStyle={{ fontWeight: 700, color: '#1A1A1A', fontFamily: "'Bodoni Moda', serif" }} />
          </Card>
        </Col>
      </Row>

      <Card style={{ borderRadius: 24, border: '1px solid rgba(26,26,26,0.10)', boxShadow: '3px 3px 0 rgba(26,26,26,0.05)' }}>
        {bills.length > 0 ? (
          <Table rowKey="id" dataSource={bills} pagination={false}
            columns={[
              { title: '账单编号', dataIndex: 'billNo', width: 160 },
              { title: '收费项目', dataIndex: 'feeItemName' },
              { title: '学期', dataIndex: 'semester' },
              { title: '应缴金额', dataIndex: 'amount', render: (v: number) => <span style={{ fontWeight: 600 }}>¥{v}</span> },
              { title: '已缴金额', dataIndex: 'paidAmount', render: (v: number) => <span style={{ color: '#16a34a' }}>¥{v}</span> },
              { title: '截止日期', dataIndex: 'dueDate' },
              { title: '状态', dataIndex: 'status', render: (s: string) => {
                const m = statusMap[s] || { color: 'default', text: s };
                return <Tag color={m.color} style={{ borderRadius: 8 }}>{m.text}</Tag>;
              }},
              { title: '操作', key: 'action', render: (_: any, record: PaymentBill) => (
                  record.status === 'UNPAID' || record.status === 'OVERDUE' ? (
                    <Button type="primary" size="small" icon={<DollarOutlined />}
                      loading={payingIds.has(record.id!)} onClick={() => handlePay(record)}>
                      去支付
                    </Button>
                  ) : null
                ),
              },
            ]}
          />
        ) : <Empty description="暂无账单" />}
      </Card>
    </div>
  );
};

export default MyBills;
