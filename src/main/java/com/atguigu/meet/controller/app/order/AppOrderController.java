package com.atguigu.meet.controller.app.order;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.order.OrderOperateDTO;
import com.atguigu.meet.model.dto.order.PlaceOrderDTO;
import com.atguigu.meet.model.dto.order.UploadVoucherDTO;
import com.atguigu.meet.service.order.OrderService;
import com.atguigu.meet.utils.AdminContext;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 订单操作
 * （抢购下单/取消/上传凭证 + 我的订单查询/详情）
 * <p>
 * 不走 {@code @RequirePermission}（后台 RBAC），依赖 {@link com.atguigu.meet.filter.JwtAuthenticationFilter}
 * 的 token 校验：C 端用户携带 JWT 通过即视为已登录，当前用户 ID 从 {@link AdminContext} 取。
 * <p>
 * 所有订单操作均经 Service 层 buyerId 归属校验，防越权操作他人订单。
 */
@RestController
@RequestMapping("/app/order")
@Validated
public class AppOrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 我的买方仓库
     * （按 buyerId 分页，可选 orderStatus 筛选）
     */
    @GetMapping("/my-list")
    public Response listMyOrders(@RequestParam(required = false) Integer orderStatus,
                                 @RequestParam(defaultValue = "1") Integer pageNum,
                                 @RequestParam(defaultValue = "10") Integer pageSize) {
        return orderService.listMyOrders(AdminContext.getLoginUserId(), orderStatus, pageNum, pageSize);
    }

    /**
     * 订单详情
     * （校验订单归属当前买家）
     */
    @GetMapping("/{id}")
    public Response getDetail(@PathVariable Long id) {
        return orderService.getOrderDetailForUser(id, AdminContext.getLoginUserId());
    }

    /**
     * 抢购下单
     * <p>商品状态 1挂卖中 -> 2已抢购待付款；订单状态 -> 1待付款；pay_deadline = now + 30min
     */
    @PostMapping("/place")
    public Response placeOrder(@RequestBody @Valid PlaceOrderDTO dto) {
        return orderService.placeOrder(dto, AdminContext.getLoginUserId());
    }

    /**
     * 用户取消订单
     * <p>订单状态 待付款/已付款 -> 5已取消；商品状态回滚至 1挂卖中
     */
    @PostMapping("/cancel")
    public Response cancelOrder(@RequestBody @Valid OrderOperateDTO dto) {
        return orderService.cancelOrderByUser(dto, AdminContext.getLoginUserId());
    }

    /**
     * 用户上传支付凭证
     * <p>订单状态 1待付款 -> 2已付款；商品状态 2已抢购待付款 -> 3等待确认付款
     */
    @PostMapping("/uploadVoucher")
    public Response uploadVoucher(@RequestBody @Valid UploadVoucherDTO dto) {
        return orderService.uploadVoucherByUser(dto, AdminContext.getLoginUserId());
    }
}
