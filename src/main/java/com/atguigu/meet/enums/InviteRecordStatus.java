package com.atguigu.meet.enums;

import lombok.Getter;

/**
 * 邀请记录状态枚举
 * 对应 sys_invite_record.status
 * 0已邀请待注册 1已注册 2已取消
 */
@Getter
public enum InviteRecordStatus {

    WAIT_REGISTER(0, "已邀请待注册"),
    REGISTERED(1, "已注册"),
    CANCEL(2, "已取消");

    private final int code;
    private final String desc;

    InviteRecordStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static InviteRecordStatus of(Integer code) {
        if (code == null) return null;
        for (InviteRecordStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        return null;
    }

    public static String descOf(Integer code) {
        InviteRecordStatus s = of(code);
        return s == null ? "未知" : s.getDesc();
    }
}
