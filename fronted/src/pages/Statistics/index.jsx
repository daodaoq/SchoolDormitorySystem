import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Select, Table, Spin, Statistic } from 'antd';
import ReactECharts from 'echarts-for-react';
import { getCollectionRate, getArrears, getSemesterReport } from '../../services/api';

function Statistics() {
  const [loading, setLoading] = useState(true);
  const [collectionData, setCollectionData] = useState([]);
  const [arrears, setArrears] = useState({ totalCount: 0, totalArrears: 0, records: [] });
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
    } catch (e) { console.error(e); }
    setLoading(false);
  };

  const barOption = {
    title: { text: '各类型收缴率', left: 'center' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: collectionData.map((d) => d.feeType) },
    yAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}%' } },
    series: [{
      type: 'bar', data: collectionData.map((d) => d.collectionRate),
      itemStyle: { color: '#1890ff' }, label: { show: true, formatter: '{c}%' }
    }],
  };

  const pieOption = {
    title: { text: '金额分布', left: 'center' },
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    series: [{
      type: 'pie', radius: '60%',
      data: collectionData.map((d) => ({ name: d.feeType, value: d.totalAmount })),
    }],
  };

  const columns = [
    { title: '学号', dataIndex: 'studentNo', key: 'studentNo' },
    { title: '姓名', dataIndex: 'studentName', key: 'studentName' },
    { title: '宿舍号', dataIndex: 'dormitoryNo', key: 'dormitoryNo' },
    { title: '欠费金额', dataIndex: 'arrears', key: 'arrears' },
    { title: '应缴金额', dataIndex: 'amount', key: 'amount' },
    { title: '截止日期', dataIndex: 'dueDate', key: 'dueDate' },
    { title: '状态', dataIndex: 'status', key: 'status' },
  ];

  if (loading) return <Spin size="large" style={{ display: 'block', margin: '100px auto' }} />;

  return (
    <div>
      <Space style={{ marginBottom: 16 }}>
        <Select placeholder="选择学期" allowClear style={{ width: 150 }} value={semester || undefined}
          onChange={(v) => setSemester(v || '')}>
          <Select.Option value="2024-1">2024年第1学期</Select.Option>
          <Select.Option value="2024-2">2024年第2学期</Select.Option>
        </Select>
      </Space>

      <Row gutter={[16, 16]}>
        <Col span={6}><Card><Statistic title="欠费总人数" value={arrears.totalCount || 0} /></Card></Col>
        <Col span={6}><Card><Statistic title="欠费总金额" value={arrears.totalArrears || 0} precision={2} /></Card></Col>
        <Col span={6}><Card><Statistic title="收费类型数" value={collectionData.length} /></Card></Col>
        <Col span={6}>
          <Card><Statistic title="平均收缴率" value={
            collectionData.length > 0
              ? (collectionData.reduce((s, d) => s + (d.collectionRate || 0), 0) / collectionData.length).toFixed(2)
              : 0
          } suffix="%" /></Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col span={12}><Card><ReactECharts option={barOption} style={{ height: 350 }} /></Card></Col>
        <Col span={12}><Card><ReactECharts option={pieOption} style={{ height: 350 }} /></Card></Col>
      </Row>

      <Card title="欠费明细" style={{ marginTop: 16 }}>
        <Table rowKey="billNo" columns={columns} dataSource={arrears.records || []} pagination={{ pageSize: 10 }} />
      </Card>
    </div>
  );
}

export default Statistics;
