package com.atguigu.meet.controller.seckill.sessionproductstock;

import com.atguigu.meet.annotation.RequirePermission;
import com.atguigu.meet.common.Response;
import com.atguigu.meet.constant.PermissionConst;
import com.atguigu.meet.model.dto.seckill.sessionproduct.SessionProductPageQueryDTO;
import com.atguigu.meet.model.dto.seckill.sessionproductstock.SessionProductStockSetDTO;
import com.atguigu.meet.model.vo.seckill.sessionproduct.SessionProductVO;
import com.atguigu.meet.service.seckill.sessionproductstock.SessionProductStockService;
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
 * 场次商品库存管理
 */
@RestController
@RequestMapping("/sessionProductStocks")
@Validated
@Tag(name = "场次商品库存管理", description = "场次商品库存查询与设置（库存行级挂在场次商品关联上）")
public class SessionProductStockController {

    @Autowired
    private SessionProductStockService sessionProductStockService;

    /** 库存分页列表 */
    @GetMapping
    @RequirePermission(PermissionConst.SESSION_PRODUCT_STOCK_QUERY)
    @Operation(summary = "场次商品库存分页列表", description = "分页查询场次商品库存（含场次名称、商品信息与当前库存）")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SessionProductVO.class)))
    public Response getPageList(@Valid SessionProductPageQueryDTO parameter) {
        return sessionProductStockService.getPageList(parameter);
    }

    /** 设置库存（绝对值） */
    @PutMapping("/stock")
    @RequirePermission(PermissionConst.SESSION_PRODUCT_STOCK_UPDATE)
    @Operation(summary = "设置场次商品库存", description = "按关联ID设置该场次该商品的抢购库存（绝对值）")
    public Response<Void> setStock(@RequestBody @Valid SessionProductStockSetDTO dto) {
        return sessionProductStockService.setStock(dto);
    }
}
