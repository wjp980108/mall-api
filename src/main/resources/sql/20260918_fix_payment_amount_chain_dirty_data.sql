-- =============================================
-- 历史脏数据订正脚本：20260918_fix_payment_amount_chain_dirty_data.sql
-- 变更：fix-rob-order-cancel-payment-rollback
-- 背景：2026-09-15 付款金额/回款功能上线后，取消订单未回滚 t_consign_goods.payment_amount，
--      导致"下单→取消→重抢同一商品"时重单行本金/回款取了已取消订单单价（生产实测 712→10400）。
--
-- 【人工执行，勿随应用启动】执行流程：
--   1. 先在 dev 库事务内执行全部预览 SELECT + UPDATE 并核对结果；
--   2. 生产执行前务必备份：
--        CREATE TABLE bak_consign_goods_20260918 AS SELECT * FROM t_consign_goods;
--        CREATE TABLE bak_rob_order_operate_log_20260918 AS SELECT * FROM t_rob_order_operate_log;
--   3. 每段先跑【预览】SELECT 核对差异行，确认后再执行对应【订正】UPDATE；
--   4. 全部语句幂等，可重复执行，二次执行预览/影响行数应为 0。
-- 重放口径：订单当前 order_status=1 视为有效成交（取消视为该轮从未成交），
--          与修复后取消链式回滚的反事实语义一致；范围限定功能上线日 2026-09-15 起。
-- =============================================

-- #############################################################
-- # 第一段：校正商品当前付款金额指针 t_consign_goods.payment_amount
-- # 规则：存在有效订单(status=1)的商品 → 最新有效订单(max id)的 unit_price；
-- #       无有效订单的商品不触碰（保留管理端维护值）。
-- #############################################################

-- ---------- 预览 1-1（只查不改）：当前指针与最新有效成交单价不一致的商品 ----------
SELECT g.id AS goods_id, g.goods_name, g.payment_amount AS current_payment_amount,
       lv.unit_price AS expected_payment_amount, lv.id AS latest_valid_order_id
FROM t_consign_goods g
JOIN (
    SELECT o.goods_id, o.id, o.unit_price
    FROM t_rob_order o
    JOIN (SELECT goods_id, MAX(id) AS max_id
            FROM t_rob_order
           WHERE order_status = 1 AND is_deleted = 0
           GROUP BY goods_id) m ON m.max_id = o.id
    WHERE o.is_deleted = 0
) lv ON lv.goods_id = g.id
WHERE g.payment_amount <> lv.unit_price;

-- ---------- 【确认预览后执行】订正 1：校正指针（幂等，二次执行影响 0 行） ----------
UPDATE t_consign_goods g
JOIN (
    SELECT o.goods_id, o.id, o.unit_price
    FROM t_rob_order o
    JOIN (SELECT goods_id, MAX(id) AS max_id
            FROM t_rob_order
           WHERE order_status = 1 AND is_deleted = 0
           GROUP BY goods_id) m ON m.max_id = o.id
    WHERE o.is_deleted = 0
) lv ON lv.goods_id = g.id
SET g.payment_amount = lv.unit_price
WHERE g.payment_amount <> lv.unit_price;

-- #############################################################
-- # 第二段：重放订正事件行 t_rob_order_operate_log（仅 2026-09-15 起）
-- # 正确本金基数 = 同商品 id 更小且当前有效的最新订单 unit_price；
-- #             无更早有效订单 → 最早订单下单事件行已落本金÷数量（首轮读到的管理端真实值）。
-- # 下单行：payment = ROUND(基数×quantity,2)，receipt = 订单 self_buy_bonus_amount + payment；
-- # 取消行：两值与同订单下单行同值（正数原值，符号由流水查询层决定）。
-- # 用会话临时表承载重放结果，避免 MySQL 同表更新子查询限制，会话结束自动清理。
-- #############################################################

