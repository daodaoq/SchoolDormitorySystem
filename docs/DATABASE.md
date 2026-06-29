# 数据库设计文档

## 概述

| 属性 | 值 |
|------|-----|
| 数据库名 | `school_dormitory` |
| 字符集 | `utf8mb4` / `utf8mb4_unicode_ci` |
| 引擎 | InnoDB |
| ORM | MyBatis-Plus 3.5.6 |
| 主键策略 | AUTO_INCREMENT（业务 ID 部分使用雪花算法） |
| 逻辑删除 | `student_dormitory.deleted` |

## ER 关系图

```
                          sys_role ──N:M── sys_menu
                             │              (sys_role_menu)
                             │
                          sys_user (系统用户)
                             │ 1:1 (user_id)
                             ▼
                    student_dormitory (学生)
                        │          │
                        │          │ N:1
                        │          ▼
                        │     dormitory (宿舍)
                        │
                        │ 1:N
                        ▼
                 payment_bill (账单)
                        │
                        │ 1:N         1:N
                        ▼            ▼
              payment_record    notification_record
              (支付流水)         (通知记录)

fee_item (收费项目) ── 1:N ── payment_bill

kb_document (知识库文档) ── 1:N ── kb_chunk ── Milvus (向量)

ai_qa_log (AI 问答日志) — 独立记录

operation_log (操作审计日志) — 独立记录，AOP + RabbitMQ 异步写入
```

> 共 14 张表，33 个索引。

---

## 表结构详情

### 1. sys_user — 系统用户表

存储所有登录账号（管理员、教师、学生）。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| username | VARCHAR(50) | UNIQUE NOT NULL | 用户名；学生账号 = 学号 |
| password | VARCHAR(255) | NOT NULL | BCrypt 加密，默认 `123456` |
| real_name | VARCHAR(50) | | 真实姓名 |
| role | VARCHAR(20) | NOT NULL | ADMIN / TEACHER / STUDENT |
| avatar | VARCHAR(500) | | 头像 URL（MinIO `avatars` bucket） |
| status | VARCHAR(20) | DEFAULT 'ACTIVE' | ACTIVE / DISABLED |
| create_time | DATETIME | DEFAULT NOW() | |
| update_time | DATETIME | ON UPDATE NOW() | |

```sql
CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(50),
    role VARCHAR(20) NOT NULL,
    avatar VARCHAR(500),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_role (role)
);
```

---

### 2. student_dormitory — 学生宿舍信息表

核心业务表，记录学生与宿舍的关联关系。通过 `user_id` 与系统账号 1:1 绑定。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| student_name | VARCHAR(50) | NOT NULL | 学生姓名 |
| student_no | VARCHAR(50) | NOT NULL | 学号 |
| dormitory_no | VARCHAR(50) | | 宿舍编号，外键关联 `dormitory.dormitory_no` |
| phone | VARCHAR(20) | | 联系电话 |
| photo | VARCHAR(500) | | 照片 URL（MinIO `student-photos` bucket） |
| check_in_date | DATE | | 入住日期 |
| payment_status | VARCHAR(20) | DEFAULT 'UNPAID' | UNPAID / PAID / OVERDUE |
| user_id | BIGINT | | 关联 `sys_user.id`，可为 NULL |
| deleted | TINYINT | DEFAULT 0 | 逻辑删除标记 |
| create_time | DATETIME | DEFAULT NOW() | |
| update_time | DATETIME | ON UPDATE NOW() | |

```sql
CREATE TABLE student_dormitory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_name VARCHAR(50) NOT NULL,
    student_no VARCHAR(50) NOT NULL,
    dormitory_no VARCHAR(50),
    phone VARCHAR(20),
    photo VARCHAR(500),
    check_in_date DATE,
    payment_status VARCHAR(20) DEFAULT 'UNPAID',
    user_id BIGINT DEFAULT NULL,
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_student_no (student_no),
    INDEX idx_dormitory_no (dormitory_no),
    INDEX idx_user_id (user_id),
    INDEX idx_payment_status (payment_status)
);
```

数据同步机制：
- 新增学生 → 自动创建 `sys_user`（username = 学号，role = STUDENT）
- 批量导入 → 同上
- 启动时 DataInitializer 可将已有学生同步到 sys_user

