-- =============================================
-- 增量升级脚本：20260918_add_rob_order_prev_payment_amount.sql
-- 变更：fix-rob-order-cancel-payment-rollback
--   1. t_rob_order 新增 prev_payment_amount 下单时点商品付款金额旧值快照（单价口径，取消链式回滚基数）
--   2. t_rob_order 新增 idx_goods_id 索引（取消回滚目标查询按 goods_id 过滤 + id 排序）
-- 幂等性：通过 information_schema 判断列/索引存在与否，可重复执行（MySQL 8+）
-- =============================================

-- ---------- 1. t_rob_order.prev_payment_amount ----------
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME   = 't_rob_order'
        AND COLUMN_NAME  = 'prev_payment_amount') = 0,
    'ALTER TABLE `t_rob_order` ADD COLUMN `prev_payment_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT ''下单时点回写前的商品付款金额旧值(单价口径)，取消链式回滚基数'' AFTER `unit_price`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- 2. t_rob_order.idx_goods_id ----------
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME   = 't_rob_order'
        AND INDEX_NAME   = 'idx_goods_id') = 0,
    'ALTER TABLE `t_rob_order` ADD INDEX `idx_goods_id` (`goods_id`) COMMENT ''按商品查询(取消付款金额链式回滚)''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
