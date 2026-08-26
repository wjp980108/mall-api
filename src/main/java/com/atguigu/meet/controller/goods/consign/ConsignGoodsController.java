package com.atguigu.meet.controller.goods.consign;

import com.atguigu.meet.annotation.RequirePermission;
import com.atguigu.meet.common.Response;
import com.atguigu.meet.constant.PermissionConst;
import com.atguigu.meet.model.dto.goods.consign.ConsignGoodsBizStatusDTO;
import com.atguigu.meet.model.dto.goods.consign.ConsignGoodsDeleteDTO;
import com.atguigu.meet.model.dto.goods.consign.ConsignGoodsOnlineStatusDTO;
import com.atguigu.meet.model.dto.goods.consign.ConsignGoodsPageQueryDTO;
import com.atguigu.meet.model.dto.goods.consign.ConsignGoodsSaveDTO;
import com.atguigu.meet.model.dto.goods.consign.ConsignGoodsUpdateDTO;
import com.atguigu.meet.service.file.FileService;
import com.atguigu.meet.service.goods.consign.ConsignGoodsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 抢购商品订单管理
 * <p>
 * 业务状态：1挂卖中 2已抢购待付款 3等待确认付款 4待处理 5委托代卖 6委托发货
 */
@RestController
@RequestMapping("/consign-goods")
@Validated
public class ConsignGoodsController {
    @Autowired
    private ConsignGoodsService consignGoodsService;

    @Autowired
    private FileService fileService;

    /** 上传商品缩略图（内部调用通用上传接口 bizType=consignCover，独立权限） */
    @PostMapping("/coverImg")
    @RequirePermission(PermissionConst.CONSIGN_GOODS_COVER_IMG_UPLOAD)
    public Response uploadCoverImg(@RequestParam("file") MultipartFile file,
                                   @RequestParam(value = "platform", required = false) String platform) {
        try {
            return fileService.upload(file, "consignCover", platform);
        } catch (RuntimeException e) {
            return Response.fail(500, e.getMessage());
        }
    }

    /** 上传商品详情图（内部调用通用上传接口 bizType=consignDetail，独立权限） */
    @PostMapping("/detailImg")
    @RequirePermission(PermissionConst.CONSIGN_GOODS_DETAIL_IMG_UPLOAD)
    public Response uploadDetailImg(@RequestParam("file") MultipartFile file,
                                    @RequestParam(value = "platform", required = false) String platform) {
        try {
            return fileService.upload(file, "consignDetail", platform);
        } catch (RuntimeException e) {
            return Response.fail(500, e.getMessage());
        }
    }

    /** 分页列表 */
    @GetMapping
    @RequirePermission(PermissionConst.CONSIGN_GOODS_QUERY)
    public Response getPageList(@Valid ConsignGoodsPageQueryDTO parameter) {
        return consignGoodsService.getPageList(parameter);
    }

    /** 根据ID查详情（含委托人信息 + 场次名称） */
    @GetMapping("/{id}")
    @RequirePermission(PermissionConst.CONSIGN_GOODS_QUERY)
    public Response getConsignGoodsById(@PathVariable Long id) {
        return consignGoodsService.getConsignGoodsById(id);
    }

    /** 新增 */
    @PostMapping
    @RequirePermission(PermissionConst.CONSIGN_GOODS_ADD)
    public Response addConsignGoods(@RequestBody @Valid ConsignGoodsSaveDTO dto) {
        return consignGoodsService.addConsignGoods(dto);
    }

    /** 修改 */
    @PutMapping
    @RequirePermission(PermissionConst.CONSIGN_GOODS_UPDATE)
    public Response updateConsignGoods(@RequestBody @Valid ConsignGoodsUpdateDTO dto) {
        return consignGoodsService.updateConsignGoods(dto);
    }

    /** 上下架 */
    @PatchMapping("/online-status")
    @RequirePermission(PermissionConst.CONSIGN_GOODS_SHELF)
    public Response updateOnlineStatus(@RequestBody @Valid ConsignGoodsOnlineStatusDTO dto) {
        return consignGoodsService.updateOnlineStatus(dto);
    }

    /** 业务状态流转 */
    @PatchMapping("/biz-status")
    @RequirePermission(PermissionConst.CONSIGN_GOODS_BIZ_STATUS)
    public Response updateBizStatus(@RequestBody @Valid ConsignGoodsBizStatusDTO dto) {
        return consignGoodsService.updateBizStatus(dto);
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    @RequirePermission(PermissionConst.CONSIGN_GOODS_DELETE)
    public Response deleteConsignGoods(@PathVariable Long id) {
        return consignGoodsService.deleteConsignGoods(id);
    }

    /** 批量删除 */
    @DeleteMapping("/batch")
    @RequirePermission(PermissionConst.CONSIGN_GOODS_DELETE)
    public Response deleteConsignGoodsBatch(@RequestBody @Valid ConsignGoodsDeleteDTO dto) {
        return consignGoodsService.deleteConsignGoodsBatch(dto);
    }
}
