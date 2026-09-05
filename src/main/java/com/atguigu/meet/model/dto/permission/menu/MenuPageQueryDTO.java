package com.atguigu.meet.model.dto.permission.menu;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 菜单分页查询DTO
 */
@Data
@Schema(description = "菜单分页查询参数")
public class MenuPageQueryDTO {
    @Schema(description = "分页页码", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "分页页码不能为空")
    private Integer pageNum;
    
    @Schema(description = "每页条数", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "每页条数不能为空")
    private Integer pageSize;

    /** 菜单名称（模糊查询） */
    @Schema(description = "菜单名称（模糊查询）", example = "用户管理")
    private String name;
    
    /** 类型 0目录 1菜单 2按钮权限 */
    @Schema(description = "类型", example = "0", allowableValues = {"0", "1", "2"})
    private Integer type;
    
    /** 状态 1启用 0禁用 */
    @Schema(description = "状态", example = "1", allowableValues = {"0", "1"})
    private Integer status;
}