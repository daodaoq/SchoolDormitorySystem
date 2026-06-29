import React, { useEffect, useState } from 'react';
import { Card, Descriptions, Spin, Tag, Avatar } from 'antd';
import { UserOutlined } from '@ant-design/icons';
import { useAuthStore } from '../../stores/authStore';
import { getCurrentUser } from '../../services/api';

const roleColorMap: Record<string, string> = {
  ADMIN: '#E85D4E',
  USER: '#8BB4F7',
  STUDENT: '#C4D94E',
};

const roleLabelMap: Record<string, string> = {
  ADMIN: '管理员',
  USER: '普通用户',
  STUDENT: '学生',
};

const Profile: React.FC = () => {
  const user = useAuthStore((s) => s.user);
  const [detail, setDetail] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getCurrentUser()
      .then((res) => setDetail(res.data))
      .catch(() => setDetail(user))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return <Spin size="large" style={{ display: 'block', margin: '120px auto' }} />;
  }

  const displayUser = detail || user;

  return (
    <div style={{ padding: 24 }}>
      {/* 头像 & 姓名区 */}
      <Card
        style={{
          marginBottom: 24,
          borderRadius: 24,
          border: '1px solid rgba(26,26,26,0.10)',
          boxShadow: '3px 3px 0 rgba(26,26,26,0.05)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 20 }}>
          <Avatar
            size={72}
            icon={<UserOutlined />}
            src={displayUser?.avatar}
            style={{
              background: 'linear-gradient(135deg, #E85D4E, #C5B5E0)',
              borderRadius: 24,
              flexShrink: 0,
            }}
          />
          <div>
            <h1
              style={{
                fontFamily: "'Bodoni Moda', 'Noto Serif SC', serif",
                fontSize: 24,
                fontWeight: 700,
                margin: '0 0 4px',
                color: '#1A1A1A',
                letterSpacing: '-0.01em',
              }}
            >
              {displayUser?.realName || displayUser?.username || '—'}
            </h1>
            <p
              style={{
                margin: 0,
                color: 'rgba(26,26,26,0.35)',
                fontSize: 14,
                fontFamily: "'Space Grotesk', sans-serif",
              }}
            >
              @{displayUser?.username || '—'}
            </p>
          </div>
          {displayUser?.role && (
            <Tag
              color={roleColorMap[displayUser.role] || '#C5B5E0'}
              style={{
                marginLeft: 'auto',
                fontSize: 14,
                padding: '6px 18px',
                borderRadius: 9999,
                border: '2px solid rgba(26,26,26,0.10)',
                fontWeight: 600,
              }}
            >
              {displayUser?.roleName || roleLabelMap[displayUser.role] || displayUser.role}
            </Tag>
          )}
        </div>
      </Card>

      {/* 详细信息 */}
      <Card
        title={
          <span
            style={{
              fontFamily: "'Bodoni Moda', 'Noto Serif SC', serif",
              fontSize: 18,
              fontWeight: 700,
              color: '#1A1A1A',
            }}
          >
            账户信息
          </span>
        }
        style={{
          borderRadius: 24,
          border: '1px solid rgba(26,26,26,0.10)',
          boxShadow: '3px 3px 0 rgba(26,26,26,0.05)',
        }}
      >
        <Descriptions
          column={1}
          size="middle"
          labelStyle={{
            color: 'rgba(26,26,26,0.55)',
            fontWeight: 500,
            fontSize: 14,
          }}
          contentStyle={{
            color: '#1A1A1A',
            fontSize: 14,
            fontWeight: 500,
          }}
        >
          <Descriptions.Item label="用户名">
            {displayUser?.username || '—'}
          </Descriptions.Item>
          <Descriptions.Item label="真实姓名">
            {displayUser?.realName || '—'}
          </Descriptions.Item>
          <Descriptions.Item label="角色">
            <Tag
              color={roleColorMap[displayUser?.role] || '#C5B5E0'}
              style={{ borderRadius: 9999, padding: '2px 12px' }}
            >
              {displayUser?.roleName || roleLabelMap[displayUser?.role] || displayUser?.role || '—'}
            </Tag>
          </Descriptions.Item>
          <Descriptions.Item label="账号状态">
            <Tag
              color={displayUser?.status === 'ACTIVE' ? '#C4D94E' : '#E85D4E'}
              style={{ borderRadius: 9999, padding: '2px 12px' }}
            >
              {displayUser?.status === 'ACTIVE' ? '正常' : displayUser?.status || '—'}
            </Tag>
          </Descriptions.Item>
          <Descriptions.Item label="创建时间">
            {displayUser?.createTime || '—'}
          </Descriptions.Item>
          <Descriptions.Item label="上次更新">
            {displayUser?.updateTime || '—'}
          </Descriptions.Item>
        </Descriptions>
      </Card>

      {/* 安全提示 */}
      <Card
        style={{
          marginTop: 24,
          borderRadius: 24,
          border: '1px solid rgba(26,26,26,0.10)',
          boxShadow: '3px 3px 0 rgba(26,26,26,0.05)',
          background: 'rgba(232,93,78,0.04)',
        }}
      >
        <p
          style={{
            margin: 0,
            color: 'rgba(26,26,26,0.55)',
            fontSize: 13,
            fontFamily: "'Space Grotesk', sans-serif",
          }}
        >
          个人信息为只读展示，如需修改请联系管理员。
        </p>
      </Card>
    </div>
  );
};

export default Profile;
