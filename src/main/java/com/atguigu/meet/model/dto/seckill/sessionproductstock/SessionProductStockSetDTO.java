package com.atguigu.meet.model.dto.seckill.sessionproductstock;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 场次商品库存设置DTO（按关联ID设置该场次该商品的抢购库存）
 */
@Data
@Schema(description = "场次商品库存设置参数")
public class SessionProductStockSetDTO {

    /** 关联ID（t_session_product.id） */
    @Schema(description = "关联ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "关联ID不能为空")
    private Long id;

    /** 目标库存（绝对值设置） */
    @Schema(description = "目标库存", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "目标库存不能为空")
    @Min(value = 0, message = "目标库存不能为负数")
    private Integer stock;
}
