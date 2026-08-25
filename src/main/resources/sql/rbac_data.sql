-- =============================================
-- RBAC 及业务模块初始化数据脚本（DML：仅数据，不含建表）
-- 执行前请先执行 rbac_schema.sql 完成建表
-- 依赖表：sys_role / sys_menu / sys_role_menu / sys_user / sys_user_role
-- 所有 INSERT 均为幂等操作(INSERT IGNORE)，可重复执行
-- =============================================

-- 0. 切换到目标数据库
USE mall;

-- =============================================
-- 初始化数据
-- =============================================

-- 超级管理员角色
INSERT IGNORE INTO sys_role(role_name, role_code) VALUES('超级管理员', 'SUPER_ADMIN');

-- 菜单数据：目录 -> 菜单 -> 按钮 三级结构
-- 系统管理目录
INSERT IGNORE INTO sys_menu(id, parent_id, name, menu_code, perm, type, path, component_path, icon, sort, visible) VALUES
-- 系统管理目录
(1, 0, '系统管理', 'sys',  NULL,        0, '/sys',       NULL,                     'Setting', 10, 1),
-- 用户管理菜单
(2, 1, '用户管理', 'user', NULL,        1, 'user',       'sys/user/index',         'User',     10, 1),
-- 用户管理下的按钮权限（格式: 模块:页面:操作）
(3, 2, '用户查询', NULL,   'sys:user:query',  2, NULL, NULL, NULL, 1, 1),
(4, 2, '用户新增', NULL,   'sys:user:add',    2, NULL, NULL, NULL, 2, 1),
(5, 2, '用户修改', NULL,   'sys:user:update', 2, NULL, NULL, NULL, 3, 1),
(6, 2, '用户删除', NULL,   'sys:user:delete', 2, NULL, NULL, NULL, 4, 1),
(7, 2, '用户启用/禁用', NULL, 'sys:user:status', 2, NULL, NULL, NULL, 5, 1),
-- 角色管理菜单 (id 从 70 开始，避开已使用的 20/30/40/50/60 段)
(70, 1, '角色管理', 'role', NULL,        1, 'role',       'sys/role/index',         'UserFilled', 20, 1),
-- 角色管理下的按钮权限
(71, 70, '角色查询', NULL,    'sys:role:query',         2, NULL, NULL, NULL, 1, 1),
(72, 70, '角色新增', NULL,    'sys:role:add',           2, NULL, NULL, NULL, 2, 1),
(73, 70, '角色修改', NULL,    'sys:role:update',        2, NULL, NULL, NULL, 3, 1),
(74, 70, '角色删除', NULL,    'sys:role:delete',        2, NULL, NULL, NULL, 4, 1),
(75, 70, '角色启用/禁用', NULL, 'sys:role:status',      2, NULL, NULL, NULL, 5, 1),
(76, 70, '角色分配菜单', NULL, 'sys:role:assign:menu',  2, NULL, NULL, NULL, 6, 1),
-- 菜单管理菜单 (id 从 80 开始)
(80, 1, '菜单管理', 'menu', NULL,       1, 'menu',       'sys/menu/index',         'Menu',     30, 1),
-- 菜单管理下的按钮权限
(81, 80, '菜单查询', NULL,    'sys:menu:query',         2, NULL, NULL, NULL, 1, 1),
(82, 80, '菜单新增', NULL,    'sys:menu:add',           2, NULL, NULL, NULL, 2, 1),
(83, 80, '菜单修改', NULL,    'sys:menu:update',        2, NULL, NULL, NULL, 3, 1),
(84, 80, '菜单删除', NULL,    'sys:menu:delete',        2, NULL, NULL, NULL, 4, 1),
(85, 80, '菜单启用/禁用', NULL, 'sys:menu:status',      2, NULL, NULL, NULL, 5, 1);

-- 给超级管理员分配以上全部菜单/权限
INSERT IGNORE INTO sys_role_menu(role_id, menu_id)
SELECT 1, id FROM sys_menu;