---

### 3. dormitory — 宿舍信息表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| dormitory_no | VARCHAR(20) | UNIQUE NOT NULL | 宿舍编号，如 `A-101` |
| building | VARCHAR(50) | | 楼栋名称 |
| floor | VARCHAR(20) | | 楼层 |
| room_type | VARCHAR(30) | | 单人间 / 双人间 / 四人间 / 六人间 |
| capacity | INT | DEFAULT 4 | 容纳人数 |
| status | VARCHAR(20) | NOT NULL DEFAULT 'ACTIVE' | ACTIVE / INACTIVE |
| create_time | DATETIME | DEFAULT NOW() | |
| update_time | DATETIME | ON UPDATE NOW() | |

```sql
CREATE TABLE dormitory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dormitory_no VARCHAR(20) NOT NULL,
    building VARCHAR(50),
    floor VARCHAR(20),
    room_type VARCHAR(30),
    capacity INT DEFAULT 4,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_dormitory_no (dormitory_no),
    INDEX idx_building (building),
    INDEX idx_status (status)
);
```

---

### 4. fee_item — 收费项目表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| item_name | VARCHAR(100) | NOT NULL | 项目名称，如"住宿费" |
| fee_type | VARCHAR(30) | NOT NULL | 收费类型（自由文本输入） |
| unit_price | DECIMAL(10,2) | NOT NULL | 单价 |
| billing_cycle | VARCHAR(20) | NOT NULL | MONTHLY / SEMESTER / YEARLY |
| applicable_dorm_type | VARCHAR(30) | DEFAULT 'ALL' | 适用宿舍类型 |
| status | VARCHAR(20) | DEFAULT 'ACTIVE' | ACTIVE / INACTIVE |
| description | VARCHAR(500) | | 说明 |
| create_time | DATETIME | DEFAULT NOW() | |
| update_time | DATETIME | ON UPDATE NOW() | |

```sql
CREATE TABLE fee_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_name VARCHAR(100) NOT NULL,
    fee_type VARCHAR(30) NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    billing_cycle VARCHAR(20) NOT NULL,
    applicable_dorm_type VARCHAR(30) DEFAULT 'ALL',
    status VARCHAR(20) DEFAULT 'ACTIVE',
    description VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_fee_type (fee_type),
    INDEX idx_status (status)
);
```

---

### 5. payment_bill — 缴费账单表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| student_id | BIGINT | NOT NULL | 关联 `student_dormitory.id` |
| fee_item_id | BIGINT | NOT NULL | 关联 `fee_item.id` |
| bill_no | VARCHAR(50) | UNIQUE NOT NULL | 账单编号（雪花 ID） |
| semester | VARCHAR(20) | NOT NULL | 学期，如 `2026-1` |
| amount | DECIMAL(10,2) | NOT NULL | 应缴金额 |
| paid_amount | DECIMAL(10,2) | DEFAULT 0 | 已缴金额 |
| due_date | DATE | | 截止日期 |
| status | VARCHAR(20) | DEFAULT 'UNPAID' | UNPAID / PAID / OVERDUE / CANCELLED |
| remark | VARCHAR(500) | | 备注 |
| create_time | DATETIME | DEFAULT NOW() | |
| update_time | DATETIME | ON UPDATE NOW() | |

```sql
CREATE TABLE payment_bill (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    fee_item_id BIGINT NOT NULL,
    bill_no VARCHAR(50) NOT NULL UNIQUE,
    semester VARCHAR(20) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    paid_amount DECIMAL(10,2) DEFAULT 0,
    due_date DATE,
    status VARCHAR(20) DEFAULT 'UNPAID',
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_student_id (student_id),
    INDEX idx_semester (semester),
    INDEX idx_status (status),
    INDEX idx_bill_no (bill_no)
);
```

---

