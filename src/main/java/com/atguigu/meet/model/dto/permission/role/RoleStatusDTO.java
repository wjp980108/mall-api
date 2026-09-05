package com.atguigu.meet.model.dto.permission.role;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 角色启用/禁用DTO
 */
@Data
@Schema(description = "角色启用/禁用参数")
public class RoleStatusDTO {
    @Schema(description = "角色ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "角色ID不能为空")
    private Long id;

    /** 目标状态 true启用 false禁用 */
    @Schema(description = "目标状态", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "目标状态不能为空")
    private Boolean status;
}