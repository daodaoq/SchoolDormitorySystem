import React, { useEffect, useState } from 'react';
import { Table, Button, Space, Modal, Form, Input, Select, InputNumber, message, Popconfirm, Tag } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { AlertTriangle } from 'lucide-react';
import { getDormitories, addDormitory, updateDormitory, deleteDormitory } from '../../services/api';
import { Alert, AlertIcon, AlertTitle } from '@/components/ui/alert-1';

interface DormitoryRecord {
  id?: number;
  dormitoryNo: string;
  building?: string;
  floor?: string;
  roomType?: string;
  capacity?: number;
  status: string;
}

const roomTypeOptions = ['单人间', '双人间', '四人间', '六人间'];

const Dormitory: React.FC = () => {
  const [data, setData] = useState<DormitoryRecord[]>([]);
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
    try { const res = await getDormitories({ page, pageSize, ...filters }); setData(res.data.records); setTotal(res.data.total); } catch { message.error('加载宿舍数据失败'); }
    setLoading(false);
  };

  const handleAdd = () => { setEditingId(null); form.resetFields(); form.setFieldsValue({ status: 'ACTIVE', capacity: 4 }); setModalVisible(true); };
  const handleEdit = (record: DormitoryRecord) => { setEditingId(record.id!); form.setFieldsValue(record); setModalVisible(true); };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      if (editingId) { await updateDormitory(editingId, values); message.success('更新成功'); }
      else { await addDormitory(values); message.success('添加成功'); }
      setModalVisible(false); fetchData();
    } catch (e: any) { if (!e?.errorFields) message.error('操作失败，请稍后重试'); }
  };

  const columns = [
    { title: '宿舍编号', dataIndex: 'dormitoryNo' },
    { title: '楼栋', dataIndex: 'building' },
    { title: '楼层', dataIndex: 'floor' },
    { title: '房型', dataIndex: 'roomType' },
    { title: '容量', dataIndex: 'capacity' },
    { title: '状态', dataIndex: 'status', render: (s: string) => <Tag color={s === 'ACTIVE' ? 'green' : 'red'}>{s === 'ACTIVE' ? '启用' : '停用'}</Tag> },
    { title: '操作', render: (_: any, record: DormitoryRecord) => (
        <Space>
          <Button type="link" size="small" onClick={() => handleEdit(record)}>编辑</Button>
          <Popconfirm title="确定删除?" onConfirm={() => deleteDormitory(record.id!).then(() => { message.success('删除成功'); fetchData(); })}>
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
          <Input placeholder="宿舍编号" allowClear style={{ width: 130 }} onChange={(e) => setFilters((f) => ({ ...f, dormitoryNo: e.target.value || undefined }))} />
          <Input placeholder="楼栋" allowClear style={{ width: 130 }} onChange={(e) => setFilters((f) => ({ ...f, building: e.target.value || undefined }))} />
          <Button type="primary" onClick={fetchData}>搜索</Button>
        </Space>
      </div>
      <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd} style={{ marginBottom: 16 }}>新增宿舍</Button>
      <Table rowKey="id" columns={columns} dataSource={data} loading={loading} pagination={{ current: page, pageSize, total, onChange: (p, ps) => { setPage(p); setPageSize(ps); } }} />
      <Modal title={editingId ? '编辑宿舍' : '新增宿舍'} open={modalVisible} onOk={handleSubmit} onCancel={() => setModalVisible(false)} okText="确定" cancelText="取消">
        <Alert variant="warning" appearance="light" size="sm" className="mb-4">
          <AlertIcon><AlertTriangle className="size-3.5" /></AlertIcon>
          <AlertTitle>带 * 的字段为必填项，请完整填写后提交</AlertTitle>
        </Alert>
        <Form form={form} layout="vertical">
          <Form.Item name="dormitoryNo" label="宿舍编号" rules={[{ required: true, message: '请输入宿舍编号' }]}><Input placeholder="如: A-101" /></Form.Item>
          <Form.Item name="building" label="楼栋"><Input placeholder="如: A栋" /></Form.Item>
          <Form.Item name="floor" label="楼层"><Input placeholder="如: 1层" /></Form.Item>
          <Form.Item name="roomType" label="房型">
            <Select>{roomTypeOptions.map((t) => <Select.Option key={t} value={t}>{t}</Select.Option>)}</Select>
          </Form.Item>
          <Form.Item name="capacity" label="容纳人数"><InputNumber min={1} max={10} style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="status" label="状态"><Select><Select.Option value="ACTIVE">启用</Select.Option><Select.Option value="INACTIVE">停用</Select.Option></Select></Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default Dormitory;
