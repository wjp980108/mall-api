-- =============================================
-- 增量升级脚本：20260921_add_receipt_round_amount.sql
-- 变更：enhance-rob-order-flow-summary
--   为 t_rob_order_operate_log 新增 receipt_round_amount 回款金额取整列（整数口径），
--   并对存量行按 receipt_amount 四舍五入回填。
-- 幂等性：通过 information_schema 判断列存在与否，可重复执行（MySQL 8+）
-- =============================================

-- ---------- 1. 新增列 receipt_round_amount ----------
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME   = 't_rob_order_operate_log'
        AND COLUMN_NAME  = 'receipt_round_amount') = 0,
    'ALTER TABLE `t_rob_order_operate_log` ADD COLUMN `receipt_round_amount` DECIMAL(12,0) NOT NULL DEFAULT 0 COMMENT ''回款金额取整值(四舍五入整数)：下单事件对回款金额HALF_UP取整，取消/转移事件复制下单行同值'' AFTER `receipt_amount`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- 2. 存量数据回填：取整回款 = ROUND(原回款)，仅回填未取整(默认0)且原回款非0的行 ----------
UPDATE `t_rob_order_operate_log`
SET `receipt_round_amount` = ROUND(`receipt_amount`)
WHERE `receipt_round_amount` = 0
  AND `receipt_amount` <> 0;
