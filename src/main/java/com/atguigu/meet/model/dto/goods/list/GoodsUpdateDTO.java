package com.atguigu.meet.model.dto.goods.list;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品修改DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "商品修改参数")
public class GoodsUpdateDTO extends GoodsSaveDTO {
    @Schema(description = "商品ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "商品ID不能为空")
    private Long id;
}