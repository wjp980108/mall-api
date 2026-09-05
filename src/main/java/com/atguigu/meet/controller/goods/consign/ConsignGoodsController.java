package com.atguigu.meet.controller.goods.consign;

import com.atguigu.meet.annotation.RequirePermission;
import com.atguigu.meet.common.Response;
import com.atguigu.meet.constant.PermissionConst;
import com.atguigu.meet.model.dto.goods.consign.ConsignGoodsAuditDTO;
import com.atguigu.meet.model.dto.goods.consign.ConsignGoodsBizStatusDTO;
import com.atguigu.meet.model.dto.goods.consign.ConsignGoodsDeleteDTO;
import com.atguigu.meet.model.dto.goods.consign.ConsignGoodsOnlineStatusDTO;
import com.atguigu.meet.model.dto.goods.consign.ConsignGoodsPageQueryDTO;
import com.atguigu.meet.model.dto.goods.consign.ConsignGoodsSaveDTO;
import com.atguigu.meet.model.dto.goods.consign.ConsignGoodsUpdateDTO;
import com.atguigu.meet.service.file.FileService;
import com.atguigu.meet.service.goods.consign.ConsignGoodsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 抢购商品订单管理
 * <p>
 * 业务状态：1挂卖中 2已抢购待付款 3等待确认付款 4待处理 5委托代卖
 */
@RestController
@RequestMapping("/consign-goods")
@Validated
@Tag(name = "托售商品管理", description = "托售商品CRUD、状态流转及审核管理")
public class ConsignGoodsController {
    @Autowired
    private ConsignGoodsService consignGoodsService;

    @Autowired
    private FileService fileService;

    /** 上传商品缩略图（内部调用通用上传接口 bizType=consignCover，独立权限） */
    @PostMapping("/coverImg")
    @RequirePermission(PermissionConst.CONSIGN_GOODS_COVER_IMG_UPLOAD)
    @Operation(summary = "上传商品缩略图", description = "上传托售商品缩略图")
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
    @Operation(summary = "上传商品详情图", description = "上传托售商品详情图")
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
    @Operation(summary = "托售商品分页列表", description = "分页查询托售商品列表")
    public Response getPageList(@Valid ConsignGoodsPageQueryDTO parameter) {
        return consignGoodsService.getPageList(parameter);
    }

    /** 根据ID查详情（含委托人信息 + 场次名称） */
    @GetMapping("/{id}")
    @RequirePermission(PermissionConst.CONSIGN_GOODS_QUERY)
    @Operation(summary = "托售商品详情", description = "根据ID查询托售商品详情（含委托人信息+场次名称）")
    public Response getConsignGoodsById(@PathVariable Long id) {
        return consignGoodsService.getConsignGoodsById(id);
    }

    /** 新增 */
    @PostMapping
    @RequirePermission(PermissionConst.CONSIGN_GOODS_ADD)
    @Operation(summary = "新增托售商品", description = "创建托售商品")
    public Response addConsignGoods(@RequestBody @Valid ConsignGoodsSaveDTO dto) {
        return consignGoodsService.addConsignGoods(dto);
    }

    /** 修改 */
    @PutMapping
    @RequirePermission(PermissionConst.CONSIGN_GOODS_UPDATE)
    @Operation(summary = "修改托售商品", description = "更新托售商品信息")
    public Response updateConsignGoods(@RequestBody @Valid ConsignGoodsUpdateDTO dto) {
        return consignGoodsService.updateConsignGoods(dto);
    }

    /** 上下架 */
    @PatchMapping("/online-status")
    @RequirePermission(PermissionConst.CONSIGN_GOODS_SHELF)
    @Operation(summary = "更新上下架状态", description = "更新托售商品上下架状态")
    public Response updateOnlineStatus(@RequestBody @Valid ConsignGoodsOnlineStatusDTO dto) {
        return consignGoodsService.updateOnlineStatus(dto);
    }

    /** 业务状态流转 */
    @PatchMapping("/biz-status")
    @RequirePermission(PermissionConst.CONSIGN_GOODS_BIZ_STATUS)
    @Operation(summary = "更新业务状态", description = "托售商品业务状态流转")
    public Response updateBizStatus(@RequestBody @Valid ConsignGoodsBizStatusDTO dto) {
        return consignGoodsService.updateBizStatus(dto);
    }

    /** 委托代卖审核（通过：商品重新上架进入下一轮售卖；驳回：退回待处理） */
    @PatchMapping("/entrust-audit")
    @RequirePermission(PermissionConst.CONSIGN_GOODS_ENTRUST_AUDIT)
    @Operation(summary = "委托代卖审核", description = "审核委托代卖申请（通过/驳回）")
    public Response auditEntrust(@RequestBody @Valid ConsignGoodsAuditDTO dto) {
        return consignGoodsService.auditEntrust(dto);
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    @RequirePermission(PermissionConst.CONSIGN_GOODS_DELETE)
    @Operation(summary = "删除托售商品", description = "删除托售商品")
    public Response deleteConsignGoods(@PathVariable Long id) {
        return consignGoodsService.deleteConsignGoods(id);
    }

    /** 批量删除 */
    @DeleteMapping("/batch")
    @RequirePermission(PermissionConst.CONSIGN_GOODS_DELETE)
    @Operation(summary = "批量删除托售商品", description = "批量删除托售商品")
    public Response deleteConsignGoodsBatch(@RequestBody @Valid ConsignGoodsDeleteDTO dto) {
        return consignGoodsService.deleteConsignGoodsBatch(dto);
    }
}