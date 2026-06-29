import React, { useState } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { Layout, Avatar, Dropdown, Typography } from 'antd';
import * as Icons from '@ant-design/icons';
import { LogoutOutlined, UserOutlined, RightOutlined, DownOutlined, IdcardOutlined } from '@ant-design/icons';
import { useAuthStore } from '../stores/authStore';
import type { MenuItem } from '../types';

const { Content } = Layout;
const { Text } = Typography;

/** 动态获取 Ant Design 图标 */
const getIcon = (iconName?: string): React.ReactNode => {
  if (!iconName) return null;
  const IconComp = (Icons as Record<string, React.ComponentType>)[iconName];
  return IconComp ? <IconComp /> : null;
};

/** 判断菜单项或其子项是否匹配当前路径 */
function isMenuActive(menu: MenuItem, pathname: string): boolean {
  if (menu.path && pathname.startsWith(menu.path)) return true;
  if (menu.children?.length) {
    return menu.children.some((child) => child.path && pathname.startsWith(child.path));
  }
  return false;
}

const BasicLayout: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const user = useAuthStore((s) => s.user);
  const menus = useAuthStore((s) => s.menus);
  const logout = useAuthStore((s) => s.logout);
  const [expanded, setExpanded] = useState<Set<string>>(new Set());

  // 过滤出一级菜单
  const topMenus = menus
    .filter((m) => m.menuType === 'MENU' || (m.menuType === 'PAGE' && m.parentId === 0))
    .sort((a, b) => a.sortOrder - b.sortOrder);

  // 首次渲染时自动展开包含当前路径的父菜单
  React.useEffect(() => {
    topMenus.forEach((menu) => {
      if (isMenuActive(menu, location.pathname) && menu.children?.length) {
        setExpanded((prev) => new Set([...prev, String(menu.id)]));
      }
    });
  }, [location.pathname]);

  const toggleExpand = (id: string) => {
    setExpanded((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  };

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  const SIDEBAR_W = 248;

  return (
    <Layout style={{ minHeight: '100vh', background: '#F5F5F0', position: 'relative' }}>
      {/* === 纸纹肌理 === */}
      <div className="grain-overlay" />

      {/* ============================================================
           Sidebar — 左侧竖排导航
           ============================================================ */}
      <aside
        style={{
          position: 'fixed',
          top: 0,
          left: 0,
          bottom: 0,
          width: SIDEBAR_W,
          background: '#FAF9F5',
          borderRight: '1px solid rgba(26,26,26,0.10)',
          boxShadow: '2px 0 12px rgba(26,26,26,0.04)',
          display: 'flex',
          flexDirection: 'column',
          zIndex: 100,
          overflow: 'hidden',
        }}
      >
        {/* Logo 区 */}
        <div
          style={{
            padding: '28px 24px 20px',
            borderBottom: '1px solid rgba(26,26,26,0.10)',
            cursor: 'pointer',
          }}
          onClick={() => navigate('/dashboard')}
        >
          <h2
            style={{
              margin: 0,
              fontFamily: "'Bodoni Moda', 'Noto Serif SC', serif",
              fontSize: 21,
              fontWeight: 800,
              color: '#1A1A1A',
              letterSpacing: '-0.02em',
              lineHeight: 1.1,
            }}
          >
            宿舍收费
            <br />
            管理
          </h2>
          <span
            style={{
              fontFamily: "'Space Grotesk', sans-serif",
              fontSize: 11,
              color: 'rgba(26,26,26,0.30)',
              letterSpacing: '0.10em',
              textTransform: 'uppercase',
            }}
          >
            Admin Console
          </span>
        </div>

        {/* 菜单列表 */}
        <nav
          style={{
            flex: 1,
            overflowY: 'auto',
            padding: '12px 16px',
            display: 'flex',
            flexDirection: 'column',
            gap: 2,
          }}
        >
          {topMenus.map((menu) => {
            const active = isMenuActive(menu, location.pathname);
            const hasChildren = menu.children && menu.children.length > 0;
            const isOpen = expanded.has(String(menu.id));
            const icon = getIcon(menu.icon);

            return (
              <div key={menu.id}>
                {/* 一级菜单 */}
                <button
                  onClick={() => {
                    if (hasChildren) {
                      toggleExpand(String(menu.id));
                    } else if (menu.path) {
                      navigate(menu.path);
                    }
                  }}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    width: '100%',
                    padding: '11px 16px',
                    borderRadius: 16,
                    border: active && !hasChildren ? '1px solid rgba(26,26,26,0.12)' : '1px solid transparent',
                    background: active && !hasChildren ? '#EDE8E0' : 'transparent',
                    color: active ? '#1A1A1A' : 'rgba(26,26,26,0.55)',
                    fontSize: 13.5,
                    fontWeight: active ? 600 : 500,
                    fontFamily: "'Space Grotesk', -apple-system, BlinkMacSystemFont, 'PingFang SC', sans-serif",
                    letterSpacing: '0.02em',
                    cursor: 'pointer',
                    transition: 'all 0.16s',
                    boxShadow: active && !hasChildren ? '2px 2px 0 rgba(26,26,26,0.05)' : 'none',
                    textAlign: 'left',
                    gap: 10,
                  }}
                  onMouseEnter={(e) => {
                    if (!active) {
                      e.currentTarget.style.background = 'rgba(26,26,26,0.025)';
                      e.currentTarget.style.color = '#1A1A1A';
                    }
                  }}
                  onMouseLeave={(e) => {
                    if (!active) {
                      e.currentTarget.style.background = 'transparent';
                      e.currentTarget.style.color = 'rgba(26,26,26,0.55)';
                    }
                  }}
                >
                  {icon && <span style={{ fontSize: 16, opacity: 0.7, flexShrink: 0 }}>{icon}</span>}
                  <span style={{ flex: 1, minWidth: 0, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {menu.menuName}
                  </span>
                  {hasChildren && (
                    <span style={{ fontSize: 10, opacity: 0.4, flexShrink: 0, transition: 'transform 0.2s', transform: isOpen ? 'rotate(90deg)' : 'rotate(0deg)' }}>
                      ▶
                    </span>
                  )}
                </button>

                {/* 子菜单 */}
                {hasChildren && isOpen && (
                  <div style={{ margin: '4px 0 4px 24px', display: 'flex', flexDirection: 'column', gap: 1 }}>
                    {menu
                      .children!.filter((c) => c.menuType === 'PAGE' || c.menuType === 'MENU')
                      .sort((a, b) => a.sortOrder - b.sortOrder)
                      .map((child) => {
                        const childActive = child.path && location.pathname.startsWith(child.path);
                        return (
                          <button
                            key={child.id}
                            onClick={() => {
                              if (child.path) navigate(child.path);
                            }}
                            style={{
                              display: 'flex',
                              alignItems: 'center',
                              width: '100%',
                              padding: '9px 14px',
                              borderRadius: 12,
                              border: '1px solid transparent',
                              background: childActive ? '#EDE8E0' : 'transparent',
                              color: childActive ? '#1A1A1A' : 'rgba(26,26,26,0.45)',
                              fontSize: 13,
                              fontWeight: childActive ? 600 : 400,
                              fontFamily: "'Space Grotesk', -apple-system, BlinkMacSystemFont, 'PingFang SC', sans-serif",
                              letterSpacing: '0.01em',
                              cursor: 'pointer',
                              transition: 'all 0.15s',
                              textAlign: 'left',
                              position: 'relative',
                            }}
                            onMouseEnter={(e) => {
                              if (!childActive) {
                                e.currentTarget.style.color = '#1A1A1A';
                                e.currentTarget.style.background = 'rgba(26,26,26,0.02)';
                              }
                            }}
                            onMouseLeave={(e) => {
                              if (!childActive) {
                                e.currentTarget.style.color = 'rgba(26,26,26,0.45)';
                                e.currentTarget.style.background = 'transparent';
                              }
                            }}
                          >
                            {childActive && (
                              <span style={{
                                position: 'absolute',
                                left: 0,
                                top: '50%',
                                transform: 'translateY(-50%)',
                                width: 3,
                                height: 18,
                                borderRadius: 9999,
                                background: '#E85D4E',
                              }} />
                            )}
                            {getIcon(child.icon) && (
                              <span style={{ fontSize: 14, opacity: 0.6, marginRight: 8, flexShrink: 0 }}>
                                {getIcon(child.icon)}
                              </span>
                            )}
                            <span style={{ flex: 1, minWidth: 0, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                              {child.menuName}
                            </span>
                          </button>
                        );
                      })}
                  </div>
                )}
              </div>
            );
          })}
        </nav>

        {/* 底部用户区 */}
        <div style={{ padding: '12px 16px', borderTop: '1px solid rgba(26,26,26,0.10)' }}>
          <Dropdown
            menu={{
              items: [
                {
                  key: 'profile',
                  icon: <IdcardOutlined />,
                  label: '个人信息',
                  onClick: () => navigate('/profile'),
                },
                { type: 'divider' as const },
                {
                  key: 'logout',
                  icon: <LogoutOutlined />,
                  label: '退出登录',
                  onClick: handleLogout,
                },
              ],
            }}
            trigger={['click']}
          >
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 10,
                padding: '10px 14px',
                borderRadius: 16,
                cursor: 'pointer',
                transition: 'background 0.15s',
              }}
              onMouseEnter={(e) => { e.currentTarget.style.background = 'rgba(26,26,26,0.03)'; }}
              onMouseLeave={(e) => { e.currentTarget.style.background = 'transparent'; }}
            >
              <Avatar
                icon={<UserOutlined />}
                size={34}
                style={{ background: '#E85D4E', color: '#fff', flexShrink: 0 }}
              />
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{
                  fontSize: 13,
                  fontWeight: 600,
                  color: '#1A1A1A',
                  fontFamily: "'Space Grotesk', -apple-system, sans-serif",
                  lineHeight: 1.3,
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  whiteSpace: 'nowrap',
                }}>
                  {user?.realName || user?.username}
                </div>
                <div style={{
                  fontSize: 11,
                  color: 'rgba(26,26,26,0.35)',
                  fontFamily: "'Space Grotesk', -apple-system, sans-serif",
                  letterSpacing: '0.04em',
                }}>
                  {user?.roleName || user?.role}
                </div>
              </div>
            </div>
          </Dropdown>
        </div>
      </aside>

      {/* ============================================================
           内容区
           ============================================================ */}
      <div style={{ marginLeft: SIDEBAR_W }}>
        <Content style={{ padding: '28px 36px', position: 'relative', zIndex: 1 }}>
          <div
            style={{
              background: '#FFFFFF',
              borderRadius: 32,
              border: '1px solid rgba(26,26,26,0.10)',
              boxShadow: '4px 4px 0 rgba(26,26,26,0.05)',
              minHeight: 'calc(100vh - 56px)',
              overflow: 'hidden',
            }}
          >
            <Outlet />
          </div>
        </Content>
      </div>
    </Layout>
  );
};

export default BasicLayout;
