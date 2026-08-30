
-- =============================================
-- 用户地址簿模块表：t_user_address
-- =============================================

-- 16. 用户收货地址簿（C端用户管理多收货地址，下单时选择）
-- 注：address 存完整拼接字符串，对齐 t_order.receive_address 语义；下单快照直接写入订单
CREATE TABLE IF NOT EXISTS `t_user_address` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`        BIGINT       NOT NULL COMMENT '所属用户ID 关联sys_user.id',
    `receiver_name`  VARCHAR(50)  NOT NULL COMMENT '收货人姓名',
    `receiver_phone` VARCHAR(20) NOT NULL COMMENT '收货人手机号',
    `address`        VARCHAR(512) NOT NULL COMMENT '收货地址完整拼接字符串',
    `is_default`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认 0否 1是',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删 1已删',
    PRIMARY KEY (`id`),
    KEY `idx_user_default` (`user_id`, `is_default`, `is_deleted`) COMMENT '查询用户地址列表（含默认置顶）',
    CONSTRAINT `fk_user_address_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收货地址簿';

-- =============================================
-- 托售商品委托代卖审核：t_consign_goods 增量加列
-- =============================================
-- 业务背景：确认收款后商品交由买家处理(4待处理)，买家主动发起委托代卖申请，
-- 平台管理员审核通过后商品重新上架(1挂卖中)进入下一轮抢购；驳回则退回待处理。
-- 注意：MySQL 8 的 ALTER ADD COLUMN 不支持 IF NOT EXISTS，本脚本仅需执行一次；
--       重复执行报 Duplicate column name 错误可直接忽略（幂等性由 rbac_schema.sql 全量建表兜底）。
ALTER TABLE `t_consign_goods`
    ADD COLUMN `entrust_status` TINYINT NOT NULL DEFAULT 0 COMMENT '委托状态 0未委托 1委托代卖中' AFTER `goods_status`,
    ADD COLUMN `audit_status` TINYINT NOT NULL DEFAULT 0 COMMENT '审核状态 0无需审核 1待审核 2审核通过 3审核驳回' AFTER `entrust_status`,
    ADD INDEX `idx_entrust_audit` (`entrust_status`, `audit_status`);

-- =============================================
-- 移除预留状态6委托发货：列注释同步（MODIFY 幂等，可重复执行）
-- =============================================
-- 说明：业务闭环调整后状态机为 1挂卖中↔2↔3↔4↔5（含委托审核闭环），
--       6委托发货不再作为业务状态（发货履约功能未规划），枚举/流转矩阵/注释已同步移除。
-- 如存量数据存在 goods_status=6 的记录，先回退为 4待处理（按需手动执行）：
-- UPDATE t_consign_goods SET goods_status = 4 WHERE goods_status = 6;
ALTER TABLE `t_consign_goods`
    MODIFY COLUMN `goods_status` TINYINT COMMENT '商品业务状态 1挂卖中 2已抢购待付款 3等待确认付款 4待处理 5委托代卖';
ALTER TABLE `t_consign_goods_operate_log`
    MODIFY COLUMN `from_status` TINYINT DEFAULT NULL COMMENT '业务流转前状态(仅operate_type=5有效 1挂卖中 2已抢购待付款 3等待确认付款 4待处理 5委托代卖)';

-- =============================================
-- 用户协议模块表：sys_user_agreement（单例行设计）
-- =============================================
-- 业务说明：系统只维护一份最新版本的用户协议（代卖商城科技商城用户注册协议），
--          后台富文本编辑保存，前端(H5)读取展示；不做多条历史版本记录。
-- 单例行约束：表内永远只保留一条生效数据，约定固定 id=1（代码 SysUserAgreement.SINGLETON_ID 引用），
--          由下方 INSERT IGNORE 初始化，之后业务只做 UPDATE，永不 INSERT 新行。
-- 时间字段：对齐 t_user_address 惯例，DB 自动填充（原需求 DEFAULT NULL 无法自动维护更新时间）。
CREATE TABLE IF NOT EXISTS `sys_user_agreement` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `title`       VARCHAR(200) DEFAULT NULL COMMENT '协议标题',
    `content`     LONGTEXT     COMMENT '协议富文本内容(html)',
    `create_by`   VARCHAR(64)  DEFAULT '' COMMENT '创建人(用户名)',
    `update_by`   VARCHAR(64)  DEFAULT '' COMMENT '更新人(用户名)',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否删除 0否 1是',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户协议表(仅一条生效记录,固定id=1)';

-- 初始化单例行（固定 id=1，重复执行自动跳过）
INSERT IGNORE INTO `sys_user_agreement` (`id`, `title`, `content`, `create_by`, `update_by`)
VALUES (1, '代卖商城科技商城用户注册协议', '', 'system', 'system');

-- =============================================
-- 常规管理目录 + 用户协议菜单（菜单ID 110-113，避开已用 90-105 段）
-- =============================================
-- 常规管理一级目录
INSERT IGNORE INTO sys_menu(id, parent_id, name, menu_code, perm, type, path, component_path, icon, sort, visible) VALUES
(110, 0, '常规管理', 'common', NULL, 0, '/common', NULL, 'Files', 80, 1);

-- 用户协议菜单
INSERT IGNORE INTO sys_menu(id, parent_id, name, menu_code, perm, type, path, component_path, icon, sort, visible) VALUES
(111, 110, '用户协议', 'agreement', NULL, 1, 'agreement', 'common/agreement/index', 'Document', 10, 1);

-- 用户协议按钮权限（格式: 模块:页面:操作）
INSERT IGNORE INTO sys_menu(id, parent_id, name, menu_code, perm, type, path, component_path, icon, sort, visible) VALUES
(112, 111, '协议查询', NULL, 'sys:agreement:query',  2, NULL, NULL, NULL, 1, 1),
(113, 111, '协议保存', NULL, 'sys:agreement:update', 2, NULL, NULL, NULL, 2, 1);

-- 给超级管理员分配用户协议菜单/权限
INSERT IGNORE INTO sys_role_menu(role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE id BETWEEN 110 AND 113;
