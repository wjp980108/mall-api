package com.atguigu.meet.model.vo.permission.menu;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单响应VO（支持树形结构）
 */
@Data
public class MenuVO {
    private Long id;
    private Long parentId;
    private String name;
    private String menuCode;
    private String perm;
    /** 类型 0目录 1菜单 2按钮权限
     * @see com.atguigu.meet.enums.MenuType
     */
    private Integer type;

    /** 类型中文名：目录/菜单/按钮权限（由 Service 层通过枚举组装） */
    private String typeName;
    private String path;
    private String routeName;
    private String componentPath;
    private String icon;
    private Integer sort;
    private Boolean visible;
    private Boolean keepAlive;
    private String activeMenu;
    private Boolean hideInMenu;
    private Boolean hideInTag;
    private Boolean hideParent;
    private Boolean status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 子菜单/按钮(树形结构) */
    private List<MenuVO> children;
}