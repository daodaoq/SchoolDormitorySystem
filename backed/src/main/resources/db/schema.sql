-- ============================================
-- 学生宿舍收费管理系统 - 数据库初始化脚本
-- Database: school_dormitory / MySQL 8.0+
-- 用法: mysql -u root -p < schema.sql
-- ============================================

CREATE DATABASE IF NOT EXISTS school_dormitory DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;
USE school_dormitory;

-- ==================== 删表(按依赖顺序) ====================
DROP TABLE IF EXISTS operation_log;
DROP TABLE IF EXISTS ai_qa_log;
DROP TABLE IF EXISTS notification_record;
DROP TABLE IF EXISTS payment_record;
DROP TABLE IF EXISTS payment_bill;
DROP TABLE IF EXISTS fee_item;
DROP TABLE IF EXISTS student_dormitory;

-- ==================== 1. 学生宿舍信息表 ====================
CREATE TABLE student_dormitory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    student_name VARCHAR(50) NOT NULL COMMENT '学生姓名',
    student_no VARCHAR(20) NOT NULL COMMENT '学号',
    dormitory_no VARCHAR(20) NOT NULL COMMENT '宿舍号',
    phone VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    check_in_date DATE NOT NULL COMMENT '入住时间',
    payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID' COMMENT '缴费状态',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_student_no (student_no),
    INDEX idx_dormitory_no (dormitory_no),
    INDEX idx_payment_status (payment_status),
    INDEX idx_student_name (student_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生宿舍信息表';

-- ==================== 2. 收费项目表 ====================
CREATE TABLE fee_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    item_name VARCHAR(100) NOT NULL COMMENT '收费项目名称',
    fee_type VARCHAR(30) NOT NULL COMMENT '收费类型',
    unit_price DECIMAL(10,2) NOT NULL COMMENT '收费单价',
    billing_cycle VARCHAR(20) NOT NULL COMMENT '计费周期',
    applicable_dorm_type VARCHAR(50) DEFAULT 'ALL' COMMENT '适用宿舍类型',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
    description VARCHAR(500) DEFAULT NULL COMMENT '收费说明',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_fee_type (fee_type),
    INDEX idx_status (status),
    INDEX idx_billing_cycle (billing_cycle)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收费项目表';

-- ==================== 3. 缴费账单表 ====================
CREATE TABLE payment_bill (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    fee_item_id BIGINT NOT NULL COMMENT '收费项目ID',
    bill_no VARCHAR(50) NOT NULL COMMENT '账单编号',
    semester VARCHAR(20) NOT NULL COMMENT '所属学期',
    amount DECIMAL(10,2) NOT NULL COMMENT '应缴金额',
    paid_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '已缴金额',
    due_date DATE NOT NULL COMMENT '缴费截止日期',
    status VARCHAR(20) NOT NULL DEFAULT 'UNPAID' COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_bill_no (bill_no),
    INDEX idx_student_id (student_id),
    INDEX idx_semester (semester),
    INDEX idx_status (status),
    INDEX idx_due_date (due_date),
    INDEX idx_student_semester (student_id, semester)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='缴费账单表';

-- ==================== 4. 支付流水表 ====================
CREATE TABLE payment_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    bill_id BIGINT NOT NULL COMMENT '账单ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    amount DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    pay_method VARCHAR(20) NOT NULL DEFAULT 'ALIPAY' COMMENT '支付方式',
    trade_no VARCHAR(64) DEFAULT NULL COMMENT '第三方交易号',
    pay_time DATETIME DEFAULT NULL COMMENT '支付时间',
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING' COMMENT '状态',
    receipt_url VARCHAR(500) DEFAULT NULL COMMENT '电子凭证URL',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_order_no (order_no),
    INDEX idx_bill_id (bill_id),
    INDEX idx_student_id_pay (student_id),
    INDEX idx_pay_time (pay_time),
    INDEX idx_status (status),
    INDEX idx_trade_no (trade_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付流水表';

-- ==================== 5. 通知记录表 ====================
CREATE TABLE notification_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    bill_id BIGINT DEFAULT NULL COMMENT '关联账单ID',
    notify_type VARCHAR(30) NOT NULL COMMENT '通知类型',
    channel VARCHAR(20) NOT NULL COMMENT '通知渠道',
    recipient VARCHAR(200) NOT NULL COMMENT '接收方',
    title VARCHAR(200) NOT NULL COMMENT '通知标题',
    content TEXT NOT NULL COMMENT '通知内容',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '发送状态',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    send_time DATETIME DEFAULT NULL COMMENT '发送时间',
    fail_reason VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_student_id (student_id),
    INDEX idx_notify_type (notify_type),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知记录表';

-- ==================== 6. AI问答日志表 ====================
CREATE TABLE ai_qa_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id VARCHAR(50) NOT NULL COMMENT '用户标识',
    question TEXT NOT NULL COMMENT '用户问题',
    answer TEXT NOT NULL COMMENT 'AI回答',
    source VARCHAR(20) NOT NULL DEFAULT 'AI' COMMENT '答案来源',
    response_time INT DEFAULT NULL COMMENT '响应时间(ms)',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_source (source),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI问答日志表';

-- ==================== 7. 操作日志表 ====================
CREATE TABLE operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    operator VARCHAR(50) NOT NULL COMMENT '操作人',
    module VARCHAR(50) NOT NULL COMMENT '操作模块',
    action VARCHAR(50) NOT NULL COMMENT '操作动作',
    target_id BIGINT DEFAULT NULL COMMENT '操作目标ID',
    detail TEXT DEFAULT NULL COMMENT '操作详情',
    ip_address VARCHAR(50) DEFAULT NULL COMMENT '操作IP',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX idx_create_time (create_time),
    INDEX idx_module (module),
    INDEX idx_operator (operator)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- ==================== 种子数据 ====================
INSERT INTO fee_item (item_name, fee_type, unit_price, billing_cycle, applicable_dorm_type, status, description) VALUES
('住宿费', 'ACCOMMODATION', 1200.00, 'SEMESTER', 'ALL', 'ACTIVE', '标准住宿费，按学期收取'),
('水费', 'WATER', 5.00, 'MONTHLY', 'ALL', 'ACTIVE', '冷水费，按吨计费'),
('电费', 'ELECTRICITY', 0.60, 'MONTHLY', 'ALL', 'ACTIVE', '电费按度收取'),
('空调费', 'AC', 300.00, 'SEMESTER', 'ALL', 'ACTIVE', '空调使用费，按学期收取'),
('网络费', 'NETWORK', 50.00, 'MONTHLY', 'ALL', 'ACTIVE', '校园网络使用费');

INSERT INTO student_dormitory (student_name, student_no, dormitory_no, phone, check_in_date, payment_status) VALUES
('张三', '2024001', 'A-101', '13800138001', '2024-09-01', 'PAID'),
('李四', '2024002', 'A-102', '13800138002', '2024-09-01', 'UNPAID'),
('王五', '2024003', 'B-201', '13800138003', '2024-09-01', 'OVERDUE'),
('赵六', '2024004', 'B-202', '13800138004', '2024-09-01', 'UNPAID'),
('陈七', '2024005', 'C-301', '13800138005', '2024-09-01', 'PAID');

INSERT INTO payment_bill (student_id, fee_item_id, bill_no, semester, amount, paid_amount, due_date, status) VALUES
(1, 1, 'BILL20240901001', '2024-1', 1200.00, 1200.00, '2024-10-01', 'PAID'),
(2, 1, 'BILL20240901002', '2024-1', 1200.00, 0.00, '2024-10-01', 'UNPAID'),
(3, 1, 'BILL20240901003', '2024-1', 1200.00, 0.00, '2024-09-15', 'OVERDUE'),
(2, 2, 'BILL20240901004', '2024-1', 50.00, 0.00, '2024-10-01', 'UNPAID'),
(4, 3, 'BILL20240901005', '2024-1', 120.00, 0.00, '2024-10-01', 'UNPAID'),
(5, 1, 'BILL20240901006', '2024-1', 1200.00, 1200.00, '2024-10-01', 'PAID');

INSERT INTO payment_record (bill_id, student_id, order_no, amount, pay_method, trade_no, pay_time, status) VALUES
(1, 1, 'PAY20240901001', 1200.00, 'ALIPAY', 'TRADE20240901001', '2024-09-05 10:30:00', 'SUCCESS'),
(6, 5, 'PAY20240901002', 1200.00, 'ALIPAY', 'TRADE20240901002', '2024-09-06 14:20:00', 'SUCCESS');
