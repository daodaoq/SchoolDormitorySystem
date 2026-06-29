import React, { useEffect, useState } from 'react';
import { Card, Table, Tag, Button, Spin, Empty, Statistic, Row, Col, Modal, message, Space, Tabs } from 'antd';
import { DollarOutlined, ClockCircleOutlined, CheckCircleOutlined, CheckOutlined } from '@ant-design/icons';
import { getBills, createPaymentOrder, directPay, getPaymentRecords } from '../../services/api';
import { useAuthStore } from '../../stores/authStore';
import type { PaymentBill, PaymentRecord } from '../../types';

const statusMap: Record<string, { color: string; text: string }> = {
  UNPAID: { color: 'orange', text: '待缴费' },
  PAID: { color: 'green', text: '已缴费' },
  OVERDUE: { color: 'red', text: '已逾期' },
};

const payStatusMap: Record<string, { color: string; text: string }> = {
  WAITING: { color: 'blue', text: '等待支付' }, SUCCESS: { color: 'green', text: '已支付' },
  CLOSED: { color: 'default', text: '已关闭' }, REFUND: { color: 'orange', text: '已退款' },
};

const MyBills: React.FC = () => {
  const user = useAuthStore((s) => s.user);
  const [bills, setBills] = useState<PaymentBill[]>([]);
  const [loading, setLoading] = useState(true);
  const [payingIds, setPayingIds] = useState<Set<number>>(new Set());
  const [confirmTarget, setConfirmTarget] = useState<PaymentBill | null>(null);
  const [confirming, setConfirming] = useState(false);
  const [records, setRecords] = useState<PaymentRecord[]>([]);
  const [recordsLoading, setRecordsLoading] = useState(false);
  const [activeTab, setActiveTab] = useState('bills');

  useEffect(() => { loadBills(); }, []);

  const loadBills = async () => {
    try {
      const res = await getBills({ studentNo: user?.username, pageSize: 999 });
      setBills(res.data.records || []);
    } catch { message.error('加载账单失败'); }
    setLoading(false);
  };

  const loadRecords = async () => {
    setRecordsLoading(true);
    try {
      const res = await getPaymentRecords({ studentNo: user?.username, pageSize: 999 });
      setRecords(res.data.records || []);
    } catch { message.error('加载支付记录失败'); }
    setRecordsLoading(false);
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

  const handleTabChange = (key: string) => {
    setActiveTab(key);
    if (key === 'records') loadRecords();
  };

  const handleDirectPay = (bill: PaymentBill) => {
    setConfirmTarget(bill);
  };

  const doDirectPay = async () => {
    if (!confirmTarget?.id) return;
    setConfirming(true);
    try {
      await directPay(confirmTarget.id);
      message.success('缴费成功');
      setConfirmTarget(null);
      loadBills();
    } catch {
      message.error('操作失败');
    }
    setConfirming(false);
  };

  const paid = bills.filter((b) => b.status === 'PAID');
  const unpaid = bills.filter((b) => b.status === 'UNPAID' || b.status === 'OVERDUE');
  const totalUnpaid = unpaid.reduce((s, b) => s + b.amount - b.paidAmount, 0);

  if (loading) return <Spin size="large" style={{ display: 'block', margin: '120px auto' }} />;

  return (
    <div style={{ padding: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 24 }}>
        <h2 style={{
          fontFamily: "'Bodoni Moda', 'Noto Serif SC', serif",
          fontSize: 22, fontWeight: 700, margin: 0,
          color: '#1A1A1A', letterSpacing: '-0.01em',
        }}>我的账单</h2>
        <Card size="small" styles={{ body: { padding: '8px 16px' } }} style={{
          borderRadius: 16, border: '1px dashed rgba(232,93,78,0.40)',
          background: 'rgba(232,93,78,0.04)', boxShadow: 'none',
        }}>
          <div style={{ fontSize: 12, color: 'rgba(26,26,26,0.55)', display: 'flex', alignItems: 'center', gap: 16, whiteSpace: 'nowrap' }}>
            <span style={{ fontWeight: 600, color: 'rgba(26,26,26,0.40)' }}>🧪 沙箱</span>
            账号：<code style={{ background: '#FFF', padding: '1px 6px', borderRadius: 4 }}>qmtvxw2273@sandbox.com</code>
            登录密码：<code style={{ background: '#FFF', padding: '1px 6px', borderRadius: 4 }}>111111</code>
            支付密码：<code style={{ background: '#FFF', padding: '1px 6px', borderRadius: 4 }}>111111</code>
          </div>
        </Card>
      </div>

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
        <Tabs activeKey={activeTab} onChange={handleTabChange} items={[
          {
            key: 'bills',
            label: '待缴账单',
            children: bills.length > 0 ? (
              <Table rowKey="id" dataSource={bills} pagination={false}
                columns={[
                  { title: '账单编号', dataIndex: 'billNo', width: 160 },
                  { title: '收费项目', dataIndex: 'feeItemName' },
                  { title: '学期', dataIndex: 'semester' },
                  { title: '应缴金额', dataIndex: 'amount', render: (v: number) => <span style={{ fontWeight: 600 }}>¥{v.toFixed(2)}</span> },
                  { title: '已缴金额', dataIndex: 'paidAmount', render: (v: number) => <span style={{ color: '#16a34a' }}>¥{v.toFixed(2)}</span> },
                  { title: '截止日期', dataIndex: 'dueDate' },
                  { title: '状态', dataIndex: 'status', render: (s: string) => {
                    const m = statusMap[s] || { color: 'default', text: s };
                    return <Tag color={m.color} style={{ borderRadius: 8 }}>{m.text}</Tag>;
                  }},
                  { title: '操作', key: 'action', width: 200, render: (_: any, record: PaymentBill) => (
                      record.status === 'UNPAID' || record.status === 'OVERDUE' ? (
                        <Space>
                          <Button type="primary" size="small" icon={<DollarOutlined />}
                            loading={payingIds.has(record.id!)} onClick={() => handlePay(record)}>
                            支付宝支付
                          </Button>
                          <Button size="small" icon={<CheckOutlined />}
                            onClick={() => handleDirectPay(record)}>
                            确认缴费
                          </Button>
                        </Space>
                      ) : null
                    ),
                  },
                ]}
              />
            ) : <Empty description="暂无账单" />,
          },
          {
            key: 'records',
            label: '支付记录',
            children: recordsLoading ? <Spin style={{ display: 'block', margin: '40px auto' }} /> :
              records.length > 0 ? (
                <Table rowKey="id" dataSource={records} pagination={false}
                  columns={[
                    { title: '订单号', dataIndex: 'orderNo', width: 180 },
                    { title: '账单编号', dataIndex: 'billNo', width: 150 },
                    { title: '金额', dataIndex: 'amount', render: (v: number) => `¥${v.toFixed(2)}` },
                    { title: '支付方式', dataIndex: 'payMethod', render: (v: string) => v === 'ALIPAY' ? '支付宝' : v === 'SYSTEM' ? '系统确认' : v },
                    { title: '支付时间', dataIndex: 'payTime', render: (v: string) => v || '-' },
                    { title: '状态', dataIndex: 'status', render: (s: string) => {
                      const m = payStatusMap[s] || { color: 'default', text: s };
                      return <Tag color={m.color} style={{ borderRadius: 8 }}>{m.text}</Tag>;
                    }},
                  ]}
                />
              ) : <Empty description="暂无支付记录" />,
          },
        ]} />
      </Card>

      <Modal
        title="确认缴费"
        open={confirmTarget !== null}
        onOk={doDirectPay}
        onCancel={() => setConfirmTarget(null)}
        confirmLoading={confirming}
        okText="确认缴费"
        cancelText="取消"
      >
        {confirmTarget && (
          <p>确定将「<strong>{confirmTarget.feeItemName}</strong>」¥{confirmTarget.amount.toFixed(2)} 标记为已缴费吗？</p>
        )}
      </Modal>
    </div>
  );
};

export default MyBills;