-- =============================================
-- 初始化用户数据（密码均为 BCrypt 加密，原始密码见注释）
-- =============================================
INSERT IGNORE INTO sys_user (id, username, password, nickname, email, phone, age, gender, avatar, birthday, status, create_time, update_time, is_deleted) VALUES
-- 密码: 123456
(1, '17639524881', '$2b$10$JnMlXJG65NApREMFbecz/OOavrH8cptEARJQhKjCEzNNoU5H/WJUW', '哈哈哈', 'AbC123@example.com', '17639524881', 18, 0, '/upload/avatar/7', '1989-05-26', 1, '2026-08-13 16:06:13', '2026-08-13 16:13:16', 0),
-- 密码: admin
(2, '13823456789', '$2b$10$nA8pOSsQ4.Hvu9w1K1Il8e5LOCjmqdP9IjWtZhHCoAaQshfaynGFi', '测试数据', 'x7ZzQ9@test.com', '13823456789', 18, 2, NULL, '1999-04-01', 1, '2026-08-13 16:08:13', '2026-08-13 16:13:16', 0),
-- 密码: 111111
(3, '15987654321', '$2b$10$wTQRVgVrAG4EL/RcvD07EuPSgNubDR8osB1cxyPkm74LS2nL8jXSe', '张雨晴', 'rR2kTSu@demo.com', '15987654321', 20, 1, NULL, '2000-05-10', 1, '2026-08-13 16:08:13', '2026-08-13 16:13:16', 0),
-- 密码: 000000
(4, '18712345678', '$2b$10$LP.i0aMaRJCbTj4uOCl7ru47sRHcqvB6BUa9.77ElXJxXJv5oLCHG', '刘浩然', 'bNGzH2@mail.cc', '18712345678', 25, 1, NULL, '2008-05-15', 1, '2026-08-13 16:08:13', '2026-08-13 16:13:16', 0),
-- 密码: password
(5, '13698765432', '$2b$10$F1/AXnsxWLLKk4ueiwMGcumt2HO0P4WS2o44un9li9m4lGez8HlPu', '陈佳', 'P7q1zS@abc.com', '13698765432', 19, 2, NULL, '1991-05-15', 1, '2026-08-13 16:08:13', '2026-08-13 16:13:16', 0),
-- 密码: test
(6, '15523458769', '$2b$10$xn/n4bpSBq03Y4OZpEvDIeQIHPd.hqNAt9kmlkn0RFw7vKynVP5ke', '赵天宇', 'mK5dF8H@serv.com', '15523458769', 23, 1, NULL, '2014-05-08', 2, '2026-08-13 16:08:13', '2026-08-13 16:13:16', 0),
-- 密码: 123456
(7, '13945678912', '$2b$10$JnMlXJG65NApREMFbecz/OOavrH8cptEARJQhKjCEzNNoU5H/WJUW', '周俊伟', 'Z9xC7vB@work.com', '13945678912', 26, 1, NULL, '2000-05-18', 1, '2026-08-13 16:08:13', '2026-08-13 16:13:16', 0),
-- 密码: admin
(8, '17639528888', '$2b$10$nA8pOSsQ4.Hvu9w1K1Il8e5LOCjmqdP9IjWtZhHCoAaQshfaynGFi', '李白', NULL, '17639528888', 18, 1, NULL, NULL, 1, '2026-08-13 16:08:13', '2026-08-13 16:13:16', 0),
-- 密码: 111111
(9, '17639528888', '$2b$10$wTQRVgVrAG4EL/RcvD07EuPSgNubDR8osB1cxyPkm74LS2nL8jXSe', '李白', NULL, '17639528888', 18, 2, NULL, NULL, 2, '2026-08-13 16:08:13', '2026-08-13 16:13:16', 0),
-- 密码: 000000
(10, '17639528886', '$2b$10$LP.i0aMaRJCbTj4uOCl7ru47sRHcqvB6BUa9.77ElXJxXJv5oLCHG', '李白', NULL, '17639528888', 18, 2, NULL, NULL, 2, '2026-08-13 16:08:13', '2026-08-13 16:13:16', 0);

-- =============================================
-- 给指定用户绑定超级管理员角色
-- =============================================
INSERT IGNORE INTO sys_user_role(user_id, role_id) VALUES(1, 1);

-- =============================================
-- 轮播图模块菜单数据
-- =============================================
-- 轮播图管理菜单 (假设 id 从 20 开始，避免与已有菜单冲突)
INSERT IGNORE INTO sys_menu(id, parent_id, name, menu_code, perm, type, path, component_path, icon, sort, visible) VALUES
-- 轮播图管理菜单（如果系统管理目录id=1下没有轮播图菜单，可放在运营管理目录或独立目录，这里放在系统管理下做示例，实际可根据前端路由调整parent_id）
(20, 1, '轮播图管理', 'banner', NULL,              1, 'banner',       'banner/index',          'Picture',  20, 1),
(21, 20, '轮播图查询', NULL,   'sys:banner:query',  2, NULL, NULL, NULL, 1, 1),
(22, 20, '轮播图新增', NULL,   'sys:banner:add',    2, NULL, NULL, NULL, 2, 1),
(23, 20, '轮播图修改', NULL,   'sys:banner:update', 2, NULL, NULL, NULL, 3, 1),
(24, 20, '轮播图删除', NULL,   'sys:banner:delete', 2, NULL, NULL, NULL, 4, 1);

