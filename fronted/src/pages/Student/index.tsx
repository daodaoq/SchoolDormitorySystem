import React, { useEffect, useState } from 'react';
import { Table, Button, Space, Modal, Form, Input, Select, DatePicker, Upload, message, Popconfirm, Avatar } from 'antd';
import { PlusOutlined, UploadOutlined, DownloadOutlined, SearchOutlined, UserOutlined } from '@ant-design/icons';
import { AlertTriangle } from 'lucide-react';
import { getStudents, addStudent, updateStudent, deleteStudent, importStudents, exportStudents, getActiveDormitories } from '../../services/api';
import { Alert, AlertIcon, AlertTitle } from '@/components/ui/alert-1';
import request from '../../utils/request';
import type { StudentDormitory } from '../../types';
import dayjs from 'dayjs';

const Student: React.FC = () => {
  const [data, setData] = useState<StudentDormitory[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editingPhoto, setEditingPhoto] = useState<string>('');
  const [uploading, setUploading] = useState(false);
  const [form] = Form.useForm();
  const [filters, setFilters] = useState<Record<string, string | undefined>>({});
  const [dormitoryOptions, setDormitoryOptions] = useState<{ label: string; value: string }[]>([]);

  useEffect(() => { fetchData(); }, [page, pageSize, filters]);

  const loadDormitoryOptions = async () => {
    try {
      const res = await getActiveDormitories();
      const list = res.data || [];
      setDormitoryOptions(list.map((d: any) => ({
        label: d.dormitoryNo + (d.building ? ' (' + d.building + ')' : ''),
        value: d.dormitoryNo,
      })));
    } catch {
      message.error('加载宿舍列表失败，请刷新重试');
    }
  };

  useEffect(() => { loadDormitoryOptions(); }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      const res = await getStudents({ page, pageSize, ...filters });
      setData(res.data.records);
      setTotal(res.data.total);
    } catch { message.error('加载学生数据失败'); }
    setLoading(false);
  };

  const handleAdd = async () => {
    setEditingId(null); setEditingPhoto('');
    form.resetFields();
    if (dormitoryOptions.length === 0) await loadDormitoryOptions();
    setModalVisible(true);
  };
  const handleEdit = async (record: StudentDormitory) => {
    setEditingId(record.id!);
    setEditingPhoto(record.photo || '');
    form.setFieldsValue({ ...record, checkInDate: record.checkInDate ? dayjs(record.checkInDate) : null });
    if (dormitoryOptions.length === 0) await loadDormitoryOptions();
    setModalVisible(true);
  };

  const handlePhotoUpload = async (file: File) => {
    if (!editingId) { message.warning('请先保存学生后再上传照片'); return false; }
    setUploading(true);
    try {
      const fd = new FormData();
      fd.append('file', file);
      const res = await request.post<any, any>(`/students/${editingId}/photo`, fd, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      if (res.code === 200) {
        setEditingPhoto(res.data.photo);
        message.success('照片上传成功');
      }
    } catch { message.error('上传失败'); }
    setUploading(false);
    return false;
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
    } catch (e: any) {
      // Ant Design 表单校验失败会显示红色提示，不需要额外处理
      if (e?.errorFields) return;
      message.error('操作失败，请稍后重试');
    }
  };

  const handleDelete = async (id: number) => { await deleteStudent(id); message.success('删除成功'); fetchData(); };
  const handleImport = async (file: File) => { try { const res = await importStudents(file); message.success(res.message); fetchData(); } catch { message.error('导入失败，请检查文件格式'); } return false; };
  const handleExport = async () => {
    try {
      const res: any = await exportStudents(filters);
      const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a'); a.href = url; a.download = '学生信息.xlsx'; a.click();
      window.URL.revokeObjectURL(url);
    } catch { message.error('导出失败'); }
  };

  const columns = [
    {
      title: '照片', dataIndex: 'photo', width: 64,
      render: (url: string) => (
        <Avatar src={url || undefined} icon={!url ? <UserOutlined /> : undefined}
          size={36} style={{ background: url ? 'transparent' : '#E85D4E', color: '#fff' }} />
      ),
    },
    { title: '姓名', dataIndex: 'studentName' },
    { title: '学号', dataIndex: 'studentNo' },
    { title: '宿舍号', dataIndex: 'dormitoryNo' },
    { title: '联系电话', dataIndex: 'phone', render: (v: string) => v || '-' },
    { title: '入住时间', dataIndex: 'checkInDate' },
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
          <Button type="primary" icon={<SearchOutlined />} onClick={fetchData}>搜索</Button>
        </Space>
      </div>
      <Space style={{ marginBottom: 16 }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>新增学生</Button>
        <Upload accept=".xlsx,.xls" showUploadList={false} beforeUpload={handleImport}><Button icon={<DownloadOutlined />}>批量导入</Button></Upload>
        <Button icon={<UploadOutlined />} onClick={handleExport}>导出Excel</Button>
      </Space>
      <Table rowKey="id" columns={columns} dataSource={data} loading={loading} pagination={{ current: page, pageSize, total, onChange: (p, ps) => { setPage(p); setPageSize(ps); } }} />
      <Modal title={editingId ? '编辑学生' : '新增学生'} open={modalVisible} onOk={handleSubmit} onCancel={() => setModalVisible(false)} okText="确定" cancelText="取消">
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
          {editingId && (
            <Form.Item label="照片">
              <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
                <Avatar src={editingPhoto || undefined} icon={!editingPhoto ? <UserOutlined /> : undefined}
                  size={56} style={{ background: editingPhoto ? 'transparent' : '#E85D4E', color: '#fff' }} />
                <Upload showUploadList={false} beforeUpload={handlePhotoUpload} accept="image/*">
                  <Button icon={<UploadOutlined />} loading={uploading}>上传照片</Button>
                </Upload>
              </div>
            </Form.Item>
          )}
        </Form>
      </Modal>
    </div>
  );
};

export default Student;
