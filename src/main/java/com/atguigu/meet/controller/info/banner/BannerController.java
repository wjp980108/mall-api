package com.atguigu.meet.controller.info.banner;

import com.atguigu.meet.annotation.RequirePermission;
import com.atguigu.meet.common.Response;
import com.atguigu.meet.constant.PermissionConst;
import com.atguigu.meet.model.dto.info.banner.BannerPageQueryDTO;
import com.atguigu.meet.model.dto.info.banner.BannerSaveDTO;
import com.atguigu.meet.model.dto.info.banner.BannerUpdateDTO;
import com.atguigu.meet.service.info.banner.BannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 轮播图管理接口
 */
@RestController
@RequestMapping("/banners")
@Validated
@Tag(name = "轮播图管理", description = "后台轮播图管理接口")
public class BannerController {
    @Autowired
    private BannerService bannerService;

    /** 轮播图分页列表 */
    @GetMapping
    @RequirePermission(PermissionConst.BANNER_QUERY)
    @Operation(summary = "轮播图分页列表", description = "分页查询轮播图列表")
    public Response getPageList(@Valid BannerPageQueryDTO parameter) {
        return bannerService.getPageList(parameter);
    }

    /** 按位置获取启用轮播图（C端展示用） */
    @GetMapping("/enabled")
    @Operation(summary = "启用轮播图", description = "按位置获取启用的轮播图（C端展示用）")
    public Response getEnabledBanners(@RequestParam(required = false) String position) {
        return bannerService.getEnabledBannersByPosition(position);
    }

    /** 根据ID查轮播图 */
    @GetMapping("/{id}")
    @RequirePermission(PermissionConst.BANNER_QUERY)
    @Operation(summary = "轮播图详情", description = "根据ID查询轮播图详情")
    public Response getBannerById(@PathVariable Long id) {
        return bannerService.getBannerById(id);
    }

    /** 新增轮播图 */
    @PostMapping
    @RequirePermission(PermissionConst.BANNER_ADD)
    @Operation(summary = "新增轮播图", description = "创建新轮播图")
    public Response addBanner(@RequestBody @Valid BannerSaveDTO dto) {
        return bannerService.addBanner(dto);
    }

    /** 修改轮播图 */
    @PutMapping
    @RequirePermission(PermissionConst.BANNER_UPDATE)
    @Operation(summary = "修改轮播图", description = "更新轮播图信息")
    public Response updateBanner(@RequestBody @Valid BannerUpdateDTO dto) {
        return bannerService.updateBanner(dto);
    }

    /** 删除轮播图（逻辑删除） */
    @DeleteMapping("/{id}")
    @RequirePermission(PermissionConst.BANNER_DELETE)
    @Operation(summary = "删除轮播图", description = "逻辑删除轮播图")
    public Response deleteBanner(@PathVariable Long id) {
        return bannerService.deleteBanner(id);
    }
}