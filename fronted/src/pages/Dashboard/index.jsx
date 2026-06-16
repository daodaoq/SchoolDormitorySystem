import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Statistic, Table, Spin } from 'antd';
import { UserOutlined, FileTextOutlined, DollarOutlined, WarningOutlined } from '@ant-design/icons';
import ReactECharts from 'echarts-for-react';
import { getOverview } from '../../services/api';

function Dashboard() {
  const [loading, setLoading] = useState(true);
  const [data, setData] = useState(null);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      const res = await getOverview();
      setData(res.data);
    } catch (e) {
      console.error(e);
    }
    setLoading(false);
  };

  if (loading) return <Spin size="large" style={{ display: 'block', margin: '100px auto' }} />;
  if (!data) return <div>加载失败</div>;

  const pieOption = {
    title: { text: '收费类型分布', left: 'center' },
    tooltip: { trigger: 'item', formatter: '{b}: {c} 份 ({d}%)' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      data: (data.feeTypeDistribution || []).map((item) => ({
        name: item.feeType,
        value: item.count,
      })),
      emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.5)' } },
    }],
  };

  const gaugeOption = {
    title: { text: '本学期收缴率', left: 'center' },
    series: [{
      type: 'gauge',
      startAngle: 180, endAngle: 0,
      min: 0, max: 100,
      splitNumber: 10,
      axisLine: { lineStyle: { width: 20, color: [[0.3, '#ff4d4f'], [0.7, '#faad14'], [1, '#52c41a']] } },
      pointer: { length: '60%', width: 6 },
      detail: { valueAnimation: true, formatter: '{value}%', fontSize: 20, offsetCenter: [0, '60%'] },
      data: [{ value: data.collectionRate || 0 }],
    }],
  };

  return (
    <div>
      <Row gutter={[16, 16]}>
        <Col span={6}>
          <Card><Statistic title="学生总数" value={data.totalStudents} prefix={<UserOutlined />} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="本学期账单" value={data.totalBillsThisSemester} prefix={<FileTextOutlined />} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="应收总额" value={data.totalAmount} prefix={<DollarOutlined />} precision={2} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="逾期数量" value={data.overdueCount} prefix={<WarningOutlined />} valueStyle={{ color: data.overdueCount > 0 ? '#ff4d4f' : undefined }} /></Card>
        </Col>
      </Row>
      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col span={6}>
          <Card><Statistic title="已收金额" value={data.paidAmount} precision={2} valueStyle={{ color: '#3f8600' }} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="收缴率" value={data.collectionRate} suffix="%" precision={2} valueStyle={{ color: data.collectionRate > 80 ? '#3f8600' : '#cf1322' }} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="逾期金额" value={data.overdueAmount} precision={2} valueStyle={{ color: '#cf1322' }} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="未缴人数" value={data.unpaidCount} /></Card>
        </Col>
      </Row>
      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col span={12}>
          <Card><ReactECharts option={pieOption} style={{ height: 350 }} /></Card>
        </Col>
        <Col span={12}>
          <Card><ReactECharts option={gaugeOption} style={{ height: 350 }} /></Card>
        </Col>
      </Row>
    </div>
  );
}

export default Dashboard;
