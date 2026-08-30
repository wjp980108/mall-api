package com.atguigu.meet.model.dto.order;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * C 端用户抢购下单 DTO
 */
@Data
public class PlaceOrderDTO {

    @NotNull(message = "商品ID不能为空")
    private Long goodsId;

    @NotNull(message = "收货地址ID不能为空")
    private Long addressId;
}
