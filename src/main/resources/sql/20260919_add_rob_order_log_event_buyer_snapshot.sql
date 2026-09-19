-- =============================================
-- 增量升级脚本：20260919_add_rob_order_log_event_buyer_snapshot.sql
-- 变更：fix-rob-order-flow-group-by-order
--   t_rob_order_operate_log 新增"事件发生时买家"快照 3 列：
--     buyer_id     BIGINT      NULL  事件买家ID（下单=下单买家/取消=取消时买家/转移=新买家）
--     buyer_name   VARCHAR(64) NULL  事件买家名称快照
--     buyer_phone  VARCHAR(32) NULL  事件买家手机号快照
--   与既有 prev_buyer_* 语义区分：prev_buyer_* 仅转移红冲行用（原买家）；
--   新三列供下单行/取消行/转移正向行使用，防止订单转移后历史行买家漂移为订单当前买家。
--   存量历史行三列保持 NULL（查询层 COALESCE 订单当前买家兜底，不回填）。
-- 幂等性：通过 information_schema 判断列存在与否，可重复执行（MySQL 8+）
-- =============================================

-- ---------- 1. t_rob_order_operate_log.buyer_id ----------
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME   = 't_rob_order_operate_log'
        AND COLUMN_NAME  = 'buyer_id') = 0,
    'ALTER TABLE `t_rob_order_operate_log` ADD COLUMN `buyer_id` BIGINT DEFAULT NULL COMMENT ''事件买家ID(下单=下单买家/取消=取消时买家/转移=新买家)'' AFTER `payment_amount`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- 2. t_rob_order_operate_log.buyer_name ----------
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME   = 't_rob_order_operate_log'
        AND COLUMN_NAME  = 'buyer_name') = 0,
    'ALTER TABLE `t_rob_order_operate_log` ADD COLUMN `buyer_name` VARCHAR(64) DEFAULT NULL COMMENT ''事件买家名称快照(下单/取消/转移正向行买家)'' AFTER `buyer_id`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- 3. t_rob_order_operate_log.buyer_phone ----------
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME   = 't_rob_order_operate_log'
        AND COLUMN_NAME  = 'buyer_phone') = 0,
    'ALTER TABLE `t_rob_order_operate_log` ADD COLUMN `buyer_phone` VARCHAR(32) DEFAULT NULL COMMENT ''事件买家手机号快照(下单/取消/转移正向行买家)'' AFTER `buyer_name`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
