package com.atguigu.meet.model.dto.permission.role;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 角色修改DTO
 * <p>
 * 所有字段除 id 外均可选：传入则修改，不传则保持数据库原值。
 * menuIds != null 时执行全量覆盖（含空列表表示清除所有菜单），为 null 时不修改已有菜单关联。
 */
@Data
@Schema(description = "角色修改参数")
public class RoleUpdateDTO {
    @Schema(description = "角色ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "角色ID不能为空")
    private Long id;

    /** 状态 true启用 false禁用（可选） */
    @Schema(description = "状态", example = "true")
    private Boolean status;

    /**
     * 菜单ID列表（可选）
     * 为 null：不修改已有菜单关联
     * 非 null：全量覆盖（含空列表表示清除所有菜单权限）
     */
    @Schema(description = "菜单ID列表", example = "[1, 2, 3]")
    private List<Long> menuIds;
}