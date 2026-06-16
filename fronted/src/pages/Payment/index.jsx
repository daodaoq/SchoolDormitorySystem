import React, { useEffect, useState } from 'react';
import { Table, Button, Space, Input, Select, Tag, message, Modal, Card } from 'antd';
import { getPaymentRecords, createPaymentOrder } from '../../services/api';

function Payment() {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [filters, setFilters] = useState({});
  const [payModal, setPayModal] = useState(false);
  const [payForm, setPayForm] = useState(null);
  const [billIdInput, setBillIdInput] = useState('');

  useEffect(() => { fetchData(); }, [page, pageSize, filters]);

  const fetchData = async () => {
    setLoading(true);
    try {
      const res = await getPaymentRecords({ page, pageSize, ...filters });
      setData(res.data.records);
      setTotal(res.data.total);
    } catch (e) { console.error(e); }
    setLoading(false);
  };

  const handleCreateOrder = async () => {
    if (!billIdInput) { message.warning('请输入账单ID'); return; }
    try {
      const res = await createPaymentOrder(Number(billIdInput));
      setPayForm(res.data);
      setPayModal(true);
      setBillIdInput('');
      fetchData();
    } catch (e) { console.error(e); }
  };

  const statusMap = { WAITING: { color: 'blue', text: '等待支付' }, SUCCESS: { color: 'green', text: '已支付' }, CLOSED: { color: 'default', text: '已关闭' }, REFUND: { color: 'orange', text: '已退款' } };

  const columns = [
    { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 180 },
    { title: '学生姓名', dataIndex: 'studentName', key: 'studentName' },
    { title: '学号', dataIndex: 'studentNo', key: 'studentNo' },
    { title: '账单编号', dataIndex: 'billNo', key: 'billNo', width: 150 },
    { title: '金额', dataIndex: 'amount', key: 'amount' },
    { title: '支付方式', dataIndex: 'payMethod', key: 'payMethod' },
    { title: '交易号', dataIndex: 'tradeNo', key: 'tradeNo', width: 150 },
    { title: '支付时间', dataIndex: 'payTime', key: 'payTime' },
    { title: '状态', dataIndex: 'status', key: 'status',
      render: (s) => { const m = statusMap[s] || { color: 'default', text: s }; return <Tag color={m.color}>{m.text}</Tag>; }
    },
  ];

  return (
    <div>
      <Card title="创建支付订单" style={{ marginBottom: 16 }}>
        <Space>
          <Input placeholder="输入账单ID" value={billIdInput} onChange={(e) => setBillIdInput(e.target.value)} style={{ width: 200 }} />
          <Button type="primary" onClick={handleCreateOrder}>创建支付订单</Button>
        </Space>
      </Card>

      <div className="search-form">
        <Space>
          <Input placeholder="学号" allowClear style={{ width: 150 }}
            onChange={(e) => setFilters((f) => ({ ...f, studentNo: e.target.value || undefined }))} />
          <Button type="primary" onClick={fetchData}>搜索</Button>
        </Space>
      </div>

      <Table rowKey="id" columns={columns} dataSource={data} loading={loading}
        pagination={{ current: page, pageSize, total, onChange: (p, ps) => { setPage(p); setPageSize(ps); } }} />

      <Modal title="支付宝支付" open={payModal} onCancel={() => setPayModal(false)} footer={null} width={600}>
        {payForm && (
          <div>
            <p><strong>订单号:</strong> {payForm.orderNo}</p>
            <p><strong>金额:</strong> ¥{payForm.amount}</p>
            {payForm.receiptUrl ? (
              <div dangerouslySetInnerHTML={{ __html: payForm.receiptUrl }} />
            ) : (
              <div style={{ padding: 40, textAlign: 'center', color: '#999' }}>
                支付表单生成失败。请检查支付宝沙箱配置。
              </div>
            )}
          </div>
        )}
      </Modal>
    </div>
  );
}

export default Payment;
