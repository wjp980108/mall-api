package com.atguigu.meet.model.dto.permission.role;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 角色新增DTO
 */
@Data
@Schema(description = "角色新增参数")
public class RoleSaveDTO {
    @Schema(description = "角色名称", example = "管理员", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    @Schema(description = "角色编码", example = "admin")
    private String roleCode;

    /** 状态 true启用 false禁用 */
    @Schema(description = "状态", example = "true")
    private Boolean status;

    /**
     * 菜单ID列表（可选，传入时同时分配菜单权限，全量覆盖）
     * 为 null 时不修改已有菜单关联
     */
    @Schema(description = "菜单ID列表", example = "[1, 2, 3]")
    private List<Long> menuIds;
}