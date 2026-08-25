-- =============================================
-- RBAC 权限体系建表脚本（DDL：仅建表，不含数据）
-- 表：sys_user / sys_role / sys_user_role / sys_menu / sys_role_menu
--    sys_invite_code / sys_invite_record
--    t_notice / t_notice_log
--    t_banner
--    t_file_info
--    t_goods / t_goods_operate_log
--    t_session
--    t_consign_goods / t_consign_goods_operate_log
--    t_order / t_order_operate_log
-- 关联关系：
--   sys_user  N<->N  sys_role        (通过 sys_user_role)
--   sys_role  N<->N  sys_menu        (通过 sys_role_menu)
--   sys_menu  自关联(parent_id) 形成目录-菜单-按钮三级树
-- 菜单类型：0目录 1菜单 2按钮权限
-- 外键：关联表(sys_user_role/sys_role_menu)均建立 FK，ON DELETE/UPDATE CASCADE；引擎统一 InnoDB
-- 数据初始化请执行 rbac_data.sql
-- =============================================

-- 1. 如果数据库不存在则创建，字符集utf8mb4（支持emoji），排序规则通用
CREATE DATABASE IF NOT EXISTS mall
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;

-- 2. 切换到刚创建的数据库
USE mall;

-- 0. 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名',
    password    VARCHAR(100) NOT NULL COMMENT '密码(BCrypt加密)',
    nickname    VARCHAR(50)  COMMENT '昵称',
    email       VARCHAR(100) COMMENT '邮箱',
    phone       VARCHAR(20)  COMMENT '手机号',
    age         TINYINT      COMMENT '年龄',
    gender      TINYINT      DEFAULT 0 COMMENT '性别 0未知 1男 2女',
    avatar      VARCHAR(255) COMMENT '头像图片地址',
    birthday    DATE         COMMENT '生日',
    status      TINYINT      DEFAULT 1 COMMENT '账号状态 0禁用 1正常',
    inviter_id  BIGINT       COMMENT '邀请人ID(sys_user.id)',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted  TINYINT      DEFAULT 0 COMMENT '逻辑删除 0未删 1已删'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '系统用户表';

