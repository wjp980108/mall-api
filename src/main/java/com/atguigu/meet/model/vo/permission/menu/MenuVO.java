package com.atguigu.meet.model.vo.permission.menu;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单响应VO（支持树形结构）
 */
@Data
@Schema(description = "菜单响应数据")
public class MenuVO {
    @Schema(description = "菜单ID")
    private Long id;
    @Schema(description = "父菜单ID")
    private Long parentId;
    @Schema(description = "菜单名称")
    private String name;
    @Schema(description = "菜单编码")
    private String menuCode;
    @Schema(description = "权限标识")
    private String perm;
    /** 类型 0目录 1菜单 2按钮权限
     * @see com.atguigu.meet.enums.MenuType
     */
    @Schema(description = "类型 0目录 1菜单 2按钮权限")
    private Integer type;

    /** 类型中文名：目录/菜单/按钮权限（由 Service 层通过枚举组装） */
    @Schema(description = "类型中文名")
    private String typeName;
    @Schema(description = "路由路径")
    private String path;
    @Schema(description = "路由名称")
    private String routeName;
    @Schema(description = "组件路径")
    private String componentPath;
    @Schema(description = "图标")
    private String icon;
    @Schema(description = "排序")
    private Integer sort;
    @Schema(description = "是否可见")
    private Boolean visible;
    @Schema(description = "是否缓存")
    private Boolean keepAlive;
    @Schema(description = "激活菜单")
    private String activeMenu;
    @Schema(description = "是否在菜单中隐藏")
    private Boolean hideInMenu;
    @Schema(description = "是否在标签中隐藏")
    private Boolean hideInTag;
    @Schema(description = "是否隐藏父级")
    private Boolean hideParent;
    @Schema(description = "状态 1启用 0禁用")
    private Boolean status;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 子菜单/按钮(树形结构) */
    @Schema(description = "子菜单列表")
    private List<MenuVO> children;
}