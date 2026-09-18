-- =============================================
-- 历史脏数据订正脚本：20260919_fix_consign_goods_payment_amount.sql
-- 变更：stop-mutating-consign-goods-prices
-- 背景：t_consign_goods.payment_amount（实付价格，单价口径）与 goods_price（虚标价格）同属
--      管理端手工维护的固定配置值，抢购下单/取消业务流程本不应写入。但 placeOrder 旧逻辑在
--      首单成交后把 payment_amount 无条件覆盖为成交单价（= goods_price，实测 300/712 被改成
--      10000+ 挂牌价）；20260918_fix_payment_amount_chain_dirty_data.sql 第一段又在同一错误
--      前提下把 payment_amount 改写成"最新有效订单单价"。本脚本把当前值恢复为管理端原始设置。
--
-- 【人工执行，勿随应用启动】执行流程：
--   1. 先在 dev 库事务内执行预览 SELECT + UPDATE 并核对结果（ROLLBACK 收尾）；
--   2. 生产执行前务必备份：
--        CREATE TABLE bak_consign_goods_20260919 AS SELECT * FROM t_consign_goods;
--   3. 先跑【预览】SELECT 逐行核对 recovered/expected 值与 fix_source，确认后再执行【订正】UPDATE；
--   4. 全部语句幂等，可重复执行，二次执行预览/影响行数应为 0。
--
-- 恢复口径：
--   污染特征 = payment_amount = goods_price 且该商品存在抢购下单记录（被下单回写覆盖过的典型特征）。
--   ① 优先取该商品"最早一笔 prev_payment_amount > 0 的订单"的快照值——该列冻结了当单下单前
--      商品付款金额旧值，最早一单的快照即管理端原始设置值；
--   ② 不存在非 0 快照（2026-09-18 前的存量历史单、或管理端原值本就为 0）时回退用 goods_price，
--      fix_source 标注 fallback_goods_price，需管理员人工核对后在管理端商品编辑页改回；
--   ③ 管理端原值可能恰好等于 goods_price，此时订正为同值（等价不改），语义正确且保持幂等。
-- =============================================

-- #############################################################
-- # 第一段：预览（只查不改）——被污染商品清单 + 可能的原始值来源
-- #############################################################

-- ---------- 预览 1-1：payment_amount = goods_price 且有下单记录的商品（污染清单） ----------
-- fix_source = prev_snapshot  : expected_payment_amount 取自最早非0快照，可直接用于订正
-- fix_source = fallback_goods_price : 无非0快照可反推，回退挂牌价，务必人工核对
SELECT g.id AS goods_id,
       g.goods_name,
       g.goods_price,
       g.payment_amount AS current_payment_amount,
       src.prev_payment_amount AS recovered_from_prev,
       src.source_order_id,
       (SELECT MIN(o.id) FROM t_rob_order o
         WHERE o.goods_id = g.id AND o.is_deleted = 0) AS first_order_id,
       (SELECT COUNT(*) FROM t_rob_order o
         WHERE o.goods_id = g.id AND o.is_deleted = 0) AS order_cnt,
       CASE WHEN src.prev_payment_amount IS NULL
            THEN 'fallback_goods_price'
            ELSE 'prev_snapshot' END AS fix_source,
       COALESCE(src.prev_payment_amount, g.goods_price) AS expected_payment_amount
FROM t_consign_goods g
LEFT JOIN (
    -- 每个商品取 prev_payment_amount 非 0 的最早订单快照
    SELECT o.goods_id, o.prev_payment_amount, o.id AS source_order_id
    FROM t_rob_order o
    JOIN (SELECT goods_id, MIN(id) AS min_id
            FROM t_rob_order
           WHERE prev_payment_amount > 0 AND is_deleted = 0
           GROUP BY goods_id) m ON m.min_id = o.id
    WHERE o.is_deleted = 0
) src ON src.goods_id = g.id
WHERE g.is_deleted = 0
  AND g.payment_amount = g.goods_price
  AND EXISTS (SELECT 1 FROM t_rob_order o
               WHERE o.goods_id = g.id AND o.is_deleted = 0);

-- ---------- 预览 1-2：其中无法从快照反推、回退挂牌价的商品（需管理员人工核对） ----------
SELECT g.id AS goods_id, g.goods_name, g.goods_price,
       g.payment_amount AS current_payment_amount,
       (SELECT MIN(o.id) FROM t_rob_order o
         WHERE o.goods_id = g.id AND o.is_deleted = 0) AS first_order_id,
       (SELECT COUNT(*) FROM t_rob_order o
         WHERE o.goods_id = g.id AND o.is_deleted = 0) AS order_cnt
FROM t_consign_goods g
WHERE g.is_deleted = 0
  AND g.payment_amount = g.goods_price
  AND EXISTS (SELECT 1 FROM t_rob_order o
               WHERE o.goods_id = g.id AND o.is_deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM t_rob_order o
                   WHERE o.goods_id = g.id AND o.is_deleted = 0
                     AND o.prev_payment_amount > 0);

-- #############################################################
-- # 第二段：订正（幂等，二次执行影响 0 行）
-- # 已被快照恢复为与 goods_price 不同的值后，WHERE payment_amount = goods_price 自动排除该行；
-- # 回退/原值本就等于挂牌价的行 SET 同值，不产生实际变更。
-- #############################################################

-- ---------- 【确认预览后执行】订正：恢复管理端原始付款金额 ----------
UPDATE t_consign_goods g
JOIN (
    SELECT DISTINCT goods_id
    FROM t_rob_order
    WHERE is_deleted = 0
) polluted ON polluted.goods_id = g.id
LEFT JOIN (
    SELECT o.goods_id, o.prev_payment_amount
    FROM t_rob_order o
    JOIN (SELECT goods_id, MIN(id) AS min_id
            FROM t_rob_order
           WHERE prev_payment_amount > 0 AND is_deleted = 0
           GROUP BY goods_id) m ON m.min_id = o.id
    WHERE o.is_deleted = 0
) src ON src.goods_id = g.id
SET g.payment_amount = COALESCE(src.prev_payment_amount, g.goods_price)
WHERE g.is_deleted = 0
  AND g.payment_amount = g.goods_price;

-- ---------- 订正后对账（可选）：应无"快照可恢复但仍等于挂牌价"的残留行 ----------
SELECT g.id AS goods_id, g.goods_name, g.goods_price, g.payment_amount
FROM t_consign_goods g
WHERE g.is_deleted = 0
  AND g.payment_amount = g.goods_price
  AND EXISTS (SELECT 1 FROM t_rob_order o
               WHERE o.goods_id = g.id AND o.is_deleted = 0
                 AND o.prev_payment_amount > 0
                 AND o.prev_payment_amount <> g.goods_price);
