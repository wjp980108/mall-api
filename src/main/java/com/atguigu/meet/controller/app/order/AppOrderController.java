package com.atguigu.meet.controller.app.order;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.order.OrderOperateDTO;
import com.atguigu.meet.model.dto.order.PlaceOrderDTO;
import com.atguigu.meet.model.dto.order.UploadVoucherDTO;
import com.atguigu.meet.model.vo.order.OrderVO;
import com.atguigu.meet.service.order.OrderService;
import com.atguigu.meet.utils.AdminContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * H5 订单操作
 * <p>
 * 不走 {@code @RequirePermission}（后台 RBAC），依赖 {@link com.atguigu.meet.filter.JwtAuthenticationFilter}
 * 的 token 校验：C 端用户携带 JWT 通过即视为已登录，当前用户 ID 从 {@link AdminContext} 取。
 * <p>
 * 所有订单操作均经 Service 层 buyerId 归属校验，防越权操作他人订单。
 * <p>
 * <b>买方仓库流程</b>：我的订单列表展示已抢购的订单，订单右下角显示「去付款」按钮，
 * 点击进入付款页面，上传图片凭证后才可以点击确定付款。
 * 订单状态流转：1待付款 → 上传凭证 → 2已付款 → 后台确认收款 → 4已完成。
 */
@RestController
@RequestMapping("/app/order")
@Validated
@Tag(name = "H5订单", description = "H5端订单操作与查询")
public class AppOrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 我的买方仓库
     * <p>展示已抢购的订单列表（按 buyerId 分页），可选 orderStatus 筛选；
     * 订单右下角显示「去付款」按钮，点击进入付款页面，上传图片凭证后才可以点击确定付款。
     * <p>订单状态流转：1待付款 → 上传凭证 → 2已付款 → 后台确认收款 → 4已完成
     */
    @GetMapping("/my-list")
    @Operation(summary = "我的订单列表", description = "我的买方仓库：展示已抢购的订单列表，按 buyerId 分页，可选 orderStatus 筛选；订单右下角显示「去付款」按钮，点击后进入付款页面，上传图片凭证后才可以点击确定付款。订单状态流转：1待付款 → 上传凭证 → 2已付款 → 后台确认收款 → 4已完成")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderVO.class)))
    public Response<OrderVO> listMyOrders(@RequestParam(required = false) Integer orderStatus,
                                 @RequestParam(defaultValue = "1") Integer pageNum,
                                 @RequestParam(defaultValue = "10") Integer pageSize) {
        return orderService.listMyOrders(AdminContext.getLoginUserId(), orderStatus, pageNum, pageSize);
    }

    /**
     * 订单详情
     * <p>校验订单归属当前买家（buyerId == 当前登录用户），防越权查看他人订单。
     */
    @GetMapping("/{id}")
    @Operation(summary = "订单详情", description = "查询订单详情，校验订单归属当前买家（buyerId == 当前登录用户），防越权查看他人订单")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderVO.class)))
    public Response<OrderVO> getDetail(@PathVariable Long id) {
        return orderService.getOrderDetailForUser(id, AdminContext.getLoginUserId());
    }

    /**
     * 抢购下单
     * <p>校验抢购时间窗口 + 限购 + 商品状态条件更新（1挂卖中 → 2已抢购待付款）+ 建单 + pay_deadline = now + 30min；
     * 并发保障：商品状态条件更新与建单同处一个事务，affected=0 即判定并发冲突（商品已被他人抢走）。
     */
    @PostMapping("/place")
    @Operation(summary = "抢购下单", description = "创建抢购订单：校验抢购时间窗口 + 限购 + 商品状态条件更新（1挂卖中 → 2已抢购待付款）+ 建单 + pay_deadline = now + 30min；并发保障：商品状态条件更新与建单同处一个事务，affected=0 即判定并发冲突（商品已被他人抢走）")
    public Response<Void> placeOrder(@RequestBody @Valid PlaceOrderDTO dto) {
        return orderService.placeOrder(dto, AdminContext.getLoginUserId());
    }

    /**
     * 用户取消订单
     * <p>订单状态 待付款(1)/已付款(2) → 5已取消；商品状态回滚至 1挂卖中；
     * 归属校验：仅 buyerId == 当前登录用户的订单可取消，防越权操作他人订单。
     */
    @PostMapping("/cancel")
    @Operation(summary = "取消订单", description = "用户取消订单：订单状态 待付款(1)/已付款(2) → 5已取消，商品状态回滚至 1挂卖中；归属校验：仅 buyerId == 当前登录用户的订单可取消，防越权操作他人订单")
    public Response<Void> cancelOrder(@RequestBody @Valid OrderOperateDTO dto) {
        return orderService.cancelOrderByUser(dto, AdminContext.getLoginUserId());
    }

    /**
     * 用户上传支付凭证
     * <p>订单状态 1待付款 → 2已付款；商品状态 2已抢购待付款 → 3等待确认付款；
     * 校验：仅待付款订单可上传，且不超过 pay_deadline；归属校验：仅 buyerId == 当前登录用户的订单可上传。
     */
    @PostMapping("/uploadVoucher")
    @Operation(summary = "上传支付凭证", description = "用户上传订单支付凭证：订单状态 1待付款 → 2已付款，商品状态 2已抢购待付款 → 3等待确认付款；校验：仅待付款订单可上传且不超过 pay_deadline；归属校验：仅 buyerId == 当前登录用户的订单可上传")
    public Response<Void> uploadVoucher(@RequestBody @Valid UploadVoucherDTO dto) {
        return orderService.uploadVoucherByUser(dto, AdminContext.getLoginUserId());
    }
}