package com.atguigu.meet.model.dto.goods.list;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 商品上下架DTO
 */
@Data
@Schema(description = "商品上下架参数")
public class GoodsStatusDTO {
    @Schema(description = "商品ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "商品ID不能为空")
    private Long id;

    /** 目标状态 false=下架 true=已上架 */
    @Schema(description = "目标状态", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "目标状态不能为空")
    private Boolean status;
}