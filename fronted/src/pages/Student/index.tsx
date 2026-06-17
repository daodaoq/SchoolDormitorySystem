import React, { useEffect, useState } from 'react';
import { Table, Button, Space, Modal, Form, Input, Select, DatePicker, Upload, message, Popconfirm, Tag } from 'antd';
import { PlusOutlined, UploadOutlined, DownloadOutlined, SearchOutlined } from '@ant-design/icons';
import { AlertTriangle } from 'lucide-react';
import { getStudents, addStudent, updateStudent, deleteStudent, importStudents, exportStudents, getActiveDormitories } from '../../services/api';
import { Alert, AlertIcon, AlertTitle } from '@/components/ui/alert-1';
import type { StudentDormitory } from '../../types';
import dayjs from 'dayjs';

const statusMap: Record<string, { color: string; text: string }> = {
  PAID: { color: 'green', text: '已缴' },
  UNPAID: { color: 'orange', text: '未缴' },
  OVERDUE: { color: 'red', text: '逾期' },
};

const Student: React.FC = () => {
  const [data, setData] = useState<StudentDormitory[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form] = Form.useForm();
  const [filters, setFilters] = useState<Record<string, string | undefined>>({});
  const [dormitoryOptions, setDormitoryOptions] = useState<{ label: string; value: string }[]>([]);

  useEffect(() => { fetchData(); }, [page, pageSize, filters]);
  useEffect(() => { getActiveDormitories().then(res => setDormitoryOptions((res.data || []).map((d: any) => ({ label: d.dormitoryNo + (d.building ? ' (' + d.building + ')' : ''), value: d.dormitoryNo })))).catch(() => {}); }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      const res = await getStudents({ page, pageSize, ...filters });
      setData(res.data.records);
      setTotal(res.data.total);
    } catch { /* handled */ }
    setLoading(false);
  };

  const handleAdd = async () => {
    setEditingId(null); form.resetFields();
    try { const res = await getActiveDormitories(); setDormitoryOptions((res.data || []).map((d: any) => ({ label: d.dormitoryNo + (d.building ? ' (' + d.building + ')' : ''), value: d.dormitoryNo }))); } catch { /* */ }
    setModalVisible(true);
  };
  const handleEdit = async (record: StudentDormitory) => {
    setEditingId(record.id!); form.setFieldsValue({ ...record, checkInDate: record.checkInDate ? dayjs(record.checkInDate) : null });
    try { const res = await getActiveDormitories(); setDormitoryOptions((res.data || []).map((d: any) => ({ label: d.dormitoryNo + (d.building ? ' (' + d.building + ')' : ''), value: d.dormitoryNo }))); } catch { /* */ }
    setModalVisible(true);
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      const payload = { ...values, checkInDate: values.checkInDate?.format('YYYY-MM-DD') };
      if (editingId) {
        await updateStudent(editingId, payload);
        message.success('更新成功');
      } else {
        await addStudent(payload);
        message.success('添加成功');
      }
      setModalVisible(false);
      fetchData();
    } catch { /* validation error */ }
  };

  const handleDelete = async (id: number) => { await deleteStudent(id); message.success('删除成功'); fetchData(); };
  const handleImport = async (file: File) => { try { const res = await importStudents(file); message.success(res.message); fetchData(); } catch { /* handled */ } return false; };
  const handleExport = async () => {
    try {
      const res: any = await exportStudents(filters);
      const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a'); a.href = url; a.download = '学生信息.xlsx'; a.click();
      window.URL.revokeObjectURL(url);
    } catch { /* handled */ }
  };

  const columns = [
    { title: '姓名', dataIndex: 'studentName' },
    { title: '学号', dataIndex: 'studentNo' },
    { title: '宿舍号', dataIndex: 'dormitoryNo' },
    { title: '联系电话', dataIndex: 'phone' },
    { title: '入住时间', dataIndex: 'checkInDate' },
    { title: '缴费状态', dataIndex: 'paymentStatus', render: (s: string) => { const st = statusMap[s] || { color: 'default', text: s }; return <Tag color={st.color}>{st.text}</Tag>; } },
    { title: '操作', render: (_: any, record: StudentDormitory) => (
        <Space>
          <Button type="link" size="small" onClick={() => handleEdit(record)}>编辑</Button>
          <Popconfirm title="确定删除?" onConfirm={() => handleDelete(record.id!)}>
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
          <Input placeholder="学生姓名" allowClear style={{ width: 120 }} onChange={(e) => setFilters((f) => ({ ...f, studentName: e.target.value || undefined }))} />
          <Input placeholder="学号" allowClear style={{ width: 120 }} onChange={(e) => setFilters((f) => ({ ...f, studentNo: e.target.value || undefined }))} />
          <Select placeholder="宿舍号" allowClear style={{ width: 160 }} onChange={(v) => setFilters((f) => ({ ...f, dormitoryNo: v || undefined }))}
            options={dormitoryOptions} />
          <Select placeholder="缴费状态" allowClear style={{ width: 120 }} onChange={(v) => setFilters((f) => ({ ...f, paymentStatus: v || undefined }))}>
            <Select.Option value="PAID">已缴</Select.Option><Select.Option value="UNPAID">未缴</Select.Option><Select.Option value="OVERDUE">逾期</Select.Option>
          </Select>
          <Button type="primary" icon={<SearchOutlined />} onClick={fetchData}>搜索</Button>
        </Space>
      </div>
      <Space style={{ marginBottom: 16 }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>新增学生</Button>
        <Upload accept=".xlsx,.xls" showUploadList={false} beforeUpload={handleImport}><Button icon={<UploadOutlined />}>批量导入</Button></Upload>
        <Button icon={<DownloadOutlined />} onClick={handleExport}>导出Excel</Button>
      </Space>
      <Table rowKey="id" columns={columns} dataSource={data} loading={loading} pagination={{ current: page, pageSize, total, onChange: (p, ps) => { setPage(p); setPageSize(ps); } }} />
      <Modal title={editingId ? '编辑学生' : '新增学生'} open={modalVisible} onOk={handleSubmit} onCancel={() => setModalVisible(false)}>
        <Alert variant="warning" appearance="light" size="sm" className="mb-4">
          <AlertIcon><AlertTriangle className="size-3.5" /></AlertIcon>
          <AlertTitle>带 * 的字段为必填项，请完整填写后提交</AlertTitle>
        </Alert>
        <Form form={form} layout="vertical">
          <Form.Item name="studentName" label="姓名" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="studentNo" label="学号" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="dormitoryNo" label="宿舍号" rules={[{ required: true, message: '请选择宿舍' }]}>
            <Select showSearch placeholder="选择宿舍" options={dormitoryOptions} filterOption={(input, option) => (option?.label as string || '').includes(input)} />
          </Form.Item>
          <Form.Item name="phone" label="联系电话"><Input /></Form.Item>
          <Form.Item name="checkInDate" label="入住时间"><DatePicker style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="paymentStatus" label="缴费状态">
            <Select><Select.Option value="UNPAID">未缴</Select.Option><Select.Option value="PAID">已缴</Select.Option><Select.Option value="OVERDUE">逾期</Select.Option></Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default Student;
