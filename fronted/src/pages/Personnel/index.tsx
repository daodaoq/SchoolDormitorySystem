import React, { useEffect, useState } from 'react';
import { Table, Button, Space, Modal, Form, Input, Select, message, Popconfirm, Tag, Tabs } from 'antd';
import { PlusOutlined, KeyOutlined, LinkOutlined, DisconnectOutlined } from '@ant-design/icons';
import { AlertTriangle } from 'lucide-react';
import {
  getUsers, addUser, updateUser, deleteUser, resetUserPassword,
  getStudentPersonnel, linkStudentToUser, unlinkStudentUser,
} from '../../services/api';
import { Alert, AlertIcon, AlertTitle } from '@/components/ui/alert-1';

interface UserRecord {
  id: number;
  username: string;
  realName?: string;
  role: string;
  status: string;
  createTime: string;
}

interface StudentPersonnelRecord {
  id: number;
  studentName: string;
  studentNo: string;
  dormitoryNo: string;
  phone?: string;
  checkInDate?: string;
  paymentStatus: string;
  userId?: number;
  username?: string;
  userStatus?: string;
  createTime?: string;
}

const statusMap: Record<string, { color: string; text: string }> = {
  PAID: { color: 'green', text: '已缴' },
  UNPAID: { color: 'orange', text: '未缴' },
  OVERDUE: { color: 'red', text: '逾期' },
};

