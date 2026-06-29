import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Statistic, Spin } from 'antd';
import { UserOutlined, FileTextOutlined, DollarOutlined, WarningOutlined } from '@ant-design/icons';
import ReactECharts from 'echarts-for-react';
import { getOverview } from '../../services/api';
import type { DashboardOverview } from '../../types';

/** Capsule stat-pill 风格统计卡片 */
const statCardStyle: React.CSSProperties = {
  borderRadius: 24,
  border: '1px solid rgba(26,26,26,0.10)',
  boxShadow: '3px 3px 0 rgba(26,26,26,0.05)',
  background: '#FFFFFF',
  transition: 'transform 0.15s, box-shadow 0.15s',
  cursor: 'default',
};

const statCardHover = (e: React.MouseEvent<HTMLDivElement>) => {
  e.currentTarget.style.transform = 'translate(-2px, -2px)';
  e.currentTarget.style.boxShadow = '5px 5px 0 rgba(26,26,26,0.08)';
};
const statCardLeave = (e: React.MouseEvent<HTMLDivElement>) => {
  e.currentTarget.style.transform = 'translate(0, 0)';
  e.currentTarget.style.boxShadow = '3px 3px 0 rgba(26,26,26,0.05)';
};

/** 彩色统计条 — 药丸形状 */
const StatBar: React.FC<{ color: string; pct: number }> = ({ color, pct }) => (
  <div style={{
    width: '100%',
    height: 6,
    borderRadius: 9999,
    background: '#F5F5F0',
    marginTop: 10,
    overflow: 'hidden',
  }}>
    <div style={{
      width: `${Math.min(pct, 100)}%`,
      height: '100%',
      borderRadius: 9999,
      background: color,
      transition: 'width 0.6s ease',
    }} />
  </div>
);

