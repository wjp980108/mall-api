package com.atguigu.meet.controller.app.seckill;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.service.goods.consign.ConsignGoodsService;
import com.atguigu.meet.service.seckill.session.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * H5 抢购接口
 * （抢购场次 / 在售抢购商品）
 * <p>
 * 不走 {@code @RequirePermission}（后台 RBAC），依赖 JWT 登录态。
 * 场次信息由 {@link SessionService} 提供，商品数据由 {@link ConsignGoodsService} 提供。
 */
@RestController
@RequestMapping("/app/session")
public class AppSessionController {

    @Autowired
    private SessionService sessionService;

    @Autowired
    private ConsignGoodsService consignGoodsService;

    /**
     * 启用场次列表
     * （C 端首页展示，按 sort 升序 + 创建时间倒序，返回完整字段含时间窗口/背景图）
     */
    @GetMapping("/enabled")
    public Response listEnabled() {
        return sessionService.getAllEnabledSessions();
    }

    /**
     * 在售抢购商品列表
     * （抢购入口 / 场次对应的商品列表）
     * <p>过滤：上架+挂卖中+场次开启+当前在抢购时间窗口内；含委托人信息+场次名。
     * <p>可选 sessionId 按场次筛选。
     */
    @GetMapping("/sale-goods")
    public Response listSaleGoods(@RequestParam(required = false) Long sessionId,
                                  @RequestParam(defaultValue = "1") Integer pageNum,
                                  @RequestParam(defaultValue = "10") Integer pageSize) {
        return consignGoodsService.listSaleGoods(pageNum, pageSize, sessionId);
    }
}
