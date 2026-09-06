package com.atguigu.meet.model.vo.permission.role;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色响应VO
 */
@Data
@Schema(description = "角色响应数据")
public class RoleVO {
    @Schema(description = "角色ID")
    private Long id;
    @Schema(description = "角色名称")
    private String roleName;
    @Schema(description = "角色编码")
    private String roleCode;
    @Schema(description = "状态 1启用 0禁用")
    private Boolean status;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 角色拥有的菜单ID列表 */
    @Schema(description = "菜单ID列表")
    private List<Long> menuIds;
}