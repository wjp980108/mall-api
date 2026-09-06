package com.atguigu.meet.controller.app.goods;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.vo.goods.consign.ConsignGoodsVO;
import com.atguigu.meet.service.goods.consign.ConsignGoodsService;
import com.atguigu.meet.utils.AdminContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * H5 委托商品接口
 * <p>
 * 不走 {@code @RequirePermission}（后台 RBAC），依赖 JWT 登录态；
 * Service 层校验商品持有者归属（goods.member_id == 当前用户），防越权。
 * <p>
 * <b>卖方仓库流程</b>：我持有的商品展示确认收款后交由买家处理的商品（goodsStatus=4待处理），
 * 买家可在此发起委托代卖申请；申请后进入平台审核流程，审核通过后商品重新上架（1挂卖中）进入下一轮抢购。
 * <p>
 * 首页推荐/搜索见 {@code AppHomeController}（/app/home），抢购在售商品列表见
 * {@code AppSessionController}（/app/session/sale-goods）。
 */
@RestController
@RequestMapping("/app/consign-goods")
@Validated
@Tag(name = "H5委托商品", description = "H5端委托商品查询与申请")
public class AppConsignGoodsController {

    @Autowired
    private ConsignGoodsService consignGoodsService;

    /**
     * 商品详情
     * <p>C 端查询委托商品详情，复用管理端详情查询，含委托人信息 + 场次名称。
     */
    @GetMapping("/{id}")
    @Operation(summary = "商品详情", description = "C 端查询委托商品详情，复用管理端详情查询，含委托人信息 + 场次名称")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ConsignGoodsVO.class)))
    public Response<ConsignGoodsVO> getDetail(@PathVariable Long id) {
        return consignGoodsService.getConsignGoodsById(id);
    }

    /**
     * 我的卖方仓库
     * <p>查询 memberId=当前登录用户 的持仓商品，按状态筛选分页（委托前置）；
     * <ul>
     *   <li>goodsStatus 不传：全部持有中（1挂卖中 + 4待处理 + 5委托审核中）</li>
     *   <li>goodsStatus=4：待处理，可发起委托代卖申请</li>
     *   <li>goodsStatus=5：委托审核中，可撤销申请</li>
     *   <li>goodsStatus=1 + onlineStatus=1：委托审核通过，在售中（下一轮可被抢购）</li>
     *   <li>goodsStatus=1 + onlineStatus=0：已下架（当日未委托/未卖出被系统下架，终态）</li>
     * </ul>
     * 已卖出的商品不在本列表（member_id 已转买家），成交履历见 /app/consign-record/my-consign。
     */
    @GetMapping("/my-held")
    @Operation(summary = "我的卖方仓库", description = "查询 memberId=当前登录用户 的持仓商品分页，按状态筛选：不传 goodsStatus 查全部持有中(1/4/5)；4待处理(可申请委托)；5委托审核中(可撤销)；1挂卖中配合 onlineStatus 区分在售(1)/已下架(0)。已卖出的商品 member_id 已转买家，不在本列表，成交履历见 /app/consign-record/my-consign")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ConsignGoodsVO.class)))
    public Response<ConsignGoodsVO> listMyHeld(@RequestParam(required = false) Integer goodsStatus,
                               @RequestParam(required = false) Integer onlineStatus,
                               @RequestParam(defaultValue = "1") Integer pageNum,
                               @RequestParam(defaultValue = "10") Integer pageSize) {
        return consignGoodsService.listMyHeld(AdminContext.getLoginUserId(), goodsStatus, onlineStatus, pageNum, pageSize);
    }

    /**
     * 申请委托代卖
     * <p>仅商品持有者（确认收款后的买家）可申请；
     * 商品状态 4待处理 → 5委托代卖，进入平台审核流程；
     * 审核通过后商品重新上架（1挂卖中）进入下一轮抢购；
     * 校验：仅 goods.memberId == 当前登录用户可申请，防越权。
     */
    @PostMapping("/entrust/{goodsId}")
    @Operation(summary = "申请委托代卖", description = "申请委托代卖商品：仅商品持有者（确认收款后的买家）可申请；商品状态 4待处理 → 5委托代卖，进入平台审核流程；审核通过后商品重新上架（1挂卖中）进入下一轮抢购；校验：仅 goods.memberId == 当前登录用户可申请，防越权")
    public Response<Void> entrust(@PathVariable Long goodsId) {
        return consignGoodsService.entrustByOwner(goodsId, AdminContext.getLoginUserId());
    }

    /**
     * 撤销委托申请
     * <p>仅委托审核中（goodsStatus=5 + auditStatus=1）且持有者本人可撤销；
     * 商品状态 5委托代卖 → 4待处理（entrust=0，audit=0），委托记录置为 6用户撤销；
     * 撤销后当天可重新申请，当天未再委托则 23:59 定时任务照常下架。
     */
    @PostMapping("/entrust/cancel/{goodsId}")
    @Operation(summary = "撤销委托申请", description = "撤销委托代卖申请：仅委托审核中(5+待审核)且持有者本人可撤销；商品状态 5委托代卖 → 4待处理(entrust=0,audit=0)，委托记录置为 6用户撤销；撤销后当天可重新申请")
    public Response<Void> cancelEntrust(@PathVariable Long goodsId) {
        return consignGoodsService.cancelEntrustByOwner(goodsId, AdminContext.getLoginUserId());
    }
}