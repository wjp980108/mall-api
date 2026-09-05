-- 公告表新增 position 字段
ALTER TABLE `t_notice` ADD COLUMN `position` varchar(32) NOT NULL DEFAULT 'home' COMMENT '公告位置：home=首页' AFTER `content`;
ALTER TABLE `t_notice` ADD INDEX `idx_position` (`position`);