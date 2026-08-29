package com.atguigu.meet.enums;

import lombok.Getter;

/**
 * 邀请码状态枚举
 * 对应 sys_invite_code.status
 * 0可用 1手动失效 2名额已满停用
 */
@Getter
public enum InviteCodeStatus {

    AVAILABLE(0, "可用"),
    MANUAL_INVALID(1, "手动失效"),
    FULL_INVALID(2, "名额已满停用");

    private final int code;
    private final String desc;

    InviteCodeStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static InviteCodeStatus of(Integer code) {
        if (code == null) return null;
        for (InviteCodeStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        return null;
    }

    public static String descOf(Integer code) {
        InviteCodeStatus s = of(code);
        return s == null ? "未知" : s.getDesc();
    }
}
