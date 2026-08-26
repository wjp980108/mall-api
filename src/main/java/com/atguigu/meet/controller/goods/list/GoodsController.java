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
public class GoodsController {
    @Autowired
    private GoodsService goodsService;

    @Autowired
    private FileService fileService;

    /** 上传商品缩略图（内部调用通用上传接口 bizType=goodsCover，独立权限） */
    @PostMapping("/coverImg")
    @RequirePermission(PermissionConst.GOODS_COVER_IMG_UPLOAD)
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
    public Response getPageList(@Valid GoodsPageQueryDTO parameter) {
        return goodsService.getPageList(parameter);
    }

    /** 根据ID查商品 */
    @GetMapping("/{id}")
    @RequirePermission(PermissionConst.GOODS_QUERY)
    public Response getGoodsById(@PathVariable Long id) {
        return goodsService.getGoodsById(id);
    }

    /** 新增商品 */
    @PostMapping
    @RequirePermission(PermissionConst.GOODS_ADD)
    public Response addGoods(@RequestBody @Valid GoodsSaveDTO dto) {
        return goodsService.addGoods(dto);
    }

    /** 修改商品 */
    @PutMapping
    @RequirePermission(PermissionConst.GOODS_UPDATE)
    public Response updateGoods(@RequestBody @Valid GoodsUpdateDTO dto) {
        return goodsService.updateGoods(dto);
    }

    /** 商品上下架 */
    @PatchMapping("/status")
    @RequirePermission(PermissionConst.GOODS_SHELF)
    public Response updateStatus(@RequestBody @Valid GoodsStatusDTO dto) {
        return goodsService.updateStatus(dto);
    }

    /** 删除商品 */
    @DeleteMapping("/{id}")
    @RequirePermission(PermissionConst.GOODS_DELETE)
    public Response deleteGoods(@PathVariable Long id) {
        return goodsService.deleteGoods(id);
    }

    /** 批量删除商品 */
    @DeleteMapping("/batch")
    @RequirePermission(PermissionConst.GOODS_DELETE)
    public Response deleteGoodsBatch(@RequestBody @Valid GoodsDeleteDTO dto) {
        return goodsService.deleteGoodsBatch(dto);
    }
}
