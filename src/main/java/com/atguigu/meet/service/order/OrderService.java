package com.atguigu.meet.service.order;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.order.AllOrderQueryDTO;
import com.atguigu.meet.model.dto.order.OrderOperateDTO;
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

    /** 管理员确认收款（仅待确认可用，status 2->3->4 自动流转到已代售） */
    Response confirmReceive(OrderOperateDTO dto);
}
