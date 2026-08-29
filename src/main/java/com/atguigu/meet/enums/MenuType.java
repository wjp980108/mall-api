package com.atguigu.meet.enums;

import lombok.Getter;

/**
 * 菜单/权限类型枚举
 * 对应 sys_menu.type
 * 0目录 1菜单 2按钮权限
 */
@Getter
public enum MenuType {

    DIRECTORY(0, "目录"),
    MENU(1, "菜单"),
    BUTTON(2, "按钮权限");

    private final int code;
    private final String desc;

    MenuType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static MenuType of(Integer code) {
        if (code == null) return null;
        for (MenuType t : values()) {
            if (t.code == code) {
                return t;
            }
        }
        return null;
    }

    public static String descOf(Integer code) {
        MenuType t = of(code);
        return t == null ? "未知" : t.getDesc();
    }
}
