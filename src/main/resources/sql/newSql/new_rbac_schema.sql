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
    `pre_view_minutes`           INT           NOT NULL DEFAULT 0 COMMENT '可提前查看抢购商品(分钟), 0表示开抢时才展示商品',
    `share_valid_days`           INT           NOT NULL DEFAULT 0 COMMENT '分享资格有效期(天), 老会员最近多少天内需成功邀请新会员, 0关闭',
    `referrer_purchase_days`     INT           NOT NULL DEFAULT 0 COMMENT '推荐人购买有效期(天), 0不限; 大于0时从推荐人最后一次下单向后计算',
    `show_self_buy_bonus`        TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '前端显示自购奖: 0否 1是',
    `show_coupon`                TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '前端显示购物券: 0否 1是',
    `limit_rule`                 TINYINT       NOT NULL DEFAULT 0 COMMENT '会员限购规则: 0不限购 1同一场次限购一次 2当天限购一次',
    
    `recommend_rate`             DECIMAL(5,2)  NOT NULL DEFAULT 0.00 COMMENT '推荐奖比例(%), 直属会员成功下单时按订单金额计算',
    `self_buy_rate`              DECIMAL(5,2)  NOT NULL DEFAULT 0.00 COMMENT '自购奖励比例(%), 按会员本人订单金额计算',
    `self_buy_bonus_ratio`       DECIMAL(5,2)  NOT NULL DEFAULT 0.00 COMMENT '自购奖金占比(%), 自购奖金与购物券占比合计须为100',
    `coupon_ratio`               DECIMAL(5,2)  NOT NULL DEFAULT 0.00 COMMENT '购物券占比(%), 自购奖金与购物券占比合计须为100',
    `order_profit_rate`          DECIMAL(5,2)  NOT NULL DEFAULT 0.00 COMMENT '订单利润比例(%), 订单创建时按订单金额计算并保存',

    -- ========== 推广海报 ==========
    `poster_bg_image`            VARCHAR(512)  DEFAULT NULL COMMENT '推广海报背景图URL',

    `create_time`                DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`                DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`                 TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统设置(单行表, 全量读写)';


-- =============================================
-- 抢购订单模块表：t_rob_order
-- =============================================
-- 设计要点：
--   1. 基于「场次商品关联(t_session_product)」的平台抢购订单，下单即成交，极简两态(1正常/2已取消)
--   2. 与旧 t_order(托售二手流转)完全隔离，互不引用
--   3. 场次/商品/买家/推荐人/金额全部在下单瞬间快照冻结：改收益比例不影响历史订单
--   4. 库存扣减锚点 session_product_id：取消时按它把数量加回 t_session_product.stock
--   5. 利润池口径：profit_amount=订单总额×order_profit_rate%(奖励总上限,非平台净利)
CREATE TABLE IF NOT EXISTS `t_rob_order` (
    `id`                     BIGINT        NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_no`               VARCHAR(64)   NOT NULL COMMENT '订单编号(22位,时间+用户+随机)',
    `session_product_id`     BIGINT        NOT NULL COMMENT '场次商品关联ID(库存扣减/回滚锚点),关联t_session_product.id',
    `session_id`             BIGINT        NOT NULL COMMENT '场次ID快照,关联t_session.id',
    `session_name`           VARCHAR(128)  NOT NULL DEFAULT '' COMMENT '场次名称快照',
    `rush_start_time`        TIME          DEFAULT NULL COMMENT '场次每日抢购开始时间快照',
    `rush_end_time`          TIME          DEFAULT NULL COMMENT '场次每日抢购结束时间快照',
    `goods_id`               BIGINT        NOT NULL COMMENT '商品ID快照,关联t_goods.id',
    `goods_name`             VARCHAR(255)  NOT NULL DEFAULT '' COMMENT '商品名称快照',
    `goods_sn`               VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '商品货号快照',
    `goods_thumb`            VARCHAR(512)  DEFAULT NULL COMMENT '商品缩略图URL快照',
    `goods_thumb_platform`   VARCHAR(32)   DEFAULT NULL COMMENT '商品缩略图存储平台',
    `unit_price`             DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '商品单价快照',
    `quantity`               INT           NOT NULL DEFAULT 1 COMMENT '购买数量',
    `total_amount`           DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '订单实付总额=单价×数量',
    `profit_amount`          DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '利润池上限=总额×订单利润比例%',
    `recommend_amount`       DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '推荐奖=总额×推荐奖比例%(发邀请人)',
    `self_buy_amount`        DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '自购奖=总额×自购奖励比例%',
    `self_buy_bonus_amount`  DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '自购奖金=自购奖×自购奖金占比%(计入可用积分)',
    `self_buy_coupon_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '购物券=自购奖×购物券占比%(计入购物券积分)',
    `buyer_id`               BIGINT        NOT NULL COMMENT '买家ID,关联sys_user.id',
    `buyer_name`             VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '买家姓名快照',
    `buyer_phone`            VARCHAR(20)   NOT NULL DEFAULT '' COMMENT '买家手机号快照',
    `buyer_avatar`           VARCHAR(512)  DEFAULT NULL COMMENT '买家头像URL快照',
    `buyer_avatar_platform`  VARCHAR(32)   DEFAULT NULL COMMENT '买家头像存储平台',
    `inviter_id`             BIGINT        DEFAULT NULL COMMENT '推荐人(买家邀请人)ID快照,关联sys_user.id,无则为空',
    `inviter_name`           VARCHAR(64)   DEFAULT NULL COMMENT '推荐人姓名快照',
    `order_status`           TINYINT       NOT NULL DEFAULT 1 COMMENT '订单状态 1正常 2已取消',
    `is_deleted`             TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删 1已删',
    `create_time`            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(下单时间)',
    `update_time`            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`) COMMENT '订单编号唯一',
    KEY `idx_session_id` (`session_id`) COMMENT '按场次筛选',
    KEY `idx_buyer_id` (`buyer_id`) COMMENT '按买家查询(我的订单/限购统计)',
    KEY `idx_inviter_id` (`inviter_id`) COMMENT '按推荐人查询',
    KEY `idx_session_product_id` (`session_product_id`) COMMENT '库存锚点反查'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抢购订单表(场次商品库存下单,金额快照冻结)';


-- =============================================
-- 抢购订单操作审计表：t_rob_order_operate_log
-- =============================================
-- 结构对齐 t_order_operate_log，记录下单/取消/转移三类流水，不做逻辑删除
CREATE TABLE IF NOT EXISTS `t_rob_order_operate_log` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `order_id`          BIGINT       NOT NULL COMMENT '订单ID,关联t_rob_order.id',
    `before_status`     TINYINT      DEFAULT NULL COMMENT '操作前订单状态',
    `after_status`      TINYINT      DEFAULT NULL COMMENT '操作后订单状态',
    `operate_type`      TINYINT      NOT NULL COMMENT '操作类型 1下单 2取消订单 3转移订单',
    `operate_desc`      VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '操作类型中文描述(冗余展示)',
    `operate_user_id`   BIGINT       DEFAULT NULL COMMENT '操作人ID(管理员/会员),关联sys_user.id',
    `operate_user_name` VARCHAR(64)  DEFAULT NULL COMMENT '操作人名称快照',
    `remark`            VARCHAR(255) DEFAULT NULL COMMENT '操作备注(转移记录原买家→新买家)',
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`) COMMENT '按订单查审计流水'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抢购订单操作审计日志';


