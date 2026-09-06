

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
    `is_deleted`  TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删 1已删',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_goods` (`session_id`, `goods_id`) COMMENT '按场次查关联商品联合索引（含查重）',
    KEY `idx_goods_id` (`goods_id`) COMMENT '按商品反查关联场次索引',
    CONSTRAINT `fk_session_product_session` FOREIGN KEY (`session_id`) REFERENCES `t_session` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_session_product_goods` FOREIGN KEY (`goods_id`) REFERENCES `t_goods` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场次商品关联表（场次-商品-库存）';