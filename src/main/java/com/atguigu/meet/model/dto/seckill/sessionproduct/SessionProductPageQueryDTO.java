package com.atguigu.meet.model.dto.seckill.sessionproduct;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 场次商品关联分页查询DTO（场次商品列表 / 库存列表共用）
 */
@Data
@Schema(description = "场次商品关联分页查询参数")
public class SessionProductPageQueryDTO {

    @Schema(description = "分页页码", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "分页页码不能为空")
    private Integer pageNum;

    @Schema(description = "每页条数", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "每页条数不能为空")
    private Integer pageSize;

    /** 场次ID（精确过滤） */
    @Schema(description = "场次ID", example = "1")
    private Long sessionId;

    /** 商品名称（模糊查询） */
    @Schema(description = "商品名称（模糊查询）", example = "手机")
    private String goodsName;
}
