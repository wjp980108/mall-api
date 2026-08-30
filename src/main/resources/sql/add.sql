
-- =============================================
-- 用户地址簿模块表：t_user_address
-- =============================================

-- 16. 用户收货地址簿（C端用户管理多收货地址，下单时选择）
-- 注：address 存完整拼接字符串，对齐 t_order.receive_address 语义；下单快照直接写入订单
CREATE TABLE IF NOT EXISTS `t_user_address` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`        BIGINT       NOT NULL COMMENT '所属用户ID 关联sys_user.id',
    `receiver_name`  VARCHAR(50)  NOT NULL COMMENT '收货人姓名',
    `receiver_phone` VARCHAR(20) NOT NULL COMMENT '收货人手机号',
    `address`        VARCHAR(512) NOT NULL COMMENT '收货地址完整拼接字符串',
    `is_default`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认 0否 1是',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删 1已删',
    PRIMARY KEY (`id`),
    KEY `idx_user_default` (`user_id`, `is_default`, `is_deleted`) COMMENT '查询用户地址列表（含默认置顶）',
    CONSTRAINT `fk_user_address_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收货地址簿';
