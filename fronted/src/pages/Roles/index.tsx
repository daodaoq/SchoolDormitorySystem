import React, { useEffect, useState } from 'react';
import { Table, Button, Space, Modal, Form, Input, Tree, message, Popconfirm, Tag } from 'antd';
import { PlusOutlined, SettingOutlined } from '@ant-design/icons';
import { AlertTriangle } from 'lucide-react';
import request from '../../utils/request';
import { Alert, AlertIcon, AlertTitle } from '@/components/ui/alert-1';
import type { RoleItem, MenuItem, ApiResult } from '../../types';

const getRoles = () => request.get<any, ApiResult<RoleItem[]>>('/roles');
const addRole = (data: any) => request.post<any, ApiResult>('/roles', data);
const updateRole = (id: number, data: any) => request.put<any, ApiResult>(`/roles/${id}`, data);
const deleteRole = (id: number) => request.delete<any, ApiResult>(`/roles/${id}`);
const getRoleMenus = (id: number) => request.get<any, ApiResult<{ menus: MenuItem[]; checkedKeys: number[] }>>(`/roles/${id}/menus`);
const assignRoleMenus = (id: number, menuIds: number[]) => request.post<any, ApiResult>(`/roles/${id}/assign`, { menuIds });

const Roles: React.FC = () => {
  const [data, setData] = useState<RoleItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [permVisible, setPermVisible] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form] = Form.useForm();
  const [menuTree, setMenuTree] = useState<MenuItem[]>([]);
  const [checkedKeys, setCheckedKeys] = useState<number[]>([]);
  const [currentRoleId, setCurrentRoleId] = useState<number | null>(null);

  useEffect(() => { fetchData(); }, []);

  const fetchData = async () => {
    setLoading(true);
    try { const res = await getRoles(); setData(res.data); } catch { message.error('加载角色数据失败'); }
    setLoading(false);
  };

  const handleAdd = () => { setEditingId(null); form.resetFields(); setModalVisible(true); };
  const handleEdit = (r: RoleItem) => { setEditingId(r.id); form.setFieldsValue(r); setModalVisible(true); };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      if (editingId) { await updateRole(editingId, values); message.success('更新成功'); }
      else { await addRole(values); message.success('添加成功'); }
      setModalVisible(false); fetchData();
    } catch (e: any) { if (!e?.errorFields) message.error('操作失败，请稍后重试'); }
  };

  const openPermission = async (roleId: number) => {
    setCurrentRoleId(roleId);
    try {
      const res = await getRoleMenus(roleId);
      setMenuTree(res.data.menus);
      setCheckedKeys(res.data.checkedKeys);
    } catch { message.error('加载权限失败'); }
    setPermVisible(true);
  };

  const handleAssign = async () => {
    if (currentRoleId == null) return;
    await assignRoleMenus(currentRoleId, checkedKeys as number[]);
    message.success('权限分配成功');
    setPermVisible(false);
  };

  const columns = [
    { title: '角色编码', dataIndex: 'roleCode' },
    { title: '角色名称', dataIndex: 'roleName' },
    { title: '描述', dataIndex: 'description' },
    { title: '状态', dataIndex: 'status', render: (s: string) => <Tag color={s === 'ACTIVE' ? 'green' : 'red'}>{s === 'ACTIVE' ? '启用' : '禁用'}</Tag> },
    {
      title: '操作', width: 260,
      render: (_: any, record: RoleItem) => (
        <Space>
          <Button type="link" size="small" onClick={() => handleEdit(record)}>编辑</Button>
          <Button type="link" size="small" icon={<SettingOutlined />} onClick={() => openPermission(record.id)}>分配权限</Button>
          <Popconfirm title="确定删除?" onConfirm={() => deleteRole(record.id).then(() => fetchData())}>
            <Button type="link" size="small" danger>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd} style={{ marginBottom: 16 }}>新增角色</Button>
      <Table rowKey="id" columns={columns} dataSource={data} loading={loading} pagination={false} />

      <Modal title={editingId ? '编辑角色' : '新增角色'} open={modalVisible} onOk={handleSubmit} onCancel={() => setModalVisible(false)} okText="确定" cancelText="取消">
        <Alert variant="warning" appearance="light" size="sm" className="mb-4">
          <AlertIcon><AlertTriangle className="size-3.5" /></AlertIcon>
          <AlertTitle>带 * 的字段为必填项，请完整填写后提交</AlertTitle>
        </Alert>
        <Form form={form} layout="vertical">
          <Form.Item name="roleCode" label="角色编码" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="roleName" label="角色名称" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="description" label="描述"><Input.TextArea rows={2} /></Form.Item>
        </Form>
      </Modal>

      <Modal title="分配权限" open={permVisible} onOk={handleAssign} onCancel={() => setPermVisible(false)} okText="确定" cancelText="取消" width={500}>
        <Tree
          checkable defaultExpandAll
          fieldNames={{ title: 'menuName', key: 'id', children: 'children' }}
          treeData={menuTree as any}
          checkedKeys={checkedKeys}
          onCheck={(keys) => setCheckedKeys(keys as number[])}
        />
      </Modal>
    </div>
  );
};

export default Roles;
