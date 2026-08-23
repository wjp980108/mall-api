package com.atguigu.meet.model.dto.permission.role;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 角色分页查询DTO
 */
@Data
public class RolePageQueryDTO {
    @NotNull(message = "分页页码不能为空")
    private Integer pageNum;
    @NotNull(message = "每页条数不能为空")
    private Integer pageSize;

    /** 角色名称（模糊查询） */
    private String roleName;
    /** 角色编码（模糊查询） */
    private String roleCode;
    /** 是否启用（true启用 false禁用） */
    private Boolean status;
}