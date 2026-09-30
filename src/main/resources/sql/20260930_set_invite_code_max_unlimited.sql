-- 迁移脚本: 将存量邀请码的最大可邀请人数统一设为 0（不限制）
-- 变更说明: 业务取消邀请人数上限，max_invite_num = 0 表示不限制
-- 前提: 已确认业务侧允许将现有邀请码的上限全部解除
-- 幂等: 可安全重跑；仅当 max_invite_num > 0 时才会更新
-- 建议: 在低峰期执行

START TRANSACTION;

UPDATE sys_invite_code
SET max_invite_num = 0,
    update_time    = NOW()
WHERE max_invite_num > 0;

COMMIT;

-- 校验 (手动执行):
-- SELECT COUNT(*) FROM sys_invite_code WHERE max_invite_num > 0;
-- 期望结果为 0
