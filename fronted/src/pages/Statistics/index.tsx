import React, { useEffect, useState, useMemo } from 'react';
import { Card, Row, Col, Select, Table, Spin, Statistic, Tag, message } from 'antd';
import ReactECharts from 'echarts-for-react';
import { getCollectionRate, getArrears } from '../../services/api';

const cardStyle = {
  borderRadius: 24,
  border: '1px solid rgba(26,26,26,0.08)',
  boxShadow: '2px 2px 0 rgba(26,26,26,0.04)',
};

const arrearsStatusMap: Record<string, { color: string; text: string }> = {
  UNPAID: { color: 'orange', text: '未缴' },
  OVERDUE: { color: 'red', text: '逾期' },
};

/** 计算当前学期：1 月–8 月为第1学期，9 月–12 月为第2学期（与后端一致） */
function getDefaultSemester(): string {
  const now = new Date();
  const month = now.getMonth() + 1; // 0‑based → 1‑based
  return month <= 8 ? `${now.getFullYear()}-1` : `${now.getFullYear()}-2`;
}

const Statistics: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [collectionData, setCollectionData] = useState<any[]>([]);
  const [arrears, setArrears] = useState<any>({ totalCount: 0, totalArrears: 0, records: [] });
  const [semester, setSemester] = useState(getDefaultSemester);

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
    } catch (err) { console.error(err); message.error('加载统计数据失败'); }
    setLoading(false);
  };

  /** Capsule 设计系统色谱 */
  const capsulePalette = ['#E85D4E', '#C4D94E', '#F2D160', '#8BB4F7', '#C5B5E0', '#A8E6CF', '#F5B895'];
  const textMuted = 'rgba(26,26,26,0.35)';

  const barOption = useMemo(() => ({
    title: {
      text: '各类型收缴率',
      left: 'center',
      textStyle: { color: '#1A1A1A', fontSize: 15, fontWeight: 600 },
    },
    tooltip: {
      trigger: 'axis' as const,
      backgroundColor: '#FFFFFF',
      borderColor: 'rgba(26,26,26,0.08)',
      textStyle: { color: '#1A1A1A', fontSize: 13 },
    },
    grid: { top: 50, bottom: 30, left: 50, right: 30 },
    xAxis: {
      type: 'category' as const,
      data: collectionData.map((d: any) => d.feeType),
      axisLine: { lineStyle: { color: 'rgba(26,26,26,0.10)' } },
      axisTick: { show: false },
      axisLabel: { color: textMuted, fontSize: 12 },
    },
    yAxis: {
      type: 'value' as const,
      max: 100,
      axisLabel: { formatter: '{value}%', color: textMuted, fontSize: 12 },
      splitLine: { lineStyle: { color: 'rgba(26,26,26,0.05)' } },
      axisLine: { show: false },
    },
    series: [{
      type: 'bar' as const,
      data: collectionData.map((d: any, i: number) => ({
        value: d.collectionRate,
        itemStyle: {
          color: capsulePalette[i % capsulePalette.length],
          borderRadius: [8, 8, 0, 0],
        },
      })),
      barWidth: 72,
      barCategoryGap: '30%',
      label: { show: true, position: 'top', formatter: '{c}%', color: '#1A1A1A', fontSize: 12, fontWeight: 600 },
      emphasis: {
        itemStyle: { opacity: 0.85 },
      },
    }],
  }), [collectionData]);

  const avgRate = useMemo(() => {
    if (collectionData.length === 0) return 0;
    return (collectionData.reduce((s: number, d: any) => s + (d.collectionRate || 0), 0) / collectionData.length).toFixed(2);
  }, [collectionData]);

  const columns = useMemo(() => [
    { title: '学号', dataIndex: 'studentNo' },
    { title: '姓名', dataIndex: 'studentName' },
    { title: '宿舍号', dataIndex: 'dormitoryNo' },
    { title: '欠费金额', dataIndex: 'arrears' },
    { title: '截止日期', dataIndex: 'dueDate' },
    {
      title: '状态', dataIndex: 'status',
      render: (s: string) => {
        const m = arrearsStatusMap[s] || { color: 'default', text: s };
        return <Tag color={m.color}>{m.text}</Tag>;
      },
    },
  ], []);

  return (
    <div style={{ padding: 24 }}>
      <div className="search-form">
        <Select
          placeholder="选择学期"
          allowClear
          style={{ width: 160 }}
          value={semester || undefined}
          onChange={(v) => setSemester(v || '')}
        >
          {(() => {
            const currentYear = new Date().getFullYear();
            const options = [];
            for (let y = currentYear; y >= currentYear - 3; y--) {
              options.push(<Select.Option key={`${y}-1`} value={`${y}-1`}>{y}年第1学期</Select.Option>);
              options.push(<Select.Option key={`${y}-2`} value={`${y}-2`}>{y}年第2学期</Select.Option>);
            }
            return options;
          })()}
        </Select>
      </div>

      {/* 概览卡片 — 始终渲染，loading 时显示骨架数值 */}
      <Row gutter={[16, 16]}>
        <Col span={6}>
          <Card style={cardStyle}>
            {loading
              ? <Statistic title="欠费总人数" value="—" />
              : <Statistic title="欠费总人数" value={arrears.totalCount || 0} />}
          </Card>
        </Col>
        <Col span={6}>
          <Card style={cardStyle}>
            {loading
              ? <Statistic title="欠费总金额" value="—" />
              : <Statistic title="欠费总金额" value={arrears.totalArrears || 0} precision={2} />}
          </Card>
        </Col>
        <Col span={6}>
          <Card style={cardStyle}>
            {loading
              ? <Statistic title="收费类型数" value="—" />
              : <Statistic title="收费类型数" value={collectionData.length} />}
          </Card>
        </Col>
        <Col span={6}>
          <Card style={cardStyle}>
            {loading
              ? <Statistic title="平均收缴率" value="—" />
              : <Statistic title="平均收缴率" value={avgRate} suffix="%" />}
          </Card>
        </Col>
      </Row>

      {/* 图表 — loading 时显示 Spin 占位 */}
      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col span={24}>
          <Card style={cardStyle}>
            {loading
              ? <div style={{ height: 350, display: 'flex', alignItems: 'center', justifyContent: 'center' }}><Spin /></div>
              : <ReactECharts option={barOption} style={{ height: 350 }} />}
          </Card>
        </Col>
      </Row>

      {/* 欠费明细 — loading 时显示表格骨架 */}
      <Card title="欠费明细" style={{ marginTop: 16, ...cardStyle }}>
        <Table
          rowKey="billNo"
          columns={columns}
          dataSource={arrears.records || []}
          loading={loading}
          pagination={{ pageSize: 10 }}
        />
      </Card>
    </div>
  );
};

export default Statistics;
