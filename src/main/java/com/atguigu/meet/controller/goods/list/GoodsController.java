package com.atguigu.meet.controller.goods.list;

import com.atguigu.meet.annotation.RequirePermission;
import com.atguigu.meet.common.Response;
import com.atguigu.meet.constant.PermissionConst;
import com.atguigu.meet.model.dto.goods.list.GoodsDeleteDTO;
import com.atguigu.meet.model.dto.goods.list.GoodsPageQueryDTO;
import com.atguigu.meet.model.dto.goods.list.GoodsSaveDTO;
import com.atguigu.meet.model.dto.goods.list.GoodsStatusDTO;
import com.atguigu.meet.model.dto.goods.list.GoodsUpdateDTO;
import com.atguigu.meet.service.file.FileService;
import com.atguigu.meet.service.goods.list.GoodsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 商品列表
 */
@RestController
@RequestMapping("/goods")
@Validated
@Tag(name = "商品管理", description = "商品CRUD及上下架管理")
public class GoodsController {
    @Autowired
    private GoodsService goodsService;

    @Autowired
    private FileService fileService;

    /** 上传商品缩略图（内部调用通用上传接口 bizType=goodsCover，独立权限） */
    @PostMapping("/coverImg")
    @RequirePermission(PermissionConst.GOODS_COVER_IMG_UPLOAD)
    @Operation(summary = "上传商品缩略图", description = "上传商品缩略图")
    public Response uploadCoverImg(@RequestParam("file") MultipartFile file,
                                   @RequestParam(value = "platform", required = false) String platform) {
        try {
            return fileService.upload(file, "goodsCover", platform);
        } catch (RuntimeException e) {
            return Response.fail(500, e.getMessage());
        }
    }

    /** 上传商品详情图（内部调用通用上传接口 bizType=goodsDetail，独立权限） */
    @PostMapping("/detailImg")
    @RequirePermission(PermissionConst.GOODS_DETAIL_IMG_UPLOAD)
    @Operation(summary = "上传商品详情图", description = "上传商品详情图")
    public Response uploadDetailImg(@RequestParam("file") MultipartFile file,
                                    @RequestParam(value = "platform", required = false) String platform) {
        try {
            return fileService.upload(file, "goodsDetail", platform);
        } catch (RuntimeException e) {
            return Response.fail(500, e.getMessage());
        }
    }

    /** 商品分页列表 */
    @GetMapping
    @RequirePermission(PermissionConst.GOODS_QUERY)
    @Operation(summary = "商品分页列表", description = "分页查询商品列表")
    public Response getPageList(@Valid GoodsPageQueryDTO parameter) {
        return goodsService.getPageList(parameter);
    }

    /** 根据ID查商品 */
    @GetMapping("/{id}")
    @RequirePermission(PermissionConst.GOODS_QUERY)
    @Operation(summary = "商品详情", description = "根据ID查询商品详情")
    public Response getGoodsById(@PathVariable Long id) {
        return goodsService.getGoodsById(id);
    }

    /** 新增商品 */
    @PostMapping
    @RequirePermission(PermissionConst.GOODS_ADD)
    @Operation(summary = "新增商品", description = "创建新商品")
    public Response addGoods(@RequestBody @Valid GoodsSaveDTO dto) {
        return goodsService.addGoods(dto);
    }

    /** 修改商品 */
    @PutMapping
    @RequirePermission(PermissionConst.GOODS_UPDATE)
    @Operation(summary = "修改商品", description = "更新商品信息")
    public Response updateGoods(@RequestBody @Valid GoodsUpdateDTO dto) {
        return goodsService.updateGoods(dto);
    }

    /** 商品上下架 */
    @PatchMapping("/status")
    @RequirePermission(PermissionConst.GOODS_SHELF)
    @Operation(summary = "商品上下架", description = "更新商品上下架状态")
    public Response updateStatus(@RequestBody @Valid GoodsStatusDTO dto) {
        return goodsService.updateStatus(dto);
    }

    /** 删除商品 */
    @DeleteMapping("/{id}")
    @RequirePermission(PermissionConst.GOODS_DELETE)
    @Operation(summary = "删除商品", description = "删除商品")
    public Response deleteGoods(@PathVariable Long id) {
        return goodsService.deleteGoods(id);
    }

    /** 批量删除商品 */
    @DeleteMapping("/batch")
    @RequirePermission(PermissionConst.GOODS_DELETE)
    @Operation(summary = "批量删除商品", description = "批量删除商品")
    public Response deleteGoodsBatch(@RequestBody @Valid GoodsDeleteDTO dto) {
        return goodsService.deleteGoodsBatch(dto);
    }
}