const Personnel: React.FC = () => {
  // ===== 宿管管理状态 =====
  const [supervisorData, setSupervisorData] = useState<UserRecord[]>([]);
  const [supervisorLoading, setSupervisorLoading] = useState(false);
  const [supervisorTotal, setSupervisorTotal] = useState(0);
  const [supervisorPage, setSupervisorPage] = useState(1);
  const [supervisorPageSize, setSupervisorPageSize] = useState(10);
  const [supervisorModalVisible, setSupervisorModalVisible] = useState(false);
  const [supervisorEditingId, setSupervisorEditingId] = useState<number | null>(null);
  const [supervisorForm] = Form.useForm();

  // ===== 学生管理状态 =====
  const [studentData, setStudentData] = useState<StudentPersonnelRecord[]>([]);
  const [studentLoading, setStudentLoading] = useState(false);
  const [studentTotal, setStudentTotal] = useState(0);
  const [studentPage, setStudentPage] = useState(1);
  const [studentPageSize, setStudentPageSize] = useState(10);
  const [linkFilter, setLinkFilter] = useState<boolean | undefined>(undefined);
  const [createAccountModalVisible, setCreateAccountModalVisible] = useState(false);
  const [linkExistingModalVisible, setLinkExistingModalVisible] = useState(false);
  const [currentStudentId, setCurrentStudentId] = useState<number | null>(null);
  const [createAccountForm] = Form.useForm();
  const [linkExistingForm] = Form.useForm();
  // 搜索已有学生账号
  const [studentUserSearch, setStudentUserSearch] = useState<UserRecord[]>([]);
  const [studentUserSearchLoading, setStudentUserSearchLoading] = useState(false);

  useEffect(() => { fetchSupervisors(); }, [supervisorPage, supervisorPageSize]);
  useEffect(() => { fetchStudents(); }, [studentPage, studentPageSize, linkFilter]);

  // ========== 宿管管理 ==========
  const fetchSupervisors = async () => {
    setSupervisorLoading(true);
    try {
      const res = await getUsers({ page: supervisorPage, pageSize: supervisorPageSize, role: 'TEACHER' });
      setSupervisorData(res.data.records);
      setSupervisorTotal(res.data.total);
    } catch { /* handled */ }
    setSupervisorLoading(false);
  };

  const handleAddSupervisor = () => {
    setSupervisorEditingId(null);
    supervisorForm.resetFields();
    setSupervisorModalVisible(true);
  };

  const handleEditSupervisor = (record: UserRecord) => {
    setSupervisorEditingId(record.id);
    supervisorForm.setFieldsValue({ realName: record.realName, status: record.status });
    setSupervisorModalVisible(true);
  };

  const handleSubmitSupervisor = async () => {
    try {
      const values = await supervisorForm.validateFields();
      if (supervisorEditingId) {
        await updateUser(supervisorEditingId, values);
        message.success('更新成功');
      } else {
        await addUser({ ...values, role: 'TEACHER' });
        message.success('添加成功');
      }
      setSupervisorModalVisible(false);
      fetchSupervisors();
    } catch { /* handled */ }
  };

  const handleDeleteSupervisor = async (id: number) => {
    await deleteUser(id);
    message.success('删除成功');
    fetchSupervisors();
  };

  const handleResetPwd = async (id: number) => {
    await resetUserPassword(id);
    message.success('密码已重置为 123456');
  };

  // ========== 学生账号管理 ==========
  const fetchStudents = async () => {
    setStudentLoading(true);
    try {
      const res = await getStudentPersonnel({
        page: studentPage,
        pageSize: studentPageSize,
        linked: linkFilter,
      });
      setStudentData(res.data.records);
      setStudentTotal(res.data.total);
    } catch { /* handled */ }
    setStudentLoading(false);
  };

  const handleCreateAccount = (studentId: number) => {
    setCurrentStudentId(studentId);
    createAccountForm.resetFields();
    setCreateAccountModalVisible(true);
  };

  const handleSubmitCreateAccount = async () => {
    try {
      const values = await createAccountForm.validateFields();
      // 1. 创建 STUDENT 用户
      const userRes = await addUser({ ...values, role: 'STUDENT' });
      const newUserId = userRes.data?.id;
      if (!newUserId) { message.error('创建用户失败'); return; }
      // 2. 关联到学生记录
      await linkStudentToUser(currentStudentId!, newUserId);
      message.success('账号创建并关联成功');
      setCreateAccountModalVisible(false);
      fetchStudents();
    } catch { /* handled */ }
  };

  const handleOpenLinkExisting = async (studentId: number) => {
    setCurrentStudentId(studentId);
    linkExistingForm.resetFields();
    setStudentUserSearch([]);
    setLinkExistingModalVisible(true);
  };

  const handleSearchStudentUsers = async (username: string) => {
    if (!username) { setStudentUserSearch([]); return; }
    setStudentUserSearchLoading(true);
    try {
      const res = await getUsers({ page: 1, pageSize: 20, role: 'STUDENT', username });
      setStudentUserSearch(res.data.records || []);
    } catch { /* handled */ }
    setStudentUserSearchLoading(false);
  };

  const handleSubmitLinkExisting = async () => {
    try {
      const values = await linkExistingForm.validateFields();
      await linkStudentToUser(currentStudentId!, values.userId);
      message.success('关联成功');
      setLinkExistingModalVisible(false);
      fetchStudents();
    } catch { /* handled */ }
  };

  const handleUnlink = async (studentId: number) => {
    await unlinkStudentUser(studentId);
    message.success('已取消关联');
    fetchStudents();
  };

  const handleStudentResetPwd = async (userId: number) => {
    await resetUserPassword(userId);
    message.success('密码已重置为 123456');
  };

  // ========== 宿管表格列 ==========
  const supervisorColumns = [
    { title: '用户名', dataIndex: 'username' },
    { title: '真实姓名', dataIndex: 'realName' },
    { title: '角色', render: () => <Tag color="purple">教师/宿管</Tag> },
    {
      title: '状态', dataIndex: 'status',
      render: (s: string) => <Tag color={s === 'ACTIVE' ? 'green' : 'red'}>{s === 'ACTIVE' ? '正常' : '禁用'}</Tag>,
    },
    { title: '创建时间', dataIndex: 'createTime' },
    {
      title: '操作', width: 220,
      render: (_: any, record: UserRecord) => (
        <Space>
          <Button type="link" size="small" onClick={() => handleEditSupervisor(record)}>编辑</Button>
          <Popconfirm title="确定重置密码为123456?" onConfirm={() => handleResetPwd(record.id)}>
            <Button type="link" size="small" icon={<KeyOutlined />}>重置密码</Button>
          </Popconfirm>
          <Popconfirm title="确定删除此宿管?" onConfirm={() => handleDeleteSupervisor(record.id)}>
            <Button type="link" size="small" danger>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  // ========== 学生表格列 ==========
  const studentColumns = [
    { title: '学号', dataIndex: 'studentNo' },
    { title: '姓名', dataIndex: 'studentName' },
    {
      title: '关联账号', dataIndex: 'username',
      render: (v: string | undefined, record: StudentPersonnelRecord) =>
        v ? <Tag color="blue">{v}</Tag> : <Tag color="default">未关联</Tag>,
    },
    { title: '宿舍号', dataIndex: 'dormitoryNo' },
    {
      title: '缴费状态', dataIndex: 'paymentStatus',
      render: (s: string) => {
        const st = statusMap[s] || { color: 'default', text: s };
        return <Tag color={st.color}>{st.text}</Tag>;
      },
    },
    {
      title: '操作', width: 260,
      render: (_: any, record: StudentPersonnelRecord) => (
        <Space>
          {record.userId ? (
            <>
              <Popconfirm title="确定取消关联?" onConfirm={() => handleUnlink(record.id)}>
                <Button type="link" size="small" icon={<DisconnectOutlined />} danger>取消关联</Button>
              </Popconfirm>
              <Popconfirm title="确定重置密码为123456?" onConfirm={() => handleStudentResetPwd(record.userId!)}>
                <Button type="link" size="small" icon={<KeyOutlined />}>重置密码</Button>
              </Popconfirm>
            </>
          ) : (
            <>
              <Button type="link" size="small" icon={<PlusOutlined />} onClick={() => handleCreateAccount(record.id)}>
                创建账号
              </Button>
              <Button type="link" size="small" icon={<LinkOutlined />} onClick={() => handleOpenLinkExisting(record.id)}>
                关联已有
              </Button>
            </>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Tabs
        defaultActiveKey="supervisor"
        items={[
          {
            key: 'supervisor',
            label: '宿管管理',
            children: (
              <>
                <Button type="primary" icon={<PlusOutlined />} onClick={handleAddSupervisor} style={{ marginBottom: 16 }}>
                  新增宿管
                </Button>
                <Table
                  rowKey="id"
                  columns={supervisorColumns}
                  dataSource={supervisorData}
                  loading={supervisorLoading}
                  pagination={{
                    current: supervisorPage, pageSize: supervisorPageSize, total: supervisorTotal,
                    onChange: (p, ps) => { setSupervisorPage(p); setSupervisorPageSize(ps); },
                  }}
                />
                {/* 新增/编辑宿管 Modal */}
                <Modal
                  title={supervisorEditingId ? '编辑宿管' : '新增宿管'}
                  open={supervisorModalVisible}
                  onOk={handleSubmitSupervisor}
                  onCancel={() => setSupervisorModalVisible(false)}
                >
                  <Alert variant="warning" appearance="light" size="sm" className="mb-4">
                    <AlertIcon><AlertTriangle className="size-3.5" /></AlertIcon>
                    <AlertTitle>带 * 的字段为必填项，请完整填写后提交</AlertTitle>
                  </Alert>
                  <Form form={supervisorForm} layout="vertical">
                    {!supervisorEditingId && (
                      <>
                        <Form.Item name="username" label="用户名" rules={[{ required: true }]}><Input /></Form.Item>
                        <Form.Item name="password" label="密码" rules={[{ required: true }]}><Input.Password /></Form.Item>
                      </>
                    )}
                    <Form.Item name="realName" label="真实姓名"><Input /></Form.Item>
                    {supervisorEditingId && (
                      <>
                        <Form.Item name="status" label="状态">
                          <Select>
                            <Select.Option value="ACTIVE">正常</Select.Option>
                            <Select.Option value="DISABLED">禁用</Select.Option>
                          </Select>
                        </Form.Item>
                        <Form.Item name="password" label="新密码（留空不修改）">
                          <Input.Password placeholder="留空则不修改密码" />
                        </Form.Item>
                      </>
                    )}
                  </Form>
                </Modal>
              </>
            ),
          },
          {
            key: 'student',
            label: '学生账号管理',
            children: (
              <>
                <Space style={{ marginBottom: 16 }}>
                  <Select
                    placeholder="关联状态筛选"
                    allowClear
                    style={{ width: 160 }}
                    value={linkFilter}
                    onChange={(v) => setLinkFilter(v)}
                  >
                    <Select.Option value={true}>已关联账号</Select.Option>
                    <Select.Option value={false}>未关联账号</Select.Option>
                  </Select>
                </Space>
                <Table
                  rowKey="id"
                  columns={studentColumns}
                  dataSource={studentData}
                  loading={studentLoading}
                  pagination={{
                    current: studentPage, pageSize: studentPageSize, total: studentTotal,
                    onChange: (p, ps) => { setStudentPage(p); setStudentPageSize(ps); },
                  }}
                />
                {/* 创建账号 Modal */}
                <Modal
                  title="为学生创建登录账号"
                  open={createAccountModalVisible}
                  onOk={handleSubmitCreateAccount}
                  onCancel={() => setCreateAccountModalVisible(false)}
                >
                  <Alert variant="warning" appearance="light" size="sm" className="mb-4">
                    <AlertIcon><AlertTriangle className="size-3.5" /></AlertIcon>
                    <AlertTitle>为该学生创建一个新的 STUDENT 登录账号，创建后自动关联</AlertTitle>
                  </Alert>
                  <Form form={createAccountForm} layout="vertical">
                    <Form.Item name="username" label="用户名" rules={[{ required: true }]}><Input /></Form.Item>
                    <Form.Item name="password" label="密码" rules={[{ required: true }]}><Input.Password /></Form.Item>
                  </Form>
                </Modal>
                {/* 关联已有账号 Modal */}
                <Modal
                  title="关联已有学生账号"
                  open={linkExistingModalVisible}
                  onOk={handleSubmitLinkExisting}
                  onCancel={() => setLinkExistingModalVisible(false)}
                >
                  <Alert variant="warning" appearance="light" size="sm" className="mb-4">
                    <AlertIcon><AlertTriangle className="size-3.5" /></AlertIcon>
                    <AlertTitle>搜索并选择一个已有的 STUDENT 角色用户进行关联</AlertTitle>
                  </Alert>
                  <Form form={linkExistingForm} layout="vertical">
                    <Form.Item name="userId" label="选择用户" rules={[{ required: true, message: '请选择用户' }]}>
                      <Select
                        showSearch
                        placeholder="输入用户名搜索"
                        filterOption={false}
                        onSearch={handleSearchStudentUsers}
                        loading={studentUserSearchLoading}
                        options={studentUserSearch.map((u) => ({
                          label: `${u.username}${u.realName ? ` (${u.realName})` : ''}`,
                          value: u.id,
                        }))}
                      />
                    </Form.Item>
                  </Form>
                </Modal>
              </>
            ),
          },
        ]}
      />
    </div>
  );
};

export default Personnel;
