package com.atguigu.meet.model.dto.seckill.session;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 场次启用/禁用DTO
 */
@Data
@Schema(description = "场次启用/禁用参数")
public class SessionStatusDTO {
    @Schema(description = "场次ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "场次ID不能为空")
    private Long id;

    /** 目标状态 true启用 false禁用 */
    @Schema(description = "目标状态", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "目标状态不能为空")
    private Boolean status;
}