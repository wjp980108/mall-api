package com.atguigu.meet.model.dto.permission.role;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 角色分配菜单DTO
 */
@Data
@Schema(description = "角色分配菜单参数")
public class RoleAssignMenuDTO {
    @Schema(description = "角色ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    /** 菜单ID列表（全量覆盖：传入的列表即为该角色最终拥有的菜单权限） */
    @Schema(description = "菜单ID列表", example = "[1, 2, 3]")
    private List<Long> menuIds;
}