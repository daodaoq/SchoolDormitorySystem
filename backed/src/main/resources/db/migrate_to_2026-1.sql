-- ============================================
-- 迁移脚本：将种子数据从 2024-1 学期更新为 2026-1
-- 用法: mysql -u root -p school_dormitory < migrate_to_2026-1.sql
-- 日期: 2026-06-29
-- ============================================

USE school_dormitory;

-- 1. 更新账单学期和截止日期
UPDATE payment_bill SET
    semester = '2026-1',
    bill_no = REPLACE(bill_no, '20240901', '20260901'),
    due_date = DATE_ADD(due_date, INTERVAL 2 YEAR)  -- 2024 → 2026
WHERE semester = '2024-1';

-- 2. 更新支付记录的交易号和支付时间
UPDATE payment_record SET
    order_no = REPLACE(order_no, '20240901', '20260901'),
    trade_no = REPLACE(trade_no, '20240901', '20260901'),
    pay_time = DATE_ADD(pay_time, INTERVAL 2 YEAR)   -- 2024 → 2026
WHERE order_no LIKE 'PAY20240901%';

-- 验证结果
SELECT '=== 账单数据 ===' AS info;
SELECT id, bill_no, semester, due_date, amount, paid_amount, status FROM payment_bill;

SELECT '=== 支付记录 ===' AS info;
SELECT id, order_no, trade_no, pay_time, amount, status FROM payment_record;

SELECT '=== Dashboard 概览 (按当前学期 2026-1) ===' AS info;
SELECT
    COUNT(*) AS totalBills,
    SUM(amount) AS totalAmount,
    SUM(paid_amount) AS paidAmount,
    ROUND(SUM(paid_amount) / SUM(amount) * 100, 2) AS collectionRate,
    SUM(CASE WHEN status = 'OVERDUE' THEN 1 ELSE 0 END) AS overdueCount,
    SUM(CASE WHEN status = 'UNPAID' THEN 1 ELSE 0 END) AS unpaidCount
FROM payment_bill
WHERE semester = '2026-1';
