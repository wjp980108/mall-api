package com.atguigu.meet.model.dto.goods.consign;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 抢购托售商品业务状态流转DTO
 * <p>
 * 业务状态：1挂卖中 2已抢购待付款 3等待确认付款 4待处理 5委托代卖
 */
@Data
@Schema(description = "托售商品业务状态更新参数")
public class ConsignGoodsBizStatusDTO {
    @Schema(description = "商品ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "商品ID不能为空")
    private Long id;

    /**
     * 目标业务状态
     * 1挂卖中 2已抢购待付款 3等待确认付款 4待处理 5委托代卖
     */
    @Schema(description = "目标业务状态", example = "1", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"1", "2", "3", "4", "5"})
    @NotNull(message = "目标业务状态不能为空")
    @Min(value = 1, message = "业务状态取值范围 1-5")
    @Max(value = 5, message = "业务状态取值范围 1-5")
    private Integer goodsStatus;
}