-- ============================================================
-- 增量变更脚本（全部幂等，可重复执行）
-- ============================================================

-- 2026-09-06 订单终态语义 + 超时取消 + 委托撤销 --------------------------

-- 订单状态列注释：4 定义为「已完成」（确认收款即交易完成，代售流转属商品表）
ALTER TABLE `t_order`
    MODIFY COLUMN `order_status` tinyint NOT NULL COMMENT '订单状态：1待付款 2已付款 3已确认 4已完成 5已取消';

-- 取消来源列注释：4 超时自动取消（OrderTask 每分钟扫描 pay_deadline 过期待付款订单）
ALTER TABLE `t_order`
    MODIFY COLUMN `cancel_source` tinyint DEFAULT NULL COMMENT '取消来源：1待付款取消 2已付款取消 3代售中取消 4超时自动取消';

-- 委托记录状态列注释：新增 6 用户撤销（C端 entrust/cancel 接口）
ALTER TABLE `t_consign_record`
    MODIFY COLUMN `record_status` TINYINT NOT NULL COMMENT '委托记录状态:1待审核 2审核通过已上架 3已卖出 4未售出下架 5审核驳回 6用户撤销';

-- 菜单改名：订单终态语义对齐（管理端菜单文案）
UPDATE `sys_menu` SET `name` = '已完成订单' WHERE `id` = 94;
UPDATE `sys_menu` SET `name` = '已完成订单查询' WHERE `id` = 104;

-- recordSoldDirect 历史脏数据订正：确认收款直接成交路径早期误取 goods.memberId（promote 后已是买家），
-- 订正为订单快照卖家（与 recordSold 委托路径语义一致：memberId=成交前持有者）
UPDATE `t_consign_record` cr
    JOIN `t_order` o ON o.goods_id = cr.consign_goods_id AND o.buyer_id = cr.buyer_id AND o.order_status = 4
SET cr.member_id = o.seller_id,
    cr.member_name = o.seller_name
WHERE cr.record_status = 3
  AND cr.is_deleted = 0
  AND cr.member_id = cr.buyer_id
  AND cr.member_name IS NULL;
