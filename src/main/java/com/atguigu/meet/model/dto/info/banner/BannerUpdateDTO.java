package com.atguigu.meet.model.dto.info.banner;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 轮播图修改DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "轮播图修改参数")
public class BannerUpdateDTO extends BannerSaveDTO {
    @Schema(description = "轮播图ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "轮播图ID不能为空")
    private Long id;
}