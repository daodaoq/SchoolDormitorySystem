-- ============================================
-- 人员管理功能 - 增量迁移脚本
-- 用法: mysql -u root -p school_dormitory < migration-personnel.sql
-- ============================================

-- 1. 为 student_dormitory 添加 user_id 字段，关联系统用户
ALTER TABLE student_dormitory
  ADD COLUMN IF NOT EXISTS user_id BIGINT DEFAULT NULL COMMENT '关联系统用户ID (sys_user.id)',
  ADD INDEX IF NOT EXISTS idx_user_id (user_id);

-- 2. 新增"人员管理"菜单（系统管理 > 人员管理）
INSERT IGNORE INTO sys_menu (id, parent_id, menu_name, menu_type, path, icon, permission_code, sort_order, visible)
VALUES (15, 9, '人员管理', 'MENU', '/personnel', 'IdcardOutlined', 'personnel:view', 4, 1);

-- 3. 为管理员角色分配人员管理菜单权限
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (1, 15);
