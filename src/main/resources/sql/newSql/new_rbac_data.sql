-- =============================================
-- 场次商品关联模块菜单数据
-- =============================================
-- 场次商品管理菜单 (id 从 120 开始，避免与已有菜单冲突)
INSERT IGNORE INTO sys_menu(id, parent_id, name, menu_code, perm, type, path, component_path, icon, sort, visible) VALUES
(120, 1,  '场次商品管理', 'sessionProduct', NULL,                       1, 'sessionProduct',  'sessionProduct/index',  'Present', 51, 1),
(121, 120, '场次商品查询', NULL,     'session:product:query',      2, NULL, NULL, NULL, 1, 1),
(122, 120, '场次商品新增', NULL,     'session:product:add',        2, NULL, NULL, NULL, 2, 1),
(123, 120, '场次商品修改', NULL,     'session:product:update',     2, NULL, NULL, NULL, 3, 1),
(124, 120, '场次商品删除', NULL,     'session:product:delete',     2, NULL, NULL, NULL, 4, 1);

-- 给超级管理员分配场次商品管理菜单/权限
INSERT IGNORE INTO sys_role_menu(role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE id BETWEEN 120 AND 124;

-- =============================================
-- 场次商品库存模块菜单数据
-- =============================================
-- 场次商品库存菜单 (id 从 125 开始，避免与已有菜单冲突)
INSERT IGNORE INTO sys_menu(id, parent_id, name, menu_code, perm, type, path, component_path, icon, sort, visible) VALUES
(125, 1,  '场次商品库存', 'sessionProductStock', NULL,                   1, 'sessionProductStock',  'sessionProductStock/index',  'Box', 52, 1),
(126, 125, '场次商品库存查询', NULL, 'session:stock:query',        2, NULL, NULL, NULL, 1, 1),
(127, 125, '场次商品库存设置', NULL, 'session:stock:update',       2, NULL, NULL, NULL, 2, 1);

-- 给超级管理员分配场次商品库存菜单/权限
INSERT IGNORE INTO sys_role_menu(role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE id BETWEEN 125 AND 127;


-- =============================================
-- 系统设置模块数据
-- =============================================
-- 初始数据: 固定 id=1, INSERT IGNORE 幂等
INSERT IGNORE INTO `sys_settings` (
    `id`, `site_name`, `site_logo`,
    `new_member_days`, `new_member_advance_minutes`,
    `pre_view_minutes`, `limit_rule`, `share_valid_days`,
    `recommend_rate`, `referrer_purchase_days`,
    `self_buy_rate`, `self_buy_bonus_ratio`, `coupon_ratio`,
    `show_self_buy_bonus`, `show_coupon`, `order_profit_rate`,
    `poster_bg_image`
) VALUES (
    1, '五谷丰登商贸限时爆品热卖中', NULL,
    5, 1,
    30, 1, 0,
    0.20, 2,
    1.00, 80.00, 20.00,
    0, 0, 20.00,
    NULL
);

-- 系统设置菜单 (菜单ID 128-130, 挂在 常规管理 parent_id=110 下, sort=30)
INSERT IGNORE INTO sys_menu(id, parent_id, name, menu_code, perm, type, path, component_path, icon, sort, visible) VALUES
(128, 110, '系统设置', 'settings', NULL,               1, 'settings', 'settings/index', 'Setting', 30, 1),
(129, 128, '设置查询', NULL,     'sys:settings:query',  2, NULL, NULL, NULL, 1, 1),
(130, 128, '设置修改', NULL,     'sys:settings:update', 2, NULL, NULL, NULL, 2, 1);

-- 给超级管理员分配系统设置菜单/权限
INSERT IGNORE INTO sys_role_menu(role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE id BETWEEN 128 AND 130;