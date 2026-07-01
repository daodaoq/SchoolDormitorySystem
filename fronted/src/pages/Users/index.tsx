import React, { useEffect, useState } from 'react';
import { Table, Button, Space, Modal, Form, Input, Select, message, Popconfirm, Tag, Avatar, Upload } from 'antd';
import { PlusOutlined, KeyOutlined, UserOutlined, UploadOutlined } from '@ant-design/icons';
import { AlertTriangle } from 'lucide-react';
import { getUsers, addUser, updateUser, deleteUser, resetUserPassword } from '../../services/api';
import type { UserInfo } from '../../types';
import { Alert, AlertIcon, AlertTitle } from '@/components/ui/alert-1';
import request from '../../utils/request';

const roleMap: Record<string, { color: string; label: string }> = {
  ADMIN: { color: 'red', label: '管理员' },
  TEACHER: { color: 'purple', label: '教师/宿管' },
  STUDENT: { color: 'blue', label: '学生' },
};

const Users: React.FC = () => {
  const [data, setData] = useState<UserInfo[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [avatarUrl, setAvatarUrl] = useState<string>('');
  const [uploading, setUploading] = useState(false);
  const [roleFilter, setRoleFilter] = useState<string | undefined>(undefined);
  // 这个 form 实例相当于一个控制器，让你可以在组件中主动操控表单，而不是被动等待用户输入
  const [form] = Form.useForm();

  useEffect(() => { fetchData(); }, [page, pageSize, roleFilter]);

  const fetchData = async () => {
    setLoading(true);
    try {
      const res = await getUsers({ page, pageSize, role: roleFilter });
      setData(res.data.records);
      setTotal(res.data.total);
    } catch (err) { console.error(err); message.error('加载用户数据失败'); }
    setLoading(false);
  };

  const handleAdd = () => {
    setEditingId(null);
    setAvatarUrl('');
    form.resetFields();
    form.setFieldsValue({ role: 'STUDENT' });
    setModalVisible(true);
  };

  const handleEdit = (record: UserInfo) => {
    setEditingId(record.id);
    setAvatarUrl(record.avatar || '');
    form.setFieldsValue({ realName: record.realName, role: record.role, status: record.status });
    setModalVisible(true);
  };

  const handleAvatarUpload = async (file: File) => {
    if (!editingId) { message.warning('请先保存用户后再上传头像'); return false; }
    setUploading(true);
    try {
      const fd = new FormData();
      fd.append('file', file);
      const res = await request.post<any, any>(`/users/${editingId}/avatar`, fd, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      if (res.code === 200) {
        setAvatarUrl(res.data.avatar);
        message.success('头像上传成功');
      }
    } catch (err) { console.error(err); message.error('上传失败'); }
    setUploading(false);
    return false;
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      if (editingId) {
        await updateUser(editingId, { ...values, avatar: avatarUrl });
        message.success('更新成功');
      } else {
        await addUser(values);
        message.success('添加成功');
      }
      setModalVisible(false);
      fetchData();
    } catch (e: any) { if (!e?.errorFields) message.error('操作失败，请稍后重试'); }
  };

  const handleDelete = async (id: number) => { await deleteUser(id); message.success('删除成功'); fetchData(); };
  const handleResetPwd = async (id: number) => { await resetUserPassword(id); message.success('密码已重置为 123456'); };

  const columns = [
    {
      title: '头像', dataIndex: 'avatar', width: 64,
      render: (url: string, record: UserInfo) => (
        <Avatar src={url || undefined} icon={!url ? <UserOutlined /> : undefined}
          size={36} style={{ background: url ? 'transparent' : 'var(--coral)', color: '#fff' }} />
      ),
    },
    { title: '用户名', dataIndex: 'username' },
    { title: '真实姓名', dataIndex: 'realName', render: (v: string) => v || '-' },
    {
      title: '角色', dataIndex: 'role',
      render: (v: string) => {
        const r = roleMap[v] || { color: 'default', label: v };
        return <Tag color={r.color}>{r.label}</Tag>;
      },
    },
    {
      title: '状态', dataIndex: 'status',
      render: (s: string) => <Tag color={s === 'ACTIVE' ? 'green' : 'red'}>{s === 'ACTIVE' ? '正常' : '禁用'}</Tag>,
    },
    { title: '创建时间', dataIndex: 'createTime', render: (v: string) => v ? v.replace('T', ' ') : '-' },
    {
      title: '操作', width: 260,
      render: (_: any, record: UserInfo) => (
        <Space>
          <Button type="link" size="small" onClick={() => handleEdit(record)}>编辑</Button>
          <Popconfirm title="确定重置密码为123456?" onConfirm={() => handleResetPwd(record.id)}>
            <Button type="link" size="small" icon={<KeyOutlined />}>重置密码</Button>
          </Popconfirm>
          {record.username !== 'admin' && (
            <Popconfirm title="确定删除此用户?" onConfirm={() => handleDelete(record.id)}>
              <Button type="link" size="small" danger>删除</Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <div className="search-form" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Select placeholder="角色筛选" allowClear style={{ width: 140 }} value={roleFilter} onChange={setRoleFilter}>
          {Object.entries(roleMap).map(([k, v]) => <Select.Option key={k} value={k}>{v.label}</Select.Option>)}
        </Select>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>新增用户</Button>
      </div>
      <Table rowKey="id" columns={columns} dataSource={data} loading={loading}
        pagination={{ current: page, pageSize, total, onChange: (p, ps) => { setPage(p); setPageSize(ps); } }} />

      <Modal title={editingId ? '编辑用户' : '新增用户'} open={modalVisible}
        onOk={handleSubmit} onCancel={() => setModalVisible(false)} okText="确定" cancelText="取消" width={480}>
        <Alert variant="warning" appearance="light" size="sm" className="mb-4">
          <AlertIcon><AlertTriangle className="size-3.5" /></AlertIcon>
          <AlertTitle>带 * 的字段为必填项，请完整填写后提交</AlertTitle>
        </Alert>
        <Form form={form} layout="vertical">
          {!editingId && (
            <>
              <Form.Item name="username" label="用户名" rules={[{ required: true }]}><Input /></Form.Item>
              <Form.Item name="password" label="密码" rules={[{ required: true }]}><Input.Password /></Form.Item>
            </>
          )}
          <Form.Item name="realName" label="真实姓名"><Input /></Form.Item>
          <Form.Item name="role" label="角色">
            <Select>
              {Object.entries(roleMap).map(([k, v]) => <Select.Option key={k} value={k}>{v.label}</Select.Option>)}
            </Select>
          </Form.Item>
          {editingId && (
            <>
              <Form.Item name="status" label="状态">
                <Select>
                  <Select.Option value="ACTIVE">正常</Select.Option>
                  <Select.Option value="DISABLED">禁用</Select.Option>
                </Select>
              </Form.Item>
              <Form.Item label="头像">
                <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
                  <Avatar src={avatarUrl || undefined} icon={!avatarUrl ? <UserOutlined /> : undefined}
                    size={56} style={{ background: avatarUrl ? 'transparent' : 'var(--coral)', color: '#fff' }} />
                  <Upload showUploadList={false} beforeUpload={handleAvatarUpload} accept="image/*">
                    <Button icon={<UploadOutlined />} loading={uploading}>上传头像</Button>
                  </Upload>
                </div>
              </Form.Item>
              <Form.Item name="password" label="新密码（留空不修改）">
                <Input.Password placeholder="留空则不修改密码" />
              </Form.Item>
            </>
          )}
        </Form>
      </Modal>
    </div>
  );
};

export default Users;
