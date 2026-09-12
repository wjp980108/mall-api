-- =============================================
-- add-rob-order-flow：订单流水（事件算账读模型）
-- 为操作审计表增加按事件发生日区间查询的复合索引
-- 新库已在 rbac_schema.sql 建表语句中包含；存量库执行本语句
-- =============================================
ALTER TABLE `t_rob_order_operate_log`
    ADD INDEX `idx_flow_event` (`create_time`, `operate_type`) COMMENT '订单流水按事件日区间查询/事件类型聚合';

-- =============================================
-- 上线前历史完整性核对（手工执行，不随部署自动跑）：
-- 1) 总数核对：两值应相等
--    SELECT COUNT(1) FROM t_rob_order WHERE is_deleted = 0;
--    SELECT COUNT(DISTINCT order_id) FROM t_rob_order_operate_log WHERE operate_type = 1;
-- 2) 反查缺失下单事件的订单（应为空集）：
--    SELECT o.id, o.order_no, o.create_time
--    FROM t_rob_order o
--    LEFT JOIN t_rob_order_operate_log l
--      ON l.order_id = o.id AND l.operate_type = 1
--    WHERE o.is_deleted = 0 AND l.id IS NULL;
-- =============================================