### 6. payment_record — 支付流水表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| bill_id | BIGINT | NOT NULL | 关联 `payment_bill.id` |
| student_id | BIGINT | NOT NULL | 学生 ID（冗余，便于查询） |
| order_no | VARCHAR(100) | UNIQUE NOT NULL | 订单号（雪花 ID） |
| amount | DECIMAL(10,2) | NOT NULL | 支付金额 |
| pay_method | VARCHAR(20) | | ALIPAY / SYSTEM |
| trade_no | VARCHAR(100) | | 支付宝交易号 |
| pay_time | DATETIME | | 支付时间 |
| status | VARCHAR(20) | DEFAULT 'WAITING' | WAITING / SUCCESS / CLOSED / REFUND |
| receipt_url | TEXT | | 支付页面 HTML（支付宝返回） |
| create_time | DATETIME | DEFAULT NOW() | |
| update_time | DATETIME | ON UPDATE NOW() | |

```sql
CREATE TABLE payment_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bill_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    order_no VARCHAR(100) NOT NULL UNIQUE,
    amount DECIMAL(10,2) NOT NULL,
    pay_method VARCHAR(20),
    trade_no VARCHAR(100),
    pay_time DATETIME,
    status VARCHAR(20) DEFAULT 'WAITING',
    receipt_url TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_bill_id (bill_id),
    INDEX idx_order_no (order_no),
    INDEX idx_student_id (student_id)
);
```

---

### 7. notification_record — 通知记录表

逾期催缴、缴费提醒等通知的发送记录。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| student_id | BIGINT | NOT NULL | 学生 ID |
| bill_id | BIGINT | | 关联账单 ID |
| notify_type | VARCHAR(30) | NOT NULL | OVERDUE / PAYMENT_REMINDER 等 |
| channel | VARCHAR(20) | NOT NULL | EMAIL / SMS |
| recipient | VARCHAR(200) | NOT NULL | 接收方（邮箱 / 手机号） |
| title | VARCHAR(200) | NOT NULL | 通知标题 |
| content | TEXT | NOT NULL | 通知内容 |
| status | VARCHAR(20) | DEFAULT 'PENDING' | PENDING / SENT / FAILED |
| retry_count | INT | DEFAULT 0 | 重试次数 |
| send_time | DATETIME | | 实际发送时间 |
| fail_reason | VARCHAR(500) | | 失败原因 |
| create_time | DATETIME | DEFAULT NOW() | |

```sql
CREATE TABLE notification_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    bill_id BIGINT,
    notify_type VARCHAR(30) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    recipient VARCHAR(200) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    retry_count INT DEFAULT 0,
    send_time DATETIME,
    fail_reason VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_student_id (student_id),
    INDEX idx_notify_type (notify_type),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
);
```

---

### 8. operation_log — 操作审计日志表

通过 `@OpLog` 注解 + AOP + RabbitMQ 异步写入。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| user_id | BIGINT | | 操作人 ID |
| username | VARCHAR(50) | | 操作人用户名 |
| real_name | VARCHAR(50) | | 操作人真实姓名 |
| module | VARCHAR(50) | NOT NULL | 操作模块 |
| action | VARCHAR(50) | NOT NULL | 操作类型 |
| description | VARCHAR(500) | | 操作描述 |
| method | VARCHAR(200) | | 请求方法全限定名 |
| request_params | TEXT | | 请求参数（JSON，敏感字段脱敏） |
| duration | BIGINT | | 执行耗时（ms） |
| status | VARCHAR(20) | DEFAULT 'SUCCESS' | SUCCESS / FAIL |
| error_msg | VARCHAR(500) | | 错误信息（仅 FAIL 时记录） |
| ip_address | VARCHAR(50) | | 客户端 IP |
| user_agent | VARCHAR(500) | | User-Agent |
| create_time | DATETIME | DEFAULT NOW() | |

```sql
CREATE TABLE operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(50),
    real_name VARCHAR(50),
    module VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    method VARCHAR(200),
    request_params TEXT,
    duration BIGINT,
    status VARCHAR(20) DEFAULT 'SUCCESS',
    error_msg VARCHAR(500),
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_module (module),
    INDEX idx_action (action),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
);
```

---

### 9. ai_qa_log — AI 问答日志表