-- 给超级管理员分配轮播图菜单/权限
INSERT IGNORE INTO sys_role_menu(role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE id IN (20, 21, 22, 23, 24);

-- =============================================
-- 文件管理模块菜单数据
-- =============================================
-- 文件管理菜单 (id 从 30 开始,避免与已有菜单冲突)
INSERT IGNORE INTO sys_menu(id, parent_id, name, menu_code, perm, type, path, component_path, icon, sort, visible) VALUES
(30, 1, '文件管理', 'file',   NULL,                  1, 'file',   'file/index',   'Document', 30, 1),
(31, 30, '文件上传', NULL,   'file:upload:save',    2, NULL, NULL, NULL, 1, 1),
(32, 30, '文件删除', NULL,   'file:upload:delete',  2, NULL, NULL, NULL, 2, 1);

-- 给超级管理员分配文件管理菜单/权限
INSERT IGNORE INTO sys_role_menu(role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE id IN (30, 31, 32);

-- =============================================
-- 商品模块菜单数据
-- =============================================
-- 商品管理菜单 (id 从 40 开始,避免与已有菜单冲突)
INSERT IGNORE INTO sys_menu(id, parent_id, name, menu_code, perm, type, path, component_path, icon, sort, visible) VALUES
(40, 1,  '商品管理',   'goods', NULL,                1, 'goods',  'goods/index',  'Goods',  40, 1),
(41, 40, '商品查询',   NULL,   'goods:list:query',   2, NULL, NULL, NULL, 1, 1),
(42, 40, '商品新增',   NULL,   'goods:list:add',     2, NULL, NULL, NULL, 2, 1),
(43, 40, '商品修改',   NULL,   'goods:list:update',   2, NULL, NULL, NULL, 3, 1),
(44, 40, '商品删除',   NULL,   'goods:list:delete',   2, NULL, NULL, NULL, 4, 1),
(45, 40, '商品上下架', NULL,   'goods:list:shelf',    2, NULL, NULL, NULL, 5, 1);

-- 给超级管理员分配商品管理菜单/权限
INSERT IGNORE INTO sys_role_menu(role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE id IN (40, 41, 42, 43, 44, 45);

-- =============================================
-- 抢购场次模块菜单数据
-- =============================================
-- 抢购场次管理菜单 (id 从 50 开始，避免与已有菜单冲突)
INSERT IGNORE INTO sys_menu(id, parent_id, name, menu_code, perm, type, path, component_path, icon, sort, visible) VALUES
(50, 1,  '抢购场次管理', 'session', NULL,                  1, 'session',  'session/index',  'Clock', 50, 1),
(51, 50, '场次查询',     NULL,     'session:query',          2, NULL, NULL, NULL, 1, 1),
(52, 50, '场次新增',     NULL,     'session:add',            2, NULL, NULL, NULL, 2, 1),
(53, 50, '场次修改',     NULL,     'session:update',         2, NULL, NULL, NULL, 3, 1),
(54, 50, '场次删除',     NULL,     'session:delete',         2, NULL, NULL, NULL, 4, 1),
(55, 50, '场次背景图上传', NULL,   'session:bg:upload',      2, NULL, NULL, NULL, 5, 1);

-- 给超级管理员分配抢购场次管理菜单/权限
INSERT IGNORE INTO sys_role_menu(role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE id IN (50, 51, 52, 53, 54, 55);

-- =============================================
-- 抢购托售商品模块菜单数据
-- =============================================
-- 托售商品管理菜单 (id 从 60 开始，避免与已有菜单冲突)
INSERT IGNORE INTO sys_menu(id, parent_id, name, menu_code, perm, type, path, component_path, icon, sort, visible) VALUES
(60, 1,  '托售商品管理', 'consign', NULL,                       1, 'consign',  'goods/consign/index',  'ShoppingBag', 60, 1),
(61, 60, '托售商品查询', NULL,     'goods:consign:query',        2, NULL, NULL, NULL, 1, 1),
(62, 60, '托售商品新增', NULL,     'goods:consign:add',          2, NULL, NULL, NULL, 2, 1),
(63, 60, '托售商品修改', NULL,     'goods:consign:update',       2, NULL, NULL, NULL, 3, 1),
(64, 60, '托售商品删除', NULL,     'goods:consign:delete',       2, NULL, NULL, NULL, 4, 1),
(65, 60, '托售商品上下架', NULL,   'goods:consign:shelf',        2, NULL, NULL, NULL, 5, 1),
(66, 60, '托售商品业务状态流转', NULL, 'goods:consign:biz:status', 2, NULL, NULL, NULL, 6, 1);

-- 给超级管理员分配托售商品管理菜单/权限
INSERT IGNORE INTO sys_role_menu(role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE id IN (60, 61, 62, 63, 64, 65, 66);

-- =============================================
-- 订单管理模块菜单数据
-- 目录 -> 5个菜单 -> 按钮权限
-- 菜单ID 从 90 开始，避免与已有菜单冲突
-- =============================================

-- 订单管理目录（一级目录，放在系统管理(id=1) 下）
INSERT IGNORE INTO sys_menu(id, parent_id, name, menu_code, perm, type, path, component_path, icon, sort, visible) VALUES
(90, 1, '订单管理', 'order', NULL, 0, '/order', NULL, 'Tickets', 70, 1);

-- 5 个二级菜单
INSERT IGNORE INTO sys_menu(id, parent_id, name, menu_code, perm, type, path, component_path, icon, sort, visible) VALUES
-- 所有订单菜单
(91, 90, '所有订单',     'all',         NULL, 1, 'all',         'order/all/index',         'Document',   10, 1),
-- 待付款订单菜单
(92, 90, '待付款订单',   'waitPay',     NULL, 1, 'waitPay',     'order/waitPay/index',     'Wallet',     20, 1),
-- 待确认收款订单菜单
(93, 90, '待确认收款',   'waitConfirm', NULL, 1, 'waitConfirm', 'order/waitConfirm/index', 'Reading',    30, 1),
-- 代售记录菜单
(94, 90, '代售记录',     'agentSale',   NULL, 1, 'agentSale',   'order/agentSale/index',   'Histogram',  40, 1),
-- 已取消订单菜单
(95, 90, '已取消订单',   'cancel',      NULL, 1, 'cancel',      'order/cancel/index',      'Close',      50, 1);

-- 菜单下的按钮权限
INSERT IGNORE INTO sys_menu(id, parent_id, name, menu_code, perm, type, path, component_path, icon, sort, visible) VALUES
-- 所有订单 -> 查询
(96,  91, '所有订单查询',       NULL, 'order:all:query',                 2, NULL, NULL, NULL, 1, 1),
-- 待付款订单 -> 查询 + 上传凭证 + 取消 + 删除
(97,  92, '待付款订单查询',     NULL, 'order:waitPay:query',             2, NULL, NULL, NULL, 1, 1),
(98,  92, '上传支付凭证',       NULL, 'order:waitPay:uploadVoucher',     2, NULL, NULL, NULL, 2, 1),
(99,  92, '取消订单(待付款)',   NULL, 'order:operate:cancel',            2, NULL, NULL, NULL, 3, 1),
(100, 92, '删除订单',           NULL, 'order:operate:delete',            2, NULL, NULL, NULL, 4, 1),
-- 待确认收款 -> 查询 + 确认收款 + 取消
(101, 93, '待确认订单查询',     NULL, 'order:waitConfirm:query',         2, NULL, NULL, NULL, 1, 1),
(102, 93, '确认收款',           NULL, 'order:waitConfirm:confirmReceive',2, NULL, NULL, NULL, 2, 1),
(103, 93, '取消订单(待确认)',   NULL, 'order:operate:cancel',            2, NULL, NULL, NULL, 3, 1),
-- 代售记录 -> 查询
(104, 94, '代售记录查询',       NULL, 'order:agentSale:query',           2, NULL, NULL, NULL, 1, 1),
-- 已取消订单 -> 查询
(105, 95, '已取消订单查询',     NULL, 'order:cancel:query',              2, NULL, NULL, NULL, 1, 1);

-- 给超级管理员分配订单管理全部菜单/权限
INSERT IGNORE INTO sys_role_menu(role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE id BETWEEN 90 AND 105;
