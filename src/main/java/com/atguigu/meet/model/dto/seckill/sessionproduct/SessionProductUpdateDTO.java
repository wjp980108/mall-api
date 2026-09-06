package com.atguigu.meet.model.dto.seckill.sessionproduct;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 场次商品关联修改DTO（可改场次/商品/库存/排序）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "场次商品关联修改参数")
public class SessionProductUpdateDTO extends SessionProductSaveDTO {

    @Schema(description = "关联ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "关联ID不能为空")
    private Long id;
}
