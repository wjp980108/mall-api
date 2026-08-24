package com.atguigu.meet.controller.order;

import com.atguigu.meet.annotation.RequirePermission;
import com.atguigu.meet.common.Response;
import com.atguigu.meet.constant.PermissionConst;
import com.atguigu.meet.model.dto.order.AllOrderQueryDTO;
import com.atguigu.meet.model.dto.order.OrderOperateDTO;
import com.atguigu.meet.model.dto.order.UploadVoucherDTO;
import com.atguigu.meet.service.order.OrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 订单管理（抢购商品）控制器
 * <p>
 * 后台管理菜单共 5 个：
 * - 所有订单：全量订单，支持状态筛选
 * - 待付款订单：下单后未上传支付凭证，支持【上传凭证、取消订单、删除订单】
 * - 待确认收款订单：会员已上传凭证，管理员未审核；支持【确认收款、取消订单】
 * - 代售记录：已完成委托代售的订单
 * - 已取消订单：所有取消 / 超时取消订单，仅查询不可操作
 *
 * <p>
 * 正向流程：待付款 → 已付款 → 已确认 → 已代售
 * 分支流程：待付款 / 已付款 均可取消 → 已取消
 * 待付款支持逻辑删除订单，商品回滚
 */
@RestController
@RequestMapping("/order")
@Validated
public class OrderController {

    @Autowired
    private OrderService orderService;

    // ====================== 5 个列表查询 ======================

    /**
     * 1. 所有订单分页列表（全量订单，支持状态筛选）
     */
    @GetMapping("/list/all")
    @RequirePermission(PermissionConst.ORDER_ALL_QUERY)
    public Response listAll(@Valid AllOrderQueryDTO parameter) {
        return orderService.listAll(parameter);
    }

    /**
     * 2. 待付款订单列表（status=1，内部固定，支持【上传凭证、取消订单、删除订单】）
     */
    @GetMapping("/list/waitPay")
    @RequirePermission(PermissionConst.ORDER_WAIT_PAY_QUERY)
    public Response listWaitPay(@Valid AllOrderQueryDTO parameter) {
        return orderService.listWaitPay(parameter);
    }

    /**
     * 3. 待确认收款订单列表（status=2，内部固定，支持【确认收款、取消订单】）
     */
    @GetMapping("/list/waitConfirm")
    @RequirePermission(PermissionConst.ORDER_WAIT_CONFIRM_QUERY)
    public Response listWaitConfirm(@Valid AllOrderQueryDTO parameter) {
        return orderService.listWaitConfirm(parameter);
    }

    /**
     * 4. 代售记录列表（status=4，已完成委托代售，终态）
     */
    @GetMapping("/list/agentSale")
    @RequirePermission(PermissionConst.ORDER_AGENT_SALE_QUERY)
    public Response listAgentSale(@Valid AllOrderQueryDTO parameter) {
        return orderService.listAgentSale(parameter);
    }

    /**
     * 5. 已取消订单列表（status=5，仅查询不可操作）
     */
    @GetMapping("/list/cancel")
    @RequirePermission(PermissionConst.ORDER_CANCEL_QUERY)
    public Response listCancel(@Valid AllOrderQueryDTO parameter) {
        return orderService.listCancel(parameter);
    }

    // ====================== 4 个操作接口 ======================

    /**
     * 6. 待付款订单上传支付凭证
     * <p>订单状态：1待付款 → 2已付款</p>
     */
    @PostMapping("/uploadVoucher")
    @RequirePermission(PermissionConst.ORDER_UPLOAD_VOUCHER)
    public Response uploadVoucher(@RequestBody @Valid UploadVoucherDTO dto) {
        return orderService.uploadVoucher(dto);
    }

    /**
     * 7. 取消订单
     * <p>允许场景：待付款 / 已付款 → 已取消</p>
     */
    @PostMapping("/cancel")
    @RequirePermission(PermissionConst.ORDER_CANCEL)
    public Response cancelOrder(@RequestBody @Valid OrderOperateDTO dto) {
        return orderService.cancelOrder(dto);
    }

    /**
     * 8. 删除订单（逻辑删除）
     * <p>允许场景：仅待付款订单</p>
     * <p>商品联动：重置初始状态，二次销售商品回滚至上一个售卖会员</p>
     */
    @PostMapping("/delete")
    @RequirePermission(PermissionConst.ORDER_DELETE)
    public Response deleteOrder(@RequestBody @Valid OrderOperateDTO dto) {
        return orderService.deleteOrder(dto);
    }

    /**
     * 9. 管理员确认收款（仅待确认可用）
     * <p>订单状态：2已付款 → 3已确认 → 4已代售（系统自动流转至已代售）</p>
     * <p>商品联动：托售商品状态推进至 5委托代卖，委托人变更为本次买家</p>
     */
    @PostMapping("/confirmReceive")
    @RequirePermission(PermissionConst.ORDER_CONFIRM_RECEIVE)
    public Response confirmReceive(@RequestBody @Valid OrderOperateDTO dto) {
        return orderService.confirmReceive(dto);
    }
}
