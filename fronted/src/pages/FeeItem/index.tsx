import React, { useEffect, useState } from 'react';
import { Table, Button, Space, Modal, Form, Input, Select, InputNumber, message, Popconfirm, Tag } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { AlertTriangle } from 'lucide-react';
import { getFeeItems, addFeeItem, updateFeeItem, deleteFeeItem } from '../../services/api';
import { Alert, AlertIcon, AlertTitle } from '@/components/ui/alert-1';
import type { FeeItem } from '../../types';

const feeTypeMap: Record<string, string> = { ACCOMMODATION: '住宿费', WATER: '水费', ELECTRICITY: '电费', AC: '空调费', NETWORK: '网络费' };
const billingMap: Record<string, string> = { MONTHLY: '按月', SEMESTER: '按学期', YEARLY: '按年' };
const dormTypeMap: Record<string, string> = { ALL: '全部', MALE: '男生宿舍', FEMALE: '女生宿舍' };

const FeeItemPage: React.FC = () => {
  const [data, setData] = useState<FeeItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form] = Form.useForm();
  const [filters, setFilters] = useState<Record<string, string | undefined>>({});

  useEffect(() => { fetchData(); }, [page, pageSize, filters]);

  const fetchData = async () => {
    setLoading(true);
    try { const res = await getFeeItems({ page, pageSize, ...filters }); setData(res.data.records); setTotal(res.data.total); } catch { message.error('加载数据失败'); }
    setLoading(false);
  };

  const handleAdd = () => { setEditingId(null); form.resetFields(); setModalVisible(true); };
  const handleEdit = (record: FeeItem) => { setEditingId(record.id!); form.setFieldsValue(record); setModalVisible(true); };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      if (editingId) { await updateFeeItem(editingId, values); message.success('更新成功'); }
      else { await addFeeItem(values); message.success('添加成功'); }
      setModalVisible(false); fetchData();
    } catch (e: any) {
      if (e?.errorFields) return;
      message.error('操作失败，请稍后重试');
    }
  };

  const columns = [
    { title: '项目名称', dataIndex: 'itemName' },
    { title: '收费类型', dataIndex: 'feeType', render: (v: string) => feeTypeMap[v] || v },
    { title: '单价(元)', dataIndex: 'unitPrice' },
    { title: '计费周期', dataIndex: 'billingCycle', render: (v: string) => billingMap[v] || v },
    { title: '适用宿舍', dataIndex: 'applicableDormType', render: (v: string) => dormTypeMap[v] || v || '-' },
    { title: '状态', dataIndex: 'status', render: (s: string) => <Tag color={s === 'ACTIVE' ? 'green' : 'red'}>{s === 'ACTIVE' ? '启用' : '停用'}</Tag> },
    { title: '操作', render: (_: any, record: FeeItem) => (
        <Space>
          <Button type="link" size="small" onClick={() => handleEdit(record)}>编辑</Button>
          <Popconfirm title="确定删除?" onConfirm={() => deleteFeeItem(record.id!).then(() => { message.success('删除成功'); fetchData(); })}>
            <Button type="link" size="small" danger>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <div className="search-form">
        <Space wrap>
          <Input placeholder="项目名称" allowClear style={{ width: 150 }}
            onChange={(e) => setFilters((f) => ({ ...f, itemName: e.target.value || undefined }))} />
          <Input placeholder="收费类型" allowClear style={{ width: 150 }}
            onChange={(e) => setFilters((f) => ({ ...f, feeType: e.target.value || undefined }))} />
          <Button type="primary" onClick={fetchData}>搜索</Button>
        </Space>
      </div>
      <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd} style={{ marginBottom: 16 }}>新增收费项目</Button>
      <Table rowKey="id" columns={columns} dataSource={data} loading={loading} pagination={{ current: page, pageSize, total, onChange: (p, ps) => { setPage(p); setPageSize(ps); } }} />
      <Modal title={editingId ? '编辑收费项目' : '新增收费项目'} open={modalVisible} onOk={handleSubmit} onCancel={() => setModalVisible(false)} okText="确定" cancelText="取消">
        <Alert variant="warning" appearance="light" size="sm" className="mb-4">
          <AlertIcon><AlertTriangle className="size-3.5" /></AlertIcon>
          <AlertTitle>带 * 的字段为必填项，请完整填写后提交</AlertTitle>
        </Alert>
        <Form form={form} layout="vertical">
          <Form.Item name="itemName" label="项目名称" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="feeType" label="收费类型" rules={[{ required: true }]}>
            <Input placeholder="如：住宿费、维修费" />
          </Form.Item>
          <Form.Item name="unitPrice" label="单价" rules={[{ required: true }]}><InputNumber min={0} style={{ width: '100%' }} precision={2} /></Form.Item>
          <Form.Item name="billingCycle" label="计费周期" rules={[{ required: true }]}>
            <Select>{Object.entries(billingMap).map(([k, v]) => <Select.Option key={k} value={k}>{v}</Select.Option>)}</Select>
          </Form.Item>
          <Form.Item name="applicableDormType" label="适用宿舍类型"><Input /></Form.Item>
          <Form.Item name="status" label="状态"><Select><Select.Option value="ACTIVE">启用</Select.Option><Select.Option value="INACTIVE">停用</Select.Option></Select></Form.Item>
          <Form.Item name="description" label="说明"><Input.TextArea rows={3} /></Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default FeeItemPage;
