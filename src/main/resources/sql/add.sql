

-- =============================================
-- 系统动态配置模块：sys_config + sys_config_log
-- =============================================
-- 设计要点：
--   1. uk_group_key(config_group, config_key) 唯一索引：分组内键唯一，幂等 INSERT IGNORE 依赖此索引
--   2. config_value 统一存字符串；复选框/键值表格等复杂类型存标准 JSON 字符串（value_type=json）
--   3. 后端读取优先走 Redis 缓存(key: sys_config:group:{group})，启动时 SysConfigCacheLoader 全量预加载
--   4. is_deleted/create_time/update_time 对齐项目惯例 NOT NULL DEFAULT（配合 @TableLogic）
CREATE TABLE IF NOT EXISTS `sys_config` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键id',
    `config_group`      VARCHAR(64)  NOT NULL COMMENT '配置分组标识:base(基础配置)/member(会员配置)/pay(支付配置)/email(邮件配置)',
    `config_group_name` VARCHAR(128) NOT NULL COMMENT '分组展示名称:基础配置、会员配置、支付配置',
    `config_key`        VARCHAR(128) NOT NULL COMMENT '配置键名，对应页面变量名:site.order_number',
    `config_title`      VARCHAR(256) NOT NULL COMMENT '配置标题(前端页面展示文字):会员下单限制数量',
    `config_value`      TEXT         COMMENT '配置值，存储字符串、数字、布尔、JSON数组、JSON对象',
    `value_type`        VARCHAR(32)  NOT NULL DEFAULT 'string' COMMENT '值类型:string、number、boolean、json',
    `sort`              INT          NOT NULL DEFAULT 0 COMMENT '同分组下排序号，控制页面从上到下展示顺序',
    `remark`            VARCHAR(512) DEFAULT NULL COMMENT '配置项备注说明',
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`        TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_key` (`config_group`, `config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统动态配置表';

CREATE TABLE IF NOT EXISTS `sys_config_log` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `config_group`  VARCHAR(64)  DEFAULT NULL COMMENT '配置分组编码',
    `config_key`    VARCHAR(128) DEFAULT NULL COMMENT '配置键',
    `old_value`     TEXT         COMMENT '修改前的值',
    `new_value`     TEXT         COMMENT '修改后的值',
    `operator_id`   BIGINT       DEFAULT NULL COMMENT '操作人ID',
    `operator_name` VARCHAR(64)  DEFAULT NULL COMMENT '操作人名称',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置变更日志表';

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

-- 系统配置菜单（常规管理目录 id=110 下，菜单ID 114-116，避开已用段）
INSERT IGNORE INTO sys_menu(id, parent_id, name, menu_code, perm, type, path, component_path, icon, sort, visible) VALUES
(114, 110, '系统配置', 'config', NULL, 1, 'config', 'common/config/index', 'Tools', 20, 1),
(115, 114, '配置查询', NULL, 'sys:config:query',  2, NULL, NULL, NULL, 1, 1),
(116, 114, '配置修改', NULL, 'sys:config:update', 2, NULL, NULL, NULL, 2, 1);
