import React, { useEffect, useState } from 'react';
import { Table, Button, Space, Modal, Form, Input, Select, InputNumber, TreeSelect, message, Popconfirm, Tag } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { AlertTriangle } from 'lucide-react';
import { getMenuTree, addMenu, updateMenu, deleteMenu } from '../../services/api';
import { Alert, AlertIcon, AlertTitle } from '@/components/ui/alert-1';

interface MenuRecord {
  id: number;
  parentId: number;
  menuName: string;
  menuType: string;
  path?: string;
  icon?: string;
  permissionCode?: string;
  sortOrder: number;
  visible: boolean;
  children?: MenuRecord[];
}

const menuTypeOptions = [
  { value: 'MENU', label: '菜单' },
  { value: 'PAGE', label: '页面' },
  { value: 'BUTTON', label: '按钮' },
];

const iconOptions = [
  'DashboardOutlined', 'UserOutlined', 'TeamOutlined', 'HomeOutlined',
  'FileTextOutlined', 'DollarOutlined', 'PayCircleOutlined', 'BarChartOutlined',
  'RobotOutlined', 'SettingOutlined', 'SafetyOutlined', 'MenuOutlined',
  'BankOutlined', 'KeyOutlined', 'AppstoreOutlined', 'UnorderedListOutlined',
];

/** 将扁平菜单列表转为 TreeSelect 需要的树形数据 */
function buildTreeData(menus: MenuRecord[]) {
  return menus.map((m) => ({
    title: m.menuName,
    value: m.id,
    children: m.children?.length ? buildTreeData(m.children) : undefined,
  }));
}

const Menus: React.FC = () => {
  const [menuTree, setMenuTree] = useState<MenuRecord[]>([]);
  const [flatList, setFlatList] = useState<MenuRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form] = Form.useForm();

  useEffect(() => { fetchData(); }, []);

  const flatten = (list: MenuRecord[], result: MenuRecord[] = []) => {
    list.forEach((m) => { result.push(m); if (m.children?.length) flatten(m.children, result); });
    return result;
  };

  const fetchData = async () => {
    setLoading(true);
    try { const res = await getMenuTree(); setMenuTree(res.data); setFlatList(flatten(res.data || [])); } catch { /* */ }
    setLoading(false);
  };

  const handleAdd = () => { setEditingId(null); form.resetFields(); form.setFieldsValue({ parentId: 0, menuType: 'PAGE', sortOrder: 0, visible: true }); setModalVisible(true); };
  const handleEdit = (record: MenuRecord) => { setEditingId(record.id); form.setFieldsValue(record); setModalVisible(true); };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      if (editingId) { await updateMenu(editingId, values); message.success('更新成功'); }
      else { await addMenu(values); message.success('添加成功'); }
      setModalVisible(false); fetchData();
    } catch { /* */ }
  };

  const columns = [
    { title: '菜单名称', dataIndex: 'menuName', width: 200 },
    { title: '类型', dataIndex: 'menuType', width: 80, render: (v: string) => <Tag>{menuTypeOptions.find((o) => o.value === v)?.label || v}</Tag> },
    { title: '路由路径', dataIndex: 'path', width: 150 },
    { title: '图标', dataIndex: 'icon', width: 180 },
    { title: '权限标识', dataIndex: 'permissionCode', width: 150 },
    { title: '排序', dataIndex: 'sortOrder', width: 60 },
    { title: '可见', dataIndex: 'visible', width: 60, render: (v: boolean) => v ? '是' : '否' },
    { title: '操作', width: 150, render: (_: any, record: MenuRecord) => (
        <Space>
          <Button type="link" size="small" onClick={() => handleEdit(record)}>编辑</Button>
          <Popconfirm title="确定删除?" onConfirm={() => deleteMenu(record.id).then(() => { message.success('删除成功'); fetchData(); })}>
            <Button type="link" size="small" danger>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd} style={{ marginBottom: 16 }}>新增菜单</Button>
      <Table rowKey="id" columns={columns} dataSource={flatList} loading={loading} pagination={false} />
      <Modal title={editingId ? '编辑菜单' : '新增菜单'} open={modalVisible} onOk={handleSubmit} onCancel={() => setModalVisible(false)}>
        <Alert variant="warning" appearance="light" size="sm" className="mb-4">
          <AlertIcon><AlertTriangle className="size-3.5" /></AlertIcon>
          <AlertTitle>带 * 的字段为必填项，菜单类型为「页面」时需填写路由路径</AlertTitle>
        </Alert>
        <Form form={form} layout="vertical">
          <Form.Item name="parentId" label="上级菜单">
            <TreeSelect treeDefaultExpandAll allowClear placeholder="留空表示顶级菜单"
              treeData={[{ title: '根节点', value: 0, children: buildTreeData(menuTree) }]} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="menuName" label="菜单名称" rules={[{ required: true, message: '请输入菜单名称' }]}><Input /></Form.Item>
          <Form.Item name="menuType" label="菜单类型" rules={[{ required: true }]}>
            <Select options={menuTypeOptions} />
          </Form.Item>
          <Form.Item name="path" label="路由路径"><Input placeholder="如: /students" /></Form.Item>
          <Form.Item name="icon" label="图标">
            <Select allowClear showSearch placeholder="选择图标">
              {iconOptions.map((icon) => <Select.Option key={icon} value={icon}>{icon}</Select.Option>)}
            </Select>
          </Form.Item>
          <Form.Item name="permissionCode" label="权限标识"><Input placeholder="如: student:list" /></Form.Item>
          <Form.Item name="sortOrder" label="排序号"><InputNumber min={0} style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="visible" label="是否可见">
            <Select><Select.Option value={true}>是</Select.Option><Select.Option value={false}>否</Select.Option></Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default Menus;
