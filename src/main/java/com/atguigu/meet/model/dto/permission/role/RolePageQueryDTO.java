package com.atguigu.meet.model.dto.permission.role;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 角色分页查询DTO
 */
@Data
@Schema(description = "角色分页查询参数")
public class RolePageQueryDTO {
    @Schema(description = "分页页码", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "分页页码不能为空")
    private Integer pageNum;
    
    @Schema(description = "每页条数", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "每页条数不能为空")
    private Integer pageSize;

    /** 角色名称（模糊查询） */
    @Schema(description = "角色名称（模糊查询）", example = "管理员")
    private String roleName;
    
    /** 角色编码（模糊查询） */
    @Schema(description = "角色编码（模糊查询）", example = "admin")
    private String roleCode;
    
    /** 是否启用（true启用 false禁用） */
    @Schema(description = "状态", example = "true")
    private Boolean status;
}