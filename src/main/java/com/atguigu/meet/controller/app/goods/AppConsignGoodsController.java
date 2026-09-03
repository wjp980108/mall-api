package com.atguigu.meet.controller.app.goods;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.service.goods.consign.ConsignGoodsService;
import com.atguigu.meet.utils.AdminContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 委托商品接口
 * （抢购浏览 / 我持有的 / 商品详情 / 申请委托）
 * <p>
 * 不走 {@code @RequirePermission}（后台 RBAC），依赖 JWT 登录态；
 * Service 层校验商品持有者归属（goods.member_id == 当前用户），防越权。
 */
@RestController
@RequestMapping("/app/consign-goods")
@Validated
public class AppConsignGoodsController {

    @Autowired
    private ConsignGoodsService consignGoodsService;

    /**
     * 在售抢购商品列表
     * （抢购入口）
     * <p>过滤：上架+挂卖中+场次开启+当前在抢购时间窗口内；含委托人信息+场次名。
     */
    @GetMapping("/sale")
    public Response listSaleGoods(@RequestParam(defaultValue = "1") Integer pageNum,
                                  @RequestParam(defaultValue = "10") Integer pageSize) {
        return consignGoodsService.listSaleGoods(pageNum, pageSize);
    }

    /**
     * 商品详情
     * （C 端，复用管理端详情查询，含委托人信息+场次名）
     */
    @GetMapping("/{id}")
    public Response getDetail(@PathVariable Long id) {
        return consignGoodsService.getConsignGoodsById(id);
    }

    /**
     * 我持有的商品
     * （goodsStatus=4待处理 + memberId=当前用户，委托前置）
     */
    @GetMapping("/my-held")
    public Response listMyHeld(@RequestParam(defaultValue = "1") Integer pageNum,
                               @RequestParam(defaultValue = "10") Integer pageSize) {
        return consignGoodsService.listMyHeld(AdminContext.getLoginUserId(), pageNum, pageSize);
    }

    /**
     * 申请委托代卖
     * <p>仅商品持有者（确认收款后的买家）可申请；
     * 商品状态 4待处理 -> 5委托代卖，进入平台审核流程；
     * 审核通过后商品重新上架（1挂卖中）进入下一轮抢购。
     */
    @PostMapping("/entrust/{goodsId}")
    public Response entrust(@PathVariable Long goodsId) {
        return consignGoodsService.entrustByOwner(goodsId, AdminContext.getLoginUserId());
    }
}
