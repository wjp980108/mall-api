-- =============================================
-- 增量升级脚本：20260919_add_rob_order_log_prev_buyer_snapshot.sql
-- 变更：fix-rob-order-transfer-flow
--   t_rob_order_operate_log 新增转移事件原买家/原推荐人快照 4 列：
--     prev_buyer_id     BIGINT      NULL  转移前买家ID
--     prev_buyer_name   VARCHAR(64) NULL  转移前买家名称快照
--     prev_buyer_phone  VARCHAR(32) NULL  转移前买家手机号快照
--     prev_inviter_id   BIGINT      NULL  转移前推荐人ID
--   仅转移事件(operate_type=3)写值；下单/取消事件落 NULL；存量历史行保持 NULL（查询层 COALESCE 兜底，不回填）
-- 幂等性：通过 information_schema 判断列存在与否，可重复执行（MySQL 8+）
-- =============================================

-- ---------- 1. t_rob_order_operate_log.prev_buyer_id ----------
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME   = 't_rob_order_operate_log'
        AND COLUMN_NAME  = 'prev_buyer_id') = 0,
    'ALTER TABLE `t_rob_order_operate_log` ADD COLUMN `prev_buyer_id` BIGINT DEFAULT NULL COMMENT ''转移前买家ID(仅转移事件快照)'' AFTER `payment_amount`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- 2. t_rob_order_operate_log.prev_buyer_name ----------
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME   = 't_rob_order_operate_log'
        AND COLUMN_NAME  = 'prev_buyer_name') = 0,
    'ALTER TABLE `t_rob_order_operate_log` ADD COLUMN `prev_buyer_name` VARCHAR(64) DEFAULT NULL COMMENT ''转移前买家名称快照(仅转移事件)'' AFTER `prev_buyer_id`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- 3. t_rob_order_operate_log.prev_buyer_phone ----------
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME   = 't_rob_order_operate_log'
        AND COLUMN_NAME  = 'prev_buyer_phone') = 0,
    'ALTER TABLE `t_rob_order_operate_log` ADD COLUMN `prev_buyer_phone` VARCHAR(32) DEFAULT NULL COMMENT ''转移前买家手机号快照(仅转移事件)'' AFTER `prev_buyer_name`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- 4. t_rob_order_operate_log.prev_inviter_id ----------
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME   = 't_rob_order_operate_log'
        AND COLUMN_NAME  = 'prev_inviter_id') = 0,
    'ALTER TABLE `t_rob_order_operate_log` ADD COLUMN `prev_inviter_id` BIGINT DEFAULT NULL COMMENT ''转移前推荐人ID(仅转移事件快照)'' AFTER `prev_buyer_phone`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
