import React, { useEffect, useState } from 'react';
import { Table, Button, Space, Modal, Form, Input, Select, DatePicker, Upload, message, Popconfirm, Tag } from 'antd';
import { PlusOutlined, UploadOutlined, DownloadOutlined, SearchOutlined } from '@ant-design/icons';
import { getStudents, addStudent, updateStudent, deleteStudent, importStudents, exportStudents } from '../../services/api';
import dayjs from 'dayjs';

function Student() {
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
      const res = await getStudents({ page, pageSize, ...filters });
      setData(res.data.records);
      setTotal(res.data.total);
    } catch (e) { console.error(e); }
    setLoading(false);
  };

  const handleAdd = () => {
    setEditingId(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = (record) => {
    setEditingId(record.id);
    form.setFieldsValue({ ...record, checkInDate: record.checkInDate ? dayjs(record.checkInDate) : null });
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
    } catch (e) { console.error(e); }
  };

  const handleDelete = async (id) => {
    await deleteStudent(id);
    message.success('删除成功');
    fetchData();
  };

  const handleImport = async (file) => {
    try {
      const res = await importStudents(file);
      message.success(res.message);
      fetchData();
    } catch (e) { console.error(e); }
    return false;
  };

  const handleExport = async () => {
    try {
      const res = await exportStudents(filters);
      const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url; a.download = '学生信息.xlsx'; a.click();
      window.URL.revokeObjectURL(url);
    } catch (e) { console.error(e); }
  };

  const statusMap = { PAID: { color: 'green', text: '已缴' }, UNPAID: { color: 'orange', text: '未缴' }, OVERDUE: { color: 'red', text: '逾期' } };

  const columns = [
    { title: '姓名', dataIndex: 'studentName', key: 'studentName' },
    { title: '学号', dataIndex: 'studentNo', key: 'studentNo' },
    { title: '宿舍号', dataIndex: 'dormitoryNo', key: 'dormitoryNo' },
    { title: '联系电话', dataIndex: 'phone', key: 'phone' },
    { title: '入住时间', dataIndex: 'checkInDate', key: 'checkInDate' },
    { title: '缴费状态', dataIndex: 'paymentStatus', key: 'paymentStatus',
      render: (status) => {
        const s = statusMap[status] || { color: 'default', text: status };
        return <Tag color={s.color}>{s.text}</Tag>;
      }
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
          <Input placeholder="学生姓名" allowClear style={{ width: 120 }}
            onChange={(e) => setFilters((f) => ({ ...f, studentName: e.target.value || undefined }))} />
          <Input placeholder="学号" allowClear style={{ width: 120 }}
            onChange={(e) => setFilters((f) => ({ ...f, studentNo: e.target.value || undefined }))} />
          <Input placeholder="宿舍号" allowClear style={{ width: 120 }}
            onChange={(e) => setFilters((f) => ({ ...f, dormitoryNo: e.target.value || undefined }))} />
          <Select placeholder="缴费状态" allowClear style={{ width: 120 }}
            onChange={(v) => setFilters((f) => ({ ...f, paymentStatus: v || undefined }))}>
            <Select.Option value="PAID">已缴</Select.Option>
            <Select.Option value="UNPAID">未缴</Select.Option>
            <Select.Option value="OVERDUE">逾期</Select.Option>
          </Select>
          <Button type="primary" icon={<SearchOutlined />} onClick={fetchData}>搜索</Button>
        </Space>
      </div>
      <Space style={{ marginBottom: 16 }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>新增学生</Button>
        <Upload accept=".xlsx,.xls" showUploadList={false} beforeUpload={handleImport}>
          <Button icon={<UploadOutlined />}>批量导入</Button>
        </Upload>
        <Button icon={<DownloadOutlined />} onClick={handleExport}>导出Excel</Button>
      </Space>
      <Table rowKey="id" columns={columns} dataSource={data} loading={loading}
        pagination={{ current: page, pageSize, total, onChange: (p, ps) => { setPage(p); setPageSize(ps); } }} />
      <Modal title={editingId ? '编辑学生' : '新增学生'} open={modalVisible} onOk={handleSubmit} onCancel={() => setModalVisible(false)}>
        <Form form={form} layout="vertical">
          <Form.Item name="studentName" label="姓名" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="studentNo" label="学号" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="dormitoryNo" label="宿舍号" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="phone" label="联系电话"><Input /></Form.Item>
          <Form.Item name="checkInDate" label="入住时间"><DatePicker style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="paymentStatus" label="缴费状态">
            <Select><Select.Option value="UNPAID">未缴</Select.Option><Select.Option value="PAID">已缴</Select.Option><Select.Option value="OVERDUE">逾期</Select.Option></Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

export default Student;
