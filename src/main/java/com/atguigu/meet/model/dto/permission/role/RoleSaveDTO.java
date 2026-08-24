package com.atguigu.meet.model.dto.permission.role;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 角色新增DTO
 */
@Data
public class RoleSaveDTO {
    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    private String roleCode;

    /** 状态 true启用 false禁用 */
    private Boolean status;

    /**
     * 菜单ID列表（可选，传入时同时分配菜单权限，全量覆盖）
     * 为 null 时不修改已有菜单关联
     */
    private List<Long> menuIds;
}