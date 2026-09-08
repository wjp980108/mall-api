package com.atguigu.meet.service.roborder;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.roborder.PlaceRobOrderDTO;
import com.atguigu.meet.model.dto.roborder.RobOrderPageQueryDTO;
import com.atguigu.meet.model.dto.roborder.RobOrderTransferDTO;

/**
 * 抢购订单 Service
 */
public interface RobOrderService {

    /**
     * C 端抢购下单：校验场次/时间窗口/限购 → 条件扣库存 → 建单(金额快照) → 发放积分
     */
    Response placeOrder(PlaceRobOrderDTO dto, Long currentUserId);

    /**
     * 管理端取消订单：条件置已取消 → 回滚库存 → 冲回积分（幂等）
     */
    Response cancelOrder(Long orderId);

    /**
     * 管理端转移订单：重写买家快照 → 积分按冻结金额换受益人
     */
    Response transferOrder(RobOrderTransferDTO dto);

    /**
     * 管理端分页列表（日期/场次/关键词/金额筛选）
     */
    Response getPageList(RobOrderPageQueryDTO parameter);

    /**
     * C 端我的订单分页（按 buyerId 归属，可选状态）
     */
    Response listMyOrders(Long buyerId, Integer orderStatus, Integer pageNum, Integer pageSize);

    /**
     * C 端订单详情（归属校验）
     */
    Response getDetailForUser(Long id, Long currentUserId);

    /**
     * C 端可抢商品分页（场次开启 + 商品上架；库存为 0 也展示，下单时校验库存并提示）
     */
    Response listSaleGoods(Long sessionId, Integer pageNum, Integer pageSize);
}
