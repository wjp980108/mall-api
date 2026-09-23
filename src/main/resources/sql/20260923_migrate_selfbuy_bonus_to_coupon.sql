-- 迁移脚本: 自购奖金从可用积分迁到购物券积分 (account_type: 1 -> 2)
-- 变更说明: 自购奖金(biz_type=2) 从发放即进入锁死账户，本次将存量自购奖迁移到 coupon_points
-- 前提: 已备份 t_user_points + t_user_points_flow
-- 口径: 流水聚合自购奖净额(account_type=1) → LEAST(GREATEST(points,0), 净额) 迁到 coupon_points
--       已被转让/冲走的自购奖留在历史中，不凭空制造 coupon_points
-- 幂等: 可安全重跑；迁移前后 SUM(points + coupon_points) 全表不变
-- 建议: 停机窗口内执行

START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS tmp_selfbuy_net;
CREATE TEMPORARY TABLE tmp_selfbuy_net AS
SELECT
    user_id,
    COALESCE(SUM(CASE WHEN flow_type = 1 THEN amount ELSE 0 END), 0)
  - COALESCE(SUM(CASE WHEN flow_type = 2 THEN amount ELSE 0 END), 0)
    AS selfbuy_net
FROM t_user_points_flow
WHERE biz_type = 2
  AND account_type = 1
GROUP BY user_id;

UPDATE t_user_points up
JOIN tmp_selfbuy_net t ON up.user_id = t.user_id
SET
    up.points = up.points - LEAST(GREATEST(up.points, 0), t.selfbuy_net),
    up.coupon_points = up.coupon_points + LEAST(GREATEST(up.points, 0), t.selfbuy_net),
    up.update_time = NOW()
WHERE t.selfbuy_net > 0;

DROP TEMPORARY TABLE IF EXISTS tmp_selfbuy_net;

COMMIT;

-- 校验 (手动执行):
-- SELECT SUM(points + coupon_points) AS total FROM t_user_points;
-- 迁移前后应一致