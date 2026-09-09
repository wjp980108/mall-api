-- =============================================
-- 初始化数据
-- =============================================
-- 系统设置
-- =============================================

INSERT INTO `sys_settings` (`id`, `site_name`, `site_logo`, `new_member_days`, `new_member_advance_minutes`, `pre_view_minutes`, `share_valid_days`, `referrer_purchase_days`, `show_self_buy_bonus`, `show_coupon`, `limit_rule`, `recommend_rate`, `self_buy_rate`, `self_buy_bonus_ratio`, `coupon_ratio`, `order_profit_rate`, `poster_bg_image`, `create_time`, `update_time`, `is_deleted`) VALUES (1, '你的站点名称', '', 7, 30, 0, 0, 0, 1, 1, 0, 10.00, 10.00, 50.00, 50.00, 20.00, '', '2026-09-09 20:18:04', '2026-09-09 20:18:04', 0);

-- =============================================
-- 系统角色
-- =============================================

-- 超级管理员角色
INSERT IGNORE INTO sys_role(id, role_name, role_code) VALUES(1, '超级管理员', 'SUPER_ADMIN');
-- 平台管理员角色（后台运营人员，显式固定 id=2）
INSERT IGNORE INTO sys_role(id, role_name, role_code) VALUES(2, '平台管理员', 'PLATFORM_ADMIN');
-- 会员角色（H5 端注册用户默认角色，代码 PermissionConst.MEMBER_ROLE_ID 固定引用 id=3，显式固定 id 防止自增漂移）
INSERT IGNORE INTO sys_role(id, role_name, role_code) VALUES(3, '会员', 'MEMBER');

-- =============================================
-- 系统菜单
-- =============================================

INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (1, 0, '平台管理', NULL, NULL, 0, '/system', 'System', NULL, 'solar:settings-line-duotone', 999, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:14:17');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (2, 1, '菜单管理', NULL, NULL, 1, '/system/menu', 'SystemMenu', 'system/menu/index', 'system-uicons:side-menu', 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (3, 2, '新增', NULL, 'system:menu:add', 2, NULL, NULL, NULL, NULL, 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (4, 2, '编辑', NULL, 'system:menu:edit', 2, NULL, NULL, NULL, NULL, 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (5, 2, '删除', NULL, 'system:menu:remove', 2, NULL, NULL, NULL, NULL, 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (6, 2, '启用/禁用', NULL, 'system:menu:status', 2, NULL, NULL, NULL, NULL, 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (7, 1, '角色管理', NULL, NULL, 1, '/system/role', 'SystemRole', 'system/role/index', 'carbon:user-role', 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (8, 7, '编辑', NULL, 'system:role:edit', 2, NULL, NULL, NULL, NULL, 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (9, 7, '启用/禁用', NULL, 'system:role:status', 2, NULL, NULL, NULL, NULL, 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (10, 1, '用户管理', NULL, NULL, 1, '/system/user', 'SystemUser', 'system/user/index', 'hugeicons:user-settings-01', 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (11, 10, '新增', NULL, 'system:user:add', 2, NULL, NULL, NULL, NULL, 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (12, 10, '编辑', NULL, 'system:user:edit', 2, NULL, NULL, NULL, NULL, 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (13, 10, '删除', NULL, 'system:user:remove', 2, NULL, NULL, NULL, NULL, 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (14, 10, '启用/禁用', NULL, 'system:user:status', 2, NULL, NULL, NULL, NULL, 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (15, 0, '商品管理', NULL, NULL, 0, '/product', 'Product', NULL, 'fluent-mdl2:product', 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:38');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (16, 15, '商品列表', NULL, NULL, 1, '/product/list', 'ProductList', 'product/list/index', 'fluent-mdl2:product-list', 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (17, 16, '新增商品', NULL, 'system:product:add', 2, NULL, NULL, NULL, NULL, 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (18, 16, '编辑', NULL, 'system:product:edit', 2, NULL, NULL, NULL, NULL, 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (19, 16, '删除', NULL, 'system:product:remove', 2, NULL, NULL, NULL, NULL, 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (20, 16, '上架/下架', NULL, 'system:product:status', 2, NULL, NULL, NULL, NULL, 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (21, 15, '抢购商品列表', NULL, NULL, 1, '/product/rush-list', 'ProductRushList', 'product/rushList/index', 'Lightning', 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (22, 21, '新增商品', NULL, 'system:rushProduct:add', 2, NULL, NULL, NULL, NULL, 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (23, 21, '编辑', NULL, 'system:rushProduct:edit', 2, NULL, NULL, NULL, NULL, 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (24, 21, '删除', NULL, 'system:rushProduct:remove', 2, NULL, NULL, NULL, NULL, 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (25, 21, '上架/下架', NULL, 'system:rushProduct:status', 2, NULL, NULL, NULL, NULL, 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (26, 0, '信息管理', NULL, NULL, 0, '/information', 'Information', NULL, 'icon-park-outline:info', 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:43');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (27, 26, '公告', NULL, NULL, 1, '/information/announcement', 'InformationAnnouncement', 'information/announcement/index', 'icon-park-outline:announcement', 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (28, 27, '新增公告', NULL, 'system:announcement:add', 2, NULL, NULL, NULL, NULL, 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (29, 27, '编辑', NULL, 'system:announcement:edit', 2, NULL, NULL, NULL, NULL, 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (30, 27, '删除', NULL, 'system:announcement:remove', 2, NULL, NULL, NULL, NULL, 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (31, 26, '轮播图', NULL, NULL, 1, '/information/carousel', 'InformationCarousel', 'information/carousel/index', 'PictureFilled', 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (32, 31, '新增轮播图', NULL, 'system:carousel:add', 2, NULL, NULL, NULL, NULL, 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (33, 31, '编辑', NULL, 'system:carousel:edit', 2, NULL, NULL, NULL, NULL, 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (34, 31, '删除', NULL, 'system:carousel:remove', 2, NULL, NULL, NULL, NULL, 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (35, 0, '常规管理', NULL, NULL, 0, '/general-management', 'GeneralManagement', NULL, 'solar:settings-line-duotone', 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (36, 35, '用户协议', NULL, NULL, 1, '/general-management/user-agreement', 'GeneralManagementUserAgreement', 'generalManagement/userAgreement/index', 'iconamoon:file-document', 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (37, 35, '隐私协议', NULL, NULL, 1, '/general-management/privacy-agreement', 'GeneralManagementPrivacyAgreement', 'generalManagement/privacyAgreement/index', 'material-symbols:privacy-tip-outline', 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (38, 0, '订单管理', NULL, NULL, 1, '/rush-order/all-order', 'RushOrderAllOrder', 'rushOrder/allOrder/index', 'ep:tickets', 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (39, 38, '转移订单', NULL, 'system:allOrder:shift', 2, NULL, NULL, NULL, NULL, 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (40, 38, '取消订单', NULL, 'system:allOrder:cancel', 2, NULL, NULL, NULL, NULL, 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (41, 0, '抢购场次', NULL, NULL, 1, '/rush-system/time-setting', 'RushSystemTimeSetting', 'rushSystem/timeSetting/index', 'material-symbols-light:timer-outline', 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (42, 41, '新增抢购场次', NULL, 'system:timeSetting:add', 2, NULL, NULL, NULL, NULL, 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (43, 41, '编辑', NULL, 'system:timeSetting:edit', 2, NULL, NULL, NULL, NULL, 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (44, 41, '删除', NULL, 'system:timeSetting:remove', 2, NULL, NULL, NULL, NULL, 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (45, 41, '启用/禁用', NULL, 'system:timeSetting:status', 2, NULL, NULL, NULL, NULL, 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (46, 41, '关联商品', NULL, 'system:timeSetting:relatedProducts', 2, NULL, NULL, NULL, NULL, 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (47, 0, '系统设置', NULL, NULL, 1, '/system/settings', 'SystemSettings', 'system/settings/index', 'solar:settings-minimalistic-line-duotone', 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:14:13');
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `menu_code`, `perm`, `type`, `path`, `route_name`, `component_path`, `icon`, `sort`, `visible`, `keep_alive`, `active_menu`, `hide_in_menu`, `hide_in_tag`, `hide_parent`, `status`, `is_deleted`, `create_time`, `update_time`) VALUES (48, 47, '保存系统设置', NULL, 'system:settings:save', 2, NULL, NULL, NULL, NULL, 0, 1, 0, NULL, 0, 0, 0, 1, 0, '2026-09-09 01:13:03', '2026-09-09 01:13:03');

-- =============================================
-- 系统用户
-- =============================================

INSERT IGNORE INTO sys_user (id, username, password, nickname, status, is_deleted) VALUES
(0, 'admin', '$10$JnMlXJG65NApREMFbecz/OOavrH8cptEARJQhKjCEzNNoU5H/WJUW', '超级管理员', 1, 0);

-- =============================================
-- 系统配置
-- =============================================

INSERT IGNORE INTO `sys_config` (`config_group`, `config_group_name`, `config_key`, `config_title`, `config_value`, `value_type`, `sort`, `remark`) VALUES
-- ====================== 【会员配置】第一张截图 ======================
('member', '会员配置', 'site.order_number',         '会员下单限制数量',                '3',                     'number',  1, '单个会员下单数量限制'),
('member', '会员配置', 'site.rob_order_num',         '会员提前抢购订单数量限制',        '2',                     'number',  2, '会员提前抢购订单上限'),

-- ====================== 【基础配置】第二张截图 ======================
('base', '基础配置', 'site.name',           '站点名称',           '金鑫',                                         'string',  1, '网站站点名称'),
('base', '基础配置', 'site.beian',          '备案号',             '',                                             'string',  2, '网站ICP备案号'),
('base', '基础配置', 'site.cdnurl',         'CDN地址',            '',                                             'string',  3, '静态资源CDN访问地址'),
('base', '基础配置', 'site.version',        '版本号',             '1.0.2',                                        'string',  4, '系统当前版本号'),
('base', '基础配置', 'site.timezone',       '时区',               'Asia/Shanghai',                                 'string',  5, '系统时区配置'),
('base', '基础配置', 'site.forbiddenip',    '禁止IP',             '',                                             'string',  6, '黑名单禁止访问IP，多个换行分隔'),
('base', '基础配置', 'site.languages',     '语言',               '{"backend":"zh-cn","frontend":"zh-cn"}',        'json',    7, '前后台语言配置键值对'),
('base', '基础配置', 'site.fixedpage',      '后台固定页',         'dashboard',                                    'string',  8, '登录后默认跳转后台页面'),

-- ====================== 【支付配置】第三张截图 ======================
('pay', '支付配置', 'site.pay_type',            '支付方式',             '["余额"]',                         'json',     1, '多选启用支付方式：余额、微信、支付宝'),
('pay', '支付配置', 'site.order_limit_time',    '订单超时时间（秒）',   '36000',                           'number',   2, '未支付订单超时自动关闭，单位秒'),
('pay', '支付配置', 'site.open_adapay_query',   '开启支付查询',         'false',                           'boolean',  3, '是否开启主动查询支付状态'),
('pay', '支付配置', 'site.work_day',            '工作日',               '["周一","周二","周三","周四","周五"]','json', 4, '勾选的工作日列表'),
('pay', '支付配置', 'site.income_rate',         '收益积分比例',         '0',                               'number',   5, '收益积分百分比比例');

































