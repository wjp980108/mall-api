-- =============================================
-- 增量升级脚本：20260915_add_payment_amount_and_receipt_amount.sql
-- 变更：add-goods-payment-flow-payback
--   1. t_consign_goods 新增 payment_amount 付款金额（单价口径）
--   2. t_rob_order_operate_log 新增 receipt_amount 回款金额（事件维度快照）
-- 幂等性：通过 information_schema 判断列存在与否，可重复执行（MySQL 8+）
-- =============================================

-- ---------- 1. t_consign_goods.payment_amount ----------
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME   = 't_consign_goods'
        AND COLUMN_NAME  = 'payment_amount') = 0,
    'ALTER TABLE `t_consign_goods` ADD COLUMN `payment_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT ''付款金额(单价口径)：当前轮次买入单价，抢购成交后回写为成交单价'' AFTER `goods_price`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- 2. t_rob_order_operate_log.receipt_amount ----------
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME   = 't_rob_order_operate_log'
        AND COLUMN_NAME  = 'receipt_amount') = 0,
    'ALTER TABLE `t_rob_order_operate_log` ADD COLUMN `receipt_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT ''回款金额：下单事件快照(自购奖金+付款金额×数量)，取消事件存同值正数，转移为0'' AFTER `operate_user_name`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
