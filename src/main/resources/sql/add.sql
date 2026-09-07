-- =============================================
-- 会员权益 member-benefits 变更（幂等，可重复执行）
-- 1. sys_user 新增 member_type 列
-- 2. 转老会员按钮菜单 + 超管角色授权
-- =============================================

-- 1. sys_user 新增 member_type 列
--    MySQL 原生不支持 ADD COLUMN IF NOT EXISTS，用 INFORMATION_SCHEMA 判断列是否存在以幂等
SET @col_exists := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'member_type');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE sys_user ADD COLUMN member_type TINYINT NOT NULL DEFAULT 0 COMMENT ''会员类型 0新会员 1老会员'' AFTER status',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 转老会员按钮菜单（挂在用户管理 id=2 下，紧接用户启用/禁用 id=7，sort=6）
INSERT IGNORE INTO sys_menu(id, parent_id, name, menu_code, perm, type, path, component_path, icon, sort, visible) VALUES
(8, 2, '转老会员', NULL, 'sys:user:toOld', 2, NULL, NULL, NULL, 6, 1);

-- 3. 给超级管理员角色(role_id=1)分配转老会员按钮权限（镜像初始化时 sys_role_menu 全菜单分配，幂等）
INSERT IGNORE INTO sys_role_menu(role_id, menu_id) VALUES (1, 8);
