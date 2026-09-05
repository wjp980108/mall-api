package com.atguigu.meet.model.dto.permission.userRole;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 用户分配角色DTO
 */
@Data
@Schema(description = "用户分配角色参数")
public class UserAssignRoleDTO {
    @Schema(description = "用户ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /** 角色ID列表（全量覆盖：传入的列表即为该用户最终拥有的角色） */
    @Schema(description = "角色ID列表", example = "[1, 2]")
    private List<Long> roleIds;
}