记录每次 AI 问答的完整上下文。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| user_id | VARCHAR(50) | NOT NULL | 用户标识 |
| question | TEXT | NOT NULL | 用户问题 |
| answer | TEXT | NOT NULL | AI 回答 |
| source | VARCHAR(20) | DEFAULT 'AI' | AI / LOCAL_KB / GREETING / FALLBACK / AI_STREAM / ERROR |
| response_time | INT | | 响应时间（ms） |
| create_time | DATETIME | DEFAULT NOW() | |
| citations | TEXT | | 引用来源（JSON 数组） |

```sql
CREATE TABLE ai_qa_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    source VARCHAR(20) DEFAULT 'AI',
    response_time INT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    citations TEXT,
    INDEX idx_user_id (user_id),
    INDEX idx_source (source),
    INDEX idx_create_time (create_time)
);
```

---

### 10. kb_document — 知识库文档表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| title | VARCHAR(200) | NOT NULL | 文档标题 |
| description | VARCHAR(1000) | | 文档描述 |
| file_name | VARCHAR(200) | NOT NULL | 原始文件名 |
| file_type | VARCHAR(50) | NOT NULL | PDF / DOCX / XLSX / TXT / MD |
| file_size | BIGINT | NOT NULL | 文件大小（字节） |
| file_url | VARCHAR(500) | | MinIO 对象路径 |
| chunk_count | INT | DEFAULT 0 | 分块数量 |
| status | VARCHAR(20) | DEFAULT 'PENDING' | PENDING → PROCESSING → COMPLETED / FAILED |
| error_msg | VARCHAR(500) | | 处理失败原因 |
| create_time | DATETIME | DEFAULT NOW() | |
| update_time | DATETIME | ON UPDATE NOW() | |

```sql
CREATE TABLE kb_document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    file_name VARCHAR(200) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_size BIGINT NOT NULL,
    file_url VARCHAR(500),
    chunk_count INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'PENDING',
    error_msg VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_kb_doc_status (status),
    INDEX idx_kb_doc_create_time (create_time)
);
```

---

### 11. kb_chunk — 文档分块表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| document_id | BIGINT | NOT NULL | 关联 `kb_document.id` |
| chunk_index | INT | NOT NULL | 段落序号（0 起始） |
| content | TEXT | NOT NULL | 段落文本 |
| token_count | INT | | Token 数量 |
| create_time | DATETIME | DEFAULT NOW() | |

每个分块对应 Milvus 中的一条向量记录。删除文档时同步清理向量和分块。

---

### 12. sys_role — 角色表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| role_code | VARCHAR(50) | UNIQUE NOT NULL | ADMIN / TEACHER / STUDENT |
| role_name | VARCHAR(100) | NOT NULL | 管理员 / 宿管老师 / 学生 |
| description | VARCHAR(500) | | 描述 |
| status | VARCHAR(20) | DEFAULT 'ACTIVE' | |
| create_time | DATETIME | DEFAULT NOW() | |
| update_time | DATETIME | ON UPDATE NOW() | |

---

### 13. sys_menu — 菜单权限表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| parent_id | BIGINT | DEFAULT 0 | 父菜单 ID，0 = 顶级 |
| menu_name | VARCHAR(50) | NOT NULL | 菜单名称 |
| menu_type | VARCHAR(10) | NOT NULL | MENU（目录）/ PAGE（页面）/ BUTTON（按钮） |
| path | VARCHAR(200) | | 前端路由路径 |
| icon | VARCHAR(50) | | Ant Design 图标名 |
| permission_code | VARCHAR(100) | | 权限标识，如 `student:view` |
| sort_order | INT | DEFAULT 0 | 排序 |
| visible | TINYINT | DEFAULT 1 | 是否可见 |

---

### 14. sys_role_menu — 角色菜单关联表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 主键 |
| role_id | BIGINT | NOT NULL | 角色 ID |
| menu_id | BIGINT | NOT NULL | 菜单 ID |

N:M 中间表，实现角色-菜单的灵活权限分配。

---

## 种子数据

### 收费项目

