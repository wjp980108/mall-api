package com.atguigu.meet.controller.app.home;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.service.goods.list.GoodsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * H5 首页接口
 * （商品推荐 / 商品搜索）
 * <p>
 * 不走 {@code @RequirePermission}（后台 RBAC），依赖 JWT 登录态；
 * 商品数据由 {@link GoodsService} 提供（基于 t_goods 商品表），本 Controller 只做首页场景的路由聚合。
 */
@RestController
@RequestMapping("/app/home")
@Validated
@Tag(name = "H5首页", description = "H5端首页商品推荐与搜索")
public class AppHomeController {

    @Autowired
    private GoodsService goodsService;

    /**
     * 首页商品推荐
     * <p>返回已上架商品（status=1），按 sales 倒序（销量优先）。
     */
    @GetMapping("/recommend")
    @Operation(summary = "首页商品推荐", description = "获取首页推荐商品（销量优先）")
    public Response recommendGoods(@RequestParam(defaultValue = "1") Integer pageNum,
                                   @RequestParam(defaultValue = "10") Integer pageSize) {
        return goodsService.recommendGoods(pageNum, pageSize);
    }

    /**
     * 首页搜索商品
     * <p>按商品名称模糊查询已上架商品（status=1），按销量优先排序。
     */
    @GetMapping("/search")
    @Operation(summary = "搜索商品", description = "按关键词搜索已上架商品")
    public Response searchGoods(@RequestParam(required = false) String keyword,
                                @RequestParam(defaultValue = "1") Integer pageNum,
                                @RequestParam(defaultValue = "10") Integer pageSize) {
        return goodsService.searchGoods(keyword, pageNum, pageSize);
    }
}