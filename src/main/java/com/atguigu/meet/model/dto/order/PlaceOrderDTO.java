package com.atguigu.meet.model.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * C 端用户抢购下单 DTO
 */
@Data
@Schema(description = "C端用户抢购下单参数")
public class PlaceOrderDTO {

    @Schema(description = "商品ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "商品ID不能为空")
    private Long goodsId;

    @Schema(description = "收货地址ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "收货地址ID不能为空")
    private Long addressId;
}