```sql
INSERT INTO fee_item (item_name, fee_type, unit_price, billing_cycle, applicable_dorm_type, status, description) VALUES
('住宿费', '住宿费', 1200.00, 'SEMESTER', 'ALL', 'ACTIVE', '标准住宿费，按学期收取'),
('水费', '水费', 5.00, 'MONTHLY', 'ALL', 'ACTIVE', '冷水费，按吨计费'),
('电费', '电费', 0.60, 'MONTHLY', 'ALL', 'ACTIVE', '电费按度收取'),
('空调费', '空调费', 300.00, 'SEMESTER', 'ALL', 'ACTIVE', '空调使用费，按学期收取'),
('网络费', '网络费', 50.00, 'MONTHLY', 'ALL', 'ACTIVE', '校园网络使用费'),
('热水费', '热水费', 15.00, 'MONTHLY', 'ALL', 'ACTIVE', '热水费按吨计费');
```

### 宿舍信息

```sql
INSERT INTO dormitory (dormitory_no, building, floor, room_type, capacity, status) VALUES
('A-101', 'A栋', '1层', '四人间', 4, 'ACTIVE'),
('A-102', 'A栋', '1层', '四人间', 4, 'ACTIVE'),
('B-201', 'B栋', '2层', '双人间', 2, 'ACTIVE'),
('B-202', 'B栋', '2层', '双人间', 2, 'ACTIVE'),
('C-301', 'C栋', '3层', '单人间', 1, 'ACTIVE'),
('C-302', 'C栋', '3层', '单人间', 1, 'ACTIVE'),
('A-201', 'A栋', '2层', '四人间', 4, 'ACTIVE'),
('B-101', 'B栋', '1层', '四人间', 4, 'ACTIVE');
```

> 测试数据包含 200+ 宿舍（A-J 栋，分布在多个楼层）。

### 角色

| id | role_code | role_name | 说明 |
|----|-----------|-----------|------|
| 1 | ADMIN | 管理员 | 全部权限 |
| 2 | TEACHER | 宿管老师 | 管理权限（学生/宿舍/收费） |
| 3 | STUDENT | 学生 | 仅查看个人信息和账单 |

### 默认用户账号

由 `DataInitializer` 在启动时自动创建（`app.init.enabled=true`）：

| 用户名 | 密码 | 角色 | 姓名 |
|--------|------|------|------|
| admin | 123456 | ADMIN | 系统管理员 |
| teacher | 123456 | TEACHER | 王老师 |

学生账号在学生管理新增/导入时自动创建（用户名 = 学号，密码 = 123456）。

### 菜单树

```
首页           /dashboard          DashboardOutlined
学生管理        /students           UserOutlined
收费管理        (目录)              MoneyCollectOutlined
  ├── 收费项目管理 /fee-items       MoneyCollectOutlined
  ├── 账单管理    /bills            FileTextOutlined
  └── 支付管理    /payment          PayCircleOutlined
宿舍管理        /dormitories        HomeOutlined
统计报表        /statistics         BarChartOutlined
AI 智能        (目录)              RobotOutlined
  ├── AI 问答    /ai-qa             RobotOutlined
  └── 知识库     /knowledge-base    BookOutlined
系统管理        (目录)              SettingOutlined
  ├── 用户管理    /users             TeamOutlined
  ├── 角色管理    /roles             SafetyOutlined
  └── 操作日志    /logs              FileSearchOutlined
```

完整菜单数据（16 条）：

