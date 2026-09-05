package com.atguigu.meet.model.dto.permission.menu;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 菜单新增DTO
 */
@Data
@Schema(description = "菜单新增参数")
public class MenuSaveDTO {
    @Schema(description = "父菜单ID", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "父菜单ID不能为空")
    private Long parentId;

    @Schema(description = "菜单名称", example = "用户管理", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "菜单名称不能为空")
    private String name;

    /** 权限标识(按钮用，格式: 模块:页面:操作，如 sys:user:delete) */
    @Schema(description = "权限标识", example = "sys:user:delete")
    private String perm;

    @Schema(description = "菜单类型", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "菜单类型不能为空")
    private Integer type;

    @Schema(description = "路由路径", example = "/user")
    private String path;
    
    @Schema(description = "路由名称", example = "User")
    private String routeName;
    
    @Schema(description = "组件路径", example = "views/user/index")
    private String componentPath;
    
    @Schema(description = "图标", example = "user")
    private String icon;
    
    @Schema(description = "排序", example = "1")
    private Integer sort;
    
    @Schema(description = "是否可见", example = "true")
    private Boolean visible;
    
    @Schema(description = "是否缓存", example = "true")
    private Boolean keepAlive;
    
    @Schema(description = "激活菜单", example = "/user")
    private String activeMenu;
    
    @Schema(description = "是否在菜单中隐藏", example = "false")
    private Boolean hideInMenu;
    
    @Schema(description = "是否在标签中隐藏", example = "false")
    private Boolean hideInTag;
    
    @Schema(description = "是否隐藏父级", example = "false")
    private Boolean hideParent;
    
    @Schema(description = "状态", example = "true")
    private Boolean status;
}