const Dashboard: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [data, setData] = useState<DashboardOverview | null>(null);

  useEffect(() => { fetchData(); }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      const res = await getOverview();
      setData(res.data as DashboardOverview);
    } catch { message.error('加载概览数据失败'); }
    setLoading(false);
  };

  if (loading) return <Spin size="large" style={{ display: 'block', margin: '100px auto' }} />;
  if (!data) return <div style={{ padding: 24, color: 'rgba(26,26,26,0.45)' }}>加载失败</div>;

  const pieOption = {
    title: {
      text: '收费类型分布',
      left: 'center',
      textStyle: { color: '#1A1A1A', fontFamily: "'Space Grotesk', sans-serif", fontWeight: 600, fontSize: 15 },
    },
    tooltip: { trigger: 'item' as const, formatter: '{b}: {c} 份 ({d}%)' },
    color: ['#E85D4E', '#C4D94E', '#C5B5E0', '#8BB4F7', '#F2D160', '#F5B895', '#A06CE8', '#A8E6CF'],
    series: [{
      type: 'pie' as const,
      radius: ['40%', '70%'],
      data: (data.feeTypeDistribution || []).map((d) => ({ name: d.feeType, value: d.count })),
      itemStyle: { borderColor: '#1E1E1E', borderWidth: 2, borderRadius: 6 },
    }],
  };

  const gaugeOption = {
    series: [{
      type: 'gauge' as const,
      startAngle: 210,
      endAngle: -30,
      min: 0,
      max: 100,
      center: ['50%', '58%'],
      radius: '95%',
      splitNumber: 10,
      progress: {
        show: true,
        width: 16,
        roundCap: true,
        itemStyle: {
          color: {
            type: 'linear' as const,
            x: 0, y: 0, x2: 1, y2: 0,
            colorStops: [
              { offset: 0, color: '#E85D4E' },
              { offset: 0.5, color: '#F2D160' },
              { offset: 1, color: '#C4D94E' },
            ],
          },
        },
      },
      axisLine: {
        lineStyle: {
          width: 16,
          color: [[1, 'rgba(26,26,26,0.06)']],
        },
        roundCap: true,
      },
      axisTick: { show: false },
      splitLine: { show: false },
      axisLabel: {
        distance: 10,
        color: 'rgba(26,26,26,0.30)',
        fontSize: 11,
        fontFamily: "'Inter', sans-serif",
      },
      anchor: {
        show: true,
        size: 12,
        itemStyle: { color: '#1A1A1A' },
      },
      pointer: {
        show: false,
      },
      title: {
        show: false,
      },
      detail: {
        valueAnimation: true,
        formatter: (value: number) => `${value}%`,
        offsetCenter: [0, '32%'],
        fontSize: 44,
        color: '#1A1A1A',
        fontFamily: "'Inter', 'SF Pro Display', sans-serif",
        fontWeight: 800,
        letterSpacing: -1,
      },
      data: [{ value: data.collectionRate || 0 }],
    }],
  };

  return (
    <div style={{ padding: 24 }}>
      {/* 页面标题 — Bodoni Moda */}
      <h2 style={{
        fontFamily: "'Bodoni Moda', 'Noto Serif SC', serif",
        fontSize: 24,
        fontWeight: 700,
        color: '#1A1A1A',
        marginBottom: 24,
        letterSpacing: '-0.01em',
      }}>
        数据概览
      </h2>

      {/* 第一行统计卡片 */}
      <Row gutter={[16, 16]}>
        {[
          { label: '学生总数', value: data.totalStudents, icon: <UserOutlined />, color: 'var(--coral)', pct: 85 },
          { label: '本学期账单', value: data.totalBillsThisSemester, icon: <FileTextOutlined />, color: 'var(--lime)', pct: 72 },
          { label: '应收总额', value: `¥${(data.totalAmount ?? 0).toLocaleString()}`, icon: <DollarOutlined />, color: 'var(--sky)', pct: 65, isCurrency: true },
          { label: '逾期数量', value: data.overdueCount, icon: <WarningOutlined />, color: 'var(--yellow)', pct: 15, alert: data.overdueCount > 0 },
        ].map((stat, i) => (
          <Col span={6} key={i}>
            <div
              style={statCardStyle}
              onMouseEnter={statCardHover}
              onMouseLeave={statCardLeave}
            >
              <Card
                style={{ borderRadius: 24, border: 'none', boxShadow: 'none' }}
                bodyStyle={{ padding: '28px 24px' }}
              >
                <Statistic
                  title={
                    <span style={{
                      fontFamily: "'Space Grotesk', sans-serif",
                      fontSize: 12,
                      fontWeight: 600,
                      color: 'rgba(26,26,26,0.40)',
                      letterSpacing: '0.08em',
                      textTransform: 'uppercase' as const,
                    }}>
                      {stat.label}
                    </span>
                  }
                  value={stat.isCurrency ? undefined : stat.value}
                  formatter={stat.isCurrency ? () => stat.value as string : undefined}
                  prefix={stat.icon}
                  valueStyle={{
                    fontFamily: "'Bodoni Moda', serif",
                    fontWeight: 800,
                    fontSize: 28,
                    color: stat.alert ? '#E85D4E' : '#1A1A1A',
                    letterSpacing: '-0.02em',
                  }}
                />
                <StatBar color={stat.color} pct={stat.pct} />
              </Card>
            </div>
          </Col>
        ))}
      </Row>

      {/* 第二行统计卡片 */}
      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        {[
          { label: '已收金额', value: `¥${(data.paidAmount ?? 0).toLocaleString()}`, color: 'var(--mint)', pct: 70, isCurrency: true },
          { label: '收缴率', value: `${data.collectionRate ?? 0}%`, color: 'var(--lime)', pct: data.collectionRate ?? 0 },
          { label: '逾期金额', value: `¥${(data.overdueAmount ?? 0).toLocaleString()}`, color: 'var(--coral)', pct: 12, isCurrency: true, alert: true },
          { label: '未缴人数', value: data.unpaidCount, color: 'var(--lavender)', pct: 18 },
        ].map((stat, i) => (
          <Col span={6} key={i}>
            <div
              style={statCardStyle}
              onMouseEnter={statCardHover}
              onMouseLeave={statCardLeave}
            >
              <Card
                style={{ borderRadius: 24, border: 'none', boxShadow: 'none' }}
                bodyStyle={{ padding: '28px 24px' }}
              >
                <Statistic
                  title={
                    <span style={{
                      fontFamily: "'Space Grotesk', sans-serif",
                      fontSize: 12,
                      fontWeight: 600,
                      color: 'rgba(26,26,26,0.40)',
                      letterSpacing: '0.08em',
                      textTransform: 'uppercase' as const,
                    }}>
                      {stat.label}
                    </span>
                  }
                  value={stat.isCurrency ? undefined : stat.value}
                  formatter={stat.isCurrency ? () => stat.value as string : undefined}
                  valueStyle={{
                    fontFamily: "'Bodoni Moda', serif",
                    fontWeight: 800,
                    fontSize: 28,
                    color: stat.alert ? '#E85D4E' : '#1A1A1A',
                    letterSpacing: '-0.02em',
                  }}
                />
                <StatBar color={stat.color} pct={stat.pct} />
              </Card>
            </div>
          </Col>
        ))}
      </Row>

      {/* 图表区 */}
      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col span={12}>
          <Card
            style={{
              borderRadius: 24,
              border: '1px solid rgba(26,26,26,0.10)',
              boxShadow: '3px 3px 0 rgba(26,26,26,0.05)',
            }}
            bodyStyle={{ padding: 32 }}
          >
            <ReactECharts option={pieOption} style={{ height: 350 }} />
          </Card>
        </Col>
        <Col span={12}>
          <Card
            style={{
              borderRadius: 24,
              border: '1px solid rgba(26,26,26,0.10)',
              boxShadow: '3px 3px 0 rgba(26,26,26,0.05)',
            }}
            title={
              <span style={{
                fontFamily: "'Space Grotesk', sans-serif",
                fontSize: 14,
                fontWeight: 600,
                color: 'rgba(26,26,26,0.45)',
                letterSpacing: '0.06em',
                textTransform: 'uppercase' as const,
              }}>
                本学期收缴率
              </span>
            }
            bodyStyle={{ padding: '8px 32px 32px' }}
          >
            <ReactECharts option={gaugeOption} style={{ height: 320 }} />
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default Dashboard;
