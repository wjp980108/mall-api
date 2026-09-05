package com.atguigu.meet.model.dto.goods.consign;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * 抢购托售商品批量删除 DTO
 */
@Data
@Schema(description = "托售商品批量删除参数")
public class ConsignGoodsDeleteDTO {

    @Schema(description = "托售商品ID数组", example = "[1, 2, 3]", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "托售商品ids不能为空")
    private Long[] ids;
}