package com.atguigu.meet.controller.app.roborder;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.roborder.PlaceRobOrderDTO;
import com.atguigu.meet.model.vo.roborder.RobGoodsDetailVO;
import com.atguigu.meet.model.vo.roborder.RobOrderVO;
import com.atguigu.meet.service.roborder.RobOrderService;
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
 * H5 抢购订单（基于场次商品库存 t_session_product 的新订单）
 * <p>
 * 不走 {@code @RequirePermission}，依赖 JWT 登录态，当前用户 ID 从 {@link AdminContext} 取；
 * 详情/下单等均在 Service 层做归属与资格校验。
 */
@RestController
@RequestMapping("/app/robOrder")
@Validated
@Tag(name = "H5抢购订单", description = "H5端场次商品抢购：可抢商品浏览、下单、我的订单、订单详情")
public class AppRobOrderController {

    @Autowired
    private RobOrderService robOrderService;

    /**
     * 可抢商品列表
     *
     * @param sessionId 场次ID（不传查全部场次）
     * @return 场次开启、商品上架的商品分页（库存为0也展示，售罄态按 stock 判断；抢购时下单接口校验并提示库存不足）
     */
    @GetMapping("/sale-goods")
    @Operation(summary = "可抢商品列表", description = "查询可抢购的场次商品：场次开启、商品上架即展示（库存为0也展示，售罄商品下单时提示库存不足），支持按场次筛选")
    public Response listSaleGoods(@RequestParam(required = false) Long sessionId,
                                  @RequestParam(defaultValue = "1") Integer pageNum,
                                  @RequestParam(defaultValue = "10") Integer pageSize) {
        return robOrderService.listSaleGoods(sessionId, pageNum, pageSize);
    }

    /**
     * 抢购商品详情
     *
     * @param id 场次商品关联ID（t_session_product.id，即可抢商品列表返回的 id，也是下单入参 sessionProductId）
     * @return 商品详细信息（封面/详情图/富文本/销量等）+ 抢购下单信息（下单锚点、抢购库存、场次抢购时间窗口、
     * 当前是否可抢(含新会员提前进场)、限购规则及当前用户是否已命中限购）
     */
    @GetMapping("/sale-goods/{id}")
    @Operation(summary = "抢购商品详情", description = "按场次商品关联ID查询抢购商品详情：商品详细信息(封面图/详情图/富文本/销量) + 下单所需信息(下单锚点sessionProductId、抢购库存、场次抢购时间窗口、当前是否可抢(新会员含提前进场)、限购规则与当前用户是否已命中)。状态口径与抢购下单校验一致")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RobGoodsDetailVO.class)))
    public Response getSaleGoodsDetail(@PathVariable Long id) {
        return robOrderService.getSaleGoodsDetail(id, AdminContext.getLoginUserId());
    }

    /**
     * 我的抢购订单
     *
     * @param orderStatus 订单状态：1正常 2已取消（不传查全部）
     * @return 我的订单分页
     */
    @GetMapping("/my-list")
    @Operation(summary = "我的抢购订单", description = "按当前登录用户分页查询抢购订单，可按状态(1正常/2已取消)筛选")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RobOrderVO.class)))
    public Response listMyOrders(@RequestParam(required = false) Integer orderStatus,
                                 @RequestParam(defaultValue = "1") Integer pageNum,
                                 @RequestParam(defaultValue = "10") Integer pageSize) {
        return robOrderService.listMyOrders(AdminContext.getLoginUserId(), orderStatus, pageNum, pageSize);
    }

    /**
     * 抢购订单详情
     *
     * @param id 订单ID
     * @return 订单详情（校验归属当前用户）
     */
    @GetMapping("/{id}")
    @Operation(summary = "抢购订单详情", description = "查询抢购订单详情，校验订单归属当前登录用户，防越权查看")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RobOrderVO.class)))
    public Response getDetail(@PathVariable Long id) {
        return robOrderService.getDetailForUser(id, AdminContext.getLoginUserId());
    }

    /**
     * 抢购下单
     *
     * @param dto 场次商品关联ID + 购买数量
     * @return 下单结果
     */
    @PostMapping("/place")
    @Operation(summary = "抢购下单", description = "按场次商品关联下单：校验场次开启/抢购时间窗口(含新会员提前)/限购，原子扣减库存，创建订单并按系统设置比例快照金额、发放推荐奖/自购奖金/购物券积分")
    public Response placeOrder(@RequestBody @Valid PlaceRobOrderDTO dto) {
        return robOrderService.placeOrder(dto, AdminContext.getLoginUserId());
    }
}
