package com.atguigu.meet.controller.seckill.sessionproduct;

import com.atguigu.meet.annotation.RequirePermission;
import com.atguigu.meet.common.Response;
import com.atguigu.meet.constant.PermissionConst;
import com.atguigu.meet.model.dto.seckill.sessionproduct.SessionProductBatchSaveDTO;
import com.atguigu.meet.model.dto.seckill.sessionproduct.SessionProductPageQueryDTO;
import com.atguigu.meet.model.dto.seckill.sessionproduct.SessionProductUpdateDTO;
import com.atguigu.meet.model.vo.seckill.sessionproduct.SessionProductVO;
import com.atguigu.meet.service.seckill.sessionproduct.SessionProductService;
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
 * 场次商品关联管理
 */
@RestController
@RequestMapping("/sessionProducts")
@Validated
@Tag(name = "场次商品关联管理", description = "场次关联抢购商品CRUD（含该场次该商品的抢购库存）")
public class SessionProductController {

    @Autowired
    private SessionProductService sessionProductService;

    /** 分页列表 */
    @GetMapping
    @Operation(summary = "场次商品关联分页列表", description = "分页查询场次商品关联列表（含场次名称与商品信息）")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SessionProductVO.class)))
    public Response getPageList(@Valid SessionProductPageQueryDTO parameter) {
        return sessionProductService.getPageList(parameter);
    }

    /** 查询某场次的全部关联商品 */
    @GetMapping("/bySession/{sessionId}")
    @Operation(summary = "场次关联商品列表", description = "查询某场次的全部关联商品（场次编辑回显用）")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SessionProductVO.class)))
    public Response listBySessionId(@PathVariable Long sessionId) {
        return sessionProductService.listBySessionId(sessionId);
    }

    /** 根据ID查详情 */
    @GetMapping("/{id}")
    @Operation(summary = "场次商品关联详情", description = "根据ID查询场次商品关联详情")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SessionProductVO.class)))
    public Response getSessionProductById(@PathVariable Long id) {
        return sessionProductService.getSessionProductById(id);
    }

    /** 批量新增关联（一个场次一次关联 n 个商品，每个商品指定库存） */
    @PostMapping
    @RequirePermission(PermissionConst.SESSION_PRODUCT_ADD)
    @Operation(summary = "批量新增场次商品关联", description = "一个场次一次关联多个抢购商品，每个商品指定该场次库存；已关联的商品自动跳过")
    public Response<Void> batchAddSessionProduct(@RequestBody @Valid SessionProductBatchSaveDTO dto) {
        return sessionProductService.batchAddSessionProduct(dto);
    }

    /** 修改关联 */
    @PutMapping
    @RequirePermission(PermissionConst.SESSION_PRODUCT_UPDATE)
    @Operation(summary = "修改场次商品关联", description = "修改场次商品关联（可改场次/商品/库存/排序）")
    public Response<Void> updateSessionProduct(@RequestBody @Valid SessionProductUpdateDTO dto) {
        return sessionProductService.updateSessionProduct(dto);
    }

    /** 删除关联（逻辑删除） */
    @DeleteMapping("/{id}")
    @RequirePermission(PermissionConst.SESSION_PRODUCT_DELETE)
    @Operation(summary = "删除场次商品关联", description = "逻辑删除场次商品关联")
    public Response<Void> deleteSessionProduct(@PathVariable Long id) {
        return sessionProductService.deleteSessionProduct(id);
    }
}
