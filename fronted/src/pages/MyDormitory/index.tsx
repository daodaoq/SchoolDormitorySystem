import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Descriptions, Tag, Statistic, Spin, Empty, Progress } from 'antd';
import { HomeOutlined, PhoneOutlined, CalendarOutlined, CheckCircleOutlined, WarningOutlined } from '@ant-design/icons';
import { useAuth } from '../../contexts/AuthContext';
import { getOverview, getStudentOverview } from '../../services/api';
import type { DashboardOverview } from '../../types';

const MyDormitory: React.FC = () => {
  const { user } = useAuth();
  const [overview, setOverview] = useState<DashboardOverview | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => { loadData(); }, []);

  const loadData = async () => {
    try {
      const studentInfo = user?.studentInfo;
      // 学生端：获取个人缴费概览；管理员端：获取全局概览
      if (studentInfo?.id) {
        const res = await getStudentOverview(studentInfo.id);
        setOverview(res.data);
      } else {
        const res = await getOverview();
        setOverview(res.data);
      }
    } catch { /* */ }
    setLoading(false);
  };

  if (loading) return <Spin size="large" style={{ display: 'block', margin: '120px auto' }} />;

  const studentInfo = user?.studentInfo;

  return (
    <div style={{ padding: 24 }}>
      {/* 欢迎卡片 */}
      <Card style={{
        marginBottom: 24, borderRadius: 24,
        border: '1px solid rgba(26,26,26,0.10)',
        boxShadow: '3px 3px 0 rgba(26,26,26,0.05)',
      }}>
        <h1 style={{
          fontFamily: "'Bodoni Moda', 'Noto Serif SC', serif",
          fontSize: 22, fontWeight: 700, marginBottom: 4, color: '#1A1A1A',
          letterSpacing: '-0.01em',
        }}>
          欢迎回来，{user?.realName || '同学'}
        </h1>
        <p style={{ color: 'rgba(26,26,26,0.35)', fontSize: 15, margin: 0, fontFamily: "'Space Grotesk', sans-serif" }}>
          在这里查看你的宿舍信息和缴费状态
        </p>
      </Card>

      <Row gutter={[24, 24]}>
        {/* 宿舍信息 */}
        <Col xs={24} md={12}>
          <Card
            title={<><HomeOutlined /> 宿舍信息</>}
            style={{ borderRadius: 24, border: '1px solid rgba(26,26,26,0.10)', boxShadow: '3px 3px 0 rgba(26,26,26,0.05)' }}
          >
            <Descriptions column={1} size="middle">
              <Descriptions.Item label="姓名">{studentInfo?.studentName || user?.realName || '—'}</Descriptions.Item>
              <Descriptions.Item label="学号">{studentInfo?.studentNo || '—'}</Descriptions.Item>
              <Descriptions.Item label="宿舍号">
                <Tag color="#E85D4E" style={{ fontSize: 16, padding: '4px 16px', borderRadius: 9999 }}>
                  {studentInfo?.dormitoryNo || '未分配'}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label={<><PhoneOutlined /> 联系方式</>}>{studentInfo?.phone || '—'}</Descriptions.Item>
              <Descriptions.Item label={<><CalendarOutlined /> 入住时间</>}>{studentInfo?.checkInDate || '—'}</Descriptions.Item>
            </Descriptions>
          </Card>
        </Col>

        {/* 缴费概览 */}
        <Col xs={24} md={12}>
          <Card
            title={<><CheckCircleOutlined /> 缴费概览</>}
            style={{ borderRadius: 24, border: '1px solid rgba(26,26,26,0.10)', boxShadow: '3px 3px 0 rgba(26,26,26,0.05)' }}
          >
            {overview ? (
              <Row gutter={[16, 16]}>
                <Col span={24}>
                  <div style={{ textAlign: 'center', padding: '8px 0 16px' }}>
                    <Progress type="dashboard" percent={Math.round(overview.collectionRate || 0)}
                      strokeColor={{ '0%': '#E85D4E', '100%': '#C4D94E' }} size={140}
                      format={(p) => <span style={{ fontSize: 28, fontWeight: 700, color: '#1A1A1A', fontFamily: "'Bodoni Moda', serif" }}>{p}%</span>} />
                    <p style={{ marginTop: 4, color: 'rgba(26,26,26,0.35)', fontSize: 13 }}>{studentInfo ? '我的缴费进度' : '整体缴费进度'}</p>
                  </div>
                </Col>
                <Col span={12}>
                  <Statistic title="应缴总额" value={overview.totalAmount} prefix="¥" precision={2} />
                </Col>
                <Col span={12}>
                  <Statistic title="已缴金额" value={overview.paidAmount} prefix="¥" precision={2}
                    valueStyle={{ color: '#C4D94E' }} />
                </Col>
                <Col span={12}>
                  <Statistic title="逾期金额" value={overview.overdueAmount} prefix="¥" precision={2}
                    valueStyle={{ color: overview.overdueAmount > 0 ? '#E85D4E' : '#C4D94E' }} />
                </Col>
                <Col span={12}>
                  <Statistic title="逾期数量" value={overview.overdueCount}
                    valueStyle={{ color: overview.overdueCount > 0 ? '#E85D4E' : '#C4D94E' }} />
                </Col>
              </Row>
            ) : <Empty description="暂无数据" />}
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default MyDormitory;