```sql
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, icon, permission_code, sort_order, visible) VALUES
(1,  0,  '首页',       'MENU', '/dashboard',    'DashboardOutlined',   'dashboard:view',    1, 1),
(2,  0,  '学生管理',    'MENU', '/students',     'UserOutlined',        'student:view',      1, 1),
(3,  0,  '宿舍管理',    'MENU', '/dormitories',  'HomeOutlined',        'dormitory:view',    2, 1),
(16, 0,  '收费管理',    'MENU', '/fee-management','MoneyCollectOutlined','fee-mgmt:view',     3, 1),
(4,  16, '收费项目管理', 'MENU', '/fee-items',    'MoneyCollectOutlined','fee:view',          1, 1),
(5,  16, '账单管理',    'MENU', '/bills',        'FileTextOutlined',    'bill:view',         2, 1),
(6,  16, '支付管理',    'MENU', '/payment',      'PayCircleOutlined',   'payment:view',      3, 1),
(7,  0,  '统计报表',    'MENU', '/statistics',   'BarChartOutlined',    'statistics:view',   4, 1),
(8,  0,  '我的账单',    'MENU', '/my-bills',     'FileDoneOutlined',    'mybill:view',       5, 1),
(17, 0,  'AI智能',     'MENU', '/ai-service',   'RobotOutlined',       'ai-svc:view',       6, 1),
(13, 17, 'AI问答',     'MENU', '/ai-qa',        'RobotOutlined',       'ai:view',           1, 1),
(14, 17, '知识库',     'MENU', '/knowledge-base','BookOutlined',        'kb:view',           2, 1),
(9,  0,  '系统管理',    'MENU', '/system',       'SettingOutlined',     'system:view',       7, 1),
(10, 9,  '用户管理',    'MENU', '/users',        'TeamOutlined',        'user:view',         1, 1),
(11, 9,  '角色管理',    'MENU', '/roles',        'SafetyOutlined',      'role:view',         2, 1),
(18, 9,  '操作日志',    'PAGE', '/logs',         'FileSearchOutlined',  'log:view',          3, 1);
```

---

## 索引策略

| 表 | 索引 | 类型 | 用途 |
|----|------|------|------|
| sys_user | username | INDEX | 登录查询、模糊搜索 |
| sys_user | role | INDEX | 按角色筛选用户 |
| student_dormitory | student_no | INDEX | 学号搜索、模糊查询 |
| student_dormitory | dormitory_no | INDEX | 按宿舍筛选学生 |
| student_dormitory | user_id | INDEX | 用户关联查询、人员管理 |
| student_dormitory | payment_status | INDEX | 缴费状态筛选 |
| dormitory | dormitory_no | UNIQUE | 宿舍号唯一约束 + 精确查询 |
| dormitory | building | INDEX | 按楼栋筛选 |
| dormitory | status | INDEX | 按启用状态筛选 |
| fee_item | fee_type | INDEX | 收费类型筛选 |
| fee_item | status | INDEX | 按启用状态筛选 |
| payment_bill | student_id | INDEX | 学生账单查询 |
| payment_bill | semester | INDEX | 学期统计、报表 |
| payment_bill | status | INDEX | 按状态筛选 |
| payment_bill | bill_no | INDEX | 账单号精确查询 |
| payment_record | bill_id | INDEX | 账单支付记录 |
| payment_record | order_no | INDEX | 订单号查询、回调处理 |
| payment_record | student_id | INDEX | 学生支付历史 |
| notification_record | student_id | INDEX | 学生通知列表 |
| notification_record | notify_type | INDEX | 按通知类型查询 |
| notification_record | status | INDEX | 发送状态筛选 |
| notification_record | create_time | INDEX | 时间范围筛选 |
| operation_log | username | INDEX | 按操作人筛选 |
| operation_log | module | INDEX | 按模块筛选 |
| operation_log | action | INDEX | 按操作类型筛选 |
| operation_log | status | INDEX | 成功/失败筛选 |
| operation_log | create_time | INDEX | 时间范围查询、排序 |
| ai_qa_log | user_id | INDEX | 用户问答历史 |
| ai_qa_log | source | INDEX | 按来源统计 |
| ai_qa_log | create_time | INDEX | 时间排序 |
| kb_document | status | INDEX | 处理状态筛选 |
| kb_document | create_time | INDEX | 时间排序 |

---

## 数据量级参考（测试环境）

| 表 | 约记录数 |
|----|---------|
| sys_user | ~1007 |
| student_dormitory | ~1005 |
| dormitory | ~208 |
| fee_item | 6 |
| payment_bill | ~7000 |
| payment_record | ~20 |
| operation_log | ~10 |
| ai_qa_log | 28 |
| kb_document | 6 |
| kb_chunk | ~28 |

---

## 技术栈版本

| 组件 | 版本 |
|------|------|
| MySQL | 8.0.45 |
| MyBatis-Plus | 3.5.6 |
| Spring Boot | 3.2.5 |
| Redis | 7.x（端口 16379） |
| RabbitMQ | 3.x（端口 5672） |
| MinIO | latest（端口 9000） |
| Milvus | 2.x（端口 19530） |
