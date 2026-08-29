package com.atguigu.meet.enums;

import lombok.Getter;

/**
 * 用户性别枚举
 * 对应 sys_user.gender
 * 0未知 1男 2女
 */
@Getter
public enum Gender {

    UNKNOWN(0, "未知"),
    MALE(1, "男"),
    FEMALE(2, "女");

    private final int code;
    private final String desc;

    Gender(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static Gender of(Integer code) {
        if (code == null) return null;
        for (Gender g : values()) {
            if (g.code == code) {
                return g;
            }
        }
        return null;
    }

    public static String descOf(Integer code) {
        Gender g = of(code);
        return g == null ? "未知" : g.getDesc();
    }
}
