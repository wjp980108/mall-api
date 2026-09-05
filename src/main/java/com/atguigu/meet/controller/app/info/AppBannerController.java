package com.atguigu.meet.controller.app.info;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.service.info.banner.BannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * H5 轮播图接口
 * <p>
 * 不走 {@code @RequirePermission}（后台 RBAC），依赖 JWT 登录态。
 * 复用 {@link BannerService#getEnabledBannersByPosition}，按位置返回启用轮播图。
 */
@RestController
@RequestMapping("/app/banner")
@Tag(name = "H5轮播图", description = "H5端轮播图查询接口")
public class AppBannerController {

    @Autowired
    private BannerService bannerService;

    /**
     * 启用轮播图列表
     * （C 端首页展示，可选 position 过滤：home=首页 seckill=抢购）
     */
    @GetMapping("/enabled")
    @Operation(summary = "启用轮播图列表", description = "获取启用的轮播图列表")
    public Response listEnabled(@RequestParam(required = false) String position) {
        return bannerService.getEnabledBannersByPosition(position);
    }
}