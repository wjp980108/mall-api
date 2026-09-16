-- =============================================
-- 增量升级脚本：20260916_add_user_operate_log.sql
-- 变更：add-user-operate-log
--   新增 sys_user_operate_log 用户操作审计日志表：
--   管理端用户写操作(创建/编辑/启停/删除/转老会员)与定时任务系统自动禁用统一落此表
-- 幂等性：CREATE TABLE IF NOT EXISTS，可重复执行
-- =============================================

CREATE TABLE IF NOT EXISTS `sys_user_operate_log` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `user_id`           BIGINT       NOT NULL COMMENT '目标用户ID,关联sys_user.id',
    `before_status`     TINYINT      DEFAULT NULL COMMENT '操作前账号状态(非状态类操作为NULL)',
    `after_status`      TINYINT      DEFAULT NULL COMMENT '操作后账号状态(非状态类操作为NULL)',
    `operate_type`      TINYINT      NOT NULL COMMENT '操作类型 1创建用户 2编辑用户 3启用用户 4禁用用户 5删除用户 6转老会员 7到期未转化自动禁用',
    `operate_desc`      VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '操作类型中文描述(冗余展示)',
    `operate_user_id`   BIGINT       DEFAULT NULL COMMENT '操作人管理员ID,关联sys_user.id;定时任务系统操作为NULL',
    `operate_user_name` VARCHAR(64)  DEFAULT NULL COMMENT '操作人名称快照;定时任务系统操作固定为SYSTEM',
    `remark`            VARCHAR(512) DEFAULT NULL COMMENT '操作备注(关键入参JSON摘要,超长截断)',
    `is_deleted`        TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删 1已删',
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`) COMMENT '按目标用户查审计记录',
    KEY `idx_create_time` (`create_time`) COMMENT '按时间范围查审计记录'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户操作审计日志';
