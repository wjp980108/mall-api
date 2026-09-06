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