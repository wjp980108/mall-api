package com.atguigu.meet.model.dto.roborder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * C 端抢购下单 DTO（基于场次商品关联 t_session_product）
 */
@Data
@Schema(description = "抢购下单参数")
public class PlaceRobOrderDTO {

    /** 场次商品关联ID（t_session_product.id，库存扣减锚点） */
    @Schema(description = "场次商品关联ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "场次商品不能为空")
    private Long sessionProductId;

    /** 购买数量，默认 1 */
    @Schema(description = "购买数量", example = "1")
    @Min(value = 1, message = "购买数量至少为1")
    private Integer quantity = 1;
}
