-- =============================================
-- 场次商品关联模块表：t_session_product
-- =============================================
-- 设计要点：
--   1. 场次与抢购商品多对多关联：每行即「场次X - 商品Y - 库存Z」，库存行级挂在关联上
--   2. 同场次同商品仅一条有效关联（service 层查重；不用唯一索引以兼容逻辑删除后重新关联）
--   3. 抢购扣减走本表 stock 行级扣减，与 t_goods.stock（商品自身库存）互不干扰
CREATE TABLE IF NOT EXISTS `t_session_product` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `session_id`  BIGINT        NOT NULL COMMENT '场次ID，关联t_session.id',
    `goods_id`    BIGINT        NOT NULL COMMENT '抢购商品ID，关联t_goods.id',
    `stock`       INT UNSIGNED  NOT NULL DEFAULT 0 COMMENT '该场次该商品的抢购库存',
    `sort`        INT           NOT NULL DEFAULT 0 COMMENT '排序号（场次内商品展示顺序）',
    `is_deleted`  TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删 1已删',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_goods` (`session_id`, `goods_id`) COMMENT '按场次查关联商品联合索引（含查重）',
    KEY `idx_goods_id` (`goods_id`) COMMENT '按商品反查关联场次索引'
    -- 外键约束（需确保 t_session 和 t_goods 表已存在后再手动添加）:
    -- CONSTRAINT `fk_session_product_session` FOREIGN KEY (`session_id`) REFERENCES `t_session` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    -- CONSTRAINT `fk_session_product_goods` FOREIGN KEY (`goods_id`) REFERENCES `t_goods` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场次商品关联表（场次-商品-库存）';


-- =============================================
-- 系统设置模块 (sys_settings) 建表
-- 设计要点：
--   1. 单行表(固定字段), 与 sys_config(key-value 动态表) 并存
--      - sys_config 负责灵活扩展的配置项(站点名称/备案号/CDN/支付方式等)
--      - sys_settings 负责固定结构/一次性读写的业务设置(会员权益/抢单规则/推广海报等)
--   2. 插入时固定 id=1 保证全局只有一行; 后续只 UPDATE
--   3. 字段与截图中文一一对应, 类型按语义选 INT/DECIMAL/VARCHAR/TINYINT(1)
-- =============================================
CREATE TABLE IF NOT EXISTS `sys_settings` (
    `id`                         BIGINT        NOT NULL COMMENT '主键（固定为1，保证全局单行）',

    -- ========== 站点设置 ==========
    `site_name`                  VARCHAR(128)  NOT NULL DEFAULT '' COMMENT '站点名称',
    `site_logo`                  VARCHAR(512)  DEFAULT NULL COMMENT '站点Logo图片URL',

    -- ========== 会员权益 ==========
    `new_member_days`            INT           NOT NULL DEFAULT 0 COMMENT '新会员身份有效期(天), 注册后多少天内属于新会员',
    `new_member_advance_minutes` INT           NOT NULL DEFAULT 0 COMMENT '新会员提前抢购(分钟), 设0表示关闭提前抢购权益',

    -- ========== 抢单规则 ==========
    -- `pre_view_minutes`           INT           NOT NULL DEFAULT 0 COMMENT '可提前查看抢购商品(分钟), 0表示开抢时才展示商品',
    `limit_rule`                 TINYINT       NOT NULL DEFAULT 0 COMMENT '会员限购规则: 0不限购 1同一场次限购一次 2当天限购一次',
    -- `share_valid_days`           INT           NOT NULL DEFAULT 0 COMMENT '分享资格有效期(天), 老会员最近多少天内需成功邀请新会员, 0关闭',
    `recommend_rate`             DECIMAL(5,2)  NOT NULL DEFAULT 0.00 COMMENT '推荐奖比例(%), 直属会员成功下单时按订单金额计算',
    -- `referrer_purchase_days`     INT           NOT NULL DEFAULT 0 COMMENT '推荐人购买有效期(天), 0不限; 大于0时从推荐人最后一次下单向后计算',
    `self_buy_rate`              DECIMAL(5,2)  NOT NULL DEFAULT 0.00 COMMENT '自购奖励比例(%), 按会员本人订单金额计算',
    `self_buy_bonus_ratio`       DECIMAL(5,2)  NOT NULL DEFAULT 0.00 COMMENT '自购奖金占比(%), 自购奖金与购物券占比合计须为100',
    `coupon_ratio`               DECIMAL(5,2)  NOT NULL DEFAULT 0.00 COMMENT '购物券占比(%), 自购奖金与购物券占比合计须为100',
    `show_self_buy_bonus`        TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '前端显示自购奖: 0否 1是',
    `show_coupon`                TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '前端显示购物券: 0否 1是',
    `order_profit_rate`          DECIMAL(5,2)  NOT NULL DEFAULT 0.00 COMMENT '订单利润比例(%), 订单创建时按订单金额计算并保存',

    -- ========== 推广海报 ==========
    `poster_bg_image`            VARCHAR(512)  DEFAULT NULL COMMENT '推广海报背景图URL',

    `create_time`                DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`                DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`                 TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统设置(单行表, 全量读写)';