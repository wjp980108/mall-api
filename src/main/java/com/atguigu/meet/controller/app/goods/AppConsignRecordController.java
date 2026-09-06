package com.atguigu.meet.controller.app.goods;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.vo.goods.consign.ConsignRecordVO;
import com.atguigu.meet.service.goods.consign.ConsignRecordService;
import com.atguigu.meet.utils.AdminContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * H5 委托记录查询
 * <p>
 * 不走 {@code @RequirePermission}（后台 RBAC），依赖 JWT 登录态；
 * 当前用户ID 从 {@link AdminContext} 取，Service 层按 memberId/buyerId 过滤，防越权。
 * <p>
 * <b>卖方仓库流程</b>：我的委托记录展示作为委托人发起的委托履历；
 * 我的买入记录展示后台确认收款成功后生成的买入履历（recordStatus=3已卖出），
 * 含成交价、卖出时间、商品快照等信息。
 */
@RestController
@RequestMapping("/app/consign-record")
@Validated
@Tag(name = "H5委托记录", description = "H5端我的委托/买入记录查询")
public class AppConsignRecordController {

    @Autowired
    private ConsignRecordService consignRecordService;

    /**
     * 我的委托记录
     * <p>作为委托人(memberId)查询自己发起的委托履历，按申请时间(applyTime)倒序分页；
     * 展示委托代卖事件全生命周期快照：1待审核 → 2审核通过·已上架 → 3已卖出/4未售出下架/5审核驳回/6用户撤销。
     */
    @GetMapping("/my-consign")
    @Operation(summary = "我的委托记录", description = "作为委托人(memberId)查询自己发起的委托履历，按申请时间(applyTime)倒序分页；展示委托代卖事件全生命周期快照：1待审核 → 2审核通过·已上架 → 3已卖出/4未售出下架/5审核驳回/6用户撤销")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ConsignRecordVO.class)))
    public Response<ConsignRecordVO> listMyConsign(@RequestParam(defaultValue = "1") Integer pageNum,
                                  @RequestParam(defaultValue = "10") Integer pageSize) {
        return consignRecordService.listMyConsign(AdminContext.getLoginUserId(), pageNum, pageSize);
    }

    /**
     * 我的买入记录
     * <p>作为买家(buyerId)查询已成交(recordStatus=3已卖出)的委托记录，按卖出时间(soldTime)倒序分页；
     * 展示后台确认收款成功后生成的买入履历，含成交价、卖出时间、商品快照等信息。
     */
    @GetMapping("/my-bought")
    @Operation(summary = "我的买入记录", description = "作为买家(buyerId)查询已成交(recordStatus=3已卖出)的委托记录，按卖出时间(soldTime)倒序分页；展示后台确认收款成功后生成的买入履历，含成交价、卖出时间、商品快照等信息")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ConsignRecordVO.class)))
    public Response<ConsignRecordVO> listMyBought(@RequestParam(defaultValue = "1") Integer pageNum,
                                 @RequestParam(defaultValue = "10") Integer pageSize) {
        return consignRecordService.listMyBought(AdminContext.getLoginUserId(), pageNum, pageSize);
    }
}