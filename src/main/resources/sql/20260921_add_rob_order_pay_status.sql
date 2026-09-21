-- =============================================
-- 增量升级脚本：20260921_add_rob_order_pay_status.sql
-- 变更：add-rob-order-receipt-payback
--   为 t_rob_order 新增 5 列：收款回款状态机（pay_status）+ 4 个审计字段
--   （确认收款/回款操作人ID与时间）。
--   收款/回款为独立于下单/取消业务流的资金确认动作，落订单表维度状态字段，
--   不进 t_rob_order_operate_log 事件日志表（不扩展 RobOrderOperateType 枚举）。
-- 幂等性：通过 information_schema 判断列存在与否，可重复执行（MySQL 8+）
-- =============================================

-- ---------- 1. 新增列 pay_status（收款回款状态：0未收款/1已收款/2已回款/3无效）----------
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME   = 't_rob_order'
        AND COLUMN_NAME  = 'pay_status') = 0,
    'ALTER TABLE `t_rob_order` ADD COLUMN `pay_status` TINYINT NOT NULL DEFAULT 0 COMMENT ''收款回款状态：0未收款/1已收款/2已回款/3无效（与order_status解耦，正向0→1→2不可逆，取消订单时统一置3）'' AFTER `order_status`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- 2. 新增列 receipt_operate_user_id（确认收款操作人ID）----------
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME   = 't_rob_order'
        AND COLUMN_NAME  = 'receipt_operate_user_id') = 0,
    'ALTER TABLE `t_rob_order` ADD COLUMN `receipt_operate_user_id` BIGINT DEFAULT NULL COMMENT ''确认收款操作人ID（关联sys_user.id，确认收款时写入，取消订单时保留不清空）'' AFTER `pay_status`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- 3. 新增列 receipt_operate_time（确认收款时间）----------
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME   = 't_rob_order'
        AND COLUMN_NAME  = 'receipt_operate_time') = 0,
    'ALTER TABLE `t_rob_order` ADD COLUMN `receipt_operate_time` DATETIME DEFAULT NULL COMMENT ''确认收款时间（确认收款动作发生时写入）'' AFTER `receipt_operate_user_id`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- 4. 新增列 payback_operate_user_id（确认回款操作人ID）----------
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME   = 't_rob_order'
        AND COLUMN_NAME  = 'payback_operate_user_id') = 0,
    'ALTER TABLE `t_rob_order` ADD COLUMN `payback_operate_user_id` BIGINT DEFAULT NULL COMMENT ''确认回款操作人ID（关联sys_user.id，确认回款时写入，取消订单时保留不清空）'' AFTER `receipt_operate_time`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- 5. 新增列 payback_operate_time（确认回款时间）----------
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME   = 't_rob_order'
        AND COLUMN_NAME  = 'payback_operate_time') = 0,
    'ALTER TABLE `t_rob_order` ADD COLUMN `payback_operate_time` DATETIME DEFAULT NULL COMMENT ''确认回款时间（确认回款动作发生时写入）'' AFTER `payback_operate_user_id`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
