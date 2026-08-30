package com.atguigu.meet.service.order;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.order.AllOrderQueryDTO;
import com.atguigu.meet.model.dto.order.OrderOperateDTO;
import com.atguigu.meet.model.dto.order.PlaceOrderDTO;
import com.atguigu.meet.model.dto.order.UploadVoucherDTO;

/**
 * 订单管理 Service
 * <p>
 * 订单状态流转：1待付款 -> 2已付款 -> 3已确认 -> 4已代售
 * 分支：待付款/已付款 -> 5已取消
 * 删除订单：仅待付款可用，逻辑删除 + 商品状态回滚
 */
public interface OrderService {

    /** 所有订单分页列表（支持状态筛选） */
    Response listAll(AllOrderQueryDTO parameter);

    /** 待付款订单列表（status=1） */
    Response listWaitPay(AllOrderQueryDTO parameter);

    /** 待确认收款订单列表（status=2） */
    Response listWaitConfirm(AllOrderQueryDTO parameter);

    /** 代售记录列表（status=4） */
    Response listAgentSale(AllOrderQueryDTO parameter);

    /** 已取消订单列表（status=5，仅查询） */
    Response listCancel(AllOrderQueryDTO parameter);

    /** 待付款订单上传支付凭证（status 1->2） */
    Response uploadVoucher(UploadVoucherDTO dto);

    /** 取消订单（待付款/待确认均可，status 1/2->5） */
    Response cancelOrder(OrderOperateDTO dto);

    /** 删除订单（仅待付款可用，逻辑删除 + 商品状态回滚） */
    Response deleteOrder(OrderOperateDTO dto);

    /** 管理员确认收款（仅待确认可用，status 2->3->4 自动流转到已代售；商品 3->4待处理交由买家，可申请委托代卖） */
    Response confirmReceive(OrderOperateDTO dto);

    // ====================== C 端用户接口（带 buyerId 归属校验） ======================

    /**
     * C 端用户抢购下单
     * <p>校验抢购时间窗口 + 限购 + 商品状态条件更新(1挂卖中->2已抢购待付款) + 建单 + pay_deadline
     */
    Response placeOrder(PlaceOrderDTO dto, Long currentUserId);

    /** C 端用户取消订单（校验订单归属当前买家，复用取消核心逻辑） */
    Response cancelOrderByUser(OrderOperateDTO dto, Long currentUserId);

    /** C 端用户上传支付凭证（校验订单归属当前买家，复用上传核心逻辑） */
    Response uploadVoucherByUser(UploadVoucherDTO dto, Long currentUserId);
}
