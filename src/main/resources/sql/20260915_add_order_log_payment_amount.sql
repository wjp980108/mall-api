-- =============================================
-- 增量升级脚本：20260915_add_order_log_payment_amount.sql
-- 变更：add-goods-payment-flow-payback（第 7 组）
--   t_rob_order_operate_log 新增 payment_amount 付款金额（本金总额口径：商品付款单价×数量）
--   与 receipt_amount 同构的事件维度快照：下单事件快照本金，取消事件存同值正数，转移为0
--   注意：本列是总额口径(P×N)，与 t_consign_goods.payment_amount 的单价口径(P)同名异口径
-- 幂等性：通过 information_schema 判断列存在与否，可重复执行（MySQL 8+）
-- =============================================

SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME   = 't_rob_order_operate_log'
        AND COLUMN_NAME  = 'payment_amount') = 0,
    'ALTER TABLE `t_rob_order_operate_log` ADD COLUMN `payment_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT ''付款金额(本金总额口径)：下单事件快照(商品付款单价×数量)，取消事件存同值正数，转移为0；注意与t_consign_goods.payment_amount单价口径区分'' AFTER `receipt_amount`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