DROP TEMPORARY TABLE IF EXISTS tmp_payment_replay;
CREATE TEMPORARY TABLE tmp_payment_replay AS
SELECT o.id AS order_id,
       o.quantity,
       o.self_buy_bonus_amount AS bonus,
       ROUND(o.quantity * COALESCE(
           -- ① 更早的最新有效成交轮单价
           (SELECT p.unit_price
              FROM t_rob_order p
             WHERE p.goods_id = o.goods_id AND p.id < o.id
               AND p.order_status = 1 AND p.is_deleted = 0
             ORDER BY p.id DESC LIMIT 1),
           -- ② 首轮成交前基数：最早订单下单行已落本金 ÷ 数量
           (SELECT l0.payment_amount / NULLIF(e0.quantity, 0)
              FROM t_rob_order e0
              JOIN t_rob_order_operate_log l0
                ON l0.order_id = e0.id AND l0.operate_type = 1
             WHERE e0.goods_id = o.goods_id AND e0.is_deleted = 0
             ORDER BY e0.id ASC LIMIT 1),
           0), 2) AS correct_payment,
       ROUND(o.self_buy_bonus_amount + o.quantity * COALESCE(
           (SELECT p.unit_price
              FROM t_rob_order p
             WHERE p.goods_id = o.goods_id AND p.id < o.id
               AND p.order_status = 1 AND p.is_deleted = 0
             ORDER BY p.id DESC LIMIT 1),
           (SELECT l0.payment_amount / NULLIF(e0.quantity, 0)
              FROM t_rob_order e0
              JOIN t_rob_order_operate_log l0
                ON l0.order_id = e0.id AND l0.operate_type = 1
             WHERE e0.goods_id = o.goods_id AND e0.is_deleted = 0
             ORDER BY e0.id ASC LIMIT 1),
           0), 2) AS correct_receipt
FROM t_rob_order o
WHERE o.is_deleted = 0
  AND EXISTS (SELECT 1 FROM t_rob_order_operate_log l1
               WHERE l1.order_id = o.id AND l1.operate_type = 1
                 AND l1.create_time >= '2026-09-15 00:00:00');

-- ---------- 预览 2-1（只查不改）：需订正的下单事件行 ----------
SELECT l.id AS log_id, l.order_id, o.goods_id,
       l.payment_amount AS old_payment, r.correct_payment,
       l.receipt_amount AS old_receipt, r.correct_receipt
FROM t_rob_order_operate_log l
JOIN t_rob_order o ON o.id = l.order_id AND o.is_deleted = 0
JOIN tmp_payment_replay r ON r.order_id = l.order_id
WHERE l.operate_type = 1
  AND l.create_time >= '2026-09-15 00:00:00'
  AND (l.payment_amount <> r.correct_payment OR l.receipt_amount <> r.correct_receipt);

-- ---------- 【确认预览后执行】订正 2-1：重算下单事件行（幂等，二次执行影响 0 行） ----------
UPDATE t_rob_order_operate_log l
JOIN t_rob_order o ON o.id = l.order_id AND o.is_deleted = 0
JOIN tmp_payment_replay r ON r.order_id = l.order_id
SET l.payment_amount = r.correct_payment,
    l.receipt_amount = r.correct_receipt
WHERE l.operate_type = 1
  AND l.create_time >= '2026-09-15 00:00:00'
  AND (l.payment_amount <> r.correct_payment OR l.receipt_amount <> r.correct_receipt);

-- ---------- 预览 2-2（只查不改）：需同步的取消事件行（下单行已订正后） ----------
SELECT c.id AS cancel_log_id, c.order_id,
       c.payment_amount AS old_payment, p.payment_amount AS correct_payment,
       c.receipt_amount AS old_receipt, p.receipt_amount AS correct_receipt
FROM t_rob_order_operate_log c
JOIN t_rob_order_operate_log p
  ON p.order_id = c.order_id AND p.operate_type = 1
WHERE c.operate_type = 2
  AND c.create_time >= '2026-09-15 00:00:00'
  AND (c.payment_amount <> p.payment_amount OR c.receipt_amount <> p.receipt_amount);

-- ---------- 【确认预览后执行】订正 2-2：取消事件行同步同订单下单行两值（幂等） ----------
UPDATE t_rob_order_operate_log c
JOIN t_rob_order_operate_log p
  ON p.order_id = c.order_id AND p.operate_type = 1
SET c.payment_amount = p.payment_amount,
    c.receipt_amount = p.receipt_amount
WHERE c.operate_type = 2
  AND c.create_time >= '2026-09-15 00:00:00'
  AND (c.payment_amount <> p.payment_amount OR c.receipt_amount <> p.receipt_amount);

-- ---------- 收尾：清理会话临时表 ----------
DROP TEMPORARY TABLE IF EXISTS tmp_payment_replay;

-- ---------- 订正后对账（可选）：2026-09-18 当日流水成交/红冲付款本金与回款合计 ----------
SELECT CASE l.operate_type WHEN 1 THEN 'income' WHEN 2 THEN 'reversal' END AS grp,
       COUNT(*) AS cnt,
       SUM(o.total_amount) AS total_amount,
       SUM(l.payment_amount) AS payment_amount,
       SUM(l.receipt_amount) AS receipt_amount
FROM t_rob_order_operate_log l
JOIN t_rob_order o ON o.id = l.order_id AND o.is_deleted = 0
WHERE l.operate_type IN (1, 2)
  AND l.create_time >= '2026-09-18 00:00:00' AND l.create_time <= '2026-09-18 23:59:59'
GROUP BY l.operate_type;