-- 0.1 邀请码表（1个用户只能生成1个邀请码，单码最多邀请10人）
-- 设计：seq 由 Redis 发号器自增分配，invite_code 由 seq 经 54 进制编码生成，二者一一对应
CREATE TABLE IF NOT EXISTS sys_invite_code (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    seq             BIGINT       NOT NULL UNIQUE COMMENT '原始序列号(Redis发号器分配,与invite_code一一对应)',
    invite_code     VARCHAR(8)   NOT NULL UNIQUE COMMENT '邀请码(8位,区分大小写,数字+字母,由seq经54进制编码)',
    inviter_id      BIGINT       NOT NULL COMMENT '邀请人ID(生成者)',
    status          TINYINT      DEFAULT 0 COMMENT '0可用 1手动失效 2名额已满停用',
    max_invite_num  INT          DEFAULT 10 COMMENT '最大可邀请人数',
    used_invite_num INT          DEFAULT 0 COMMENT '已邀请注册人数(冗余,仅展示)',
    expire_time     DATETIME     COMMENT '过期时间(NULL永久有效)',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted      TINYINT      DEFAULT 0 COMMENT '逻辑删除 0未删 1已删',
    UNIQUE KEY uk_inviter (inviter_id),
    KEY idx_invite_code (invite_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '邀请码表';

-- 第2步：回填存量邀请码的 seq（通过 invite_code 反向解码，保证 seq ↔ invite_code 一一对应）
--        说明：MySQL 原生无 54 进制解码函数，需用 Java 工具批量回填；
--        若当前表行为空或可接受从新起点发号，也可直接按 1..N 递增分配 seq。
--        以下 SQL 提供"简单递增回填"（仅当表中无邀请码、或不关心历史 seq 对应关系时可用）：
--        UPDATE sys_invite_code SET seq = id WHERE seq IS NULL;

-- 第3步：加 NOT NULL 约束 + UNIQUE KEY（第2步完成、seq 全非空后再执行）
-- ALTER TABLE sys_invite_code MODIFY COLUMN seq BIGINT NOT NULL COMMENT '原始序列号(Redis发号器分配,与invite_code一一对应)';
-- ALTER TABLE sys_invite_code ADD UNIQUE KEY IF NOT EXISTS uk_seq (seq);

-- 0.2 邀请明细流水表（分佣核心：记录每次邀请注册行为，预留分佣字段）
CREATE TABLE IF NOT EXISTS sys_invite_record (
    id                  BIGINT         AUTO_INCREMENT PRIMARY KEY,
    invite_code         VARCHAR(8)     NOT NULL COMMENT '使用的邀请码',
    inviter_id          BIGINT         NOT NULL COMMENT '邀请人ID',
    invitee_id          BIGINT         NOT NULL COMMENT '被邀请人ID(新注册用户)',
    invitee_phone       VARCHAR(20)    COMMENT '被邀请人手机号(冗余)',
    status              TINYINT        DEFAULT 1 COMMENT '0已邀请待注册 1已注册 2已取消',
    commission_amount   DECIMAL(10,2)  DEFAULT 0.00 COMMENT '分佣金额(预留)',
    commission_status   TINYINT        DEFAULT 0 COMMENT '分佣状态 0待结算 1已结算 2已取消(预留)',
    settle_time         DATETIME       COMMENT '结算时间(预留)',
    create_time         DATETIME       DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted          TINYINT        DEFAULT 0 COMMENT '逻辑删除 0未删 1已删',
    UNIQUE KEY uk_invitee (invitee_id),
    KEY idx_inviter (inviter_id),
    KEY idx_invite_code (invite_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '邀请明细流水表';

-- 1. 角色表（权限分组载体：管理员、普通用户、运营等）
CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name   VARCHAR(50)  NOT NULL COMMENT '角色名称',
    role_code   VARCHAR(50)  NOT NULL UNIQUE COMMENT '角色编码',
    status      TINYINT      DEFAULT 1 COMMENT '状态 1启用 0禁用',
    is_deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '系统角色表';

-- 2. 用户-角色关联表（多对多：一个用户多个角色，一个角色多个用户）
CREATE TABLE IF NOT EXISTS sys_user_role (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID(sys_user.id)',
    role_id BIGINT NOT NULL COMMENT '角色ID(sys_role.id)',
    UNIQUE KEY uk_user_role (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '用户角色关联表';

-- 3. 菜单/权限表（核心：存储菜单目录、页面菜单、操作按钮，按钮统一存本表）
--    通过 type 区分：0目录 1菜单 2按钮权限
--    通过 parent_id 自关联形成树形结构
CREATE TABLE IF NOT EXISTS sys_menu (
    id           BIGINT       AUTO_INCREMENT PRIMARY KEY,
    parent_id    BIGINT       DEFAULT 0 COMMENT '父菜单ID(0表示顶级；自关联外键未加,因0非有效id,如需可改NULL后添加)',
    name         VARCHAR(50)  NOT NULL COMMENT '菜单/权限名称',
    menu_code    VARCHAR(100) COMMENT '菜单编码(目录/菜单可用，如 sys)',
    perm         VARCHAR(100) COMMENT '权限标识(按钮用，格式: 模块:页面:操作，如 sys:user:delete)',
    UNIQUE KEY uk_perm (perm),
    type         TINYINT      NOT NULL DEFAULT 1 COMMENT '类型 0目录 1菜单 2按钮权限',
    path         VARCHAR(200) COMMENT '路由路径(目录/菜单)',
    route_name   VARCHAR(100) COMMENT '路由名称(前端keep-alive匹配用,对应routeName)',
    component_path VARCHAR(200) COMMENT '前端组件路径(菜单,对应componentPath)',
    icon         VARCHAR(100) COMMENT '图标',
    sort         INT          DEFAULT 0 COMMENT '排序(数字越小越靠前)',
    visible      TINYINT      DEFAULT 1 COMMENT '是否可见 1可见 0隐藏',
    keep_alive   TINYINT      DEFAULT 0 COMMENT '是否缓存组件 1是 0否(对应keepAlive)',
    active_menu  VARCHAR(200) COMMENT '高亮菜单path(详情页等场景,对应activeMenu)',
    hide_in_menu TINYINT      DEFAULT 0 COMMENT '是否在菜单栏隐藏 1是 0否(对应hideInMenu)',
    hide_in_tag  TINYINT      DEFAULT 0 COMMENT '是否在标签栏隐藏 1是 0否(对应hideInTag)',
    hide_parent  TINYINT      DEFAULT 0 COMMENT '是否隐藏父级菜单 1是 0否(对应hideParent)',
    status       TINYINT      DEFAULT 1 COMMENT '状态 1启用 0禁用',
    is_deleted      TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '系统菜单/权限表(目录-菜单-按钮统一管理)';

-- 4. 角色-菜单关联表（多对多：一个角色绑定多个菜单/按钮）
CREATE TABLE IF NOT EXISTS sys_role_menu (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL COMMENT '角色ID(sys_role.id)',
    menu_id BIGINT NOT NULL COMMENT '菜单/权限ID(sys_menu.id)',
    UNIQUE KEY uk_role_menu (role_id, menu_id),
    CONSTRAINT fk_role_menu_role FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_role_menu_menu FOREIGN KEY (menu_id) REFERENCES sys_menu(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '角色菜单关联表';

-- =============================================
-- 公告模块表：t_notice / t_notice_log
-- =============================================

-- 5. 平台公告表
CREATE TABLE IF NOT EXISTS `t_notice` (
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '公告ID，主键',
    `title`       varchar(200) NOT NULL COMMENT '公告标题',
    `content`     longtext     NOT NULL COMMENT '富文本公告内容',
    `sort`        int          DEFAULT 0 COMMENT '排序，数值越大越靠前展示',
    `status`      tinyint      NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `is_deleted`  tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删 1已删',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   bigint       DEFAULT NULL COMMENT '操作人ID(管理员id)',
    `update_by`   bigint       DEFAULT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    KEY `idx_status_deleted` (`status`,`is_deleted`) COMMENT '查询C端公告联合索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台公告表';

-- 6. 公告阅读日志表（记录用户阅读公告的行为）
CREATE TABLE IF NOT EXISTS `t_notice_log` (
    `id`          bigint   NOT NULL AUTO_INCREMENT COMMENT '日志ID，主键',
    `notice_id`   bigint   NOT NULL COMMENT '公告ID(t_notice.id)',
    `user_id`     bigint   NOT NULL COMMENT '用户ID(sys_user.id)',
    `read_time`   datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_notice_user` (`notice_id`, `user_id`) COMMENT '同一用户同一条公告只记录一次',
    KEY `idx_user_id` (`user_id`) COMMENT '按用户查询阅读记录索引',
    CONSTRAINT `fk_notice_log_notice` FOREIGN KEY (`notice_id`) REFERENCES `t_notice` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_notice_log_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告阅读日志表';

-- =============================================
-- 轮播图模块表：banner
-- =============================================

-- 7. 轮播图表
CREATE TABLE IF NOT EXISTS `t_banner` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID(列表ID)',
    `img_url`     VARCHAR(512)    NOT NULL DEFAULT '' COMMENT '轮播图地址',
    `position`    VARCHAR(32)     NOT NULL DEFAULT 'home' COMMENT '轮播位置：home=首页 seckill=抢购',
    `sort`        INT             NOT NULL DEFAULT 0 COMMENT '权重，越大越靠前',
    `link_value`  VARCHAR(512)    NOT NULL DEFAULT '' COMMENT '跳转url',
    `status`      TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `is_deleted`  TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删 1已删',
    `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    `updated_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `create_by`   BIGINT          DEFAULT NULL COMMENT '操作人ID(管理员id)',
    `update_by`   BIGINT          DEFAULT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    KEY `idx_position` (`position`),
    KEY `idx_status_deleted` (`status`, `is_deleted`) COMMENT '查询C端轮播图联合索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='轮播图表';

-- =============================================
-- 文件管理模块表：t_file_info
-- =============================================

-- 8. 文件信息表（上传文件元数据，支持假删除）
CREATE TABLE IF NOT EXISTS `t_file_info` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `url`            VARCHAR(512)    NOT NULL COMMENT '访问URL(完整路径)',
    `original_name`  VARCHAR(255)    NOT NULL DEFAULT '' COMMENT '原始文件名',
    `filename`       VARCHAR(255)    NOT NULL DEFAULT '' COMMENT '存储后的文件名',
    `path`           VARCHAR(512)    NOT NULL DEFAULT '' COMMENT '存储相对路径(含子目录)',
    `size`           BIGINT          NOT NULL DEFAULT 0 COMMENT '文件大小(字节)',
    `suffix`         VARCHAR(20)     NOT NULL DEFAULT '' COMMENT '文件后缀(小写,无点)',
    `biz_type`       VARCHAR(50)     NOT NULL DEFAULT '' COMMENT '业务类型:avatar/goods/document',
    `platform`       VARCHAR(50)     NOT NULL DEFAULT '' COMMENT '存储平台:local-1/aliyun-oss-1等',
    `bucket`         VARCHAR(100)    DEFAULT NULL COMMENT '存储桶(OSS/MinIO等)',
    `base_path`      VARCHAR(512)    DEFAULT NULL COMMENT '存储基础路径',
    `status`         TINYINT         NOT NULL DEFAULT 1 COMMENT '状态:0-已删除(假删) 1-正常',
    `is_deleted`     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删 1已删',
    `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    `updated_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`      BIGINT          DEFAULT NULL COMMENT '上传人ID(管理员id)',
    `update_by`      BIGINT          DEFAULT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_url` (`url`) COMMENT 'URL唯一,防重复上传入库',
    KEY `idx_biz_type` (`biz_type`) COMMENT '按业务类型查询索引',
    KEY `idx_create_by` (`create_by`) COMMENT '按上传人查询索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件信息表';

-- =============================================
-- 商品模块表：t_goods / t_goods_operate_log
-- =============================================

-- 9. 商品表
-- 注：原 SQL 中 is_deleted 使用 DATETIME，且 idx_deleted_at 引用了不存在的 deleted_at 列
--     此处对齐项目规范：is_deleted 改为 TINYINT DEFAULT 0 配合 @TableLogic，索引同步修正
CREATE TABLE IF NOT EXISTS `t_goods` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `goods_name`     VARCHAR(255)    NOT NULL DEFAULT '' COMMENT '商品名称',
    `category_name`  VARCHAR(128)    NOT NULL DEFAULT '' COMMENT '商品种类名称',
    `goods_sn`       VARCHAR(64)     NOT NULL DEFAULT '' COMMENT '商品货号/编码，唯一',
    `goods_thumb`    VARCHAR(512)    NOT NULL DEFAULT '' COMMENT '商品缩略图URL',
    `price`          DECIMAL(12,2)   NOT NULL DEFAULT 0.00 COMMENT '商品售价',
    `stock`          INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '库存数量',
    `sales`          INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '销量',
    `status`         TINYINT         NOT NULL DEFAULT 0 COMMENT '商品状态 0=待上架 1=已上架',
    `is_deleted`     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删 1已删',
    `create_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    `update_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`      BIGINT          DEFAULT NULL COMMENT '创建人ID(管理员id)',
    `update_by`      BIGINT          DEFAULT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_goods_sn` (`goods_sn`) COMMENT '货号唯一索引',
    KEY `idx_status_deleted` (`status`, `is_deleted`) COMMENT '查询已上架未删除商品联合索引',
    KEY `idx_category_name` (`category_name`) COMMENT '按商品种类查询索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 10. 商品操作日志表（记录新增/编辑/删除/上下架行为，content 存变更内容JSON: {before,after,changedFields,remark}）
CREATE TABLE IF NOT EXISTS `t_goods_operate_log` (
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `goods_id`     BIGINT UNSIGNED NOT NULL COMMENT '商品ID(t_goods.id)',
    `admin_id`     BIGINT UNSIGNED NOT NULL COMMENT '操作管理员ID(sys_user.id)',
    `operate_type` TINYINT         NOT NULL COMMENT '操作类型 1新增 2编辑 3删除 4上下架',
    `operate_desc` VARCHAR(255)    DEFAULT NULL COMMENT '操作中文描述(如:新增商品/编辑商品/删除商品/上下架)，列表展示用，避免每次解析JSON',
    `ip`           VARCHAR(50)     DEFAULT NULL COMMENT '操作人客户端IP，溯源定位操作来源',
    `user_agent`   VARCHAR(500)    DEFAULT NULL COMMENT '操作人浏览器/客户端设备信息，安全审计用',
    `content`      TEXT            DEFAULT NULL COMMENT '变更内容JSON，格式: {"before":{...},"after":{...},"changedFields":["xxx"],"remark":"编辑商品基础信息"}，before/after为前后快照',
    `create_time`  DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_goods_id` (`goods_id`) COMMENT '按商品查询操作记录索引',
    KEY `idx_admin_id` (`admin_id`) COMMENT '按操作人查询索引',
    KEY `idx_create_time` (`create_time`) COMMENT '按操作时间查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品操作日志表';

-- =============================================
-- 抢购场次模块表：t_session（抢购系统设置/抢购时间设置）
-- =============================================

-- 11. 抢购场次表（一个活动下有多场抢购场次）
-- 注：在用户提供的原表结构基础上补充 is_deleted 字段，对齐项目逻辑删除规范（@TableLogic）
CREATE TABLE IF NOT EXISTS `t_session` (
    `id`                   BIGINT       AUTO_INCREMENT PRIMARY KEY COMMENT '场次主键ID',
    `session_name`         VARCHAR(64)  NOT NULL COMMENT '场次名称：上午场/下午场',
    `session_status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '场次状态 1开启 0关闭',
    `enter_control_minute` INT          NOT NULL DEFAULT 0 COMMENT '进场时间控制(分钟)',
    `rush_start_time`      DATETIME     NOT NULL COMMENT '抢购开始时间（完整年月日时分秒，例：2026-08-18 09:50:00）',
    `rush_end_time`        DATETIME     NOT NULL COMMENT '抢购结束时间（完整年月日时分秒，例：2026-08-18 17:00:00）',
    `max_buy_count`        INT          NOT NULL DEFAULT 1 COMMENT '最多购买次数(次)',
    `before_forbid_minute` INT          NOT NULL DEFAULT 0 COMMENT '开场前禁止委托时间(分钟)',
    `after_forbid_minute`  INT          NOT NULL DEFAULT 0 COMMENT '结束后禁止委托时间(分钟)',
    `bg_img`               VARCHAR(255) DEFAULT '' COMMENT '场次背景图地址',
    `sort`                 INT          NOT NULL DEFAULT 0 COMMENT '排序号（用来前端按顺序展示第1场、第2场）',
    `is_deleted`           TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删 1已删',
    `create_time`          DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time`          DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_status_deleted` (`session_status`, `is_deleted`) COMMENT '查询启用场次联合索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抢购场次表';

-- =============================================
-- 抢购托售商品模块表：t_consign_goods
-- =============================================

-- 12. 抢购托售商品主表（对应编辑页 + 列表）
CREATE TABLE IF NOT EXISTS `t_consign_goods` (
    `id`            BIGINT        AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    `goods_name`    VARCHAR(255)  COMMENT '抢购区商品名称',
    `goods_price`   DECIMAL(10,2) COMMENT '抢购区商品价格',
    `member_id`     BIGINT        COMMENT '本轮委托人ID，关联sys_user',
    `session_id`    BIGINT        COMMENT '所属场次ID，关联t_session',
    `cover_img`     VARCHAR(500)  COMMENT '商品缩略图url',
    `detail_img`    VARCHAR(500)  COMMENT '商品详情图url',
    `goods_detail`  TEXT          COMMENT '商品详情富文本',
    `sale_times`    INT           DEFAULT 0 COMMENT '委托售卖次数',
    `goods_status`  TINYINT       COMMENT '商品业务状态 1挂卖中 2已抢购待付款 3等待确认付款 4待处理 5委托代卖 6委托发货',
    `online_status` TINYINT       DEFAULT 0 COMMENT '0待上架 1上架',
    `is_deleted`    TINYINT       DEFAULT 0 COMMENT '假删除 0正常 1删除',
    `create_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_member` (`member_id`) COMMENT '按委托人查询索引',
    KEY `idx_session` (`session_id`) COMMENT '按场次查询索引',
    KEY `idx_goods_status` (`goods_status`) COMMENT '按业务状态查询索引',
    KEY `idx_online_status` (`online_status`, `is_deleted`) COMMENT '查询上架未删除商品联合索引',
    CONSTRAINT `fk_consign_goods_member` FOREIGN KEY (`member_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_consign_goods_session` FOREIGN KEY (`session_id`) REFERENCES `t_session` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抢购托售商品主表';

-- 13. 抢购托售商品操作日志表（记录新增/编辑/删除/上下架/业务状态流转，content 存变更内容JSON: {before,after,changedFields,remark}）
-- 注：日志表不建外键，避免级联删除丢失审计记录、保留完整历史
CREATE TABLE IF NOT EXISTS `t_consign_goods_operate_log` (
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `consign_goods_id` BIGINT UNSIGNED NOT NULL COMMENT '托售商品ID(t_consign_goods.id)',
    `admin_id`     BIGINT UNSIGNED NOT NULL COMMENT '操作管理员ID(sys_user.id)',
    `operate_type` TINYINT         NOT NULL COMMENT '操作类型 1新增 2编辑 3删除 4上下架 5业务状态流转',
    `operate_desc` VARCHAR(255)    DEFAULT NULL COMMENT '操作中文描述(如:新增/编辑/删除/上下架/状态流转:挂卖中->待付款)，列表展示用，避免每次解析JSON',
    `from_status`  TINYINT         DEFAULT NULL COMMENT '业务流转前状态(仅operate_type=5有效 1挂卖中 2已抢购待付款 3等待确认付款 4待处理 5委托代卖 6委托发货)',
    `to_status`    TINYINT         DEFAULT NULL COMMENT '业务流转后状态(仅operate_type=5有效)',
    `ip`           VARCHAR(50)     DEFAULT NULL COMMENT '操作人客户端IP，溯源定位操作来源',
    `user_agent`   VARCHAR(500)    DEFAULT NULL COMMENT '操作人浏览器/客户端设备信息，安全审计用',
    `content`      TEXT            DEFAULT NULL COMMENT '变更内容JSON，格式: {"before":{...},"after":{...},"changedFields":["xxx"],"remark":"状态流转:挂卖中->待付款"}，before/after为前后快照',
    `create_time`  DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_consign_goods_id` (`consign_goods_id`) COMMENT '按托售商品查询操作记录索引',
    KEY `idx_admin_id` (`admin_id`) COMMENT '按操作人查询索引',
    KEY `idx_create_time` (`create_time`) COMMENT '按操作时间查询',
    KEY `idx_biz_flow` (`to_status`, `from_status`) COMMENT '业务状态流转查询索引(按目标状态+源状态)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抢购托售商品操作日志表';

-- =============================================
-- 订单模块表：t_order / t_order_operate_log
-- =============================================

-- 14. 抢购订单主表（方案B：下单快照版本）
-- 金额统一使用 DECIMAL，禁止 double/float；手机号/姓名等均为下单时快照，避免后续用户信息变更影响历史订单
CREATE TABLE IF NOT EXISTS `t_order` (
    `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_no`          varchar(64)  NOT NULL COMMENT '唯一订单编号',
    `goods_id`          bigint       NOT NULL COMMENT '商品ID',
    `goods_name`        varchar(128) NOT NULL COMMENT '商品名称【下单快照】',
    `seller_id`         bigint       DEFAULT NULL COMMENT '卖方会员ID 关联sys_user.id',
    `seller_name`       varchar(32)  DEFAULT NULL COMMENT '卖家姓名【下单快照】',
    `seller_phone`      varchar(11)  DEFAULT NULL COMMENT '卖家手机号【下单快照】',
    `buyer_id`          bigint       NOT NULL COMMENT '买方会员ID 关联sys_user.id',
    `buyer_name`        varchar(32)  DEFAULT NULL COMMENT '买家姓名【下单快照】',
    `buyer_phone`       varchar(11)  DEFAULT NULL COMMENT '买家手机号【下单快照】',
    `rush_price`        decimal(18,2) NOT NULL COMMENT '抢购成交价格',
    `receive_address`   varchar(512) DEFAULT NULL COMMENT '收货地址完整拼接字符串',
    `order_status`      tinyint      NOT NULL COMMENT '订单状态：1待付款 2已付款 3已确认 4已代售 5已取消',
    `put_commission`    decimal(18,2) DEFAULT 0.00 COMMENT '上架手续费',
    `coupon_amount`     decimal(18,2) DEFAULT 0.00 COMMENT '优惠券抵扣金额',
    `pay_voucher_url`   varchar(255) DEFAULT NULL COMMENT '支付凭证图片地址',
    `pay_deadline`      datetime     DEFAULT NULL COMMENT '付款截止时间（倒计时）',
    `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单创建时间',
    `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`        tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_buyer_id` (`buyer_id`),
    KEY `idx_seller_id` (`seller_id`),
    KEY `idx_goods_id` (`goods_id`),
    KEY `idx_order_status` (`order_status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抢购订单主表';

-- 15. 订单操作审计日志（保留，记录状态变更流水）
-- 注：日志表不建外键，避免级联删除丢失审计记录、保留完整历史
-- operate_type 存数字编码（对齐 t_goods_operate_log / t_consign_goods_operate_log），
-- operate_desc 冗余存汉字用于列表展示，避免每次查库都要反查枚举
CREATE TABLE IF NOT EXISTS `t_order_operate_log` (
    `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id`          bigint       NOT NULL COMMENT '订单ID 关联t_order.id',
    `before_status`     tinyint      DEFAULT NULL COMMENT '操作前订单状态',
    `after_status`      tinyint      NOT NULL COMMENT '操作后订单状态',
    `operate_type`      tinyint      NOT NULL COMMENT '操作类型数字编码 1上传凭证 2确认收款 3取消订单 4删除订单 5超时自动取消',
    `operate_desc`      varchar(255) DEFAULT NULL COMMENT '操作类型中文描述(冗余展示列，如:确认收款/取消订单)',
    `operate_user_id`   bigint       DEFAULT NULL COMMENT '操作人管理员/会员ID 关联sys_user.id',
    `operate_user_name` varchar(64)  DEFAULT NULL COMMENT '操作人名称快照',
    `remark`            varchar(512) DEFAULT NULL COMMENT '操作备注',
    `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_operate_type` (`operate_type`),
    KEY `idx_operate_user_id` (`operate_user_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单操作审计日志';
