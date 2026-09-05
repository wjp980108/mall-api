package com.atguigu.meet.model.dto.goods.consign;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 抢购托售商品上下架DTO
 */
@Data
@Schema(description = "托售商品上下架参数")
public class ConsignGoodsOnlineStatusDTO {
    @Schema(description = "商品ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "商品ID不能为空")
    private Long id;

    /** 目标上下架状态 false下架 true上架 */
    @Schema(description = "目标上下架状态", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "目标状态不能为空")
    private Boolean onlineStatus;
}