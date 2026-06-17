import React, { useEffect, useState } from 'react';
import { Table, Button, Space, Modal, Form, Input, Select, message, Popconfirm, Tag } from 'antd';
import { PlusOutlined, KeyOutlined } from '@ant-design/icons';
import { AlertTriangle } from 'lucide-react';
import { getUsers, addUser, updateUser, deleteUser, resetUserPassword } from '../../services/api';
import { Alert, AlertIcon, AlertTitle } from '@/components/ui/alert-1';

interface UserRecord {
  id: number;
  username: string;
  realName?: string;
  role: string;
  status: string;
  createTime: string;
}

const roleMap: Record<string, string> = { ADMIN: '管理员', USER: '普通用户' };

const Users: React.FC = () => {
  const [data, setData] = useState<UserRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form] = Form.useForm();

  useEffect(() => { fetchData(); }, [page, pageSize]);

  const fetchData = async () => {
    setLoading(true);
    try { const res = await getUsers({ page, pageSize }); setData(res.data.records); setTotal(res.data.total); } catch { /* handled */ }
    setLoading(false);
  };

  const handleAdd = () => { setEditingId(null); form.resetFields(); form.setFieldsValue({ role: 'USER' }); setModalVisible(true); };
  const handleEdit = (record: UserRecord) => { setEditingId(record.id); form.setFieldsValue({ realName: record.realName, role: record.role, status: record.status }); setModalVisible(true); };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      if (editingId) {
        await updateUser(editingId, values);
        message.success('更新成功');
      } else {
        await addUser(values);
        message.success('添加成功');
      }
      setModalVisible(false);
      fetchData();
    } catch { /* handled */ }
  };

  const handleDelete = async (id: number) => { await deleteUser(id); message.success('删除成功'); fetchData(); };
  const handleResetPwd = async (id: number) => { await resetUserPassword(id); message.success('密码已重置为 123456'); };

  const columns = [
    { title: '用户名', dataIndex: 'username' },
    { title: '真实姓名', dataIndex: 'realName' },
    { title: '角色', dataIndex: 'role', render: (v: string) => <Tag color={v === 'ADMIN' ? 'red' : 'blue'}>{roleMap[v] || v}</Tag> },
    { title: '状态', dataIndex: 'status', render: (s: string) => <Tag color={s === 'ACTIVE' ? 'green' : 'red'}>{s === 'ACTIVE' ? '正常' : '禁用'}</Tag> },
    { title: '创建时间', dataIndex: 'createTime' },
    {
      title: '操作', width: 220,
      render: (_: any, record: UserRecord) => (
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
      <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd} style={{ marginBottom: 16 }}>新增用户</Button>
      <Table rowKey="id" columns={columns} dataSource={data} loading={loading} pagination={{ current: page, pageSize, total, onChange: (p, ps) => { setPage(p); setPageSize(ps); } }} />
      <Modal title={editingId ? '编辑用户' : '新增用户'} open={modalVisible} onOk={handleSubmit} onCancel={() => setModalVisible(false)}>
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
            <Select>{Object.entries(roleMap).map(([k, v]) => <Select.Option key={k} value={k}>{v}</Select.Option>)}</Select>
          </Form.Item>
          {editingId && (
            <>
              <Form.Item name="status" label="状态">
                <Select><Select.Option value="ACTIVE">正常</Select.Option><Select.Option value="DISABLED">禁用</Select.Option></Select>
              </Form.Item>
              <Form.Item name="password" label="新密码（留空不修改）"><Input.Password placeholder="留空则不修改密码" /></Form.Item>
            </>
          )}
        </Form>
      </Modal>
    </div>
  );
};

export default Users;
