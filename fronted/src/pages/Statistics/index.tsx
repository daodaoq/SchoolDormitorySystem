import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Select, Table, Spin, Statistic } from 'antd';
import ReactECharts from 'echarts-for-react';
import { getCollectionRate, getArrears } from '../../services/api';

const cardStyle = { borderRadius: 12, border: '1px solid #f0f0f0', boxShadow: '0 1px 3px rgba(0,0,0,0.04)' };

const Statistics: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [collectionData, setCollectionData] = useState<any[]>([]);
  const [arrears, setArrears] = useState<any>({ totalCount: 0, totalArrears: 0, records: [] });
  const [semester, setSemester] = useState('');

  useEffect(() => { fetchData(); }, [semester]);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [rateRes, arrearsRes] = await Promise.all([
        getCollectionRate(semester || undefined),
        getArrears({ page: 1, pageSize: 9999 }),
      ]);
      setCollectionData(rateRes.data || []);
      setArrears(arrearsRes.data || {});
    } catch { /* handled */ }
    setLoading(false);
  };

  const barOption = {
    title: { text: '各类型收缴率', left: 'center', textStyle: { color: '#475569' } },
    tooltip: { trigger: 'axis' as const },
    xAxis: { type: 'category' as const, data: collectionData.map((d: any) => d.feeType) },
    yAxis: { type: 'value' as const, max: 100, axisLabel: { formatter: '{value}%' } },
    series: [{ type: 'bar' as const, data: collectionData.map((d: any) => d.collectionRate), itemStyle: { color: '#3b82f6' }, label: { show: true, formatter: '{c}%' } }],
  };

  if (loading) return <Spin size="large" style={{ display: 'block', margin: '100px auto' }} />;

  const columns = [
    { title: '学号', dataIndex: 'studentNo' }, { title: '姓名', dataIndex: 'studentName' },
    { title: '宿舍号', dataIndex: 'dormitoryNo' }, { title: '欠费金额', dataIndex: 'arrears' },
    { title: '截止日期', dataIndex: 'dueDate' }, { title: '状态', dataIndex: 'status' },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Select placeholder="选择学期" allowClear style={{ width: 150, marginBottom: 16 }} value={semester || undefined} onChange={(v) => setSemester(v || '')}>
        <Select.Option value="2024-1">2024年第1学期</Select.Option><Select.Option value="2024-2">2024年第2学期</Select.Option>
      </Select>
      <Row gutter={[16, 16]}>
        <Col span={6}><Card style={cardStyle}><Statistic title="欠费总人数" value={arrears.totalCount || 0} /></Card></Col>
        <Col span={6}><Card style={cardStyle}><Statistic title="欠费总金额" value={arrears.totalArrears || 0} precision={2} /></Card></Col>
        <Col span={6}><Card style={cardStyle}><Statistic title="收费类型数" value={collectionData.length} /></Card></Col>
        <Col span={6}><Card style={cardStyle}><Statistic title="平均收缴率" value={collectionData.length > 0 ? (collectionData.reduce((s: number, d: any) => s + (d.collectionRate || 0), 0) / collectionData.length).toFixed(2) : 0} suffix="%" /></Card></Col>
      </Row>
      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col span={24}><Card style={cardStyle}><ReactECharts option={barOption} style={{ height: 350 }} /></Card></Col>
      </Row>
      <Card title="欠费明细" style={{ marginTop: 16, ...cardStyle }}>
        <Table rowKey="billNo" columns={columns} dataSource={arrears.records || []} pagination={{ pageSize: 10 }} />
      </Card>
    </div>
  );
};

export default Statistics;
