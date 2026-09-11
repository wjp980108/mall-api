package com.atguigu.meet.controller.roborder;

import com.atguigu.meet.annotation.RequirePermission;
import com.atguigu.meet.common.Response;
import com.atguigu.meet.constant.PermissionConst;
import com.atguigu.meet.model.dto.roborder.RobOrderCancelDTO;
import com.atguigu.meet.model.dto.roborder.RobOrderPageQueryDTO;
import com.atguigu.meet.model.dto.roborder.RobOrderTransferDTO;
import com.atguigu.meet.model.vo.roborder.PointsInsufficientVO;
import com.atguigu.meet.model.vo.roborder.RobOrderVO;
import com.atguigu.meet.service.roborder.RobOrderService;
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
 * 抢购订单管理
 */
@RestController
@RequestMapping("/robOrders")
@Validated
@Tag(name = "抢购订单管理", description = "场次商品抢购订单的单列表查询、转移订单、取消订单（回滚库存与积分，积分不足需二次确认）")
public class RobOrderController {

    @Autowired
    private RobOrderService robOrderService;

    /**
     * 分页列表
     *
     * @param parameter 分页 + 日期范围/场次/关键词(姓名手机ID商品名)/金额/状态
     * @return 抢购订单分页
     */
    @GetMapping
    @Operation(summary = "抢购订单分页列表", description = "单列表分页查询，支持日期范围、所属场次、关键词(姓名/手机号/用户ID/商品名)、金额模糊、状态筛选")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RobOrderVO.class)))
    public Response getPageList(@Valid RobOrderPageQueryDTO parameter) {
        return robOrderService.getPageList(parameter);
    }

    /**
     * 转移订单
     *
     * @param dto 订单ID + 新买家用户ID + 确认积分不足仍继续
     * @return 操作结果（data 非空表示积分不足待二次确认）
     */
    @PutMapping("/transfer")
    @RequirePermission(PermissionConst.ROB_ORDER_TRANSFER)
    @Operation(summary = "转移订单", description = "把订单买家更换为选择的用户，买家快照与推荐奖/自购奖等积分权益一并划转（金额冻结不变）。原受益人积分余额不足时返回积分不足提示（data 非空），携带 confirmInsufficient=true 确认后继续执行（允许负余额）")
    public Response<Void> transferOrder(@RequestBody @Valid RobOrderTransferDTO dto) {
        return robOrderService.transferOrder(dto);
    }

    /**
     * 取消订单
     *
     * @param dto 订单ID + 确认积分不足仍继续
     * @return 操作结果（data 非空表示积分不足待二次确认）
     */
    @PutMapping("/cancel")
    @RequirePermission(PermissionConst.ROB_ORDER_CANCEL)
    @Operation(summary = "取消订单", description = "取消正常订单：场次商品库存回滚、推荐奖/自购奖/购物券积分全额冲回（幂等）。原受益人积分余额不足时返回积分不足提示（data 非空），携带 confirmInsufficient=true 确认后继续执行（允许负余额）")
    @ApiResponse(responseCode = "200", description = "成功：data=null；积分不足待二次确认：data=PointsInsufficientVO（pointsInsufficient/couponInsufficient 标志）", content = @Content(mediaType = "application/json", schema = @Schema(oneOf = {PointsInsufficientVO.class})))
    public Response<Void> cancelOrder(@RequestBody @Valid RobOrderCancelDTO dto) {
        return robOrderService.cancelOrder(dto);
    }
}
