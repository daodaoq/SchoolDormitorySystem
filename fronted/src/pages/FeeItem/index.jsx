import React, { useEffect, useState } from 'react';
import { Table, Button, Space, Modal, Form, Input, Select, InputNumber, message, Popconfirm, Tag } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { getFeeItems, addFeeItem, updateFeeItem, deleteFeeItem } from '../../services/api';

function FeeItem() {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [form] = Form.useForm();
  const [filters, setFilters] = useState({});

  useEffect(() => { fetchData(); }, [page, pageSize, filters]);

  const fetchData = async () => {
    setLoading(true);
    try {
      const res = await getFeeItems({ page, pageSize, ...filters });
      setData(res.data.records);
      setTotal(res.data.total);
    } catch (e) { console.error(e); }
    setLoading(false);
  };

  const handleAdd = () => { setEditingId(null); form.resetFields(); setModalVisible(true); };
  const handleEdit = (record) => { setEditingId(record.id); form.setFieldsValue(record); setModalVisible(true); };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      if (editingId) {
        await updateFeeItem(editingId, values);
        message.success('更新成功');
      } else {
        await addFeeItem(values);
        message.success('添加成功');
      }
      setModalVisible(false);
      fetchData();
    } catch (e) { console.error(e); }
  };

  const handleDelete = async (id) => {
    await deleteFeeItem(id);
    message.success('删除成功');
    fetchData();
  };

  const feeTypeMap = { ACCOMMODATION: '住宿费', WATER: '水费', ELECTRICITY: '电费', AC: '空调费', NETWORK: '网络费' };
  const billingMap = { MONTHLY: '按月', SEMESTER: '按学期', YEARLY: '按年' };

  const columns = [
    { title: '项目名称', dataIndex: 'itemName', key: 'itemName' },
    { title: '收费类型', dataIndex: 'feeType', key: 'feeType', render: (v) => feeTypeMap[v] || v },
    { title: '单价(元)', dataIndex: 'unitPrice', key: 'unitPrice' },
    { title: '计费周期', dataIndex: 'billingCycle', key: 'billingCycle', render: (v) => billingMap[v] || v },
    { title: '适用宿舍', dataIndex: 'applicableDormType', key: 'applicableDormType' },
    { title: '状态', dataIndex: 'status', key: 'status',
      render: (s) => <Tag color={s === 'ACTIVE' ? 'green' : 'red'}>{s === 'ACTIVE' ? '启用' : '停用'}</Tag>
    },
    {
      title: '操作', key: 'action',
      render: (_, record) => (
        <Space>
          <Button type="link" size="small" onClick={() => handleEdit(record)}>编辑</Button>
          <Popconfirm title="确定删除?" onConfirm={() => handleDelete(record.id)}>
            <Button type="link" size="small" danger>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div className="search-form">
        <Space wrap>
          <Select placeholder="收费类型" allowClear style={{ width: 150 }}
            onChange={(v) => setFilters((f) => ({ ...f, feeType: v || undefined }))}>
            {Object.entries(feeTypeMap).map(([k, v]) => <Select.Option key={k} value={k}>{v}</Select.Option>)}
          </Select>
          <Button type="primary" onClick={fetchData}>搜索</Button>
        </Space>
      </div>
      <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd} style={{ marginBottom: 16 }}>新增收费项目</Button>
      <Table rowKey="id" columns={columns} dataSource={data} loading={loading}
        pagination={{ current: page, pageSize, total, onChange: (p, ps) => { setPage(p); setPageSize(ps); } }} />
      <Modal title={editingId ? '编辑收费项目' : '新增收费项目'} open={modalVisible} onOk={handleSubmit} onCancel={() => setModalVisible(false)}>
        <Form form={form} layout="vertical">
          <Form.Item name="itemName" label="项目名称" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="feeType" label="收费类型" rules={[{ required: true }]}>
            <Select>{Object.entries(feeTypeMap).map(([k, v]) => <Select.Option key={k} value={k}>{v}</Select.Option>)}</Select>
          </Form.Item>
          <Form.Item name="unitPrice" label="单价" rules={[{ required: true }]}><InputNumber min={0} style={{ width: '100%' }} precision={2} /></Form.Item>
          <Form.Item name="billingCycle" label="计费周期" rules={[{ required: true }]}>
            <Select>{Object.entries(billingMap).map(([k, v]) => <Select.Option key={k} value={k}>{v}</Select.Option>)}</Select>
          </Form.Item>
          <Form.Item name="applicableDormType" label="适用宿舍类型"><Input /></Form.Item>
          <Form.Item name="status" label="状态">
            <Select><Select.Option value="ACTIVE">启用</Select.Option><Select.Option value="INACTIVE">停用</Select.Option></Select>
          </Form.Item>
          <Form.Item name="description" label="说明"><Input.TextArea rows={3} /></Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

export default FeeItem;