-- =============================================
-- 用户积分账户表：t_user_points
-- =============================================
-- 设计要点：
--   1. 每个用户一行,双余额:points 可用积分(推荐奖+自购奖金,可转让) / coupon_points 购物券积分(锁死不可转)
--   2. 积分即金额,DECIMAL(12,2);账户行不存在时由业务 INSERT IGNORE 初始化
--   3. 变动走行锁(SELECT ... FOR UPDATE)在订单事务内串行化,冲回允许负余额(平台待追回)
CREATE TABLE IF NOT EXISTS `t_user_points` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`       BIGINT        NOT NULL COMMENT '用户ID,关联sys_user.id',
    `points`        DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '可用积分(推荐奖+自购奖金),可转让',
    `coupon_points` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '购物券积分(自购返券),锁死不可转让',
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`) COMMENT '一个用户一个积分账户'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户积分账户(双余额:可用积分/购物券积分)';


-- =============================================
-- 用户积分流水表：t_user_points_flow
-- =============================================
-- 每笔积分变动一行:biz_type 区分奖项来源,flow_type 区分方向,account_type 区分计入哪个余额
CREATE TABLE IF NOT EXISTS `t_user_points_flow` (
    `id`                   BIGINT        NOT NULL AUTO_INCREMENT COMMENT '流水ID',
    `user_id`              BIGINT        NOT NULL COMMENT '积分归属人ID,关联sys_user.id',
    `order_id`             BIGINT        DEFAULT NULL COMMENT '关联订单ID,关联t_rob_order.id(转让时为空)',
    `order_no`             VARCHAR(64)   DEFAULT NULL COMMENT '关联订单编号(冗余展示)',
    `biz_type`             TINYINT       NOT NULL COMMENT '业务类型 1推荐奖 2自购奖 3购物券奖 4积分对冲(转让)',
    `flow_type`            TINYINT       NOT NULL COMMENT '变动类型 1收入 2冲回 3转让转出 4转让转入',
    `account_type`         TINYINT       NOT NULL COMMENT '计入账户 1可用积分 2购物券积分',
    `amount`               DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '变动金额绝对值(方向由flow_type表达)',
    `before_points`        DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '变动前对应账户余额',
    `after_points`         DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '变动后对应账户余额',
    `counterparty_user_id` BIGINT        DEFAULT NULL COMMENT '转让对方用户ID(仅转让流水)',
    `counterparty_name`    VARCHAR(64)   DEFAULT NULL COMMENT '转让对方姓名(仅转让流水)',
    `remark`               VARCHAR(255)  DEFAULT NULL COMMENT '备注',
    `create_time`          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_time` (`user_id`, `create_time`) COMMENT '按用户查明细分页',
    KEY `idx_order_id` (`order_id`) COMMENT '按订单查积分流水'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户积分流水(收入/冲回/转让)';
