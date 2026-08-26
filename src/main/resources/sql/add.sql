ALTER TABLE sys_user        ADD COLUMN avatar_platform        VARCHAR(50) DEFAULT NULL COMMENT '头像存储平台'        AFTER avatar;
ALTER TABLE t_goods         ADD COLUMN goods_thumb_platform   VARCHAR(50) DEFAULT NULL COMMENT '商品缩略图存储平台'   AFTER goods_thumb;
ALTER TABLE t_consign_goods ADD COLUMN cover_img_platform     VARCHAR(50) DEFAULT NULL COMMENT '商品缩略图存储平台'   AFTER cover_img;
ALTER TABLE t_consign_goods ADD COLUMN detail_img_platform    VARCHAR(50) DEFAULT NULL COMMENT '商品详情图存储平台'   AFTER detail_img;
ALTER TABLE t_session       ADD COLUMN bg_img_platform       VARCHAR(50) DEFAULT NULL COMMENT '场次背景图存储平台'   AFTER bg_img;
ALTER TABLE t_order         ADD COLUMN pay_voucher_platform  VARCHAR(50) DEFAULT NULL COMMENT '支付凭证存储平台'     AFTER pay_voucher_url;
ALTER TABLE t_banner        ADD COLUMN img_url_platform      VARCHAR(50) DEFAULT NULL COMMENT '轮播图存储平台'       AFTER img_url;